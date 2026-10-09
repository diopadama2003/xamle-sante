package sn.uasz.xamle.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sn.uasz.xamle.dto.SpecialisteResponse;
import sn.uasz.xamle.service.SpecialisteService;

import java.util.List;

@RestController
@RequestMapping("/api/moderation/specialistes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ModerationController {

    private final SpecialisteService service;

    @GetMapping("/en-attente")
    public List<SpecialisteResponse> enAttente() {
        return service.enAttente();
    }

    @PatchMapping("/{id}/valider")
    public SpecialisteResponse valider(@PathVariable Long id) {
        return service.valider(id);
    }

    @PatchMapping("/{id}/rejeter")
    public SpecialisteResponse rejeter(@PathVariable Long id) {
        return service.rejeter(id);
    }
}