package sn.uasz.xamle.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sn.uasz.xamle.model.Signalement;
import sn.uasz.xamle.model.StatutValidation;
import sn.uasz.xamle.model.Symptome;
import sn.uasz.xamle.repository.SignalementRepository;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class DemoSignalementsInitializer {

    private final SignalementRepository repo;

    @Bean
    CommandLineRunner initSignalementsDemo() {
        return args -> {
            if (repo.count() > 0) {
                return;
            }
            // Données FICTIVES de démonstration
            for (int i = 0; i < 7; i++) ajouter(Symptome.FIEVRE, "Escale", StatutValidation.VALIDE, i % 5 + 1);
            for (int i = 0; i < 4; i++) ajouter(Symptome.DIARRHEE, "Boucotte", StatutValidation.VALIDE, i + 1);
            for (int i = 0; i < 3; i++) ajouter(Symptome.TOUX, "Lyndiane", StatutValidation.VALIDE, i + 2);
            // Seulement 2 : sous le seuil d'anonymat, ce quartier reste invisible
            for (int i = 0; i < 2; i++) ajouter(Symptome.FIEVRE, "Néma", StatutValidation.VALIDE, i + 1);
            // En attente de modération
            ajouter(Symptome.VOMISSEMENTS, "Santhiaba", StatutValidation.EN_ATTENTE, 0);
            ajouter(Symptome.FIEVRE, "Kénia", StatutValidation.EN_ATTENTE, 0);
        };
    }

    private void ajouter(Symptome symptome, String quartier, StatutValidation statut, int joursPasses) {
        repo.save(Signalement.builder()
                .symptome(symptome)
                .quartier(quartier)
                .statut(statut)
                .dateCreation(LocalDateTime.now().minusDays(joursPasses))
                .build());
    }
}