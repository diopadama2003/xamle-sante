package sn.uasz.xamle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.EtablissementRequest;
import sn.uasz.xamle.dto.EtablissementResponse;
import sn.uasz.xamle.model.Etablissement;
import sn.uasz.xamle.model.TypeEtablissement;
import sn.uasz.xamle.repository.EtablissementRepository;
import sn.uasz.xamle.util.DistanceUtil;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EtablissementService {

    private final EtablissementRepository repo;

    public List<EtablissementResponse> rechercher(TypeEtablissement type, Boolean urgence,
                                                  Double lat, Double lng, Double rayonKm) {
        boolean geo = lat != null && lng != null;

        Comparator<EtablissementResponse> tri;
        if (geo) {
            tri = Comparator.comparingDouble(EtablissementResponse::distanceKm);
        } else {
            tri = Comparator.comparing(EtablissementResponse::nom);
        }

        return repo.findAll().stream()
                .filter(e -> type == null || e.getType() == type)
                .filter(e -> urgence == null || e.isUrgence() == urgence)
                .map(e -> EtablissementResponse.from(e,
                        geo ? DistanceUtil.km(lat, lng, e.getLatitude(), e.getLongitude()) : null))
                .filter(r -> !geo || rayonKm == null || r.distanceKm() <= rayonKm)
                .sorted(tri)
                .toList();
    }

    public List<EtablissementResponse> urgencesProches(double lat, double lng) {
        return rechercher(null, true, lat, lng, null);
    }

    public EtablissementResponse trouver(Long id) {
        return EtablissementResponse.from(chercher(id), null);
    }

    public EtablissementResponse creer(EtablissementRequest req) {
        Etablissement e = new Etablissement();
        appliquer(e, req);
        return EtablissementResponse.from(repo.save(e), null);
    }

    public EtablissementResponse modifier(Long id, EtablissementRequest req) {
        Etablissement e = chercher(id);
        appliquer(e, req);
        return EtablissementResponse.from(repo.save(e), null);
    }

    public void supprimer(Long id) {
        repo.delete(chercher(id));
    }

    private Etablissement chercher(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
    }

    private void appliquer(Etablissement e, EtablissementRequest req) {
        e.setNom(req.nom());
        e.setType(req.type());
        e.setAdresse(req.adresse());
        e.setQuartier(req.quartier());
        e.setLatitude(req.latitude());
        e.setLongitude(req.longitude());
        e.setTelephone(req.telephone());
        e.setHoraires(req.horaires());
        e.setServices(req.services());
        e.setUrgence(req.urgence());
    }
}