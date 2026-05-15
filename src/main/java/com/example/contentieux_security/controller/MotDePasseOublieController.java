package com.example.contentieux_security.controller;

import com.example.contentieux_security.service.MotDePasseOublieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class MotDePasseOublieController {

    private final MotDePasseOublieService motDePasseOublieService;

    // ✅ Route publique — pas de token requis
    // Appelée quand l'utilisateur clique "Mot de passe oublié"
    @PostMapping("/mot-de-passe-oublie")
    public ResponseEntity<?> demanderReinitialisation(
            @RequestBody Map<String, String> body) {
        try {
            // ✅ Accepte "email" OU "username" dans le body
            String valeur = body.getOrDefault("email",
                            body.getOrDefault("username", null));
    
            if (valeur == null || valeur.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email ou username obligatoire"));
            }
    
            motDePasseOublieService.traiterDemande(valeur);
    
            return ResponseEntity.ok(Map.of(
                "message", "Si cet identifiant existe, "
                         + "un administrateur a été notifié."
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "message", "Si cet identifiant existe, "
                         + "un administrateur a été notifié."
            ));
        }
    }
    // ✅ Appelé par l'admin pour réinitialiser le mot de passe
    @PostMapping("/reinitialiser-mdp/{username}")
    public ResponseEntity<?> reinitialiserMotDePasse(
            @PathVariable String username) {
        try {
            motDePasseOublieService.reinitialiserMotDePasse(username);
            return ResponseEntity.ok(Map.of(
                "message", "Mot de passe réinitialisé et envoyé à l'utilisateur"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}