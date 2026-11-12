package sn.uasz.xamle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.uasz.xamle.model.Paiement;

import java.util.Optional;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    Optional<Paiement> findByReference(String reference);

    Optional<Paiement> findFirstByRendezVousIdOrderByIdDesc(Long rendezVousId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Paiement p where p.rendezVous.id in "
            + "(select r.id from RendezVous r where r.creneau.specialiste.id = :id)")
    void supprimerDuSpecialiste(@Param("id") Long id);
}