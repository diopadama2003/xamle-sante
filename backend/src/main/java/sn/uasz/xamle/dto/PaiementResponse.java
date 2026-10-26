package sn.uasz.xamle.dto;

import sn.uasz.xamle.model.StatutPaiement;

import java.time.LocalDateTime;

public record PaiementResponse(
        String reference,
        int montant,
        StatutPaiement statut,
        Long rendezVousId,
        String specialisteNom,
        LocalDateTime debut
) {}