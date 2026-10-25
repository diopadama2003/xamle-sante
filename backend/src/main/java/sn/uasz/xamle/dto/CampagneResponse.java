package sn.uasz.xamle.dto;

import sn.uasz.xamle.model.Campagne;
import sn.uasz.xamle.model.TypeCampagne;

import java.time.LocalDateTime;

public record CampagneResponse(
        Long id,
        String titre,
        TypeCampagne type,
        String description,
        String lieu,
        String quartier,
        double latitude,
        double longitude,
        LocalDateTime dateDebut,
        LocalDateTime dateFin
) {
    public static CampagneResponse from(Campagne c) {
        return new CampagneResponse(c.getId(), c.getTitre(), c.getType(), c.getDescription(),
                c.getLieu(), c.getQuartier(), c.getLatitude(), c.getLongitude(),
                c.getDateDebut(), c.getDateFin());
    }
}