package sn.uasz.xamle.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import sn.uasz.xamle.model.Role;
import sn.uasz.xamle.model.Utilisateur;
import sn.uasz.xamle.repository.UtilisateurRepository;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UtilisateurRepository repo;
    private final PasswordEncoder encoder;

    // Les identifiants viennent de la configuration : en production, ce sont des
    // variables d'environnement (aucun mot de passe n'est ecrit dans le code).
    @Value("${app.admin.email}")
    private String emailAdmin;

    @Value("${app.admin.password}")
    private String motDePasseAdmin;

    @Value("${app.moderateur.email}")
    private String emailModerateur;

    @Value("${app.moderateur.password}")
    private String motDePasseModerateur;

    @Bean
    CommandLineRunner initComptes() {
        return args -> {
            creerSiAbsent(emailAdmin, "Admin", "Santé", motDePasseAdmin, Role.ADMIN_SANTE);
            creerSiAbsent(emailModerateur, "Modérateur", "Médical", motDePasseModerateur, Role.MODERATEUR);
        };
    }

    private void creerSiAbsent(String email, String nom, String prenom, String mdp, Role role) {
        if (!repo.existsByEmail(email)) {
            repo.save(Utilisateur.builder()
                    .nom(nom).prenom(prenom).email(email)
                    .motDePasse(encoder.encode(mdp))
                    .role(role)
                    .build());
        }
    }
}
