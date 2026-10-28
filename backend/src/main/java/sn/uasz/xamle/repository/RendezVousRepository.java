package sn.uasz.xamle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.uasz.xamle.model.RendezVous;
import sn.uasz.xamle.model.StatutRendezVous;

import java.time.LocalDateTime;
import java.util.List;

public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {

    List<RendezVous> findByPatientEmailOrderByCreneauDebutDesc(String email);

    @Query("select r from RendezVous r "
            + "where r.creneau.specialiste.utilisateur.email = :email "
            + "order by r.creneau.debut desc")
    List<RendezVous> findDuProfessionnel(@Param("email") String email);

    List<RendezVous> findByStatutAndDateCreationBefore(StatutRendezVous statut, LocalDateTime date);

    boolean existsByCreneauId(Long creneauId);

    long countByStatut(StatutRendezVous statut);
}