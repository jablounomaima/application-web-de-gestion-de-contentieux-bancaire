package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.DossierCreationRequest;
import com.example.contentieux_security.dto.DossierDetailDTO;
import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.entity.Validateur;
import com.example.contentieux_security.enums.TypeValidateur;
import com.example.contentieux_security.repository.ClientRepository;
import com.example.contentieux_security.repository.DossierRepository;
import com.example.contentieux_security.repository.ValidateurRepository;
import com.example.contentieux_security.service.DossierService;
import com.example.contentieux_security.service.HistoriqueService;
import com.example.contentieux_security.dto.RisqueAjoutRequest;
import com.example.contentieux_security.dto.GarantieAjoutRequest;
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

    // ================== DEPENDANCES ==================
    private final DossierService dossierService;
    private final HistoriqueService historiqueService;
    private final DossierRepository dossierRepository;
    private final ValidateurRepository validateurRepository;
    private final ClientRepository clientRepository;

    // =====================================================
    // 📌 LISTE DES DOSSIERS (AGENT)
    // =====================================================
    @GetMapping("/agent/dossiers")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String listeDossiers(Model model, Principal principal) {

        String username = principal.getName();

        // Récupérer les dossiers de l’agent connecté
        model.addAttribute("dossiers", dossierService.getDossiersAgent(username));

        return "agent/dossiers/list";
    }

    // =====================================================
    // 📌 FORMULAIRE CRÉATION
    // =====================================================
    @GetMapping("/agent/dossiers/create")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String formulaireCreation(Model model) {

        model.addAttribute("dossierRequest", new DossierCreationRequest());
        model.addAttribute("clients", clientRepository.findAll());

        return "agent/dossiers/create";
    }

    // =====================================================
    // 📌 CRÉATION DOSSIER
    // =====================================================
    @PostMapping("/agent/dossiers/creer")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String creerDossier(@ModelAttribute DossierCreationRequest request,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {

        try {
            DossierContentieux dossier =
                    dossierService.creerDossier(request, principal.getName());

            redirectAttributes.addFlashAttribute("success",
                    "Dossier créé avec succès");

            return "redirect:/agent/dossiers/" + dossier.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/dossiers/create";
        }
    }

    // =====================================================
    // 📌 DÉTAIL DOSSIER
    // =====================================================
    @GetMapping("/agent/dossiers/{id}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String detailDossier(@PathVariable Long id,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        try {
            // DTO avec historique
            DossierDetailDTO dossier = dossierService.getDossierDetail(id);

            model.addAttribute("dossier", dossier);

            // Liste validateurs
            model.addAttribute("validateurs_financiers",
                    validateurRepository.findByTypeValidateurAndActifTrue(TypeValidateur.VALIDATEUR_FINANCIER));

            model.addAttribute("validateurs_juridiques",
                    validateurRepository.findByTypeValidateurAndActifTrue(TypeValidateur.VALIDATEUR_JURIDIQUE));

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
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String choisirValidateurs(@PathVariable Long id,
                                     @RequestParam String validateurFinancier,
                                     @RequestParam String validateurJuridique,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {

        try {
            dossierService.choisirValidateurs(id,
                    validateurFinancier,
                    validateurJuridique,
                    principal.getName());

            redirectAttributes.addFlashAttribute("success",
                    "Validateurs assignés");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/agent/dossiers/" + id;
    }

    // =====================================================
    // 📌 SOUMETTRE À VALIDATION
    // =====================================================
    @PostMapping("/agent/dossiers/{id}/soumettre")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String soumettre(@PathVariable Long id,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {

        try {
            dossierService.soumettreAValidation(id, principal.getName());

            redirectAttributes.addFlashAttribute("success",
                    "Dossier envoyé aux validateurs");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/agent/dossiers/" + id;
    }

    // =====================================================
    // 📌 SUPPRESSION
    // =====================================================
    @PostMapping("/agent/dossiers/{id}/supprimer")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String supprimer(@PathVariable Long id,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {

        try {
            dossierService.supprimerDossier(id, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Dossier supprimé");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/agent/dossiers";
    }

    // =====================================================
    // 📌 ÉDITION (GET)
    // =====================================================
    @GetMapping("/agent/dossiers/{id}/edit")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String editForm(@PathVariable Long id,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        try {
            DossierContentieux dossier = dossierService.getDossierForEdit(id);

            DossierCreationRequest request = new DossierCreationRequest();

            // Copier les données
            request.setLibelle(dossier.getLibelle());
            request.setDescription(dossier.getDescription());
            request.setNotes(dossier.getNotes());

            model.addAttribute("dossierRequest", request);
            model.addAttribute("dossier", dossier);
            model.addAttribute("clients", clientRepository.findAll());

            return "agent/dossiers/edit";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/dossiers";
        }
    }

    // =====================================================
    // 📌 ÉDITION (POST)
    // =====================================================
    @PostMapping("/agent/dossiers/{id}/edit")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String modifier(@PathVariable Long id,
                           @ModelAttribute DossierCreationRequest request,
                           Principal principal,
                           RedirectAttributes redirectAttributes) {

        try {
            dossierService.modifierDossier(id, request, principal.getName());

            redirectAttributes.addFlashAttribute("success",
                    "Dossier modifié");

            return "redirect:/agent/dossiers/" + id;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/dossiers/" + id + "/edit";
        }
    }

    // ════════════════════════════════════════════════════
    //  SÉLECTIONNER UN RISQUE
    // ════════════════════════════════════════════════════

    @PostMapping("/agent/dossiers/{dossierId}/risques/{risqueId}/selectionner")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String selectionnerRisque(@PathVariable Long dossierId,
                                     @PathVariable Long risqueId,
                                     @RequestParam boolean selectionne,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {
        try {
            dossierService.selectionnerRisque(
                    dossierId,
                    risqueId,
                    selectionne,
                    principal.getName()
            );
            redirectAttributes.addFlashAttribute("success", "Risque mis à jour.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/dossiers/" + dossierId;
    }

    // ════════════════════════════════════════════════════
    //  AJOUTER UN RISQUE
    // ════════════════════════════════════════════════════

    @PostMapping("/agent/dossiers/{dossierId}/risques/ajouter")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String ajouterRisque(@PathVariable Long dossierId,
                                 @ModelAttribute RisqueAjoutRequest request,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            dossierService.ajouterRisque(dossierId, request, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Risque ajouté.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/dossiers/" + dossierId;
    }

    // ════════════════════════════════════════════════════
    //  AJOUTER UNE GARANTIE
    // ════════════════════════════════════════════════════

    @PostMapping("/agent/dossiers/{dossierId}/risques/{risqueId}/garanties/ajouter")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String ajouterGarantie(@PathVariable Long dossierId,
                                   @PathVariable Long risqueId,
                                   @ModelAttribute GarantieAjoutRequest request,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            dossierService.ajouterGarantie(risqueId, request, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Garantie ajoutée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/dossiers/" + dossierId;
    }

}