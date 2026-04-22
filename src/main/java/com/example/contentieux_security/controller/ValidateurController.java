package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.enums.DossierStatus;
import com.example.contentieux_security.repository.DossierRepository;
import com.example.contentieux_security.service.DossierService;
import com.example.contentieux_security.service.HistoriqueService;
import com.example.contentieux_security.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/validateur")
@RequiredArgsConstructor
public class ValidateurController {

    private final DossierService      dossierService;
    private final DossierRepository   dossierRepository;
    private final HistoriqueService   historiqueService;
    private final NotificationService notificationService;

    // ════════════════════════════════════════════════════
    //  DASHBOARDS
    // ════════════════════════════════════════════════════

    @GetMapping("/dashboard-financier")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> dashboardFinancier(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        response.put("username", principal.getName());
        response.put("notifCount", notificationService.countNonLues(principal.getName()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard-juridique")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> dashboardJuridique(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        response.put("username", principal.getName());
        response.put("notifCount", notificationService.countNonLues(principal.getName()));
        return ResponseEntity.ok(response);
    }

    // ════════════════════════════════════════════════════
    //  VALIDATEUR FINANCIER — LISTE
    // ════════════════════════════════════════════════════

    @GetMapping("/financier/dossiers")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> dossiersFinancier(Principal principal, @RequestParam(required = false) String recherche) {
        String username = principal.getName();
        List<DossierContentieux> dossiers = dossierService.getDossiersEnAttenteValidationFinanciere(username);
        
        if (recherche != null && !recherche.trim().isEmpty()) {
            String kw = recherche.trim().toLowerCase();
            dossiers = dossiers.stream()
                    .filter(d -> (d.getNumeroDossier() != null && d.getNumeroDossier().toLowerCase().contains(kw))
                              || (d.getClient() != null && d.getClient().getNom() != null && d.getClient().getNom().toLowerCase().contains(kw)))
                    .toList();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("dossiers", dossiers);
        response.put("enAttente", dossiers.size());
        return ResponseEntity.ok(response);
    }

    // ════════════════════════════════════════════════════
    //  VALIDATEUR FINANCIER — DÉTAIL
    // ════════════════════════════════════════════════════

    @GetMapping("/financier/dossiers/{id}")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detailFinancier(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(dossierService.getDossierDetail(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    //  VALIDATEUR FINANCIER — VALIDER
    // ════════════════════════════════════════════════════

    @PostMapping("/financier/dossiers/{id}/valider")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    @Transactional
    public ResponseEntity<?> validerFinancier(@PathVariable Long id, @RequestBody Map<String, String> body, Principal principal) {
        try {
            String commentaire = body.get("commentaire");
            DossierContentieux d = dossierRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new RuntimeException("Dossier introuvable : " + id));

            d.setValidationFinanciere(true);
            d.setCommentaireFinancier(commentaire);
            d.setValidateurFinancierUsername(principal.getName());

            if (Boolean.TRUE.equals(d.getValidationFinanciere()) && Boolean.TRUE.equals(d.getValidationJuridique())) {
                d.setStatut(DossierStatus.VALIDE);
            }

            dossierRepository.save(d);

            historiqueService.enregistrer(d, HistoriqueService.VALIDATION_FIN,
                    "Validation financière accordée." + (commentaire != null ? " " + commentaire : ""),
                    principal.getName());

            if (d.getAgentCreateur() != null) {
                notificationService.notifier(
                        d.getAgentCreateur().getUsername(),
                        "Validation financière accordée",
                        "Le dossier " + d.getNumeroDossier() + " a été validé financièrement par " + principal.getName()
                        + (commentaire != null ? ". Commentaire : " + commentaire : "."),
                        "VALIDATION_FINANCIERE_OK", d);
            }

            return ResponseEntity.ok(Map.of("message", "Validation financière accordée."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    //  VALIDATEUR FINANCIER — REJETER
    // ════════════════════════════════════════════════════

    @PostMapping("/financier/dossiers/{id}/rejeter")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    @Transactional
    public ResponseEntity<?> rejeterFinancier(@PathVariable Long id, @RequestBody Map<String, String> body, Principal principal) {
        try {
            String commentaire = body.get("commentaire");
            DossierContentieux d = dossierRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new RuntimeException("Dossier introuvable : " + id));
    
            d.setValidationFinanciere(false);
            d.setCommentaireFinancier(commentaire);
            d.setValidateurFinancierUsername(principal.getName());
            d.setStatut(DossierStatus.EN_TRAITEMENT);
            dossierRepository.save(d);
    
            historiqueService.enregistrer(d, HistoriqueService.REJET_FIN,
                    "Rejet financier. Motif : " + commentaire,
                    principal.getName());
    
            if (d.getAgentCreateur() != null) {
                notificationService.notifier(
                        d.getAgentCreateur().getUsername(),
                        "Dossier rejeté — validation financière",
                        "Le dossier " + d.getNumeroDossier() + " a été rejeté par " + principal.getName() + ". Motif : " + commentaire,
                        "REJET_FINANCIER", d);
            }
    
            return ResponseEntity.ok(Map.of("message", "Dossier rejeté."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    //  VALIDATEUR JURIDIQUE — LISTE
    // ════════════════════════════════════════════════════

    @GetMapping("/juridique/dossiers-juridique")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> dossiersJuridique(Principal principal, @RequestParam(required = false) String recherche) {
        String username = principal.getName();
        List<DossierContentieux> dossiers = dossierService.getDossiersEnAttenteValidationJuridique(username);
        
        if (recherche != null && !recherche.trim().isEmpty()) {
            String kw = recherche.trim().toLowerCase();
            dossiers = dossiers.stream()
                    .filter(d -> (d.getNumeroDossier() != null && d.getNumeroDossier().toLowerCase().contains(kw))
                              || (d.getClient() != null && d.getClient().getNom() != null && d.getClient().getNom().toLowerCase().contains(kw)))
                    .toList();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("dossiers", dossiers);
        response.put("enAttente", dossiers.size());
        return ResponseEntity.ok(response);
    }

    // ════════════════════════════════════════════════════
    //  VALIDATEUR JURIDIQUE — DÉTAIL
    // ════════════════════════════════════════════════════

    @GetMapping("/juridique/dossiers/{id}")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detailJuridique(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(dossierService.getDossierDetail(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    //  VALIDATEUR JURIDIQUE — VALIDER
    // ════════════════════════════════════════════════════

    @PostMapping("/juridique/dossiers/{id}/valider")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    @Transactional
    public ResponseEntity<?> validerJuridique(@PathVariable Long id, @RequestBody Map<String, String> body, Principal principal) {
        try {
            String commentaire = body.get("commentaire");
            DossierContentieux d = dossierRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new RuntimeException("Dossier introuvable : " + id));

            d.setValidationJuridique(true);
            d.setCommentaireJuridique(commentaire);
            d.setValidateurJuridiqueUsername(principal.getName());

            if (Boolean.TRUE.equals(d.getValidationFinanciere()) && Boolean.TRUE.equals(d.getValidationJuridique())) {
                d.setStatut(DossierStatus.VALIDE);
            }

            dossierRepository.save(d);

            historiqueService.enregistrer(d, HistoriqueService.VALIDATION_JUR,
                    "Validation juridique accordée." + (commentaire != null ? " " + commentaire : ""),
                    principal.getName());

            if (d.getAgentCreateur() != null) {
                notificationService.notifier(
                        d.getAgentCreateur().getUsername(),
                        "Validation juridique accordée",
                        "Le dossier " + d.getNumeroDossier() + " a été validé juridiquement par " + principal.getName()
                        + (commentaire != null ? ". Commentaire : " + commentaire : "."),
                        "VALIDATION_JURIDIQUE_OK", d);
            }

            return ResponseEntity.ok(Map.of("message", "Validation juridique accordée."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    //  VALIDATEUR JURIDIQUE — REJETER
    // ════════════════════════════════════════════════════

    @PostMapping("/juridique/dossiers/{id}/rejeter")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    @Transactional
    public ResponseEntity<?> rejeterJuridique(@PathVariable Long id, @RequestBody Map<String, String> body, Principal principal) {
        try {
            String commentaire = body.get("commentaire");
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
                        "Le dossier " + d.getNumeroDossier() + " a été rejeté par " + principal.getName() + ". Motif : " + commentaire,
                        "REJET_JURIDIQUE", d);
            }

            return ResponseEntity.ok(Map.of("message", "Dossier rejeté."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

