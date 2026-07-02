package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.DossierDetailDTO;
import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.TypePrestation;
import com.example.contentieux_security.service.AffaireJudiciaireService;
import com.example.contentieux_security.service.DossierService;
import com.example.contentieux_security.service.MissionService;
import com.example.contentieux_security.service.NotificationService;
import com.example.contentieux_security.service.PrestationService;
import com.example.contentieux_security.repository.AgentBancaireRepository;
import com.example.contentieux_security.repository.PrestataireRepository;
import com.example.contentieux_security.enums.TypePrestataire;
import com.example.contentieux_security.enums.StatutMission;
import lombok.RequiredArgsConstructor;
import com.example.contentieux_security.repository.DossierRepository;
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
import com.example.contentieux_security.entity.DossierContentieux;
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
    private final NotificationService      notificationService; // ✅ AJOUTÉ
private final DossierRepository dossierRepository;
    // ═══════════════════════════════════════════════════════════════
    // LANCER LA PROCÉDURE JUDICIAIRE
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/{dossierId}/prestations/lancer")
    public ResponseEntity<?> lancerProcedureJudiciaire(
            @PathVariable Long dossierId,
            @RequestBody Map<String, Object> body,
            Principal principal) {

        try {
            log.info(">>> LANCER PROCÉDURE - dossierId={} agent={}", dossierId, principal.getName());

            String typeProcedure        = (String) body.get("typeProcedure");
            String tribunal             = (String) body.get("tribunal");
            String chambre              = (String) body.get("chambre");
            String numeroRole           = (String) body.get("numeroRole");
            String datePremierAudienceS = (String) body.get("datePremierAudience");
            String motif                = (String) body.get("motif");
            String observations         = (String) body.get("observations");

            if (typeProcedure == null || typeProcedure.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Le type de procédure est obligatoire."));
            if (tribunal == null || tribunal.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Le tribunal est obligatoire."));
            if (datePremierAudienceS == null || datePremierAudienceS.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "La date de première audience est obligatoire."));
            if (motif == null || motif.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Le motif de la procédure est obligatoire."));

            StringBuilder description = new StringBuilder();
            description.append("Type procédure : ").append(typeProcedure);
            description.append(" | Tribunal : ").append(tribunal);
            if (chambre != null && !chambre.isBlank())
                description.append(" | Chambre : ").append(chambre);
            if (numeroRole != null && !numeroRole.isBlank())
                description.append(" | N° Rôle : ").append(numeroRole);
            description.append(" | 1ère audience : ").append(datePremierAudienceS);
            description.append(" | Motif : ").append(motif);
            if (observations != null && !observations.isBlank())
                description.append(" | Observations : ").append(observations);

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
            log.warn(">>> RÈGLE MÉTIER : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn(">>> ARGUMENT INVALIDE : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error(">>> ERREUR inattendue : {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur interne. Veuillez réessayer."));
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

        // ── 🔔 Notifier l'avocat nouvellement désigné ──────────────
        try {
            Prestataire avocat = mission.getPrestataire();
            if (avocat != null && avocat.getUsername() != null) {
                DossierContentieux dossier = dossierRepository.findById(dossierId).orElse(null);

                String clientNom = "—";
                if (dossier != null && dossier.getClient() != null) {
                    clientNom = dossier.getClient().getNom()
                            + (dossier.getClient().getPrenom() != null
                                ? " " + dossier.getClient().getPrenom() : "");
                }

                String numeroDossier = dossier != null ? dossier.getNumeroDossier() : String.valueOf(dossierId);

                notificationService.notifier(
                        avocat.getUsername(),
                        "⚖️ Dossier assigné — " + numeroDossier,
                        String.format(
                                "Le dossier %s vous a été assigné en tant qu'avocat.\nClient : %s",
                                numeroDossier, clientNom),
                        "NOUVELLE_AFFAIRE",
                        dossier,
                        "/avocat/affaires"
                );
            }
        } catch (Exception notifEx) {
            // On ne fait jamais échouer la désignation à cause d'un souci de notif
            log.warn(">>> Notification désignation avocat non envoyée : {}", notifEx.getMessage());
        }

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
            return ResponseEntity.badRequest().body(Map.of("error", "Aucune procédure judiciaire trouvée."));
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

    @GetMapping("/{dossierId}/affaire/avocats")
    public ResponseEntity<?> avocatsPourReaffectation(@PathVariable Long dossierId, Principal principal) {
        AgentBancaire agent = agentBancaireRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Agent introuvable"));

        List<Prestataire> avocats = prestataireRepository.findByTypeAndAgentResponsable_Id(
                TypePrestataire.AVOCAT, agent.getId()
        ).stream()
                .filter(Prestataire::isActif)
                .toList();

        return ResponseEntity.ok(Map.of(
                "dossierId", dossierId,
                "avocats", avocats
        ));
    }

    @PostMapping("/{dossierId}/affaire/reassigner-avocat")
    public ResponseEntity<?> reassignerAvocat(@PathVariable Long dossierId,
                                              @RequestBody Map<String, Object> body) {
        try {
            Long prestataireId = Long.valueOf(body.get("prestataireId").toString());

            AffaireJudiciaire affaireAvant = affaireService.getAffaireParDossier(dossierId);
            Prestataire ancienAvocat = affaireAvant != null ? affaireAvant.getAvocat() : null;

            AffaireJudiciaire affaire = affaireService.reassignerAvocatPourDossier(dossierId, prestataireId);
            Prestataire nouvelAvocat = affaire.getAvocat();
            DossierContentieux dossier = affaire.getDossier();

            if (ancienAvocat != null && nouvelAvocat != null
                    && !ancienAvocat.getId().equals(nouvelAvocat.getId())) {

                String clientNom = "—";
                if (dossier != null && dossier.getClient() != null) {
                    clientNom = dossier.getClient().getNom()
                            + (dossier.getClient().getPrenom() != null ? " " + dossier.getClient().getPrenom() : "");
                }

                notificationService.notifier(
                        nouvelAvocat.getUsername(),
                        "⚖️ Dossier assigné — " + dossier.getNumeroDossier(),
                        String.format(
                                "Le dossier %s vous a été assigné en tant qu'avocat.\nClient : %s\nAffaire : %s",
                                dossier.getNumeroDossier(), clientNom,
                                affaire.getNumeroAffaire()),
                        "NOUVELLE_AFFAIRE",
                        dossier,
                        "/avocat/affaires"
                );

                notificationService.notifier(
                        ancienAvocat.getUsername(),
                        "🔄 Dossier réassigné",
                        String.format(
                                "Le dossier %s a été réassigné à un autre avocat.\nAffaire : %s",
                                dossier.getNumeroDossier(), affaire.getNumeroAffaire()),
                        "AFFAIRE_REASSIGNEE",
                        dossier,
                        "/avocat/affaires"
                );
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Avocat réassigné avec succès",
                    "affaire", affaire
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn(">>> RÉASSIGNATION AVOCAT IMPOSSIBLE : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error(">>> ERREUR réassignation avocat : {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur interne. Veuillez réessayer."));
        }
    }

    @PostMapping("/{dossierId}/affaire/lancer")
    public ResponseEntity<?> lancerAffaire(@PathVariable Long dossierId,
                                           @RequestBody Map<String, Object> body,
                                           Principal principal) {
        try {
            Long missionId = Long.valueOf(body.get("missionId").toString());
            log.info(">>> LANCER AFFAIRE - dossierId={} missionId={} agent={}",
                    dossierId, missionId, principal.getName());

            Mission mission = missionService.getMissionAvocatDuDossier(dossierId);
            Long avocatId = mission.getPrestataire().getId();

            // ── 1. Créer l'affaire ────────────────────────────────────
            AffaireJudiciaire affaire = affaireService.creerAffaire(dossierId, avocatId, principal.getName());
            log.info(">>> AFFAIRE CRÉÉE : id={} num={}", affaire.getId(), affaire.getNumeroAffaire());

            // ── 2. Notification → avocat ──────────────────────────────
            // ✅ C'est ici que la notification manquait.
            // L'avocat reçoit maintenant un signal WebSocket immédiatement
            // après la création de l'affaire, ce qui déclenche le rechargement
            // automatique dans avocat-dashboard sans recharger la page.
            try {
                Prestataire avocat = affaire.getAvocat();
                DossierContentieux dossier = affaire.getDossier();

                if (avocat != null && dossier != null) {
                    String clientNom = "—";
                    if (dossier.getClient() != null) {
                        clientNom = dossier.getClient().getNom()
                                + (dossier.getClient().getPrenom() != null
                                   ? " " + dossier.getClient().getPrenom() : "");
                    }

                    notificationService.notifier(
                            avocat.getUsername(),
                            "⚖️ Nouvelle affaire judiciaire — " + affaire.getNumeroAffaire(),
                            String.format(
                                "Une nouvelle affaire judiciaire vous a été assignée.\n" +
                                "Affaire  : %s\n" +
                                "Dossier  : %s\n" +
                                "Client   : %s\n" +
                                "Date     : %s",
                                affaire.getNumeroAffaire(),
                                dossier.getNumeroDossier(),
                                clientNom,
                                affaire.getDateLancement()
                            ),
                            "NOUVELLE_AFFAIRE",   // ← type écouté par avocat-dashboard
                            dossier,              // ← dossierId inclus automatiquement
                            "/avocat/affaires"
                    );

                    log.info("✅ Notification NOUVELLE_AFFAIRE envoyée à l'avocat {}",
                            avocat.getUsername());
                }
            } catch (Exception notifEx) {
                // La notification échoue silencieusement — l'affaire est déjà créée
                log.warn("⚠️ Notification avocat échouée (affaire créée quand même) : {}",
                        notifEx.getMessage());
            }

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
    @Transactional(readOnly = true)
    public ResponseEntity<?> voirAffaire(@PathVariable Long dossierId) {
        List<AffaireJudiciaire> affairesList = affaireService.getAllAffairesParDossier(dossierId);

        Map<String, Object> response = new HashMap<>();
        response.put("dossierId", dossierId);

        if (affairesList == null || affairesList.isEmpty()) {
            response.put("pasDAffaire", true);
        } else {
            response.put("pasDAffaire", false);
            List<Map<String, Object>> affairesData = new ArrayList<>();
            for (AffaireJudiciaire a : affairesList) {
                AffaireJudiciaire affaire = affaireService.getAffaireById(a.getId());
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

                // PV
                affaireData.put("pvTexte",  affaire.getPvTexte());
                affaireData.put("pvStatut", affaire.getPvStatut() != null
                                            ? affaire.getPvStatut().name() : null);

                List<Map<String, Object>> fichiers = new ArrayList<>();
                if (affaire.getPvFichiers() != null) {
                    for (String data : affaire.getPvFichiers()) {
                        String[] parts = data.split("\\|", 3);
                        if (parts.length == 3) {
                            fichiers.add(Map.of("nom", parts[0], "typeMime", parts[1], "base64", parts[2]));
                        }
                    }
                }
                affaireData.put("pvFichiers", fichiers);

                // Facture
                affaireData.put("factureRef",                    affaire.getFactureRef());
                affaireData.put("montantFacture",                affaire.getMontantFacture());
                affaireData.put("factureStatut",                 affaire.getFactureStatut() != null
                                                                  ? affaire.getFactureStatut().name() : null);
                affaireData.put("factureCommentaireValidation",  affaire.getFactureCommentaireValidation());
                affaireData.put("factureValidePar",              affaire.getFactureValidePar());

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
                        Map<String, Object> audData = new HashMap<>();
                        audData.put("id",                aud.getId());
                        audData.put("dateAudience",      aud.getDateAudience());
                        audData.put("heure",             aud.getHeure());
                        audData.put("salle",             aud.getSalle());
                        audData.put("motif",             aud.getMotif());
                        audData.put("resultat",          aud.getResultat());
                        audData.put("statut",            aud.getStatut());
                        audData.put("prochaineAudience", aud.getProchaineAudience());
                        audiences.add(audData);
                    }
                }
                affaireData.put("audiences", audiences);
                affairesData.add(affaireData);
            }
            response.put("affaires", affairesData);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{dossierId}/affaire/{affaireId}/pv/valider")
    public ResponseEntity<?> validerPV(@PathVariable Long dossierId, @PathVariable Long affaireId, @RequestBody Map<String, Boolean> body) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null) return ResponseEntity.badRequest().body(Map.of("error", "Aucune affaire trouvée."));
            boolean accepte = body.getOrDefault("accepte", false);
            affaireService.validerPV(affaire.getId(), accepte);

            // ── 🔔 Notifier l'avocat de la décision sur son PV ──────────────
            try {
                String avocatUsername = affaire.getAvocat() != null ? affaire.getAvocat().getUsername() : null;
                if (avocatUsername != null) {
                    String numeroAffaire = affaire.getNumeroAffaire() != null ? affaire.getNumeroAffaire() : String.valueOf(affaireId);
                    String typeNotif = accepte ? "PV_VALIDE" : "PV_REFUSE";
                    String titre     = accepte ? "✅ Votre PV a été validé" : "❌ Votre PV a été refusé";
                    String message   = accepte
                        ? "Votre procès-verbal pour l'affaire " + numeroAffaire + " a été validé. Vous pouvez maintenant soumettre votre facture d'honoraires."
                        : "Votre procès-verbal pour l'affaire " + numeroAffaire + " a été refusé. Veuillez le corriger et le resoumettre.";
                    String urlAction = "/avocat/affaires/" + affaireId + "/honoraires";
                    notificationService.notifierSansDossier(avocatUsername, titre, message, typeNotif, urlAction);
                }
            } catch (Exception notifEx) {
                log.warn(">>> Notification PV avocat non envoyée : {}", notifEx.getMessage());
            }

            return ResponseEntity.ok(Map.of("message", "PV " + (accepte ? "validé" : "refusé")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{dossierId}/affaire/{affaireId}/facture/valider")
    public ResponseEntity<?> validerFacture(@PathVariable Long dossierId, @PathVariable Long affaireId, @RequestBody Map<String, Boolean> body) {
        // ✅ La validation des factures avocat passe désormais par le VALIDATEUR FINANCIER
        // Ce endpoint est conservé pour compatibilité mais retourne une erreur explicite.
        return ResponseEntity.status(403).body(Map.of(
            "error", "La validation des factures d'avocat est désormais effectuée par le validateur financier. Veuillez contacter le validateur financier."
        ));
    }
}