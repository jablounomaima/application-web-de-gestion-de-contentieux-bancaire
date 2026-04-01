package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.enums.DossierStatus;
import com.example.contentieux_security.repository.DossierRepository;
import com.example.contentieux_security.service.DossierService;
import com.example.contentieux_security.service.HistoriqueService;
import com.example.contentieux_security.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ValidateurController {

    private final DossierService      dossierService;
    private final DossierRepository   dossierRepository;
    private final HistoriqueService   historiqueService;
    private final NotificationService notificationService;



    @GetMapping("/validateur/dashboard-financier")
    public String dashboardFinancier() {
        return "validateur/dashboard-financier"; // sans .html
    }

    @GetMapping("/validateur/dashboard-juridique")
    public String dashboardJuridique() {
        return "validateur/dashboard-juridique";
    }

    // ══════════════════════════════════════════════════════
    //  VALIDATEUR FINANCIER
    // ══════════════════════════════════════════════════════

    /**
     * Liste des dossiers EN_TRAITEMENT assignés au validateur financier connecté.
     */
    @GetMapping("/validateur/financier/dossiers")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    public String dossiersFinancier(Model model, Principal principal) {
        String username = principal.getName();
        List<DossierContentieux> dossiers =
                dossierService.getDossiersEnAttenteValidationFinanciere(username);

        model.addAttribute("dossiers",   dossiers);
        model.addAttribute("givenName",  username);
        model.addAttribute("enAttente",  dossiers.size());
        model.addAttribute("notifCount",
                notificationService.countNonLues(username));

                return "validateur/financier/dossiers-financier";
    }

    /**
     * Détail d'un dossier pour le validateur financier.
     */
    @GetMapping("/validateur/financier/dossiers/{id}")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    public String detailFinancier(@PathVariable Long id, Model model,
                                   Principal principal) {
        model.addAttribute("dossier",
                dossierService.getDossierDetail(id));
        model.addAttribute("notifCount",
                notificationService.countNonLues(principal.getName()));
        return "validateur/financier/detail-financier";
    }

    /**
     * Valide financièrement un dossier.
     * Si les deux validations sont accordées → statut VALIDE.
     * Envoie une notification à l'agent créateur.
     */
    @PostMapping("/validateur/financier/dossiers/{id}/valider")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    @Transactional 
    public String validerFinancier(@PathVariable Long id,
                                   @RequestParam(required = false) String commentaire,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            DossierContentieux d = dossierService.getDossierById(id);
    
            // ← logs temporaires
            System.out.println("=== validerFinancier id=" + id);
            System.out.println("=== agentCreateur="
                    + (d.getAgentCreateur() != null
                       ? d.getAgentCreateur().getUsername()
                       : "NULL ⚠"));
    
            d.setValidationFinanciere(true);
            d.setCommentaireFinancier(commentaire);
            d.setValidateurFinancierUsername(principal.getName());
            if (d.isEntierementValide()) d.setStatut(DossierStatus.VALIDE);
            dossierRepository.save(d);
    
            historiqueService.enregistrer(d, HistoriqueService.VALIDATION_FIN,
                    "Validation financière accordée."
                    + (commentaire != null ? " " + commentaire : ""),
                    principal.getName());
    
            // ✅ Vérifier que agentCreateur n'est pas null avant notifier
            if (d.getAgentCreateur() != null) {
                notificationService.notifier(
                        d.getAgentCreateur().getUsername(),
                        "✅ Validation financière accordée",
                        "Le dossier " + d.getNumeroDossier()
                        + " a été validé financièrement par " + principal.getName()
                        + (commentaire != null ? ". Commentaire : " + commentaire : "."),
                        "VALIDATION_FINANCIERE_OK", d);
                System.out.println("=== Notification envoyée à : "
                        + d.getAgentCreateur().getUsername());
            } else {
                System.err.println("=== ⚠ agentCreateur est NULL — notification non envoyée");
            }
    
            redirectAttributes.addFlashAttribute("success",
                    "Validation financière accordée ✅");
        } catch (Exception e) {
            System.err.println("=== ERREUR validerFinancier : " + e.getMessage());
            e.printStackTrace();
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
                    "REJET_FINANCIER", d);

            redirectAttributes.addFlashAttribute("success", "Dossier rejeté ❌");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validateur/financier/dossiers-financier";
    }

    // ══════════════════════════════════════════════════════
    //  VALIDATEUR JURIDIQUE
    // ══════════════════════════════════════════════════════

    /**
     * Liste des dossiers EN_TRAITEMENT assignés au validateur juridique connecté.
     */
    @GetMapping("/validateur/juridique/dossiers-juridique")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    public String dossiersJuridique(Model model, Principal principal) {
        String username = principal.getName();
        List<DossierContentieux> dossiers =
                dossierService.getDossiersEnAttenteValidationJuridique(username);

        model.addAttribute("dossiers",   dossiers);
        model.addAttribute("givenName",  username);
        model.addAttribute("enAttente",  dossiers.size());
        model.addAttribute("notifCount",
                notificationService.countNonLues(username));

        return "validateur/juridique/dossiers-juridique";
    }

    /**
     * Détail d'un dossier pour le validateur juridique.
     */
    @GetMapping("/validateur/juridique/dossiers/{id}")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    public String detailJuridique(@PathVariable Long id, Model model,
                                   Principal principal) {
        model.addAttribute("dossier",
                dossierService.getDossierDetail(id));
        model.addAttribute("notifCount",
                notificationService.countNonLues(principal.getName()));
        return "validateur/juridique/detail-juridique";
    }

    /**
     * Valide juridiquement un dossier.
     * Si les deux validations sont accordées → statut VALIDE.
     * Envoie une notification à l'agent créateur.
     */
    @PostMapping("/validateur/juridique/dossiers/{id}/valider")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    @Transactional 
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
                    "VALIDATION_JURIDIQUE_OK", d);

            redirectAttributes.addFlashAttribute("success",
                    "Validation juridique accordée ✅");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validateur/juridique/dossiers-juridique";
    }

    /**
     * Rejette juridiquement un dossier → statut REJETE.
     * Envoie une notification à l'agent créateur avec le motif.
     */
    @PostMapping("/validateur/juridique/dossiers/{id}/rejeter")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    @Transactional
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
                    "REJET_JURIDIQUE", d);

            redirectAttributes.addFlashAttribute("success", "Dossier rejeté ❌");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validateur/juridique/dossiers-juridique";
    }

    @GetMapping("/validateur/juridique/dossiers")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    @Transactional
    public String dossiersJuridique(Model model, Principal principal) {
        String username = principal.getName();
        List<DossierContentieux> dossiers =
                dossierService.getDossiersEnAttenteValidationJuridique(username);
    
        model.addAttribute("dossiers",   dossiers);
        model.addAttribute("givenName",  username);
        model.addAttribute("enAttente",  dossiers.size());
        model.addAttribute("notifCount",
                notificationService.countNonLues(username));
    
        return "validateur/juridique/dossiers-juridique";
    }




}