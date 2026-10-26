package sn.uasz.xamle.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RendezVousScheduler {

    private final RendezVousService service;

    /** Toutes les 5 minutes : annule les rendez-vous non payés après 15 minutes. */
    @Scheduled(initialDelay = 120_000, fixedDelay = 300_000)
    public void expirer() {
        int n = service.expirerNonPayes();
        if (n > 0) {
            log.info("{} rendez-vous non payé(s) annulé(s)", n);
        }
    }
}