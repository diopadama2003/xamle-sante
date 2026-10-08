package sn.uasz.xamle.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.uasz.xamle.model.Role;

public record RegisterRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "8 caractères minimum") String motDePasse,
        String telephone,
        @NotNull Role role
) {}