package sn.uasz.xamle.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import sn.uasz.xamle.dto.SignalementRequest;
import sn.uasz.xamle.dto.ZoneChaleurResponse;
import sn.uasz.xamle.model.Symptome;
import sn.uasz.xamle.service.QuartierCatalogue;
import sn.uasz.xamle.service.SignalementService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/signalements")
@RequiredArgsConstructor
public class SignalementController {

    private final SignalementService service;
    private final QuartierCatalogue catalogue;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> signaler(@Valid @RequestBody SignalementRequest req,
                                        HttpServletRequest http) {
        service.creer(req, http.getRemoteAddr());
        return Map.of("message", "Signalement reçu. Il sera pris en compte après vérification.");
    }

    @GetMapping("/chaleur")
    public List<ZoneChaleurResponse> chaleur(
            @RequestParam(name = "jours", defaultValue = "14") int jours,
            @RequestParam(name = "symptome", required = false) Symptome symptome) {
        return service.chaleur(jours, symptome);
    }

    @GetMapping("/quartiers")
    public List<String> quartiers() {
        return catalogue.tous().stream().map(QuartierCatalogue.Quartier::nom).toList();
    }
}