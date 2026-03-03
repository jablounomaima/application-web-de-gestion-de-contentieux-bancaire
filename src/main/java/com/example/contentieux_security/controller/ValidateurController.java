package com.example.contentieux_security.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/validateur")
public class ValidateurController {

    @GetMapping("/financier/dashboard")
    @PreAuthorize("hasAnyRole('VALIDATEUR_FINANCIER', 'ADMIN')")
    public String validateurFinancierDashboard(Model model, Principal principal) {
        model.addAttribute("username", principal.getName());
        model.addAttribute("role", "Validateur Financier");
        model.addAttribute("typeValidation", "Financière");
        return "validateur/dashboard";
    }

    @GetMapping("/juridique/dashboard")
    @PreAuthorize("hasAnyRole('VALIDATEUR_JURIDIQUE', 'ADMIN')")
    public String validateurJuridiqueDashboard(Model model, Principal principal) {
        model.addAttribute("username", principal.getName());
        model.addAttribute("role", "Validateur Juridique");
        model.addAttribute("typeValidation", "Juridique");
        return "validateur/dashboard";
    }

    @GetMapping("/dossiers")
    @PreAuthorize("hasAnyRole('VALIDATEUR_FINANCIER', 'VALIDATEUR_JURIDIQUE', 'ADMIN')")
    public String validerDossiers(Model model) {
        model.addAttribute("pageTitle", "Valider les Dossiers");
        return "validateur/dossiers";
    }

    @GetMapping("/factures")
    @PreAuthorize("hasAnyRole('VALIDATEUR_FINANCIER', 'ADMIN')")
    public String validerFactures(Model model) {
        model.addAttribute("pageTitle", "Valider les Factures");
        return "validateur/factures";
    }
}