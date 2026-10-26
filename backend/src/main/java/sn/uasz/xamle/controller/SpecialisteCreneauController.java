package sn.uasz.xamle.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.uasz.xamle.dto.CreneauResponse;
import sn.uasz.xamle.service.CreneauService;

import java.util.List;

@RestController
@RequestMapping("/api/specialistes")
@RequiredArgsConstructor
public class SpecialisteCreneauController {

    private final CreneauService service;

    @GetMapping("/{id}/creneaux")
    public List<CreneauResponse> libres(@PathVariable("id") Long id) {
        return service.libres(id);
    }
}