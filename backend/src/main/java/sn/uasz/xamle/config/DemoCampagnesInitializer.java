package sn.uasz.xamle.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sn.uasz.xamle.model.Campagne;
import sn.uasz.xamle.model.TypeCampagne;
import sn.uasz.xamle.repository.CampagneRepository;
import sn.uasz.xamle.service.QuartierCatalogue;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class DemoCampagnesInitializer {

    private final CampagneRepository repo;
    private final QuartierCatalogue catalogue;

    @Bean
    CommandLineRunner initCampagnesDemo() {
        return args -> {
            if (repo.count() > 0) {
                return;
            }
            LocalDateTime maintenant = LocalDateTime.now();
            // Campagnes FICTIVES de démonstration. La première commence dans 20 h :
            // elle permet de tester le rappel SMS 24 h avant.
            ajouter("Démo : vaccination contre la rougeole", TypeCampagne.VACCINATION,
                    "Vaccination gratuite des enfants de 9 mois à 5 ans.",
                    "Centre de santé de démonstration", "Boucotte",
                    maintenant.plusHours(20), 4);
            ajouter("Démo : dépistage du paludisme", TypeCampagne.DEPISTAGE,
                    "Tests rapides gratuits pour toute la famille.",
                    "Place publique", "Escale",
                    maintenant.plusDays(5), 6);
            ajouter("Démo : sensibilisation à l'hygiène des mains", TypeCampagne.SENSIBILISATION,
                    "Séance d'information et démonstration pour les écoles du quartier.",
                    "École de démonstration", "Lyndiane",
                    maintenant.plusDays(12), 3);
        };
    }

    private void ajouter(String titre, TypeCampagne type, String description,
                         String lieu, String quartier, LocalDateTime debut, int dureeHeures) {
        QuartierCatalogue.Quartier q = catalogue.trouver(quartier).orElseThrow();
        repo.save(Campagne.builder()
                .titre(titre).type(type).description(description)
                .lieu(lieu).quartier(q.nom())
                .latitude(q.latitude()).longitude(q.longitude())
                .dateDebut(debut).dateFin(debut.plusHours(dureeHeures))
                .build());
    }
}