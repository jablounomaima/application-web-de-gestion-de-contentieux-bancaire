package com.example.contentieux_security.controller;

import com.example.contentieux_security.service.NotificationService;
import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.repository.*;
import com.example.contentieux_security.service.MissionService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/dossiers/{dossierId}/missions")
@PreAuthorize("hasRole('AGENT')")
@RequiredArgsConstructor
public class DossierMissionController {

    private final DossierRepository dossierRepository;
    private final MissionRepository missionRepository;
    private final PrestationRepository prestationRepository;
    private final PrestataireRepository prestataireRepository;
    private final MissionService missionService;
    private final NotificationService notificationService;
    private final AgentBancaireRepository agentBancaireRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> listeMissions(@PathVariable Long dossierId, Authentication auth) {
        DossierContentieux dossier = dossierRepository.findByIdWithDetails(dossierId).orElse(null);
        if (dossier == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Dossier introuvable"));
        }

        if (!auth.getName().equals(dossier.getCreePar())) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));
        }

        List<Mission> missions = missionRepository.findByDossierIdWithPrestataire(dossierId);

        AgentBancaire agent = agentBancaireRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Agent introuvable"));

        List<Prestataire> prestataires = prestataireRepository.findByAgence_Id(agent.getAgence().getId());
        List<Prestation> prestations = prestationRepository.findByDossier_Id(dossierId);

        String clientNom = dossier.getClient() != null
                ? dossier.getClient().getNom() + " " + (dossier.getClient().getPrenom() != null ? dossier.getClient().getPrenom() : "")
                : "—";

        Map<String, Object> response = new HashMap<>();
        response.put("nbAssignee", missions.stream().filter(m -> m.getStatut() == StatutMission.ASSIGNEE).count());
        response.put("nbEnCours", missions.stream().filter(m -> m.getStatut() == StatutMission.EN_COURS).count());
        response.put("nbPvSoumis", missions.stream().filter(m -> m.getStatut() == StatutMission.PV_SOUMIS).count());
        response.put("nbFactureSoumise", missions.stream().filter(m -> m.getStatut() == StatutMission.FACTURE_SOUMISE).count());
        response.put("nbTerminee", missions.stream().filter(m -> m.getStatut() == StatutMission.TERMINEE).count());
        response.put("nbRetard", missions.stream()
                .filter(m -> m.getDateFinPrevue() != null && m.getDateFinPrevue().isBefore(LocalDate.now()) && m.getStatut() != StatutMission.TERMINEE)
                .count());
        response.put("dossier", dossier);
        response.put("clientNom", clientNom);
        response.put("missions", missions);
        response.put("prestataires", prestataires);
        response.put("prestations", prestations);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/creer")
    public ResponseEntity<?> creerMission(@PathVariable Long dossierId,
                                          @RequestBody Map<String, Object> body,
                                          Authentication auth) {
        try {
            Long prestationId = Long.valueOf(body.get("prestationId").toString());
            Long prestataireId = Long.valueOf(body.get("prestataireId").toString());
            String description = (String) body.get("description");
            String dateFinStr = (String) body.get("dateFinPrevue");
            LocalDate dateFinPrevue = (dateFinStr != null && !dateFinStr.isBlank()) ? LocalDate.parse(dateFinStr) : null;

            DossierContentieux dossier = dossierRepository.findByIdWithDetails(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));

            if (!auth.getName().equals(dossier.getCreePar())) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));
            }

            Prestation prestation = prestationRepository.findById(prestationId)
                .orElseThrow(() -> new RuntimeException("Prestation introuvable"));

            Prestataire prestataire = prestataireRepository.findById(prestataireId)
                .orElseThrow(() -> new RuntimeException("Prestataire introuvable"));

            Mission mission = new Mission();
            mission.setPrestation(prestation);
            mission.setPrestataire(prestataire);
            mission.setDescription(description);
            mission.setDateFinPrevue(dateFinPrevue);
            mission.setDateAssignation(prestation.getDateCreation().toLocalDate());
            mission.setStatut(StatutMission.ASSIGNEE);

            String prefix = "MISS-" + LocalDate.now().getYear();
            long count = missionRepository.count() + 1;
            mission.setNumeroMission(String.format("%s-%05d", prefix, count));

            mission = missionRepository.save(mission);

            String clientNom = dossier.getClient() != null
                ? dossier.getClient().getNom() + " " + (dossier.getClient().getPrenom() != null ? dossier.getClient().getPrenom() : "")
                : "—";

            String echeance = dateFinPrevue != null
                ? dateFinPrevue.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "Non définie";

            String messageNotification = String.format(
                "Nouvelle mission assignée : %s\nDossier : %s\nClient : %s\nLibellé : %s\nType prestation : %s\nDescription : %s\nÉchéance : %s",
                mission.getNumeroMission(), dossier.getNumeroDossier(), clientNom,
                dossier.getLibelle() != null ? dossier.getLibelle() : "—",
                prestation.getType().name(), description, echeance
            );

            notificationService.notifier(
                prestataire.getUsername(),
                "Nouvelle mission — " + mission.getNumeroMission(),
                messageNotification,
                "MISSION",
                dossier
            );

            return ResponseEntity.ok(Map.of(
                "message", "Mission " + mission.getNumeroMission() + " créée avec succès",
                "mission", mission
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{missionId}/statut")
    public ResponseEntity<?> changerStatut(@PathVariable Long dossierId,
                                           @PathVariable Long missionId,
                                           @RequestBody Map<String, String> body) {
        try {
            StatutMission statut = StatutMission.valueOf(body.get("statut"));
            missionService.changerStatut(missionId, statut);
            return ResponseEntity.ok(Map.of("message", "Statut mis à jour avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{missionId}")
    public ResponseEntity<?> supprimer(@PathVariable Long dossierId,
                                       @PathVariable Long missionId,
                                       Authentication auth) {
        try {
            missionService.supprimerMission(missionId, auth.getName());
            return ResponseEntity.ok(Map.of("message", "Mission supprimée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}