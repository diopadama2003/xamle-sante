package sn.uasz.xamle.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "specialistes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Specialiste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private String specialite;

    @ManyToOne
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    /** Compte de connexion du professionnel (facultatif au départ) */
    @OneToOne
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    private String quartier;

    @Builder.Default
    private String ville = "Ziguinchor";

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    private String telephone;

    @Column(length = 1000)
    private String horaires;

    /** Langues séparées par des virgules, ex : "Français,Wolof,Diola" */
    private String langues;

    private String photoUrl;

    @Builder.Default
    private boolean disponible = true;

    /** Un profil reste EN_ATTENTE tant qu'il n'est pas validé */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutValidation statut = StatutValidation.EN_ATTENTE;

    private LocalDateTime dateCreation;

    @PrePersist
    void avantCreation() {
        this.dateCreation = LocalDateTime.now();
    }
}