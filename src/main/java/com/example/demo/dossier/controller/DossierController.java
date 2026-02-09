package com.example.demo.dossier.controller;

import com.example.demo.dossier.dto.DossierRequest;
import com.example.demo.dossier.dto.DossierResponse;
import com.example.demo.dossier.service.DossierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dossiers")
@RequiredArgsConstructor
public class DossierController {

    private final DossierService dossierService;

    @PostMapping
    @PreAuthorize("hasRole('AGENT_BANCAIRE')")
    public ResponseEntity<DossierResponse> create(@RequestBody DossierRequest request, Authentication auth) {
        return ResponseEntity.ok(dossierService.createDossier(request, auth.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_BANCAIRE', 'ROLE_VALIDATEUR_JURIDIQUE', 'ROLE_VALIDATEUR_FINANCIER')")
    public ResponseEntity<List<DossierResponse>> getAll() {
        return ResponseEntity.ok(dossierService.getAllDossiers());
    }
}
