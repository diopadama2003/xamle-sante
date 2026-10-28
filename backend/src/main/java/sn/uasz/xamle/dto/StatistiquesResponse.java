package sn.uasz.xamle.dto;

import java.time.LocalDate;
import java.util.List;

public record StatistiquesResponse(
        int jours,
        long signalementsValides,
        long signalementsEnAttente,
        List<Comptage> parSymptome,
        List<Comptage> parQuartier,
        List<ZoneChaleurResponse> zonesARisque,
        List<JourCompte> parJour,
        Plateforme plateforme
) {
    public record Comptage(String cle, long total) {}

    public record JourCompte(LocalDate jour, long total) {}

    public record Plateforme(
            long patients,
            long professionnels,
            long specialistesValides,
            long specialistesEnAttente,
            long rendezVousConfirmes,
            long rendezVousAnnules,
            long alertesSosSemaine,
            long campagnesAVenir
    ) {}
}
