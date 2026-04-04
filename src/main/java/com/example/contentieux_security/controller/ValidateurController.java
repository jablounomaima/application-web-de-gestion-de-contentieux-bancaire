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

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ValidateurController {

    private final DossierService      dossierService;
    private final DossierRepository   dossierRepository;
    private final HistoriqueService   historiqueService;
    private final NotificationService notificationService;

    // ════════════════════════════════════════════════════
    //  DASHBOARDS
    // ════════════════════════════════════════════════════

    @GetMapping("/validateur/dashboard-financier")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    @Transactional
    public String dashboardFinancier() {
        return "validateur/dashboard-financier";
    }

    @GetMapping("/validateur/dashboard-juridique")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    @Transactional
    public String dashboardJuridique() {
        return "validateur/dashboard-juridique";
    }

    // ════════════════════════════════════════════════════
    //  VALIDATEUR FINANCIER — LISTE
    // ════════════════════════════════════════════════════

    @GetMapping("/validateur/financier/dossiers")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    @Transactional(readOnly = true)
    public String dossiersFinancier(Model model, Principal principal) {
        String username = principal.getName();
        List<DossierContentieux> dossiers =
                dossierService.getDossiersEnAttenteValidationFinanciere(username);
        model.addAttribute("dossiers",   dossiers);
        model.addAttribute("givenName",  username);
        model.addAttribute("enAttente",  dossiers.size());
        model.addAttribute("notifCount", notificationService.countNonLues(username));
        return "validateur/financier/dossiers-financier";
    }

    // ════════════════════════════════════════════════════
    //  VALIDATEUR FINANCIER — DÉTAIL
    // ════════════════════════════════════════════════════

    @GetMapping("/validateur/financier/dossiers/{id}")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    @Transactional(readOnly = true)
    public String detailFinancier(@PathVariable Long id, Model model,
                                  Principal principal) {
        model.addAttribute("dossier",
                dossierService.getDossierDetail(id));
        model.addAttribute("notifCount",
                notificationService.countNonLues(principal.getName()));
        return "validateur/financier/detail-financier";
    }

    // ════════════════════════════════════════════════════
    //  VALIDATEUR FINANCIER — VALIDER
    // ════════════════════════════════════════════════════

    @PostMapping("/validateur/financier/dossiers/{id}/valider")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    @Transactional
    public String validerFinancier(@PathVariable Long id,
                                   @RequestParam(required = false) String commentaire,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            // ✅ JOIN FETCH pour charger agentCreateur
            DossierContentieux d = dossierRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new RuntimeException("Dossier introuvable : " + id));

            d.setValidationFinanciere(true);
            d.setCommentaireFinancier(commentaire);
            d.setValidateurFinancierUsername(principal.getName());

            // ✅ Vérification explicite des deux booleans
            if (Boolean.TRUE.equals(d.getValidationFinanciere())
                    && Boolean.TRUE.equals(d.getValidationJuridique())) {
                d.setStatut(DossierStatus.VALIDE);
            }

            dossierRepository.save(d);

            historiqueService.enregistrer(d, HistoriqueService.VALIDATION_FIN,
                    "Validation financière accordée."
                    + (commentaire != null ? " " + commentaire : ""),
                    principal.getName());

            if (d.getAgentCreateur() != null) {
                notificationService.notifier(
                        d.getAgentCreateur().getUsername(),
                        "Validation financière accordée",
                        "Le dossier " + d.getNumeroDossier()
                        + " a été validé financièrement par " + principal.getName()
                        + (commentaire != null ? ". Commentaire : " + commentaire : "."),
                        "VALIDATION_FINANCIERE_OK", d);
            }

            redirectAttributes.addFlashAttribute("success",
                    "Validation financière accordée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validateur/financier/dossiers";
    }

    // ════════════════════════════════════════════════════
    //  VALIDATEUR FINANCIER — REJETER
    // ════════════════════════════════════════════════════

    @PostMapping("/validateur/financier/dossiers/{id}/rejeter")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    @Transactional
    public String rejeterFinancier(@PathVariable Long id,
                                   @RequestParam String commentaire,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            DossierContentieux d = dossierRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new RuntimeException("Dossier introuvable : " + id));
    
            d.setValidationFinanciere(false);
            d.setCommentaireFinancier(commentaire);
            d.setValidateurFinancierUsername(principal.getName());
    
            // ❗ IMPORTANT : NE PAS mettre REJETE directement
            d.setStatut(DossierStatus.EN_TRAITEMENT);
    
            dossierRepository.save(d);
    
            historiqueService.enregistrer(d, HistoriqueService.REJET_FIN,
                    "Rejet financier. Motif : " + commentaire,
                    principal.getName());
    
            // ✅ SAFE CHECK
            if (d.getAgentCreateur() != null) {
                notificationService.notifier(
                        d.getAgentCreateur().getUsername(),
                        "Dossier rejeté — validation financière",
                        "Le dossier " + d.getNumeroDossier()
                        + " a été rejeté par " + principal.getName()
                        + ". Motif : " + commentaire,
                        "REJET_FINANCIER", d);
            } else {
                System.err.println("⚠ agentCreateur NULL !");
            }
    
            redirectAttributes.addFlashAttribute("success", "Dossier rejeté.");
    
        } catch (Exception e) {
            e.printStackTrace(); // 🔥 IMPORTANT pour voir l'erreur réelle
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
    
        return "redirect:/validateur/financier/dossiers";
    }
    // ════════════════════════════════════════════════════
    //  VALIDATEUR JURIDIQUE — LISTE
    // ════════════════════════════════════════════════════

    @GetMapping("/validateur/juridique/dossiers-juridique")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    @Transactional(readOnly = true)
    public String dossiersJuridique(Model model, Principal principal) {
        String username = principal.getName();
        List<DossierContentieux> dossiers =
                dossierService.getDossiersEnAttenteValidationJuridique(username);
        model.addAttribute("dossiers",   dossiers);
        model.addAttribute("givenName",  username);
        model.addAttribute("enAttente",  dossiers.size());
        model.addAttribute("notifCount", notificationService.countNonLues(username));
        return "validateur/juridique/dossiers-juridique";
    }

    // ════════════════════════════════════════════════════
    //  VALIDATEUR JURIDIQUE — DÉTAIL
    // ════════════════════════════════════════════════════

    @GetMapping("/validateur/juridique/dossiers/{id}")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    @Transactional(readOnly = true)
    public String detailJuridique(@PathVariable Long id, Model model,
                                  Principal principal) {
        model.addAttribute("dossier",
                dossierService.getDossierDetail(id));
        model.addAttribute("notifCount",
                notificationService.countNonLues(principal.getName()));
        return "validateur/juridique/detail-juridique";
    }

    // ════════════════════════════════════════════════════
    //  VALIDATEUR JURIDIQUE — VALIDER
    // ════════════════════════════════════════════════════

    @PostMapping("/validateur/juridique/dossiers/{id}/valider")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    @Transactional
    public String validerJuridique(@PathVariable Long id,
                                   @RequestParam(required = false) String commentaire,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            // ✅ JOIN FETCH pour charger agentCreateur
            DossierContentieux d = dossierRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new RuntimeException("Dossier introuvable : " + id));

            d.setValidationJuridique(true);
            d.setCommentaireJuridique(commentaire);
            d.setValidateurJuridiqueUsername(principal.getName());

            // ✅ Vérification explicite des deux booleans
            if (Boolean.TRUE.equals(d.getValidationFinanciere())
                    && Boolean.TRUE.equals(d.getValidationJuridique())) {
                d.setStatut(DossierStatus.VALIDE);
            }

            dossierRepository.save(d);

            historiqueService.enregistrer(d, HistoriqueService.VALIDATION_JUR,
                    "Validation juridique accordée."
                    + (commentaire != null ? " " + commentaire : ""),
                    principal.getName());

            if (d.getAgentCreateur() != null) {
                notificationService.notifier(
                        d.getAgentCreateur().getUsername(),
                        "Validation juridique accordée",
                        "Le dossier " + d.getNumeroDossier()
                        + " a été validé juridiquement par " + principal.getName()
                        + (commentaire != null ? ". Commentaire : " + commentaire : "."),
                        "VALIDATION_JURIDIQUE_OK", d);
            }

            redirectAttributes.addFlashAttribute("success",
                    "Validation juridique accordée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validateur/juridique/dossiers-juridique";
    }

    // ════════════════════════════════════════════════════
    //  VALIDATEUR JURIDIQUE — REJETER
    // ════════════════════════════════════════════════════

    @PostMapping("/validateur/juridique/dossiers/{id}/rejeter")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    @Transactional
    public String rejeterJuridique(@PathVariable Long id,
                                   @RequestParam String commentaire,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            DossierContentieux d = dossierRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new RuntimeException("Dossier introuvable : " + id));

            d.setValidationJuridique(false);
            d.setCommentaireJuridique(commentaire);
            d.setValidateurJuridiqueUsername(principal.getName());
            if (Boolean.FALSE.equals(d.getValidationFinanciere())) {
                d.setStatut(DossierStatus.REJETE);
            } else {
                d.setStatut(DossierStatus.EN_CORRECTION);
            }
            dossierRepository.save(d);

            historiqueService.enregistrer(d, HistoriqueService.REJET_JUR,
                    "Rejet juridique. Motif : " + commentaire,
                    principal.getName());

            if (d.getAgentCreateur() != null) {
                notificationService.notifier(
                        d.getAgentCreateur().getUsername(),
                        "Dossier rejeté — validation juridique",
                        "Le dossier " + d.getNumeroDossier()
                        + " a été rejeté par " + principal.getName()
                        + ". Motif : " + commentaire,
                        "REJET_JURIDIQUE", d);
            }

            redirectAttributes.addFlashAttribute("success", "Dossier rejeté.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validateur/juridique/dossiers-juridique";
    }
}

