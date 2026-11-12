package sn.uasz.xamle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /** Rendez-vous à venir d'un professionnel (sauf les annulés) */
    List<RendezVous> findByCreneauSpecialisteIdAndCreneauDebutAfterAndStatutNot(
            Long specialisteId, LocalDateTime apres, StatutRendezVous statut);

    /** Vrai si le patient a un rendez-vous de ce statut avec ce professionnel */
    boolean existsByPatientEmailAndCreneauSpecialisteIdAndStatut(
            String email, Long specialisteId, StatutRendezVous statut);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RendezVous r where r.creneau.id in "
            + "(select c.id from Creneau c where c.specialiste.id = :id)")
    void supprimerDuSpecialiste(@Param("id") Long id);
}