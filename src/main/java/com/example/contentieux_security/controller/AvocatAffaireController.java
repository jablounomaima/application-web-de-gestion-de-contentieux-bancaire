package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.AffaireJudiciaire;
import com.example.contentieux_security.entity.Audience;
import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.entity.Prestataire;
import com.example.contentieux_security.dto.DossierDetailDTO;
import com.example.contentieux_security.service.AffaireJudiciaireService;
import com.example.contentieux_security.service.DossierService;
import com.example.contentieux_security.service.HistoriqueService;
import com.example.contentieux_security.service.MissionService;
import com.example.contentieux_security.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/avocat/affaires")
@PreAuthorize("hasAnyRole('AVOCAT')")
@RequiredArgsConstructor
@Slf4j
public class AvocatAffaireController {

    private final AffaireJudiciaireService affaireService;
    private final DossierService dossierService;
    private final HistoriqueService historiqueService;
    private final AffaireJudiciaireService affaireJudiciaireService;
    private final MissionService missionService;
    private final NotificationService notificationService; // ✅ NOTIF — injection

    // ─── Helpers communs ──────────────────────────────────────────────────────

    /** Nom complet de l'avocat connecté (prenom + nom ou username si absent). */
    private String nomAvocat(AffaireJudiciaire affaire, String username) {
        Prestataire av = affaire.getAvocat();
        if (av == null) return username;
        String prenom = av.getPrenom() != null ? av.getPrenom() : "";
        String nom    = av.getNom()    != null ? av.getNom()    : "";
        String full   = (prenom + " " + nom).trim();
        return full.isEmpty() ? username : full;
    }

    /** Username de l'agent responsable du dossier (null si introuvable). */
  /** Username de l'agent responsable du dossier — lit le champ String creePar. */
private String agentDuDossier(AffaireJudiciaire affaire) {
    try {
        DossierContentieux d = affaire.getDossier();
        if (d == null) return null;
        // ✅ creePar est un String direct, pas de lazy-loading
        return d.getCreePar();
    } catch (Exception e) {
        log.warn("Impossible de résoudre l'agent du dossier : {}", e.getMessage());
        return null;
    }
}

    /** Numéro du dossier ou "inconnu". */
    private String numeroDossier(AffaireJudiciaire affaire) {
        try {
            return affaire.getDossier() != null
                   ? affaire.getDossier().getNumeroDossier() : "inconnu";
        } catch (Exception e) { return "inconnu"; }
    }

    /** Id du dossier (pour construire l'URL de notification). */
    private Long dossierIdOf(AffaireJudiciaire affaire) {
        try {
            return affaire.getDossier() != null ? affaire.getDossier().getId() : null;
        } catch (Exception e) { return null; }
    }

    // ─── AFFAIRES ─────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> mesAffaires(Principal principal) {
        String username = principal.getName();
        List<AffaireJudiciaire> affaires = affaireService.getAffairesParAvocat(username);
        Map<Long, String> missionStatuts = new HashMap<>();
        Map<Long, Long>   missionIds     = new HashMap<>();
        Map<Long, Long>   dossierIds     = new HashMap<>();  // ← AJOUTER
    
        for (AffaireJudiciaire affaire : affaires) {
            if (affaire.getMission() != null) {
                try {
                    missionStatuts.put(affaire.getId(),
                        affaire.getMission().getStatut() != null
                            ? affaire.getMission().getStatut().name() : "INCONNU");
                    missionIds.put(affaire.getId(), affaire.getMission().getId());
                } catch (Exception e) {
                    log.warn("Mission lazy affaire {} : {}", affaire.getId(), e.getMessage());
                }
            }
            // ← AJOUTER : inclure dossierId
            Long dossierId = dossierIdOf(affaire);
            if (dossierId != null) {
                dossierIds.put(affaire.getId(), dossierId);
            }
        }
    
        Map<String, Object> response = new HashMap<>();
        response.put("affaires",       affaires);
        response.put("missionStatuts", missionStatuts);
        response.put("missionIds",     missionIds);
        response.put("dossierIds",     dossierIds);  // ← AJOUTER
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/{affaireId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detailAffaire(@PathVariable Long affaireId, Principal principal) {
        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
        if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));

