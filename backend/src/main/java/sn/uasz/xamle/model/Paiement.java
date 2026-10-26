package sn.uasz.xamle.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "paiements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rendez_vous_id")
    private RendezVous rendezVous;

    /** Montant en FCFA */
    private int montant;

    private String fournisseur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutPaiement statut = StatutPaiement.EN_ATTENTE;

    private LocalDateTime dateCreation;
    private LocalDateTime datePaiement;

    @PrePersist
    void avantCreation() {
        this.dateCreation = LocalDateTime.now();
    }
}