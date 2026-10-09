package sn.uasz.xamle.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SpecialisteRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotBlank String specialite,
        Long etablissementId,
        String quartier,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        String telephone,
        String horaires,
        String langues,
        String photoUrl,
        boolean disponible
) {}