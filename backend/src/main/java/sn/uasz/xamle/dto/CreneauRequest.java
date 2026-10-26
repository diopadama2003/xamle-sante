package sn.uasz.xamle.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreneauRequest(
        @NotNull LocalDateTime debut,
        @NotNull LocalDateTime fin
) {}