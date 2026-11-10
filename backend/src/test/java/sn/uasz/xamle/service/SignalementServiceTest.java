package sn.uasz.xamle.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.SignalementRequest;
import sn.uasz.xamle.dto.SignalementResponse;
import sn.uasz.xamle.dto.ZoneChaleurResponse;
import sn.uasz.xamle.model.Signalement;
import sn.uasz.xamle.model.StatutValidation;
import sn.uasz.xamle.model.Symptome;
import sn.uasz.xamle.repository.SignalementRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignalementServiceTest {

    private static final String IP = "10.0.0.2";

    @Mock
    SignalementRepository repo;
    @Spy
    QuartierCatalogue catalogue = new QuartierCatalogue();
    @InjectMocks
    SignalementService service;

    private List<Signalement> signalements(String quartier, Symptome symptome, int nombre) {
        List<Signalement> liste = new ArrayList<>();
        for (int i = 0; i < nombre; i++) {
            liste.add(Signalement.builder().id((long) i).quartier(quartier).symptome(symptome)
                    .statut(StatutValidation.VALIDE).dateCreation(LocalDateTime.now()).build());
        }
        return liste;
    }

    private int codeErreur(Runnable action) {
        try {
            action.run();
        } catch (ResponseStatusException e) {
            return e.getStatusCode().value();
        }
        return -1;
    }

    // ---------- Création ----------

    @Test
    void creer_enregistreLeNomOfficielDuQuartierSansIp() {
        service.creer(new SignalementRequest(Symptome.FIEVRE, "kande"), IP);

        ArgumentCaptor<Signalement> capture = ArgumentCaptor.forClass(Signalement.class);
        verify(repo).save(capture.capture());
        assertThat(capture.getValue().getQuartier()).isEqualTo("Kandé");
        assertThat(capture.getValue().getSymptome()).isEqualTo(Symptome.FIEVRE);
        assertThat(capture.getValue().getStatut()).isEqualTo(StatutValidation.EN_ATTENTE);
    }

    @Test
    void creer_quartierInconnu_400() {
        assertThat(codeErreur(() -> service.creer(new SignalementRequest(Symptome.TOUX, "Atlantide"), IP)))
                .isEqualTo(400);
        verify(repo, never()).save(any());
    }

    @Test
    void creer_tropDeSignalementsDepuisLaMemeIp_429() {
        for (int i = 0; i < 5; i++) {
            service.creer(new SignalementRequest(Symptome.TOUX, "Escale"), IP);
        }

        assertThat(codeErreur(() -> service.creer(new SignalementRequest(Symptome.TOUX, "Escale"), IP)))
                .isEqualTo(429);
    }

    @Test
    void creer_laLimiteEstParIp() {
        for (int i = 0; i < 5; i++) {
            service.creer(new SignalementRequest(Symptome.TOUX, "Escale"), IP);
        }

        service.creer(new SignalementRequest(Symptome.TOUX, "Escale"), "10.0.0.99");

        verify(repo, times(6)).save(any(Signalement.class));
    }

    // ---------- Carte de chaleur ----------

    @Test
    void chaleur_masqueLesQuartiersSousLeSeuilDAnonymat() {
        when(repo.findByStatutAndDateCreationAfter(eq(StatutValidation.VALIDE), any(LocalDateTime.class)))
                .thenReturn(signalements("Escale", Symptome.FIEVRE, SignalementService.SEUIL_ANONYMAT - 1));

        assertThat(service.chaleur(7, null)).isEmpty();
    }

    @Test
    void chaleur_affichePasLesQuartiersAuSeuil() {
        when(repo.findByStatutAndDateCreationAfter(eq(StatutValidation.VALIDE), any(LocalDateTime.class)))
                .thenReturn(signalements("Escale", Symptome.FIEVRE, SignalementService.SEUIL_ANONYMAT));

        List<ZoneChaleurResponse> zones = service.chaleur(7, null);

        assertThat(zones).hasSize(1);
        assertThat(zones.get(0).quartier()).isEqualTo("Escale");
        assertThat(zones.get(0).niveau()).isEqualTo("FAIBLE");
    }

    @Test
    void chaleur_niveauxModereEtEleve_etTriDecroissant() {
        List<Signalement> tous = new ArrayList<>(signalements("Escale", Symptome.FIEVRE, 5));
        tous.addAll(signalements("Boucotte", Symptome.FIEVRE, 12));
        when(repo.findByStatutAndDateCreationAfter(eq(StatutValidation.VALIDE), any(LocalDateTime.class)))
                .thenReturn(tous);

        List<ZoneChaleurResponse> zones = service.chaleur(7, null);

        assertThat(zones).extracting(ZoneChaleurResponse::quartier).containsExactly("Boucotte", "Escale");
        assertThat(zones).extracting(ZoneChaleurResponse::niveau).containsExactly("ELEVE", "MODERE");
    }

    @Test
    void chaleur_filtreParSymptome() {
        List<Signalement> tous = new ArrayList<>(signalements("Escale", Symptome.FIEVRE, 3));
        tous.addAll(signalements("Escale", Symptome.TOUX, 3));
        when(repo.findByStatutAndDateCreationAfter(eq(StatutValidation.VALIDE), any(LocalDateTime.class)))
                .thenReturn(tous);

        List<ZoneChaleurResponse> zones = service.chaleur(7, Symptome.TOUX);

        assertThat(zones).hasSize(1);
        assertThat(zones.get(0).total()).isEqualTo(3);
    }

    @Test
    void chaleur_ignoreUnQuartierAbsentDuCatalogue() {
        when(repo.findByStatutAndDateCreationAfter(eq(StatutValidation.VALIDE), any(LocalDateTime.class)))
                .thenReturn(signalements("Inconnu", Symptome.FIEVRE, 4));

        assertThat(service.chaleur(500, null)).isEmpty();
    }

    // ---------- Modération ----------

    @Test
    void enAttente_retourneLesSignalementsAModerer() {
        when(repo.findByStatut(StatutValidation.EN_ATTENTE))
                .thenReturn(signalements("Escale", Symptome.TOUX, 2));

        assertThat(service.enAttente()).hasSize(2);
    }

    @Test
    void valider_changeLeStatut() {
        Signalement s = Signalement.builder().id(1L).quartier("Escale").symptome(Symptome.TOUX).build();
        when(repo.findById(1L)).thenReturn(Optional.of(s));
        when(repo.save(s)).thenReturn(s);

        SignalementResponse res = service.valider(1L);

        assertThat(s.getStatut()).isEqualTo(StatutValidation.VALIDE);
        assertThat(res.quartier()).isEqualTo("Escale");
    }

    @Test
    void rejeter_changeLeStatut() {
        Signalement s = Signalement.builder().id(1L).quartier("Escale").symptome(Symptome.TOUX).build();
        when(repo.findById(1L)).thenReturn(Optional.of(s));
        when(repo.save(s)).thenReturn(s);

        service.rejeter(1L);

        assertThat(s.getStatut()).isEqualTo(StatutValidation.REJETE);
    }

    @Test
    void valider_signalementInconnu_404() {
        when(repo.findById(9L)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.valider(9L))).isEqualTo(404);
    }
}
