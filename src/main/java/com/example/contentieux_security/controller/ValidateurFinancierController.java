package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.TypeValidateur;
import com.example.contentieux_security.repository.*;
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

    private final ValidateurRepository  validateurRepository;
    private final MissionRepository     missionRepository;
    private final DossierRepository     dossierRepository;

    // ═══════════════════════════════════════════════════════
    // TOUTES LES FACTURES DE L'AGENCE
    // ═══════════════════════════════════════════════════════
    @GetMapping("/factures")
    @Transactional(readOnly = true)
    public ResponseEntity<?> toutesLesFactures(Principal principal) {
        try {
            // ✅ Utiliser Validateur directement
            Validateur validateur = validateurRepository
                    .findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException(
                            "Validateur introuvable : " + principal.getName()));

            // ✅ Vérifier que c'est bien un validateur financier
            if (validateur.getTypeValidateur() != TypeValidateur.VALIDATEUR_FINANCIER) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Accès réservé au validateur financier"));
            }

            Long agenceId = validateur.getAgence().getId();

            List<Mission> missions = missionRepository.findFacturesParAgence(agenceId);

            // ✅ Grouper par dossier
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
                    d.put("statut",        dossier.getStatut() != null
                                           ? dossier.getStatut().name() : null);

                    // Client
                    if (dossier.getClient() != null) {
                        Client c = dossier.getClient();
                        boolean isEntreprise = c.getTypeClient() != null
                                && c.getTypeClient().name().equals("ENTREPRISE");
                        String nom = isEntreprise
                                ? c.getRaisonSociale()
                                : (c.getNom() + " " + c.getPrenom()).trim();
                        d.put("clientNom", nom);
                    }

                    d.put("factures",  new ArrayList<>());
                    d.put("totalHT",   0.0);
                    d.put("totalTTC",  0.0);
                    return d;
                });

                // ✅ Construire la facture
                Map<String, Object> factureData = new LinkedHashMap<>();
                factureData.put("missionId",     m.getId());
                factureData.put("numeroMission", m.getNumeroMission());
                factureData.put("factureRef",    m.getFactureRef());
                factureData.put("montantHT",     m.getMontantFacture());
                factureData.put("montantTTC",    m.getMontantFacture() != null
                                                 ? m.getMontantFacture() * 1.19 : 0);
                factureData.put("factureValide", m.getFactureValide());
                factureData.put("dateFacture",   m.getDateValidationFacture());
                factureData.put("commentaire",   m.getCommentaireAgent());
                factureData.put("statutMission",  m.getStatut() != null
                                  ? m.getStatut().name() : null);
