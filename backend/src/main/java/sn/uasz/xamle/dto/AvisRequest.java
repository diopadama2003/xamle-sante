package sn.uasz.xamle.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvisRequest(
        @NotNull @Min(1) @Max(5) Integer note,
        @Size(max = 500) String commentaire
) {}