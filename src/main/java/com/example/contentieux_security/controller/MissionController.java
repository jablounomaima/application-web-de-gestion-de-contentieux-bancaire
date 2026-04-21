package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.repository.FichierResultatRepository;
import com.example.contentieux_security.repository.MissionRepository;
import com.example.contentieux_security.repository.ResultatMissionRepository;
import com.example.contentieux_security.service.AffaireJudiciaireService;
import com.example.contentieux_security.service.FileStorageService;
import com.example.contentieux_security.service.HistoriqueService;
import com.example.contentieux_security.service.MissionService;
import com.example.contentieux_security.service.PrestationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.contentieux_security.enums.StatutMission;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/prestataire/missions")
@RequiredArgsConstructor
@Slf4j
public class MissionController {

    private final PrestationService prestationService;
    private final MissionService missionService;
    private final FileStorageService fileStorageService;
    private final ResultatMissionRepository resultatMissionRepository;
    private final FichierResultatRepository fichierResultatRepository;
    private final HistoriqueService historiqueService;
    private final MissionRepository missionRepository;
    private final AffaireJudiciaireService affaireJudiciaireService;

    // ── LISTE ─────────────────────────────────────────────────────
    @GetMapping
    public String mesMissions(Authentication auth, Model model,
                               @RequestParam(required = false) String recherche) {
    
        // ✅ Extraire le username correct (OIDC ou standard)
        String username = auth.getName();
        if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
            username = oidcUser.getPreferredUsername();
        } else if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
            Object preferred = oauth2User.getAttribute("preferred_username");
            if (preferred != null) username = preferred.toString();
        }
    
        // ✅ Missions filtrées par agent créateur du dossier
        List<Mission> missions = missionRepository
                .findByPrestation_Dossier_CreeParWithDetails(username);
    
        if (recherche != null && !recherche.isBlank()) {
            String kw = recherche.toLowerCase();
            missions = missions.stream()
                .filter(m -> m.getPrestation() != null &&
                             m.getPrestation().getDossier() != null && (
                    (m.getPrestation().getDossier().getNumeroDossier() != null &&
                     m.getPrestation().getDossier().getNumeroDossier().toLowerCase().contains(kw))
                    ||
                    (m.getPrestation().getDossier().getClient() != null &&
                     m.getPrestation().getDossier().getClient().getNom().toLowerCase().contains(kw))
                ))
                .toList();
        }
    
        Map<String, List<Mission>> missionsParDossier = missions.stream()
                .filter(m -> m.getPrestation() != null && m.getPrestation().getDossier() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        m -> m.getPrestation().getDossier().getNumeroDossier(),
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
    
        long missionsRejetees = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.REJETEE)
                .count();
    
        model.addAttribute("missions",           missions);
        model.addAttribute("missionsParDossier", missionsParDossier);
        model.addAttribute("recherche",          recherche != null ? recherche : "");
        model.addAttribute("totalMissions",      missions.size());
        model.addAttribute("missionsRejetees",   missionsRejetees);
    
        return "prestataire/missions/liste";
    }
    // ── DETAIL ────────────────────────────────────────────────────
    @Transactional
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE','AVOCAT','AGENT')")
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
    
        boolean isAvocat = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("AVOCAT"));
    
        // Bloquer uniquement si aucun rôle valide
        if (!isPrestataire && !isAgent && !isAvocat) {
            ra.addFlashAttribute("error", "Accès non autorisé");
            return "redirect:/prestataire/missions";
        }
    
        List<ResultatMission> resultats = resultatMissionRepository
                .findByMission_IdOrderByDateSoumissionDesc(id);
    
        List<HistoriqueDossier> historique = List.of();
        try {
            Long dossierId = mission.getPrestation().getDossier().getId();
            historique = historiqueService.getHistorique(dossierId);
        } catch (Exception ignored) {}
    
        boolean resultatVerrouille = false;
        if (mission.getResultatMission() != null) {
            StatutMission s = mission.getStatut();
            resultatVerrouille = s == StatutMission.TERMINEE
                              || s == StatutMission.REJETEE
                              || s == StatutMission.VALIDEE_AGENT;
        }
    
        // Charger l'affaire liée via le service (pas le repository directement)
        AffaireJudiciaire affaire = null;
        if (isAvocat) {
            try {
                affaire = affaireJudiciaireService.findByMissionId(id);
            } catch (Exception ignored) {}
        }
    
        model.addAttribute("resultatVerrouille", resultatVerrouille);
        model.addAttribute("mission",            mission);
        model.addAttribute("resultats",          resultats);
        model.addAttribute("historique",         historique);
        model.addAttribute("isAgent",            isAgent);
        model.addAttribute("isPrestataire",      isPrestataire);
        model.addAttribute("isAvocat",           isAvocat);
        model.addAttribute("affaire",            affaire);
    
        return "prestataire/missions/detail";
    }
   
    // ── PV — POST ─────────────────────────────────────────────────
   
    
        // ── FACTURE — POST ────────────────────────────────────────────
      // =========================================================
