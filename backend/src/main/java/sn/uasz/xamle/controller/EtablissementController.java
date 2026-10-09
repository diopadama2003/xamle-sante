package sn.uasz.xamle.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.uasz.xamle.dto.EtablissementRequest;
import sn.uasz.xamle.dto.EtablissementResponse;
import sn.uasz.xamle.model.TypeEtablissement;
import sn.uasz.xamle.service.EtablissementService;

import java.util.List;

@RestController
@RequestMapping("/api/etablissements")
@RequiredArgsConstructor
public class EtablissementController {

    private final EtablissementService service;

    @GetMapping
    public List<EtablissementResponse> liste(
            @RequestParam(name = "type", required = false) TypeEtablissement type,
            @RequestParam(name = "urgence", required = false) Boolean urgence,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lng", required = false) Double lng,
            @RequestParam(name = "rayonKm", required = false) Double rayonKm) {
        return service.rechercher(type, urgence, lat, lng, rayonKm);
    }

    @GetMapping("/urgences")
    public List<EtablissementResponse> urgences(
            @RequestParam(name = "lat") double lat,
            @RequestParam(name = "lng") double lng) {
        return service.urgencesProches(lat, lng);
    }

    @GetMapping("/{id}")
    public EtablissementResponse detail(@PathVariable Long id) {
        return service.trouver(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_SANTE')")
    @SecurityRequirement(name = "bearerAuth")
    public EtablissementResponse creer(@Valid @RequestBody EtablissementRequest req) {
        return service.creer(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SANTE')")
    @SecurityRequirement(name = "bearerAuth")
    public EtablissementResponse modifier(@PathVariable Long id,
                                          @Valid @RequestBody EtablissementRequest req) {
        return service.modifier(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN_SANTE')")
    @SecurityRequirement(name = "bearerAuth")
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }
}