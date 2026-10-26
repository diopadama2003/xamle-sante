package sn.uasz.xamle.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sn.uasz.xamle.dto.PaiementConfirmation;
import sn.uasz.xamle.dto.PaiementResponse;
import sn.uasz.xamle.dto.RendezVousRequest;
import sn.uasz.xamle.dto.RendezVousResponse;
import sn.uasz.xamle.service.PaiementService;
import sn.uasz.xamle.service.RendezVousService;

import java.util.List;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PATIENT')")
@SecurityRequirement(name = "bearerAuth")
public class PatientController {

    private final RendezVousService rendezVous;
    private final PaiementService paiements;

    @PostMapping("/rendez-vous")
    @ResponseStatus(HttpStatus.CREATED)
    public RendezVousResponse reserver(Authentication auth, @Valid @RequestBody RendezVousRequest req) {
        return rendezVous.reserver(auth.getName(), req);
    }

    @GetMapping("/rendez-vous")
    public List<RendezVousResponse> mesRendezVous(Authentication auth) {
        return rendezVous.mesRendezVous(auth.getName());
    }

    @PatchMapping("/rendez-vous/{id}/annuler")
    public RendezVousResponse annuler(Authentication auth, @PathVariable("id") Long id) {
        return rendezVous.annulerParPatient(auth.getName(), id);
    }

    @GetMapping("/paiements/{reference}")
    public PaiementResponse paiement(Authentication auth, @PathVariable("reference") String reference) {
        return paiements.consulter(auth.getName(), reference);
    }

    @PostMapping("/paiements/{reference}/confirmer")
    public PaiementResponse confirmer(Authentication auth,
                                      @PathVariable("reference") String reference,
                                      @Valid @RequestBody PaiementConfirmation req) {
        return paiements.confirmer(auth.getName(), reference, req.succes());
    }
}
