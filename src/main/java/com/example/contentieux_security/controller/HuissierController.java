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
@RequestMapping("/api/huissier")
@RequiredArgsConstructor
public class HuissierController {

    private final PrestationService prestationService;

    @Transactional(readOnly = true)
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('HUISSIER')")
    public ResponseEntity<?> dashboard(Authentication auth) {

        List<Mission> missions = prestationService.getMissionsPrestataire(auth.getName());

        long enCours = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.ASSIGNEE
                          || m.getStatut() == StatutMission.EN_COURS)
                .count();

        long pvSoumis = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.PV_SOUMIS)
                .count();

        long terminees = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.TERMINEE)
                .count();

        long enRetard = missions.stream()
                .filter(m -> m.getDateFinPrevue() != null
                          && m.getDateFinPrevue().isBefore(LocalDate.now())
                          && m.getStatut() != StatutMission.TERMINEE)
                .count();

        Map<String, Object> response = new HashMap<>();
        response.put("dernieresMissions", missions);
        response.put("missionsActives", enCours);
        response.put("rapportsAttente", pvSoumis);
        response.put("evaluationsFinalisees", terminees);
        response.put("contreExpertises", enRetard);
        response.put("totalMissions", missions.size());

        return ResponseEntity.ok(response);
    }
}