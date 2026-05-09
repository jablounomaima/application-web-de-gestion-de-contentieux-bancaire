package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.DossierDetailDTO;
import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.TypePrestation;
import com.example.contentieux_security.service.AffaireJudiciaireService;
import com.example.contentieux_security.service.DossierService;
import com.example.contentieux_security.service.MissionService;
import com.example.contentieux_security.service.PrestationService;
import com.example.contentieux_security.repository.AgentBancaireRepository;
import com.example.contentieux_security.repository.PrestataireRepository;
import com.example.contentieux_security.enums.TypePrestataire;
// ✅ Ajouter cet import en haut du fichier
import com.example.contentieux_security.enums.StatutMission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/dossiers")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AgentAffaireController {

    private final AffaireJudiciaireService affaireService;
    private final MissionService           missionService;
    private final PrestationService        prestationService;
    private final PrestataireRepository    prestataireRepository;
    private final DossierService           dossierService;
    private final AgentBancaireRepository  agentBancaireRepository;

    // ═══════════════════════════════════════════════════════════════
    // LANCER LA PROCÉDURE JUDICIAIRE
    // POST /api/agent/dossiers/{dossierId}/prestations/lancer
    //
    // Délègue à prestationService.lancerPrestation() qui :
    //   ✅ vérifie DossierStatus.VALIDE
    //   ✅ crée la Prestation (statut EN_COURS, type PROCEDURE_JUDICIAIRE)
    //   ✅ passe le dossier en DossierStatus.EN_PROCEDURE
    //   ✅ enregistre l'historique via historiqueService
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/{dossierId}/prestations/lancer")
    public ResponseEntity<?> lancerProcedureJudiciaire(
            @PathVariable Long dossierId,
            @RequestBody Map<String, Object> body,
            Principal principal) {

        try {
            log.info(">>> LANCER PROCÉDURE - dossierId={} agent={}", dossierId, principal.getName());

            // ── Extraction des champs du formulaire Angular ────────
            String typeProcedure        = (String) body.get("typeProcedure");
            String tribunal             = (String) body.get("tribunal");
            String chambre              = (String) body.get("chambre");
            String numeroRole           = (String) body.get("numeroRole");
            String datePremierAudienceS = (String) body.get("datePremierAudience");
            String motif                = (String) body.get("motif");
            String observations         = (String) body.get("observations");

            // ── Validation des champs obligatoires ─────────────────
            if (typeProcedure == null || typeProcedure.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Le type de procédure est obligatoire."));
            }
            if (tribunal == null || tribunal.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Le tribunal est obligatoire."));
            }
            if (datePremierAudienceS == null || datePremierAudienceS.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "La date de première audience est obligatoire."));
            }
            if (motif == null || motif.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Le motif de la procédure est obligatoire."));
            }

            // ── Construction de la description ─────────────────────
            // lancerPrestation() stocke tout dans le champ "description"
            // de la Prestation. On y regroupe tous les détails du formulaire.
            StringBuilder description = new StringBuilder();
            description.append("Type procédure : ").append(typeProcedure);
            description.append(" | Tribunal : ").append(tribunal);
            if (chambre != null && !chambre.isBlank()) {
                description.append(" | Chambre : ").append(chambre);
            }
            if (numeroRole != null && !numeroRole.isBlank()) {
                description.append(" | N° Rôle : ").append(numeroRole);
            }
            description.append(" | 1ère audience : ").append(datePremierAudienceS);
            description.append(" | Motif : ").append(motif);
            if (observations != null && !observations.isBlank()) {
                description.append(" | Observations : ").append(observations);
            }

            // ── Appel unique au service existant ───────────────────
            Prestation prestation = prestationService.lancerPrestation(
                    dossierId,
                    TypePrestation.PROCEDURE_JUDICIAIRE,
                    description.toString(),
                    principal.getName()
            );

            log.info(">>> PROCÉDURE LANCÉE : prestationId={} num={}",
                    prestation.getId(), prestation.getNumeroPrestation());

            return ResponseEntity.ok(Map.of(
                    "message",          "Procédure judiciaire lancée avec succès.",
                    "prestationId",     prestation.getId(),
                    "numeroPrestation", prestation.getNumeroPrestation(),
                    "statut",           prestation.getStatut().name()
            ));

        } catch (IllegalStateException e) {
            // Cas : dossier pas VALIDE, ou procédure déjà existante
            log.warn(">>> RÈGLE MÉTIER : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (IllegalArgumentException e) {
            // Cas : dossierId ou agentUsername introuvable
            log.warn(">>> ARGUMENT INVALIDE : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error(">>> ERREUR inattendue : {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur interne. Veuillez réessayer."));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DESIGNER AVOCAT
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/{dossierId}/prestation/{prestationId}/designer-avocat")
    public ResponseEntity<?> formulaireDesignerAvocat(@PathVariable Long dossierId,
                                                      @PathVariable Long prestationId,
                                                      Principal principal) {
        AgentBancaire agent = agentBancaireRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Agent introuvable"));

        List<Prestataire> avocats = prestataireRepository.findByTypeAndAgentResponsable_Id(
                TypePrestataire.AVOCAT, agent.getId()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("dossierId",    dossierId);
        response.put("prestationId", prestationId);
        response.put("avocats",      avocats);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{dossierId}/prestation/{prestationId}/designer-avocat")
    public ResponseEntity<?> designerAvocat(@PathVariable Long dossierId,
                                            @PathVariable Long prestationId,
                                            @RequestBody Map<String, Object> body,
                                            Principal principal) {
        try {
            Long prestataireId = Long.valueOf(body.get("prestataireId").toString());
            String description = (String) body.get("description");
            String dateFinStr  = (String) body.get("dateFinPrevue");
            LocalDate dateFinPrevue = (dateFinStr != null && !dateFinStr.isBlank())
                    ? LocalDate.parse(dateFinStr) : null;

            Mission mission = prestationService.designerPrestataire(
                    prestationId, prestataireId, description, dateFinPrevue, principal.getName()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Avocat désigné avec succès",
                    "mission", mission
            ));

        } catch (Exception e) {
            log.error(">>> ERREUR désignation avocat : {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // AFFAIRE JUDICIAIRE
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/{dossierId}/affaire/lancer")
    public ResponseEntity<?> formulaireLancerAffaire(@PathVariable Long dossierId) {
        Mission missionAvocat = missionService.getMissionAvocatDuDossier(dossierId);

        if (missionAvocat == null) {
            Prestation prestation = prestationService.getPrestationJudiciaireParDossier(dossierId);
            if (prestation != null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error",        "Désignez d'abord un avocat.",
                        "prestationId", prestation.getId()
                ));
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Aucune procédure judiciaire trouvée."));
        }

        DossierDetailDTO dossier = dossierService.getDossierDetail(dossierId);

        Map<String, Object> response = new HashMap<>();
        response.put("dossierId", dossierId);
        response.put("missionId", missionAvocat.getId());
        response.put("avocat",    missionAvocat.getPrestataire());
        response.put("dossier",   dossier);
        response.put("mission",   missionAvocat);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{dossierId}/affaire/lancer")
    public ResponseEntity<?> lancerAffaire(@PathVariable Long dossierId,
                                           @RequestBody Map<String, Object> body,
                                           Principal principal) {
        try {
            Long missionId = Long.valueOf(body.get("missionId").toString());
            log.info(">>> LANCER AFFAIRE - dossierId={} missionId={} agent={}",
                    dossierId, missionId, principal.getName());

            AffaireJudiciaire affaire = affaireService.creerAffaire(missionId, principal.getName());
            log.info(">>> AFFAIRE CRÉÉE : id={} num={}", affaire.getId(), affaire.getNumeroAffaire());

            return ResponseEntity.ok(Map.of(
                    "message", "Affaire lancée avec succès",
                    "affaire", affaire
            ));

        } catch (Exception e) {
            log.error(">>> ERREUR affaire : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{dossierId}/affaire")
    @Transactional(readOnly = true)  // ✅ AJOUTÉ

    public ResponseEntity<?> voirAffaire(@PathVariable Long dossierId) {
        AffaireJudiciaire affaire = affaireService.getAffaireParDossier(dossierId);
    
        Map<String, Object> response = new HashMap<>();
        response.put("dossierId", dossierId);
    
        if (affaire == null) {
            response.put("pasDAffaire", true);
        } else {
            affaire = affaireService.getAffaireById(affaire.getId());
    
            Map<String, Object> affaireData = new HashMap<>();
            affaireData.put("id",                  affaire.getId());
            affaireData.put("numeroAffaire",        affaire.getNumeroAffaire());
            affaireData.put("statut",               affaire.getStatut());
            affaireData.put("dateLancement",        affaire.getDateLancement());
            affaireData.put("dateProchainAudience", affaire.getDateProchainAudience());
            affaireData.put("tribunal",             affaire.getTribunal());
            affaireData.put("chambre",              affaire.getChambre());
            affaireData.put("numeroRole",           affaire.getNumeroRole());
            affaireData.put("typeJugement",         affaire.getTypeJugement());
            affaireData.put("dateJugement",         affaire.getDateJugement());
            affaireData.put("montantJuge",          affaire.getMontantJuge());
            affaireData.put("delaiPaiementJuge",    affaire.getDelaiPaiementJuge());
            affaireData.put("descriptionJugement",  affaire.getDescriptionJugement());
            affaireData.put("dateLimiteAppel",      affaire.getDateLimiteAppel());
            // Dans la section affaireData de voirAffaire()

// ✅ PV
affaireData.put("pvTexte",  affaire.getPvTexte());
affaireData.put("pvStatut", affaire.getPvStatut() != null
                            ? affaire.getPvStatut().name() : null);

// ✅ Fichiers PV — l'agent peut les télécharger
List<Map<String, Object>> fichiers = new ArrayList<>();
if (affaire.getPvFichiers() != null) {
    for (String data : affaire.getPvFichiers()) {
        String[] parts = data.split("\\|", 3);
        if (parts.length == 3) {
            fichiers.add(Map.of(
                "nom",      parts[0],
                "typeMime", parts[1],
                "base64",   parts[2]
            ));
        }
    }
}
affaireData.put("pvFichiers", fichiers);

// ✅ Facture
affaireData.put("factureRef",     affaire.getFactureRef());
affaireData.put("montantFacture", affaire.getMontantFacture());
affaireData.put("factureStatut",  affaire.getFactureStatut() != null
                                  ? affaire.getFactureStatut().name() : null);
    
            // Avocat
            if (affaire.getAvocat() != null) {
                try {
                    Prestataire av = affaire.getAvocat();
                    Map<String, Object> avocat = new HashMap<>();
                    avocat.put("id",        av.getId());
                    avocat.put("nom",       av.getNom());
                    avocat.put("prenom",    av.getPrenom());
                    avocat.put("email",     av.getEmail());
                    avocat.put("telephone", av.getTelephone());
                    affaireData.put("avocat", avocat);
                } catch (Exception e) {
                    log.warn("Avocat lazy : {}", e.getMessage());
                }
            }
    
            // Audiences
            List<Map<String, Object>> audiences = new ArrayList<>();
            if (affaire.getAudiences() != null) {
                for (Audience aud : affaire.getAudiences()) {
                    Map<String, Object> a = new HashMap<>();
                    a.put("id",                aud.getId());
                    a.put("dateAudience",      aud.getDateAudience());
                    a.put("heure",             aud.getHeure());
                    a.put("salle",             aud.getSalle());
                    a.put("motif",             aud.getMotif());
                    a.put("resultat",          aud.getResultat());
                    a.put("statut",            aud.getStatut());
                    a.put("prochaineAudience", aud.getProchaineAudience());
                    audiences.add(a);
                }
            }
            affaireData.put("audiences", audiences);
    
            // ✅ Mission — PV + Facture visibles par l'agent
            if (affaire.getMission() != null) {
                try {
                    Mission m = affaire.getMission();
                    Map<String, Object> mission = new HashMap<>();
                    mission.put("id",             m.getId());
                    mission.put("statut",         m.getStatut() != null ? m.getStatut().name() : null);
    
                    // ✅ PV
                    mission.put("pvTexte",        m.getPvMission());
                    mission.put("pvStatut",       m.getStatut() == StatutMission.PV_SOUMIS
                                                  || m.getStatut() == StatutMission.FACTURE_SOUMISE
                                                  || m.getStatut() == StatutMission.TERMINEE
                                                  ? "EN_ATTENTE" : null);
                    mission.put("dateValidationPv", m.getDateValidationPv());
    
                    // ✅ Facture
                    mission.put("factureRef",       m.getFactureRef());
                    mission.put("montantFacture",   m.getMontantFacture());
                    mission.put("factureStatut",    m.getStatut() == StatutMission.FACTURE_SOUMISE
                                                    || m.getStatut() == StatutMission.TERMINEE
                                                    ? "EN_ATTENTE" : null);
                    mission.put("dateValidationFacture", m.getDateValidationFacture());
    
                    affaireData.put("mission", mission);
                } catch (Exception e) {
                    log.warn("Mission lazy : {}", e.getMessage());
                }
            }
    
            response.put("affaire", affaireData);
        }
    
        return ResponseEntity.ok(response);
    }

  
}