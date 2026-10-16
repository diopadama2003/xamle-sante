package sn.uasz.xamle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import sn.uasz.xamle.model.Symptome;

public record SignalementRequest(
        @NotNull Symptome symptome,
        @NotBlank String quartier
) {}