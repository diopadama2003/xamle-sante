package sn.uasz.xamle.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.CampagneRequest;
import sn.uasz.xamle.dto.CampagneResponse;
import sn.uasz.xamle.model.AbonnementRappel;
import sn.uasz.xamle.model.Campagne;
import sn.uasz.xamle.model.TypeCampagne;
import sn.uasz.xamle.repository.AbonnementRappelRepository;
import sn.uasz.xamle.repository.CampagneRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampagneServiceTest {

    private static final String IP = "10.0.0.1";

    @Mock
    CampagneRepository campagnes;
    @Mock
    AbonnementRappelRepository abonnements;
    @Spy
    QuartierCatalogue catalogue = new QuartierCatalogue();
    @Mock
    SmsService smsService;
    @InjectMocks
    CampagneService service;

    private Campagne campagne(Long id, LocalDateTime debut) {
        return Campagne.builder()
                .id(id).titre("Vaccination rougeole").type(TypeCampagne.VACCINATION)
                .lieu("Centre de santé Boucotte").quartier("Boucotte")
                .latitude(12.59).longitude(-16.28)
                .dateDebut(debut).dateFin(debut.plusHours(4)).build();
    }

    private CampagneRequest requete(LocalDateTime debut, LocalDateTime fin, String quartier,
                                    Double lat, Double lng) {
        return new CampagneRequest("Dépistage", TypeCampagne.DEPISTAGE, "Gratuit",
                "Place centrale", quartier, lat, lng, debut, fin);
    }

    private int codeErreur(Runnable action) {
        try {
            action.run();
        } catch (ResponseStatusException e) {
            return e.getStatusCode().value();
        }
        return -1;
    }

    // ---------- Public ----------

    @Test
    void aVenir_retourneLesCampagnes() {
        when(campagnes.findByDateFinAfterOrderByDateDebutAsc(any(LocalDateTime.class)))
                .thenReturn(List.of(campagne(1L, LocalDateTime.now().plusDays(3))));

        List<CampagneResponse> res = service.aVenir();

        assertThat(res).hasSize(1);
        assertThat(res.get(0).titre()).isEqualTo("Vaccination rougeole");
    }

    @Test
    void sAbonner_numeroValide_enregistreLAbonnementAuFormatInternational() {
        when(campagnes.findById(1L)).thenReturn(Optional.of(campagne(1L, LocalDateTime.now().plusDays(2))));

        service.sAbonner(1L, "77 123 45 67", IP);

        ArgumentCaptor<AbonnementRappel> capture = ArgumentCaptor.forClass(AbonnementRappel.class);
        verify(abonnements).save(capture.capture());
        assertThat(capture.getValue().getTelephone()).isEqualTo("+221771234567");
    }

    @Test
    void sAbonner_campagneDejaCommencee_400() {
        when(campagnes.findById(1L)).thenReturn(Optional.of(campagne(1L, LocalDateTime.now().minusHours(1))));

        assertThat(codeErreur(() -> service.sAbonner(1L, "771234567", IP))).isEqualTo(400);
    }

    @Test
    void sAbonner_numeroInvalide_400() {
        when(campagnes.findById(1L)).thenReturn(Optional.of(campagne(1L, LocalDateTime.now().plusDays(2))));

        assertThat(codeErreur(() -> service.sAbonner(1L, "abc", IP))).isEqualTo(400);
        verify(abonnements, never()).save(any());
    }

    @Test
    void sAbonner_dejaInscrit_409() {
        when(campagnes.findById(1L)).thenReturn(Optional.of(campagne(1L, LocalDateTime.now().plusDays(2))));
        when(abonnements.existsByCampagneIdAndTelephone(1L, "+221771234567")).thenReturn(true);

        assertThat(codeErreur(() -> service.sAbonner(1L, "771234567", IP))).isEqualTo(409);
    }

    @Test
    void sAbonner_campagneInconnue_404() {
        when(campagnes.findById(9L)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.sAbonner(9L, "771234567", IP))).isEqualTo(404);
    }

    @Test
    void sAbonner_tropDeDemandesDepuisLaMemeIp_429() {
        when(campagnes.findById(1L)).thenReturn(Optional.of(campagne(1L, LocalDateTime.now().plusDays(2))));

        for (int i = 0; i < 5; i++) {
            service.sAbonner(1L, "771234567", IP);
        }

        assertThat(codeErreur(() -> service.sAbonner(1L, "771234567", IP))).isEqualTo(429);
    }

    // ---------- Admin ----------

    @Test
    void creer_dateFinAvantDebut_400() {
        LocalDateTime debut = LocalDateTime.now().plusDays(2);

        assertThat(codeErreur(() -> service.creer(requete(debut, debut.minusHours(1), "Escale", null, null))))
                .isEqualTo(400);
    }

    @Test
    void creer_avecCoordonnees_lesConserve() {
        LocalDateTime debut = LocalDateTime.now().plusDays(2);
        when(campagnes.save(any(Campagne.class))).thenAnswer(inv -> inv.getArgument(0));

        CampagneResponse res = service.creer(requete(debut, debut.plusHours(3), "Escale", 12.50, -16.20));

        assertThat(res.latitude()).isEqualTo(12.50);
        assertThat(res.longitude()).isEqualTo(-16.20);
    }

    @Test
    void creer_sansCoordonnees_utiliseCellesDuQuartier() {
        LocalDateTime debut = LocalDateTime.now().plusDays(2);
        when(campagnes.save(any(Campagne.class))).thenAnswer(inv -> inv.getArgument(0));

        CampagneResponse res = service.creer(requete(debut, debut.plusHours(3), "escale", null, null));

        assertThat(res.latitude()).isEqualTo(12.5845);
        assertThat(res.longitude()).isEqualTo(-16.2690);
    }

    @Test
    void creer_quartierInconnuSansCoordonnees_400() {
        LocalDateTime debut = LocalDateTime.now().plusDays(2);

        assertThat(codeErreur(() -> service.creer(requete(debut, debut.plusHours(3), "Atlantide", null, null))))
                .isEqualTo(400);
    }

    @Test
    void supprimer_effaceLesAbonnementsPuisLaCampagne() {
        Campagne c = campagne(1L, LocalDateTime.now().plusDays(2));
        when(campagnes.findById(1L)).thenReturn(Optional.of(c));

        service.supprimer(1L);

        verify(abonnements).deleteByCampagneId(1L);
        verify(campagnes).delete(c);
    }

    @Test
    void supprimer_campagneInconnue_404() {
        when(campagnes.findById(9L)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.supprimer(9L))).isEqualTo(404);
    }

    // ---------- Rappels ----------

    @Test
    void envoyerRappels_smsPartis_marqueLesAbonnementsCommeEnvoyes() {
        AbonnementRappel a = AbonnementRappel.builder().id(1L)
                .campagne(campagne(1L, LocalDateTime.now().plusHours(10)))
                .telephone("+221771234567").build();
        when(abonnements.aRappeler(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(a));
        when(smsService.envoyer(anyString(), anyString())).thenReturn(true);

        int envoyes = service.envoyerRappels();

        assertThat(envoyes).isEqualTo(1);
        assertThat(a.isRappelEnvoye()).isTrue();
        verify(abonnements).save(a);
    }

    @Test
    void envoyerRappels_smsEnEchec_neMarquePasLAbonnement() {
        AbonnementRappel a = AbonnementRappel.builder().id(1L)
                .campagne(campagne(1L, LocalDateTime.now().plusHours(10)))
                .telephone("+221771234567").build();
        when(abonnements.aRappeler(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(a));
        when(smsService.envoyer(anyString(), anyString())).thenReturn(false);

        int envoyes = service.envoyerRappels();

        assertThat(envoyes).isZero();
        assertThat(a.isRappelEnvoye()).isFalse();
        verify(abonnements, never()).save(any());
    }
}
