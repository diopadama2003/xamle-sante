package sn.uasz.xamle.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import sn.uasz.xamle.dto.CampagneResponse;
import sn.uasz.xamle.dto.RappelRequest;
import sn.uasz.xamle.service.CampagneService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campagnes")
@RequiredArgsConstructor
public class CampagneController {

    private final CampagneService service;

    @GetMapping
    public List<CampagneResponse> liste() {
        return service.aVenir();
    }

    @PostMapping("/{id}/rappel")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> rappel(@PathVariable Long id,
                                      @Valid @RequestBody RappelRequest req,
                                      HttpServletRequest http) {
        service.sAbonner(id, req.telephone(), http.getRemoteAddr());
        return Map.of("message", "Rappel enregistré : tu recevras un SMS 24 h avant.");
    }
}