package sn.uasz.xamle.dto;

import sn.uasz.xamle.model.StatutPaiement;
import sn.uasz.xamle.model.StatutRendezVous;

import java.time.LocalDateTime;

public record RendezVousResponse(
        Long id,
        Long specialisteId,
        String specialisteNom,
        String specialite,
        String patientNom,
        LocalDateTime debut,
        LocalDateTime fin,
        String motif,
        StatutRendezVous statut,
        String lienVisio,
        String paiementReference,
        StatutPaiement paiementStatut,
        int montant
) {}
