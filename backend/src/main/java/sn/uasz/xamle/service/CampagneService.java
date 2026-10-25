package sn.uasz.xamle.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.CampagneRequest;
import sn.uasz.xamle.dto.CampagneResponse;
import sn.uasz.xamle.model.AbonnementRappel;
import sn.uasz.xamle.model.Campagne;
import sn.uasz.xamle.repository.AbonnementRappelRepository;
import sn.uasz.xamle.repository.CampagneRepository;
import sn.uasz.xamle.util.TelephoneUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampagneService {

    private static final int MAX_PAR_FENETRE = 5;
    private static final long FENETRE_MS = 10 * 60 * 1000L;
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final CampagneRepository campagnes;
    private final AbonnementRappelRepository abonnements;
    private final QuartierCatalogue catalogue;
    private final SmsService smsService;

    // L'IP sert uniquement à limiter les abus, en mémoire : elle n'est jamais enregistrée
    private final Map<String, Deque<Long>> historique = new ConcurrentHashMap<>();

    // ---------- Public ----------

    public List<CampagneResponse> aVenir() {
        return campagnes.findByDateFinAfterOrderByDateDebutAsc(LocalDateTime.now()).stream()
                .map(CampagneResponse::from)
                .toList();
    }

    public void sAbonner(Long campagneId, String telephone, String ip) {
        verifierLimite(ip);
        Campagne c = chercher(campagneId);
        if (!c.getDateDebut().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cette campagne a déjà commencé");
        }
        String numero = TelephoneUtil.normaliser(telephone);
        if (numero == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numéro de téléphone invalide");
        }
        if (abonnements.existsByCampagneIdAndTelephone(campagneId, numero)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tu es déjà inscrit à ce rappel");
        }
        abonnements.save(AbonnementRappel.builder()
                .campagne(c)
                .telephone(numero)
                .build());
    }

    // ---------- Admin ----------

    public CampagneResponse creer(CampagneRequest req) {
        if (!req.dateFin().isAfter(req.dateDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de fin doit être après la date de début");
        }
        double lat;
        double lng;
        if (req.latitude() != null && req.longitude() != null) {
            lat = req.latitude();
            lng = req.longitude();
        } else {
            QuartierCatalogue.Quartier q = catalogue.trouver(req.quartier()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quartier inconnu"));
            lat = q.latitude();
            lng = q.longitude();
        }
        Campagne c = Campagne.builder()
                .titre(req.titre())
                .type(req.type())
                .description(req.description())
                .lieu(req.lieu())
                .quartier(req.quartier())
                .latitude(lat)
                .longitude(lng)
                .dateDebut(req.dateDebut())
                .dateFin(req.dateFin())
                .build();
        return CampagneResponse.from(campagnes.save(c));
    }

    @Transactional
    public void supprimer(Long id) {
        Campagne c = chercher(id);
        abonnements.deleteByCampagneId(id);
        campagnes.delete(c);
    }

    // ---------- Rappels (appelé par le planificateur et par le bouton admin) ----------

    public int envoyerRappels() {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime limite = maintenant.plusHours(24);
        int envoyes = 0;
        for (AbonnementRappel a : abonnements.aRappeler(maintenant, limite)) {
            Campagne c = a.getCampagne();
            String texte = "Rappel Xamlé Santé : " + c.getTitre() + " le "
                    + c.getDateDebut().format(FORMAT) + ", " + c.getLieu() + ".";
            if (smsService.envoyer(a.getTelephone(), texte)) {
                a.setRappelEnvoye(true);
                abonnements.save(a);
                envoyes++;
            } else {
                log.warn("Rappel non envoyé pour l'abonnement {}", a.getId());
            }
        }
        return envoyes;
    }

    // ---------- Méthodes internes ----------

    private Campagne chercher(Long id) {
        return campagnes.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Campagne introuvable"));
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
                        "Trop de demandes, réessaie dans quelques minutes");
            }
            envois.addLast(maintenant);
        }
    }
}