package com.example.contentieux_security.controller;
import com.example.contentieux_security.dto.DossierCreationRequest;
import com.example.contentieux_security.dto.DossierDetailDTO;
import com.example.contentieux_security.dto.RisqueAjoutRequest;
import com.example.contentieux_security.dto.GarantieAjoutRequest;
import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.entity.Garantie;
import com.example.contentieux_security.entity.Validateur;
import com.example.contentieux_security.enums.TypeValidateur;
import com.example.contentieux_security.repository.ClientRepository;
import com.example.contentieux_security.repository.DossierRepository;
import com.example.contentieux_security.repository.GarantieRepository;
import com.example.contentieux_security.repository.RisqueRepository;
import com.example.contentieux_security.repository.ValidateurRepository;
import com.example.contentieux_security.service.DossierService;
import com.example.contentieux_security.service.HistoriqueService;
import com.example.contentieux_security.service.NotificationService;
import com.example.contentieux_security.service.RisqueService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.contentieux_security.entity.Risque;
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
    private final NotificationService  notificationService;

    private final GarantieRepository garantieRepository;
   
    private final RisqueRepository risqueRepository;
    private final RisqueService risqueService;

    

    
    
    // =====================================================
    // 📌 LISTE DES DOSSIERS (AGENT)
    // =====================================================
  //  @GetMapping("/agent/dossiers")
//    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
//    public String listeDossiers(Model model, Principal principal) {
//
//        String username = principal.getName();
//
//        // Récupérer les dossiers de l’agent connecté
//        model.addAttribute("dossiers", dossierService.getDossiersAgent(username));
//
//        return "agent/dossiers/list";
//    }

