package sn.uasz.xamle.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "campagnes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campagne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCampagne type;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String lieu;

    private String quartier;

    private double latitude;
    private double longitude;

    @Column(nullable = false)
    private LocalDateTime dateDebut;

    @Column(nullable = false)
    private LocalDateTime dateFin;
}