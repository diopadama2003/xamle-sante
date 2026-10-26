package sn.uasz.xamle.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.uasz.xamle.model.Creneau;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CreneauRepository extends JpaRepository<Creneau, Long> {

    List<Creneau> findBySpecialisteIdAndDebutAfterOrderByDebutAsc(Long specialisteId, LocalDateTime date);

    List<Creneau> findBySpecialisteIdAndReserveFalseAndDebutAfterOrderByDebutAsc(
            Long specialisteId, LocalDateTime date);

    /** Vrai si un créneau existant chevauche l'intervalle [debut, fin] */
    boolean existsBySpecialisteIdAndDebutLessThanAndFinGreaterThan(
            Long specialisteId, LocalDateTime fin, LocalDateTime debut);

    /** Verrou en écriture : deux patients ne peuvent pas réserver le même créneau en même temps */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Creneau c where c.id = :id")
    Optional<Creneau> findByIdForUpdate(@Param("id") Long id);
}