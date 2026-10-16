package sn.uasz.xamle.dto;

import sn.uasz.xamle.model.Signalement;
import sn.uasz.xamle.model.Symptome;

import java.time.LocalDateTime;

public record SignalementResponse(
        Long id,
        Symptome symptome,
        String quartier,
        LocalDateTime dateCreation
) {
    public static SignalementResponse from(Signalement s) {
        return new SignalementResponse(s.getId(), s.getSymptome(), s.getQuartier(), s.getDateCreation());
    }
}