package sn.uasz.xamle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.uasz.xamle.model.SosAlerte;

import java.time.LocalDateTime;

public interface SosAlerteRepository extends JpaRepository<SosAlerte, Long> {
    long countByDateCreationAfter(LocalDateTime date);
}