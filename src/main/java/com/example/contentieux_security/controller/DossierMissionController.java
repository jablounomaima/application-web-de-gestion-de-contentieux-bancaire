package com.example.contentieux_security.controller;

import com.example.contentieux_security.config.NotificationAdvice;
import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.repository.*;
import com.example.contentieux_security.service.MissionService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import com.example.contentieux_security.repository.AgentBancaireRepository;
import com.example.contentieux_security.entity.AgentBancaire;
import java.time.LocalDate;
import java.util.List;
@PreAuthorize("hasRole('AGENT')")
@Controller
@RequestMapping("/agent/dossiers/{dossierId}/missions")
@RequiredArgsConstructor
public class DossierMissionController {

    private final DossierRepository dossierRepository;
    private final MissionRepository missionRepository;
    private final PrestationRepository prestationRepository;
    private final PrestataireRepository prestataireRepository;
    private final MissionService missionService;
    private final NotificationAdvice notificationService; // ← ajouter
    private final AgentBancaireRepository agentBancaireRepository; // ✅ AJOUTER ICI

    @PreAuthorize("hasRole('AGENT')")
    @GetMapping
    @Transactional(readOnly = true)
    public String listeMissions(@PathVariable Long dossierId,
                                Model model,
                                Authentication auth,  // ✅ Authentication existe déjà
                                RedirectAttributes ra) {
    
        DossierContentieux dossier = dossierRepository.findByIdWithDetails(dossierId).orElse(null);
        if (dossier == null) {
            ra.addFlashAttribute("error", "Dossier introuvable");
            return "redirect:/agent/dossiers";
        }
    
        if (!auth.getName().equals(dossier.getCreePar())) {
            ra.addFlashAttribute("error", "Accès non autorisé");
            return "redirect:/agent/dossiers";
        }
    
        List<Mission> missions = missionRepository.findByDossierIdWithPrestataire(dossierId);
        missions.forEach(m -> {
            try {
                if (m.getPrestataire() != null) {
                    m.getPrestataire().getNom();
                    m.getPrestataire().getPrenom();
                    m.getPrestataire().getUsername();
                    m.getPrestataire().getType();
                }
                if (m.getPrestation() != null) {
                    m.getPrestation().getNumeroPrestation();
                    m.getPrestation().getType();
                    if (m.getPrestation().getDossier() != null) {
                        m.getPrestation().getDossier().getNumeroDossier();
                    }
                }
            } catch (Exception ignored) {}
        });
    
        // Statistiques (inchangées)
        model.addAttribute("nbAssignee", missions.stream()
                .filter(m -> m.getStatut() == StatutMission.ASSIGNEE).count());
        model.addAttribute("nbEnCours", missions.stream()
                .filter(m -> m.getStatut() == StatutMission.EN_COURS).count());
        model.addAttribute("nbPvSoumis", missions.stream()
                .filter(m -> m.getStatut() == StatutMission.PV_SOUMIS).count());
        model.addAttribute("nbFactureSoumise", missions.stream()
                .filter(m -> m.getStatut() == StatutMission.FACTURE_SOUMISE).count());
        model.addAttribute("nbTerminee", missions.stream()
                .filter(m -> m.getStatut() == StatutMission.TERMINEE).count());
        model.addAttribute("nbRetard", missions.stream()
                .filter(m -> m.getDateFinPrevue() != null
                          && m.getDateFinPrevue().isBefore(LocalDate.now())
                          && m.getStatut() != StatutMission.TERMINEE).count());
    
        // ✅ Récupérer l'agence de l'agent connecté
        AgentBancaire agent = agentBancaireRepository
                .findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Agent introuvable"));
    
        // ✅ Prestataires filtrés par agence (au lieu de findAll())
        List<Prestataire> prestataires = prestataireRepository
                .findByAgence_Id(agent.getAgence().getId());
    
        List<Prestation> prestations = prestationRepository.findByDossier_Id(dossierId);
    
        String clientNom = dossier.getClient() != null
                ? dossier.getClient().getNom() + " "
                  + (dossier.getClient().getPrenom() != null
                     ? dossier.getClient().getPrenom() : "")
                : "—";
    
        model.addAttribute("dossier",      dossier);
        model.addAttribute("clientNom",    clientNom);
        model.addAttribute("missions",     missions);
        model.addAttribute("prestataires", prestataires);
        model.addAttribute("prestations",  prestations);
    
        return "agent/dossiers/missions";
    }
   
