package com.example.contentieux_security.controller;

import com.example.contentieux_security.config.KeycloakUserService;
import com.example.contentieux_security.dto.PasswordChangeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/prestataire")
@RequiredArgsConstructor
public class PrestatairePasswordController {

    private final KeycloakUserService keycloakUserService;

    @GetMapping("/change-password")
    public String showForm(Model model) {
        model.addAttribute("passwordChange", new PasswordChangeRequest());
        return "prestataire/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@ModelAttribute PasswordChangeRequest request,
                                 @AuthenticationPrincipal OidcUser oidcUser,
                                 RedirectAttributes redirectAttrs) {

        // Validations
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            redirectAttrs.addFlashAttribute("error", "Le mot de passe doit contenir au moins 8 caractères.");
            return "redirect:/prestataire/change-password";
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            redirectAttrs.addFlashAttribute("error", "Les mots de passe ne correspondent pas.");
            return "redirect:/prestataire/change-password";
        }

        try {
            keycloakUserService.changeUserPassword(
                oidcUser.getPreferredUsername(),
                request.getNewPassword()
            );

            // ✅ Rediriger vers le dashboard selon le rôle
            String dashboardUrl = oidcUser.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> switch (a) {
                        case "ROLE_AVOCAT"               -> "/avocat/dashboard";
                        case "ROLE_HUISSIER"             -> "/huissier/dashboard";
                        case "ROLE_EXPERT"               -> "/expert/dashboard";
                        case "ROLE_VALIDATEUR_JURIDIQUE" -> "/validateur/juridique/dashboard";
                        case "ROLE_VALIDATEUR_FINANCIER" -> "/validateur/financier/dashboard";
                        default -> null;
                    })
                    .filter(url -> url != null)
                    .findFirst()
                    .orElse("/");

            redirectAttrs.addFlashAttribute("success", "✅ Mot de passe changé avec succès !");
            return "redirect:" + dashboardUrl;

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "❌ Erreur : " + e.getMessage());
            return "redirect:/prestataire/change-password";
        }
    }
}