package sn.uasz.xamle.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "etablissements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Etablissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeEtablissement type;

    private String adresse;
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

    @Column(length = 1000)
    private String services;

    /** true si l'établissement dispose d'un service d'urgence */
    private boolean urgence;
}