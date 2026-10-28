package sn.uasz.xamle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.uasz.xamle.model.Signalement;
import sn.uasz.xamle.model.StatutValidation;

import java.time.LocalDateTime;
import java.util.List;

public interface SignalementRepository extends JpaRepository<Signalement, Long> {
    List<Signalement> findByStatut(StatutValidation statut);
    List<Signalement> findByStatutAndDateCreationAfter(StatutValidation statut, LocalDateTime date);
    long countByStatut(StatutValidation statut);
}