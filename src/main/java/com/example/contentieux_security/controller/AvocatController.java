package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.service.*;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/avocat")
@RequiredArgsConstructor
@Slf4j

public class AvocatController {

    private final PrestationService prestationService;
    private final AffaireJudiciaireService affaireService;
    private final FileStorageService fileStorageService;
    private final ResultatMissionService resultatMissionService;
    private final FactureService factureService;
    private final NotificationService notificationService;

    // ============================
    // 📋 LISTE AFFAIRES
    // ============================
    @GetMapping("/affaires")
    @PreAuthorize("hasRole('AVOCAT')")
    public String mesAffaires(Principal principal, Model model) {

        List<AffaireJudiciaire> affaires =
                affaireService.getAffairesParAvocat(principal.getName());

        long total = affaires.size();

        long enCours = affaires.stream()
        .filter(a -> "EN_COURS".equals(a.getStatut().name()))

                .count();

        long jugementRendu = affaires.stream()
                .filter(a -> "JUGEMENT_RENDU".equals(a.getStatut().name()))
                .count();

        long cloturees = affaires.stream()
                .filter(a -> "CLOSE".equals(a.getStatut().name())
                        || "EXECUTION_FORCEE".equals(a.getStatut().name()))
                .count();

        model.addAttribute("affaires", affaires);
        model.addAttribute("total", total);
        model.addAttribute("enCours", enCours);
        model.addAttribute("jugementRendu", jugementRendu);
        model.addAttribute("cloturees", cloturees);

        return "avocat/affaires/liste";
    }

    // ============================
    // 📄 DETAIL AFFAIRE
    // ============================
    @GetMapping("/affaires/{id}")
    @PreAuthorize("hasRole('AVOCAT')")
    public String detailAffaire(@PathVariable Long id, Model model) {

        AffaireJudiciaire affaire = affaireService.getAffaireById(id);

        if (affaire == null) {
            return "redirect:/avocat/affaires?error=Affaire introuvable";
        }

        model.addAttribute("affaire", affaire);

        model.addAttribute("typesDocument",
                List.of("ACTE", "CONCLUSION", "JUGEMENT", "AUTRE"));

        model.addAttribute("statutsAudience",
                Arrays.stream(Audience.StatutAudience.values())
                        .map(Enum::name)
                        .collect(Collectors.toList()));

        model.addAttribute("typesJugement",
                List.of("CONTRADICTOIRE", "REPUTE_CONTRA", "AFFAIRE_EN_DELIBERE"));

        return "avocat/affaires/detail";
    }

    // ============================
    // ⚖️ AJOUT AUDIENCE
    // ============================
    @PostMapping("/affaires/{id}/audiences/ajouter")
    @PreAuthorize("hasRole('AVOCAT')")
    public String ajouterAudience(@PathVariable Long id,
                                 @RequestParam String dateAudience,
                                 @RequestParam(required = false) String heure,
                                 @RequestParam(required = false) String salle,
                                 @RequestParam(required = false) String motif,
                                 RedirectAttributes ra) {

        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(id);

            affaireService.ajouterAudience(
                    id,
                    LocalDate.parse(dateAudience),
                    heure,
                    salle,
                    motif
            );

            String agentUsername = affaire.getDossier().getAgentCreateur().getUsername();

            notificationService.notifierNouvelleAudience(
                    id, dateAudience, affaire.getTribunal(), agentUsername
            );

            ra.addFlashAttribute("successMsg", "Audience ajoutée.");

        } catch (Exception e) {
            log.error("Erreur ajout audience", e);
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/avocat/affaires/" + id;
    }

