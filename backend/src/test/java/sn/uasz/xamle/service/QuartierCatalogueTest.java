package sn.uasz.xamle.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuartierCatalogueTest {

    private final QuartierCatalogue catalogue = new QuartierCatalogue();

    @Test
    void tous_contientLesQuartiersDeZiguinchor() {
        assertThat(catalogue.tous()).hasSize(12);
        assertThat(catalogue.tous()).extracting(QuartierCatalogue.Quartier::nom)
                .contains("Escale", "Boucotte", "Kandé");
    }

    @Test
    void trouver_ignoreLesAccentsLaCasseEtLesEspaces() {
        assertThat(catalogue.trouver("kande")).isPresent();
        assertThat(catalogue.trouver("kande").get().nom()).isEqualTo("Kandé");
        assertThat(catalogue.trouver("  ESCALE ")).isPresent();
    }

    @Test
    void trouver_quartierInconnu_estVide() {
        assertThat(catalogue.trouver("Atlantide")).isEmpty();
    }

    @Test
    void trouver_null_estVide() {
        assertThat(catalogue.trouver(null)).isEmpty();
    }
}
