package sn.uasz.xamle.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sn.uasz.xamle.dto.CreneauRequest;
import sn.uasz.xamle.dto.CreneauResponse;
import sn.uasz.xamle.dto.RendezVousResponse;
import sn.uasz.xamle.service.CreneauService;
import sn.uasz.xamle.service.RendezVousService;

import java.util.List;

@RestController
@RequestMapping("/api/professionnel")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESSIONNEL')")
@SecurityRequirement(name = "bearerAuth")
public class ProfessionnelAgendaController {

    private final CreneauService creneaux;
    private final RendezVousService rendezVous;

    @PostMapping("/creneaux")
    @ResponseStatus(HttpStatus.CREATED)
    public CreneauResponse creer(Authentication auth, @Valid @RequestBody CreneauRequest req) {
        return creneaux.creer(auth.getName(), req);
    }

    @GetMapping("/creneaux")
    public List<CreneauResponse> mesCreneaux(Authentication auth) {
        return creneaux.mesCreneaux(auth.getName());
    }

    @DeleteMapping("/creneaux/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(Authentication auth, @PathVariable("id") Long id) {
        creneaux.supprimer(auth.getName(), id);
    }

    @GetMapping("/rendez-vous")
    public List<RendezVousResponse> mesRendezVous(Authentication auth) {
        return rendezVous.rendezVousDuProfessionnel(auth.getName());
    }

    @PatchMapping("/rendez-vous/{id}/annuler")
    public RendezVousResponse annuler(Authentication auth, @PathVariable("id") Long id) {
        return rendezVous.annulerParProfessionnel(auth.getName(), id);
    }
}