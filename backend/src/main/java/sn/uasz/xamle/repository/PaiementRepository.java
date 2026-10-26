package sn.uasz.xamle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.uasz.xamle.model.Paiement;

import java.util.Optional;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    Optional<Paiement> findByReference(String reference);

    Optional<Paiement> findFirstByRendezVousIdOrderByIdDesc(Long rendezVousId);
}