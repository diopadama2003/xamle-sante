package sn.uasz.xamle.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RappelScheduler {

    private final CampagneService service;

    /** Toutes les 15 minutes, 1 minute après le démarrage. */
    @Scheduled(initialDelay = 60_000, fixedDelay = 900_000)
    public void executer() {
        int n = service.envoyerRappels();
        if (n > 0) {
            log.info("{} rappel(s) de campagne envoyé(s)", n);
        }
    }
}