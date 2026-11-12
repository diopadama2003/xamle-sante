package sn.uasz.xamle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.AvisRequest;
import sn.uasz.xamle.dto.AvisResponse;
import sn.uasz.xamle.dto.MonAvisResponse;
import sn.uasz.xamle.dto.SpecialisteResponse;
import sn.uasz.xamle.model.Avis;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.StatutRendezVous;
import sn.uasz.xamle.model.StatutValidation;
import sn.uasz.xamle.repository.AvisRepository;
import sn.uasz.xamle.repository.RendezVousRepository;
import sn.uasz.xamle.repository.SpecialisteRepository;
import sn.uasz.xamle.repository.UtilisateurRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvisService {

    private final AvisRepository avis;
    private final SpecialisteRepository specialistes;
    private final UtilisateurRepository utilisateurs;
    private final RendezVousRepository rendezVous;

    // ---------- Public ----------

    @Transactional(readOnly = true)
    public List<AvisResponse> liste(Long specialisteId) {
        specialisteValide(specialisteId);
        return avis.findBySpecialisteIdOrderByDateCreationDesc(specialisteId).stream()
                .map(AvisResponse::from)
                .toList();
    }

    /** Ajoute la note moyenne et le nombre d'avis à chaque professionnel de la liste */
    @Transactional(readOnly = true)
    public List<SpecialisteResponse> avecNotes(List<SpecialisteResponse> liste) {
        if (liste.isEmpty()) {
            return liste;
        }
        Map<Long, AvisRepository.Statistique> stats = avis.statistiques().stream()
                .collect(Collectors.toMap(AvisRepository.Statistique::getSpecialisteId, Function.identity()));
        return liste.stream().map(r -> {
            AvisRepository.Statistique s = stats.get(r.id());
            return s == null ? r : r.avecNotes(arrondir(s.getMoyenne()), s.getTotal());
        }).toList();
    }

    public SpecialisteResponse avecNotes(SpecialisteResponse reponse) {
        return avecNotes(List.of(reponse)).get(0);
    }

    // ---------- Patient connecté ----------

    @Transactional(readOnly = true)
    public MonAvisResponse monAvis(String emailPatient, Long specialisteId) {
        specialisteValide(specialisteId);
        AvisResponse mon = avis.findBySpecialisteIdAndPatientEmail(specialisteId, emailPatient)
                .map(AvisResponse::from).orElse(null);
        boolean peutNoter = mon != null || aUnRendezVousConfirme(emailPatient, specialisteId);
        return new MonAvisResponse(peutNoter, mon);
    }

    /** Crée l'avis, ou le remplace si le patient en avait déjà donné un (un seul avis par professionnel). */
    @Transactional
    public AvisResponse donner(String emailPatient, Long specialisteId, AvisRequest req) {
        Specialiste s = specialisteValide(specialisteId);
        Optional<Avis> existant = avis.findBySpecialisteIdAndPatientEmail(specialisteId, emailPatient);

        Avis a;
        if (existant.isPresent()) {
            a = existant.get();
        } else {
            if (!aUnRendezVousConfirme(emailPatient, specialisteId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Tu pourras donner ton avis après avoir pris rendez-vous avec ce professionnel");
            }
            a = Avis.builder()
                    .specialiste(s)
                    .patient(utilisateurs.findByEmail(emailPatient).orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable")))
                    .build();
        }
        a.setNote(req.note());
        a.setCommentaire(nettoyer(req.commentaire()));
        return AvisResponse.from(avis.save(a));
    }

    @Transactional
    public void supprimer(String emailPatient, Long specialisteId) {
        Avis a = avis.findBySpecialisteIdAndPatientEmail(specialisteId, emailPatient).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun avis à supprimer"));
        avis.delete(a);
    }

    // ---------- Méthodes internes ----------

    private Specialiste specialisteValide(Long id) {
        Specialiste s = specialistes.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Spécialiste introuvable"));
        if (s.getStatut() != StatutValidation.VALIDE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Spécialiste introuvable");
        }
        return s;
    }

    private boolean aUnRendezVousConfirme(String email, Long specialisteId) {
        return rendezVous.existsByPatientEmailAndCreneauSpecialisteIdAndStatut(
                email, specialisteId, StatutRendezVous.CONFIRME);
    }

    private String nettoyer(String commentaire) {
        if (commentaire == null || commentaire.isBlank()) {
            return null;
        }
        return commentaire.trim();
    }

    private Double arrondir(Double valeur) {
        return valeur == null ? null : Math.round(valeur * 10) / 10.0;
    }
}