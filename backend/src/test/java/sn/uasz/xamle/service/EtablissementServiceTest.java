package sn.uasz.xamle.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.EtablissementRequest;
import sn.uasz.xamle.dto.EtablissementResponse;
import sn.uasz.xamle.model.Etablissement;
import sn.uasz.xamle.model.TypeEtablissement;
import sn.uasz.xamle.repository.EtablissementRepository;
import sn.uasz.xamle.repository.SpecialisteRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EtablissementServiceTest {

    private static final double LAT = 12.5833;
    private static final double LNG = -16.2719;

    @Mock
    EtablissementRepository repo;
    @Mock
    SpecialisteRepository specialistes;
    @InjectMocks
    EtablissementService service;

    private Etablissement etab(long id, String nom, TypeEtablissement type, boolean urgence,
                               double lat, double lng) {
        return Etablissement.builder().id(id).nom(nom).type(type).urgence(urgence)
                .latitude(lat).longitude(lng).quartier("Escale").build();
    }

    private EtablissementRequest requete(String nom) {
        return new EtablissementRequest(nom, TypeEtablissement.CLINIQUE, "Rue 1", "Escale",
                LAT, LNG, "339911111", "24h/24", "Urgences", true);
    }

    private int codeErreur(Runnable action) {
        try {
            action.run();
        } catch (ResponseStatusException e) {
            return e.getStatusCode().value();
        }
        return -1;
    }

    // ---------- Recherche ----------

    @Test
    void rechercher_sansFiltre_triParNom() {
        when(repo.findAll()).thenReturn(List.of(
                etab(1, "Zeta", TypeEtablissement.HOPITAL, true, LAT, LNG),
                etab(2, "Alpha", TypeEtablissement.PHARMACIE, false, LAT, LNG)));

        List<EtablissementResponse> res = service.rechercher(null, null, null, null, null);

        assertThat(res).extracting(EtablissementResponse::nom).containsExactly("Alpha", "Zeta");
        assertThat(res.get(0).distanceKm()).isNull();
    }

    @Test
    void rechercher_parType() {
        when(repo.findAll()).thenReturn(List.of(
                etab(1, "Hôpital", TypeEtablissement.HOPITAL, true, LAT, LNG),
                etab(2, "Pharmacie", TypeEtablissement.PHARMACIE, false, LAT, LNG)));

        List<EtablissementResponse> res = service.rechercher(TypeEtablissement.PHARMACIE, null, null, null, null);

        assertThat(res).extracting(EtablissementResponse::nom).containsExactly("Pharmacie");
    }

    @Test
    void rechercher_parUrgence() {
        when(repo.findAll()).thenReturn(List.of(
                etab(1, "Hôpital", TypeEtablissement.HOPITAL, true, LAT, LNG),
                etab(2, "Pharmacie", TypeEtablissement.PHARMACIE, false, LAT, LNG)));

        List<EtablissementResponse> res = service.rechercher(null, true, null, null, null);

        assertThat(res).extracting(EtablissementResponse::nom).containsExactly("Hôpital");
    }

    @Test
    void rechercher_avecPosition_triParDistance() {
        when(repo.findAll()).thenReturn(List.of(
                etab(1, "Loin", TypeEtablissement.HOPITAL, true, 12.70, -16.40),
                etab(2, "Proche", TypeEtablissement.HOPITAL, true, LAT, LNG)));

        List<EtablissementResponse> res = service.rechercher(null, null, LAT, LNG, null);

        assertThat(res).extracting(EtablissementResponse::nom).containsExactly("Proche", "Loin");
        assertThat(res.get(0).distanceKm()).isLessThan(0.01);
    }

    @Test
    void rechercher_avecRayon_ecarteLesTropLoin() {
        when(repo.findAll()).thenReturn(List.of(
                etab(1, "Loin", TypeEtablissement.HOPITAL, true, 12.70, -16.40),
                etab(2, "Proche", TypeEtablissement.HOPITAL, true, LAT, LNG)));

        List<EtablissementResponse> res = service.rechercher(null, null, LAT, LNG, 5.0);

        assertThat(res).extracting(EtablissementResponse::nom).containsExactly("Proche");
    }

    @Test
    void urgencesProches_neGardeQueLesEtablissementsAvecUrgences() {
        when(repo.findAll()).thenReturn(List.of(
                etab(1, "Avec urgences", TypeEtablissement.HOPITAL, true, LAT, LNG),
                etab(2, "Sans urgences", TypeEtablissement.PHARMACIE, false, LAT, LNG)));

        List<EtablissementResponse> res = service.urgencesProches(LAT, LNG);

        assertThat(res).extracting(EtablissementResponse::nom).containsExactly("Avec urgences");
        assertThat(res.get(0).distanceKm()).isNotNull();
    }

    // ---------- Lecture et écriture ----------

    @Test
    void trouver_existant() {
        when(repo.findById(1L)).thenReturn(Optional.of(etab(1, "Hôpital", TypeEtablissement.HOPITAL, true, LAT, LNG)));

        assertThat(service.trouver(1L).nom()).isEqualTo("Hôpital");
    }

    @Test
    void trouver_inconnu_404() {
        when(repo.findById(9L)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.trouver(9L))).isEqualTo(404);
    }

    @Test
    void creer_enregistreLEtablissement() {
        when(repo.save(any(Etablissement.class))).thenAnswer(inv -> inv.getArgument(0));

        EtablissementResponse res = service.creer(requete("Clinique du Fleuve"));

        assertThat(res.nom()).isEqualTo("Clinique du Fleuve");
        assertThat(res.type()).isEqualTo(TypeEtablissement.CLINIQUE);
        assertThat(res.urgence()).isTrue();
    }

    @Test
    void modifier_remplaceLesChamps() {
        Etablissement existant = etab(1, "Ancien nom", TypeEtablissement.HOPITAL, false, LAT, LNG);
        when(repo.findById(1L)).thenReturn(Optional.of(existant));
        when(repo.save(any(Etablissement.class))).thenAnswer(inv -> inv.getArgument(0));

        EtablissementResponse res = service.modifier(1L, requete("Nouveau nom"));

        assertThat(res.nom()).isEqualTo("Nouveau nom");
        assertThat(res.horaires()).isEqualTo("24h/24");
    }

    @Test
    void modifier_inconnu_404() {
        when(repo.findById(9L)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.modifier(9L, requete("X")))).isEqualTo(404);
    }

    @Test
    void supprimer_sansSpecialiste_estSupprime() {
        Etablissement e = etab(1, "Hôpital", TypeEtablissement.HOPITAL, true, LAT, LNG);
        when(repo.findById(1L)).thenReturn(Optional.of(e));

        service.supprimer(1L);

        verify(repo).delete(e);
    }

    @Test
    void supprimer_avecSpecialistesRattaches_409() {
        when(repo.findById(1L)).thenReturn(Optional.of(etab(1, "Hôpital", TypeEtablissement.HOPITAL, true, LAT, LNG)));
        when(specialistes.existsByEtablissementId(1L)).thenReturn(true);

        assertThat(codeErreur(() -> service.supprimer(1L))).isEqualTo(409);
        verify(repo, never()).delete(any());
    }

    @Test
    void supprimer_inconnu_404() {
        when(repo.findById(9L)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.supprimer(9L))).isEqualTo(404);
    }
}
