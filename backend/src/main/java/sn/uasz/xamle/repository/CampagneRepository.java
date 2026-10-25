package sn.uasz.xamle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.uasz.xamle.model.Campagne;

import java.time.LocalDateTime;
import java.util.List;

public interface CampagneRepository extends JpaRepository<Campagne, Long> {
    List<Campagne> findByDateFinAfterOrderByDateDebutAsc(LocalDateTime date);
}