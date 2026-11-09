package sn.uasz.xamle.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.SpecialisteRequest;
import sn.uasz.xamle.dto.SpecialisteResponse;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.StatutValidation;
import sn.uasz.xamle.model.Utilisateur;
import sn.uasz.xamle.repository.EtablissementRepository;
import sn.uasz.xamle.repository.SpecialisteRepository;
import sn.uasz.xamle.repository.UtilisateurRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialisteServiceTest {

    private static final String EMAIL = "pro@test.sn";
    private static final double LAT = 12.5833;
    private static final double LNG = -16.2719;

    @Mock
    SpecialisteRepository repo;
    @Mock
    EtablissementRepository etablissementRepo;
    @Mock
    UtilisateurRepository utilisateurRepo;
    @InjectMocks
    SpecialisteService service;

    private Specialiste spec(long id, String nom, String specialite, String langues,
                             double lat, double lng, boolean disponible) {
        return Specialiste.builder()
                .id(id).nom(nom).prenom("Test").specialite(specialite).langues(langues)
                .latitude(lat).longitude(lng).disponible(disponible)
                .statut(StatutValidation.VALIDE).build();
    }

    private SpecialisteRequest requete(Long etablissementId) {
        return new SpecialisteRequest("Diop", "Awa", "Pédiatre", etablissementId, "Escale",
                LAT, LNG, "771234567", "8h-17h", "Français,Wolof", null, true);
    }

    private int codeErreur(Runnable action) {
        try {
            action.run();
        } catch (ResponseStatusException e) {
            return e.getStatusCode().value();
        }
        return -1;
    }

    private void validesRenvoyes(Specialiste... specialistes) {
        when(repo.findByStatut(StatutValidation.VALIDE)).thenReturn(List.of(specialistes));
    }

    // ---------- Recherche ----------

    @Test
    void rechercher_sansFiltre_triParNom() {
        validesRenvoyes(spec(1, "Zeta", "Pédiatre", "Français", LAT, LNG, true),
                spec(2, "Alpha", "Dentiste", "Français", LAT, LNG, true));

        List<SpecialisteResponse> res = service.rechercher(null, null, null, null, null, null, null);

        assertThat(res).extracting(SpecialisteResponse::nom).containsExactly("Alpha", "Zeta");
    }

    @Test
    void rechercher_specialite_ignoreAccentsEtCasse() {
        validesRenvoyes(spec(1, "Awa", "Pédiatre", "Français", LAT, LNG, true),
                spec(2, "Ibra", "Dentiste", "Français", LAT, LNG, true));

        List<SpecialisteResponse> res = service.rechercher("PEDIATRE", null, null, null, null, null, null);

        assertThat(res).extracting(SpecialisteResponse::nom).containsExactly("Awa");
    }

    @Test
    void rechercher_langue() {
        validesRenvoyes(spec(1, "Awa", "Pédiatre", "Français,Wolof", LAT, LNG, true),
                spec(2, "Ibra", "Dentiste", "Français", LAT, LNG, true));

        List<SpecialisteResponse> res = service.rechercher(null, "wolof", null, null, null, null, null);

        assertThat(res).extracting(SpecialisteResponse::nom).containsExactly("Awa");
    }

    @Test
    void rechercher_disponibleSeulement() {
        validesRenvoyes(spec(1, "Awa", "Pédiatre", "Français", LAT, LNG, true),
                spec(2, "Ibra", "Dentiste", "Français", LAT, LNG, false));

        List<SpecialisteResponse> res = service.rechercher(null, null, null, true, null, null, null);

        assertThat(res).extracting(SpecialisteResponse::nom).containsExactly("Awa");
    }

    @Test
    void rechercher_avecPosition_triParDistance() {
        validesRenvoyes(spec(1, "Loin", "Pédiatre", "Français", 12.70, -16.40, true),
                spec(2, "Proche", "Dentiste", "Français", LAT, LNG, true));

        List<SpecialisteResponse> res = service.rechercher(null, null, null, null, LAT, LNG, null);

        assertThat(res).extracting(SpecialisteResponse::nom).containsExactly("Proche", "Loin");
        assertThat(res.get(0).distanceKm()).isLessThan(0.01);
        assertThat(res.get(1).distanceKm()).isGreaterThan(10.0);
    }

    @Test
    void rechercher_avecRayon_ecarteLesTropLoin() {
        validesRenvoyes(spec(1, "Loin", "Pédiatre", "Français", 12.70, -16.40, true),
                spec(2, "Proche", "Dentiste", "Français", LAT, LNG, true));

        List<SpecialisteResponse> res = service.rechercher(null, null, null, null, LAT, LNG, 5.0);

        assertThat(res).extracting(SpecialisteResponse::nom).containsExactly("Proche");
    }

    @Test
    void trouverValide_profilNonValide_404() {
        Specialiste enAttente = spec(1, "Awa", "Pédiatre", "Français", LAT, LNG, true);
        enAttente.setStatut(StatutValidation.EN_ATTENTE);
        when(repo.findById(1L)).thenReturn(Optional.of(enAttente));

        assertThat(codeErreur(() -> service.trouverValide(1L))).isEqualTo(404);
    }

    @Test
    void trouverValide_profilValide_ok() {
        when(repo.findById(1L)).thenReturn(Optional.of(spec(1, "Awa", "Pédiatre", "Français", LAT, LNG, true)));

        assertThat(service.trouverValide(1L).nom()).isEqualTo("Awa");
    }

    // ---------- Espace professionnel ----------

    @Test
    void creerProfil_nouveauProfil_estEnAttente() {
        when(utilisateurRepo.findByEmail(EMAIL)).thenReturn(Optional.of(new Utilisateur()));
        when(repo.findByUtilisateurEmail(EMAIL)).thenReturn(Optional.empty());
        when(repo.save(any(Specialiste.class))).thenAnswer(inv -> inv.getArgument(0));

        SpecialisteResponse res = service.creerProfil(EMAIL, requete(null));

        assertThat(res.nom()).isEqualTo("Diop");
        assertThat(res.statut()).isEqualTo(StatutValidation.EN_ATTENTE);
    }

    @Test
    void creerProfil_dejaUnProfil_409() {
        when(utilisateurRepo.findByEmail(EMAIL)).thenReturn(Optional.of(new Utilisateur()));
        when(repo.findByUtilisateurEmail(EMAIL))
                .thenReturn(Optional.of(spec(1, "Awa", "Pédiatre", "Français", LAT, LNG, true)));

        assertThat(codeErreur(() -> service.creerProfil(EMAIL, requete(null)))).isEqualTo(409);
        verify(repo, never()).save(any());
    }

    @Test
    void creerProfil_utilisateurInconnu_404() {
        when(utilisateurRepo.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.creerProfil(EMAIL, requete(null)))).isEqualTo(404);
    }

    @Test
    void creerProfil_etablissementInconnu_400() {
        when(utilisateurRepo.findByEmail(EMAIL)).thenReturn(Optional.of(new Utilisateur()));
        when(repo.findByUtilisateurEmail(EMAIL)).thenReturn(Optional.empty());
        when(etablissementRepo.findById(99L)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.creerProfil(EMAIL, requete(99L)))).isEqualTo(400);
    }

    @Test
    void modifierProfil_remetLeProfilEnAttente() {
        Specialiste existant = spec(1, "Ancien", "Dentiste", "Français", LAT, LNG, true);
        when(repo.findByUtilisateurEmail(EMAIL)).thenReturn(Optional.of(existant));
        when(repo.save(any(Specialiste.class))).thenAnswer(inv -> inv.getArgument(0));

        SpecialisteResponse res = service.modifierProfil(EMAIL, requete(null));

        assertThat(res.nom()).isEqualTo("Diop");
        assertThat(res.statut()).isEqualTo(StatutValidation.EN_ATTENTE);
    }

    @Test
    void monProfil_absent_404() {
        when(repo.findByUtilisateurEmail(EMAIL)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.monProfil(EMAIL))).isEqualTo(404);
    }

    // ---------- Modération ----------

    @Test
    void valider_passeLeProfilEnValide() {
        Specialiste s = spec(1, "Awa", "Pédiatre", "Français", LAT, LNG, true);
        s.setStatut(StatutValidation.EN_ATTENTE);
        when(repo.findById(1L)).thenReturn(Optional.of(s));
        when(repo.save(any(Specialiste.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.valider(1L).statut()).isEqualTo(StatutValidation.VALIDE);
    }

    @Test
    void rejeter_passeLeProfilEnRejete() {
        Specialiste s = spec(1, "Awa", "Pédiatre", "Français", LAT, LNG, true);
        s.setStatut(StatutValidation.EN_ATTENTE);
        when(repo.findById(1L)).thenReturn(Optional.of(s));
        when(repo.save(any(Specialiste.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.rejeter(1L).statut()).isEqualTo(StatutValidation.REJETE);
    }

    @Test
    void valider_profilInconnu_404() {
        when(repo.findById(9L)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.valider(9L))).isEqualTo(404);
    }
}
