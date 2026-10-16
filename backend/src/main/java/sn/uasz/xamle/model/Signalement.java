package sn.uasz.xamle.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "signalements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Signalement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Symptome symptome;

    @Column(nullable = false)
    private String quartier;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutValidation statut = StatutValidation.EN_ATTENTE;

    private LocalDateTime dateCreation;

    @PrePersist
    void avantCreation() {
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
    }
}