    @PreAuthorize("hasRole('AGENT')") 
    @PostMapping("/creer")
public String creerMission(@PathVariable Long dossierId,
                            @RequestParam Long prestationId,
                            @RequestParam Long prestataireId,
                            @RequestParam String description,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate dateFinPrevue,
                            Authentication auth,
                            RedirectAttributes ra) {
    try {
        DossierContentieux dossier = dossierRepository.findByIdWithDetails(dossierId)
            .orElseThrow(() -> new RuntimeException("Dossier introuvable"));

        if (!auth.getName().equals(dossier.getCreePar())) {
            ra.addFlashAttribute("error", "Accès non autorisé");
            return "redirect:/agent/dossiers/" + dossierId + "/missions";
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
        mission.setDateAssignation(prestation.getDateCreation().toLocalDate());        mission.setStatut(StatutMission.ASSIGNEE);

        // Numéro mission basé sur l'année
        String prefix = "MISS-" + LocalDate.now().getYear();
        long count = missionRepository.count() + 1;
        mission.setNumeroMission(String.format("%s-%05d", prefix, count));

        mission = missionRepository.save(mission);

        // ── Notification complète au prestataire ──────────────────────────
        String clientNom = dossier.getClient() != null
            ? dossier.getClient().getNom() + " " +
              (dossier.getClient().getPrenom() != null ? dossier.getClient().getPrenom() : "")
            : "—";

        String echeance = dateFinPrevue != null
            ? dateFinPrevue.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            : "Non définie";

        String messageNotification = String.format(
            "Nouvelle mission assignée : %s\n" +
            "Dossier : %s\n" +
            "Client : %s\n" +
            "Libellé : %s\n" +
            "Type prestation : %s\n" +
            "Description : %s\n" +
            "Échéance : %s",
            mission.getNumeroMission(),
            dossier.getNumeroDossier(),
            clientNom,
            dossier.getLibelle() != null ? dossier.getLibelle() : "—",
            prestation.getType().name(),
            description,
            echeance
        );

        notificationService.notifier(
            prestataire.getUsername(),
            "Nouvelle mission — " + mission.getNumeroMission(),
            messageNotification,
            "MISSION",
            dossier
        );

        ra.addFlashAttribute("success",
            "Mission " + mission.getNumeroMission() + " créée et notifiée à "
            + prestataire.getNom() + " " + prestataire.getPrenom());

    } catch (Exception e) {
        ra.addFlashAttribute("error", e.getMessage());
    }

    return "redirect:/agent/dossiers/" + dossierId + "/missions";
}
    
    
    
    @PostMapping("/{missionId}/statut")
    public String changerStatut(@PathVariable Long dossierId,
                                @PathVariable Long missionId,
                                @RequestParam StatutMission statut,
                                Authentication auth,
                                RedirectAttributes ra) {
        try {
            missionService.changerStatut(missionId, statut);
            ra.addFlashAttribute("success", "Statut mis à jour");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/dossiers/" + dossierId + "/missions";
    }

    @PostMapping("/{missionId}/supprimer")
    public String supprimer(@PathVariable Long dossierId,
                            @PathVariable Long missionId,
                            Authentication auth,
                            RedirectAttributes ra) {
        try {
            missionService.supprimerMission(missionId, auth.getName());
            ra.addFlashAttribute("success", "Mission supprimée");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/dossiers/" + dossierId + "/missions";
    }
}