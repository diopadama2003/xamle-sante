package sn.uasz.xamle.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import sn.uasz.xamle.model.TypeEtablissement;

public record EtablissementRequest(
        @NotBlank String nom,
        @NotNull TypeEtablissement type,
        String adresse,
        String quartier,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        String telephone,
        String horaires,
        String services,
        boolean urgence
) {}