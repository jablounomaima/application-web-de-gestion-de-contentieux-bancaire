package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.DossierCreationRequest;
import com.example.contentieux_security.dto.DossierDetailDTO;
import com.example.contentieux_security.dto.GarantieAjoutRequest;
import com.example.contentieux_security.dto.RisqueAjoutRequest;
import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.entity.Garantie;
import com.example.contentieux_security.entity.TypePrestataire;
import com.example.contentieux_security.enums.DossierStatus;
import com.example.contentieux_security.repository.DossierRepository;
import com.example.contentieux_security.repository.GarantieRepository;
import com.example.contentieux_security.repository.PrestataireRepository;
import com.example.contentieux_security.service.DossierService;
import com.example.contentieux_security.service.HistoriqueService;
import com.example.contentieux_security.service.NotificationService;

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

    private final DossierService        dossierService;
    private final HistoriqueService     historiqueService;
    private final GarantieRepository    garantieRepository;
    private final DossierRepository     dossierRepository;
    private final PrestataireRepository prestataireRepository;
    private final NotificationService   notificationService; // ← ajouter


    // ══════════════════════════════════════════════════════
    //  AGENT — Dossiers
    // ══════════════════════════════════════════════════════
    @GetMapping("/agent/dossiers")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String listeDossiers(Model model, Principal principal) {
        String username = principal.getName();
        List<DossierContentieux> dossiers = dossierService.getDossiersAgent(username);
        model.addAttribute("dossiers",         dossiers);
        model.addAttribute("givenName",         username);
        model.addAttribute("totalDossiers",     dossiers.size());
        model.addAttribute("dossiersOuverts",
            dossiers.stream().filter(d -> d.getStatut() == DossierStatus.OUVERT).count());
        model.addAttribute("dossiersEnAttente",
            dossiers.stream().filter(d -> d.getStatut() == DossierStatus.EN_TRAITEMENT).count());
        model.addAttribute("dossiersValides",
            dossiers.stream().filter(d -> d.getStatut() == DossierStatus.VALIDE).count());
    
        // ✅ Badge notifications non lues
        model.addAttribute("notifCount",
            notificationService.countNonLues(username));
    
        return "agent/dossiers/list";
    }

    @GetMapping("/agent/dossiers/create")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String formulaireCreation(Model model) {
        model.addAttribute("dossierRequest", new DossierCreationRequest());
        return "agent/dossiers/create";
    }

    @PostMapping("/agent/dossiers/creer")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String creerDossier(@ModelAttribute DossierCreationRequest request,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            DossierContentieux d = dossierService.creerDossier(request, principal.getName());
            redirectAttributes.addFlashAttribute("success",
                    "Dossier " + d.getNumeroDossier() + " créé avec succès !");
            return "redirect:/agent/dossiers/" + d.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/dossiers/create";
        }
    }

    @GetMapping("/agent/dossiers/{id}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String detailDossier(@PathVariable Long id, Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            DossierDetailDTO dossier = dossierService.getDossierDetail(id);
            model.addAttribute("dossier", dossier);

            var vf = prestataireRepository
                    .findByTypeAndActifTrue(TypePrestataire.VALIDATEUR_FINANCIER);
            var vj = prestataireRepository
                    .findByTypeAndActifTrue(TypePrestataire.VALIDATEUR_JURIDIQUE);

            System.out.println("=== Validateurs financiers : " + vf.size());
            System.out.println("=== Validateurs juridiques : " + vj.size());

            model.addAttribute("validateurs_financiers", vf);
            model.addAttribute("validateurs_juridiques", vj);

            return "agent/dossiers/detail";

        } catch (Exception e) {  // ✅ accolade fermante manquante — corrigée
            System.err.println("=== ERREUR detailDossier : " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Impossible de charger le dossier : " + e.getMessage());
            return "redirect:/agent/dossiers";
        }
    } // ✅ fin de detailDossier

    @PostMapping("/agent/dossiers/{id}/choisir-validateurs")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String choisirValidateurs(@PathVariable Long id,
                                      @RequestParam String validateurFinancier,
                                      @RequestParam String validateurJuridique,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        try {
            dossierService.choisirValidateurs(id, validateurFinancier,
                                               validateurJuridique, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Validateurs assignés avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/dossiers/" + id;
    }

    @GetMapping("/agent/dossiers/{id}/edit")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String formulaireEdition(@PathVariable Long id, Model model,
                                    RedirectAttributes redirectAttributes) {
        try {
            DossierDetailDTO dossier = dossierService.getDossierDetail(id);
            if (!dossier.getStatut().equals("OUVERT")) {
                redirectAttributes.addFlashAttribute("error",
                        "Ce dossier n'est plus modifiable (statut : " + dossier.getStatut() + ").");
                return "redirect:/agent/dossiers/" + id;
            }
            model.addAttribute("dossier", dossier);
            return "agent/dossiers/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/dossiers";
        }
    }

    @PostMapping("/agent/dossiers/{id}/edit")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String modifierDossier(@PathVariable Long id,
                                  @RequestParam String libelle,
                                  @RequestParam(required = false) String description,
                                  @RequestParam(required = false) String notes,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        try {
            DossierContentieux d = dossierService.getDossierByIdAndAgent(id, principal.getName());
            d.setLibelle(libelle);
            d.setDescription(description);
            d.setNotes(notes);
            dossierRepository.save(d);
            redirectAttributes.addFlashAttribute("success", "Dossier modifié avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/dossiers/" + id;
    }



    @PostMapping("/agent/dossiers/{id}/supprimer")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String supprimerDossier(@PathVariable Long id,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes) {
        try {
            // ✅ Utiliser la méthode service qui gère les FK
            DossierContentieux d = dossierService.getDossierById(id);
            String numero = d.getNumeroDossier();
            dossierService.supprimerDossier(id, principal.getName());
            redirectAttributes.addFlashAttribute("success",
                    "Dossier " + numero + " supprimé avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/dossiers";
    }

    @PostMapping("/agent/dossiers/{id}/soumettre")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String soumettre(@PathVariable Long id, Principal principal,
                            RedirectAttributes redirectAttributes) {
        try {
            dossierService.soumettreAValidation(id, principal.getName());
            redirectAttributes.addFlashAttribute("success",
                    "Dossier soumis à validation. En attente des décisions financière et juridique.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/dossiers/" + id;
    }

    // ══════════════════════════════════════════════════════
    //  AGENT — Risques & Garanties
    // ══════════════════════════════════════════════════════

    @PostMapping("/agent/dossiers/{id}/risques/ajouter")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String ajouterRisque(@PathVariable Long id,
                                @ModelAttribute RisqueAjoutRequest request,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            dossierService.ajouterRisque(id, request, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Crédit ajouté.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/dossiers/" + id + "/edit";
    }

    @PostMapping("/agent/dossiers/risques/{rId}/selectionner")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String selectionnerRisque(@PathVariable Long rId,
                                     @RequestHeader(value = "Referer", required = false) String referer,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {
        try {
            dossierService.selectionnerRisque(rId, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Crédit sélectionné pour la procédure.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return referer != null ? "redirect:" + referer : "redirect:/agent/dossiers";
    }

    @PostMapping("/agent/dossiers/risques/{rId}/garanties/ajouter")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String ajouterGarantie(@PathVariable Long rId,
                                  @ModelAttribute GarantieAjoutRequest request,
                                  @RequestHeader(value = "Referer", required = false) String referer,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        try {
            dossierService.ajouterGarantie(rId, request, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Garantie ajoutée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return referer != null ? "redirect:" + referer : "redirect:/agent/dossiers";
    }

    @GetMapping("/agent/dossiers/garanties/{gId}/edit")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String formulaireEditGarantie(@PathVariable Long gId,
                                          Model model,
                                          Principal principal) {
        try {
            Garantie g = garantieRepository.findByIdWithRisqueAndDossier(gId)
                    .orElseThrow(() -> new RuntimeException("Garantie introuvable : " + gId));
            model.addAttribute("garantie", g);
            model.addAttribute("dossierId", g.getRisque().getDossier().getId());
            return "agent/dossiers/edit-garantie";
        } catch (Exception e) {
            System.err.println("=== ERREUR editGarantie : " + e.getMessage());
            e.printStackTrace();
            return "redirect:/agent/dossiers";
        }
    }

    @PostMapping("/agent/dossiers/garanties/{gId}/edit")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String modifierGarantie(@PathVariable Long gId,
                                    @RequestParam String typeGarantie,
                                    @RequestParam(required = false) String description,
                                    @RequestParam(required = false) Double valeurEstimee,
                                    @RequestParam(required = false) String documentRef,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes) {
        try {
            dossierService.modifierGarantie(gId, typeGarantie, description,
                                             valeurEstimee, documentRef, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Garantie modifiée avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        Garantie g = garantieRepository.findByIdWithRisqueAndDossier(gId)
                .orElseThrow(() -> new RuntimeException("Garantie introuvable : " + gId));
        return "redirect:/agent/dossiers/" + g.getRisque().getDossier().getId() + "/edit";
    }

    @PostMapping("/agent/dossiers/garanties/{gId}/supprimer")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String supprimerGarantie(@PathVariable Long gId,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {
        try {
            Garantie g = garantieRepository.findByIdWithRisqueAndDossier(gId)
                    .orElseThrow(() -> new RuntimeException("Garantie introuvable : " + gId));
            Long dossierId = g.getRisque().getDossier().getId();
            garantieRepository.deleteById(gId);
            redirectAttributes.addFlashAttribute("success", "Garantie supprimée.");
            return "redirect:/agent/dossiers/" + dossierId + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/dossiers";
        }
    }

    // ══════════════════════════════════════════════════════
    //  VALIDATEUR FINANCIER
    // ══════════════════════════════════════════════════════

    @GetMapping("/validateur/financier/dossiers")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    public String dossiersFinancier(Model model, Principal principal) {
        model.addAttribute("dossiers",
                dossierService.getDossiersEnAttenteValidationFinanciere(principal.getName()));
        return "validateur/dossiers-financier";
    }

    @GetMapping("/validateur/financier/dossiers/{id}")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    public String detailFinancier(@PathVariable Long id, Model model) {
        model.addAttribute("dossier", dossierService.getDossierDetail(id));
        return "validateur/detail-financier";
    }

    @PostMapping("/validateur/financier/dossiers/{id}/valider")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    public String validerFinancier(@PathVariable Long id,
                                   @RequestParam(required = false) String commentaire,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            DossierContentieux d = dossierService.getDossierById(id);
            d.setValidationFinanciere(true);
            d.setCommentaireFinancier(commentaire);
            d.setValidateurFinancierUsername(principal.getName());
            if (d.isEntierementValide()) d.setStatut(DossierStatus.VALIDE);
            dossierRepository.save(d);
    
            historiqueService.enregistrer(d, HistoriqueService.VALIDATION_FIN,
                    "Validation financière accordée."
                    + (commentaire != null ? " " + commentaire : ""),
                    principal.getName());
    
            // ✅ Notifier l'agent créateur
            notificationService.notifier(
                    d.getAgentCreateur().getUsername(),
                    "✅ Validation financière accordée",
                    "Le dossier " + d.getNumeroDossier()
                    + " a été validé financièrement par " + principal.getName()
                    + (commentaire != null ? ". Commentaire : " + commentaire : "."),
                    "VALIDATION_FINANCIERE_OK",
                    d
            );
    
            redirectAttributes.addFlashAttribute("success", "Validation financière accordée ✅");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validateur/financier/dossiers";
    }
    
    @PostMapping("/validateur/financier/dossiers/{id}/rejeter")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    public String rejeterFinancier(@PathVariable Long id,
                                   @RequestParam String commentaire,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            DossierContentieux d = dossierService.getDossierById(id);
            d.setValidationFinanciere(false);
            d.setCommentaireFinancier(commentaire);
            d.setValidateurFinancierUsername(principal.getName());
            d.setStatut(DossierStatus.REJETE);
            dossierRepository.save(d);
    
            historiqueService.enregistrer(d, HistoriqueService.REJET_FIN,
                    "Rejet financier. Motif : " + commentaire, principal.getName());
    
            // ✅ Notifier l'agent créateur
            notificationService.notifier(
                    d.getAgentCreateur().getUsername(),
                    "❌ Dossier rejeté — validation financière",
                    "Le dossier " + d.getNumeroDossier()
                    + " a été rejeté par " + principal.getName()
                    + ". Motif : " + commentaire,
                    "REJET_FINANCIER",
                    d
            );
    
            redirectAttributes.addFlashAttribute("success", "Dossier rejeté ❌");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validateur/financier/dossiers";
    }




  

    // ══════════════════════════════════════════════════════
    //  VALIDATEUR JURIDIQUE
    // ══════════════════════════════════════════════════════

    @GetMapping("/validateur/juridique/dossiers")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    public String dossiersJuridique(Model model, Principal principal) {
        model.addAttribute("dossiers",
                dossierService.getDossiersEnAttenteValidationJuridique(principal.getName()));
        return "validateur/dossiers-juridique";
    }

    @GetMapping("/validateur/juridique/dossiers/{id}")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    public String detailJuridique(@PathVariable Long id, Model model) {
        model.addAttribute("dossier", dossierService.getDossierDetail(id));
        return "validateur/detail-juridique";
    }

    @PostMapping("/validateur/juridique/dossiers/{id}/valider")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    public String validerJuridique(@PathVariable Long id,
                                   @RequestParam(required = false) String commentaire,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            DossierContentieux d = dossierService.getDossierById(id);
            d.setValidationJuridique(true);
            d.setCommentaireJuridique(commentaire);
            d.setValidateurJuridiqueUsername(principal.getName());
            if (d.isEntierementValide()) d.setStatut(DossierStatus.VALIDE);
            dossierRepository.save(d);
    
            historiqueService.enregistrer(d, HistoriqueService.VALIDATION_JUR,
                    "Validation juridique accordée."
                    + (commentaire != null ? " " + commentaire : ""),
                    principal.getName());
    
            // ✅ Notifier l'agent créateur
            notificationService.notifier(
                    d.getAgentCreateur().getUsername(),
                    "✅ Validation juridique accordée",
                    "Le dossier " + d.getNumeroDossier()
                    + " a été validé juridiquement par " + principal.getName()
                    + (commentaire != null ? ". Commentaire : " + commentaire : "."),
                    "VALIDATION_JURIDIQUE_OK",
                    d
            );
    
            redirectAttributes.addFlashAttribute("success", "Validation juridique accordée ✅");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validateur/juridique/dossiers";
    }

    @PostMapping("/validateur/juridique/dossiers/{id}/rejeter")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    public String rejeterJuridique(@PathVariable Long id,
                                   @RequestParam String commentaire,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            DossierContentieux d = dossierService.getDossierById(id);
            d.setValidationJuridique(false);
            d.setCommentaireJuridique(commentaire);
            d.setValidateurJuridiqueUsername(principal.getName());
            d.setStatut(DossierStatus.REJETE);
            dossierRepository.save(d);
    
            historiqueService.enregistrer(d, HistoriqueService.REJET_JUR,
                    "Rejet juridique. Motif : " + commentaire, principal.getName());
    
            // ✅ Notifier l'agent créateur
            notificationService.notifier(
                    d.getAgentCreateur().getUsername(),
                    "❌ Dossier rejeté — validation juridique",
                    "Le dossier " + d.getNumeroDossier()
                    + " a été rejeté par " + principal.getName()
                    + ". Motif : " + commentaire,
                    "REJET_JURIDIQUE",
                    d
            );
    
            redirectAttributes.addFlashAttribute("success", "Dossier rejeté ❌");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validateur/juridique/dossiers";
    }
} // ✅ une seule accolade fermante pour la classe