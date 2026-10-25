package sn.uasz.xamle.dto;

import jakarta.validation.constraints.NotBlank;

public record RappelRequest(@NotBlank String telephone) {}