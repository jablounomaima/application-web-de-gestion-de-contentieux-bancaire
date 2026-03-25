package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.DossierCreationRequest;
import com.example.contentieux_security.dto.DossierDetailDTO;
import com.example.contentieux_security.dto.GarantieAjoutRequest;
import com.example.contentieux_security.dto.RisqueAjoutRequest;
import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.entity.Garantie;
import com.example.contentieux_security.enums.TypePrestataire;
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

    // ── Dépendances injectées ─────────────────────────────
    private final DossierService        dossierService;
    private final HistoriqueService     historiqueService;
    private final GarantieRepository    garantieRepository;
    private final DossierRepository     dossierRepository;
    private final PrestataireRepository prestataireRepository;
    private final NotificationService   notificationService;

    // ══════════════════════════════════════════════════════
    //  AGENT — Gestion des dossiers
    // ══════════════════════════════════════════════════════

    /**
     * Liste tous les dossiers de l'agent connecté.
     * Injecte les stats et le badge de notifications non lues.
     */
    @GetMapping("/agent/dossiers")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String listeDossiers(Model model, Principal principal) {
        String username = principal.getName();
        List<DossierContentieux> dossiers = dossierService.getDossiersAgent(username);

        model.addAttribute("dossiers",        dossiers);
        model.addAttribute("givenName",        username);
        model.addAttribute("totalDossiers",    dossiers.size());
        model.addAttribute("dossiersOuverts",
            dossiers.stream().filter(d -> d.getStatut() == DossierStatus.OUVERT).count());
        model.addAttribute("dossiersEnAttente",
            dossiers.stream().filter(d -> d.getStatut() == DossierStatus.EN_TRAITEMENT).count());
        model.addAttribute("dossiersValides",
            dossiers.stream().filter(d -> d.getStatut() == DossierStatus.VALIDE).count());

        // Badge 🔔 — nombre de notifications non lues pour l'agent
        model.addAttribute("notifCount",
            notificationService.countNonLues(username));

        return "agent/dossiers/list";
    }

    /**
     * Formulaire de création d'un nouveau dossier.
     */
    @GetMapping("/agent/dossiers/create")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String formulaireCreation(Model model) {
        model.addAttribute("dossierRequest", new DossierCreationRequest());
        return "agent/dossiers/create";
    }

    /**
     * Traitement de la création du dossier.
     * Redirige vers le détail du dossier créé.
     */
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

    /**
     * Détail complet d'un dossier.
     * Charge aussi les listes de validateurs disponibles pour l'assignation.
     */
    @GetMapping("/agent/dossiers/{id}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String detailDossier(@PathVariable Long id, Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            DossierDetailDTO dossier = dossierService.getDossierDetail(id);
            model.addAttribute("dossier", dossier);

            // Listes des validateurs disponibles (depuis table Prestataire)
            var vf = prestataireRepository
                    .findByTypeAndActifTrue(TypePrestataire.VALIDATEUR_FINANCIER);
            var vj = prestataireRepository
                    .findByTypeAndActifTrue(TypePrestataire.VALIDATEUR_JURIDIQUE);

            System.out.println("=== Validateurs financiers : " + vf.size());
            System.out.println("=== Validateurs juridiques : " + vj.size());

            model.addAttribute("validateurs_financiers", vf);
            model.addAttribute("validateurs_juridiques", vj);

            return "agent/dossiers/detail";

        } catch (Exception e) {
            System.err.println("=== ERREUR detailDossier : " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Impossible de charger le dossier : " + e.getMessage());
            return "redirect:/agent/dossiers";
        }
    }

    /**
     * Assigne les validateurs financier et juridique au dossier.
     * Obligatoire avant la soumission à validation.
     */
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

    /**
     * Formulaire d'édition d'un dossier.
     * Accessible uniquement si statut OUVERT ou REJETE
     * (un dossier rejeté peut être corrigé et re-soumis).
     */
    @GetMapping("/agent/dossiers/{id}/edit")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String formulaireEdition(@PathVariable Long id, Model model,
                                    RedirectAttributes redirectAttributes) {
        try {
            DossierDetailDTO dossier = dossierService.getDossierDetail(id);

            // Bloquer la modification si EN_TRAITEMENT ou VALIDE
            if (!dossier.getStatut().equals("OUVERT")
                    && !dossier.getStatut().equals("REJETE")) {
                redirectAttributes.addFlashAttribute("error",
                        "Ce dossier n'est plus modifiable (statut : "
                        + dossier.getStatut() + ").");
                return "redirect:/agent/dossiers/" + id;
            }

            model.addAttribute("dossier", dossier);
            return "agent/dossiers/edit";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/dossiers";
        }
    }

    /**
     * Sauvegarde les modifications du dossier (libellé, description, notes).
     */
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

    /**
     * Supprime un dossier et toutes ses données liées
     * dans l'ordre correct pour respecter les contraintes FK :
     * garanties → risques → historique → notifications → dossier.
     */
    @PostMapping("/agent/dossiers/{id}/supprimer")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String supprimerDossier(@PathVariable Long id,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes) {
        try {
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

    /**
     * Soumet le dossier à validation (statut OUVERT ou REJETE → EN_TRAITEMENT).
     * Envoie une notification aux deux validateurs assignés.
     * Si re-soumission après rejet, réinitialise les décisions précédentes.
     */
    @PostMapping("/agent/dossiers/{id}/soumettre")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String soumettre(@PathVariable Long id, Principal principal,
                            RedirectAttributes redirectAttributes) {
        try {
            dossierService.soumettreAValidation(id, principal.getName());
            redirectAttributes.addFlashAttribute("success",
                    "Dossier soumis à validation. "
                    + "En attente des décisions financière et juridique.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/dossiers/" + id;
    }

    // ══════════════════════════════════════════════════════
    //  AGENT — Risques & Garanties
    // ══════════════════════════════════════════════════════

    /**
     * Ajoute un risque (crédit) au dossier.
     */
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

    /**
     * Sélectionne le risque principal à traiter dans la procédure contentieuse.
     * Un seul risque peut être sélectionné à la fois.
     */
    @PostMapping("/agent/dossiers/risques/{rId}/selectionner")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String selectionnerRisque(@PathVariable Long rId,
                                     @RequestHeader(value = "Referer", required = false) String referer,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {
        try {
            dossierService.selectionnerRisque(rId, principal.getName());
            redirectAttributes.addFlashAttribute("success",
                    "Crédit sélectionné pour la procédure.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return referer != null ? "redirect:" + referer : "redirect:/agent/dossiers";
    }

    /**
     * Ajoute une garantie à un risque existant.
     */
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

    /**
     * Formulaire d'édition d'une garantie existante.
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
                .orElseThrow(() -> new RuntimeException(
                        "Garantie introuvable : " + gId));
        return "redirect:/agent/dossiers/"
                + g.getRisque().getDossier().getId() + "/edit";
    }

    /**
     * Supprime une garantie d'un risque.
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

    // ══════════════════════════════════════════════════════
    //  VALIDATEUR FINANCIER
    // ══════════════════════════════════════════════════════

    /**
     * Liste les dossiers EN_TRAITEMENT assignés au validateur financier connecté.
     * Injecte le badge de notifications non lues.
     */
    @GetMapping("/validateur/financier/dossiers")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    public String dossiersFinancier(Model model, Principal principal) {
        String username = principal.getName();
    
        // ← logs
        System.out.println("=== USERNAME CONNECTÉ : [" + username + "]");
        
        List<DossierContentieux> dossiers =
                dossierService.getDossiersEnAttenteValidationFinanciere(username);
    
        long count = notificationService.countNonLues(username);
        
        System.out.println("=== countNonLues : " + count);
    
        model.addAttribute("dossiers",   dossiers);
        model.addAttribute("givenName",  username);
        model.addAttribute("enAttente",  dossiers.size());
        model.addAttribute("notifCount", count); // ← une seule fois
    
        return "validateur/dossiers-financier";
    }

    /**
     * Détail d'un dossier pour le validateur financier.
     * Injecte le badge de notifications.
     */
    @GetMapping("/validateur/financier/dossiers/{id}")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    public String detailFinancier(@PathVariable Long id, Model model,
                                   Principal principal) {
        model.addAttribute("dossier", dossierService.getDossierDetail(id));

        // Badge 🔔 notifications non lues
        model.addAttribute("notifCount",
                notificationService.countNonLues(principal.getName()));

        return "validateur/detail-financier";
    }

    /**
     * Valide financièrement un dossier.
     * Si les deux validations sont accordées → statut VALIDE.
     * Envoie une notification à l'agent créateur.
     */
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

            // Si validation juridique déjà accordée → dossier entièrement validé
            if (d.isEntierementValide()) d.setStatut(DossierStatus.VALIDE);
            dossierRepository.save(d);

            historiqueService.enregistrer(d, HistoriqueService.VALIDATION_FIN,
                    "Validation financière accordée."
                    + (commentaire != null ? " " + commentaire : ""),
                    principal.getName());

            // Notifier l'agent créateur
            notificationService.notifier(
                    d.getAgentCreateur().getUsername(),
                    "✅ Validation financière accordée",
                    "Le dossier " + d.getNumeroDossier()
                    + " a été validé financièrement par " + principal.getName()
                    + (commentaire != null ? ". Commentaire : " + commentaire : "."),
                    "VALIDATION_FINANCIERE_OK",
                    d
            );

            redirectAttributes.addFlashAttribute("success",
                    "Validation financière accordée ✅");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validateur/financier/dossiers";
    }

    /**
     * Rejette financièrement un dossier → statut REJETE.
     * Envoie une notification à l'agent créateur avec le motif.
     */
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
                    "Rejet financier. Motif : " + commentaire,
                    principal.getName());

            // Notifier l'agent créateur avec le motif de rejet
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

    /**
     * Liste les dossiers EN_TRAITEMENT assignés au validateur juridique connecté.
     * Injecte le badge de notifications non lues.
     */
    @GetMapping("/validateur/juridique/dossiers")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    public String dossiersJuridique(Model model, Principal principal) {
        String username = principal.getName();
        List<DossierContentieux> dossiers =
                dossierService.getDossiersEnAttenteValidationJuridique(username);

        model.addAttribute("dossiers",   dossiers);
        model.addAttribute("givenName",  username);
        model.addAttribute("enAttente",  dossiers.size());

        // Badge 🔔 notifications non lues
        model.addAttribute("notifCount",
                notificationService.countNonLues(username));

        return "validateur/dossiers-juridique";
    }

    /**
     * Détail d'un dossier pour le validateur juridique.
     * Injecte le badge de notifications.
     */
    @GetMapping("/validateur/juridique/dossiers/{id}")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    public String detailJuridique(@PathVariable Long id, Model model,
                                   Principal principal) {
        model.addAttribute("dossier", dossierService.getDossierDetail(id));

        // Badge 🔔 notifications non lues
        model.addAttribute("notifCount",
                notificationService.countNonLues(principal.getName()));

        return "validateur/detail-juridique";
    }

    /**
     * Valide juridiquement un dossier.
     * Si les deux validations sont accordées → statut VALIDE.
     * Envoie une notification à l'agent créateur.
     */
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

            // Si validation financière déjà accordée → dossier entièrement validé
            if (d.isEntierementValide()) d.setStatut(DossierStatus.VALIDE);
            dossierRepository.save(d);

            historiqueService.enregistrer(d, HistoriqueService.VALIDATION_JUR,
                    "Validation juridique accordée."
                    + (commentaire != null ? " " + commentaire : ""),
                    principal.getName());

            // Notifier l'agent créateur
            notificationService.notifier(
                    d.getAgentCreateur().getUsername(),
                    "✅ Validation juridique accordée",
                    "Le dossier " + d.getNumeroDossier()
                    + " a été validé juridiquement par " + principal.getName()
                    + (commentaire != null ? ". Commentaire : " + commentaire : "."),
                    "VALIDATION_JURIDIQUE_OK",
                    d
            );

            redirectAttributes.addFlashAttribute("success",
                    "Validation juridique accordée ✅");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validateur/juridique/dossiers";
    }

    /**
     * Rejette juridiquement un dossier → statut REJETE.
     * Envoie une notification à l'agent créateur avec le motif.
     */
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
                    "Rejet juridique. Motif : " + commentaire,
                    principal.getName());

            // Notifier l'agent créateur avec le motif de rejet
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
}