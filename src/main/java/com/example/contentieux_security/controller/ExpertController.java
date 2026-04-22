package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.service.PrestationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expert")
@RequiredArgsConstructor
public class ExpertController {

    private final PrestationService prestationService;

    @Transactional(readOnly = true)
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<?> dashboard(Authentication auth) {

        List<Mission> missions = prestationService.getMissionsPrestataire(auth.getName());

        List<MissionDTO> dtos = missions.stream().map(m -> {
            String numeroDossier = "—";
            String clientNom = "—";
            try {
                if (m.getPrestation() != null && m.getPrestation().getDossier() != null) {
                    numeroDossier = m.getPrestation().getDossier().getNumeroDossier();
                    var client = m.getPrestation().getDossier().getClient();
                    if (client != null) {
                        clientNom = client.getNom() + " " + (client.getPrenom() != null ? client.getPrenom() : "");
                    }
                }
            } catch (Exception ignored) {}

            boolean enRetard = m.getDateFinPrevue() != null
                    && m.getDateFinPrevue().isBefore(LocalDate.now())
                    && m.getStatut() != StatutMission.TERMINEE;

            String echeance = m.getDateFinPrevue() != null
                    ? m.getDateFinPrevue().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "—";

            return new MissionDTO(m.getId(), m.getNumeroMission(), numeroDossier,
                    clientNom, m.getDescription(), echeance, enRetard, m.getStatut());
        }).toList();

        long enCours = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.ASSIGNEE || m.getStatut() == StatutMission.EN_COURS).count();
        long pvSoumis = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.PV_SOUMIS).count();
        long factureSoumise = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.FACTURE_SOUMISE).count();
        long terminees = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.TERMINEE).count();
        long enRetard = missions.stream()
                .filter(m -> m.getDateFinPrevue() != null
                          && m.getDateFinPrevue().isBefore(LocalDate.now())
                          && m.getStatut() != StatutMission.TERMINEE).count();

        Map<String, Object> response = new HashMap<>();
        response.put("dernieresMissions", dtos);
        response.put("missionsActives", enCours);
        response.put("rapportsAttente", pvSoumis);
        response.put("factureSoumise", factureSoumise);
        response.put("evaluationsFinalisees", terminees);
        response.put("contreExpertises", enRetard);

        return ResponseEntity.ok(response);
    }

    record MissionDTO(Long id, String numeroMission, String numeroDossier,
                      String clientNom, String description, String dateFinPrevue,
                      boolean enRetard, StatutMission statut) {}
}