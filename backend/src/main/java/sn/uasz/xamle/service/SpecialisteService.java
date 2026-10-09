package sn.uasz.xamle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.SpecialisteRequest;
import sn.uasz.xamle.dto.SpecialisteResponse;
import sn.uasz.xamle.model.Etablissement;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.StatutValidation;
import sn.uasz.xamle.model.Utilisateur;
import sn.uasz.xamle.repository.EtablissementRepository;
import sn.uasz.xamle.repository.SpecialisteRepository;
import sn.uasz.xamle.repository.UtilisateurRepository;
import sn.uasz.xamle.util.DistanceUtil;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialisteService {

    private final SpecialisteRepository repo;
    private final EtablissementRepository etablissementRepo;
    private final UtilisateurRepository utilisateurRepo;

    // ---------- Consultation publique (profils validés uniquement) ----------

    public List<SpecialisteResponse> rechercher(String specialite, String langue, String quartier,
                                                Boolean disponible, Double lat, Double lng,
                                                Double rayonKm) {
        boolean geo = lat != null && lng != null;

        Comparator<SpecialisteResponse> tri;
        if (geo) {
            tri = Comparator.comparingDouble(SpecialisteResponse::distanceKm);
        } else {
            tri = Comparator.comparing(SpecialisteResponse::nom);
        }

        return repo.findByStatut(StatutValidation.VALIDE).stream()
                .filter(s -> specialite == null || contient(s.getSpecialite(), specialite))
                .filter(s -> langue == null || contient(s.getLangues(), langue))
                .filter(s -> quartier == null || quartier.equalsIgnoreCase(s.getQuartier()))
                .filter(s -> disponible == null || s.isDisponible() == disponible)
                .map(s -> SpecialisteResponse.from(s,
                        geo ? DistanceUtil.km(lat, lng, s.getLatitude(), s.getLongitude()) : null))
                .filter(r -> !geo || rayonKm == null || r.distanceKm() <= rayonKm)
                .sorted(tri)
                .toList();
    }

    public SpecialisteResponse trouverValide(Long id) {
        Specialiste s = chercher(id);
        if (s.getStatut() != StatutValidation.VALIDE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Spécialiste introuvable");
        }
        return SpecialisteResponse.from(s, null);
    }

    // ---------- Espace du professionnel ----------

    public SpecialisteResponse creerProfil(String email, SpecialisteRequest req) {
        Utilisateur u = utilisateurRepo.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        if (repo.findByUtilisateurEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tu as déjà un profil");
        }
        Specialiste s = new Specialiste();
        s.setUtilisateur(u);
        s.setStatut(StatutValidation.EN_ATTENTE);
        appliquer(s, req);
        return SpecialisteResponse.from(repo.save(s), null);
    }

    public SpecialisteResponse monProfil(String email) {
        return SpecialisteResponse.from(profilDe(email), null);
    }

    public SpecialisteResponse modifierProfil(String email, SpecialisteRequest req) {
        Specialiste s = profilDe(email);
        appliquer(s, req);
        // Toute modification doit être revérifiée avant d'être publique
        s.setStatut(StatutValidation.EN_ATTENTE);
        return SpecialisteResponse.from(repo.save(s), null);
    }

    // ---------- Modération ----------

    public List<SpecialisteResponse> enAttente() {
        return repo.findByStatut(StatutValidation.EN_ATTENTE).stream()
                .map(s -> SpecialisteResponse.from(s, null))
                .toList();
    }

    public SpecialisteResponse valider(Long id) {
        return changerStatut(id, StatutValidation.VALIDE);
    }

    public SpecialisteResponse rejeter(Long id) {
        return changerStatut(id, StatutValidation.REJETE);
    }

    // ---------- Méthodes internes ----------

    private SpecialisteResponse changerStatut(Long id, StatutValidation statut) {
        Specialiste s = chercher(id);
        s.setStatut(statut);
        return SpecialisteResponse.from(repo.save(s), null);
    }

    private Specialiste chercher(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Spécialiste introuvable"));
    }

    private Specialiste profilDe(String email) {
        return repo.findByUtilisateurEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun profil pour ce compte"));
    }

    private boolean contient(String texte, String recherche) {
        return texte != null && texte.toLowerCase().contains(recherche.toLowerCase());
    }

    private void appliquer(Specialiste s, SpecialisteRequest req) {
        Etablissement etablissement = null;
        if (req.etablissementId() != null) {
            etablissement = etablissementRepo.findById(req.etablissementId()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Établissement inconnu"));
        }
        s.setNom(req.nom());
        s.setPrenom(req.prenom());
        s.setSpecialite(req.specialite());
        s.setEtablissement(etablissement);
        s.setQuartier(req.quartier());
        s.setLatitude(req.latitude());
        s.setLongitude(req.longitude());
        s.setTelephone(req.telephone());
        s.setHoraires(req.horaires());
        s.setLangues(req.langues());
        s.setPhotoUrl(req.photoUrl());
        s.setDisponible(req.disponible());
    }
}