package sn.uasz.xamle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.uasz.xamle.model.Etablissement;
import sn.uasz.xamle.model.TypeEtablissement;

import java.util.List;

public interface EtablissementRepository extends JpaRepository<Etablissement, Long> {
    List<Etablissement> findByType(TypeEtablissement type);
    List<Etablissement> findByUrgenceTrue();
}
