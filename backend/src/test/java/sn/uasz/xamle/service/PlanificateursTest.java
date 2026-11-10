package sn.uasz.xamle.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanificateursTest {

    @Mock
    CampagneService campagneService;
    @Mock
    RendezVousService rendezVousService;

    @Test
    void rappelScheduler_appelleLEnvoiDesRappels() {
        when(campagneService.envoyerRappels()).thenReturn(2);

        new RappelScheduler(campagneService).executer();

        verify(campagneService).envoyerRappels();
    }

    @Test
    void rappelScheduler_aucunRappelAEnvoyer() {
        when(campagneService.envoyerRappels()).thenReturn(0);

        new RappelScheduler(campagneService).executer();

        verify(campagneService).envoyerRappels();
    }

    @Test
    void rendezVousScheduler_expireLesRendezVousNonPayes() {
        when(rendezVousService.expirerNonPayes()).thenReturn(3);

        new RendezVousScheduler(rendezVousService).expirer();

        verify(rendezVousService).expirerNonPayes();
    }

    @Test
    void rendezVousScheduler_rienAExpirer() {
        when(rendezVousService.expirerNonPayes()).thenReturn(0);

        new RendezVousScheduler(rendezVousService).expirer();

        verify(rendezVousService).expirerNonPayes();
    }
}
