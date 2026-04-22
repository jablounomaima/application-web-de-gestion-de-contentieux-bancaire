package com.example.contentieux_security.controller;

import com.example.contentieux_security.config.KeycloakUserService;
import com.example.contentieux_security.dto.PasswordChangeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/prestataire")
@RequiredArgsConstructor
public class PrestatairePasswordController {

    private final KeycloakUserService keycloakUserService;

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest request,
                                            @AuthenticationPrincipal Jwt jwt) {

        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le mot de passe doit contenir au moins 8 caractères."));
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Les mots de passe ne correspondent pas."));
        }

        try {
            String username = jwt.getClaimAsString("preferred_username");
            if (username == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Utilisateur non identifié dans le token."));
            }

            keycloakUserService.changeUserPassword(
                username,
                request.getNewPassword()
            );

            return ResponseEntity.ok(Map.of("message", "Mot de passe changé avec succès !"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erreur : " + e.getMessage()));
        }
    }
}