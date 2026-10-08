package sn.uasz.xamle.dto;

import sn.uasz.xamle.model.Role;

public record AuthResponse(
        String token,
        String email,
        String nom,
        String prenom,
        Role role
) {}