package sn.uasz.xamle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.RendezVousRequest;
import sn.uasz.xamle.dto.RendezVousResponse;
import sn.uasz.xamle.model.Creneau;
import sn.uasz.xamle.model.Paiement;
import sn.uasz.xamle.model.RendezVous;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.StatutPaiement;
import sn.uasz.xamle.model.StatutRendezVous;
import sn.uasz.xamle.model.StatutValidation;
import sn.uasz.xamle.model.Utilisateur;
import sn.uasz.xamle.repository.CreneauRepository;
import sn.uasz.xamle.repository.PaiementRepository;
import sn.uasz.xamle.repository.RendezVousRepository;
import sn.uasz.xamle.repository.UtilisateurRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RendezVousService {

    /** Frais de dossier symboliques, en FCFA */
    public static final int FRAIS_DOSSIER = 1000;

    private static final int DELAI_PAIEMENT_MIN = 15;

    private final RendezVousRepository rendezVous;
    private final CreneauRepository creneaux;
    private final PaiementRepository paiements;
    private final UtilisateurRepository utilisateurs;
    private final RendezVousMapper mapper;

    // ---------- Patient ----------

    @Transactional
    public RendezVousResponse reserver(String emailPatient, RendezVousRequest req) {
        Utilisateur patient = utilisateurs.findByEmail(emailPatient).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));

        // Verrou : un seul patient peut réserver ce créneau
        Creneau creneau = creneaux.findByIdForUpdate(req.creneauId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Créneau introuvable"));
        Specialiste specialiste = creneau.getSpecialiste();

        if (specialiste.getStatut() != StatutValidation.VALIDE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Créneau introuvable");
        }
        if (creneau.isReserve()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce créneau vient d'être réservé");
        }
        if (!creneau.getDebut().isAfter(
                LocalDateTime.now().plusMinutes(CreneauService.DELAI_MIN_RESERVATION))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce créneau est trop proche ou passé");
        }

        creneau.setReserve(true);
        creneaux.save(creneau);

        RendezVous r = rendezVous.save(RendezVous.builder()
                .creneau(creneau)
                .patient(patient)
                .motif(req.motif())
                .lienVisio("https://meet.jit.si/XamleSante-" + UUID.randomUUID())
                .build());

        paiements.save(Paiement.builder()
                .reference("WAVE-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .rendezVous(r)
                .montant(FRAIS_DOSSIER)
                .fournisseur("WAVE_SIMULE")
                .build());

        return mapper.versReponse(r);
    }

    @Transactional(readOnly = true)
    public List<RendezVousResponse> mesRendezVous(String emailPatient) {
        return rendezVous.findByPatientEmailOrderByCreneauDebutDesc(emailPatient).stream()
                .map(mapper::versReponse)
                .toList();
    }

    @Transactional
    public RendezVousResponse annulerParPatient(String emailPatient, Long id) {
        RendezVous r = rendezVous.findById(id)
                .filter(x -> x.getPatient().getEmail().equals(emailPatient))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable"));
        return annuler(r);
    }

    // ---------- Professionnel ----------

    @Transactional(readOnly = true)
    public List<RendezVousResponse> rendezVousDuProfessionnel(String emailPro) {
        return rendezVous.findDuProfessionnel(emailPro).stream()
                .map(mapper::versReponse)
                .toList();
    }

    @Transactional
    public RendezVousResponse annulerParProfessionnel(String emailPro, Long id) {
        RendezVous r = rendezVous.findById(id)
                .filter(x -> {
                    Utilisateur u = x.getCreneau().getSpecialiste().getUtilisateur();
                    return u != null && emailPro.equals(u.getEmail());
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable"));
        return annuler(r);
    }

    // ---------- Expiration (appelée par le planificateur) ----------

    @Transactional
    public int expirerNonPayes() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(DELAI_PAIEMENT_MIN);
        List<RendezVous> expires = rendezVous
                .findByStatutAndDateCreationBefore(StatutRendezVous.EN_ATTENTE_PAIEMENT, limite);
        for (RendezVous r : expires) {
            paiements.findFirstByRendezVousIdOrderByIdDesc(r.getId()).ifPresent(p -> {
                if (p.getStatut() == StatutPaiement.EN_ATTENTE) {
                    p.setStatut(StatutPaiement.ECHOUE);
                    paiements.save(p);
                }
            });
            libererCreneau(r.getCreneau());
            r.setStatut(StatutRendezVous.ANNULE);
            rendezVous.save(r);
        }
        return expires.size();
    }

    // ---------- Méthodes internes ----------

    private RendezVousResponse annuler(RendezVous r) {
        if (r.getStatut() == StatutRendezVous.ANNULE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce rendez-vous est déjà annulé");
        }
        if (!r.getCreneau().getDebut().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce rendez-vous est déjà passé");
        }
        paiements.findFirstByRendezVousIdOrderByIdDesc(r.getId()).ifPresent(p -> {
            if (p.getStatut() == StatutPaiement.PAYE) {
                p.setStatut(StatutPaiement.REMBOURSE); // remboursement simulé
                paiements.save(p);
            } else if (p.getStatut() == StatutPaiement.EN_ATTENTE) {
                p.setStatut(StatutPaiement.ECHOUE);
                paiements.save(p);
            }
        });
        libererCreneau(r.getCreneau());
        r.setStatut(StatutRendezVous.ANNULE);
        rendezVous.save(r);
        return mapper.versReponse(r);
    }

    private void libererCreneau(Creneau c) {
        c.setReserve(false);
        creneaux.save(c);
    }
}