    // ============================
    // ⚖️ RESULTAT AUDIENCE
    // ============================
    @PostMapping("/affaires/{affaireId}/audiences/{audienceId}/resultat")
    @PreAuthorize("hasRole('AVOCAT')")
    public String enregistrerResultatAudience(
            @PathVariable Long affaireId,
            @PathVariable Long audienceId,
            @RequestParam String resultat,
            @RequestParam String statut,
            @RequestParam(required = false) String prochaineAudience,
            RedirectAttributes ra) {

        try {
            LocalDate prochaine = (prochaineAudience != null && !prochaineAudience.isBlank())
                    ? LocalDate.parse(prochaineAudience)
                    : null;

            affaireService.enregistrerResultatAudience(
                    audienceId,
                    resultat,
                    Audience.StatutAudience.valueOf(statut),
                    prochaine
            );

            ra.addFlashAttribute("successMsg", "Résultat enregistré.");

        } catch (Exception e) {
            log.error("Erreur résultat audience", e);
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/avocat/affaires/" + affaireId;
    }

    // ============================
    // 📜 JUGEMENT
    // ============================
    @PostMapping("/affaires/{id}/jugement")
    @PreAuthorize("hasRole('AVOCAT')")
    public String enregistrerJugement(
            @PathVariable Long id,
            @RequestParam String typeJugement,
            @RequestParam String dateJugement,
            @RequestParam(required = false) String montantJuge,
            @RequestParam(required = false) String delaiPaiement,
            @RequestParam(required = false) String description,
            RedirectAttributes ra) {

        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(id);

            affaireService.enregistrerJugement(
                    id,
                    typeJugement,
                    LocalDate.parse(dateJugement),
                    montantJuge,
                    delaiPaiement,
                    description
            );

            String agentUsername = affaire.getDossier().getAgentCreateur().getUsername();

            notificationService.notifierJugementRendu(
                    id, typeJugement, montantJuge, agentUsername
            );

            ra.addFlashAttribute("successMsg", "Jugement enregistré.");

        } catch (Exception e) {
            log.error("Erreur jugement", e);
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/avocat/affaires/" + id;
    }

    // ============================
    // 💰 FACTURE
    // ============================
    @PostMapping("/affaires/{affaireId}/missions/{missionId}/facture")
    @PreAuthorize("hasRole('AVOCAT')")
    public String soumettreFacture(
            @PathVariable Long affaireId,
            @PathVariable Long missionId,
            @RequestParam BigDecimal montantHT,
            @RequestParam BigDecimal tauxTva,
            @RequestParam(required = false) String numeroFacture,
            @RequestParam(required = false) String dateFacture,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile fichier,
            Principal principal,
            RedirectAttributes ra) {

        try {
            LocalDate date = (dateFacture != null && !dateFacture.isBlank())
                    ? LocalDate.parse(dateFacture)
                    : LocalDate.now();

            factureService.soumettreFacture(
                    missionId, montantHT, tauxTva,
                    numeroFacture, date, description,
                    fichier, principal.getName()
            );

            prestationService.changerStatutMission(
                    missionId, StatutMission.FACTURE_SOUMISE
            );

            ra.addFlashAttribute("successMsg", "Facture envoyée.");

        } catch (Exception e) {
            log.error("Erreur facture", e);
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/avocat/affaires/" + affaireId;
    }

    // ============================
    // 📊 DASHBOARD
    // ============================
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('AVOCAT')")
    public String dashboard(Model model, Authentication auth) {
    
        List<Mission> missions = prestationService.getMissionsPrestataire(auth.getName());
        log.info(">>> USERNAME: {} | MISSIONS COUNT: {}", auth.getName(), missions.size());    
        long enCours = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.EN_COURS
                          || m.getStatut() == StatutMission.ASSIGNEE)
                .count();
    
        long pvSoumis = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.PV_SOUMIS)
                .count();
    
        long factureSoumise = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.FACTURE_SOUMISE)
                .count();
    
        long terminees = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.TERMINEE
                          || m.getStatut() == StatutMission.REALISEE)
                .count();
    
        model.addAttribute("missions",          missions);
        model.addAttribute("totalMissions",     missions.size());
        model.addAttribute("missionsEnCours",   enCours);
        model.addAttribute("pvSoumis",          pvSoumis);
        model.addAttribute("factureSoumise",    factureSoumise);
        model.addAttribute("missionsTerminees", terminees);
        model.addAttribute("dernieresMissions", missions); // ← ICI

    
        return "avocat/dashboard";
    }
}