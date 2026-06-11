package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.TypeValidateur;
import com.example.contentieux_security.repository.*;
import com.example.contentieux_security.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.example.contentieux_security.enums.StatutMission;
import java.time.LocalDateTime;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/validateur/financier")
@PreAuthorize("hasAnyRole('VALIDATEUR_FINANCIER','ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class ValidateurFinancierController {

    private final ValidateurRepository   validateurRepository;
    private final MissionRepository      missionRepository;
    private final DossierRepository      dossierRepository;
    private final NotificationService    notificationService;

    // ═══════════════════════════════════════════════════════
    // TOUTES LES FACTURES DE L'AGENCE
    // ═══════════════════════════════════════════════════════
    @GetMapping("/factures")
    @Transactional(readOnly = true)
    public ResponseEntity<?> toutesLesFactures(Principal principal) {
        try {
            Validateur validateur = validateurRepository
                    .findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException(
                            "Validateur introuvable : " + principal.getName()));

            if (validateur.getTypeValidateur() != TypeValidateur.VALIDATEUR_FINANCIER)
                return ResponseEntity.status(403).body(Map.of("error", "Accès réservé au validateur financier"));

            Long agenceId = validateur.getAgence().getId();
            List<Mission> missions = missionRepository.findFacturesParAgence(agenceId);

            Map<Long, Map<String, Object>> parDossier = new LinkedHashMap<>();

            for (Mission m : missions) {
                Prestation prestation = m.getPrestation();
                if (prestation == null || prestation.getDossier() == null) continue;

                DossierContentieux dossier = prestation.getDossier();
                Long dossierId = dossier.getId();

                parDossier.computeIfAbsent(dossierId, k -> {
                    Map<String, Object> d = new LinkedHashMap<>();
                    d.put("dossierId",     dossier.getId());
                    d.put("numeroDossier", dossier.getNumeroDossier());
                    d.put("libelle",       dossier.getLibelle());
                    d.put("statut",        dossier.getStatut() != null ? dossier.getStatut().name() : null);
                    if (dossier.getClient() != null) {
                        Client c = dossier.getClient();
                        boolean isEntreprise = c.getTypeClient() != null &&
                                               c.getTypeClient().name().equals("ENTREPRISE");
                        String nom = isEntreprise ? c.getRaisonSociale()
                                                  : (c.getNom() + " " + c.getPrenom()).trim();
                        d.put("clientNom", nom);
                    }
                    d.put("factures",  new ArrayList<>());
                    d.put("totalHT",   0.0);
                    d.put("totalTTC",  0.0);
                    return d;
                });

                Map<String, Object> factureData = new LinkedHashMap<>();
                factureData.put("missionId",     m.getId());
                factureData.put("numeroMission", m.getNumeroMission());
                factureData.put("factureRef",    m.getFactureRef());
                factureData.put("montantHT",     m.getMontantFacture());
                factureData.put("montantTTC",    m.getMontantFacture() != null ? m.getMontantFacture() * 1.19 : 0);
                factureData.put("factureValide", m.getFactureValide());
                factureData.put("dateFacture",   m.getDateValidationFacture());
                factureData.put("commentaire",   m.getCommentaireAgent());
                factureData.put("statutMission", m.getStatut() != null ? m.getStatut().name() : null);
                factureData.put("statutLibelle", m.getStatut() != null ? m.getStatut().getLibelle() : null);

                if (m.getPrestataire() != null) {
                    Prestataire p = m.getPrestataire();
                    factureData.put("prestataireId",    p.getId());
                    factureData.put("prestataireNom",   (p.getPrenom() + " " + p.getNom()).trim());
                    factureData.put("prestataireType",  p.getType() != null ? p.getType().name() : null);
                    factureData.put("prestataireEmail", p.getEmail());
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> factures =
                    (List<Map<String, Object>>) parDossier.get(dossierId).get("factures");
                factures.add(factureData);

                double ht = m.getMontantFacture() != null ? m.getMontantFacture() : 0.0;
                parDossier.get(dossierId).merge("totalHT",  ht,  (a, b) -> (double) a + (double) b);
                parDossier.get(dossierId).merge("totalTTC", ht * 1.19, (a, b) -> (double) a + (double) b);
            }

            return ResponseEntity.ok(Map.of(
                "dossiers",      new ArrayList<>(parDossier.values()),
                "totalDossiers", parDossier.size(),
                "totalFactures", missions.size()
            ));

        } catch (Exception e) {
            log.error("Erreur factures: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════
    // FACTURES D'UN DOSSIER SPÉCIFIQUE
    // ═══════════════════════════════════════════════════════
    @GetMapping("/dossiers/{dossierId}/factures")
    @Transactional(readOnly = true)
    public ResponseEntity<?> facturesParDossier(@PathVariable Long dossierId, Principal principal) {
        try {
            Validateur validateur = validateurRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Validateur introuvable"));
            if (validateur.getTypeValidateur() != TypeValidateur.VALIDATEUR_FINANCIER)
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));

            List<Mission> missions = missionRepository.findFacturesParDossier(dossierId);
            List<Map<String, Object>> factures = missions.stream().map(m -> {
                Map<String, Object> f = new LinkedHashMap<>();
                f.put("missionId",     m.getId());
                f.put("numeroMission", m.getNumeroMission());
                f.put("factureRef",    m.getFactureRef());
                f.put("montantHT",     m.getMontantFacture());
                f.put("montantTTC",    m.getMontantFacture() != null ? m.getMontantFacture() * 1.19 : 0);
                f.put("factureValide", m.getFactureValide());
                f.put("dateFacture",   m.getDateValidationFacture());
                f.put("commentaire",   m.getCommentaireAgent());
                if (m.getPrestataire() != null) {
                    Prestataire p = m.getPrestataire();
                    f.put("prestataireId",    p.getId());
                    f.put("prestataireNom",   (p.getPrenom() + " " + p.getNom()).trim());
                    f.put("prestataireType",  p.getType() != null ? p.getType().name() : null);
                    f.put("prestataireEmail", p.getEmail());
                }
                return f;
            }).collect(Collectors.toList());

            double totalHT = missions.stream()
                    .mapToDouble(m -> m.getMontantFacture() != null ? m.getMontantFacture() : 0).sum();

            return ResponseEntity.ok(Map.of(
                "dossierId",  dossierId,
                "factures",   factures,
                "totalHT",    totalHT,
                "totalTTC",   totalHT * 1.19,
                "nbFactures", factures.size()
            ));

        } catch (Exception e) {
            log.error("Erreur factures dossier {}: {}", dossierId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════
    // ✅ VALIDER / REJETER UNE FACTURE — avec notifications complètes
    // ═══════════════════════════════════════════════════════
    @PostMapping("/missions/{missionId}/valider-facture")
    @Transactional
    public ResponseEntity<?> validerFacture(
            @PathVariable Long missionId,
            @RequestBody Map<String, Object> body,
            Principal principal) {
        try {
            log.info("=== DEBUT validerFacture missionId={} user={}", missionId, principal.getName());

            // ── Vérifier validateur ──
            Validateur validateur = validateurRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Validateur introuvable : " + principal.getName()));

            if (validateur.getTypeValidateur() != TypeValidateur.VALIDATEUR_FINANCIER)
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));

            // ── Trouver mission ──
            Mission mission = missionRepository.findById(missionId)
                    .orElseThrow(() -> new RuntimeException("Mission introuvable : " + missionId));

            if (mission.getFactureRef() == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Aucune facture soumise."));

            // ── Décision ──
            boolean valide     = Boolean.TRUE.equals(body.get("valide"));
            String commentaire = body.get("commentaire") != null ? body.get("commentaire").toString() : "";

            mission.setFactureValide(valide);
            mission.setCommentaireAgent(commentaire.isBlank() ? null : commentaire);
            mission.setValideParAgent(principal.getName());
            mission.setDateValidationFacture(LocalDateTime.now());
            mission.setStatut(valide ? StatutMission.FACTURE_VALIDEE : StatutMission.FACTURE_REJETEE);
            missionRepository.save(mission);

            log.info("Mission {} statut → {}", missionId, mission.getStatut());

            // ── Récupérer infos pour notifications ──
            DossierContentieux dossier = mission.getPrestation() != null
                                         ? mission.getPrestation().getDossier() : null;
            String agentUsername       = dossier != null ? dossier.getCreePar() : null;
            String prestataireUsername = mission.getPrestataire() != null
                                         ? mission.getPrestataire().getUsername() : null;

            if (valide) {
                // ✅ CAS 1 — VALIDATION ACCEPTÉE

                // Notifier agent bancaire — URL explicite vers resultats-prestataires + missionId
                if (agentUsername != null && dossier != null) {
                    notificationService.notifier(
                        agentUsername,
                        "✅ Facture validée par le validateur financier",
                        "La facture de la mission " + mission.getNumeroMission()
                            + " (dossier " + dossier.getNumeroDossier() + ")"
                            + " a été validée par le validateur financier."
                            + " Vous pouvez maintenant clôturer la mission.",
                        "VALIDATION_FINANCIERE_OK",
                        dossier,
                        "/agent/dossiers/" + dossier.getId()
                            + "/resultats-prestataires?missionId=" + missionId  // ← URL explicite
                    );
                    log.info("✅ Agent {} notifié — facture validée → /agent/dossiers/{}/resultats-prestataires?missionId={}",
                        agentUsername, dossier.getId(), missionId);
                }

                // Notifier prestataire
                if (prestataireUsername != null) {
                    notificationService.notifierSansDossier(
                        prestataireUsername,
                        "✅ Votre facture a été validée",
                        "Votre facture pour la mission " + mission.getNumeroMission()
                            + " a été validée par le validateur financier."
                            + " L'agent bancaire va maintenant clôturer la mission.",
                        "VALIDATION_FINANCIERE_OK",
                        "/prestataire/missions/" + missionId
                    );
                    log.info("✅ Prestataire {} notifié — facture validée", prestataireUsername);
                }

            } else {
                // ❌ CAS 2 — VALIDATION REJETÉE

                // Notifier agent bancaire — URL explicite vers resultats-prestataires + missionId
                if (agentUsername != null && dossier != null) {
                    notificationService.notifier(
                        agentUsername,
                        "❌ Facture rejetée par le validateur financier",
                        "La facture de la mission " + mission.getNumeroMission()
                            + " (dossier " + dossier.getNumeroDossier() + ")"
                            + " a été rejetée par le validateur financier."
                            + (commentaire.isBlank() ? "" : " Motif : " + commentaire),
                        "REJET_FINANCIER",
                        dossier,
                        "/agent/dossiers/" + dossier.getId()
                            + "/resultats-prestataires?missionId=" + missionId  // ← URL explicite
                    );
                    log.info("✅ Agent {} notifié — facture rejetée → /agent/dossiers/{}/resultats-prestataires?missionId={}",
                        agentUsername, dossier.getId(), missionId);
                }

                // Notifier prestataire
                if (prestataireUsername != null) {
                    notificationService.notifierSansDossier(
                        prestataireUsername,
                        "❌ Votre facture a été rejetée",
                        "Votre facture pour la mission " + mission.getNumeroMission()
                            + " a été rejetée par le validateur financier."
                            + (commentaire.isBlank() ? "" : " Motif : " + commentaire)
                            + " Merci de corriger et resoumettre vos documents.",
                        "REJET_FINANCIER",
                        "/prestataire/missions/" + missionId
                    );
                    log.info("✅ Prestataire {} notifié — facture rejetée", prestataireUsername);
                }
            }

            return ResponseEntity.ok(Map.of(
                "message",       valide ? "Facture validée." : "Facture rejetée.",
                "missionId",     missionId,
                "statut",        mission.getStatut().name(),
                "factureValide", valide
            ));

        } catch (Exception e) {
            log.error("=== ERREUR validerFacture missionId={} : {}", missionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════
    // DASHBOARD RÉSUMÉ
    // ═══════════════════════════════════════════════════════
    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public ResponseEntity<?> dashboard(Principal principal) {
        try {
            Validateur validateur = validateurRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Validateur introuvable"));

            Long agenceId = validateur.getAgence().getId();
            List<Mission> missions = missionRepository.findFacturesParAgence(agenceId);

            long enAttente = missions.stream().filter(m -> m.getFactureValide() == null).count();
            long validees  = missions.stream().filter(m -> Boolean.TRUE.equals(m.getFactureValide())).count();
            long rejetees  = missions.stream().filter(m -> Boolean.FALSE.equals(m.getFactureValide())).count();
            double totalHT = missions.stream()
                    .filter(m -> Boolean.TRUE.equals(m.getFactureValide()))
                    .mapToDouble(m -> m.getMontantFacture() != null ? m.getMontantFacture() : 0).sum();

            return ResponseEntity.ok(Map.of(
                "totalFactures", missions.size(),
                "enAttente",     enAttente,
                "validees",      validees,
                "rejetees",      rejetees,
                "totalHT",       totalHT,
                "totalTTC",      totalHT * 1.19,
                "validateur",    Map.of(
                    "nom",    validateur.getNom(),
                    "prenom", validateur.getPrenom(),
                    "agence", validateur.getAgence() != null ? validateur.getAgence().getNom() : ""
                )
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}