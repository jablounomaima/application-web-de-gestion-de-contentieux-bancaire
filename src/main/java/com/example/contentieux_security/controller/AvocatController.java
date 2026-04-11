package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.AffaireJudiciaire;
import com.example.contentieux_security.entity.Audience;
import com.example.contentieux_security.entity.DocumentAffaire;
import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.entity.ResultatMission;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.service.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Path;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/avocat")
@RequiredArgsConstructor
@Slf4j
public class AvocatController {

    private final PrestationService prestationService;
    private final AffaireJudiciaireService affaireService;
    private final FileStorageService fileStorageService;
    private final ResultatMissionService resultatMissionService; // ← AJOUTÉ

    // ─────────────────────────────────────────────
    //  DASHBOARD
    // ─────────────────────────────────────────────

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('AVOCAT')")
    public String dashboard(Model model, Authentication authentication) {
        String username = authentication.getName();

        List<Mission> missions =
                prestationService.getMissionsPrestataire(authentication.getName());

        long enCours   = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.ASSIGNEE
                          || m.getStatut() == StatutMission.EN_COURS)
                .count();
        long pvSoumis  = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.PV_SOUMIS)
                .count();
        long terminees = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.TERMINEE
                          || m.getStatut() == StatutMission.REALISEE) // ← AJOUTÉ
                .count();

        List<Mission> dernieres = missions.stream().limit(5).toList();
        
        
        

        model.addAttribute("totalMissions",     missions.size());
        model.addAttribute("missionsEnCours",   enCours);
        model.addAttribute("pvSoumis",          pvSoumis);
        model.addAttribute("missionsTerminees", terminees);
        model.addAttribute("dernieresMissions", dernieres);

        return "avocat/dashboard";
    }

    // ─────────────────────────────────────────────
    //  LISTE DES AFFAIRES
    // ─────────────────────────────────────────────

    @GetMapping("/affaires")
    public String mesAffaires(Principal principal, Model model) {
        model.addAttribute("affaires",
                affaireService.getAffairesParAvocat(principal.getName()));
        return "avocat/affaires/liste";
    }

    // ─────────────────────────────────────────────
    //  DÉTAIL AFFAIRE
    // ─────────────────────────────────────────────

    @GetMapping("/affaires/{id}")
    public String detailAffaire(@PathVariable Long id, Model model) {
        AffaireJudiciaire affaire = affaireService.getAffaireById(id);
        model.addAttribute("affaire", affaire);
        model.addAttribute("typesDocument",    DocumentAffaire.TypeDocument.values());
        model.addAttribute("statutsAudience",  Audience.StatutAudience.values());
        model.addAttribute("typesJugement",    AffaireJudiciaire.TypeJugement.values());

        // Résultat de mission existant (null si pas encore soumis)
        ResultatMission resultat =
                resultatMissionService.getResultat(affaire.getMission().getId());
        model.addAttribute("resultatMission", resultat); // ← AJOUTÉ

        return "avocat/affaires/detail";
    }

    // ─────────────────────────────────────────────
    //  AUDIENCES
    // ─────────────────────────────────────────────

    @PostMapping("/avocat/affaires/{id}/audiences/ajouter")
    public String ajouterAudience(@PathVariable Long id,
                                   @RequestParam String dateAudience,
                                   @RequestParam(required = false) String heure,
                                   @RequestParam(required = false) String salle,
                                   @RequestParam(required = false) String motif,
                                   RedirectAttributes ra) {
        try {
            affaireService.ajouterAudience(
                id,
                LocalDate.parse(dateAudience),
                heure, salle, motif
            );
            ra.addFlashAttribute("successMsg", "Audience planifiée avec succès");
        } catch (Exception e) {
            log.error("Erreur ajout audience", e);
            ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
        }
        return "redirect:/avocat/affaires/" + id;
    }

    @PostMapping("/avocat/affaires/{affaireId}/audiences/{audienceId}/resultat")
    public String enregistrerResultat(@PathVariable Long affaireId,
                                       @PathVariable Long audienceId,
                                       @RequestParam String resultat,
                                       @RequestParam String statut,
                                       @RequestParam(required = false) String prochaineAudience,
                                       RedirectAttributes ra) {
        try {
            LocalDate prochaine = (prochaineAudience != null && !prochaineAudience.isBlank())
                ? LocalDate.parse(prochaineAudience) : null;

            affaireService.enregistrerResultatAudience(
                audienceId,
                resultat,
                Audience.StatutAudience.valueOf(statut),
                prochaine
            );
            ra.addFlashAttribute("successMsg", "Résultat d'audience enregistré");
        } catch (Exception e) {
            log.error("Erreur résultat audience", e);
            ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
        }
        return "redirect:/avocat/affaires/" + affaireId;
    }

    // ─────────────────────────────────────────────
    //  JUGEMENT
    // ─────────────────────────────────────────────

    @PostMapping("/avocat/affaires/{id}/jugement")
    public String enregistrerJugement(@PathVariable Long id,
                                       @RequestParam String typeJugement,
                                       @RequestParam String dateJugement,
                                       @RequestParam(required = false) String montantJuge,
                                       @RequestParam(required = false) String delaiPaiement,
                                       @RequestParam(required = false) String description,
                                       RedirectAttributes ra) {
        try {
            affaireService.enregistrerJugement(
                id,
                AffaireJudiciaire.TypeJugement.valueOf(typeJugement),
                LocalDate.parse(dateJugement),
                montantJuge,
                delaiPaiement,
                description
            );
            ra.addFlashAttribute("successMsg", "Jugement enregistré avec succès");
        } catch (Exception e) {
            log.error("Erreur enregistrement jugement", e);
            ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
        }
        return "redirect:/avocat/affaires/" + id;
    }

    // ─────────────────────────────────────────────
    //  UPLOAD DOCUMENTS
    // ─────────────────────────────────────────────

    @PostMapping("/avocat/affaires/{id}/documents/upload")
    public String uploadDocument(@PathVariable Long id,
                                  @RequestParam("fichier") MultipartFile fichier,
                                  @RequestParam String typeDocument,
                                  @RequestParam(required = false) String description,
                                  Principal principal,
                                  RedirectAttributes ra) {
        try {
            if (fichier.isEmpty()) {
                ra.addFlashAttribute("errorMsg", "Veuillez sélectionner un fichier");
                return "redirect:/avocat/affaires/" + id;
            }
            if (fichier.getSize() > 20 * 1024 * 1024) {
                ra.addFlashAttribute("errorMsg", "Fichier trop volumineux (max 20 MB)");
                return "redirect:/avocat/affaires/" + id;
            }
            affaireService.uploadDocument(
                id,
                fichier,
                DocumentAffaire.TypeDocument.valueOf(typeDocument),
                description,
                principal.getName()
            );
            ra.addFlashAttribute("successMsg",
                "Document \"" + fichier.getOriginalFilename() + "\" uploadé avec succès");

        } catch (IOException e) {
            log.error("Erreur upload document", e);
            ra.addFlashAttribute("errorMsg", "Erreur lors de l'upload : " + e.getMessage());
        } catch (Exception e) {
            log.error("Erreur upload document", e);
            ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
        }
        return "redirect:/avocat/affaires/" + id;
    }

    // ─────────────────────────────────────────────
    //  TÉLÉCHARGEMENT DOCUMENT
    // ─────────────────────────────────────────────

    @GetMapping("/avocat/affaires/{affaireId}/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long affaireId,
                                                      @PathVariable Long documentId) {
        try {
            DocumentAffaire doc = affaireService.getDocumentById(documentId);
            Path filePath = fileStorageService.getCheminFichier(
                "affaires", doc.getNomFichierServeur());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = doc.getTypeMime() != null
                ? doc.getTypeMime() : "application/octet-stream";

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + doc.getNomFichierOriginal() + "\"")
                .body(resource);

        } catch (Exception e) {
            log.error("Erreur téléchargement document {}", documentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────
    //  SUPPRESSION DOCUMENT
    // ─────────────────────────────────────────────

    @PostMapping("/avocat/affaires/{affaireId}/documents/{documentId}/supprimer")
    public String supprimerDocument(@PathVariable Long affaireId,
                                     @PathVariable Long documentId,
                                     Principal principal,
                                     RedirectAttributes ra) {
        try {
            affaireService.supprimerDocument(documentId, principal.getName());
            ra.addFlashAttribute("successMsg", "Document supprimé");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
        }
        return "redirect:/avocat/affaires/" + affaireId;
    }

    // ═════════════════════════════════════════════
    //  RÉSULTAT DE MISSION  ← SECTION AJOUTÉE
    // ═════════════════════════════════════════════

    /**
     * Page dédiée à la soumission du résultat de mission.
     * URL : GET /affaires/{affaireId}/missions/{missionId}/resultat
     */
    @GetMapping("/avocat/affaires/{affaireId}/missions/{missionId}/resultat")
    public String pageResultatMission(@PathVariable Long affaireId,
                                       @PathVariable Long missionId,
                                       Model model) {
        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
        ResultatMission existant  = resultatMissionService.getResultat(missionId);

        model.addAttribute("affaire",          affaire);
        model.addAttribute("missionId",        missionId);
        model.addAttribute("resultatExistant", existant); // null = première soumission
        return "avocat/affaires/resultat-mission";
    }

    /**
     * Traite la soumission du résultat (fichier + commentaire).
     * ➡ statut mission → REALISEE
     * ➡ notification agent "Mission avocat terminée"
     * URL : POST /affaires/{affaireId}/missions/{missionId}/resultat
     */
    @PostMapping("/avocat/affaires/{affaireId}/missions/{missionId}/resultat")
    public String soumettreResultat(@PathVariable Long affaireId,
                                     @PathVariable Long missionId,
                                     @RequestParam String commentaire,
                                     @RequestParam(value = "fichier", required = false)
                                         MultipartFile fichier,
                                     Principal principal,
                                     RedirectAttributes ra) {
        try {
            resultatMissionService.soumettre(
                missionId, commentaire, fichier, principal.getName());

            ra.addFlashAttribute("successMsg",
                "Résultat soumis avec succès. L'agent a été notifié.");

        } catch (IOException e) {
            log.error("Erreur upload résultat mission", e);
            ra.addFlashAttribute("errorMsg", "Erreur upload : " + e.getMessage());
        } catch (Exception e) {
            log.error("Erreur soumission résultat mission", e);
            ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
        }
        return "redirect:/affaires/" + affaireId;
    }

    /**
     * Télécharge le fichier attaché au résultat de mission.
     * URL : GET /affaires/{affaireId}/missions/{missionId}/resultat/download
     */
    @GetMapping("/avocat/affaires/{affaireId}/missions/{missionId}/resultat/download")
    public ResponseEntity<Resource> downloadResultat(@PathVariable Long affaireId,
                                                      @PathVariable Long missionId) {
        try {
            ResultatMission resultat = resultatMissionService.getResultat(missionId);
            if (resultat == null || resultat.getNomFichierServeur() == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = fileStorageService.getCheminFichier(
                "missions", resultat.getNomFichierServeur());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = resultat.getTypeMime() != null
                ? resultat.getTypeMime() : "application/octet-stream";

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + resultat.getNomFichierOriginal() + "\"")
                .body(resource);

        } catch (Exception e) {
            log.error("Erreur téléchargement résultat mission {}", missionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}