//  🧾 FACTURE — POST soumettre (prestataire uniquement)
// =========================================================

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
            missionService.modifierMission(id, description, dateFinPrevue,
                                           authentication.getName());
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

    // ── UPLOAD RÉSULTAT ───────────────────────────────────────────
    @Transactional
    @PostMapping("/{id}/resultat")
    public String soumettreResultat(@PathVariable Long id,
                                   @RequestParam(required = false) String commentaire,
                                   @RequestParam(value = "fichiers", required = false)
                                   List<MultipartFile> fichiers,
                                   Authentication authentication,
                                   RedirectAttributes ra) {

        String username = authentication.getName();

        Mission mission = missionRepository.findByIdWithPrestataire(id)
            .orElseThrow(() -> new IllegalArgumentException("Mission introuvable"));

        if (mission.getPrestataire() == null ||
            !username.equals(mission.getPrestataire().getUsername())) {
            ra.addFlashAttribute("error", "Accès non autorisé");
            return "redirect:/prestataire/missions";
        }

        boolean hasCommentaire = commentaire != null && !commentaire.isBlank();
        boolean hasFichiers = fichiers != null && !fichiers.isEmpty() &&
                              fichiers.stream().anyMatch(f -> f != null && !f.isEmpty());

        if (!hasCommentaire && !hasFichiers) {
            ra.addFlashAttribute("error", "Ajoutez un commentaire ou au moins un fichier");
            return "redirect:/prestataire/missions/" + id;
        }

        try {
            Optional<ResultatMission> existingOpt = resultatMissionRepository.findByMission_Id(id);
            ResultatMission resultat;

            if (existingOpt.isPresent()) {
                resultat = existingOpt.get();
                if (hasCommentaire) resultat.setCommentaire(commentaire);
                resultat.setSoumisePar(username);
                resultat.setDateSoumission(LocalDateTime.now());
            } else {
                resultat = ResultatMission.builder()
                    .mission(mission)
                    .commentaire(commentaire != null ? commentaire : "")
                    .soumisePar(username)
                    .dateSoumission(LocalDateTime.now())
                    .fichiers(new ArrayList<>())
                    .build();
            }

            if (hasFichiers) {
                for (MultipartFile fichier : fichiers) {
                    if (fichier == null || fichier.isEmpty()) continue;
                    if (fichier.getSize() > 20 * 1024 * 1024) {
                        ra.addFlashAttribute("error",
                            "Fichier \"" + fichier.getOriginalFilename() + "\" trop volumineux");
                        return "redirect:/prestataire/missions/" + id;
                    }
                    String nomServeur = fileStorageService.stocker(fichier, "missions");
                    FichierResultat fichierResultat = FichierResultat.builder()
                        .nomFichierOriginal(fichier.getOriginalFilename())
                        .nomFichierServeur(nomServeur)
                        .typeMime(fichier.getContentType())
                        .tailleFichier(fichier.getSize())
                        .dateUpload(LocalDateTime.now())
                        .resultat(resultat)
                        .build();
                    resultat.addFichier(fichierResultat);
                }
            }

            resultat.setMission(mission);
            resultatMissionRepository.save(resultat);

            StatutMission statutActuel = mission.getStatut();
            if (statutActuel == StatutMission.ASSIGNEE || statutActuel == StatutMission.REJETEE) {
                missionService.changerStatut(id, StatutMission.EN_COURS);
            }

            try {
                Long dossierId = mission.getPrestation() != null &&
                                 mission.getPrestation().getDossier() != null
                                 ? mission.getPrestation().getDossier().getId() : null;
                int nbFichiers = resultat.getFichiers() != null ? resultat.getFichiers().size() : 0;
                String desc = nbFichiers > 0 ? nbFichiers + " fichier(s) soumis" : "Commentaire soumis";
                if (hasCommentaire && commentaire.length() > 50)
                    desc += " — " + commentaire.substring(0, 50) + "...";
                else if (hasCommentaire)
                    desc += " — " + commentaire;
                historiqueService.enregistrerParId(dossierId, "RESULTAT_SOUMIS", desc, username);
            } catch (Exception ignored) {}

            int nbFichiers = resultat.getFichiers() != null ? resultat.getFichiers().size() : 0;
            String message = "Résultat soumis avec succès";
            if (nbFichiers > 0)
                message += " (" + nbFichiers + " fichier" + (nbFichiers > 1 ? "s" : "") + ")";
            ra.addFlashAttribute("success", message);

        } catch (Exception e) {
            log.error("Erreur lors de la soumission du résultat", e);
            ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }

        return "redirect:/prestataire/missions/" + id;
    }

    // ── TÉLÉCHARGER FICHIER PAR NOM ───────────────────────────────
    @GetMapping("/fichier/{nomServeur}")
    public ResponseEntity<Resource> telechargerFichier(
            @PathVariable String nomServeur,
            Authentication authentication) {
        try {
            Path chemin = fileStorageService.getCheminFichier("missions", nomServeur);
            Resource resource = new UrlResource(chemin.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nomServeur + "\"")
                .body(resource);
        } catch (Exception e) {
            log.error("Erreur téléchargement fichier: {}", nomServeur, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── TÉLÉCHARGER FICHIER PAR ID ────────────────────────────────
    @GetMapping("/fichier/id/{fichierId}")
    public ResponseEntity<Resource> telechargerFichierParId(@PathVariable Long fichierId) {
        try {
            Optional<FichierResultat> fichierOpt = fichierResultatRepository.findById(fichierId);
            if (fichierOpt.isEmpty()) return ResponseEntity.notFound().build();
            FichierResultat fichier = fichierOpt.get();
            Path chemin = fileStorageService.getCheminFichier("missions",
                                                               fichier.getNomFichierServeur());
            Resource resource = new UrlResource(chemin.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fichier.getNomFichierOriginal() + "\"")
                .header(HttpHeaders.CONTENT_TYPE,
                        fichier.getTypeMime() != null ? fichier.getTypeMime()
                                                      : "application/octet-stream")
                .body(resource);
        } catch (Exception e) {
            log.error("Erreur téléchargement fichier id: {}", fichierId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
// ── FORMULAIRE MODIFIER RÉSULTAT ──────────────────────────────
@GetMapping("/{id}/resultat/modifier")
public String formulaireModifierResultat(@PathVariable Long id,
                                         Model model,
                                         Authentication authentication,
                                         RedirectAttributes ra) {
    if (authentication == null) {
        ra.addFlashAttribute("error", "Session expirée");
        return "redirect:/login";
    }

    String username = authentication.getName();
    Mission mission = missionRepository.findById(id).orElse(null);

    // 1. Vérification existence
    if (mission == null) {
        ra.addFlashAttribute("error", "Mission introuvable");
        return "redirect:/prestataire/missions";
    }

    // 2. Vérification Propriétaire
    if (mission.getPrestataire() == null || 
        !mission.getPrestataire().getUsername().equals(username)) {
        ra.addFlashAttribute("error", "Accès non autorisé");
        return "redirect:/prestataire/missions";
    }

    // 3. Vérification Statut autorisant la modification
    List<StatutMission> autorises = Arrays.asList(
        StatutMission.EN_COURS, 
        StatutMission.REJETEE, 
        StatutMission.FACTURE_SOUMISE
    );
    
    if (!autorises.contains(mission.getStatut())) {
        ra.addFlashAttribute("error", 
            "Modification non autorisée pour le statut : " + mission.getStatut());
        return "redirect:/prestataire/missions/" + id;
    }

    // 4. Récupération ou création du résultat
    ResultatMission resultat = resultatMissionRepository.findByMission_Id(id)
        .map(existing -> existing)  // Si présent, on le garde
        .orElseGet(() -> {
            // Sinon, on crée un nouveau (non persisté, juste pour le formulaire)
            ResultatMission newRes = new ResultatMission();
            newRes.setMission(mission);
            newRes.setFichiers(new ArrayList<>());
            return newRes;
        });

    model.addAttribute("mission", mission);
    model.addAttribute("resultat", resultat);
    
    return "prestataire/missions/modifier_mission_pour_prestataire";
}
// ── POST MODIFIER RÉSULTAT ────────────────────────────────────
@PostMapping("/{id}/resultat/modifier")
@Transactional
public String modifierResultat(@PathVariable Long id,
                                @RequestParam(required = false) String commentaire,
                                @RequestParam(required = false) String pvTexte,
                                @RequestParam(required = false) Double montant,
                                @RequestParam(required = false) String factureRef,
                                @RequestParam(value = "fichiers", required = false)
                                List<MultipartFile> fichiers,
                                Authentication authentication,
                                RedirectAttributes ra) {
    
    String username = authentication.getName();
    Mission mission = missionRepository.findByIdWithPrestataire(id)
            .orElseThrow(() -> new IllegalArgumentException("Mission introuvable"));

    try {
        // Mettre à jour la mission (PV, montant, facture)
        if (pvTexte != null && !pvTexte.isBlank()) {
            mission.setPvMission(pvTexte.trim());
        }
        if (montant != null && montant > 0) {
            mission.setMontantFacture(montant);
        }
        if (factureRef != null && !factureRef.isBlank()) {
            mission.setFactureRef(factureRef.trim());
        }
        missionRepository.save(mission);

        // Récupérer ou créer le résultat
        ResultatMission resultat = resultatMissionRepository.findByMission_Id(id)
            .orElseGet(() -> ResultatMission.builder()
                .mission(mission)
                .fichiers(new ArrayList<>())
                .build());

        if (commentaire != null && !commentaire.isBlank()) {
            resultat.setCommentaire(commentaire.trim());
        }
        resultat.setSoumisePar(username);
        resultat.setDateSoumission(LocalDateTime.now());

        // Gestion des fichiers...
        boolean hasFichiers = fichiers != null &&
                fichiers.stream().anyMatch(f -> f != null && !f.isEmpty());

        if (hasFichiers) {
            for (MultipartFile fichier : fichiers) {
                if (fichier == null || fichier.isEmpty()) continue;
                // ... upload fichier ...
                String nomServeur = fileStorageService.stocker(fichier, "missions");
                FichierResultat fichierResultat = FichierResultat.builder()
                    .nomFichierOriginal(fichier.getOriginalFilename())
                    .nomFichierServeur(nomServeur)
                    .typeMime(fichier.getContentType())
                    .tailleFichier(fichier.getSize())
                    .dateUpload(LocalDateTime.now())
                    .resultat(resultat)
                    .build();
                resultat.addFichier(fichierResultat);
            }
        }

        resultatMissionRepository.save(resultat);
        ra.addFlashAttribute("success", "Résultat modifié avec succès");

    } catch (Exception e) {
        log.error("Erreur modification résultat mission {}", id, e);
        ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
    }

    return "redirect:/prestataire/missions/" + id;
}

}