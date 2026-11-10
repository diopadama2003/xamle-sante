package sn.uasz.xamle.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.RendezVousRequest;
import sn.uasz.xamle.model.Creneau;
import sn.uasz.xamle.model.Paiement;
import sn.uasz.xamle.model.RendezVous;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.StatutPaiement;
import sn.uasz.xamle.model.StatutRendezVous;
import sn.uasz.xamle.model.StatutValidation;
import sn.uasz.xamle.model.Utilisateur;
import sn.uasz.xamle.repository.CreneauRepository;
import sn.uasz.xamle.repository.PaiementRepository;
import sn.uasz.xamle.repository.RendezVousRepository;
import sn.uasz.xamle.repository.UtilisateurRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RendezVousServiceTest {

    private static final String PATIENT = "patient@test.sn";
    private static final String PRO = "pro@test.sn";

    @Mock
    RendezVousRepository rendezVous;
    @Mock
    CreneauRepository creneaux;
    @Mock
    PaiementRepository paiements;
    @Mock
    UtilisateurRepository utilisateurs;
    @Mock
    RendezVousMapper mapper;
    @InjectMocks
    RendezVousService service;

    private Utilisateur patient() {
        return Utilisateur.builder().id(2L).nom("Sow").prenom("Fatou").email(PATIENT).build();
    }

    private Specialiste specialiste(StatutValidation statut) {
        Utilisateur pro = Utilisateur.builder().id(3L).email(PRO).build();
        return Specialiste.builder().id(1L).nom("Diop").prenom("Awa").specialite("Pédiatre")
                .utilisateur(pro).statut(statut).build();
    }

    private Creneau creneau(LocalDateTime debut, boolean reserve, StatutValidation statut) {
        return Creneau.builder().id(5L).specialiste(specialiste(statut))
                .debut(debut).fin(debut.plusMinutes(30)).reserve(reserve).build();
    }

    private RendezVous rdv(Creneau c, StatutRendezVous statut) {
        return RendezVous.builder().id(7L).creneau(c).patient(patient()).statut(statut).build();
    }

    private int codeErreur(Runnable action) {
        try {
            action.run();
        } catch (ResponseStatusException e) {
            return e.getStatusCode().value();
        }
        return -1;
    }

    // ---------- Réservation ----------

    @Test
    void reserver_succes_bloqueLeCreneauEtCreeUnPaiement() {
        Creneau c = creneau(LocalDateTime.now().plusDays(2), false, StatutValidation.VALIDE);
        when(utilisateurs.findByEmail(PATIENT)).thenReturn(Optional.of(patient()));
        when(creneaux.findByIdForUpdate(5L)).thenReturn(Optional.of(c));
        when(rendezVous.save(any(RendezVous.class))).thenAnswer(inv -> inv.getArgument(0));

        service.reserver(PATIENT, new RendezVousRequest(5L, "Toux persistante"));

        assertThat(c.isReserve()).isTrue();
        ArgumentCaptor<Paiement> capture = ArgumentCaptor.forClass(Paiement.class);
        verify(paiements).save(capture.capture());
        assertThat(capture.getValue().getMontant()).isEqualTo(RendezVousService.FRAIS_DOSSIER);
        assertThat(capture.getValue().getReference()).startsWith("WAVE-");
        ArgumentCaptor<RendezVous> rdvCapture = ArgumentCaptor.forClass(RendezVous.class);
        verify(rendezVous).save(rdvCapture.capture());
        assertThat(rdvCapture.getValue().getLienVisio()).startsWith("https://meet.jit.si/XamleSante-");
        assertThat(rdvCapture.getValue().getMotif()).isEqualTo("Toux persistante");
    }

    @Test
    void reserver_utilisateurInconnu_401() {
        when(utilisateurs.findByEmail(PATIENT)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.reserver(PATIENT, new RendezVousRequest(5L, null)))).isEqualTo(401);
    }

    @Test
    void reserver_creneauInconnu_404() {
        when(utilisateurs.findByEmail(PATIENT)).thenReturn(Optional.of(patient()));
        when(creneaux.findByIdForUpdate(5L)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.reserver(PATIENT, new RendezVousRequest(5L, null)))).isEqualTo(404);
    }

    @Test
    void reserver_specialisteNonValide_404() {
        when(utilisateurs.findByEmail(PATIENT)).thenReturn(Optional.of(patient()));
        when(creneaux.findByIdForUpdate(5L)).thenReturn(
                Optional.of(creneau(LocalDateTime.now().plusDays(2), false, StatutValidation.EN_ATTENTE)));

        assertThat(codeErreur(() -> service.reserver(PATIENT, new RendezVousRequest(5L, null)))).isEqualTo(404);
    }

    @Test
    void reserver_creneauDejaReserve_409() {
        when(utilisateurs.findByEmail(PATIENT)).thenReturn(Optional.of(patient()));
        when(creneaux.findByIdForUpdate(5L)).thenReturn(
                Optional.of(creneau(LocalDateTime.now().plusDays(2), true, StatutValidation.VALIDE)));

        assertThat(codeErreur(() -> service.reserver(PATIENT, new RendezVousRequest(5L, null)))).isEqualTo(409);
    }

    @Test
    void reserver_creneauTropProche_400() {
        when(utilisateurs.findByEmail(PATIENT)).thenReturn(Optional.of(patient()));
        when(creneaux.findByIdForUpdate(5L)).thenReturn(
                Optional.of(creneau(LocalDateTime.now().plusMinutes(10), false, StatutValidation.VALIDE)));

        assertThat(codeErreur(() -> service.reserver(PATIENT, new RendezVousRequest(5L, null)))).isEqualTo(400);
        verify(rendezVous, never()).save(any());
    }

    // ---------- Consultation ----------

    @Test
    void mesRendezVous_retourneUneReponseParRendezVous() {
        Creneau c = creneau(LocalDateTime.now().plusDays(2), true, StatutValidation.VALIDE);
        when(rendezVous.findByPatientEmailOrderByCreneauDebutDesc(PATIENT))
                .thenReturn(List.of(rdv(c, StatutRendezVous.CONFIRME), rdv(c, StatutRendezVous.ANNULE)));

        assertThat(service.mesRendezVous(PATIENT)).hasSize(2);
        verify(mapper, times(2)).versReponse(any(RendezVous.class));
    }

    @Test
    void rendezVousDuProfessionnel_retourneSesRendezVous() {
        Creneau c = creneau(LocalDateTime.now().plusDays(2), true, StatutValidation.VALIDE);
        when(rendezVous.findDuProfessionnel(PRO)).thenReturn(List.of(rdv(c, StatutRendezVous.CONFIRME)));

        assertThat(service.rendezVousDuProfessionnel(PRO)).hasSize(1);
    }

    // ---------- Annulation ----------

    @Test
    void annulerParPatient_rendezVousPaye_rembourseEtLiberelCreneau() {
        Creneau c = creneau(LocalDateTime.now().plusDays(2), true, StatutValidation.VALIDE);
        RendezVous r = rdv(c, StatutRendezVous.CONFIRME);
        Paiement p = Paiement.builder().id(9L).statut(StatutPaiement.PAYE).build();
        when(rendezVous.findById(7L)).thenReturn(Optional.of(r));
        when(paiements.findFirstByRendezVousIdOrderByIdDesc(7L)).thenReturn(Optional.of(p));

        service.annulerParPatient(PATIENT, 7L);

        assertThat(p.getStatut()).isEqualTo(StatutPaiement.REMBOURSE);
        assertThat(r.getStatut()).isEqualTo(StatutRendezVous.ANNULE);
        assertThat(c.isReserve()).isFalse();
        verify(creneaux).save(c);
    }

    @Test
    void annulerParPatient_paiementEnAttente_leMarqueEchoue() {
        Creneau c = creneau(LocalDateTime.now().plusDays(2), true, StatutValidation.VALIDE);
        RendezVous r = rdv(c, StatutRendezVous.EN_ATTENTE_PAIEMENT);
        Paiement p = Paiement.builder().id(9L).statut(StatutPaiement.EN_ATTENTE).build();
        when(rendezVous.findById(7L)).thenReturn(Optional.of(r));
        when(paiements.findFirstByRendezVousIdOrderByIdDesc(7L)).thenReturn(Optional.of(p));

        service.annulerParPatient(PATIENT, 7L);

        assertThat(p.getStatut()).isEqualTo(StatutPaiement.ECHOUE);
    }

    @Test
    void annulerParPatient_rendezVousDUnAutre_404() {
        Creneau c = creneau(LocalDateTime.now().plusDays(2), true, StatutValidation.VALIDE);
        when(rendezVous.findById(7L)).thenReturn(Optional.of(rdv(c, StatutRendezVous.CONFIRME)));

        assertThat(codeErreur(() -> service.annulerParPatient("autre@test.sn", 7L))).isEqualTo(404);
    }

    @Test
    void annulerParPatient_dejaAnnule_409() {
        Creneau c = creneau(LocalDateTime.now().plusDays(2), false, StatutValidation.VALIDE);
        when(rendezVous.findById(7L)).thenReturn(Optional.of(rdv(c, StatutRendezVous.ANNULE)));

        assertThat(codeErreur(() -> service.annulerParPatient(PATIENT, 7L))).isEqualTo(409);
    }

    @Test
    void annulerParPatient_dejaPasse_400() {
        Creneau c = creneau(LocalDateTime.now().minusHours(2), true, StatutValidation.VALIDE);
        when(rendezVous.findById(7L)).thenReturn(Optional.of(rdv(c, StatutRendezVous.CONFIRME)));

        assertThat(codeErreur(() -> service.annulerParPatient(PATIENT, 7L))).isEqualTo(400);
    }

    @Test
    void annulerParProfessionnel_succes() {
        Creneau c = creneau(LocalDateTime.now().plusDays(2), true, StatutValidation.VALIDE);
        RendezVous r = rdv(c, StatutRendezVous.CONFIRME);
        when(rendezVous.findById(7L)).thenReturn(Optional.of(r));
        when(paiements.findFirstByRendezVousIdOrderByIdDesc(7L)).thenReturn(Optional.empty());

        service.annulerParProfessionnel(PRO, 7L);

        assertThat(r.getStatut()).isEqualTo(StatutRendezVous.ANNULE);
    }

    @Test
    void annulerParProfessionnel_autreProfessionnel_404() {
        Creneau c = creneau(LocalDateTime.now().plusDays(2), true, StatutValidation.VALIDE);
        when(rendezVous.findById(7L)).thenReturn(Optional.of(rdv(c, StatutRendezVous.CONFIRME)));

        assertThat(codeErreur(() -> service.annulerParProfessionnel("autre@test.sn", 7L))).isEqualTo(404);
    }

    @Test
    void annulerParProfessionnel_specialisteSansCompteUtilisateur_404() {
        Creneau c = creneau(LocalDateTime.now().plusDays(2), true, StatutValidation.VALIDE);
        c.getSpecialiste().setUtilisateur(null);
        when(rendezVous.findById(7L)).thenReturn(Optional.of(rdv(c, StatutRendezVous.CONFIRME)));

        assertThat(codeErreur(() -> service.annulerParProfessionnel(PRO, 7L))).isEqualTo(404);
    }

    // ---------- Expiration ----------

    @Test
    void expirerNonPayes_annuleLesRendezVousEtLibereLesCreneaux() {
        Creneau c = creneau(LocalDateTime.now().plusDays(2), true, StatutValidation.VALIDE);
        RendezVous r = rdv(c, StatutRendezVous.EN_ATTENTE_PAIEMENT);
        Paiement p = Paiement.builder().id(9L).statut(StatutPaiement.EN_ATTENTE).build();
        when(rendezVous.findByStatutAndDateCreationBefore(eq(StatutRendezVous.EN_ATTENTE_PAIEMENT),
                any(LocalDateTime.class))).thenReturn(List.of(r));
        when(paiements.findFirstByRendezVousIdOrderByIdDesc(7L)).thenReturn(Optional.of(p));

        int nb = service.expirerNonPayes();

        assertThat(nb).isEqualTo(1);
        assertThat(r.getStatut()).isEqualTo(StatutRendezVous.ANNULE);
        assertThat(p.getStatut()).isEqualTo(StatutPaiement.ECHOUE);
        assertThat(c.isReserve()).isFalse();
    }

    @Test
    void expirerNonPayes_rienAExpirer_retourneZero() {
        when(rendezVous.findByStatutAndDateCreationBefore(any(), any(LocalDateTime.class))).thenReturn(List.of());

        assertThat(service.expirerNonPayes()).isZero();
    }
}
