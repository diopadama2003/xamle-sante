package sn.uasz.xamle.dto;

import sn.uasz.xamle.model.Creneau;

import java.time.LocalDateTime;

public record CreneauResponse(
        Long id,
        LocalDateTime debut,
        LocalDateTime fin,
        boolean reserve
) {
    public static CreneauResponse from(Creneau c) {
        return new CreneauResponse(c.getId(), c.getDebut(), c.getFin(), c.isReserve());
    }
}
