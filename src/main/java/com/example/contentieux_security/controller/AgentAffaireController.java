package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.DossierDetailDTO;
import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.service.AffaireJudiciaireService;
import com.example.contentieux_security.service.DossierService;
import com.example.contentieux_security.service.MissionService;
import com.example.contentieux_security.service.PrestationService;
import com.example.contentieux_security.repository.AgentBancaireRepository;
import com.example.contentieux_security.repository.PrestataireRepository;
import com.example.contentieux_security.enums.TypePrestataire;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
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
    private final DossierService dossierService;
    private final AgentBancaireRepository agentBancaireRepository;

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
        response.put("dossierId", dossierId);
        response.put("prestationId", prestationId);
        response.put("avocats", avocats);

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
            String dateFinStr = (String) body.get("dateFinPrevue");
            LocalDate dateFinPrevue = (dateFinStr != null && !dateFinStr.isBlank()) ? LocalDate.parse(dateFinStr) : null;

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

    @GetMapping("/{dossierId}/affaire/lancer")
    public ResponseEntity<?> formulaireLancerAffaire(@PathVariable Long dossierId) {
        Mission missionAvocat = missionService.getMissionAvocatDuDossier(dossierId);

        if (missionAvocat == null) {
            Prestation prestation = prestationService.getPrestationJudiciaireParDossier(dossierId);
            if (prestation != null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Désignez d'abord un avocat.",
                    "prestationId", prestation.getId()
                ));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Aucune procédure judiciaire trouvée."));
        }

        DossierDetailDTO dossier = dossierService.getDossierDetail(dossierId);

        Map<String, Object> response = new HashMap<>();
        response.put("dossierId", dossierId);
        response.put("missionId", missionAvocat.getId());
        response.put("avocat", missionAvocat.getPrestataire());
        response.put("dossier", dossier);
        response.put("mission", missionAvocat);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{dossierId}/affaire/lancer")
    public ResponseEntity<?> lancerAffaire(@PathVariable Long dossierId,
                                           @RequestBody Map<String, Object> body,
                                           Principal principal) {
        try {
            Long missionId = Long.valueOf(body.get("missionId").toString());
            log.info(">>> LANCER AFFAIRE - dossierId={} missionId={} agent={}", dossierId, missionId, principal.getName());

            AffaireJudiciaire affaire = affaireService.creerAffaire(missionId, principal.getName());

            log.info(">>> AFFAIRE CRÉÉE : id={} num={}", affaire.getId(), affaire.getNumeroAffaire());

            return ResponseEntity.ok(Map.of(
                "message", "Affaire lancée avec succès",
                "affaire", affaire
            ));

        } catch (Exception e) {
            log.error(">>> ERREUR : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{dossierId}/affaire")
    public ResponseEntity<?> voirAffaire(@PathVariable Long dossierId) {
        AffaireJudiciaire affaire = affaireService.getAffaireParDossier(dossierId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("dossierId", dossierId);
        
        if (affaire == null) {
            response.put("pasDAffaire", true);
        } else {
            affaire = affaireService.getAffaireById(affaire.getId());
            response.put("affaire", affaire);
        }
        
        return ResponseEntity.ok(response);
    }
}