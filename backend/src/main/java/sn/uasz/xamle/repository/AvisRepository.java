package sn.uasz.xamle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.uasz.xamle.model.Avis;

import java.util.List;
import java.util.Optional;

public interface AvisRepository extends JpaRepository<Avis, Long> {

    List<Avis> findBySpecialisteIdOrderByDateCreationDesc(Long specialisteId);

    Optional<Avis> findBySpecialisteIdAndPatientEmail(Long specialisteId, String email);

    /** Note moyenne et nombre d'avis de chaque professionnel, en une seule requête */
    @Query("select a.specialiste.id as specialisteId, avg(a.note) as moyenne, count(a) as total "
            + "from Avis a group by a.specialiste.id")
    List<Statistique> statistiques();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Avis a where a.specialiste.id = :id")
    void supprimerDuSpecialiste(@Param("id") Long id);

    interface Statistique {
        Long getSpecialisteId();

        Double getMoyenne();

        Long getTotal();
    }
}