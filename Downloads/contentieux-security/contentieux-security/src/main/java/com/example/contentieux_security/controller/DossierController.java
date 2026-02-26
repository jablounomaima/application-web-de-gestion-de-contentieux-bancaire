package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.DossierRequest;
import com.example.contentieux_security.dto.RisqueRequest;
import com.example.contentieux_security.entity.*; // Added this import
import com.example.contentieux_security.service.DossierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dossiers")
@RequiredArgsConstructor
public class DossierController {

    private final DossierService dossierService;

    @PostMapping
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<Dossier> createDossier(@RequestBody DossierRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(dossierService.createDossier(request.getClientId(), request.getAgenceId(), username));
    }

    @PostMapping("/{id}/risques")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<Void> addRisque(@PathVariable Long id, @RequestBody RisqueRequest request) {
        Risque risque = Risque.builder()
                .montantInitial(request.getMontantInitial())
                .montantRestant(request.getMontantInitial())
                .build();
        dossierService.addRisque(id, risque);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<Void> submit(@PathVariable Long id) {
        dossierService.submitForValidation(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/validate-fin")
    @PreAuthorize("hasRole('VALID_FINANCIER')")
    public ResponseEntity<Void> validateFin(@PathVariable Long id) {
        dossierService.validateFinanciere(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/validate-jur")
    @PreAuthorize("hasRole('VALID_JURIDIQUE')")
    public ResponseEntity<Void> validateJur(@PathVariable Long id) {
        dossierService.validateJuridique(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/risques/{risqueId}/garanties")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<Void> addGarantie(@PathVariable Long risqueId, @RequestBody Garantie garantie) {
        dossierService.addGarantie(risqueId, garantie);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<Dossier>> getAll() {
        return ResponseEntity.ok(dossierService.getAllDossiers());
    }

    @PostMapping("/{id}/prestations")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<Void> createPrestation(@PathVariable Long id,
            @RequestBody com.example.contentieux_security.dto.PrestationRequest request) {
        dossierService.createPrestation(id, request.getType(), request.getDescription());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/prestations/{prestationId}/missions")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<Void> createMission(@PathVariable Long prestationId,
            @RequestBody com.example.contentieux_security.dto.MissionRequest request) {
        dossierService.createMission(prestationId, request.getDescription());
        return ResponseEntity.ok().build();
    }
}
