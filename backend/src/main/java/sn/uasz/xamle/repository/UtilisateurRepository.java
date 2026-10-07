package sn.uasz.xamle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.uasz.xamle.model.Utilisateur;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByEmail(String email);
}