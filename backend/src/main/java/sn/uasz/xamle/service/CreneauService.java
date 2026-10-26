package sn.uasz.xamle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.CreneauRequest;
import sn.uasz.xamle.dto.CreneauResponse;
import sn.uasz.xamle.model.Creneau;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.StatutValidation;
import sn.uasz.xamle.repository.CreneauRepository;
import sn.uasz.xamle.repository.RendezVousRepository;
import sn.uasz.xamle.repository.SpecialisteRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreneauService {

    /** Un créneau n'est réservable que s'il commence dans plus de 30 minutes. */
    public static final int DELAI_MIN_RESERVATION = 30;

    private final CreneauRepository creneaux;
    private final SpecialisteRepository specialistes;
    private final RendezVousRepository rendezVous;

    // ---------- Public ----------

    @Transactional(readOnly = true)
    public List<CreneauResponse> libres(Long specialisteId) {
        Specialiste s = specialistes.findById(specialisteId)
                .filter(x -> x.getStatut() == StatutValidation.VALIDE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Spécialiste introuvable"));
        return creneaux.findBySpecialisteIdAndReserveFalseAndDebutAfterOrderByDebutAsc(
                        s.getId(), LocalDateTime.now().plusMinutes(DELAI_MIN_RESERVATION))
                .stream().map(CreneauResponse::from).toList();
    }

    // ---------- Professionnel ----------

    @Transactional
    public CreneauResponse creer(String email, CreneauRequest req) {
        Specialiste s = specialistes.findByUtilisateurEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Crée d'abord ton profil"));
        if (s.getStatut() != StatutValidation.VALIDE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ton profil doit être validé avant de publier des créneaux");
        }
        if (!req.debut().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le créneau doit être dans le futur");
        }
        long minutes = Duration.between(req.debut(), req.fin()).toMinutes();
        if (minutes < 15 || minutes > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un créneau dure entre 15 et 120 minutes");
        }
        if (creneaux.existsBySpecialisteIdAndDebutLessThanAndFinGreaterThan(
                s.getId(), req.fin(), req.debut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce créneau chevauche un créneau existant");
        }
        return CreneauResponse.from(creneaux.save(Creneau.builder()
                .specialiste(s)
                .debut(req.debut())
                .fin(req.fin())
                .build()));
    }

    @Transactional(readOnly = true)
    public List<CreneauResponse> mesCreneaux(String email) {
        Specialiste s = specialistes.findByUtilisateurEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun profil pour ce compte"));
        return creneaux.findBySpecialisteIdAndDebutAfterOrderByDebutAsc(s.getId(), LocalDateTime.now())
                .stream().map(CreneauResponse::from).toList();
    }

    @Transactional
    public void supprimer(String email, Long id) {
        Creneau c = creneaux.findById(id)
                .filter(x -> x.getSpecialiste().getUtilisateur() != null
                        && email.equals(x.getSpecialiste().getUtilisateur().getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Créneau introuvable"));
        if (c.isReserve() || rendezVous.existsByCreneauId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce créneau a déjà un rendez-vous ou un historique : il ne peut pas être supprimé");
        }
        creneaux.delete(c);
    }
}