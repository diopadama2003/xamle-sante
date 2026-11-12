package sn.uasz.xamle.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Avis d'un patient sur un professionnel : une note de 1 à 5 et un commentaire facultatif. */
@Entity
@Table(name = "avis", uniqueConstraints = @UniqueConstraint(columnNames = {"specialiste_id", "patient_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Avis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "specialiste_id")
    private Specialiste specialiste;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id")
    private Utilisateur patient;

    @Column(nullable = false)
    private int note;

    @Column(length = 500)
    private String commentaire;

    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;

    @PrePersist
    void avantCreation() {
        this.dateCreation = LocalDateTime.now();
    }

    @PreUpdate
    void avantModification() {
        this.dateModification = LocalDateTime.now();
    }
}