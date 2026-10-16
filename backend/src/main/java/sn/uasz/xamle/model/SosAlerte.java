package sn.uasz.xamle.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sos_alertes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SosAlerte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double latitude;
    private double longitude;

    /** Numéro du proche, masqué : seuls les derniers chiffres sont conservés */
    private String contactMasque;

    private boolean smsEnvoye;

    private LocalDateTime dateCreation;

    @PrePersist
    void avantCreation() {
        this.dateCreation = LocalDateTime.now();
    }
}