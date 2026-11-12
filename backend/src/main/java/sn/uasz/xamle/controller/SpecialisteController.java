package sn.uasz.xamle.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.uasz.xamle.dto.AvisResponse;
import sn.uasz.xamle.dto.SpecialisteResponse;
import sn.uasz.xamle.model.SpecialistePhoto;
import sn.uasz.xamle.service.AvisService;
import sn.uasz.xamle.service.PhotoService;
import sn.uasz.xamle.service.SpecialisteService;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/specialistes")
@RequiredArgsConstructor
public class SpecialisteController {

    private final SpecialisteService service;
    private final PhotoService photoService;
    private final AvisService avisService;

    @GetMapping
    public List<SpecialisteResponse> liste(
            @RequestParam(name = "specialite", required = false) String specialite,
            @RequestParam(name = "langue", required = false) String langue,
            @RequestParam(name = "quartier", required = false) String quartier,
            @RequestParam(name = "disponible", required = false) Boolean disponible,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lng", required = false) Double lng,
            @RequestParam(name = "rayonKm", required = false) Double rayonKm) {
        return avisService.avecNotes(
                service.rechercher(specialite, langue, quartier, disponible, lat, lng, rayonKm));
    }

    @GetMapping("/{id}")
    public SpecialisteResponse detail(@PathVariable Long id) {
        return avisService.avecNotes(service.trouverValide(id));
    }

    @GetMapping("/{id}/avis")
    public List<AvisResponse> avis(@PathVariable("id") Long id) {
        return avisService.liste(id);
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        SpecialistePhoto p = photoService.photoDe(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(p.getTypeContenu()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(p.getContenu());
    }
}