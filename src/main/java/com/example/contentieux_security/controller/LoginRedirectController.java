package com.example.contentieux_security.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginRedirectController {

    @GetMapping("/prestation-redirect")
    public String redirectToDashboard(Authentication authentication) {

        // Récupérer les rôles
        List<String> roles = authentication.getAuthorities()
                                           .stream()
                                           .map(a -> a.getAuthority())
                                           .toList();

        if (roles.contains("ROLE_AVOCAT")) {
            return "redirect:/avocat/dashboard";
        } else if (roles.contains("ROLE_HUISSIER")) {
            return "redirect:/huissier/dashboard";
        } else if (roles.contains("ROLE_EXPERT")) {
            return "redirect:/expert/dashboard";
        } else if (roles.contains("ROLE_VALIDATEUR_FINANCIER")) {
            return "redirect:/validateur-financier/dashboard";
        } else if (roles.contains("ROLE_VALIDATEUR_JURIDIQUE")) {
            return "redirect:/validateur-juridique/dashboard";
        }

        return "redirect:/"; // défaut
    }
}