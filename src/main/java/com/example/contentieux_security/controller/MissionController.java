package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.repository.DossierRepository;
import com.example.contentieux_security.repository.FichierResultatRepository;
import com.example.contentieux_security.repository.MissionRepository;
import com.example.contentieux_security.repository.ResultatMissionRepository;
import com.example.contentieux_security.repository.ValidateurRepository;
import com.example.contentieux_security.service.AffaireJudiciaireService;
import com.example.contentieux_security.service.FileStorageService;
import com.example.contentieux_security.service.HistoriqueService;
import com.example.contentieux_security.service.MissionService;
import com.example.contentieux_security.service.PrestationService;
import com.example.contentieux_security.service.NotificationService;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.enums.TypeValidateur;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
@Slf4j
public class MissionController {

    private final PrestationService         prestationService;
    private final MissionService            missionService;
    private final FileStorageService        fileStorageService;
    private final ResultatMissionRepository resultatMissionRepository;
    private final FichierResultatRepository fichierResultatRepository;
    private final HistoriqueService         historiqueService;
    private final MissionRepository         missionRepository;
    private final AffaireJudiciaireService  affaireJudiciaireService;
    private final NotificationService       notificationService;
    private final ValidateurRepository      validateurRepository;
    private final DossierRepository         dossierRepository;

