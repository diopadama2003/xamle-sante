package sn.uasz.xamle.config;

import lombok.RequiredArgsConstructor;
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

    @Bean
    CommandLineRunner initComptes() {
        return args -> {
            creerSiAbsent("admin@xamle.sn", "Admin", "Santé", "Admin@2026", Role.ADMIN_SANTE);
            creerSiAbsent("moderateur@xamle.sn", "Modérateur", "Médical", "Modo@2026", Role.MODERATEUR);
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