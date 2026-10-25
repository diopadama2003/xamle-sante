package sn.uasz.xamle.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import sn.uasz.xamle.dto.CampagneRequest;
import sn.uasz.xamle.dto.CampagneResponse;
import sn.uasz.xamle.service.CampagneService;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/campagnes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CampagneAdminController {

    private final CampagneService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampagneResponse creer(@Valid @RequestBody CampagneRequest req) {
        return service.creer(req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }

    @PostMapping("/rappels/executer")
    public Map<String, Integer> executerRappels() {
        return Map.of("rappelsEnvoyes", service.envoyerRappels());
    }
}