package sn.uasz.xamle.dto;

/** Ce que le patient connecté peut faire sur un professionnel : noter (ou non) et son avis actuel. */
public record MonAvisResponse(
        boolean peutNoter,
        AvisResponse avis
) {}