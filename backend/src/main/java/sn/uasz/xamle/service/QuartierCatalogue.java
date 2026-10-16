package sn.uasz.xamle.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;

@Component
public class QuartierCatalogue {

    public record Quartier(String nom, double latitude, double longitude) {}

    // ATTENTION : coordonnées APPROXIMATIVES des centres de quartiers.
    // À vérifier sur Google Maps (clic droit sur le quartier → copier les coordonnées)
    // et à corriger ici : un point doit tomber sur la terre ferme, pas dans le fleuve.
    private static final List<Quartier> QUARTIERS = List.of(
            new Quartier("Escale", 12.5845, -16.2690),
            new Quartier("Santhiaba", 12.5790, -16.2760),
            new Quartier("Boucotte", 12.5900, -16.2800),
            new Quartier("Lyndiane", 12.5660, -16.2800),
            new Quartier("Kandé", 12.5800, -16.2650),
            new Quartier("Néma", 12.5700, -16.2620),
            new Quartier("Belfort", 12.5750, -16.2680),
            new Quartier("Boudody", 12.5770, -16.2850),
            new Quartier("Kénia", 12.5600, -16.2700),
            new Quartier("Silence", 12.5887, -16.2620),
            new Quartier("Djibock", 12.5750, -16.2500),
            new Quartier("Goumel", 12.5900, -16.2550)
    );

    public List<Quartier> tous() {
        return QUARTIERS;
    }

    public Optional<Quartier> trouver(String nom) {
        if (nom == null) {
            return Optional.empty();
        }
        String cible = normaliser(nom);
        return QUARTIERS.stream()
                .filter(q -> normaliser(q.nom()).equals(cible))
                .findFirst();
    }

    private static String normaliser(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }
}