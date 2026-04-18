package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.service.PrestationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/huissier")
@RequiredArgsConstructor
public class HuissierController {

    private final PrestationService prestationService;

    @Transactional(readOnly = true)
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('HUISSIER')")
    public String dashboard(Model model, Authentication auth) {

        // ✅ FIX: Removed orphaned getMissionById(missionid) block —
        // 'missionid' was never declared, and a dashboard endpoint should not
        // block on a single mission's lock status. Lock checks belong on
        // individual action endpoints, not here.

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

        model.addAttribute("dernieresMissions",     missions);
        model.addAttribute("missionsActives",       enCours);
        model.addAttribute("rapportsAttente",       pvSoumis);
        model.addAttribute("evaluationsFinalisees", terminees);
        model.addAttribute("contreExpertises",      enRetard);
        model.addAttribute("totalMissions",         missions.size());

        return "huissier/dashboard";
    }
}