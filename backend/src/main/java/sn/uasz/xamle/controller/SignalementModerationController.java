package sn.uasz.xamle.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sn.uasz.xamle.dto.SignalementResponse;
import sn.uasz.xamle.service.SignalementService;

import java.util.List;

@RestController
@RequestMapping("/api/moderation/signalements")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class SignalementModerationController {

    private final SignalementService service;

    @GetMapping("/en-attente")
    public List<SignalementResponse> enAttente() {
        return service.enAttente();
    }

    @PatchMapping("/{id}/valider")
    public SignalementResponse valider(@PathVariable Long id) {
        return service.valider(id);
    }

    @PatchMapping("/{id}/rejeter")
    public SignalementResponse rejeter(@PathVariable Long id) {
        return service.rejeter(id);
    }
}