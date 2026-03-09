package com.example.contentieux_security.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/validateur")
@RequiredArgsConstructor
public class ValidateurController {

    // ── Méthode utilitaire privée ──────────────────────────────────────────
    private void addUserAttributes(Model model, OidcUser oidcUser) {
        model.addAttribute("givenName",  oidcUser.getGivenName());
        model.addAttribute("familyName", oidcUser.getFamilyName());
        model.addAttribute("username",   oidcUser.getPreferredUsername());
    }

    // ══════════════════════════════════════════════════════════════
    //  VALIDATEUR FINANCIER
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/financier/dashboard")
    public String validateurFinancierDashboard(Model model,
                                               @AuthenticationPrincipal OidcUser oidcUser) {
        addUserAttributes(model, oidcUser);
        model.addAttribute("typeValidation", "Financière");
        return "validateur/dashboard-financier";
    }

    @GetMapping("/factures")
    public String validerFactures(Model model,
                                  @AuthenticationPrincipal OidcUser oidcUser) {
        addUserAttributes(model, oidcUser);
        model.addAttribute("pageTitle", "Valider les Factures");
        return "validateur/factures";
    }

    // ══════════════════════════════════════════════════════════════
    //  VALIDATEUR JURIDIQUE
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/juridique/dashboard")
    public String validateurJuridiqueDashboard(Model model,
                                               @AuthenticationPrincipal OidcUser oidcUser) {
        addUserAttributes(model, oidcUser);
        model.addAttribute("typeValidation", "Juridique");
        return "validateur/dashboard-juridique";
    }

    // ══════════════════════════════════════════════════════════════
    //  COMMUN (financier + juridique)
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/dossiers")
    public String validerDossiers(Model model,
                                  @AuthenticationPrincipal OidcUser oidcUser) {
        addUserAttributes(model, oidcUser);
        model.addAttribute("pageTitle", "Valider les Dossiers");
        return "validateur/dossiers";
    }

    @GetMapping("/historique")
    public String historique(Model model,
                             @AuthenticationPrincipal OidcUser oidcUser) {
        addUserAttributes(model, oidcUser);
        return "validateur/historique";
    }
}