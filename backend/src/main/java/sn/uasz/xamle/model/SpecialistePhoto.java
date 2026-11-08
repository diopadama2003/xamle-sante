package sn.uasz.xamle.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "specialiste_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpecialistePhoto {

    /** Même identifiant que le spécialiste : une seule photo par professionnel */
    @Id
    private Long specialisteId;

    @Column(nullable = false, length = 30)
    private String typeContenu;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] contenu;
}