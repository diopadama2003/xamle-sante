package sn.uasz.xamle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sn.uasz.xamle.dto.RendezVousResponse;
import sn.uasz.xamle.model.Paiement;
import sn.uasz.xamle.model.RendezVous;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.StatutRendezVous;
import sn.uasz.xamle.repository.PaiementRepository;

@Component
@RequiredArgsConstructor
public class RendezVousMapper {

    private final PaiementRepository paiements;

    public RendezVousResponse versReponse(RendezVous r) {
        Specialiste s = r.getCreneau().getSpecialiste();
        Paiement p = paiements.findFirstByRendezVousIdOrderByIdDesc(r.getId()).orElse(null);

        return new RendezVousResponse(
                r.getId(),
                s.getId(),
                "Dr " + s.getPrenom() + " " + s.getNom(),
                s.getSpecialite(),
                r.getPatient().getPrenom() + " " + r.getPatient().getNom(),
                r.getCreneau().getDebut(),
                r.getCreneau().getFin(),
                r.getMotif(),
                r.getStatut(),
                // Le lien de visio n'est révélé qu'une fois le rendez-vous confirmé
                r.getStatut() == StatutRendezVous.CONFIRME ? r.getLienVisio() : null,
                p != null ? p.getReference() : null,
                p != null ? p.getStatut() : null,
                p != null ? p.getMontant() : 0);
    }
}