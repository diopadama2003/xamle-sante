package sn.uasz.xamle.dto;

public record SuppressionResponse(
        String professionnel,
        int rendezVousAnnules,
        boolean compteBloque
) {}