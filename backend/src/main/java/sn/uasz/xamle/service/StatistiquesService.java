package sn.uasz.xamle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.xamle.dto.StatistiquesResponse;
import sn.uasz.xamle.dto.StatistiquesResponse.Comptage;
import sn.uasz.xamle.dto.StatistiquesResponse.JourCompte;
import sn.uasz.xamle.dto.StatistiquesResponse.Plateforme;
import sn.uasz.xamle.dto.ZoneChaleurResponse;
import sn.uasz.xamle.model.Role;
import sn.uasz.xamle.model.Signalement;
import sn.uasz.xamle.model.StatutRendezVous;
import sn.uasz.xamle.model.StatutValidation;
import sn.uasz.xamle.repository.CampagneRepository;
import sn.uasz.xamle.repository.RendezVousRepository;
import sn.uasz.xamle.repository.SignalementRepository;
import sn.uasz.xamle.repository.SosAlerteRepository;
import sn.uasz.xamle.repository.SpecialisteRepository;
import sn.uasz.xamle.repository.UtilisateurRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatistiquesService {

    private final SignalementRepository signalements;
    private final UtilisateurRepository utilisateurs;
    private final SpecialisteRepository specialistes;
    private final RendezVousRepository rendezVous;
    private final SosAlerteRepository alertes;
    private final CampagneRepository campagnes;
    private final QuartierCatalogue catalogue;

    @Transactional(readOnly = true)
    public StatistiquesResponse calculer(int jours) {
        int periode = borner(jours);
        List<Signalement> valides = valides(periode);

        Map<String, Long> parSymptome = valides.stream()
                .collect(Collectors.groupingBy(s -> s.getSymptome().name(), Collectors.counting()));
        Map<String, Long> parQuartier = valides.stream()
                .collect(Collectors.groupingBy(Signalement::getQuartier, Collectors.counting()));
        Map<LocalDate, Long> parDate = valides.stream()
                .collect(Collectors.groupingBy(s -> s.getDateCreation().toLocalDate(),
                        Collectors.counting()));

        // Un point par jour, y compris les jours sans signalement
        List<JourCompte> parJour = new ArrayList<>();
        LocalDate aujourdHui = LocalDate.now();
        for (int i = periode - 1; i >= 0; i--) {
            LocalDate d = aujourdHui.minusDays(i);
            parJour.add(new JourCompte(d, parDate.getOrDefault(d, 0L)));
        }

        List<ZoneChaleurResponse> zones = parQuartier.entrySet().stream()
                .filter(e -> e.getValue() >= SignalementService.SEUIL_ANONYMAT)
                .map(e -> catalogue.trouver(e.getKey()).map(q -> new ZoneChaleurResponse(
                        q.nom(), q.latitude(), q.longitude(), e.getValue(), niveau(e.getValue()))))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingLong(ZoneChaleurResponse::total).reversed())
                .toList();

        Plateforme plateforme = new Plateforme(
                utilisateurs.countByRole(Role.PATIENT),
                utilisateurs.countByRole(Role.PROFESSIONNEL),
                specialistes.countByStatut(StatutValidation.VALIDE),
                specialistes.countByStatut(StatutValidation.EN_ATTENTE),
                rendezVous.countByStatut(StatutRendezVous.CONFIRME),
                rendezVous.countByStatut(StatutRendezVous.ANNULE),
                alertes.countByDateCreationAfter(LocalDateTime.now().minusDays(7)),
                campagnes.findByDateFinAfterOrderByDateDebutAsc(LocalDateTime.now()).size());

        return new StatistiquesResponse(
                periode,
                valides.size(),
                signalements.countByStatut(StatutValidation.EN_ATTENTE),
                trier(parSymptome),
                trier(parQuartier),
                zones,
                parJour,
                plateforme);
    }

    /** Export CSV : une ligne par signalement validé, sans identité, sans heure, sans position précise. */
    @Transactional(readOnly = true)
    public String exporterCsv(int jours) {
        int periode = borner(jours);
        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM : Excel affiche correctement les accents
        sb.append("date;symptome;quartier\r\n");
        valides(periode).stream()
                .sorted(Comparator.comparing(Signalement::getDateCreation))
                .forEach(s -> sb
                        .append(s.getDateCreation().toLocalDate()).append(';')
                        .append(csv(s.getSymptome().name())).append(';')
                        .append(csv(s.getQuartier())).append("\r\n"));
        return sb.toString();
    }

    // ---------- Méthodes internes ----------

    private List<Signalement> valides(int periode) {
        LocalDateTime debut = LocalDate.now().minusDays(periode - 1L).atStartOfDay().minusSeconds(1);
        return signalements.findByStatutAndDateCreationAfter(StatutValidation.VALIDE, debut);
    }

    private int borner(int jours) {
        return Math.max(1, Math.min(jours, 90));
    }

    private List<Comptage> trier(Map<String, Long> m) {
        return m.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new Comptage(e.getKey(), e.getValue()))
                .toList();
    }

    private String niveau(long total) {
        if (total >= 10) return "ELEVE";
        if (total >= 5) return "MODERE";
        return "FAIBLE";
    }

    private String csv(String valeur) {
        if (valeur == null) {
            return "";
        }
        String s = valeur;
        // Protection contre l'injection de formules dans Excel
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}