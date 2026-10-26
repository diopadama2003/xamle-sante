package sn.uasz.xamle.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rendez_vous")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RendezVous {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "creneau_id")
    private Creneau creneau;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id")
    private Utilisateur patient;

    @Column(length = 500)
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutRendezVous statut = StatutRendezVous.EN_ATTENTE_PAIEMENT;

    /** Salle de visioconférence : visible seulement quand le rendez-vous est confirmé */
    private String lienVisio;

    private LocalDateTime dateCreation;

    /** Verrou optimiste : évite qu'un paiement et une expiration modifient le même rendez-vous en même temps */
    @Version
    private Long version;

    @PrePersist
    void avantCreation() {
        this.dateCreation = LocalDateTime.now();
    }
}