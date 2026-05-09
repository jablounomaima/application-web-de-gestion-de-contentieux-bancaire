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
import com.example.contentieux_security.enums.StatutMission;
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
import org.springframework.format.annotation.DateTimeFormat;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/missions")
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

    @GetMapping
    public ResponseEntity<?> mesMissions(Authentication auth, @RequestParam(required = false) String recherche) {
        String username = auth.getName();
        if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
            username = oidcUser.getPreferredUsername();
        } else if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
            Object preferred = oauth2User.getAttribute("preferred_username");
            if (preferred != null) username = preferred.toString();
        }

        List<Mission> missions = missionRepository.findByPrestation_Dossier_CreeParWithDetails(username);

        if (recherche != null && !recherche.isBlank()) {
            String kw = recherche.toLowerCase();
            missions = missions.stream()
                .filter(m -> m.getPrestation() != null && m.getPrestation().getDossier() != null && (
                    (m.getPrestation().getDossier().getNumeroDossier() != null && m.getPrestation().getDossier().getNumeroDossier().toLowerCase().contains(kw)) ||
                    (m.getPrestation().getDossier().getClient() != null && m.getPrestation().getDossier().getClient().getNom().toLowerCase().contains(kw))
                )).toList();
        }

        Map<String, List<Mission>> missionsParDossier = missions.stream()
                .filter(m -> m.getPrestation() != null && m.getPrestation().getDossier() != null)
                .collect(Collectors.groupingBy(m -> m.getPrestation().getDossier().getNumeroDossier(), LinkedHashMap::new, Collectors.toList()));

        long missionsRejetees = missions.stream().filter(m -> m.getStatut() == StatutMission.REJETEE).count();

        Map<String, Object> response = new HashMap<>();
        response.put("missions", missions);
        response.put("missionsParDossier", missionsParDossier);
        response.put("totalMissions", missions.size());
        response.put("missionsRejetees", missionsRejetees);

        return ResponseEntity.ok(response);
    }

    @Transactional
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE','AVOCAT','AGENT')")
    public ResponseEntity<?> detailMission(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        Mission mission = prestationService.getMissionByIdWithDetails(id);

        if (mission == null) {
            return ResponseEntity.notFound().build();
        }

        boolean isPrestataire = mission.getPrestataire() != null && username.equals(mission.getPrestataire().getUsername());
        boolean isAgent = mission.getPrestation() != null && mission.getPrestation().getDossier() != null &&
                          mission.getPrestation().getDossier().getAgentCreateur() != null &&
                          username.equals(mission.getPrestation().getDossier().getAgentCreateur().getUsername());
        boolean isAvocat = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("AVOCAT"));

        if (!isPrestataire && !isAgent && !isAvocat) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));
        }

        List<ResultatMission> resultats = resultatMissionRepository.findByMission_IdOrderByDateSoumissionDesc(id);

        List<HistoriqueDossier> historique = List.of();
        try {
            Long dossierId = mission.getPrestation().getDossier().getId();
            historique = historiqueService.getHistorique(dossierId);
        } catch (Exception ignored) {}

        boolean resultatVerrouille = false;
        if (mission.getResultatMission() != null) {
            StatutMission s = mission.getStatut();
            resultatVerrouille = s == StatutMission.TERMINEE || s == StatutMission.REJETEE || s == StatutMission.VALIDEE_AGENT;
        }

        AffaireJudiciaire affaire = null;
        if (isAvocat) {
            try {
                affaire = affaireJudiciaireService.findByMissionId(id);
            } catch (Exception ignored) {}
        }

        Map<String, Object> response = new HashMap<>();
        response.put("mission", mission);
        response.put("resultats", resultats);
        response.put("historique", historique);
        response.put("resultatVerrouille", resultatVerrouille);
        response.put("isAgent", isAgent);
        response.put("isPrestataire", isPrestataire);
        response.put("isAvocat", isAvocat);
        response.put("affaire", affaire);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modifierMission(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication authentication) {
        try {
            java.time.LocalDate dateFinPrevue = body.get("dateFinPrevue") != null ? java.time.LocalDate.parse(body.get("dateFinPrevue").toString()) : null;
            missionService.modifierMission(id, (String) body.get("description"), dateFinPrevue, authentication.getName());
            return ResponseEntity.ok(Map.of("message", "Mission modifiée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerMission(@PathVariable Long id, Authentication authentication) {
        try {
            missionService.supprimerMission(id, authentication.getName());
            return ResponseEntity.ok(Map.of("message", "Mission supprimée"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Transactional
    @PostMapping(value = "/{id}/resultat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> soumettreResultat(@PathVariable Long id,
                                               @RequestParam(required = false) String commentaire,
                                               @RequestParam(value = "fichiers", required = false) List<MultipartFile> fichiers,
                                               Authentication authentication) {
        String username = authentication.getName();
        Mission mission = missionRepository.findByIdWithPrestataire(id).orElse(null);

        if (mission == null) return ResponseEntity.notFound().build();

        if (mission.getPrestataire() == null || !username.equals(mission.getPrestataire().getUsername())) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));
        }

        boolean hasCommentaire = commentaire != null && !commentaire.isBlank();
        boolean hasFichiers = fichiers != null && !fichiers.isEmpty() && fichiers.stream().anyMatch(f -> f != null && !f.isEmpty());

        if (!hasCommentaire && !hasFichiers) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ajoutez un commentaire ou au moins un fichier"));
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
                        return ResponseEntity.badRequest().body(Map.of("error", "Fichier trop volumineux"));
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

            return ResponseEntity.ok(Map.of("message", "Résultat soumis avec succès"));
        } catch (Exception e) {
            log.error("Erreur soumission", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

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
            Optional<FichierResultat> fichierOpt = fichierResultatRepository.findById(fichierId);
            if (fichierOpt.isEmpty()) return ResponseEntity.notFound().build();
            FichierResultat fichier = fichierOpt.get();
            Path chemin = fileStorageService.getCheminFichier("missions", fichier.getNomFichierServeur());
            Resource resource = new UrlResource(chemin.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fichier.getNomFichierOriginal() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, fichier.getTypeMime() != null ? fichier.getTypeMime() : "application/octet-stream")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Transactional
    @PutMapping(value = "/{id}/resultat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> modifierResultat(@PathVariable Long id,
                                              @RequestParam(required = false) String commentaire,
                                              @RequestParam(required = false) String pvTexte,
                                              @RequestParam(required = false) Double montant,
                                              @RequestParam(required = false) String factureRef,
                                              @RequestParam(value = "fichiers", required = false) List<MultipartFile> fichiers,
                                              Authentication authentication) {
        String username = authentication.getName();
        Mission mission = missionRepository.findByIdWithPrestataire(id).orElse(null);

        if (mission == null) return ResponseEntity.notFound().build();

        try {
            if (pvTexte != null && !pvTexte.isBlank()) mission.setPvMission(pvTexte.trim());
            if (montant != null && montant > 0) mission.setMontantFacture(montant);
            if (factureRef != null && !factureRef.isBlank()) mission.setFactureRef(factureRef.trim());
            missionRepository.save(mission);

            ResultatMission resultat = resultatMissionRepository.findByMission_Id(id)
                .orElseGet(() -> ResultatMission.builder().mission(mission).fichiers(new ArrayList<>()).build());

            if (commentaire != null && !commentaire.isBlank()) resultat.setCommentaire(commentaire.trim());
            resultat.setSoumisePar(username);
            resultat.setDateSoumission(LocalDateTime.now());

            boolean hasFichiers = fichiers != null && fichiers.stream().anyMatch(f -> f != null && !f.isEmpty());
            if (hasFichiers) {
                for (MultipartFile fichier : fichiers) {
                    if (fichier == null || fichier.isEmpty()) continue;
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
            return ResponseEntity.ok(Map.of("message", "Résultat modifié avec succès"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}