        Map<String, Object> response = new HashMap<>();
        response.put("id",                 affaire.getId());
        response.put("numeroAffaire",      affaire.getNumeroAffaire());
        response.put("statut",             affaire.getStatut());
        response.put("dateLancement",      affaire.getDateLancement());
        response.put("tribunal",           affaire.getTribunal());
        response.put("chambre",            affaire.getChambre());
        response.put("numeroRole",         affaire.getNumeroRole());
        response.put("typeJugement",       affaire.getTypeJugement()  != null ? affaire.getTypeJugement().name()  : null);
        response.put("dateJugement",       affaire.getDateJugement());
        response.put("montantJuge",        affaire.getMontantJuge());
        response.put("delaiPaiementJuge",  affaire.getDelaiPaiementJuge());
        response.put("descriptionJugement",affaire.getDescriptionJugement());
        response.put("dateLimiteAppel",    affaire.getDateLimiteAppel());
        response.put("pvTexte",            affaire.getPvTexte());
        response.put("pvStatut",           affaire.getPvStatut()     != null ? affaire.getPvStatut().name()     : null);
        response.put("factureRef",         affaire.getFactureRef());
        response.put("montantFacture",     affaire.getMontantFacture());
        response.put("factureStatut",      affaire.getFactureStatut() != null ? affaire.getFactureStatut().name() : null);