factureData.put("statutLibelle",  m.getStatut() != null
                                  ? m.getStatut().getLibelle() : null);

                // ✅ Prestataire
                if (m.getPrestataire() != null) {
                    Prestataire p = m.getPrestataire();
                    factureData.put("prestataireId",    p.getId());
                    factureData.put("prestataireNom",
                        (p.getPrenom() + " " + p.getNom()).trim());
                    factureData.put("prestataireType",
                        p.getType() != null ? p.getType().name() : null);
                    factureData.put("prestataireEmail", p.getEmail());
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> factures =
                    (List<Map<String, Object>>) parDossier.get(dossierId).get("factures");
                factures.add(factureData);

                double ht  = m.getMontantFacture() != null ? m.getMontantFacture() : 0.0;
                parDossier.get(dossierId).merge("totalHT",  ht,
                    (a, b) -> (double) a + (double) b);
                parDossier.get(dossierId).merge("totalTTC", ht * 1.19,
                    (a, b) -> (double) a + (double) b);
            }

            return ResponseEntity.ok(Map.of(
                "dossiers",      new ArrayList<>(parDossier.values()),
                "totalDossiers", parDossier.size(),
                "totalFactures", missions.size()
            ));

        } catch (Exception e) {
            log.error("Erreur factures: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════
    // FACTURES D'UN DOSSIER SPÉCIFIQUE
    // ═══════════════════════════════════════════════════════
    @GetMapping("/dossiers/{dossierId}/factures")
    @Transactional(readOnly = true)
    public ResponseEntity<?> facturesParDossier(
            @PathVariable Long dossierId,
            Principal principal) {
        try {
            // ✅ Vérifier validateur
            Validateur validateur = validateurRepository
                    .findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Validateur introuvable"));

            if (validateur.getTypeValidateur() != TypeValidateur.VALIDATEUR_FINANCIER) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Accès refusé"));
            }

            List<Mission> missions = missionRepository
                    .findFacturesParDossier(dossierId);

            List<Map<String, Object>> factures = missions.stream().map(m -> {
                Map<String, Object> f = new LinkedHashMap<>();
                f.put("missionId",     m.getId());
                f.put("numeroMission", m.getNumeroMission());
                f.put("factureRef",    m.getFactureRef());
                f.put("montantHT",     m.getMontantFacture());
                f.put("montantTTC",    m.getMontantFacture() != null
                                       ? m.getMontantFacture() * 1.19 : 0);
                f.put("factureValide", m.getFactureValide());
                f.put("dateFacture",   m.getDateValidationFacture());
                f.put("commentaire",   m.getCommentaireAgent());

                if (m.getPrestataire() != null) {
                    Prestataire p = m.getPrestataire();
                    f.put("prestataireId",    p.getId());
                    f.put("prestataireNom",
                        (p.getPrenom() + " " + p.getNom()).trim());
                    f.put("prestataireType",
                        p.getType() != null ? p.getType().name() : null);
                    f.put("prestataireEmail", p.getEmail());
                }
                return f;
            }).collect(Collectors.toList());

            double totalHT = missions.stream()
                    .mapToDouble(m -> m.getMontantFacture() != null
                                     ? m.getMontantFacture() : 0)
                    .sum();

            return ResponseEntity.ok(Map.of(
                "dossierId",  dossierId,
                "factures",   factures,
                "totalHT",    totalHT,
                "totalTTC",   totalHT * 1.19,
                "nbFactures", factures.size()
            ));

        } catch (Exception e) {
            log.error("Erreur factures dossier {}: {}", dossierId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════
    // VALIDER / REJETER UNE FACTURE
    // ═══════════════════════════════════════════════════════
    @PostMapping("/missions/{missionId}/valider-facture")
    @Transactional
    public ResponseEntity<?> validerFacture(
            @PathVariable Long missionId,
            @RequestBody Map<String, Object> body,
            Principal principal) {
        try {
            log.info("=== DEBUT validerFacture missionId={} user={}",
                    missionId, principal.getName());
    
            // ── Étape 1 : Trouver validateur ──
            Validateur validateur = validateurRepository
                    .findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException(
                            "Validateur introuvable : " + principal.getName()));
            log.info("Étape 1 OK - validateur={} type={}",
                    validateur.getId(), validateur.getTypeValidateur());
    
            // ── Étape 2 : Vérifier type ──
            if (validateur.getTypeValidateur() != TypeValidateur.VALIDATEUR_FINANCIER) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }
            log.info("Étape 2 OK - type validateur financier confirmé");
    
            // ── Étape 3 : Trouver mission ──
            Mission mission = missionRepository.findById(missionId)
                    .orElseThrow(() -> new RuntimeException(
                            "Mission introuvable : " + missionId));
            log.info("Étape 3 OK - mission={} statut={}",
                    mission.getId(), mission.getStatut());
    
            // ── Étape 4 : Vérifier facture ──
            if (mission.getFactureRef() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Aucune facture soumise."));
            }
            log.info("Étape 4 OK - factureRef={}", mission.getFactureRef());
    
            // ── Étape 5 : Appliquer décision ──
            boolean valide = Boolean.TRUE.equals(body.get("valide"));
            String commentaire = body.get("commentaire") != null
                                 ? body.get("commentaire").toString() : "";
            log.info("Étape 5 - valide={} commentaire={}", valide, commentaire);
    
            mission.setFactureValide(valide);
            mission.setCommentaireAgent(commentaire.isBlank() ? null : commentaire);
            mission.setValideParAgent(principal.getName());
            mission.setDateValidationFacture(LocalDateTime.now());
            mission.setStatut(valide
                    ? StatutMission.FACTURE_VALIDEE
                    : StatutMission.FACTURE_REJETEE);
    
            // ── Étape 6 : Sauvegarder ──
            missionRepository.save(mission);
            log.info("Étape 6 OK - mission sauvegardée statut={}",
                    mission.getStatut());
    
            return ResponseEntity.ok(Map.of(
                "message",       valide ? "Facture validée." : "Facture rejetée.",
                "missionId",     missionId,
                "statut",        mission.getStatut().name(),
                "factureValide", valide
            ));
    
        } catch (Exception e) {
            log.error("=== ERREUR validerFacture missionId={} : {}",
                    missionId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    // ═══════════════════════════════════════════════════════
    // DASHBOARD RÉSUMÉ
    // ═══════════════════════════════════════════════════════
    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public ResponseEntity<?> dashboard(Principal principal) {
        try {
            Validateur validateur = validateurRepository
                    .findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Validateur introuvable"));

            Long agenceId = validateur.getAgence().getId();
            List<Mission> missions = missionRepository.findFacturesParAgence(agenceId);

            long enAttente = missions.stream()
                    .filter(m -> m.getFactureValide() == null)
                    .count();
            long validees = missions.stream()
                    .filter(m -> Boolean.TRUE.equals(m.getFactureValide()))
                    .count();
            long rejetees = missions.stream()
                    .filter(m -> Boolean.FALSE.equals(m.getFactureValide()))
                    .count();
            double totalHT = missions.stream()
                    .filter(m -> Boolean.TRUE.equals(m.getFactureValide()))
                    .mapToDouble(m -> m.getMontantFacture() != null
                                     ? m.getMontantFacture() : 0)
                    .sum();

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
                    "agence", validateur.getAgence() != null
                              ? validateur.getAgence().getNom() : ""
                )
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}