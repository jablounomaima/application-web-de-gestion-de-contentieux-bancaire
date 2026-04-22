package com.example.contentieux_security.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LoginRedirectController {

    @GetMapping("/prestation-redirect")
    public ResponseEntity<?> redirectToDashboard(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        List<String> roles = authentication.getAuthorities()
                                           .stream()
                                           .map(a -> a.getAuthority())
                                           .toList();

        String target = "/";
        if (roles.contains("ROLE_AVOCAT")) {
            target = "/avocat/dashboard";
        } else if (roles.contains("ROLE_HUISSIER")) {
            target = "/huissier/dashboard";
        } else if (roles.contains("ROLE_EXPERT")) {
            target = "/expert/dashboard";
        } else if (roles.contains("ROLE_VALIDATEUR_FINANCIER")) {
            target = "/validateur-financier/dashboard";
        } else if (roles.contains("ROLE_VALIDATEUR_JURIDIQUE")) {
            target = "/validateur-juridique/dashboard";
        }

        return ResponseEntity.ok(Map.of("redirectUrl", target, "roles", roles));
    }
}