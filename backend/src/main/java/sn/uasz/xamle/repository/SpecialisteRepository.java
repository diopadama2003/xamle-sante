package sn.uasz.xamle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.StatutValidation;

import java.util.List;
import java.util.Optional;

public interface SpecialisteRepository extends JpaRepository<Specialiste, Long> {
    List<Specialiste> findByStatut(StatutValidation statut);
    Optional<Specialiste> findByUtilisateurEmail(String email);
}