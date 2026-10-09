package sn.uasz.xamle.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sn.uasz.xamle.dto.SpecialisteResponse;
import sn.uasz.xamle.service.SpecialisteService;

import java.util.List;

@RestController
@RequestMapping("/api/specialistes")
@RequiredArgsConstructor
public class SpecialisteController {

    private final SpecialisteService service;

    @GetMapping
    public List<SpecialisteResponse> liste(
            @RequestParam(name = "specialite", required = false) String specialite,
            @RequestParam(name = "langue", required = false) String langue,
            @RequestParam(name = "quartier", required = false) String quartier,
            @RequestParam(name = "disponible", required = false) Boolean disponible,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lng", required = false) Double lng,
            @RequestParam(name = "rayonKm", required = false) Double rayonKm) {
        return service.rechercher(specialite, langue, quartier, disponible, lat, lng, rayonKm);
    }

    @GetMapping("/{id}")
    public SpecialisteResponse detail(@PathVariable Long id) {
        return service.trouverValide(id);
    }
}