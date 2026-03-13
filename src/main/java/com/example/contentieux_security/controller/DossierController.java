package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.DossierCreationRequest;
import com.example.contentieux_security.entity.Dossier;
import com.example.contentieux_security.repository.ClientRepository;
import com.example.contentieux_security.service.DossierService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/agent/dossiers")
@RequiredArgsConstructor
public class DossierController {

    private final DossierService   dossierService;
    private final ClientRepository clientRepository;

    @GetMapping
    public String list(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        String username = oidcUser.getPreferredUsername();
        List<Dossier> dossiers = dossierService.getDossiersAgent(username);

        model.addAttribute("dossiers",  dossiers);
        model.addAttribute("username",  username);
        model.addAttribute("givenName", oidcUser.getGivenName());

        // ✅ comparaison String == String
        model.addAttribute("totalDossiers",     dossiers.size());
        model.addAttribute("dossiersOuverts",   dossiers.stream().filter(d -> "OUVERT".equals(d.getStatut())).count());
        model.addAttribute("dossiersEnAttente", dossiers.stream().filter(d -> "EN_ATTENTE_VALIDATION".equals(d.getStatut())).count());
        model.addAttribute("dossiersValides",   dossiers.stream().filter(d -> "VALIDE".equals(d.getStatut())).count());

        return "agent/dossiers/list";
    }
    @GetMapping("/create")
    public String showCreateForm(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        model.addAttribute("dossierRequest", new DossierCreationRequest());
        model.addAttribute("clients", clientRepository.findAll());
        model.addAttribute("username", oidcUser.getPreferredUsername());
        model.addAttribute("givenName", oidcUser.getGivenName());
        return "agent/dossiers/create";
    }

    @PostMapping("/create")
    public String createDossier(@ModelAttribute DossierCreationRequest request,
                                @AuthenticationPrincipal OidcUser oidcUser,
                                RedirectAttributes redirectAttrs) {
        try {
            Dossier dossier = dossierService.creerDossier(request, oidcUser.getPreferredUsername());
            redirectAttrs.addFlashAttribute("success",
                "✅ Dossier " + dossier.getNumeroDossier() + " créé avec succès !");
            return "redirect:/agent/dossiers/" + dossier.getId();
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "❌ " + e.getMessage());
            return "redirect:/agent/dossiers/create";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model,
                         @AuthenticationPrincipal OidcUser oidcUser) {
        Dossier dossier = dossierService.getDossierByIdAndAgent(id, oidcUser.getPreferredUsername());
        model.addAttribute("dossier",   dossier);
        model.addAttribute("username",  oidcUser.getPreferredUsername());
        model.addAttribute("givenName", oidcUser.getGivenName());
        return "agent/dossiers/detail";
    }

    @PostMapping("/{id}/soumettre")
    public String soumettreValidation(@PathVariable Long id,
                                      @AuthenticationPrincipal OidcUser oidcUser,
                                      RedirectAttributes redirectAttrs) {
        try {
            dossierService.soumettreAValidation(id, oidcUser.getPreferredUsername());
            redirectAttrs.addFlashAttribute("success", "Dossier soumis à validation !");
            return "redirect:/agent/dossiers/" + id;
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/dossiers/" + id;
        }
    }

    @PostMapping("/{dossierId}/risques/{risqueId}/selectionner")
    public String selectionnerRisque(@PathVariable Long dossierId,
                                     @PathVariable Long risqueId,
                                     @AuthenticationPrincipal OidcUser oidcUser,
                                     RedirectAttributes redirectAttrs) {
        try {
            dossierService.selectionnerRisque(risqueId, oidcUser.getPreferredUsername());
            redirectAttrs.addFlashAttribute("success", "✅ Crédit sélectionné pour la procédure.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "❌ " + e.getMessage());
        }
        return "redirect:/agent/dossiers/" + dossierId;
    }
}