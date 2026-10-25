package sn.uasz.xamle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.uasz.xamle.model.AbonnementRappel;

import java.time.LocalDateTime;
import java.util.List;

public interface AbonnementRappelRepository extends JpaRepository<AbonnementRappel, Long> {

    boolean existsByCampagneIdAndTelephone(Long campagneId, String telephone);

    @Query("select a from AbonnementRappel a join fetch a.campagne c "
            + "where a.rappelEnvoye = false and c.dateDebut > :maintenant and c.dateDebut <= :limite")
    List<AbonnementRappel> aRappeler(@Param("maintenant") LocalDateTime maintenant,
                                     @Param("limite") LocalDateTime limite);

    void deleteByCampagneId(Long campagneId);
}