        List<Map<String, Object>> fichiers = new ArrayList<>();
        if (affaire.getPvFichiers() != null) {
            for (String data : affaire.getPvFichiers()) {
                String[] parts = data.split("\\|", 3);
                if (parts.length == 3)
                    fichiers.add(Map.of("nom", parts[0], "typeMime", parts[1], "base64", parts[2]));
            }
        }
        response.put("pvFichiers", fichiers);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Principal principal) {
        String username = principal.getName();
        List<AffaireJudiciaire> affaires = affaireService.getAffairesParAvocat(username);

        long totalAffaires   = affaires.size();
        long affairesEnCours = affaires.stream()
            .filter(a -> a.getStatut() == AffaireJudiciaire.StatutAffaire.EN_COURS).count();
        long jugementRendu   = affaires.stream()
            .filter(a -> a.getStatut() == AffaireJudiciaire.StatutAffaire.JUGEMENT_RENDU).count();
        long audiencesAVenir = affaireService.getAudiencesAVenir(username).size();

        double totalHonoraires = affaires.stream()
            .filter(a -> a.getMontantFacture() != null && a.getFactureStatut() != null
                      && a.getFactureStatut() != AffaireJudiciaire.StatutFacture.REJETEE)
            .mapToDouble(AffaireJudiciaire::getMontantFacture).sum();

        long nombreFactures    = affaires.stream()
            .filter(a -> a.getFactureRef() != null && !a.getFactureRef().isBlank()).count();
        long facturesPayees    = affaires.stream()
            .filter(a -> a.getFactureStatut() == AffaireJudiciaire.StatutFacture.PAYEE).count();
        long pvEnAttente       = affaires.stream()
            .filter(a -> a.getPvStatut() == AffaireJudiciaire.StatutPV.EN_ATTENTE).count();
        long facturesEnAttente = affaires.stream()
            .filter(a -> a.getFactureStatut() == AffaireJudiciaire.StatutFacture.EN_ATTENTE).count();

        Map<String, Object> response = new HashMap<>();
        response.put("totalAffaires",     totalAffaires);
        response.put("affairesEnCours",   affairesEnCours);
        response.put("jugementRendu",     jugementRendu);
        response.put("audiencesAVenir",   audiencesAVenir);
        response.put("totalHonoraires",   totalHonoraires);
        response.put("nombreFactures",    nombreFactures);
        response.put("facturesPayees",    facturesPayees);
        response.put("pvEnAttente",       pvEnAttente);
        response.put("facturesEnAttente", facturesEnAttente);
        return ResponseEntity.ok(response);
    }

    // ─── DOSSIER ──────────────────────────────────────────────────────────────

    @GetMapping("/{affaireId}/dossier")
    public ResponseEntity<?> voirDossier(@PathVariable Long affaireId, Principal principal) {
        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
        if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        if (affaire.getDossier() == null)
            return ResponseEntity.status(404).body(Map.of("error", "Dossier introuvable"));

        Long dossierId = affaire.getDossier().getId();
        DossierDetailDTO dossier = dossierService.getDossierDetail(dossierId);
        AffaireJudiciaire affaireAvecMission = affaireJudiciaireService.findById(affaireId);
        Mission mission = affaireAvecMission.getMission();
        return ResponseEntity.ok(Map.of("affaire", affaire, "dossier", dossier, "mission", mission));
    }

    // ─── AUDIENCES ────────────────────────────────────────────────────────────

    @GetMapping("/{affaireId}/audiences")
    public ResponseEntity<?> gererAudiences(@PathVariable Long affaireId, Principal principal) {
        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
        if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        return ResponseEntity.ok(Map.of(
            "audiences",       affaire.getAudiences(),
            "statutsAudience", Audience.StatutAudience.values()
        ));
    }

    @PostMapping("/{affaireId}/audiences")
    public ResponseEntity<?> ajouterAudience(@PathVariable Long affaireId,
            @RequestBody Map<String, Object> body, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));

            LocalDate dateAudience      = LocalDate.parse(body.get("dateAudience").toString());
            String    heure             = (String) body.get("heure");
            String    salle             = (String) body.get("salle");
            String    motif             = (String) body.get("motif");
            String    resultat          = (String) body.get("resultat");
            LocalDate prochaineAudience = body.get("prochaineAudience") != null
                                          ? LocalDate.parse(body.get("prochaineAudience").toString()) : null;
            Audience.StatutAudience statut =
                Audience.StatutAudience.valueOf((String) body.get("statut"));

            affaireService.ajouterAudience(affaireId, dateAudience, heure, salle,
                                           motif, resultat, prochaineAudience, statut);

            // ✅ NOTIF — audience ajoutée
            String agentUsername = agentDuDossier(affaire);
            if (agentUsername != null) {
                String avocatNom = nomAvocat(affaire, principal.getName());
                String numDoss   = numeroDossier(affaire);
                Long   dossId    = dossierIdOf(affaire);
                notificationService.notifierSansDossier(
                    agentUsername,
                    "📅 Nouvelle audience ajoutée",
                    String.format("L'avocat %s a ajouté une audience le %s pour le dossier %s.",
                        avocatNom, dateAudience, numDoss),
                    "NOUVELLE_AUDIENCE",
                    "/agent/dossiers/" + dossId + "/affaire"
                );
            }

            return ResponseEntity.ok(Map.of("message", "Audience ajoutée avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{affaireId}/audiences/{audienceId}")
    public ResponseEntity<?> modifierAudience(@PathVariable Long affaireId,
            @PathVariable Long audienceId,
            @RequestBody Map<String, Object> body, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));

            LocalDate dateAudience      = LocalDate.parse(body.get("dateAudience").toString());
            String    heure             = (String) body.get("heure");
            String    salle             = (String) body.get("salle");
            String    motif             = (String) body.get("motif");
            String    resultat          = (String) body.get("resultat");
            LocalDate prochaineAudience = body.get("prochaineAudience") != null
                                          ? LocalDate.parse(body.get("prochaineAudience").toString()) : null;
            Audience.StatutAudience statut =
                Audience.StatutAudience.valueOf((String) body.get("statut"));

            affaireService.modifierAudienceComplete(audienceId, dateAudience, heure, salle,
                                                    motif, resultat, prochaineAudience, statut);

            // ✅ NOTIF — audience modifiée
            String agentUsername = agentDuDossier(affaire);
            if (agentUsername != null) {
                notificationService.notifierSansDossier(
                    agentUsername,
                    "✏️ Audience modifiée",
                    String.format("L'avocat %s a modifié une audience pour le dossier %s.",
                        nomAvocat(affaire, principal.getName()), numeroDossier(affaire)),
                    "AUDIENCE_MODIFIEE",
                    "/agent/dossiers/" + dossierIdOf(affaire) + "/affaire"
                );
            }

            return ResponseEntity.ok(Map.of("message", "Audience modifiée avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{affaireId}/audiences/{audienceId}/statut")
    public ResponseEntity<?> modifierStatutAudience(@PathVariable Long affaireId,
            @PathVariable Long audienceId,
            @RequestBody Map<String, Object> body, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));

            Audience.StatutAudience statut =
                Audience.StatutAudience.valueOf((String) body.get("statut"));
            String    resultat          = (String) body.get("resultat");
            LocalDate prochaineAudience = body.get("prochaineAudience") != null
                                          ? LocalDate.parse(body.get("prochaineAudience").toString()) : null;

            affaireService.modifierStatutAudience(audienceId, statut, resultat, prochaineAudience);

            // ✅ NOTIF — statut audience mis à jour
            String agentUsername = agentDuDossier(affaire);
            if (agentUsername != null) {
                notificationService.notifierSansDossier(
                    agentUsername,
                    "🔄 Statut d'audience mis à jour",
                    String.format("L'avocat %s a mis à jour le statut d'une audience (%s) pour le dossier %s.",
                        nomAvocat(affaire, principal.getName()), statut.name(), numeroDossier(affaire)),
                    "AUDIENCE_MODIFIEE",
                    "/agent/dossiers/" + dossierIdOf(affaire) + "/affaire"
                );
            }

            return ResponseEntity.ok(Map.of("message", "Audience mise à jour."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{affaireId}/audiences/{audienceId}")
    public ResponseEntity<?> supprimerAudience(@PathVariable Long affaireId,
            @PathVariable Long audienceId, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));

            affaireService.supprimerAudience(audienceId);
            return ResponseEntity.ok(Map.of("message", "Audience supprimée avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── JUGEMENT ─────────────────────────────────────────────────────────────

    @PostMapping("/{affaireId}/jugement")
    public ResponseEntity<?> saisirJugement(@PathVariable Long affaireId,
            @RequestBody Map<String, Object> body, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));

            AffaireJudiciaire.TypeJugement typeJugement =
                AffaireJudiciaire.TypeJugement.valueOf((String) body.get("typeJugement"));
            LocalDate dateJugement     = LocalDate.parse(body.get("dateJugement").toString());
            String montantJuge         = (String) body.get("montantJuge");
            String delaiPaiementJuge   = (String) body.get("delaiPaiementJuge");
            String descriptionJugement = (String) body.get("descriptionJugement");

            LocalDate dateLimiteAppel = null;
            if (body.get("dateLimiteAppel") != null
                    && !body.get("dateLimiteAppel").toString().isBlank())
                dateLimiteAppel = LocalDate.parse(body.get("dateLimiteAppel").toString());

            affaireService.enregistrerJugement(affaireId, typeJugement, dateJugement,
                                               montantJuge, delaiPaiementJuge, descriptionJugement);

            if (dateLimiteAppel != null) {
                AffaireJudiciaire maj = affaireService.getAffaireById(affaireId);
                maj.setDateLimiteAppel(dateLimiteAppel);
                affaireService.sauvegarderAffaire(maj);
            }

            // ✅ NOTIF — jugement saisi
            String agentUsername = agentDuDossier(affaire);
            if (agentUsername != null) {
                notificationService.notifierSansDossier(
                    agentUsername,
                    "⚖️ Jugement enregistré",
                    String.format("L'avocat %s a enregistré un jugement (%s) pour le dossier %s.",
                        nomAvocat(affaire, principal.getName()),
                        typeJugement.name(),
                        numeroDossier(affaire)),
                    "JUGEMENT_RENDU",
                    "/agent/dossiers/" + dossierIdOf(affaire) + "/affaire"
                );
            }

            return ResponseEntity.ok(Map.of("message", "Jugement enregistré avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{affaireId}/jugement")
    public ResponseEntity<?> supprimerJugement(@PathVariable Long affaireId, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            affaireService.supprimerJugement(affaireId);
            return ResponseEntity.ok(Map.of("message", "Jugement supprimé avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── TRIBUNAL ─────────────────────────────────────────────────────────────

    @PostMapping("/{affaireId}/tribunal")
    public ResponseEntity<?> modifierTribunal(@PathVariable Long affaireId,
            @RequestBody Map<String, String> body, Principal principal) {
        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
        if (affaire == null || !isOwner(affaire, principal.getName()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        try {
            affaireService.modifierTribunal(affaireId,
                body.get("tribunal"), body.get("chambre"), body.get("numeroRole"));

            // ✅ NOTIF — infos tribunal mises à jour
            String agentUsername = agentDuDossier(affaire);
            if (agentUsername != null) {
                notificationService.notifierSansDossier(
                    agentUsername,
                    "🏛️ Informations tribunal mises à jour",
                    String.format("L'avocat %s a mis à jour les informations du tribunal pour le dossier %s.",
                        nomAvocat(affaire, principal.getName()), numeroDossier(affaire)),
                    "RESULTAT_AVOCAT",
                    "/agent/dossiers/" + dossierIdOf(affaire) + "/affaire"
                );
            }

            return ResponseEntity.ok(Map.of("message", "Informations du tribunal mises à jour."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{affaireId}/tribunal")
    public ResponseEntity<?> supprimerTribunal(@PathVariable Long affaireId, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isOwner(affaire, principal.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            affaireService.modifierTribunal(affaireId, null, null, null);
            return ResponseEntity.ok(Map.of("message", "Informations du tribunal supprimées."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── PV ───────────────────────────────────────────────────────────────────

    @PostMapping("/{affaireId}/pv")
    public ResponseEntity<?> soumettrePV(@PathVariable Long affaireId,
            @RequestBody Map<String, String> body, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));

            affaireJudiciaireService.soumettreAvocatPV(affaireId, body.get("pvTexte"));

            // ✅ NOTIF — PV soumis
            String agentUsername = agentDuDossier(affaire);
            if (agentUsername != null) {
                notificationService.notifierSansDossier(
                    agentUsername,
                    "📋 Procès-Verbal soumis",
                    String.format("L'avocat %s a soumis un PV pour le dossier %s.",
                        nomAvocat(affaire, principal.getName()), numeroDossier(affaire)),
                    "PV_SOUMIS",
                    "/agent/dossiers/" + dossierIdOf(affaire) + "/affaire"
                );
            }

            return ResponseEntity.ok(Map.of("message", "PV soumis avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{affaireId}/pv/fichiers")
    public ResponseEntity<?> soumettrePVAvecFichiers(
            @PathVariable Long affaireId,
            @RequestPart("pvTexte") String pvTexte,
            @RequestPart(value = "piecesJointes", required = false) List<MultipartFile> fichiers,
            Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));

            log.info("Fichiers reçus : {}", fichiers != null ? fichiers.size() : 0);
            affaireJudiciaireService.soumettreAvocatPVAvecFichiers(affaireId, pvTexte, fichiers);

            // ✅ NOTIF — PV avec fichiers soumis
            String agentUsername = agentDuDossier(affaire);
            if (agentUsername != null) {
                int nbFichiers = fichiers != null ? fichiers.size() : 0;
                notificationService.notifierSansDossier(
                    agentUsername,
                    "📋 Procès-Verbal soumis avec pièces jointes",
                    String.format("L'avocat %s a soumis un PV (%d pièce(s) jointe(s)) pour le dossier %s.",
                        nomAvocat(affaire, principal.getName()), nbFichiers, numeroDossier(affaire)),
                    "PV_SOUMIS",
                    "/agent/dossiers/" + dossierIdOf(affaire) + "/affaire"
                );
            }

            return ResponseEntity.ok(Map.of("message", "PV soumis avec succès."));
        } catch (Exception e) {
            log.error("Erreur PV fichiers: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{affaireId}/pv")
    public ResponseEntity<?> modifierPV(@PathVariable Long affaireId,
            @RequestBody Map<String, String> body, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));

            affaireJudiciaireService.modifierAvocatPV(affaireId, body.get("pvTexte"));

            // ✅ NOTIF — PV modifié
            String agentUsername = agentDuDossier(affaire);
            if (agentUsername != null) {
                notificationService.notifierSansDossier(
                    agentUsername,
                    "✏️ Procès-Verbal modifié",
                    String.format("L'avocat %s a modifié le PV pour le dossier %s.",
                        nomAvocat(affaire, principal.getName()), numeroDossier(affaire)),
                    "PV_SOUMIS",
                    "/agent/dossiers/" + dossierIdOf(affaire) + "/affaire"
                );
            }

            return ResponseEntity.ok(Map.of("message", "PV modifié avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{affaireId}/pv")
    public ResponseEntity<?> supprimerPV(@PathVariable Long affaireId, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            affaireJudiciaireService.supprimerAvocatPV(affaireId);
            return ResponseEntity.ok(Map.of("message", "PV supprimé avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── FACTURE ──────────────────────────────────────────────────────────────

    @PostMapping("/{affaireId}/facture")
    public ResponseEntity<?> soumettreFacture(@PathVariable Long affaireId,
            @RequestBody Map<String, Object> body, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));

            String factureRef    = (String) body.get("factureRef");
            Double montant       = Double.valueOf(body.get("montantFacture").toString());

            affaireJudiciaireService.soumettreAvocatFacture(affaireId, factureRef, montant);

            // ✅ NOTIF — facture soumise
            String agentUsername = agentDuDossier(affaire);
            if (agentUsername != null) {
                notificationService.notifierSansDossier(
                    agentUsername,
                    "🧾 Facture d'honoraires soumise",
                    String.format("L'avocat %s a soumis une facture (réf. %s, %.3f TND) pour le dossier %s.",
                        nomAvocat(affaire, principal.getName()),
                        factureRef, montant, numeroDossier(affaire)),
                    "FACTURE_SOUMISE",
                    "/agent/dossiers/" + dossierIdOf(affaire) + "/affaire"
                );
            }

            return ResponseEntity.ok(Map.of("message", "Facture soumise avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{affaireId}/facture")
    public ResponseEntity<?> modifierFacture(@PathVariable Long affaireId,
            @RequestBody Map<String, Object> body, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));

            String factureRef = (String) body.get("factureRef");
            Double montant    = Double.valueOf(body.get("montantFacture").toString());

            affaireJudiciaireService.modifierAvocatFacture(affaireId, factureRef, montant);

            // ✅ NOTIF — facture modifiée
            String agentUsername = agentDuDossier(affaire);
            if (agentUsername != null) {
                notificationService.notifierSansDossier(
                    agentUsername,
                    "✏️ Facture d'honoraires modifiée",
                    String.format("L'avocat %s a modifié sa facture (réf. %s, %.3f TND) pour le dossier %s.",
                        nomAvocat(affaire, principal.getName()),
                        factureRef, montant, numeroDossier(affaire)),
                    "FACTURE_SOUMISE",
                    "/agent/dossiers/" + dossierIdOf(affaire) + "/affaire"
                );
            }

            return ResponseEntity.ok(Map.of("message", "Facture modifiée avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{affaireId}/facture")
    public ResponseEntity<?> supprimerFacture(@PathVariable Long affaireId, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            affaireJudiciaireService.supprimerAvocatFacture(affaireId);
            return ResponseEntity.ok(Map.of("message", "Facture supprimée avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── FICHIERS PV ──────────────────────────────────────────────────────────

    @DeleteMapping("/{affaireId}/pv/fichiers/{index}")
    public ResponseEntity<?> supprimerFichierPV(
            @PathVariable Long affaireId,
            @PathVariable int index,
            Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));

            List<String> fichiers = new ArrayList<>(affaire.getPvFichiers());
            if (index < 0 || index >= fichiers.size())
                return ResponseEntity.badRequest().body(Map.of("error", "Index invalide"));

            fichiers.remove(index);
            affaire.setPvFichiers(fichiers);
            affaireJudiciaireService.sauvegarderAffaire(affaire);
            return ResponseEntity.ok(Map.of("message", "Fichier supprimé."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private boolean isAvocatOwner(AffaireJudiciaire affaire, String username) {
        return affaire.getAvocat() != null
               && username.equals(affaire.getAvocat().getUsername());
    }

    private boolean isOwner(AffaireJudiciaire affaire, String username) {
        return isAvocatOwner(affaire, username);
    }
}