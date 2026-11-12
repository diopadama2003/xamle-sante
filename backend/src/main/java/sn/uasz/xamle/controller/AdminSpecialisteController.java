package sn.uasz.xamle.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.uasz.xamle.dto.SuppressionResponse;
import sn.uasz.xamle.service.ProfessionnelAdminService;

@RestController
@RequestMapping("/api/admin/specialistes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminSpecialisteController {

    private final ProfessionnelAdminService service;

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un professionnel (même déjà validé)",
            description = "Supprime le profil, ses créneaux, rendez-vous, paiements, avis et sa photo. "
                    + "Les patients ayant un rendez-vous à venir sont prévenus par SMS.")
    public SuppressionResponse supprimer(
            @PathVariable("id") Long id,
            @RequestParam(name = "bloquerCompte", defaultValue = "false") boolean bloquerCompte) {
        return service.supprimer(id, bloquerCompte);
    }
}