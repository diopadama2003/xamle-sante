package sn.uasz.xamle.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.EtablissementResponse;
import sn.uasz.xamle.dto.SosRequest;
import sn.uasz.xamle.dto.SosResponse;
import sn.uasz.xamle.model.SosAlerte;
import sn.uasz.xamle.model.TypeEtablissement;
import sn.uasz.xamle.repository.SosAlerteRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SosServiceTest {

    private static final String IP = "10.0.0.3";

    @Mock
    EtablissementService etablissementService;
    @Mock
    SmsService smsService;
    @Mock
    SosAlerteRepository alertes;
    @InjectMocks
    SosService service;

    private EtablissementResponse urgence(String nom, double km) {
        return new EtablissementResponse(1L, nom, TypeEtablissement.HOPITAL, "Rue 1", "Escale",
                "Ziguinchor", 12.58, -16.27, "339911111", "24h/24", "Urgences", true, km);
    }

    private SosRequest requete(String telephone) {
        return new SosRequest(12.5833, -16.2719, telephone);
    }

    private int codeErreur(Runnable action) {
        try {
            action.run();
        } catch (ResponseStatusException e) {
            return e.getStatusCode().value();
        }
        return -1;
    }

    @Test
    void declencher_envoieUnSmsAvecLaPositionEtLesDeuxUrgencesLesPlusProches() {
        when(etablissementService.urgencesProches(anyDouble(), anyDouble())).thenReturn(List.of(
                urgence("Hôpital Régional", 1.2), urgence("Clinique du Fleuve", 2.5), urgence("Poste de santé", 4.0)));
        when(smsService.envoyer(anyString(), anyString())).thenReturn(true);

        SosResponse res = service.declencher(requete("77 123 45 67"), IP);

        assertThat(res.smsEnvoye()).isTrue();
        assertThat(res.urgences()).hasSize(3);
        assertThat(res.message()).contains("https://maps.google.com/?q=12.583300,-16.271900");
        assertThat(res.message()).contains("Hôpital Régional (1.2 km)");
        assertThat(res.message()).contains("Clinique du Fleuve (2.5 km)");
        assertThat(res.message()).doesNotContain("Poste de santé");
        verify(smsService).envoyer(eq("+221771234567"), anyString());
    }

    @Test
    void declencher_enregistreLAlerteAvecLeNumeroMasque() {
        when(etablissementService.urgencesProches(anyDouble(), anyDouble())).thenReturn(List.of());
        when(smsService.envoyer(anyString(), anyString())).thenReturn(false);

        service.declencher(requete("771234567"), IP);

        ArgumentCaptor<SosAlerte> capture = ArgumentCaptor.forClass(SosAlerte.class);
        verify(alertes).save(capture.capture());
        assertThat(capture.getValue().getContactMasque()).isEqualTo("+221*****567");
        assertThat(capture.getValue().getContactMasque()).doesNotContain("771234567");
        assertThat(capture.getValue().isSmsEnvoye()).isFalse();
    }

    @Test
    void declencher_sansUrgenceProche_nAjouteAucuneListe() {
        when(etablissementService.urgencesProches(anyDouble(), anyDouble())).thenReturn(List.of());
        when(smsService.envoyer(anyString(), anyString())).thenReturn(true);

        SosResponse res = service.declencher(requete("771234567"), IP);

        assertThat(res.message()).doesNotContain("Urgences proches");
    }

    @Test
    void declencher_accepteLesFormatsInternationaux() {
        when(etablissementService.urgencesProches(anyDouble(), anyDouble())).thenReturn(List.of());
        when(smsService.envoyer(anyString(), anyString())).thenReturn(true);

        service.declencher(requete("00221771234567"), IP);
        service.declencher(requete("221771234567"), IP);
        service.declencher(requete("+221771234567"), IP);

        verify(smsService, times(3)).envoyer(eq("+221771234567"), anyString());
    }

    @Test
    void declencher_numeroInvalide_400() {
        assertThat(codeErreur(() -> service.declencher(requete("abc"), IP))).isEqualTo(400);
        verifyNoInteractions(smsService);
    }

    @Test
    void declencher_tropDeDemandesDepuisLaMemeIp_429() {
        when(etablissementService.urgencesProches(anyDouble(), anyDouble())).thenReturn(List.of());
        when(smsService.envoyer(anyString(), anyString())).thenReturn(true);
        for (int i = 0; i < 5; i++) {
            service.declencher(requete("771234567"), IP);
        }

        assertThat(codeErreur(() -> service.declencher(requete("771234567"), IP))).isEqualTo(429);
    }
}
