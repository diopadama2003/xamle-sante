package sn.uasz.xamle.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.uasz.xamle.dto.RendezVousResponse;
import sn.uasz.xamle.model.Creneau;
import sn.uasz.xamle.model.Paiement;
import sn.uasz.xamle.model.RendezVous;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.StatutPaiement;
import sn.uasz.xamle.model.StatutRendezVous;
import sn.uasz.xamle.model.Utilisateur;
import sn.uasz.xamle.repository.PaiementRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RendezVousMapperTest {

    private static final String LIEN = "https://meet.jit.si/XamleSante-test";

    @Mock
    PaiementRepository paiements;
    @InjectMocks
    RendezVousMapper mapper;

    private RendezVous rendezVous(StatutRendezVous statut) {
        Specialiste s = Specialiste.builder().id(1L).nom("Diop").prenom("Awa").specialite("Pédiatre").build();
        LocalDateTime debut = LocalDateTime.now().plusDays(2);
        Creneau c = Creneau.builder().id(5L).specialiste(s).debut(debut).fin(debut.plusMinutes(30)).build();
        Utilisateur patient = Utilisateur.builder().id(2L).nom("Sow").prenom("Fatou").build();
        return RendezVous.builder().id(7L).creneau(c).patient(patient).motif("Toux")
                .statut(statut).lienVisio(LIEN).build();
    }

    @Test
    void versReponse_rendezVousConfirme_reveleLeLienVisio() {
        Paiement p = Paiement.builder().reference("WAVE-1").montant(1000).statut(StatutPaiement.PAYE).build();
        when(paiements.findFirstByRendezVousIdOrderByIdDesc(7L)).thenReturn(Optional.of(p));

        RendezVousResponse res = mapper.versReponse(rendezVous(StatutRendezVous.CONFIRME));

        assertThat(res.lienVisio()).isEqualTo(LIEN);
        assertThat(res.specialisteNom()).isEqualTo("Dr Awa Diop");
        assertThat(res.patientNom()).isEqualTo("Fatou Sow");
        assertThat(res.paiementReference()).isEqualTo("WAVE-1");
        assertThat(res.paiementStatut()).isEqualTo(StatutPaiement.PAYE);
        assertThat(res.montant()).isEqualTo(1000);
    }

    @Test
    void versReponse_rendezVousNonConfirme_masqueLeLienVisio() {
        Paiement p = Paiement.builder().reference("WAVE-1").montant(1000).build();
        when(paiements.findFirstByRendezVousIdOrderByIdDesc(7L)).thenReturn(Optional.of(p));

        RendezVousResponse res = mapper.versReponse(rendezVous(StatutRendezVous.EN_ATTENTE_PAIEMENT));

        assertThat(res.lienVisio()).isNull();
    }

    @Test
    void versReponse_sansPaiement_laisseLesChampsDePaiementVides() {
        when(paiements.findFirstByRendezVousIdOrderByIdDesc(7L)).thenReturn(Optional.empty());

        RendezVousResponse res = mapper.versReponse(rendezVous(StatutRendezVous.ANNULE));

        assertThat(res.paiementReference()).isNull();
        assertThat(res.paiementStatut()).isNull();
        assertThat(res.montant()).isZero();
    }
}
