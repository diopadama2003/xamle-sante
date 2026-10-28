package sn.uasz.xamle.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.uasz.xamle.dto.StatistiquesResponse;
import sn.uasz.xamle.service.StatistiquesService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/statistiques")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminStatistiquesController {

    private final StatistiquesService service;

    @GetMapping
    public StatistiquesResponse statistiques(
            @RequestParam(name = "jours", defaultValue = "30") int jours) {
        return service.calculer(jours);
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exporter(
            @RequestParam(name = "jours", defaultValue = "30") int jours) {
        byte[] contenu = service.exporterCsv(jours).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"signalements-" + LocalDate.now() + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(contenu);
    }
}