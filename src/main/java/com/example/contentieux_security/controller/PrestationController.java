package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.TypePrestataire;
import com.example.contentieux_security.enums.TypePrestation;
import com.example.contentieux_security.repository.AgentBancaireRepository;
import com.example.contentieux_security.repository.PrestataireRepository;
import com.example.contentieux_security.service.PrestationService;

import jakarta.transaction.Transactional;

import com.example.contentieux_security.service.AffaireJudiciaireService;
import com.example.contentieux_security.service.DossierService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import lombok.extern.slf4j.Slf4j;
import com.example.contentieux_security.entity.AgentBancaire;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
@Slf4j
@Controller
@RequiredArgsConstructor // Injection automatique des dépendances via constructeur
public class PrestationController {

    // Services utilisés pour la logique métier
    private final PrestationService prestationService;
    private final DossierService dossierService;

    // Repository pour accéder aux prestataires
    private final PrestataireRepository prestataireRepository;

    private final AffaireJudiciaireService affaireService; // ← AJOUTER

     private final AgentBancaireRepository agentBancaireRepository;
    // ─────────────────────────────────────────────────────────────────────────
    // 🔹 GET : Afficher le formulaire de lancement d’une prestation
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/agent/dossiers/{dossierId}/prestations/lancer")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')") // Sécurité : seuls AGENT et ADMIN
    
    public String formLancer(@PathVariable Long dossierId, Model model, Principal principal) {

        // Récupération du dossier
        DossierContentieux dossier = dossierService.getDossierById(dossierId);

        // Vérification : le dossier doit être VALIDÉ
        if (!"VALIDE".equals(dossier.getStatut().name())) {
            return "redirect:/agent/dossiers/" + dossierId
                    + "?erreur=Le dossier doit être au statut VALIDE";
        }
        // ✅ Récupérer l'agent connecté
    AgentBancaire agent = agentBancaireRepository
    .findByUsername(principal.getName())
    .orElseThrow(() -> new RuntimeException("Agent introuvable"));

    
        // Ajouter les données au modèle pour Thymeleaf
        model.addAttribute("dossier", dossier);
        model.addAttribute("typesPrestations", TypePrestation.values());

        // Retourner la vue
        return "agent/prestations/lancer";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 🔹 POST : Lancer une prestation
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/agent/dossiers/{dossierId}/prestations/lancer")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public String lancerPrestation(@PathVariable Long dossierId,
                                  @RequestParam TypePrestation type,
                                  @RequestParam(required = false) String description,
                                  Authentication authentication,
                                  RedirectAttributes ra) {
        try {
            // Appel au service métier
            Prestation p = prestationService.lancerPrestation(
                dossierId, type, description, authentication.getName());
            // Message succès
            ra.addFlashAttribute("success",
                    "Prestation " + p.getNumeroPrestation() + " lancée avec succès.");

            // Redirection vers détail prestation
            return "redirect:/agent/dossiers/" + dossierId + "/prestations/" + p.getId();

        } catch (IllegalStateException e) {
            // Cas métier (ex: dossier non valide)
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/dossiers/" + dossierId + "/prestations/lancer";

        } catch (Exception e) {
            // Erreur générale
            ra.addFlashAttribute("error", "Erreur inattendue : " + e.getMessage());
            return "redirect:/agent/dossiers/" + dossierId;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 🔹 GET : Afficher le détail d’une prestation
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/agent/dossiers/{dossierId}/prestations/{prestationId}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public String detailPrestation(@PathVariable Long dossierId,
                                  @PathVariable Long prestationId,
                                  Model model) {

        // Récupération des données
        DossierContentieux dossier = dossierService.getDossierById(dossierId);
        Prestation prestation = prestationService.getPrestationById(prestationId);
        List<Mission> missions = prestationService.getMissionsByPrestation(prestationId);

        // Vérification : la prestation appartient bien au dossier
        if (!prestation.getDossier().getId().equals(dossierId)) {
            return "redirect:/agent/dossiers/" + dossierId
                    + "?erreur=Prestation non liée à ce dossier";
        }

        // Sélection des prestataires selon le type de prestation
        List<Prestataire> prestataires;

        prestataires = prestataireRepository.findByTypeInAndActifTrue(
            List.of(TypePrestataire.AVOCAT, TypePrestataire.EXPERT, TypePrestataire.HUISSIER)
        );

        // Ajouter les données à la vue
        model.addAttribute("dossier", dossier);
        model.addAttribute("prestation", prestation);
        model.addAttribute("missions", missions);
        model.addAttribute("prestataires", prestataires);

        return "agent/prestations/detail";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 🔹 POST : Désigner un prestataire (création mission)
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/agent/dossiers/{dossierId}/prestations/{prestationId}/designer")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public String designerPrestataire(@PathVariable Long dossierId,
                                      @PathVariable Long prestationId,
                                      @RequestParam Long prestataireId,
                                      @RequestParam(required = false) String description,
                                      @RequestParam(required = false) String dateFinPrevue,
                                      Authentication authentication,
                                      RedirectAttributes ra) {
        try {
            LocalDate dateFin = (dateFinPrevue != null && !dateFinPrevue.isBlank())
                    ? LocalDate.parse(dateFinPrevue)
                    : null;
    
            // ── 1. Créer la mission ──────────────────────────────
            Mission m = prestationService.designerPrestataire(
                    prestationId,
                    prestataireId,
                    description,
                    dateFin,
                    authentication.getName()
            );
    
            ra.addFlashAttribute("success",
                    "Mission " + m.getNumeroMission() + " assignée avec succès.");
    
            // ── 2. Créer l'affaire si avocat + procédure judiciaire ──
            Prestation prestation = prestationService.getPrestationById(prestationId);
    
            boolean estAvocat = m.getPrestataire() != null
                    && m.getPrestataire().getType() == TypePrestataire.AVOCAT;
    
            boolean estProcedureJudiciaire =
                    prestation.getType() == TypePrestation.PROCEDURE_JUDICIAIRE;
    
            if (estProcedureJudiciaire && estAvocat) {
                try {
                    // Utiliser creerAffaireDirecte avec les objets déjà en mémoire
                    DossierContentieux dossier = prestation.getDossier();
    
                    if (dossier == null) {
                        // Fallback : recharger le dossier depuis le service
                        dossier = dossierService.getDossierById(dossierId);
                    }
    
                    AffaireJudiciaire affaire = affaireService.creerAffaireDirecte(
                            m,
                            dossier,
                            authentication.getName()
                    );
    
                    ra.addFlashAttribute("success",
                            "Mission " + m.getNumeroMission()
                            + " assignée et affaire judiciaire "
                            + affaire.getNumeroAffaire()
                            + " créée avec succès pour l'avocat "
                            + m.getPrestataire().getUsername() + ".");
    
                } catch (IllegalStateException e) {
                    // Doublon — affaire déjà existante, pas grave
                    log.warn("Affaire déjà existante pour mission {} : {}",
                            m.getId(), e.getMessage());
                    ra.addFlashAttribute("success",
                            "Mission " + m.getNumeroMission()
                            + " assignée (affaire judiciaire déjà existante).");
    
                } catch (Exception e) {
                    log.error("Erreur création affaire pour mission {} : {}",
                            m.getId(), e.getMessage(), e);
                    ra.addFlashAttribute("warning",
                            "Mission créée mais erreur affaire : " + e.getMessage());
                }
    
            } else if (estProcedureJudiciaire && !estAvocat) {
                // Expert ou Huissier sur une procédure judiciaire → mission seulement
                log.info("Prestataire {} ({}) désigné sur procédure judiciaire — pas d'affaire créée",
                        m.getPrestataire() != null ? m.getPrestataire().getUsername() : "NULL",
                        m.getPrestataire() != null ? m.getPrestataire().getType() : "NULL");
            }
    
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "Données invalides : " + e.getMessage());
        } catch (Exception e) {
            log.error("Erreur designerPrestataire dossierId={} prestationId={} : {}",
                    dossierId, prestationId, e.getMessage(), e);
            ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
    
        return "redirect:/agent/dossiers/" + dossierId
                + "/prestations/" + prestationId;
    }
    @Transactional
// Afficher le formulaire de modification du résultat d'une mission






// Soumettre la modification du résultat (commentaire + fichiers)


// Supprimer un fichier du résultat
@PostMapping("/prestataire/missions/fichiers/{fichierId}/supprimer")
@PreAuthorize("hasAnyRole('ROLE_AVOCAT','ROLE_HUISSIER','ROLE_EXPERT')")   // ← Correction principale
public String supprimerFichierResultat(
        @PathVariable Long fichierId,
        @RequestParam Long missionId,
        Authentication authentication,
        RedirectAttributes ra) {

    try {
        prestationService.supprimerFichierResultat(
                fichierId, authentication.getName());
        ra.addFlashAttribute("success", "Fichier supprimé.");
    } catch (SecurityException e) {
        ra.addFlashAttribute("error", "Accès refusé.");
    } catch (Exception e) {
        ra.addFlashAttribute("error",
                "Erreur suppression : " + e.getMessage());
    }

    return "redirect:/prestataire/missions/"
            + missionId + "/resultat/modifier";
}
}