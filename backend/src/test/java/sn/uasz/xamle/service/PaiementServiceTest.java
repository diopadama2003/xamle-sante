package sn.uasz.xamle.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.PaiementResponse;
import sn.uasz.xamle.model.Creneau;
import sn.uasz.xamle.model.Paiement;
import sn.uasz.xamle.model.RendezVous;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.StatutPaiement;
import sn.uasz.xamle.model.StatutRendezVous;
import sn.uasz.xamle.model.Utilisateur;
import sn.uasz.xamle.repository.CreneauRepository;
import sn.uasz.xamle.repository.PaiementRepository;
import sn.uasz.xamle.repository.RendezVousRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaiementServiceTest {

    private static final String EMAIL = "patient@test.sn";
    private static final String REFERENCE = "WAVE-ABC123";

    @Mock
    PaiementRepository paiements;
    @Mock
    RendezVousRepository rendezVous;
    @Mock
    CreneauRepository creneaux;
    @Mock
    SmsService smsService;
    @InjectMocks
    PaiementService service;

    private Utilisateur patient(String telephone) {
        return Utilisateur.builder().id(2L).nom("Sow").prenom("Fatou").email(EMAIL)
                .telephone(telephone).build();
    }

    private Specialiste specialiste(Utilisateur pro) {
        return Specialiste.builder().id(1L).nom("Diop").prenom("Awa").specialite("Pédiatre")
                .utilisateur(pro).build();
    }

    private Paiement paiement(Utilisateur patient, Utilisateur pro) {
        LocalDateTime debut = LocalDateTime.now().plusDays(2);
        Creneau c = Creneau.builder().id(5L).specialiste(specialiste(pro))
                .debut(debut).fin(debut.plusMinutes(30)).reserve(true).build();
        RendezVous r = RendezVous.builder().id(7L).creneau(c).patient(patient)
                .lienVisio("https://meet.jit.si/XamleSante-test").build();
        return Paiement.builder().id(9L).reference(REFERENCE).rendezVous(r).montant(1000).build();
    }

    private int codeErreur(Runnable action) {
        try {
            action.run();
        } catch (ResponseStatusException e) {
            return e.getStatusCode().value();
        }
        return -1;
    }

    // ---------- Consultation ----------

    @Test
    void consulter_retourneLePaiementDuPatient() {
        Paiement p = paiement(patient("771234567"), null);
        when(paiements.findByReference(REFERENCE)).thenReturn(Optional.of(p));

        PaiementResponse res = service.consulter(EMAIL, REFERENCE);

        assertThat(res.reference()).isEqualTo(REFERENCE);
        assertThat(res.montant()).isEqualTo(1000);
        assertThat(res.specialisteNom()).isEqualTo("Dr Awa Diop");
        assertThat(res.statut()).isEqualTo(StatutPaiement.EN_ATTENTE);
    }

    @Test
    void consulter_paiementDUnAutrePatient_404() {
        when(paiements.findByReference(REFERENCE)).thenReturn(Optional.of(paiement(patient("771234567"), null)));

        assertThat(codeErreur(() -> service.consulter("autre@test.sn", REFERENCE))).isEqualTo(404);
    }

    @Test
    void consulter_referenceInconnue_404() {
        when(paiements.findByReference("XXX")).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.consulter(EMAIL, "XXX"))).isEqualTo(404);
    }

    // ---------- Confirmation ----------

    @Test
    void confirmer_succes_confirmeLeRendezVousEtEnvoieLesSms() {
        Utilisateur pro = Utilisateur.builder().id(3L).email("pro@test.sn").telephone("775555555").build();
        Paiement p = paiement(patient("771234567"), pro);
        when(paiements.findByReference(REFERENCE)).thenReturn(Optional.of(p));

        PaiementResponse res = service.confirmer(EMAIL, REFERENCE, true);

        assertThat(res.statut()).isEqualTo(StatutPaiement.PAYE);
        assertThat(p.getRendezVous().getStatut()).isEqualTo(StatutRendezVous.CONFIRME);
        assertThat(p.getDatePaiement()).isNotNull();
        verify(paiements).save(p);
        verify(rendezVous).save(p.getRendezVous());
        verify(smsService, times(2)).envoyer(anyString(), anyString());
    }

    @Test
    void confirmer_succes_sansNumeroValideAucunSmsNEstEnvoye() {
        Paiement p = paiement(patient("pas-un-numero"), null);
        when(paiements.findByReference(REFERENCE)).thenReturn(Optional.of(p));

        service.confirmer(EMAIL, REFERENCE, true);

        verify(smsService, never()).envoyer(anyString(), anyString());
    }

    @Test
    void confirmer_echec_annuleLeRendezVousEtLibereLeCreneau() {
        Paiement p = paiement(patient("771234567"), null);
        when(paiements.findByReference(REFERENCE)).thenReturn(Optional.of(p));

        PaiementResponse res = service.confirmer(EMAIL, REFERENCE, false);

        assertThat(res.statut()).isEqualTo(StatutPaiement.ECHOUE);
        assertThat(p.getRendezVous().getStatut()).isEqualTo(StatutRendezVous.ANNULE);
        assertThat(p.getRendezVous().getCreneau().isReserve()).isFalse();
        verify(creneaux).save(p.getRendezVous().getCreneau());
        verify(smsService, never()).envoyer(anyString(), anyString());
    }

    @Test
    void confirmer_paiementDejaTraite_409() {
        Paiement p = paiement(patient("771234567"), null);
        p.setStatut(StatutPaiement.PAYE);
        when(paiements.findByReference(REFERENCE)).thenReturn(Optional.of(p));

        assertThat(codeErreur(() -> service.confirmer(EMAIL, REFERENCE, true))).isEqualTo(409);
    }

    @Test
    void confirmer_rendezVousPlusEnAttenteDePaiement_409() {
        Paiement p = paiement(patient("771234567"), null);
        p.getRendezVous().setStatut(StatutRendezVous.ANNULE);
        when(paiements.findByReference(REFERENCE)).thenReturn(Optional.of(p));

        assertThat(codeErreur(() -> service.confirmer(EMAIL, REFERENCE, true))).isEqualTo(409);
        verify(paiements, never()).save(any());
    }

    @Test
    void confirmer_paiementDUnAutrePatient_404() {
        when(paiements.findByReference(REFERENCE)).thenReturn(Optional.of(paiement(patient("771234567"), null)));

        assertThat(codeErreur(() -> service.confirmer("autre@test.sn", REFERENCE, true))).isEqualTo(404);
    }
}
