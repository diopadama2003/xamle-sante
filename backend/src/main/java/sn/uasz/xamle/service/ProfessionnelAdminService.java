package sn.uasz.xamle.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.SuppressionResponse;
import sn.uasz.xamle.model.RendezVous;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.StatutRendezVous;
import sn.uasz.xamle.model.Utilisateur;
import sn.uasz.xamle.repository.AvisRepository;
import sn.uasz.xamle.repository.CreneauRepository;
import sn.uasz.xamle.repository.PaiementRepository;
import sn.uasz.xamle.repository.RendezVousRepository;
import sn.uasz.xamle.repository.SpecialistePhotoRepository;
import sn.uasz.xamle.repository.SpecialisteRepository;
import sn.uasz.xamle.repository.UtilisateurRepository;
import sn.uasz.xamle.util.TelephoneUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Retrait définitif d'un professionnel par l'administrateur. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfessionnelAdminService {

    private static final DateTimeFormatter FORMAT_SMS =
            DateTimeFormatter.ofPattern("dd/MM 'à' HH'h'mm", Locale.FRANCE);

    private final SpecialisteRepository specialistes;
    private final SpecialistePhotoRepository photos;
    private final RendezVousRepository rendezVous;
    private final PaiementRepository paiements;
    private final CreneauRepository creneaux;
    private final AvisRepository avis;
    private final UtilisateurRepository utilisateurs;
    private final SmsService smsService;

    /**
     * Supprime le professionnel et tout ce qui lui est rattaché (créneaux, rendez-vous, paiements, avis, photo).
     * Les patients qui avaient un rendez-vous à venir sont prévenus par SMS.
     *
     * @param bloquerCompte si vrai, le compte de connexion est aussi désactivé (le professionnel ne peut plus se reconnecter)
     */
    @Transactional
    public SuppressionResponse supprimer(Long id, boolean bloquerCompte) {
        Specialiste s = specialistes.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Spécialiste introuvable"));
        String nomComplet = s.getPrenom() + " " + s.getNom();
        Utilisateur compte = s.getUtilisateur();

        List<RendezVous> aVenir = rendezVous.findByCreneauSpecialisteIdAndCreneauDebutAfterAndStatutNot(
                id, LocalDateTime.now(), StatutRendezVous.ANNULE);
        prevenirPatients(aVenir, nomComplet);

        // L'ordre compte : on supprime d'abord ce qui dépend des autres
        paiements.supprimerDuSpecialiste(id);
        rendezVous.supprimerDuSpecialiste(id);
        creneaux.supprimerDuSpecialiste(id);
        avis.supprimerDuSpecialiste(id);
        if (photos.existsById(id)) {
            photos.deleteById(id);
        }
        specialistes.delete(s);

        boolean bloque = false;
        if (bloquerCompte && compte != null) {
            compte.setActif(false);
            utilisateurs.save(compte);
            bloque = true;
        }
        return new SuppressionResponse(nomComplet, aVenir.size(), bloque);
    }

    private void prevenirPatients(List<RendezVous> aVenir, String nomProfessionnel) {
        for (RendezVous r : aVenir) {
            String numero = TelephoneUtil.normaliser(r.getPatient().getTelephone());
            if (numero == null) {
                continue;
            }
            String texte = "Xamlé Santé : votre rendez-vous du " + r.getCreneau().getDebut().format(FORMAT_SMS)
                    + " avec Dr " + nomProfessionnel + " est annulé, ce professionnel n'est plus référencé. "
                    + "Vos frais de dossier vous sont remboursés.";
            try {
                smsService.envoyer(numero, texte);
            } catch (RuntimeException e) {
                log.warn("SMS d'annulation non envoyé : {}", e.getMessage());
            }
        }
    }
}