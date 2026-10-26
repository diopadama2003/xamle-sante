package sn.uasz.xamle.dto;

import jakarta.validation.constraints.NotNull;

/** Simule la réponse de Wave : succes = true (paiement accepté) ou false (échec) */
public record PaiementConfirmation(@NotNull Boolean succes) {}