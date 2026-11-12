package sn.uasz.xamle.dto;

import sn.uasz.xamle.model.Avis;

import java.time.LocalDateTime;

/** L'auteur est abrégé (« Fatou S. ») : on ne publie jamais le nom complet d'un patient. */
public record AvisResponse(
        Long id,
        int note,
        String commentaire,
        String auteur,
        LocalDateTime date
) {
    public static AvisResponse from(Avis a) {
        String nom = a.getPatient().getNom();
        String initiale = (nom == null || nom.isBlank()) ? "" : " " + Character.toUpperCase(nom.trim().charAt(0)) + ".";
        LocalDateTime date = a.getDateModification() != null ? a.getDateModification() : a.getDateCreation();
        return new AvisResponse(a.getId(), a.getNote(), a.getCommentaire(),
                a.getPatient().getPrenom() + initiale, date);
    }
}