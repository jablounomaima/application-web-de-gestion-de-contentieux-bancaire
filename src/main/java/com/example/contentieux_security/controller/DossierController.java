package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.*;
import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.*;
import com.example.contentieux_security.repository.*;
import com.example.contentieux_security.service.*;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class DossierController {

    // =========================
    // INJECTIONS
    // =========================
    private final DossierService dossierService;
    private final HistoriqueService historiqueService;
    private final GarantieRepository garantieRepository;
    private final DossierRepository dossierRepository;
    private final ValidateurRepository validateurRepository; // ✅ FIX: remplacer PrestataireRepository
    private final NotificationService notificationService;

    // =====================================================
    // 📌 LISTE DOSSIERS AGENT
    // =====================================================
    @GetMapping("/agent/dossiers")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String listeDossiers(Model model, Principal principal) {

        String username = principal.getName();
        List<DossierContentieux> dossiers = dossierService.getDossiersAgent(username);

        model.addAttribute("dossiers", dossiers);
        model.addAttribute("notifCount", notificationService.countNonLues(username));

        return "agent/dossiers/list";
    }

    // =====================================================
    // 📌 DETAIL DOSSIER + LISTE VALIDATEURS
    // =====================================================
    @GetMapping("/agent/dossiers/{id}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String detailDossier(@PathVariable Long id, Model model,
                                RedirectAttributes redirectAttributes) {
        try {

            DossierDetailDTO dossier = dossierService.getDossierDetail(id);
            model.addAttribute("dossier", dossier);

            // =========================
            // VALIDATEURS FINANCIERS
            // =========================
            List<Validateur> vf =
                    validateurRepository.findByTypeValidateurAndActifTrue(TypeValidateur.VALIDATEUR_FINANCIER);

            // =========================
            // VALIDATEURS JURIDIQUES
            // =========================
            List<Validateur> vj =
                    validateurRepository.findByTypeValidateurAndActifTrue(TypeValidateur.VALIDATEUR_JURIDIQUE);

            model.addAttribute("validateurs_financiers", vf);
            model.addAttribute("validateurs_juridiques", vj);

            return "agent/dossiers/detail";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/dossiers";
        }
    }

    // =====================================================
    // 📌 CHOISIR VALIDATEURS
    // =====================================================
    @PostMapping("/agent/dossiers/{id}/choisir-validateurs")
    public String choisirValidateurs(@PathVariable Long id,
                                     @RequestParam String validateurFinancier,
                                     @RequestParam String validateurJuridique,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {

        dossierService.choisirValidateurs(id, validateurFinancier, validateurJuridique, principal.getName());

        redirectAttributes.addFlashAttribute("success", "Validateurs assignés");
        return "redirect:/agent/dossiers/" + id;
    }

    // =====================================================
    // 📌 SUPPRESSION DOSSIER
    // =====================================================
    @PostMapping("/agent/dossiers/{id}/supprimer")
    public String supprimerDossier(@PathVariable Long id,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {

        dossierService.supprimerDossier(id, principal.getName());

        redirectAttributes.addFlashAttribute("success", "Dossier supprimé");
        return "redirect:/agent/dossiers";
    }

    // =====================================================
    // 📌 VALIDATION FINANCIERE
    // =====================================================
    @PostMapping("/validateur/financier/dossiers/{id}/valider")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    public String validerFinancier(@PathVariable Long id,
                                   @RequestParam(required = false) String commentaire,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {

        DossierContentieux d = dossierService.getDossierById(id);

        d.setValidationFinanciere(true);
        d.setCommentaireFinancier(commentaire);
        d.setValidateurFinancierUsername(principal.getName());

        dossierRepository.save(d);

        return "redirect:/validateur/financier/dossiers";
    }

    // =====================================================
    // 📌 VALIDATION JURIDIQUE
    // =====================================================
    @PostMapping("/validateur/juridique/dossiers/{id}/valider")
    public String validerJuridique(@PathVariable Long id,
                                   @RequestParam(required = false) String commentaire,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {

        DossierContentieux d = dossierService.getDossierById(id);

        d.setValidationJuridique(true);
        d.setCommentaireJuridique(commentaire);
        d.setValidateurJuridiqueUsername(principal.getName());

        dossierRepository.save(d);

        return "redirect:/validateur/juridique/dossiers";
    }
}