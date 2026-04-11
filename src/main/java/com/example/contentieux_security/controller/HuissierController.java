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
import java.time.LocalDate;
import java.util.List;
@Controller
@RequestMapping("/huissier")
@RequiredArgsConstructor
public class HuissierController {

    private final PrestationService prestationService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('HUISSIER')")
    public String dashboard(Model model, Authentication authentication) {

        String username = authentication.getName();
        List<Mission> missions = prestationService.getMissionsPrestataire(username);

        // ── Convertir en DTO pour éviter LazyInitializationException ──
        List<MissionDTO> dtos = missions.stream().map(m -> {
            String numeroDossier = "—";
            String clientNom = "—";
            try {
                if (m.getPrestation() != null && m.getPrestation().getDossier() != null) {
                    numeroDossier = m.getPrestation().getDossier().getNumeroDossier();
                    var client = m.getPrestation().getDossier().getClient();
                    if (client != null) {
                        clientNom = client.getNom() + " " +
                            (client.getPrenom() != null ? client.getPrenom() : "");
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

            return new MissionDTO(
                m.getId(),
                m.getNumeroMission(),
                numeroDossier,
                clientNom,
                m.getDescription(),
                echeance,
                enRetard,
                m.getStatut()
            );
        }).toList();

        long enCours   = missions.stream()
            .filter(m -> m.getStatut() == StatutMission.ASSIGNEE
                      || m.getStatut() == StatutMission.EN_COURS).count();
        long terminees = missions.stream()
            .filter(m -> m.getStatut() == StatutMission.TERMINEE).count();
        long enRetard  = missions.stream()
            .filter(m -> m.getDateFinPrevue() != null
                      && m.getDateFinPrevue().isBefore(LocalDate.now())
                      && m.getStatut() != StatutMission.TERMINEE).count();

        model.addAttribute("dernieresMissions", dtos.stream().limit(5).toList());
        model.addAttribute("mandatsActifs",     enCours);
        model.addAttribute("significations",    missions.size());
        model.addAttribute("saisiesEnCours",    enRetard);
        model.addAttribute("montantRecouvre",   terminees + " terminée(s)");

        return "huissier/dashboard";
    }

    record MissionDTO(Long id, String numeroMission, String numeroDossier,
                      String clientNom, String description, String dateFinPrevue,
                      boolean enRetard, StatutMission statut) {}
}