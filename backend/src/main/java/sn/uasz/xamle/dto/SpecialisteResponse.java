package sn.uasz.xamle.dto;

import sn.uasz.xamle.model.Etablissement;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.StatutValidation;

public record SpecialisteResponse(
        Long id,
        String nom,
        String prenom,
        String specialite,
        Long etablissementId,
        String etablissementNom,
        String quartier,
        String ville,
        double latitude,
        double longitude,
        String telephone,
        String horaires,
        String langues,
        String photoUrl,
        boolean disponible,
        StatutValidation statut,
        Double distanceKm
) {
    public static SpecialisteResponse from(Specialiste s, Double distanceKm) {
        Etablissement e = s.getEtablissement();
        return new SpecialisteResponse(
                s.getId(), s.getNom(), s.getPrenom(), s.getSpecialite(),
                e != null ? e.getId() : null,
                e != null ? e.getNom() : null,
                s.getQuartier(), s.getVille(), s.getLatitude(), s.getLongitude(),
                s.getTelephone(), s.getHoraires(), s.getLangues(), s.getPhotoUrl(),
                s.isDisponible(), s.getStatut(), distanceKm);
    }
}