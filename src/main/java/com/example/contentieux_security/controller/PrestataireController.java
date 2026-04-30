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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prestataire")
@RequiredArgsConstructor
@Slf4j
public class PrestataireController {

    private final PrestationService      prestationService;
    private final FichierResultatService fichierResultatService;
    private final DossierService         dossierService;
    private final MissionService         missionService;
    private final MissionRepository missionRepository;
    private final PrestataireService prestataireService;

    @Value("${app.upload.dir:uploads/resultats}")
    private String uploadDir;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('AVOCAT','EXPERT','PRESTATAIRE','HUISSIER')")
    public ResponseEntity<?> dashboard(Authentication auth) {
        List<Mission> missions = prestationService.getMissionsPrestataire(auth.getName());

        long enCours = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.ASSIGNEE || m.getStatut() == StatutMission.EN_COURS).count();
        long pvSoumis = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.PV_SOUMIS).count();
        long factureSoumise = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.FACTURE_SOUMISE).count();
        long terminees = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.TERMINEE || m.getStatut() == StatutMission.REALISEE).count();

        Map<String, Object> response = new HashMap<>();
        response.put("dernieresMissions", missions);
        response.put("totalMissions", missions.size());
        response.put("missionsEnCours", enCours);
        response.put("pvSoumis", pvSoumis);
        response.put("factureSoumise", factureSoumise);
        response.put("missionsTerminees", terminees);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/missions/{missionId}/pv")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
    public ResponseEntity<?> formulairePV(@PathVariable Long missionId, Principal principal) {
        Mission mission = missionService.getMissionWithDetails(missionId);

        if (mission.getPrestataire() == null || !mission.getPrestataire().getUsername().equals(principal.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        }

        return ResponseEntity.ok(Map.of("mission", mission));
    }

    @GetMapping("/missions/{missionId}/facture")
    @PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
    public ResponseEntity<?> formulaireFacture(@PathVariable Long missionId, Principal principal) {
        Mission mission = missionService.getMissionWithDetails(missionId);

        if (mission.getPrestataire() == null || !mission.getPrestataire().getUsername().equals(principal.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        }

        return ResponseEntity.ok(Map.of("mission", mission));
    }

    @PostMapping("/missions/{id}/pv")
    public ResponseEntity<?> soumettrePV(@PathVariable Long id,
                                         @RequestBody Map<String, String> body,
                                         Authentication authentication) {
        try {
            String pvTexte = body.get("pvTexte");
            Mission mission = missionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Mission introuvable"));

            StatutMission statut = mission.getStatut();
            if (statut != StatutMission.ASSIGNEE && statut != StatutMission.EN_COURS && statut != StatutMission.REJETEE) {
                return ResponseEntity.badRequest().body(Map.of("error", "PV déjà soumis ou mission non active."));
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

    @PostMapping("/missions/{id}/facture")
    public ResponseEntity<?> soumettreFacture(@PathVariable Long id,
                                              @RequestBody Map<String, Object> body,
                                              Authentication authentication) {
        try {
            String factureRef = (String) body.get("factureRef");
            Double montant = Double.valueOf(body.get("montant").toString());
            Mission mission = missionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Mission introuvable"));

            if (mission.getStatut() != StatutMission.PV_SOUMIS && mission.getStatut() != StatutMission.REJETEE) {
                return ResponseEntity.badRequest().body(Map.of("error", "Soumettez d'abord le PV."));
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

    @GetMapping("/missions/fichier/id/{id}")
    public ResponseEntity<byte[]> telechargerFichierById(@PathVariable Long id) throws IOException {
        FichierResultat fichier = fichierResultatService.findById(id);
        if (fichier == null) return ResponseEntity.notFound().build();

        Path chemin = Paths.get(uploadDir, fichier.getNomFichierServeur());
        byte[] contenu = Files.readAllBytes(chemin);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fichier.getTypeMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fichier.getNomFichierOriginal() + "\"")
                .body(contenu);
    }

    @GetMapping("/missions/fichier/{nomFichierServeur}")
    public ResponseEntity<byte[]> telechargerFichier(@PathVariable String nomFichierServeur) throws IOException {
        FichierResultat fichier = fichierResultatService.findByNomFichierServeur(nomFichierServeur);
        if (fichier == null) return ResponseEntity.notFound().build();

        Path chemin = Paths.get(uploadDir, nomFichierServeur);
        byte[] contenu = Files.readAllBytes(chemin);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fichier.getTypeMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fichier.getNomFichierOriginal() + "\"")
                .body(contenu);
    }



    

// Endpoint GET pour l'URL "/mon-profil"
@GetMapping("/mon-profil")
public ResponseEntity<?> getMonProfil(Principal principal) {
    // Principal contient les informations de l'utilisateur authentifié
    
    try {
        // Récupérer le prestataire connecté via son nom d'utilisateur
        // principal.getName() retourne le username de l'utilisateur actuel
        Prestataire p = prestataireService.findByUsername(principal.getName());
        
        // Retourner une réponse HTTP 200 avec les données du profil
        // Map.of() crée un objet contenant les champs à envoyer au client
        return ResponseEntity.ok(Map.of(
            "username", p.getUsername(),   // Nom d'utilisateur
            "nom",      p.getNom(),        // Nom de famille
            "prenom",   p.getPrenom(),     // Prénom
            "actif",    p.isActif()        // Statut actif/inactif
        ));
        
    } catch (Exception e) {
        // En cas d'erreur (prestataire non trouvé, problème technique)
        // Retourner une erreur HTTP 403 (Accès refusé)
        return ResponseEntity.status(403)
                .body(Map.of("error", "Accès refusé"));
    }
}


// ─────────────────────────────────────────────────────────────
// 📄 Détail dossier pour une mission (PRESTATAIRE)
// ─────────────────────────────────────────────────────────────
@Transactional
@GetMapping("/missions/{missionId}/dossier")
@PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
public ResponseEntity<?> getDossierMission(@PathVariable Long missionId,
                                           Authentication auth) {

    Mission mission = missionService.getMissionWithDetails(missionId);

    if (mission.getPrestataire() == null ||
        !mission.getPrestataire().getUsername().equals(auth.getName())) {
        return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
    }

    DossierContentieux dossier = mission.getPrestation().getDossier();

    // ── Mission (données simples) ──────────────────────────────
    Map<String, Object> missionMap = new HashMap<>();
    missionMap.put("id",             mission.getId());
    missionMap.put("numeroMission",  mission.getNumeroMission());
    missionMap.put("statut",         mission.getStatut());
    missionMap.put("description",    mission.getDescription());
    missionMap.put("dateFinPrevue",  mission.getDateFinPrevue());

    // ── Client ─────────────────────────────────────────────────
    Map<String, Object> clientMap = new HashMap<>();
    if (dossier.getClient() != null) {
        var c = dossier.getClient();
        clientMap.put("nom",       c.getNom());
        clientMap.put("prenom",    c.getPrenom());
        clientMap.put("email",     c.getEmail());
        clientMap.put("telephone", c.getTelephone());
        clientMap.put("adresse",   c.getAdresse());
    }

    // ── Risques ────────────────────────────────────────────────
    List<Map<String, Object>> risquesMap = new java.util.ArrayList<>();
    if (dossier.getRisques() != null) {
        for (var r : dossier.getRisques()) {
            // ── Garanties du risque ──
            List<Map<String, Object>> garantiesMap = new java.util.ArrayList<>();
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
            rm.put("garanties",     garantiesMap);  // ← ajouter
            risquesMap.add(rm);
        }
    }

    // ── Dossier ────────────────────────────────────────────────
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

    return ResponseEntity.ok(Map.of(
            "mission", missionMap,
            "dossier", dossierMap
    ));
}
}