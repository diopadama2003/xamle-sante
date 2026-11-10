package sn.uasz.xamle.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.uasz.xamle.dto.StatistiquesResponse;
import sn.uasz.xamle.model.Role;
import sn.uasz.xamle.model.Signalement;
import sn.uasz.xamle.model.StatutRendezVous;
import sn.uasz.xamle.model.StatutValidation;
import sn.uasz.xamle.model.Symptome;
import sn.uasz.xamle.repository.CampagneRepository;
import sn.uasz.xamle.repository.RendezVousRepository;
import sn.uasz.xamle.repository.SignalementRepository;
import sn.uasz.xamle.repository.SosAlerteRepository;
import sn.uasz.xamle.repository.SpecialisteRepository;
import sn.uasz.xamle.repository.UtilisateurRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatistiquesServiceTest {

    @Mock
    SignalementRepository signalements;
    @Mock
    UtilisateurRepository utilisateurs;
    @Mock
    SpecialisteRepository specialistes;
    @Mock
    RendezVousRepository rendezVous;
    @Mock
    SosAlerteRepository alertes;
    @Mock
    CampagneRepository campagnes;
    @Spy
    QuartierCatalogue catalogue = new QuartierCatalogue();
    @InjectMocks
    StatistiquesService service;

    private Signalement signalement(String quartier, Symptome symptome, LocalDateTime date) {
        return Signalement.builder().quartier(quartier).symptome(symptome)
                .statut(StatutValidation.VALIDE).dateCreation(date).build();
    }

    private List<Signalement> repetes(String quartier, Symptome symptome, int nombre) {
        List<Signalement> liste = new ArrayList<>();
        for (int i = 0; i < nombre; i++) {
            liste.add(signalement(quartier, symptome, LocalDateTime.now()));
        }
        return liste;
    }

    private void stubValides(List<Signalement> liste) {
        when(signalements.findByStatutAndDateCreationAfter(eq(StatutValidation.VALIDE), any(LocalDateTime.class)))
                .thenReturn(liste);
    }

    @Test
    void calculer_agregueParSymptomeEtParQuartier() {
        List<Signalement> liste = new ArrayList<>(repetes("Escale", Symptome.FIEVRE, 4));
        liste.addAll(repetes("Boucotte", Symptome.TOUX, 1));
        stubValides(liste);
        when(signalements.countByStatut(StatutValidation.EN_ATTENTE)).thenReturn(2L);

        StatistiquesResponse res = service.calculer(7);

        assertThat(res.jours()).isEqualTo(7);
        assertThat(res.signalementsValides()).isEqualTo(5);
        assertThat(res.signalementsEnAttente()).isEqualTo(2);
        assertThat(res.parSymptome().get(0).cle()).isEqualTo("FIEVRE");
        assertThat(res.parSymptome().get(0).total()).isEqualTo(4);
        assertThat(res.parQuartier()).extracting(c -> c.cle()).containsExactly("Escale", "Boucotte");
    }

    @Test
    void calculer_produitUnPointParJourMemeSansSignalement() {
        stubValides(List.of());

        StatistiquesResponse res = service.calculer(7);

        assertThat(res.parJour()).hasSize(7);
        assertThat(res.parJour()).allMatch(j -> j.total() == 0);
    }

    @Test
    void calculer_bornePeriodeEntre1Et90() {
        stubValides(List.of());

        assertThat(service.calculer(0).jours()).isEqualTo(1);
        assertThat(service.calculer(1000).jours()).isEqualTo(90);
    }

    @Test
    void calculer_zonesARisque_respecteLeSeuilEtLesNiveaux() {
        List<Signalement> liste = new ArrayList<>(repetes("Escale", Symptome.FIEVRE, 2));
        liste.addAll(repetes("Boucotte", Symptome.FIEVRE, 5));
        liste.addAll(repetes("Kandé", Symptome.FIEVRE, 10));
        stubValides(liste);

        StatistiquesResponse res = service.calculer(30);

        assertThat(res.zonesARisque()).extracting(z -> z.quartier()).containsExactly("Kandé", "Boucotte");
        assertThat(res.zonesARisque()).extracting(z -> z.niveau()).containsExactly("ELEVE", "MODERE");
    }

    @Test
    void calculer_remplitLesCompteursDeLaPlateforme() {
        stubValides(List.of());
        when(utilisateurs.countByRole(Role.PATIENT)).thenReturn(40L);
        when(utilisateurs.countByRole(Role.PROFESSIONNEL)).thenReturn(8L);
        when(specialistes.countByStatut(StatutValidation.VALIDE)).thenReturn(6L);
        when(specialistes.countByStatut(StatutValidation.EN_ATTENTE)).thenReturn(2L);
        when(rendezVous.countByStatut(StatutRendezVous.CONFIRME)).thenReturn(12L);
        when(rendezVous.countByStatut(StatutRendezVous.ANNULE)).thenReturn(3L);
        when(alertes.countByDateCreationAfter(any(LocalDateTime.class))).thenReturn(1L);

        StatistiquesResponse.Plateforme p = service.calculer(7).plateforme();

        assertThat(p.patients()).isEqualTo(40);
        assertThat(p.professionnels()).isEqualTo(8);
        assertThat(p.specialistesValides()).isEqualTo(6);
        assertThat(p.specialistesEnAttente()).isEqualTo(2);
        assertThat(p.rendezVousConfirmes()).isEqualTo(12);
        assertThat(p.rendezVousAnnules()).isEqualTo(3);
        assertThat(p.alertesSosSemaine()).isEqualTo(1);
        assertThat(p.campagnesAVenir()).isZero();
    }

    // ---------- Export CSV ----------

    @Test
    void exporterCsv_contientEnTeteBomEtLignesTriees() {
        LocalDateTime hier = LocalDateTime.now().minusDays(1);
        stubValides(List.of(
                signalement("Escale", Symptome.TOUX, LocalDateTime.now()),
                signalement("Kandé", Symptome.FIEVRE, hier)));

        String csv = service.exporterCsv(7);

        assertThat(csv).startsWith("﻿date;symptome;quartier\r\n");
        String[] lignes = csv.split("\r\n");
        assertThat(lignes).hasSize(3);
        assertThat(lignes[1]).endsWith(";FIEVRE;Kandé");
        assertThat(lignes[2]).endsWith(";TOUX;Escale");
    }

    @Test
    void exporterCsv_neContientAucuneHeure() {
        stubValides(List.of(signalement("Escale", Symptome.TOUX, LocalDateTime.of(2026, 3, 5, 14, 32, 10))));

        String csv = service.exporterCsv(90);

        assertThat(csv).contains("2026-03-05;TOUX;Escale");
        assertThat(csv).doesNotContain("14:32");
    }

    @Test
    void exporterCsv_neutraliseLesFormulesExcelEtProtegeLesPointsVirgules() {
        stubValides(List.of(
                signalement("=SOMME(A1)", Symptome.TOUX, LocalDateTime.now()),
                signalement("Rue;\"X\"", Symptome.FIEVRE, LocalDateTime.now().minusDays(1))));

        String csv = service.exporterCsv(7);

        assertThat(csv).contains(";'=SOMME(A1)");
        assertThat(csv).contains(";\"Rue;\"\"X\"\"\"");
    }

    @Test
    void exporterCsv_quartierNul_donneUneCelluleVide() {
        stubValides(List.of(signalement(null, Symptome.TOUX, LocalDateTime.now())));

        String csv = service.exporterCsv(7);

        assertThat(csv).contains(";TOUX;\r\n");
    }
}
