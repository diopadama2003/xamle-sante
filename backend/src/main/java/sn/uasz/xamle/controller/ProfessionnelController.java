package sn.uasz.xamle.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sn.uasz.xamle.dto.SpecialisteRequest;
import sn.uasz.xamle.dto.SpecialisteResponse;
import sn.uasz.xamle.service.PhotoService;
import sn.uasz.xamle.service.SpecialisteService;

@RestController
@RequestMapping("/api/professionnel")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESSIONNEL')")
@SecurityRequirement(name = "bearerAuth")
public class ProfessionnelController {

    private final SpecialisteService service;
    private final PhotoService photoService;

    @PostMapping("/profil")
    @ResponseStatus(HttpStatus.CREATED)
    public SpecialisteResponse creer(Authentication auth,
                                     @Valid @RequestBody SpecialisteRequest req) {
        return service.creerProfil(auth.getName(), req);
    }

    @GetMapping("/profil")
    public SpecialisteResponse monProfil(Authentication auth) {
        return service.monProfil(auth.getName());
    }

    @PutMapping("/profil")
    public SpecialisteResponse modifier(Authentication auth,
                                        @Valid @RequestBody SpecialisteRequest req) {
        return service.modifierProfil(auth.getName(), req);
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SpecialisteResponse envoyerPhoto(Authentication auth,
                                            @RequestParam("fichier") MultipartFile fichier) {
        return photoService.enregistrer(auth.getName(), fichier);
    }

    @DeleteMapping("/photo")
    public SpecialisteResponse supprimerPhoto(Authentication auth) {
        return photoService.supprimer(auth.getName());
    }
}