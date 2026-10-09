package sn.uasz.xamle.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sn.uasz.xamle.model.Etablissement;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.StatutValidation;
import sn.uasz.xamle.model.TypeEtablissement;
import sn.uasz.xamle.repository.EtablissementRepository;
import sn.uasz.xamle.repository.SpecialisteRepository;

@Configuration
@RequiredArgsConstructor
public class DonneesDemoInitializer {

    private final EtablissementRepository etablissements;
    private final SpecialisteRepository specialistes;

    @Bean
    CommandLineRunner initDonneesDemo() {
        return args -> {
            if (etablissements.count() > 0) {
                return;
            }

            // ATTENTION : coordonnées APPROXIMATIVES, à vérifier sur Google Maps
            // (clic droit sur le lieu → copier les coordonnées) puis à corriger ici.
            Etablissement hopital = etablissements.save(Etablissement.builder()
                    .nom("Hôpital régional de Ziguinchor")
                    .type(TypeEtablissement.HOPITAL)
                    .quartier("Centre-ville")
                    .latitude(12.5745).longitude(-16.2740)
                    .horaires("24h/24")
                    .services("Urgences, Médecine générale, Chirurgie, Maternité")
                    .urgence(true)
                    .build());

            Etablissement silence = etablissements.save(Etablissement.builder()
                    .nom("Hôpital Silence")
                    .type(TypeEtablissement.HOPITAL)
                    .quartier("Silence")
                    .latitude(12.5887).longitude(-16.2620)
                    .horaires("24h/24")
                    .services("Urgences, Consultations")
                    .urgence(true)
                    .build());

            Etablissement pharmacie = etablissements.save(Etablissement.builder()
                    .nom("Pharmacie de démonstration")
                    .type(TypeEtablissement.PHARMACIE)
                    .quartier("Centre-ville")
                    .latitude(12.5820).longitude(-16.2700)
                    .horaires("08h00 - 20h00")
                    .urgence(false)
                    .build());

            etablissements.save(Etablissement.builder()
                    .nom("Centre de santé de démonstration")
                    .type(TypeEtablissement.DISPENSAIRE)
                    .quartier("Boucotte")
                    .latitude(12.5900).longitude(-16.2800)
                    .horaires("08h00 - 17h00")
                    .urgence(false)
                    .build());

            // Spécialistes FICTIFS (noms inventés pour la démo)
            specialistes.save(Specialiste.builder()
                    .nom("Démo").prenom("Awa").specialite("Pédiatre")
                    .etablissement(hopital).quartier("Centre-ville")
                    .latitude(12.5746).longitude(-16.2739)
                    .telephone("770000001").horaires("Lun-Ven 08h-16h")
                    .langues("Français,Wolof,Diola")
                    .statut(StatutValidation.VALIDE).build());

            specialistes.save(Specialiste.builder()
                    .nom("Démo").prenom("Moussa").specialite("Cardiologue")
                    .etablissement(silence).quartier("Silence")
                    .latitude(12.5888).longitude(-16.2621)
                    .telephone("770000002").horaires("Mar-Jeu 09h-15h")
                    .langues("Français,Wolof")
                    .statut(StatutValidation.VALIDE).build());

            specialistes.save(Specialiste.builder()
                    .nom("Démo").prenom("Fatou").specialite("Gynécologue")
                    .etablissement(hopital).quartier("Centre-ville")
                    .latitude(12.5744).longitude(-16.2741)
                    .telephone("770000003").horaires("Lun-Sam 08h-14h")
                    .langues("Français,Mandingue")
                    .statut(StatutValidation.VALIDE).build());

            // Un profil EN_ATTENTE pour tester le workflow de validation
            specialistes.save(Specialiste.builder()
                    .nom("Démo").prenom("Ibrahima").specialite("Dentiste")
                    .etablissement(pharmacie).quartier("Centre-ville")
                    .latitude(12.5819).longitude(-16.2701)
                    .telephone("770000004").langues("Français,Wolof")
                    .statut(StatutValidation.EN_ATTENTE).build());
        };
    }
}