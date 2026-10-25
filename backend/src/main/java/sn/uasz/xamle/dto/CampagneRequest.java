package sn.uasz.xamle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.uasz.xamle.model.TypeCampagne;

import java.time.LocalDateTime;

public record CampagneRequest(
        @NotBlank String titre,
        @NotNull TypeCampagne type,
        @Size(max = 1000) String description,
        @NotBlank String lieu,
        @NotBlank String quartier,
        Double latitude,
        Double longitude,
        @NotNull LocalDateTime dateDebut,
        @NotNull LocalDateTime dateFin
) {}