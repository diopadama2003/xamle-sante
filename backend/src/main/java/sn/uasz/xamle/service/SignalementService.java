package sn.uasz.xamle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.SignalementRequest;
import sn.uasz.xamle.dto.SignalementResponse;
import sn.uasz.xamle.dto.ZoneChaleurResponse;
import sn.uasz.xamle.model.Signalement;
import sn.uasz.xamle.model.StatutValidation;
import sn.uasz.xamle.model.Symptome;
import sn.uasz.xamle.repository.SignalementRepository;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SignalementService {

    /** Un quartier n'apparaît sur la carte publique qu'à partir de ce nombre de signalements validés. */
    public static final int SEUIL_ANONYMAT = 3;

    private static final int MAX_PAR_FENETRE = 5;
    private static final long FENETRE_MS = 60 * 60 * 1000L;

    private final SignalementRepository repo;
    private final QuartierCatalogue catalogue;

    // L'IP sert uniquement à limiter les abus, en mémoire : elle n'est jamais enregistrée en base
    private final Map<String, Deque<Long>> historique = new ConcurrentHashMap<>();

    // ---------- Citoyen (public) ----------

    public void creer(SignalementRequest req, String ip) {
        verifierLimite(ip);
        QuartierCatalogue.Quartier q = catalogue.trouver(req.quartier()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quartier inconnu"));
        repo.save(Signalement.builder()
                .symptome(req.symptome())
                .quartier(q.nom())
                .build());
    }

    public List<ZoneChaleurResponse> chaleur(int jours, Symptome symptome) {
        int periode = Math.max(1, Math.min(jours, 90));
        LocalDateTime depuis = LocalDateTime.now().minusDays(periode);

        Map<String, Long> parQuartier = repo
                .findByStatutAndDateCreationAfter(StatutValidation.VALIDE, depuis).stream()
                .filter(s -> symptome == null || s.getSymptome() == symptome)
                .collect(Collectors.groupingBy(Signalement::getQuartier, Collectors.counting()));

        return parQuartier.entrySet().stream()
                .filter(e -> e.getValue() >= SEUIL_ANONYMAT)
                .map(e -> catalogue.trouver(e.getKey()).map(q -> new ZoneChaleurResponse(
                        q.nom(), q.latitude(), q.longitude(), e.getValue(), niveau(e.getValue()))))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingLong(ZoneChaleurResponse::total).reversed())
                .toList();
    }

    // ---------- Modération ----------

    public List<SignalementResponse> enAttente() {
        return repo.findByStatut(StatutValidation.EN_ATTENTE).stream()
                .map(SignalementResponse::from)
                .toList();
    }

    public SignalementResponse valider(Long id) {
        return changerStatut(id, StatutValidation.VALIDE);
    }

    public SignalementResponse rejeter(Long id) {
        return changerStatut(id, StatutValidation.REJETE);
    }

    // ---------- Méthodes internes ----------

    private SignalementResponse changerStatut(Long id, StatutValidation statut) {
        Signalement s = repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Signalement introuvable"));
        s.setStatut(statut);
        return SignalementResponse.from(repo.save(s));
    }

    private String niveau(long total) {
        if (total >= 10) return "ELEVE";
        if (total >= 5) return "MODERE";
        return "FAIBLE";
    }

    private void verifierLimite(String ip) {
        long maintenant = System.currentTimeMillis();
        Deque<Long> envois = historique.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (envois) {
            while (!envois.isEmpty() && maintenant - envois.peekFirst() > FENETRE_MS) {
                envois.pollFirst();
            }
            if (envois.size() >= MAX_PAR_FENETRE) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Trop de signalements, réessaie plus tard");
            }
            envois.addLast(maintenant);
        }
    }
}