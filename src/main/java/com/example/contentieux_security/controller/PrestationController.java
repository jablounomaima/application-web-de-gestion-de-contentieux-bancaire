package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.TypePrestataire;
import com.example.contentieux_security.enums.TypePrestation;
import com.example.contentieux_security.repository.AgentBancaireRepository;
import com.example.contentieux_security.repository.PrestataireRepository;
import com.example.contentieux_security.service.PrestationService;
import com.example.contentieux_security.service.AffaireJudiciaireService;
import com.example.contentieux_security.service.DossierService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PrestationController {

    private final PrestationService prestationService;
    private final DossierService dossierService;
    private final PrestataireRepository prestataireRepository;
    private final AffaireJudiciaireService affaireService;
    private final AgentBancaireRepository agentBancaireRepository;

    @GetMapping("/api/agent/dossiers/{dossierId}/prestations/lancer")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<?> formLancer(@PathVariable Long dossierId, Principal principal) {
        DossierContentieux dossier = dossierService.getDossierById(dossierId);

        if (!"VALIDE".equals(dossier.getStatut().name())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le dossier doit être au statut VALIDE"));
        }

        AgentBancaire agent = agentBancaireRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("Agent introuvable"));

        Map<String, Object> response = new HashMap<>();
        response.put("dossier", dossier);
        response.put("typesPrestations", TypePrestation.values());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/agent/dossiers/{dossierId}/prestations")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<?> lancerPrestation(@PathVariable Long dossierId,
                                              @RequestBody Map<String, Object> body,
                                              Authentication authentication) {
        try {
            TypePrestation type = TypePrestation.valueOf(body.get("type").toString());
            String description = (String) body.get("description");

            Prestation p = prestationService.lancerPrestation(dossierId, type, description, authentication.getName());
            return ResponseEntity.ok(Map.of("message", "Prestation lancée avec succès", "prestation", p));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur lancement prestation", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur inattendue : " + e.getMessage()));
        }
    }

    @GetMapping("/api/agent/dossiers/{dossierId}/prestations/{prestationId}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<?> detailPrestation(@PathVariable Long dossierId, @PathVariable Long prestationId) {
        DossierContentieux dossier = dossierService.getDossierById(dossierId);
        Prestation prestation = prestationService.getPrestationById(prestationId);

        if (!prestation.getDossier().getId().equals(dossierId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prestation non liée à ce dossier"));
        }

        List<Mission> missions = prestationService.getMissionsByPrestation(prestationId);
        List<Prestataire> prestataires = prestataireRepository.findByTypeInAndActifTrue(
            List.of(TypePrestataire.AVOCAT, TypePrestataire.EXPERT, TypePrestataire.HUISSIER)
        );

        Map<String, Object> response = new HashMap<>();
        response.put("dossier", dossier);
        response.put("prestation", prestation);
        response.put("missions", missions);
        response.put("prestataires", prestataires);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/agent/dossiers/{dossierId}/prestations/{prestationId}/designer")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<?> designerPrestataire(@PathVariable Long dossierId,
                                                 @PathVariable Long prestationId,
                                                 @RequestBody Map<String, Object> body,
                                                 Authentication authentication) {
        try {
            Long prestataireId = Long.valueOf(body.get("prestataireId").toString());
            String description = (String) body.get("description");
            String dateFinPrevue = (String) body.get("dateFinPrevue");

            LocalDate dateFin = (dateFinPrevue != null && !dateFinPrevue.isBlank()) ? LocalDate.parse(dateFinPrevue) : null;

            Mission m = prestationService.designerPrestataire(prestationId, prestataireId, description, dateFin, authentication.getName());

            Prestation prestation = prestationService.getPrestationById(prestationId);
            boolean estAvocat = m.getPrestataire() != null && m.getPrestataire().getType() == TypePrestataire.AVOCAT;
            boolean estProcedureJudiciaire = prestation.getType() == TypePrestation.PROCEDURE_JUDICIAIRE;

            Map<String, Object> response = new HashMap<>();
            response.put("mission", m);
            response.put("message", "Mission assignée avec succès");

            if (estProcedureJudiciaire && estAvocat) {
                try {
                    DossierContentieux dossier = prestation.getDossier();
                    if (dossier == null) {
                        dossier = dossierService.getDossierById(dossierId);
                    }

                    AffaireJudiciaire affaire = affaireService.creerAffaireDirecte(m.getPrestataire(), dossier, authentication.getName());
                    response.put("affaire", affaire);
                    response.put("message", "Mission assignée et affaire judiciaire créée avec succès");
                } catch (IllegalStateException e) {
                    log.warn("Affaire déjà existante : {}", e.getMessage());
                } catch (Exception e) {
                    log.error("Erreur création affaire : {}", e.getMessage(), e);
                }
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Données invalides : " + e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur designerPrestataire", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur : " + e.getMessage()));
        }
    }

    @DeleteMapping("/api/prestataire/missions/fichiers/{fichierId}")
    @PreAuthorize("hasAnyRole('ROLE_AVOCAT','ROLE_HUISSIER','ROLE_EXPERT')")
    public ResponseEntity<?> supprimerFichierResultat(@PathVariable Long fichierId, Authentication authentication) {
        try {
            prestationService.supprimerFichierResultat(fichierId, authentication.getName());
            return ResponseEntity.ok(Map.of("message", "Fichier supprimé avec succès"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur suppression : " + e.getMessage()));
        }
    }
}