// ── Remplacer la méthode listeDossiers dans DossierController.java ──


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
                                @Valid @ModelAttribute RisqueAjoutRequest request,
                                BindingResult bindingResult,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        
        // Validation des erreurs
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .findFirst()
                    .orElse("Données invalides");
            redirectAttributes.addFlashAttribute("error", errorMsg);
            return "redirect:/agent/dossiers/" + dossierId;
        }
    
        // Vérification Principal
        if (principal == null || principal.getName() == null) {
            redirectAttributes.addFlashAttribute("error", "Session expirée");
            return "redirect:/login";
        }
    
        try {
            dossierService.ajouterRisque(dossierId, request, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Risque ajouté avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        
        return "redirect:/agent/dossiers/" + dossierId;
    }
    // ════════════════════════════════════════════════════
    //  AJOUTER UNE GARANTIE
    // ════════════════════════════════════════════════════

    @PostMapping("/agent/dossiers/risques/{risqueId}/garanties/ajouter")
    public String ajouterGarantieSimple(@PathVariable Long risqueId,
                                         @RequestParam String typeGarantie,
                                         @RequestParam(required = false) String description,
                                         @RequestParam(required = false) Double valeurEstimee,
                                         @RequestParam(required = false) String documentRef,
                                         RedirectAttributes redirectAttributes) {
    
        Risque risque = risqueRepository.findById(risqueId)
                .orElseThrow(() -> new RuntimeException("Risque introuvable"));
    
        Garantie garantie = new Garantie();
        garantie.setTypeGarantie(typeGarantie);
        garantie.setDescription(description);
        garantie.setValeurEstimee(valeurEstimee);
        garantie.setDocumentRef(documentRef);
        garantie.setRisque(risque);
    
        garantieRepository.save(garantie);
    
        redirectAttributes.addFlashAttribute("success", "Garantie ajoutée");
    
        return "redirect:/agent/dossiers/" + risque.getDossier().getId();
    }
// ── Remplacer la méthode listeDossiers dans DossierController.java ──

    @GetMapping("/agent/dossiers")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String listeDossiers(Model model, Principal principal,
                                @RequestParam(required = false) String recherche) {

        String username = principal.getName();

        List<DossierContentieux> dossiers = dossierService.rechercherDossiers(username, recherche);

        model.addAttribute("dossiers", dossiers);
        model.addAttribute("recherche", recherche != null ? recherche : "");

        // Stats rapides
        model.addAttribute("totalDossiers", dossiers.size());

        return "agent/dossiers/list";
    }




    



// ══════════════════════════════════════════════════════
//  AGENT — Risques & Garanties
// ══════════════════════════════════════════════════════




/**
 * Sélectionne le risque principal du dossier.
 */
@PostMapping("/agent/dossiers/risques/{risqueId}/selectionner")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
public String selectionnerRisque(@PathVariable Long risqueId,
                                 @RequestHeader(value = "Referer", required = false)
                                 String referer,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
    try {
        dossierService.selectionnerRisque(risqueId, risqueId, false, principal.getName());
        redirectAttributes.addFlashAttribute("success",
                "Crédit sélectionné pour la procédure.");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    return referer != null ? "redirect:" + referer : "redirect:/agent/dossiers";
}


/**
 * Formulaire d'édition d'une garantie.
 */
@GetMapping("/agent/dossiers/garanties/{gId}/edit")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
public String formulaireEditGarantie(@PathVariable Long gId,
                                      Model model,
                                      Principal principal) {
    try {
        Garantie g = garantieRepository.findByIdWithRisqueAndDossier(gId)
                .orElseThrow(() -> new RuntimeException(
                        "Garantie introuvable : " + gId));
        model.addAttribute("garantie", g);
        model.addAttribute("dossierId", g.getRisque().getDossier().getId());
        model.addAttribute("risqueId", g.getRisque().getId()); // ✅ Ajouter cette ligne

        return "agent/dossiers/edit-garantie";
    } catch (Exception e) {
        System.err.println("=== ERREUR editGarantie : " + e.getMessage());
        e.printStackTrace();
        return "redirect:/agent/dossiers";
    }
}



/**
 * Sauvegarde les modifications d'une garantie.
 */
@PostMapping("/agent/dossiers/garanties/{gId}/edit")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
public String modifierGarantie(@PathVariable Long gId,
                                @RequestParam String typeGarantie,
                                
                                @RequestParam(required = false) Double valeurEstimee,
                                @RequestParam(required = false) String description,
                                @RequestParam(required = false) String documentRef,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
    try {
        dossierService.modifierGarantie(gId, typeGarantie, description,
                                         valeurEstimee, documentRef,
                                         principal.getName());
        redirectAttributes.addFlashAttribute("success",
                "Garantie modifiée avec succès.");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    Garantie g = garantieRepository.findByIdWithRisqueAndDossier(gId)
            .orElseThrow(() -> new RuntimeException(
                    "Garantie introuvable : " + gId));
    return "redirect:/agent/dossiers/"
            + g.getRisque().getDossier().getId() + "/edit";
}

/**
 * Supprime une garantie.
 */
@PostMapping("/agent/dossiers/garanties/{gId}/supprimer")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
public String supprimerGarantie(@PathVariable Long gId,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
    try {
        Garantie g = garantieRepository.findByIdWithRisqueAndDossier(gId)
                .orElseThrow(() -> new RuntimeException(
                        "Garantie introuvable : " + gId));
        Long dossierId = g.getRisque().getDossier().getId();
        garantieRepository.deleteById(gId);
        redirectAttributes.addFlashAttribute("success", "Garantie supprimée.");
        return "redirect:/agent/dossiers/" + dossierId + "/edit";
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/agent/dossiers";
    }
}



@GetMapping("/agent/dossiers/risques/{risqueId}/garanties/edit/{id}")
public String editGarantie(@PathVariable Long id, Model model) {

    Garantie garantie = garantieRepository.findById(id)
            .orElse(new Garantie()); // IMPORTANT

    model.addAttribute("garantie", garantie);

    return "agent/dossiers/edit-garantie";
}
@GetMapping("/agent/dossiers/risques/{risqueId}/edit")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
public String editRisque(@PathVariable Long risqueId, Model model) {

    Risque risque = risqueRepository.findById(risqueId)
            .orElseThrow(() -> new RuntimeException("Risque introuvable"));

    model.addAttribute("risque", risque);
    model.addAttribute("garanties", risque.getGaranties());

    return "agent/dossiers/edit-garantie";
}
}