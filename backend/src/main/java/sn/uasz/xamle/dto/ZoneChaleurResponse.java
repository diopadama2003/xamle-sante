package sn.uasz.xamle.dto;

public record ZoneChaleurResponse(
        String quartier,
        double latitude,
        double longitude,
        long total,
        String niveau
) {}