package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.repository.FichierResultatRepository;
import com.example.contentieux_security.repository.MissionRepository;
import com.example.contentieux_security.repository.ResultatMissionRepository;
import com.example.contentieux_security.service.FileStorageService;
import com.example.contentieux_security.service.HistoriqueService;
import com.example.contentieux_security.service.MissionService;
import com.example.contentieux_security.service.PrestationService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    // ── LISTE ─────────────────────────────────────────────────────
  // =========================================================
    //  LISTE DES MISSIONS DU PRESTATAIRE CONNECTÉ
    // =========================================================
   @GetMapping
public String mesMissions(Authentication auth, Model model, 
                           @RequestParam(required = false) String recherche) {

    String username = auth.getName();
    if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
        username = oidcUser.getPreferredUsername();
    } else if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
        Object preferred = oauth2User.getAttribute("preferred_username");
        if (preferred != null) username = preferred.toString();
    }

    // ✅ Récupérer TOUTES les missions (tous prestataires, tous dossiers)
    List<Mission> missions = missionRepository.findAllWithDetails();

    // Filtrage recherche
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
            .filter(m -> m.getPrestation() != null
                      && m.getPrestation().getDossier() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                    m -> m.getPrestation().getDossier().getNumeroDossier(),
                    java.util.LinkedHashMap::new,
                    java.util.stream.Collectors.toList()
            ));


            // ✅ Compter les missions rejetées pour alerte dashboard
    long missionsRejetees = missions.stream()
    .filter(m -> m.getStatut() == StatutMission.REJETEE)
    .count();

    model.addAttribute("missions",           missions);
    model.addAttribute("missionsParDossier", missionsParDossier);
    model.addAttribute("recherche",          recherche != null ? recherche : "");
    model.addAttribute("totalMissions",      missions.size());
    model.addAttribute("missionsRejetees",   missionsRejetees); // ✅ pour badge alerte


    return "prestataire/missions/liste";
}
   
   
   
   
    // ── DETAIL ────────────────────────────────────────────────────
  // ── DETAIL MISSION ────────────────────────────────────────────────────
  @Transactional
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
              .findByMission_IdOrderByDateSoumissionDesc(id);
  
      // Historique
      List<HistoriqueDossier> historique = List.of();
      try {
          Long dossierId = mission.getPrestation().getDossier().getId();
          historique = historiqueService.getHistorique(dossierId);
      } catch (Exception ignored) {}
  
      // ====================== LOGIQUE DE VERROUILLAGE ======================
      boolean resultatVerrouille = false;
  
      if (mission.getResultatMission() != null) {
        StatutMission s = mission.getStatut();
        resultatVerrouille = s == StatutMission.TERMINEE
                          || s == StatutMission.REJETEE
                          || s == StatutMission.VALIDEE_AGENT;
    }
  
      model.addAttribute("resultatVerrouille", resultatVerrouille);
      // =====================================================================
  
      model.addAttribute("mission",       mission);
      model.addAttribute("resultats",     resultats);
      model.addAttribute("historique",    historique);
      model.addAttribute("isAgent",       isAgent);
      model.addAttribute("isPrestataire", isPrestataire);
  
      return "prestataire/missions/detail";
  }
  
  // ── PV ────────────────────────────────────────────────────────
  @Transactional
  @PostMapping("/{id}/pv")
    public String soumettrePV(@PathVariable Long id,
        @RequestParam(required = false) String commentaire,
        @RequestParam(required = false) String pvTexte,
        @RequestParam(required = false) Double montantFacture,
        @RequestParam(required = false) String factureRef,
        @RequestParam(value = "fichiers", required = false) List<MultipartFile> fichiers,
        Authentication authentication,
        RedirectAttributes ra) {
    
        // 🔹 validation propre
        if (pvTexte == null || pvTexte.trim().isEmpty()) {
            ra.addFlashAttribute("error", "PV vide");
            return "redirect:/prestataire/missions/" + id;
        }
    
        try {
            // 🔹 appel service
            prestationService.soumettrePV(id, pvTexte.trim(), authentication.getName());
    
            ra.addFlashAttribute("success", "PV soumis avec succès");
    
        } catch (IllegalStateException e) {
            // 🔹 gestion des règles métier (ex: TERMINEE bloquée)
            ra.addFlashAttribute("error", e.getMessage());
    
        } catch (Exception e) {
            // 🔹 sécurité fallback
            ra.addFlashAttribute("error", "Erreur lors de la soumission du PV");
        }
    
        return "redirect:/prestataire/missions/" + id;
    }

    // ── FACTURE ───────────────────────────────────────────────────
    @Transactional
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

    // ── UPLOAD FICHIERS MULTIPLES + COMMENTAIRE ──────────────────────────────
    @Transactional
    @PostMapping("/{id}/resultat")
    public String soumettreResultat(@PathVariable Long id,
                                   @RequestParam(required = false) String commentaire,
                                   @RequestParam(value = "fichiers", required = false) List<MultipartFile> fichiers,
                                   Authentication authentication,
                                   RedirectAttributes ra) {
    
        String username = authentication.getName();
    
        // ✅ UNE SEULE mission chargée AVEC prestataire
        Mission mission = missionRepository.findByIdWithPrestataire(id)
            .orElseThrow(() -> new IllegalArgumentException("Mission introuvable"));
    
        // ✅ Vérification accès (sans LazyException)
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
    
                if (hasCommentaire) {
                    resultat.setCommentaire(commentaire);
                }
    
                resultat.setSoumisePar(username);
                resultat.setDateSoumission(LocalDateTime.now());
    
            } else {
                resultat = ResultatMission.builder()
                    .mission(mission) // ✅ entité managée
                    .commentaire(commentaire != null ? commentaire : "")
                    .soumisePar(username)
                    .dateSoumission(LocalDateTime.now())
                    .fichiers(new ArrayList<>())
                    .build();
            }
    
            // ✅ fichiers
            if (hasFichiers) {
                for (MultipartFile fichier : fichiers) {
    
                    if (fichier == null || fichier.isEmpty()) continue;
    
                    if (fichier.getSize() > 20 * 1024 * 1024) {
                        ra.addFlashAttribute("error",
                            "Fichier \"" + fichier.getOriginalFilename() + "\" trop volumineux (max 20 MB)");
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
    
            // ✅ IMPORTANT
            resultat.setMission(mission);
            resultatMissionRepository.save(resultat);
    
            // ✅ statut
            StatutMission statutActuel = mission.getStatut();
            if (statutActuel == StatutMission.ASSIGNEE || statutActuel == StatutMission.REJETEE) {
                // ✅ Si rejetée, on remet EN_COURS quand le prestataire resoumet
                missionService.changerStatut(id, StatutMission.EN_COURS);
            }
    
            // ✅ historique
            try {
                Long dossierId = mission.getPrestation() != null &&
                                 mission.getPrestation().getDossier() != null
                                 ? mission.getPrestation().getDossier().getId()
                                 : null;
    
                int nbFichiers = resultat.getFichiers() != null ? resultat.getFichiers().size() : 0;
    
                String desc = nbFichiers > 0
                    ? nbFichiers + " fichier(s) soumis"
                    : "Commentaire soumis";
    
                if (hasCommentaire && commentaire.length() > 50) {
                    desc += " — " + commentaire.substring(0, 50) + "...";
                } else if (hasCommentaire) {
                    desc += " — " + commentaire;
                }
    
                historiqueService.enregistrerParId(dossierId, "RESULTAT_SOUMIS", desc, username);
    
            } catch (Exception ignored) {}
    
            int nbFichiers = resultat.getFichiers() != null ? resultat.getFichiers().size() : 0;
    
            String message = "Résultat soumis avec succès";
            if (nbFichiers > 0) {
                message += " (" + nbFichiers + " fichier" + (nbFichiers > 1 ? "s" : "") + ")";
            }
    
            ra.addFlashAttribute("success", message);
    
        } catch (Exception e) {
            log.error("Erreur lors de la soumission du résultat", e);
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
            log.error("Erreur téléchargement fichier: {}", nomServeur, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ── TÉLÉCHARGER UN FICHIER PAR ID (pour les fichiers multiples) ──
    @GetMapping("/fichier/id/{fichierId}")
    public ResponseEntity<Resource> telechargerFichierParId(@PathVariable Long fichierId) {
        try {
            Optional<FichierResultat> fichierOpt = fichierResultatRepository.findById(fichierId);
            if (fichierOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            FichierResultat fichier = fichierOpt.get();
            Path chemin = fileStorageService.getCheminFichier("missions", fichier.getNomFichierServeur());
            Resource resource = new UrlResource(chemin.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fichier.getNomFichierOriginal() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, fichier.getTypeMime() != null ? fichier.getTypeMime() : "application/octet-stream")
                .body(resource);

        } catch (Exception e) {
            log.error("Erreur téléchargement fichier id: {}", fichierId, e);
            return ResponseEntity.internalServerError().build();
        }
    }



// ── GET : afficher le formulaire de modification du résultat ──
@GetMapping("/{id}/resultat/modifier")
public String formulaireModifierResultat(@PathVariable Long id,
                                         Model model,
                                         Authentication authentication,
                                         RedirectAttributes ra) {

    // 🔐 Vérifier utilisateur connecté
    if (authentication == null) {
        ra.addFlashAttribute("error", "Session expirée");
        return "redirect:/login";
    }

    String username = authentication.getName();

    // 🔎 Récupérer mission
    Mission mission = missionRepository.findById(id).orElse(null);

    if (mission == null) {
        ra.addFlashAttribute("error", "Mission introuvable");
        return "redirect:/prestataire/missions";
    }

    // 🔐 Vérifier que le prestataire est bien le propriétaire
    if (mission.getPrestataire() == null ||
        !mission.getPrestataire().getUsername().equals(username)) {

        ra.addFlashAttribute("error", "Accès non autorisé");
        return "redirect:/prestataire/missions";
    }

    // 🚫 Bloquer si statut interdit
    if (!(mission.getStatut().name().equals("EN_COURS") ||
          mission.getStatut().name().equals("REJETEE")    || 
          mission.getStatut().name().equals("FACTURE_SOUMISE")  )) {

        ra.addFlashAttribute("error", "Modification non autorisée pour ce statut");
        return "redirect:/prestataire/missions/" + id;
    }

    // 📄 Récupérer résultat
    Optional<ResultatMission> resultatOpt =
            resultatMissionRepository.findByMission_Id(id);

    if (resultatOpt.isEmpty()) {
        ra.addFlashAttribute("error", "Aucun résultat à modifier");
        return "redirect:/prestataire/missions/" + id;
    }

    // 📦 Ajouter au model
    model.addAttribute("mission", mission);
    model.addAttribute("resultat", resultatOpt.get());

    return "prestataire/missions/modifier_mission_pour_prestataire";
}



// ── POST : traiter la modification du résultat ──
@Transactional
@PostMapping("/{id}/resultat/modifier")
public String modifierResultat(@PathVariable Long id,
                                @RequestParam(required = false) String commentaire,
                                @RequestParam(required = false) String pvTexte,
                                @RequestParam(required = false) Double montant,
                                @RequestParam(required = false) String factureRef,
                                @RequestParam(value = "fichiers", required = false) List<MultipartFile> fichiers,
                                Authentication authentication,
                                RedirectAttributes ra) {

    String username = authentication.getName();

    Mission mission = missionRepository.findByIdWithPrestataire(id)
            .orElseThrow(() -> new IllegalArgumentException("Mission introuvable"));

    try {
        // ── 1. Mise à jour du PV ──────────────────────────────
        if (pvTexte != null && !pvTexte.isBlank()) {
            mission.setPvMission(pvTexte.trim());
        }

        // ── 2. Mise à jour facture ────────────────────────────
        if (montant != null && montant > 0) {
            mission.setMontantFacture(montant);
        }

        if (factureRef != null && !factureRef.isBlank()) {
            mission.setFactureRef(factureRef.trim());
        }

        missionRepository.save(mission); // ← sauvegarde montant + PV + ref

        // ── 3. Mise à jour du résultat (commentaire + fichiers) ──
        Optional<ResultatMission> existingOpt = resultatMissionRepository.findByMission_Id(id);
        ResultatMission resultat = existingOpt.orElseGet(() ->
                ResultatMission.builder()
                        .mission(mission)
                        .fichiers(new ArrayList<>())
                        .build()
        );

        if (commentaire != null && !commentaire.isBlank()) {
            resultat.setCommentaire(commentaire.trim());
        }

        resultat.setSoumisePar(username);
        resultat.setDateSoumission(LocalDateTime.now());

        // ── 4. Nouveaux fichiers ──────────────────────────────
        boolean hasFichiers = fichiers != null &&
                fichiers.stream().anyMatch(f -> f != null && !f.isEmpty());

        if (hasFichiers) {
            for (MultipartFile fichier : fichiers) {
                if (fichier == null || fichier.isEmpty()) continue;

                if (fichier.getSize() > 20 * 1024 * 1024) {
                    ra.addFlashAttribute("error",
                            "Fichier \"" + fichier.getOriginalFilename() + "\" trop volumineux (max 20 MB)");
                    return "redirect:/prestataire/missions/" + id + "/resultat/modifier";
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

        resultatMissionRepository.save(resultat);

        ra.addFlashAttribute("success", "Résultat modifié avec succès");

    } catch (Exception e) {
        log.error("Erreur modification résultat mission {}", id, e);
        ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
    }

    return "redirect:/prestataire/missions/" + id;
}
}