    // ─────────────────────────────────────────────────────────
    // LISTE MISSIONS (agent)
    // ─────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> mesMissions(Authentication auth,
                                         @RequestParam(required = false) String recherche) {
        String username = auth.getName();
        if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser u)
            username = u.getPreferredUsername();
        else if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User u) {
            Object p = u.getAttribute("preferred_username");
            if (p != null) username = p.toString();
        }

        List<Mission> missions = missionRepository.findByPrestation_Dossier_CreeParWithDetails(username);

        if (recherche != null && !recherche.isBlank()) {
            String kw = recherche.toLowerCase();
            missions = missions.stream()
                .filter(m -> m.getPrestation() != null && m.getPrestation().getDossier() != null && (
                    (m.getPrestation().getDossier().getNumeroDossier() != null &&
                     m.getPrestation().getDossier().getNumeroDossier().toLowerCase().contains(kw)) ||
                    (m.getPrestation().getDossier().getClient() != null &&
                     m.getPrestation().getDossier().getClient().getNom().toLowerCase().contains(kw))
                )).toList();
        }

        Map<String, List<Mission>> parDossier = missions.stream()
            .filter(m -> m.getPrestation() != null && m.getPrestation().getDossier() != null)
            .collect(Collectors.groupingBy(
                m -> m.getPrestation().getDossier().getNumeroDossier(),
                LinkedHashMap::new, Collectors.toList()));

        long rejetees = missions.stream().filter(m -> m.getStatut() == StatutMission.REJETEE).count();

        return ResponseEntity.ok(Map.of(
            "missions",           missions,
            "missionsParDossier", parDossier,
            "totalMissions",      missions.size(),
            "missionsRejetees",   rejetees
        ));
    }

    // ─────────────────────────────────────────────────────────
    // DÉTAIL MISSION
    // ─────────────────────────────────────────────────────────
    @Transactional
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE','AVOCAT','AGENT')")
    public ResponseEntity<?> detailMission(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        Mission mission = prestationService.getMissionByIdWithDetails(id);
        if (mission == null) return ResponseEntity.notFound().build();

        boolean isPrestataire = mission.getPrestataire() != null &&
                                username.equals(mission.getPrestataire().getUsername());
        boolean isAgent = mission.getPrestation() != null &&
                          mission.getPrestation().getDossier() != null &&
                          mission.getPrestation().getDossier().getAgentCreateur() != null &&
                          username.equals(mission.getPrestation().getDossier().getAgentCreateur().getUsername());
        boolean isAvocat = authentication.getAuthorities().stream()
                           .anyMatch(a -> a.getAuthority().contains("AVOCAT"));

        if (!isPrestataire && !isAgent && !isAvocat)
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        List<ResultatMission> resultats =
            resultatMissionRepository.findByMission_IdOrderByDateSoumissionDesc(id);

        List<HistoriqueDossier> historique = List.of();
        try {
            historique = historiqueService.getHistorique(
                mission.getPrestation().getDossier().getId());
        } catch (Exception ignored) {}

        boolean resultatVerrouille = mission.getResultatMission() != null &&
            (mission.getStatut() == StatutMission.TERMINEE ||
             mission.getStatut() == StatutMission.REJETEE  ||
             mission.getStatut() == StatutMission.VALIDEE_AGENT);

        AffaireJudiciaire affaire = null;
        if (isAvocat) {
            try { 
                if (mission.getPrestation() != null && mission.getPrestation().getDossier() != null) {
                    affaire = affaireJudiciaireService.getAffaireParDossier(mission.getPrestation().getDossier().getId());
                }
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(Map.of(
            "mission",            mission,
            "resultats",          resultats,
            "historique",         historique,
            "resultatVerrouille", resultatVerrouille,
            "isAgent",            isAgent,
            "isPrestataire",      isPrestataire,
            "isAvocat",           isAvocat,
            "affaire",            affaire != null ? affaire : Map.of()
        ));
    }

    // ─────────────────────────────────────────────────────────
    // MODIFIER / SUPPRIMER MISSION
    // ─────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> modifierMission(@PathVariable Long id,
                                             @RequestBody Map<String, Object> body,
                                             Authentication auth) {
        try {
            java.time.LocalDate fin = body.get("dateFinPrevue") != null
                ? java.time.LocalDate.parse(body.get("dateFinPrevue").toString()) : null;
            missionService.modifierMission(id, (String) body.get("description"), fin, auth.getName());
            return ResponseEntity.ok(Map.of("message", "Mission modifiée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerMission(@PathVariable Long id, Authentication auth) {
        try {
            missionService.supprimerMission(id, auth.getName());
            return ResponseEntity.ok(Map.of("message", "Mission supprimée"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    // SOUMETTRE RÉSULTAT — workflow complet
    // ─────────────────────────────────────────────────────────
    @Transactional
    @PostMapping(value = "/{id}/resultat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> soumettreResultat(
            @PathVariable Long id,
            @RequestParam(required = false) String commentaire,
            @RequestParam(value = "fichiers", required = false) List<MultipartFile> fichiers,
            Authentication authentication) {

        String username = authentication.getName();
        Mission mission = missionRepository.findByIdWithDetails(id).orElse(null);
        if (mission == null) return ResponseEntity.notFound().build();

        if (mission.getPrestataire() == null ||
            !username.equals(mission.getPrestataire().getUsername()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        boolean hasCommentaire = commentaire != null && !commentaire.isBlank();
        boolean hasFichiers    = fichiers != null && !fichiers.isEmpty() &&
                                 fichiers.stream().anyMatch(f -> f != null && !f.isEmpty());

        if (!hasCommentaire && !hasFichiers)
            return ResponseEntity.badRequest()
                   .body(Map.of("error", "Ajoutez un commentaire ou au moins un fichier"));

        try {
            // ── Sauvegarder résultat ──
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
                    if (fichier.getSize() > 20 * 1024 * 1024)
                        return ResponseEntity.badRequest().body(Map.of("error", "Fichier trop volumineux"));
                    String nomServeur = fileStorageService.stocker(fichier, "missions");
                    resultat.addFichier(FichierResultat.builder()
                        .nomFichierOriginal(fichier.getOriginalFilename())
                        .nomFichierServeur(nomServeur)
                        .typeMime(fichier.getContentType())
                        .tailleFichier(fichier.getSize())
                        .dateUpload(LocalDateTime.now())
                        .resultat(resultat)
                        .build());
                }
            }

            resultat.setMission(mission);
            resultatMissionRepository.save(resultat);

            // ── Changer statut ──
            StatutMission statutActuel = mission.getStatut();
            if (statutActuel == StatutMission.ASSIGNEE || statutActuel == StatutMission.REJETEE)
                missionService.changerStatut(id, StatutMission.EN_COURS);

            // ── Préparer infos prestataire ──
            if (mission.getPrestation() == null || mission.getPrestation().getDossier() == null) {
                log.warn("⚠️ prestation ou dossier null — notifications non envoyées");
                return ResponseEntity.ok(Map.of("message", "Résultat soumis avec succès"));
            }

            DossierContentieux dossier = mission.getPrestation().getDossier();

            // ── ✅ Recharger le dossier complet pour avoir agentCreateur et agence ──
            DossierContentieux dossierFull = dossierRepository
                .findById(dossier.getId())
                .orElse(dossier);

            // ── Résoudre agentUsername depuis TOUTES les sources disponibles ──
            String agentUsername = _resolveAgentUsername(dossierFull);

            log.info(">>> [DEBUG] dossier.id={}  creePar='{}'  agentCreateur='{}'  agentUsername résolu='{}'",
                dossierFull.getId(),
                dossierFull.getCreePar(),
                dossierFull.getAgentCreateur() != null ? dossierFull.getAgentCreateur().getUsername() : "NULL",
                agentUsername);

            String typePrestataire = mission.getPrestataire().getType() != null
                                     ? mission.getPrestataire().getType().name() : "Prestataire";
            String nomPrestataire  = (mission.getPrestataire().getPrenom() + " "
                                     + mission.getPrestataire().getNom()).trim();

            boolean estResoumission = statutActuel == StatutMission.FACTURE_REJETEE ||
                                      statutActuel == StatutMission.REJETEE;

            // ── Notification agent bancaire ──
            if (agentUsername != null && !agentUsername.isBlank()) {
                String titreAgent = estResoumission
                    ? "🔄 Documents resoumis par le prestataire"
                    : "📬 Résultats de mission soumis";
                String messageAgent = estResoumission
                    ? "Le prestataire " + nomPrestataire + " (" + typePrestataire + ")"
                      + " a resoumis les documents pour la mission "
                      + mission.getNumeroMission()
                      + " du dossier " + dossierFull.getNumeroDossier() + "."
                    : "Le prestataire " + nomPrestataire + " (" + typePrestataire + ")"
                      + " a soumis les résultats pour la mission "
                      + mission.getNumeroMission()
                      + " du dossier " + dossierFull.getNumeroDossier() + ".";

                      String urlAgent = "/agent/dossiers/" + dossierFull.getId()
                      + "/resultats-prestataires?missionId=" + mission.getId();
      notificationService.notifier(agentUsername, titreAgent, messageAgent,
          estResoumission ? "RESULTAT_MODIFIE" : "RESULTAT_SOUMIS", dossierFull, urlAgent);
                log.info("✅ Notification agent '{}' envoyée", agentUsername);
            } else {
                log.warn("⚠️ agentUsername vide — notification agent non envoyée pour dossier {}",
                    dossierFull.getId());
            }

            // ── Notification validateur financier ──
            _notifierValidateurFinancier(dossierFull, mission, nomPrestataire,
                typePrestataire, estResoumission);

            return ResponseEntity.ok(Map.of("message", "Résultat soumis avec succès"));

        } catch (Exception e) {
            log.error("Erreur soumission résultat mission {}", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    // MODIFIER RÉSULTAT
    // ─────────────────────────────────────────────────────────
    @Transactional
    @PutMapping(value = "/{id}/resultat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> modifierResultat(
            @PathVariable Long id,
            @RequestParam(required = false) String commentaire,
            @RequestParam(required = false) String pvTexte,
            @RequestParam(required = false) Double montant,
            @RequestParam(required = false) String factureRef,
            @RequestParam(value = "fichiers", required = false) List<MultipartFile> fichiers,
            Authentication authentication) {

        String username = authentication.getName();
        Mission mission = missionRepository.findByIdWithDetails(id).orElse(null);
        if (mission == null) return ResponseEntity.notFound().build();

        try {
            if (pvTexte    != null && !pvTexte.isBlank())    mission.setPvMission(pvTexte.trim());
            if (montant    != null && montant > 0)           mission.setMontantFacture(montant);
            if (factureRef != null && !factureRef.isBlank()) mission.setFactureRef(factureRef.trim());
            missionRepository.save(mission);

            ResultatMission resultat = resultatMissionRepository.findByMission_Id(id)
                .orElseGet(() -> ResultatMission.builder()
                    .mission(mission).fichiers(new ArrayList<>()).build());

            if (commentaire != null && !commentaire.isBlank()) resultat.setCommentaire(commentaire.trim());
            resultat.setSoumisePar(username);
            resultat.setDateSoumission(LocalDateTime.now());

            if (fichiers != null && fichiers.stream().anyMatch(f -> f != null && !f.isEmpty())) {
                for (MultipartFile fichier : fichiers) {
                    if (fichier == null || fichier.isEmpty()) continue;
                    String nomServeur = fileStorageService.stocker(fichier, "missions");
                    resultat.addFichier(FichierResultat.builder()
                        .nomFichierOriginal(fichier.getOriginalFilename())
                        .nomFichierServeur(nomServeur)
                        .typeMime(fichier.getContentType())
                        .tailleFichier(fichier.getSize())
                        .dateUpload(LocalDateTime.now())
                        .resultat(resultat)
                        .build());
                }
            }
            resultatMissionRepository.save(resultat);

            if (mission.getPrestation() != null && mission.getPrestation().getDossier() != null) {
                DossierContentieux dossier = mission.getPrestation().getDossier();
                DossierContentieux dossierFull = dossierRepository
                    .findById(dossier.getId()).orElse(dossier);

                String agentUsername   = _resolveAgentUsername(dossierFull);
                String typePrestataire = mission.getPrestataire().getType() != null
                                         ? mission.getPrestataire().getType().name() : "Prestataire";
                String nomPrestataire  = (mission.getPrestataire().getPrenom() + " "
                                         + mission.getPrestataire().getNom()).trim();

                if (agentUsername != null && !agentUsername.isBlank()) {
                    String urlAgent = "/agent/dossiers/" + dossierFull.getId()
                    + "/resultats-prestataires?missionId=" + mission.getId();
    notificationService.notifier(agentUsername,
        "✏️ Résultats de mission modifiés",
        "Le prestataire " + nomPrestataire + " (" + typePrestataire + ")"
        + " a modifié les résultats de la mission " + mission.getNumeroMission()
        + " du dossier " + dossierFull.getNumeroDossier() + ".",
        "RESULTAT_MODIFIE", dossierFull, urlAgent);
                }

                _notifierValidateurFinancier(dossierFull, mission, nomPrestataire, typePrestataire, true);
            }

            return ResponseEntity.ok(Map.of("message", "Résultat modifié avec succès"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    // TÉLÉCHARGEMENT
    // ─────────────────────────────────────────────────────────
    @GetMapping("/fichier/{nomServeur}")
    public ResponseEntity<Resource> telechargerFichier(@PathVariable String nomServeur) {
        try {
            Path chemin = fileStorageService.getCheminFichier("missions", nomServeur);
            Resource resource = new UrlResource(chemin.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomServeur + "\"")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/fichier/id/{fichierId}")
    public ResponseEntity<Resource> telechargerFichierParId(@PathVariable Long fichierId) {
        try {
            Optional<FichierResultat> opt = fichierResultatRepository.findById(fichierId);
            if (opt.isEmpty()) return ResponseEntity.notFound().build();
            FichierResultat fichier = opt.get();
            Path chemin = fileStorageService.getCheminFichier("missions", fichier.getNomFichierServeur());
            Resource resource = new UrlResource(chemin.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fichier.getNomFichierOriginal() + "\"")
                .header(HttpHeaders.CONTENT_TYPE,
                        fichier.getTypeMime() != null ? fichier.getTypeMime() : "application/octet-stream")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────────────────
    // SUPPRIMER FICHIER
    // ─────────────────────────────────────────────────────────
    @DeleteMapping("/fichier/{fichierId}")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
    @Transactional
    public ResponseEntity<?> supprimerFichier(@PathVariable Long fichierId, Authentication auth) {
        try {
            Optional<FichierResultat> opt = fichierResultatRepository.findById(fichierId);
            if (opt.isEmpty()) return ResponseEntity.notFound().build();

            FichierResultat fichier = opt.get();
            Mission mission = fichier.getResultat().getMission();

            if (mission.getPrestataire() == null ||
                !mission.getPrestataire().getUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));

            StatutMission statut = mission.getStatut();
            if (statut != StatutMission.ASSIGNEE && statut != StatutMission.EN_COURS)
                return ResponseEntity.badRequest()
                       .body(Map.of("error", "Suppression impossible. Statut : " + statut));

            try {
                Path chemin = fileStorageService.getCheminFichier("missions", fichier.getNomFichierServeur());
                Files.deleteIfExists(chemin);
            } catch (Exception ignored) {}

            fichierResultatRepository.delete(fichier);
            return ResponseEntity.ok(Map.of("message", "Fichier supprimé avec succès."));

        } catch (Exception e) {
            log.error("Erreur suppression fichier {}", fichierId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    // HELPER — résoudre agentUsername depuis toutes les sources
    // ─────────────────────────────────────────────────────────
    private String _resolveAgentUsername(DossierContentieux dossier) {
        // 1. Via agentCreateur (relation JPA)
        if (dossier.getAgentCreateur() != null &&
            dossier.getAgentCreateur().getUsername() != null &&
            !dossier.getAgentCreateur().getUsername().isBlank()) {
            return dossier.getAgentCreateur().getUsername();
        }
        // 2. Via creePar (string stocké directement)
        if (dossier.getCreePar() != null && !dossier.getCreePar().isBlank()) {
            return dossier.getCreePar();
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────
    // HELPER — notifier validateur financier
    // ─────────────────────────────────────────────────────────
    private void _notifierValidateurFinancier(DossierContentieux dossier,
                                              Mission mission,
                                              String nomPrestataire,
                                              String typePrestataire,
                                              boolean estResoumission) {
        try {
            if (dossier == null) {
                log.warn("⚠️ dossier null — validateur non notifié");
                return;
            }

            Long agenceId = null;
            if (dossier.getAgence() != null) {
                agenceId = dossier.getAgence().getId();
            } else {
                log.warn("⚠️ agence null sur dossier {} — tentative rechargement BDD", dossier.getId());
                DossierContentieux dossierFull = dossierRepository.findById(dossier.getId()).orElse(null);
                if (dossierFull != null && dossierFull.getAgence() != null) {
                    agenceId = dossierFull.getAgence().getId();
                    log.info("✅ agenceId rechargé: {}", agenceId);
                }
            }

            log.info(">>> [DEBUG] agenceId résolu={}", agenceId);

            if (agenceId == null) {
                log.warn("⚠️ agenceId toujours null — validateur non notifié");
                return;
            }

            List<Validateur> validateurs = validateurRepository
                .findByAgence_IdAndTypeValidateur(agenceId, TypeValidateur.VALIDATEUR_FINANCIER);

            log.info(">>> [DEBUG] validateurs financiers trouvés pour agence {}: {}",
                agenceId, validateurs.size());

            if (validateurs.isEmpty()) {
                log.warn("⚠️ Aucun validateur financier pour agence {}", agenceId);
                return;
            }

            String titre = estResoumission
                ? "🔄 Nouvelle facture resoumise — à revalider"
                : "💰 Facture en attente de validation financière";
            String message = estResoumission
                ? "Le prestataire " + nomPrestataire + " (" + typePrestataire + ")"
                  + " a resoumis une facture corrigée pour la mission "
                  + mission.getNumeroMission()
                  + " (dossier " + dossier.getNumeroDossier() + "). Merci de revalider."
                : "Le prestataire " + nomPrestataire + " (" + typePrestataire + ")"
                  + " a soumis une facture pour la mission "
                  + mission.getNumeroMission()
                  + " (dossier " + dossier.getNumeroDossier() + ")."
                  + " Une validation financière est requise.";

                  String urlValidateur = "/agent/dossiers/" + dossier.getId()
                  + "/resultats-prestataires?missionId=" + mission.getId();
for (Validateur v : validateurs) {
 notificationService.notifier(
     v.getUsername(), titre, message, "VALIDATION_FINANCIERE", dossier, urlValidateur);
 log.info("✅ Validateur financier {} notifié pour mission {}",
     v.getUsername(), mission.getNumeroMission());
}

        } catch (Exception e) {
            log.error("Erreur notification validateur financier: {}", e.getMessage(), e);
        }
    }
}