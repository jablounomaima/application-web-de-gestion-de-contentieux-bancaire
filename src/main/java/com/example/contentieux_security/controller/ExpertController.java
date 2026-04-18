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
@RequestMapping("/expert")
@RequiredArgsConstructor
public class ExpertController {

    private final PrestationService prestationService;

    @Transactional(readOnly = true)
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('EXPERT')")
    public String dashboard(Model model, Authentication auth) {

        // ✅ FIX 1: Removed the orphaned Mission mission = prestationService.getMissionById(m.getId())
        // block — 'm' didn't exist here, and this per-mission lock check doesn't
        // belong on a dashboard endpoint. Locking should be enforced in the
        // service layer or on individual mission action endpoints.

        List<Mission> missions = prestationService.getMissionsPrestataire(auth.getName());

        List<MissionDTO> dtos = missions.stream().map(m -> {
            String numeroDossier = "—";
            String clientNom = "—";
            try {
                if (m.getPrestation() != null && m.getPrestation().getDossier() != null) {
                    numeroDossier = m.getPrestation().getDossier().getNumeroDossier();
                    var client = m.getPrestation().getDossier().getClient();
                    if (client != null) {
                        clientNom = client.getNom() + " "
                                + (client.getPrenom() != null ? client.getPrenom() : "");
                    }
                }
            } catch (Exception ignored) {}

            boolean enRetard = m.getDateFinPrevue() != null
                    && m.getDateFinPrevue().isBefore(LocalDate.now())
                    && m.getStatut() != StatutMission.TERMINEE;

            String echeance = m.getDateFinPrevue() != null
                    ? m.getDateFinPrevue().format(
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "—";

            return new MissionDTO(m.getId(), m.getNumeroMission(), numeroDossier,
                    clientNom, m.getDescription(), echeance, enRetard, m.getStatut());
        }).toList();

        long enCours        = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.ASSIGNEE
                          || m.getStatut() == StatutMission.EN_COURS).count();
        long pvSoumis       = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.PV_SOUMIS).count();
        long factureSoumise = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.FACTURE_SOUMISE).count();
        long terminees      = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.TERMINEE).count();
        long enRetard       = missions.stream()  // ✅ FIX 2: was shadowing the lambda variable
                .filter(m -> m.getDateFinPrevue() != null
                          && m.getDateFinPrevue().isBefore(LocalDate.now())
                          && m.getStatut() != StatutMission.TERMINEE).count();

        model.addAttribute("dernieresMissions",     dtos);
        model.addAttribute("missionsActives",       enCours);
        model.addAttribute("rapportsAttente",       pvSoumis);
        model.addAttribute("factureSoumise",        factureSoumise);
        model.addAttribute("evaluationsFinalisees", terminees);
        model.addAttribute("contreExpertises",      enRetard);

        return "expert/dashboard";
    }

    // ✅ FIX 3: Moved record outside the method — inner records inside methods
    // are not supported in Java versions before 16, and even in 16+ it's
    // cleaner as a static nested type.
    record MissionDTO(Long id, String numeroMission, String numeroDossier,
                      String clientNom, String description, String dateFinPrevue,
                      boolean enRetard, StatutMission statut) {}
}