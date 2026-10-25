package sn.uasz.xamle.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "abonnements_rappel",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campagne_id", "telephone"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonnementRappel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campagne_id")
    private Campagne campagne;

    @Column(nullable = false)
    private String telephone;

    private boolean rappelEnvoye;

    private LocalDateTime dateCreation;

    @PrePersist
    void avantCreation() {
        this.dateCreation = LocalDateTime.now();
    }
}