package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.Validateur;
import com.example.contentieux_security.repository.ValidateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/validateur")
@RequiredArgsConstructor
public class ValidateurProfileController {

    private final ValidateurRepository validateurRepository;

    // ✅ Appelé par ValidateurActifGuard à chaque navigation
    @GetMapping("/mon-profil")
    public ResponseEntity<?> getMonProfil(Principal principal) {
        try {
            Validateur v = validateurRepository
                .findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Validateur non trouvé"));

            return ResponseEntity.ok(Map.of(
                "username", v.getUsername(),
                "nom",      v.getNom(),
                "prenom",   v.getPrenom(),
                "actif",    v.isActif(),
                "type",     v.getTypeValidateur()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Accès refusé"));
        }
    }
}