package sn.uasz.xamle.dto;

import sn.uasz.xamle.model.Etablissement;
import sn.uasz.xamle.model.TypeEtablissement;

public record EtablissementResponse(
        Long id,
        String nom,
        TypeEtablissement type,
        String adresse,
        String quartier,
        String ville,
        double latitude,
        double longitude,
        String telephone,
        String horaires,
        String services,
        boolean urgence,
        Double distanceKm
) {
    public static EtablissementResponse from(Etablissement e, Double distanceKm) {
        return new EtablissementResponse(
                e.getId(), e.getNom(), e.getType(), e.getAdresse(), e.getQuartier(),
                e.getVille(), e.getLatitude(), e.getLongitude(), e.getTelephone(),
                e.getHoraires(), e.getServices(), e.isUrgence(), distanceKm);
    }
}