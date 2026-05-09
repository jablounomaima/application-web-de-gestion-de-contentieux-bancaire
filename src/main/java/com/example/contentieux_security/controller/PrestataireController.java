package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.repository.MissionRepository;
import com.example.contentieux_security.service.*;
import java.security.Principal;
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

        long enCours       = missions.stream().filter(m -> m.getStatut() == StatutMission.ASSIGNEE   || m.getStatut() == StatutMission.EN_COURS).count();
        long pvSoumis      = missions.stream().filter(m -> m.getStatut() == StatutMission.PV_SOUMIS).count();
        long factureSoumise= missions.stream().filter(m -> m.getStatut() == StatutMission.FACTURE_SOUMISE).count();
        long terminees     = missions.stream().filter(m -> m.getStatut() == StatutMission.TERMINEE   || m.getStatut() == StatutMission.REALISEE).count();

        List<Map<String, Object>> missionsMap = new ArrayList<>();
        for (Mission m : missions) {
            Map<String, Object> mm = new HashMap<>();
            mm.put("id",              m.getId());
            mm.put("numeroMission",   m.getNumeroMission());
            mm.put("statut",          m.getStatut());
            mm.put("dateAssignation", m.getDateAssignation());
            mm.put("description",     m.getDescription());

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
    // FORMULAIRES PV / FACTURE
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
    // SOUMETTRE PV
    // ─────────────────────────────────────────────────────────
    @PostMapping("/missions/{id}/pv")
    public ResponseEntity<?> soumettrePV(@PathVariable Long id,
                                         @RequestBody Map<String, String> body,
                                         Authentication authentication) {
        try {
            String pvTexte = body.get("pvTexte");
            Mission mission = missionRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new RuntimeException("Mission introuvable"));

            StatutMission statut = mission.getStatut();
            if (statut != StatutMission.ASSIGNEE &&
                statut != StatutMission.EN_COURS  &&
                statut != StatutMission.REJETEE) {
                return ResponseEntity.badRequest()
                       .body(Map.of("error", "PV déjà soumis ou mission non active."));
            }

            mission.setPvMission(pvTexte);
            mission.setStatut(StatutMission.PV_SOUMIS);
            mission.setDateValidationPv(java.time.LocalDateTime.now());
            missionRepository.save(mission);

            return ResponseEntity.ok(Map.of("message", "PV soumis avec succès."));
        } catch (Exception e) {
            log.error("Erreur soumission PV mission {} : {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Erreur : " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    // SOUMETTRE FACTURE
    // ─────────────────────────────────────────────────────────
    @PostMapping("/missions/{id}/facture")
    public ResponseEntity<?> soumettreFacture(@PathVariable Long id,
                                              @RequestBody Map<String, Object> body,
                                              Authentication authentication) {
        try {
            String factureRef = (String) body.get("factureRef");
            Double montant    = Double.valueOf(body.get("montant").toString());
            Mission mission   = missionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Mission introuvable"));

            if (mission.getStatut() != StatutMission.PV_SOUMIS &&
                mission.getStatut() != StatutMission.REJETEE) {
                return ResponseEntity.badRequest()
                       .body(Map.of("error", "Soumettez d'abord le PV."));
            }

            mission.setFactureRef(factureRef);
            mission.setMontantFacture(montant);
            mission.setStatut(StatutMission.FACTURE_SOUMISE);
            mission.setDateValidationFacture(java.time.LocalDateTime.now());
            missionRepository.save(mission);

            return ResponseEntity.ok(Map.of("message", "Facture soumise avec succès."));
        } catch (Exception e) {
            log.error("Erreur soumission facture mission {} : {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Erreur : " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    // TÉLÉCHARGEMENT FICHIERS
    // ─────────────────────────────────────────────────────────
    @GetMapping("/missions/fichier/id/{id}")
    public ResponseEntity<byte[]> telechargerFichierById(@PathVariable Long id) throws IOException {
        FichierResultat fichier = fichierResultatService.findById(id);
        if (fichier == null) return ResponseEntity.notFound().build();
        Path chemin = Paths.get(uploadDir, fichier.getNomFichierServeur());
        byte[] contenu = Files.readAllBytes(chemin);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fichier.getTypeMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fichier.getNomFichierOriginal() + "\"")
                .body(contenu);
    }

    @GetMapping("/missions/fichier/{nomFichierServeur}")
    public ResponseEntity<byte[]> telechargerFichier(
            @PathVariable String nomFichierServeur) throws IOException {
        FichierResultat fichier = fichierResultatService.findByNomFichierServeur(nomFichierServeur);
        if (fichier == null) return ResponseEntity.notFound().build();
        Path chemin = Paths.get(uploadDir, nomFichierServeur);
        byte[] contenu = Files.readAllBytes(chemin);
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
                        Map<String, Object> gm = new HashMap<>();
                        gm.put("typeGarantie",  g.getTypeGarantie());
                        gm.put("valeurEstimee", g.getValeurEstimee());
                        gm.put("description",   g.getDescription());
                        gm.put("statut",        g.getStatut());
                        garantiesMap.add(gm);
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
    // UPLOAD DOCUMENTS
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

            ResultatMission resultat = resultatMissionRepository
                    .findByMission_Id(missionId)
                    .orElseGet(() -> {
                        ResultatMission r = new ResultatMission();
                        r.setMission(mission);
                        r.setDateCreation(java.time.LocalDateTime.now());
                        r.setDateSoumission(java.time.LocalDateTime.now());
                        r.setSoumisePar(auth.getName());
                        r.setCommentaire("");
                        return resultatMissionRepository.save(r);
                    });

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            List<Map<String, Object>> fichiersInfo = new ArrayList<>();
            for (MultipartFile f : fichiers) {
                String nomServeur = UUID.randomUUID() + "_" + f.getOriginalFilename();
                Files.copy(f.getInputStream(), uploadPath.resolve(nomServeur));

                FichierResultat fichier = FichierResultat.builder()
                        .nomFichierOriginal(f.getOriginalFilename())
                        .nomFichierServeur(nomServeur)
                        .typeMime(f.getContentType())
                        .tailleFichier(f.getSize())
                        .dateUpload(java.time.LocalDateTime.now())
                        .resultat(resultat)
                        .build();
                fichierResultatService.save(fichier);

                Map<String, Object> info = new HashMap<>();
                info.put("id",                fichier.getId());
                info.put("nomFichierOriginal", fichier.getNomFichierOriginal());
                info.put("nomFichierServeur",  fichier.getNomFichierServeur());
                info.put("typeMime",           fichier.getTypeMime());
                info.put("tailleFichier",      fichier.getTailleFichier());
                info.put("dateUpload",         fichier.getDateUpload());
                fichiersInfo.add(info);
            }

            return ResponseEntity.ok(Map.of(
                    "message",  "Documents ajoutés avec succès",
                    "fichiers", fichiersInfo
            ));
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
    public ResponseEntity<?> getDocuments(@PathVariable Long missionId,
                                          Authentication auth) {
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
                            Map<String, Object> info = new HashMap<>();
                            info.put("id",                f.getId());
                            info.put("nomFichierOriginal", f.getNomFichierOriginal());
                            info.put("nomFichierServeur",  f.getNomFichierServeur());
                            info.put("typeMime",           f.getTypeMime());
                            info.put("tailleFichier",      f.getTailleFichier());
                            info.put("dateUpload",         f.getDateUpload());
                            fichiersInfo.add(info);
                        }
                    });

            return ResponseEntity.ok(Map.of("fichiers", fichiersInfo));
        } catch (Exception e) {
            log.error("Erreur get documents mission {} : {}", missionId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET RÉSULTAT MISSION  ✅ CORRIGÉ
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

            // PV + Facture directement sur Mission
            Map<String, Object> response = new HashMap<>();
            response.put("pvTexte",               mission.getPvMission());
            response.put("dateSoumissionPV",       mission.getDateValidationPv());
            response.put("factureRef",             mission.getFactureRef());
            response.put("montant",                mission.getMontantFacture());
            response.put("dateSoumissionFacture",  mission.getDateValidationFacture());

            // Commentaire depuis ResultatMission
            Optional<ResultatMission> opt = resultatMissionRepository
                    .findByMission_Id(missionId);
            response.put("commentaire", opt.map(ResultatMission::getCommentaire).orElse(null));
            response.put("soumisePar",  opt.map(ResultatMission::getSoumisePar).orElse(null));

            // Fichiers
            List<Map<String, Object>> fichiers = new ArrayList<>();
            opt.ifPresent(r -> {
                if (r.getFichiers() != null) {
                    for (FichierResultat f : r.getFichiers()) {
                        Map<String, Object> fm = new HashMap<>();
                        fm.put("id",                f.getId());
                        fm.put("nomFichierOriginal", f.getNomFichierOriginal());
                        fm.put("nomFichierServeur",  f.getNomFichierServeur());
                        fm.put("typeMime",           f.getTypeMime());
                        fm.put("tailleFichier",      f.getTailleFichier());
                        fm.put("dateUpload",         f.getDateUpload());
                        fichiers.add(fm);
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
    // GET MISSION DÉTAIL  ✅ CORRIGÉ (double @GetMapping supprimé)
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
            response.put("id",              mission.getId());
            response.put("numeroMission",   mission.getNumeroMission());
            response.put("statut",          mission.getStatut() != null
                                            ? mission.getStatut().name() : null);
            response.put("dateAssignation", mission.getDateAssignation());

            if (mission.getPrestation() != null) {
                Map<String, Object> prestation = new HashMap<>();
                prestation.put("type", mission.getPrestation().getType());

                if (mission.getPrestation().getDossier() != null) {
                    var d = mission.getPrestation().getDossier();
                    Map<String, Object> dossier = new HashMap<>();
                    dossier.put("numeroDossier", d.getNumeroDossier());
                    dossier.put("libelle",       d.getLibelle());
                    dossier.put("dateCreation",  d.getDateCreation());
                    dossier.put("montant",       d.calculerSolde());

                    if (d.getClient() != null) {
                        Map<String, Object> client = new HashMap<>();
                        client.put("nom",    d.getClient().getNom());
                        client.put("prenom", d.getClient().getPrenom());
                        dossier.put("client", client);
                    }
                    prestation.put("dossier", dossier);
                }
                response.put("prestation", prestation);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur getMissionDetail {} : {}", missionId, e.getMessage(), e);
            return ResponseEntity.status(500)
                   .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Erreur inconnue",
                                "cause", e.getClass().getSimpleName()));
        }
    }
}