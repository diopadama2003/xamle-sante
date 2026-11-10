package sn.uasz.xamle.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.CreneauRequest;
import sn.uasz.xamle.dto.CreneauResponse;
import sn.uasz.xamle.model.Creneau;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.StatutValidation;
import sn.uasz.xamle.model.Utilisateur;
import sn.uasz.xamle.repository.CreneauRepository;
import sn.uasz.xamle.repository.RendezVousRepository;
import sn.uasz.xamle.repository.SpecialisteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreneauServiceTest {

    private static final String EMAIL = "pro@test.sn";

    @Mock
    CreneauRepository creneaux;
    @Mock
    SpecialisteRepository specialistes;
    @Mock
    RendezVousRepository rendezVous;
    @InjectMocks
    CreneauService service;

    private Specialiste specialiste(StatutValidation statut) {
        Utilisateur u = Utilisateur.builder().id(1L).email(EMAIL).build();
        return Specialiste.builder().id(1L).nom("Diop").prenom("Awa").specialite("Pédiatre")
                .utilisateur(u).statut(statut).build();
    }

    private Creneau creneau(Long id, Specialiste s, boolean reserve) {
        LocalDateTime debut = LocalDateTime.now().plusDays(2);
        return Creneau.builder().id(id).specialiste(s).debut(debut).fin(debut.plusMinutes(30))
                .reserve(reserve).build();
    }

    private int codeErreur(Runnable action) {
        try {
            action.run();
        } catch (ResponseStatusException e) {
            return e.getStatusCode().value();
        }
        return -1;
    }

    private CreneauRequest requete(int dansMinutes, int dureeMinutes) {
        LocalDateTime debut = LocalDateTime.now().plusMinutes(dansMinutes);
        return new CreneauRequest(debut, debut.plusMinutes(dureeMinutes));
    }

    // ---------- Public ----------

    @Test
    void libres_retourneLesCreneauxNonReserves() {
        Specialiste s = specialiste(StatutValidation.VALIDE);
        when(specialistes.findById(1L)).thenReturn(Optional.of(s));
        when(creneaux.findBySpecialisteIdAndReserveFalseAndDebutAfterOrderByDebutAsc(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(creneau(5L, s, false)));

        List<CreneauResponse> res = service.libres(1L);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).reserve()).isFalse();
    }

    @Test
    void libres_specialisteNonValide_404() {
        when(specialistes.findById(1L)).thenReturn(Optional.of(specialiste(StatutValidation.EN_ATTENTE)));

        assertThat(codeErreur(() -> service.libres(1L))).isEqualTo(404);
    }

    @Test
    void libres_specialisteInconnu_404() {
        when(specialistes.findById(9L)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.libres(9L))).isEqualTo(404);
    }

    // ---------- Création ----------

    @Test
    void creer_creneauValide_estEnregistre() {
        when(specialistes.findByUtilisateurEmail(EMAIL))
                .thenReturn(Optional.of(specialiste(StatutValidation.VALIDE)));
        when(creneaux.save(any(Creneau.class))).thenAnswer(inv -> inv.getArgument(0));

        CreneauResponse res = service.creer(EMAIL, requete(24 * 60, 30));

        assertThat(res.reserve()).isFalse();
        assertThat(res.fin()).isAfter(res.debut());
    }

    @Test
    void creer_sansProfil_404() {
        when(specialistes.findByUtilisateurEmail(EMAIL)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.creer(EMAIL, requete(60, 30)))).isEqualTo(404);
    }

    @Test
    void creer_profilNonValide_403() {
        when(specialistes.findByUtilisateurEmail(EMAIL))
                .thenReturn(Optional.of(specialiste(StatutValidation.EN_ATTENTE)));

        assertThat(codeErreur(() -> service.creer(EMAIL, requete(60, 30)))).isEqualTo(403);
    }

    @Test
    void creer_dansLePasse_400() {
        when(specialistes.findByUtilisateurEmail(EMAIL))
                .thenReturn(Optional.of(specialiste(StatutValidation.VALIDE)));

        assertThat(codeErreur(() -> service.creer(EMAIL, requete(-60, 30)))).isEqualTo(400);
    }

    @Test
    void creer_tropCourt_400() {
        when(specialistes.findByUtilisateurEmail(EMAIL))
                .thenReturn(Optional.of(specialiste(StatutValidation.VALIDE)));

        assertThat(codeErreur(() -> service.creer(EMAIL, requete(60, 10)))).isEqualTo(400);
    }

    @Test
    void creer_tropLong_400() {
        when(specialistes.findByUtilisateurEmail(EMAIL))
                .thenReturn(Optional.of(specialiste(StatutValidation.VALIDE)));

        assertThat(codeErreur(() -> service.creer(EMAIL, requete(60, 130)))).isEqualTo(400);
    }

    @Test
    void creer_chevauchantUnAutreCreneau_409() {
        when(specialistes.findByUtilisateurEmail(EMAIL))
                .thenReturn(Optional.of(specialiste(StatutValidation.VALIDE)));
        when(creneaux.existsBySpecialisteIdAndDebutLessThanAndFinGreaterThan(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(true);

        assertThat(codeErreur(() -> service.creer(EMAIL, requete(60, 30)))).isEqualTo(409);
        verify(creneaux, never()).save(any());
    }

    // ---------- Consultation et suppression ----------

    @Test
    void mesCreneaux_retourneLesCreneauxAVenir() {
        Specialiste s = specialiste(StatutValidation.VALIDE);
        when(specialistes.findByUtilisateurEmail(EMAIL)).thenReturn(Optional.of(s));
        when(creneaux.findBySpecialisteIdAndDebutAfterOrderByDebutAsc(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(creneau(5L, s, true), creneau(6L, s, false)));

        assertThat(service.mesCreneaux(EMAIL)).hasSize(2);
    }

    @Test
    void mesCreneaux_sansProfil_404() {
        when(specialistes.findByUtilisateurEmail(EMAIL)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.mesCreneaux(EMAIL))).isEqualTo(404);
    }

    @Test
    void supprimer_creneauLibre_estSupprime() {
        Creneau c = creneau(5L, specialiste(StatutValidation.VALIDE), false);
        when(creneaux.findById(5L)).thenReturn(Optional.of(c));

        service.supprimer(EMAIL, 5L);

        verify(creneaux).delete(c);
    }

    @Test
    void supprimer_creneauDUnAutreProfessionnel_404() {
        Creneau c = creneau(5L, specialiste(StatutValidation.VALIDE), false);
        when(creneaux.findById(5L)).thenReturn(Optional.of(c));

        assertThat(codeErreur(() -> service.supprimer("autre@test.sn", 5L))).isEqualTo(404);
        verify(creneaux, never()).delete(any());
    }

    @Test
    void supprimer_creneauReserve_409() {
        Creneau c = creneau(5L, specialiste(StatutValidation.VALIDE), true);
        when(creneaux.findById(5L)).thenReturn(Optional.of(c));

        assertThat(codeErreur(() -> service.supprimer(EMAIL, 5L))).isEqualTo(409);
    }

    @Test
    void supprimer_creneauAvecHistorique_409() {
        Creneau c = creneau(5L, specialiste(StatutValidation.VALIDE), false);
        when(creneaux.findById(5L)).thenReturn(Optional.of(c));
        when(rendezVous.existsByCreneauId(5L)).thenReturn(true);

        assertThat(codeErreur(() -> service.supprimer(EMAIL, 5L))).isEqualTo(409);
    }

    @Test
    void supprimer_creneauInconnu_404() {
        when(creneaux.findById(9L)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.supprimer(EMAIL, 9L))).isEqualTo(404);
    }
}
