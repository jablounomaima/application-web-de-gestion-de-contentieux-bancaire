package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.repository.MissionRepository;
import com.example.contentieux_security.service.*;
import java.security.Principal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import com.example.contentieux_security.repository.ResultatMissionRepository;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/prestataire")
@RequiredArgsConstructor
@Slf4j
public class PrestataireController {

    private final ResultatMissionRepository resultatMissionRepository;
    private final PrestationService         prestationService;
    private final FichierResultatService    fichierResultatService;
    private final DossierService            dossierService;
    private final MissionService            missionService;
    private final MissionRepository         missionRepository;
    private final PrestataireService        prestataireService;
    private final NotificationService       notificationService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    // ─────────────────────────────────────────────────────────
    // DASHBOARD
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('AVOCAT','EXPERT','PRESTATAIRE','HUISSIER')")
    @Transactional
    public ResponseEntity<?> dashboard(Authentication auth) {
        List<Mission> missions = prestationService.getMissionsPrestataire(auth.getName());

        long enCours        = missions.stream().filter(m -> m.getStatut() == StatutMission.ASSIGNEE || m.getStatut() == StatutMission.EN_COURS).count();
        long pvSoumis       = missions.stream().filter(m -> m.getStatut() == StatutMission.PV_SOUMIS).count();
        long factureSoumise = missions.stream().filter(m -> m.getStatut() == StatutMission.FACTURE_SOUMISE).count();
        long terminees      = missions.stream().filter(m -> m.getStatut() == StatutMission.TERMINEE || m.getStatut() == StatutMission.REALISEE).count();

        List<Map<String, Object>> missionsMap = new ArrayList<>();
        for (Mission m : missions) {
            Map<String, Object> mm = new HashMap<>();
            mm.put("id",               m.getId());
            mm.put("numeroMission",    m.getNumeroMission());
            mm.put("statut",           m.getStatut());
            mm.put("dateAssignation",  m.getDateAssignation());
            mm.put("description",      m.getDescription());
            mm.put("commentaireAgent", m.getCommentaireAgent());

            if (m.getPrestation() != null && m.getPrestation().getDossier() != null) {
                var d = m.getPrestation().getDossier();
                var c = d.getClient();
                Map<String, Object> clientMap = new HashMap<>();
                if (c != null) {
                    clientMap.put("nom",    c.getNom());
                    clientMap.put("prenom", c.getPrenom());
                }
                Map<String, Object> dossierMap = new HashMap<>();
                dossierMap.put("numeroDossier", d.getNumeroDossier());
                dossierMap.put("libelle",       d.getLibelle());
                dossierMap.put("dateCreation",  d.getDateCreation());
                dossierMap.put("montant",       d.calculerSolde());
                dossierMap.put("client",        clientMap);

                Map<String, Object> prestationMap = new HashMap<>();
                prestationMap.put("type",    m.getPrestation().getType());
                prestationMap.put("dossier", dossierMap);
                mm.put("prestation", prestationMap);
            }
            missionsMap.add(mm);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("dernieresMissions", missionsMap);
        response.put("totalMissions",     missions.size());
        response.put("missionsEnCours",   enCours);
        response.put("pvSoumis",          pvSoumis);
        response.put("factureSoumise",    factureSoumise);
        response.put("missionsTerminees", terminees);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────
    // LISTE DES MISSIONS
    // ─────────────────────────────────────────────────────────
    @GetMapping("/missions")
    @PreAuthorize("hasAnyRole('AVOCAT','EXPERT','PRESTATAIRE','HUISSIER')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMissions(
            @RequestParam(required = false) String recherche,
            Authentication auth) {

        List<Mission> missions = prestationService.getMissionsPrestataire(auth.getName());

        if (recherche != null && !recherche.isBlank()) {
            String q = recherche.toLowerCase();
            missions = missions.stream()
                .filter(m -> (m.getNumeroMission() != null && m.getNumeroMission().toLowerCase().contains(q))
                          || (m.getDescription()   != null && m.getDescription().toLowerCase().contains(q)))
                .toList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Mission m : missions) {
            Map<String, Object> mm = new HashMap<>();
            mm.put("id",               m.getId());
            mm.put("numeroMission",    m.getNumeroMission());
            mm.put("statut",           m.getStatut());
            mm.put("dateAssignation",  m.getDateAssignation());
            mm.put("description",      m.getDescription());
            mm.put("commentaireAgent", m.getCommentaireAgent());

            if (m.getPrestation() != null && m.getPrestation().getDossier() != null) {
                var d = m.getPrestation().getDossier();
                Map<String, Object> clientMap = new HashMap<>();
                if (d.getClient() != null) {
                    clientMap.put("nom",    d.getClient().getNom());
                    clientMap.put("prenom", d.getClient().getPrenom());
                }
                Map<String, Object> dossierMap = new HashMap<>();
                dossierMap.put("numeroDossier", d.getNumeroDossier());
                dossierMap.put("libelle",       d.getLibelle());
                dossierMap.put("montant",       d.calculerSolde());
                dossierMap.put("client",        clientMap);

                Map<String, Object> prestationMap = new HashMap<>();
                prestationMap.put("type",    m.getPrestation().getType());
                prestationMap.put("dossier", dossierMap);
                mm.put("prestation", prestationMap);
            }
            result.add(mm);
        }

        return ResponseEntity.ok(Map.of("missions", result, "total", result.size()));
    }

    // ─────────────────────────────────────────────────────────
    // LISTE DES FACTURES
    // ─────────────────────────────────────────────────────────
    @GetMapping("/factures")
    @PreAuthorize("hasAnyRole('AVOCAT','EXPERT','PRESTATAIRE','HUISSIER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getMesFactures(Authentication auth) {
        List<Mission> missions = prestationService.getMissionsPrestataire(auth.getName());

        List<Map<String, Object>> factures = missions.stream()
            .filter(m -> m.getFactureRef() != null)
            .map(m -> {
                Map<String, Object> f = new HashMap<>();
                f.put("factureRef",     m.getFactureRef());
                f.put("montant",        m.getMontantFacture());
                f.put("dateSoumission", m.getDateValidationFacture());
                f.put("statut", switch (m.getStatut()) {
                    case FACTURE_SOUMISE     -> "EN_ATTENTE";
                    case FACTURE_VALIDEE     -> "APPROUVEE";
                    case FACTURE_REJETEE     -> "REJETEE";
                    case TERMINEE, REALISEE  -> "PAYEE";
                    default                  -> m.getStatut().name();
                });

                Map<String, Object> missionMap = new HashMap<>();
                missionMap.put("numeroMission", m.getNumeroMission());

                if (m.getPrestation() != null && m.getPrestation().getDossier() != null) {
                    var d = m.getPrestation().getDossier();
                    Map<String, Object> clientMap = new HashMap<>();
                    if (d.getClient() != null) {
                        clientMap.put("nom",    d.getClient().getNom());
                        clientMap.put("prenom", d.getClient().getPrenom());
                    }
                    Map<String, Object> dossierMap = new HashMap<>();
                    dossierMap.put("numeroDossier", d.getNumeroDossier());
                    dossierMap.put("client",        clientMap);

                    Map<String, Object> prestationMap = new HashMap<>();
                    prestationMap.put("dossier", dossierMap);
                    missionMap.put("prestation", prestationMap);
                }
                f.put("mission", missionMap);
                return f;
            })
            .toList();

        return ResponseEntity.ok(factures);
    }

    // ─────────────────────────────────────────────────────────
    // FORMULAIRES GET PV / FACTURE
    // ─────────────────────────────────────────────────────────
    @GetMapping("/missions/{missionId}/pv")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
    public ResponseEntity<?> formulairePV(@PathVariable Long missionId, Principal principal) {
        Mission mission = missionService.getMissionWithDetails(missionId);
        if (mission.getPrestataire() == null ||
            !mission.getPrestataire().getUsername().equals(principal.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        }
        return ResponseEntity.ok(Map.of("mission", mission));
    }

    @GetMapping("/missions/{missionId}/facture")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
    public ResponseEntity<?> formulaireFacture(@PathVariable Long missionId, Principal principal) {
        Mission mission = missionService.getMissionWithDetails(missionId);
        if (mission.getPrestataire() == null ||
            !mission.getPrestataire().getUsername().equals(principal.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        }
        return ResponseEntity.ok(Map.of("mission", mission));
    }

    // ─────────────────────────────────────────────────────────
    // SOUMETTRE PV  ASSIGNEE / EN_COURS → PV_SOUMIS
    // ─────────────────────────────────────────────────────────
    @PostMapping("/missions/{id}/pv")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
    public ResponseEntity<?> soumettrePV(@PathVariable Long id,
                                         @RequestBody Map<String, String> body,
                                         Authentication auth) {
        try {
            Mission mission = missionRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new RuntimeException("Mission introuvable"));

            if (mission.getPrestataire() == null ||
                !mission.getPrestataire().getUsername().equals(auth.getName())) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }

            StatutMission statut = mission.getStatut();
            if (statut != StatutMission.ASSIGNEE && statut != StatutMission.EN_COURS) {
                return ResponseEntity.badRequest()
                       .body(Map.of("error", "PV déjà soumis ou mission non active. Statut : " + statut));
            }

            mission.setPvMission(body.get("pvTexte"));
            mission.setStatut(StatutMission.PV_SOUMIS);
            mission.setDateValidationPv(LocalDateTime.now());
            missionRepository.save(mission);

            DossierContentieux dossierPV = mission.getPrestation() != null
                    ? mission.getPrestation().getDossier() : null;
            if (dossierPV != null && dossierPV.getAgentCreateur() != null) {
                notificationService.notifier(
                    dossierPV.getAgentCreateur().getUsername(),
                    "📄 PV de mission soumis",
                    "Le prestataire a soumis le PV de la mission "
                    + mission.getNumeroMission()
                    + " (dossier " + dossierPV.getNumeroDossier() + ").",
                    "PV_SOUMIS",
                    dossierPV
                );
            }

            return ResponseEntity.ok(Map.of("message", "PV soumis avec succès."));
        } catch (Exception e) {
            log.error("Erreur soumission PV mission {} : {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    // SOUMETTRE FACTURE  PV_SOUMIS → FACTURE_SOUMISE
    // ─────────────────────────────────────────────────────────
    @PostMapping("/missions/{id}/facture")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
    public ResponseEntity<?> soumettreFacture(@PathVariable Long id,
                                              @RequestBody Map<String, Object> body,
                                              Authentication auth) {
        try {
            Mission mission = missionRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new RuntimeException("Mission introuvable"));

            if (mission.getPrestataire() == null ||
                !mission.getPrestataire().getUsername().equals(auth.getName())) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }

            if (mission.getStatut() != StatutMission.PV_SOUMIS) {
                return ResponseEntity.badRequest()
                       .body(Map.of("error", "Soumettez d'abord le PV. Statut actuel : " + mission.getStatut()));
            }

            mission.setFactureRef((String) body.get("factureRef"));
            mission.setMontantFacture(Double.valueOf(body.get("montant").toString()));
            mission.setStatut(StatutMission.FACTURE_SOUMISE);
            mission.setDateValidationFacture(LocalDateTime.now());
            missionRepository.save(mission);

            DossierContentieux dossierFact = mission.getPrestation() != null
                    ? mission.getPrestation().getDossier() : null;

                    if (dossierFact != null && dossierFact.getAgentCreateur() != null) {
                        String urlFacture = "/agent/dossiers/" + dossierFact.getId()
                                          + "/resultats-prestataires?missionId=" + mission.getId();
                        notificationService.notifier(
                            dossierFact.getAgentCreateur().getUsername(),
                            "🧾 Facture soumise — mission " + mission.getNumeroMission(),
                            "Le prestataire a soumis une facture de "
                            + mission.getMontantFacture() + " DT pour la mission "
                            + mission.getNumeroMission()
                            + " — dossier " + dossierFact.getNumeroDossier() + ".",
                            "FACTURE_SOUMISE",
                            dossierFact,
                            urlFacture    // ← URL explicite avec missionId
                        );
                    }

            // ✅ FIX : getValidateurFinancierUsername() retourne un String directement
            if (dossierFact != null && dossierFact.getValidateurFinancierUsername() != null) {
                notificationService.notifier(
                    dossierFact.getValidateurFinancierUsername(),
                    "🧾 Facture en attente de validation",
                    "Une facture a été soumise pour la mission "
                    + mission.getNumeroMission()
                    + " (dossier " + dossierFact.getNumeroDossier() + "). Votre validation est requise.",
                    "FACTURE_SOUMISE",
                    dossierFact
                );
            }

            return ResponseEntity.ok(Map.of("message", "Facture soumise avec succès."));
        } catch (Exception e) {
            log.error("Erreur soumission facture mission {} : {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    // CAS 1 — REJET AGENT : resoumettre PV + facture + documents
    // REJETEE → PV_SOUMIS → FACTURE_SOUMISE
    // ─────────────────────────────────────────────────────────
    @PostMapping("/missions/{missionId}/resoumettre-pv")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
    @Transactional
    public ResponseEntity<?> resoumettreApresRejetAgent(
            @PathVariable Long missionId,
            @RequestParam("pvTexte")                            String pvTexte,
            @RequestParam("factureRef")                         String factureRef,
            @RequestParam("montant")                            Double montant,
            @RequestParam(value = "fichiers", required = false) List<MultipartFile> fichiers,
            Authentication auth) {
        try {
            missionService.resoumettreApresRejetAgent(missionId, pvTexte, auth.getName());
            missionService.soumettreFacture(missionId, factureRef, montant, auth.getName());
            _uploadDocuments(missionId, fichiers, auth.getName());

            Mission missionResout = missionRepository.findByIdWithDetails(missionId).orElse(null);
            if (missionResout != null) {
                DossierContentieux dossierR = missionResout.getPrestation() != null
                        ? missionResout.getPrestation().getDossier() : null;

                if (dossierR != null && dossierR.getAgentCreateur() != null) {
                    notificationService.notifier(
                        dossierR.getAgentCreateur().getUsername(),
                        "🔄 Resoumission — mission " + missionResout.getNumeroMission(),
                        "Le prestataire a resoumis le PV et la facture de la mission "
                        + missionResout.getNumeroMission() + " après rejet.",
                        "RESOUMISSION",
                        dossierR
                    );
                }
                // ✅ FIX : String — pas de .getUsername() supplémentaire
                if (dossierR != null && dossierR.getValidateurFinancierUsername() != null) {
                    notificationService.notifier(
                        dossierR.getValidateurFinancierUsername(),
                        "🔄 Nouvelle facture à valider — mission " + missionResout.getNumeroMission(),
                        "Le prestataire a resoumis sa facture pour la mission "
                        + missionResout.getNumeroMission() + ". Votre validation est requise.",
                        "FACTURE_SOUMISE",
                        dossierR
                    );
                }
            }
            return ResponseEntity.ok(Map.of("message", "PV, facture et documents soumis avec succès."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur resoumettre-pv mission {} : {}", missionId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    // CAS 2 — REJET FINANCIER : resoumettre facture seule
    // FACTURE_REJETEE → FACTURE_SOUMISE
    // ─────────────────────────────────────────────────────────
    @PostMapping("/missions/{missionId}/resoumettre-facture")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
    @Transactional
    public ResponseEntity<?> resoumettreFactureApresRejetFinancier(
            @PathVariable Long missionId,
            @RequestParam("factureRef")                         String factureRef,
            @RequestParam("montant")                            Double montant,
            @RequestParam(value = "fichiers", required = false) List<MultipartFile> fichiers,
            Authentication auth) {
        try {
            missionService.resoumettreFactureApresRejetFinancier(missionId, factureRef, montant, auth.getName());
            _uploadDocuments(missionId, fichiers, auth.getName());

            Mission missionResoumise = missionRepository.findByIdWithDetails(missionId).orElse(null);
            if (missionResoumise != null) {
                DossierContentieux dossierRF = missionResoumise.getPrestation() != null
                        ? missionResoumise.getPrestation().getDossier() : null;

                if (dossierRF != null && dossierRF.getAgentCreateur() != null) {
                    notificationService.notifier(
                        dossierRF.getAgentCreateur().getUsername(),
                        "🔄 Facture corrigée soumise — mission " + missionResoumise.getNumeroMission(),
                        "Le prestataire a resoumis sa facture corrigée pour la mission "
                        + missionResoumise.getNumeroMission() + " après rejet financier.",
                        "RESOUMISSION",
                        dossierRF
                    );
                }
                // ✅ FIX : String — pas de .getUsername() supplémentaire
                if (dossierRF != null && dossierRF.getValidateurFinancierUsername() != null) {
                    notificationService.notifier(
                        dossierRF.getValidateurFinancierUsername(),
                        "🔄 Nouvelle facture à valider — mission " + missionResoumise.getNumeroMission(),
                        "Le prestataire a resoumis une nouvelle facture pour la mission "
                        + missionResoumise.getNumeroMission() + ". Votre validation est requise.",
                        "FACTURE_SOUMISE",
                        dossierRF
                    );
                }
            }
            return ResponseEntity.ok(Map.of("message", "Facture et documents resoumis avec succès."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur resoumettre-facture mission {} : {}", missionId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    // MÉTHODE PRIVÉE — upload documents
    // ─────────────────────────────────────────────────────────
    private void _uploadDocuments(Long missionId,
                                  List<MultipartFile> fichiers,
                                  String username) throws Exception {
        if (fichiers == null || fichiers.isEmpty()) return;

        Mission mission = missionRepository.findByIdWithDetails(missionId)
                .orElseThrow(() -> new RuntimeException("Mission introuvable"));

        ResultatMission resultat = resultatMissionRepository
                .findByMission_Id(missionId)
                .orElseGet(() -> {
                    ResultatMission r = new ResultatMission();
                    r.setMission(mission);
                    r.setDateCreation(LocalDateTime.now());
                    r.setDateModification(LocalDateTime.now());
                    r.setDateSoumission(LocalDateTime.now());
                    r.setSoumisePar(username);
                    r.setCommentaire("");
                    return resultatMissionRepository.save(r);
                });

        resultat.setDateSoumission(LocalDateTime.now());
        resultat.setDateModification(LocalDateTime.now());
        resultatMissionRepository.save(resultat);

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        for (MultipartFile f : fichiers) {
            String nomServeur = UUID.randomUUID() + "_" + f.getOriginalFilename();
            Files.copy(f.getInputStream(), uploadPath.resolve(nomServeur), StandardCopyOption.REPLACE_EXISTING);
            fichierResultatService.save(
                FichierResultat.builder()
                    .nomFichierOriginal(f.getOriginalFilename())
                    .nomFichierServeur(nomServeur)
                    .typeMime(f.getContentType())
                    .tailleFichier(f.getSize())
                    .dateUpload(LocalDateTime.now())
                    .resultat(resultat)
                    .build()
            );
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET RÉSULTAT MISSION
    // ─────────────────────────────────────────────────────────
    @GetMapping("/missions/{missionId}/resultat")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getResultatMission(@PathVariable Long missionId,
                                                Authentication auth) {
        try {
            Mission mission = missionRepository.findById(missionId)
                    .orElseThrow(() -> new RuntimeException("Mission introuvable"));

            if (mission.getPrestataire() == null ||
                !mission.getPrestataire().getUsername().equals(auth.getName())) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("pvTexte",              mission.getPvMission());
            response.put("dateSoumissionPV",     mission.getDateValidationPv());
            response.put("factureRef",           mission.getFactureRef());
            response.put("montant",              mission.getMontantFacture());
            response.put("dateSoumissionFacture",mission.getDateValidationFacture());
            response.put("commentaireAgent",     mission.getCommentaireAgent());

            Optional<ResultatMission> opt = resultatMissionRepository.findByMission_Id(missionId);
            response.put("commentaire", opt.map(ResultatMission::getCommentaire).orElse(null));
            response.put("soumisePar",  opt.map(ResultatMission::getSoumisePar).orElse(null));

            List<Map<String, Object>> fichiers = new ArrayList<>();
            opt.ifPresent(r -> {
                if (r.getFichiers() != null) {
                    for (FichierResultat f : r.getFichiers()) {
                        fichiers.add(Map.of(
                            "id",                f.getId(),
                            "nomFichierOriginal", f.getNomFichierOriginal(),
                            "nomFichierServeur",  f.getNomFichierServeur(),
                            "typeMime",           f.getTypeMime(),
                            "tailleFichier",      f.getTailleFichier(),
                            "dateUpload",         f.getDateUpload()
                        ));
                    }
                }
            });
            response.put("fichiers", fichiers);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur getResultatMission {} : {}", missionId, e.getMessage(), e);
            return ResponseEntity.status(500)
                   .body(Map.of("error",  e.getMessage() != null ? e.getMessage() : "Erreur inconnue",
                                "cause",  e.getClass().getSimpleName()));
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET MISSION DÉTAIL
    // ─────────────────────────────────────────────────────────
    @GetMapping("/missions/{missionId}")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMissionDetail(@PathVariable Long missionId,
                                              Authentication auth) {
        try {
            Mission mission = missionRepository.findByIdWithDetails(missionId)
                    .orElseThrow(() -> new RuntimeException("Mission introuvable"));

            if (mission.getPrestataire() == null ||
                !mission.getPrestataire().getUsername().equals(auth.getName())) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id",               mission.getId());
            response.put("numeroMission",    mission.getNumeroMission());
            response.put("statut",           mission.getStatut() != null ? mission.getStatut().name() : null);
            response.put("dateAssignation",  mission.getDateAssignation());
            response.put("dateFinPrevue",    mission.getDateFinPrevue());
            response.put("dateRealisation",  mission.getDateRealisation());
            response.put("description",      mission.getDescription());
            response.put("commentaireAgent", mission.getCommentaireAgent());
            response.put("pvMission",        mission.getPvMission());
            response.put("montantFacture",   mission.getMontantFacture());
            response.put("factureRef",       mission.getFactureRef());
            response.put("isAgent",          false);
            response.put("dateValidationPv",      mission.getDateValidationPv());
            response.put("dateValidationFacture", mission.getDateValidationFacture());
            response.put("pvValide",      mission.getStatut() == StatutMission.VALIDEE_AGENT
                                       || mission.getStatut() == StatutMission.TERMINEE);
            response.put("factureValide", mission.getStatut() == StatutMission.TERMINEE);

            if (mission.getPrestataire() != null) {
                var p = mission.getPrestataire();
                response.put("prestataire", Map.of(
                    "nom",      p.getNom()    != null ? p.getNom()    : "",
                    "prenom",   p.getPrenom() != null ? p.getPrenom() : "",
                    "username", p.getUsername(),
                    "type",     p.getType()   != null ? p.getType().name() : "",
                    "email",    p.getEmail()  != null ? p.getEmail()  : ""
                ));
            }

            if (mission.getPrestation() != null) {
                Map<String, Object> prestation = new HashMap<>();
                prestation.put("type", mission.getPrestation().getType());

                if (mission.getPrestation().getDossier() != null) {
                    var d = mission.getPrestation().getDossier();
                    Map<String, Object> dossier = new HashMap<>();
                    dossier.put("id",            d.getId());
                    dossier.put("numeroDossier", d.getNumeroDossier());
                    dossier.put("libelle",       d.getLibelle());
                    dossier.put("dateCreation",  d.getDateCreation());
                    dossier.put("montant",       d.calculerSolde());
                    if (d.getClient() != null) {
                        dossier.put("client", Map.of(
                            "nom",    d.getClient().getNom(),
                            "prenom", d.getClient().getPrenom()
                        ));
                    }
                    prestation.put("dossier", dossier);
                }
                response.put("prestation", prestation);
            }

            List<Map<String, Object>> resultats = new ArrayList<>();
            resultatMissionRepository.findByMission_Id(missionId).ifPresent(r -> {
                List<Map<String, Object>> fichiers = new ArrayList<>();
                if (r.getFichiers() != null) {
                    for (FichierResultat f : r.getFichiers()) {
                        fichiers.add(Map.of(
                            "id",                 f.getId(),
                            "nomFichierOriginal", f.getNomFichierOriginal(),
                            "nomFichierServeur",  f.getNomFichierServeur(),
                            "typeMime",           f.getTypeMime() != null ? f.getTypeMime() : "",
                            "tailleFichier",      f.getTailleFichier(),
                            "dateUpload",         f.getDateUpload()
                        ));
                    }
                }
                Map<String, Object> resultat = new HashMap<>();
                resultat.put("commentaire",    r.getCommentaire());
                resultat.put("soumisePar",     r.getSoumisePar());
                resultat.put("dateSoumission", r.getDateSoumission());
                resultat.put("fichiers",       fichiers);
                resultats.add(resultat);
            });

            response.put("resultats",          resultats);
            response.put("historique",         List.of());
            response.put("resultatVerrouille", false);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur getMissionDetail {} : {}", missionId, e.getMessage(), e);
            return ResponseEntity.status(500)
                   .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Erreur inconnue"));
        }
    }

    // ─────────────────────────────────────────────────────────
    // DOSSIER MISSION
    // ─────────────────────────────────────────────────────────
    @GetMapping("/missions/{missionId}/dossier")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
    @Transactional
    public ResponseEntity<?> getDossierMission(@PathVariable Long missionId,
                                               Authentication auth) {
        Mission mission = missionService.getMissionWithDetails(missionId);
        if (mission.getPrestataire() == null ||
            !mission.getPrestataire().getUsername().equals(auth.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        }

        DossierContentieux dossier = mission.getPrestation().getDossier();

        Map<String, Object> missionMap = new HashMap<>();
        missionMap.put("id",            mission.getId());
        missionMap.put("numeroMission", mission.getNumeroMission());
        missionMap.put("statut",        mission.getStatut());
        missionMap.put("description",   mission.getDescription());
        missionMap.put("dateFinPrevue", mission.getDateFinPrevue());

        Map<String, Object> clientMap = new HashMap<>();
        if (dossier.getClient() != null) {
            var c = dossier.getClient();
            clientMap.put("nom",       c.getNom());
            clientMap.put("prenom",    c.getPrenom());
            clientMap.put("email",     c.getEmail());
            clientMap.put("telephone", c.getTelephone());
            clientMap.put("adresse",   c.getAdresse());
        }

        List<Map<String, Object>> risquesMap = new ArrayList<>();
        if (dossier.getRisques() != null) {
            for (var r : dossier.getRisques()) {
                List<Map<String, Object>> garantiesMap = new ArrayList<>();
                if (r.getGaranties() != null) {
                    for (var g : r.getGaranties()) {
                        garantiesMap.add(Map.of(
                            "typeGarantie",  g.getTypeGarantie(),
                            "valeurEstimee", g.getValeurEstimee(),
                            "description",   g.getDescription() != null ? g.getDescription() : "",
                            "statut",        g.getStatut()
                        ));
                    }
                }
                Map<String, Object> rm = new HashMap<>();
                rm.put("type",          r.getType());
                rm.put("montant",       r.getMontantInitial());
                rm.put("montantImpaye", r.getMontantImpaye());
                rm.put("selectionne",   r.isSelectionne());
                rm.put("dateEcheance",  r.getDateEcheance());
                rm.put("description",   r.getDescription());
                rm.put("garanties",     garantiesMap);
                risquesMap.add(rm);
            }
        }

        Map<String, Object> dossierMap = new HashMap<>();
        dossierMap.put("numeroDossier", dossier.getNumeroDossier());
        dossierMap.put("libelle",       dossier.getLibelle());
        dossierMap.put("statut",        dossier.getStatut());
        dossierMap.put("montant",       dossier.calculerSolde());
        dossierMap.put("dateCreation",  dossier.getDateCreation());
        dossierMap.put("agenceNom",     dossier.getAgence() != null ? dossier.getAgence().getNom() : null);
        dossierMap.put("creePar",       dossier.getCreePar());
        dossierMap.put("client",        clientMap);
        dossierMap.put("risques",       risquesMap);

        return ResponseEntity.ok(Map.of("mission", missionMap, "dossier", dossierMap));
    }

    // ─────────────────────────────────────────────────────────
    // UPLOAD DOCUMENTS (endpoint séparé)
    // ─────────────────────────────────────────────────────────
    @PostMapping("/missions/{missionId}/documents")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
    public ResponseEntity<?> ajouterDocuments(
            @PathVariable Long missionId,
            @RequestParam("fichiers") List<MultipartFile> fichiers,
            Authentication auth) {
        try {
            Mission mission = missionRepository.findByIdWithDetails(missionId)
                    .orElseThrow(() -> new RuntimeException("Mission introuvable"));
            if (mission.getPrestataire() == null ||
                !mission.getPrestataire().getUsername().equals(auth.getName())) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }
            _uploadDocuments(missionId, fichiers, auth.getName());
            return ResponseEntity.ok(Map.of("message", "Documents ajoutés avec succès"));
        } catch (Exception e) {
            log.error("Erreur upload documents mission {} : {}", missionId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET DOCUMENTS
    // ─────────────────────────────────────────────────────────
    @GetMapping("/missions/{missionId}/documents")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
    @Transactional
    public ResponseEntity<?> getDocuments(@PathVariable Long missionId, Authentication auth) {
        try {
            Mission mission = missionRepository.findByIdWithDetails(missionId)
                    .orElseThrow(() -> new RuntimeException("Mission introuvable"));
            if (mission.getPrestataire() == null ||
                !mission.getPrestataire().getUsername().equals(auth.getName())) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }
            List<Map<String, Object>> fichiersInfo = new ArrayList<>();
            resultatMissionRepository.findByMissionIdWithFichiers(missionId)
                    .ifPresent(resultat -> {
                        for (FichierResultat f : resultat.getFichiers()) {
                            fichiersInfo.add(Map.of(
                                "id",                f.getId(),
                                "nomFichierOriginal", f.getNomFichierOriginal(),
                                "nomFichierServeur",  f.getNomFichierServeur(),
                                "typeMime",           f.getTypeMime(),
                                "tailleFichier",      f.getTailleFichier(),
                                "dateUpload",         f.getDateUpload()
                            ));
                        }
                    });
            return ResponseEntity.ok(Map.of("fichiers", fichiersInfo));
        } catch (Exception e) {
            log.error("Erreur get documents mission {} : {}", missionId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    // TÉLÉCHARGEMENT FICHIERS
    // ─────────────────────────────────────────────────────────
    @GetMapping("/missions/fichier/id/{id}")
    public ResponseEntity<byte[]> telechargerFichierById(@PathVariable Long id) throws IOException {
        FichierResultat fichier = fichierResultatService.findById(id);
        if (fichier == null) return ResponseEntity.notFound().build();
        byte[] contenu = Files.readAllBytes(Paths.get(uploadDir, fichier.getNomFichierServeur()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fichier.getTypeMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fichier.getNomFichierOriginal() + "\"")
                .body(contenu);
    }

    @GetMapping("/missions/fichier/{nomFichierServeur}")
    public ResponseEntity<byte[]> telechargerFichier(@PathVariable String nomFichierServeur) throws IOException {
        FichierResultat fichier = fichierResultatService.findByNomFichierServeur(nomFichierServeur);
        if (fichier == null) return ResponseEntity.notFound().build();
        byte[] contenu = Files.readAllBytes(Paths.get(uploadDir, nomFichierServeur));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fichier.getTypeMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fichier.getNomFichierOriginal() + "\"")
                .body(contenu);
    }

    // ─────────────────────────────────────────────────────────
    // PROFIL
    // ─────────────────────────────────────────────────────────
    @GetMapping("/mon-profil")
    public ResponseEntity<?> getMonProfil(Principal principal) {
        try {
            Prestataire p = prestataireService.findByUsername(principal.getName());
            return ResponseEntity.ok(Map.of(
                "username", p.getUsername(),
                "nom",      p.getNom(),
                "prenom",   p.getPrenom(),
                "actif",    p.isActif()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        }
    }
}