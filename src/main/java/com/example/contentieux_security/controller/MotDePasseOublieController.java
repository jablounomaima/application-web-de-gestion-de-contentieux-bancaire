package com.example.contentieux_security.controller;

import com.example.contentieux_security.service.MotDePasseOublieService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class MotDePasseOublieController {

    private final MotDePasseOublieService motDePasseOublieService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping(
        value    = "/mot-de-passe-oublie",
        consumes = { "application/json", "text/plain", "*/*" }
    )
    public ResponseEntity<?> demanderReinitialisation(
            @RequestBody String bodyBrut) {

        System.out.println("📩 Body reçu: [" + bodyBrut + "]");

        try {
            String valeur  = null;
            String trimmed = bodyBrut.trim();

            if (trimmed.startsWith("{")) {
                // ── JSON valide ─────────────────────────────────────
                try {
                    Map<String, String> map = objectMapper.readValue(
                        trimmed, new TypeReference<>() {});
                    valeur = map.getOrDefault("email",
                             map.getOrDefault("username", null));
                    if (valeur != null) {
                        valeur = valeur.trim().replace("\"", "").trim();
                    }
                    System.out.println("✅ Extrait (JSON): [" + valeur + "]");

                } catch (Exception e) {
                    System.err.println("❌ Erreur parsing JSON: " + e.getMessage());
                    // fallback si JSON malformé
                    if (trimmed.contains(":")) {
                        valeur = trimmed
                            .replaceAll(".*:\\s*\"?([^\"]+)\"?.*", "$1")
                            .trim();
                        System.out.println("✅ Extrait (fallback JSON): [" + valeur + "]");
                    }
                }

            } else if (trimmed.startsWith("\"email\"")
                    || trimmed.startsWith("\"username\"")
                    || trimmed.contains(":")) {
                // ── Texte style  "email": "xxx"  sans accolades ─────
                valeur = trimmed
                    .replaceAll(".*:\\s*\"?([^\"]+)\"?.*", "$1")
                    .trim();
                System.out.println("✅ Extrait (clé-valeur brut): [" + valeur + "]");

            } else {
                // ── Texte brut  → email ou username direct ───────────
                valeur = trimmed.replace("\"", "").trim();
                System.out.println("✅ Extrait (texte brut): [" + valeur + "]");
            }

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
            System.err.println("❌ Erreur controller: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("❌ Erreur réinit mdp: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}