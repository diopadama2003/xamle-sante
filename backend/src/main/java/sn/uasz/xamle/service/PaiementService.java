package sn.uasz.xamle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.PaiementResponse;
import sn.uasz.xamle.model.Creneau;
import sn.uasz.xamle.model.Paiement;
import sn.uasz.xamle.model.RendezVous;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.StatutPaiement;
import sn.uasz.xamle.model.StatutRendezVous;
import sn.uasz.xamle.model.Utilisateur;
import sn.uasz.xamle.repository.CreneauRepository;
import sn.uasz.xamle.repository.PaiementRepository;
import sn.uasz.xamle.repository.RendezVousRepository;
import sn.uasz.xamle.util.TelephoneUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PaiementService {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final PaiementRepository paiements;
    private final RendezVousRepository rendezVous;
    private final CreneauRepository creneaux;
    private final SmsService smsService;

    @Transactional(readOnly = true)
    public PaiementResponse consulter(String email, String reference) {
        return versReponse(chercher(email, reference));
    }

    /**
     * Simule le retour de Wave : succes = true confirme le rendez-vous,
     * succes = false annule le rendez-vous et libère le créneau.
     */
    @Transactional
    public PaiementResponse confirmer(String email, String reference, boolean succes) {
        Paiement p = chercher(email, reference);
        RendezVous r = p.getRendezVous();

        if (p.getStatut() != StatutPaiement.EN_ATTENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce paiement a déjà été traité");
        }
        if (r.getStatut() != StatutRendezVous.EN_ATTENTE_PAIEMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce rendez-vous n'est plus en attente de paiement");
        }

        if (succes) {
            p.setStatut(StatutPaiement.PAYE);
            p.setDatePaiement(LocalDateTime.now());
            r.setStatut(StatutRendezVous.CONFIRME);
            paiements.save(p);
            rendezVous.save(r);
            notifierConfirmation(r);
        } else {
            p.setStatut(StatutPaiement.ECHOUE);
            r.setStatut(StatutRendezVous.ANNULE);
            Creneau c = r.getCreneau();
            c.setReserve(false);
            creneaux.save(c);
            paiements.save(p);
            rendezVous.save(r);
        }
        return versReponse(p);
    }

    // ---------- Méthodes internes ----------

    private Paiement chercher(String email, String reference) {
        return paiements.findByReference(reference)
                .filter(p -> p.getRendezVous().getPatient().getEmail().equals(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement introuvable"));
    }

    private PaiementResponse versReponse(Paiement p) {
        RendezVous r = p.getRendezVous();
        Specialiste s = r.getCreneau().getSpecialiste();
        return new PaiementResponse(p.getReference(), p.getMontant(), p.getStatut(), r.getId(),
                "Dr " + s.getPrenom() + " " + s.getNom(), r.getCreneau().getDebut());
    }

    private void notifierConfirmation(RendezVous r) {
        Specialiste s = r.getCreneau().getSpecialiste();
        String date = r.getCreneau().getDebut().format(FORMAT);

        notifier(r.getPatient(), "Xamlé Santé : rendez-vous confirmé avec Dr " + s.getPrenom() + " "
                + s.getNom() + " le " + date + ". Consultation vidéo : " + r.getLienVisio());
        notifier(s.getUtilisateur(), "Xamlé Santé : nouveau rendez-vous confirmé le " + date
                + " avec " + r.getPatient().getPrenom() + ".");
    }

    private void notifier(Utilisateur u, String texte) {
        if (u == null) {
            return;
        }
        String numero = TelephoneUtil.normaliser(u.getTelephone());
        if (numero != null) {
            smsService.envoyer(numero, texte);
        }
    }
}