package sn.uasz.xamle.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RendezVousRequest(
        @NotNull Long creneauId,
        @Size(max = 500) String motif
) {}