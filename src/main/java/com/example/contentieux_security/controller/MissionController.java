package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.HistoriqueDossier;
import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.entity.ResultatMission;
import com.example.contentieux_security.repository.ResultatMissionRepository;
import com.example.contentieux_security.service.FileStorageService;
import com.example.contentieux_security.service.HistoriqueService;
import com.example.contentieux_security.service.MissionService;
import com.example.contentieux_security.service.PrestationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.contentieux_security.enums.StatutMission;
import java.nio.file.Path;
import java.util.List;

@Controller
@RequestMapping("/prestataire/missions")
@RequiredArgsConstructor
public class MissionController {

    private final PrestationService prestationService;
    private final MissionService missionService;
    private final FileStorageService fileStorageService;
    private final ResultatMissionRepository resultatMissionRepository;
    private final HistoriqueService historiqueService;

    // ── LISTE ─────────────────────────────────────────────────────
    @GetMapping
    public String mesMissions(Model model, Authentication authentication) {

        String username = authentication.getName();
        List<Mission> missions = prestationService.getMissionsPrestataire(username);
        model.addAttribute("missions", missions != null ? missions : List.of());
        return "prestataire/missions/liste";
    }

    // ── DETAIL ────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public String detailMission(@PathVariable Long id,
                                Model model,
                                Authentication authentication,
                                RedirectAttributes ra) {

        String username = authentication.getName();
        Mission mission = prestationService.getMissionByIdWithDetails(id);

        if (mission == null) {
            ra.addFlashAttribute("error", "Mission introuvable");
            return "redirect:/prestataire/missions";
        }

        boolean isPrestataire = mission.getPrestataire() != null &&
                                username.equals(mission.getPrestataire().getUsername());

        boolean isAgent = mission.getPrestation() != null &&
                          mission.getPrestation().getDossier() != null &&
                          mission.getPrestation().getDossier().getAgentCreateur() != null &&
                          username.equals(mission.getPrestation().getDossier()
                                               .getAgentCreateur().getUsername());

        if (!isPrestataire && !isAgent) {
            ra.addFlashAttribute("error", "Accès non autorisé");
            return "redirect:/prestataire/missions";
        }

        List<ResultatMission> resultats = resultatMissionRepository
            .findByMissionIdOrderByDateSoumissionDesc(id);

        // ── Historique du dossier ─────────────────────────────────
        List<HistoriqueDossier> historique = List.of();
        try {
            Long dossierId = mission.getPrestation().getDossier().getId();
            historique = historiqueService.getHistorique(dossierId);
        } catch (Exception ignored) {}

        model.addAttribute("mission",       mission);
        model.addAttribute("resultats",     resultats);
        model.addAttribute("historique",    historique);
        model.addAttribute("isAgent",       isAgent);
        model.addAttribute("isPrestataire", isPrestataire);
        return "prestataire/missions/detail";
    }

    // ── PV ────────────────────────────────────────────────────────
    @PostMapping("/{id}/pv")
    public String soumettrePV(@PathVariable Long id,
                              @RequestParam String pvTexte,
                              Authentication authentication,
                              RedirectAttributes ra) {

        if (pvTexte == null || pvTexte.trim().isEmpty()) {
            ra.addFlashAttribute("error", "PV vide");
            return "redirect:/prestataire/missions/" + id;
        }

        prestationService.soumettrePV(id, pvTexte, authentication.getName());
        ra.addFlashAttribute("success", "PV soumis avec succès");
        return "redirect:/prestataire/missions/" + id;
    }

    // ── FACTURE ───────────────────────────────────────────────────
    @PostMapping("/{id}/facture")
    public String soumettreFacture(@PathVariable Long id,
                                   @RequestParam Double montant,
                                   @RequestParam String factureRef,
                                   Authentication authentication,
                                   RedirectAttributes ra) {

        if (montant == null || montant <= 0) {
            ra.addFlashAttribute("error", "Montant invalide");
            return "redirect:/prestataire/missions/" + id;
        }

        if (factureRef == null || factureRef.trim().isEmpty()) {
            ra.addFlashAttribute("error", "Référence obligatoire");
            return "redirect:/prestataire/missions/" + id;
        }

        prestationService.soumettreFacture(id, montant, factureRef, authentication.getName());
        ra.addFlashAttribute("success", "Facture envoyée");
        return "redirect:/prestataire/missions/" + id;
    }

    // ── FORMULAIRE MODIFICATION ───────────────────────────────────
    @GetMapping("/{id}/modifier")
    public String formulaireModifier(@PathVariable Long id,
                                     Model model,
                                     Authentication authentication,
                                     RedirectAttributes ra) {

        String username = authentication.getName();
        Mission mission = prestationService.getMissionByIdWithDetails(id);

        if (mission == null) {
            ra.addFlashAttribute("error", "Mission introuvable");
            return "redirect:/prestataire/missions";
        }

        boolean isAgent = mission.getPrestation().getDossier().getAgentCreateur() != null &&
                          username.equals(mission.getPrestation().getDossier()
                                               .getAgentCreateur().getUsername());

        if (!isAgent) {
            ra.addFlashAttribute("error", "Accès non autorisé");
            return "redirect:/prestataire/missions";
        }

        if (mission.getStatut() != StatutMission.ASSIGNEE) {
            ra.addFlashAttribute("error", "Cette mission ne peut plus être modifiée");
            return "redirect:/prestataire/missions/" + id;
        }

        model.addAttribute("mission", mission);
        return "prestataire/missions/modifier";
    }

    // ── POST MODIFICATION ─────────────────────────────────────────
    @PostMapping("/{id}/modifier")
    public String modifierMission(@PathVariable Long id,
                                   @RequestParam String description,
                                   @RequestParam(required = false)
                                   @org.springframework.format.annotation.DateTimeFormat(
                                       iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                                   java.time.LocalDate dateFinPrevue,
                                   Authentication authentication,
                                   RedirectAttributes ra) {
        try {
            missionService.modifierMission(id, description, dateFinPrevue, authentication.getName());
            ra.addFlashAttribute("success", "Mission modifiée avec succès");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/prestataire/missions/" + id;
    }

    // ── SUPPRESSION ───────────────────────────────────────────────
    @PostMapping("/{id}/supprimer")
    public String supprimerMission(@PathVariable Long id,
                                    Authentication authentication,
                                    RedirectAttributes ra) {
        try {
            missionService.supprimerMission(id, authentication.getName());
            ra.addFlashAttribute("success", "Mission supprimée");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/prestataire/missions";
    }

    // ── UPLOAD FICHIER + COMMENTAIRE ──────────────────────────────
    @PostMapping("/{id}/resultat")
    public String soumettreResultat(@PathVariable Long id,
                                    @RequestParam(required = false) String commentaire,
                                    @RequestParam(required = false) MultipartFile fichier,
                                    Authentication authentication,
                                    RedirectAttributes ra) {

        String username = authentication.getName();
        Mission mission = prestationService.getMissionByIdWithDetails(id);

        if (mission == null) {
            ra.addFlashAttribute("error", "Mission introuvable");
            return "redirect:/prestataire/missions";
        }

        if (mission.getPrestataire() == null ||
            !username.equals(mission.getPrestataire().getUsername())) {
            ra.addFlashAttribute("error", "Accès non autorisé");
            return "redirect:/prestataire/missions";
        }

        if ((commentaire == null || commentaire.isBlank()) &&
            (fichier == null || fichier.isEmpty())) {
            ra.addFlashAttribute("error", "Ajoutez un commentaire ou un fichier");
            return "redirect:/prestataire/missions/" + id;
        }

        try {
            ResultatMission resultat = new ResultatMission();
            resultat.setMission(mission);
            resultat.setCommentaire(commentaire != null ? commentaire : "");
            resultat.setSoumisePar(username);
            resultat.setDateSoumission(java.time.LocalDateTime.now());

            if (fichier != null && !fichier.isEmpty()) {
                String nomServeur = fileStorageService.stocker(fichier, "missions");
                resultat.setNomFichierOriginal(fichier.getOriginalFilename());
                resultat.setNomFichierServeur(nomServeur);
                resultat.setTypeMime(fichier.getContentType());
                resultat.setTailleFichier(fichier.getSize());
            }

            resultatMissionRepository.save(resultat);

            // Mettre EN_COURS si ASSIGNEE
            if (mission.getStatut() == StatutMission.ASSIGNEE) {
                missionService.changerStatut(id, StatutMission.EN_COURS);
            }

            // ── Enregistrer dans l'historique ─────────────────────
            try {
                Long dossierId = mission.getPrestation().getDossier().getId();
                String desc = (fichier != null && !fichier.isEmpty())
                    ? "Fichier soumis : " + fichier.getOriginalFilename()
                    : "Commentaire soumis";
                if (commentaire != null && !commentaire.isBlank()) {
                    desc += " — " + commentaire.substring(0,
                        Math.min(50, commentaire.length()));
                }
                historiqueService.enregistrerParId(
                    dossierId, "RESULTAT_SOUMIS", desc, username);
            } catch (Exception ignored) {}

            ra.addFlashAttribute("success", "Résultat soumis avec succès");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }

        return "redirect:/prestataire/missions/" + id;
    }

    // ── TÉLÉCHARGER UN FICHIER ────────────────────────────────────
    @GetMapping("/fichier/{nomServeur}")
    public ResponseEntity<Resource> telechargerFichier(
            @PathVariable String nomServeur,
            Authentication authentication) {

        try {
            Path chemin = fileStorageService.getCheminFichier("missions", nomServeur);
            Resource resource = new UrlResource(chemin.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nomServeur + "\"")
                .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}