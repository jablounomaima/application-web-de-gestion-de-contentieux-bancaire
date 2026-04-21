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

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/agent/dossiers")
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
    // ─────────────────────────────────────────────
    //  FORMULAIRE DÉSIGNATION AVOCAT
    // ─────────────────────────────────────────────
    @GetMapping("/{dossierId}/prestation/{prestationId}/designer-avocat")
    public String formulaireDesignerAvocat(@PathVariable Long dossierId,
                                            @PathVariable Long prestationId,
                                            Model model,
                                            Principal principal) {  // ✅ ajouter Principal
    
        // ✅ Récupérer l'agent connecté
        AgentBancaire agent = agentBancaireRepository
                .findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Agent introuvable"));
    
        // ✅ Avocats ajoutés par cet agent uniquement
        List<Prestataire> avocats = prestataireRepository
                .findByTypeAndAgentResponsable_Id(
                    TypePrestataire.AVOCAT,
                    agent.getId()
                );
    
        model.addAttribute("dossierId", dossierId);
        model.addAttribute("prestationId", prestationId);
        model.addAttribute("avocats", avocats);
    
        return "agent/affaires/designer-avocat";
    }

    // ─────────────────────────────────────────────
    //  POST DÉSIGNATION AVOCAT
    // ─────────────────────────────────────────────
    @PostMapping("/{dossierId}/prestation/{prestationId}/designer-avocat")
    public String designerAvocat(@PathVariable Long dossierId,
                                  @PathVariable Long prestationId,
                                  @RequestParam Long prestataireId,
                                  @RequestParam(required = false) String description,
                                  @RequestParam(required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                  LocalDate dateFinPrevue,
                                  Principal principal,
                                  RedirectAttributes ra) {
        try {
            Mission mission = prestationService.designerPrestataire(
                    prestationId,
                    prestataireId,
                    description,
                    dateFinPrevue,
                    principal.getName()
            );

            ra.addFlashAttribute("successMsg",
                    "Avocat désigné avec succès — Mission : "
                    + mission.getNumeroMission());

            return "redirect:/agent/dossiers/" + dossierId + "/affaire/lancer";

        } catch (Exception e) {
            log.error(">>> ERREUR désignation avocat : {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
            return "redirect:/agent/dossiers/" + dossierId
                   + "/prestation/" + prestationId + "/designer-avocat";
        }
    }

    // ─────────────────────────────────────────────
    //  LANCER UNE AFFAIRE (depuis le dossier)
    // ─────────────────────────────────────────────
    @GetMapping("/{dossierId}/affaire/lancer")
    public String formulaireLancerAffaire(@PathVariable Long dossierId,
                                           Model model,
                                           RedirectAttributes ra) {
    
        Mission missionAvocat = missionService.getMissionAvocatDuDossier(dossierId);
    
        if (missionAvocat == null) {
            Prestation prestation = prestationService
                    .getPrestationJudiciaireParDossier(dossierId);
            if (prestation != null) {
                ra.addFlashAttribute("errorMsg",
                        "Désignez d'abord un avocat.");
                return "redirect:/agent/dossiers/" + dossierId
                       + "/prestation/" + prestation.getId() + "/designer-avocat";
            }
            ra.addFlashAttribute("errorMsg", "Aucune procédure judiciaire trouvée.");
            return "redirect:/agent/dossiers/" + dossierId;
        }
    
        // ── Charger le dossier complet ──
        DossierDetailDTO dossier = dossierService.getDossierDetail(dossierId);
    
        model.addAttribute("dossierId", dossierId);
        model.addAttribute("missionId", missionAvocat.getId());
        model.addAttribute("avocat",    missionAvocat.getPrestataire());
        model.addAttribute("dossier",   dossier);
        model.addAttribute("mission",   missionAvocat);
    
        return "agent/affaires/lancer";
    }
    
    @PostMapping("/{dossierId}/affaire/lancer")
    public String lancerAffaire(@PathVariable Long dossierId,
                                 @RequestParam Long missionId, // Khallina ken el missionId khaterha s3ib tna77iha
                                 Principal principal,
                                 RedirectAttributes ra) {
        try {
            // Log n9assna fih el ktiba l-zayda
            log.info(">>> LANCER AFFAIRE - dossierId={} missionId={} agent={}",
                    dossierId, missionId, principal.getName());
    
            // Houni fil service, tna77i el les arguments mte3 el tribunal, role, w chambre
            // Thabbet barka elli l-méthode "creerAffaire" fil service mte3ek t9bel l-format hedha
            AffaireJudiciaire affaire = affaireService.creerAffaire(missionId, principal.getName());
    
            log.info(">>> AFFAIRE CRÉÉE : id={} num={}", 
                    affaire.getId(), affaire.getNumeroAffaire());
    
            ra.addFlashAttribute("successMsg", "Affaire lancée avec succès");
            return "redirect:/agent/dossiers/" + dossierId + "/affaire";
    
        } catch (Exception e) {
            log.error(">>> ERREUR : {}", e.getMessage());
            ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
            return "redirect:/agent/dossiers/" + dossierId + "/affaire/lancer";
        }
    }
    // ─────────────────────────────────────────────
    //  CONSULTER L'AFFAIRE D'UN DOSSIER
    // ─────────────────────────────────────────────
    @GetMapping("/{dossierId}/affaire")
    public String voirAffaire(@PathVariable Long dossierId, Model model) {
        AffaireJudiciaire affaire = affaireService.getAffaireParDossier(dossierId);
        if (affaire == null) {
            model.addAttribute("pasDAffaire", true);
        } else {
            affaire = affaireService.getAffaireById(affaire.getId());
            model.addAttribute("affaire", affaire);
        }
        model.addAttribute("dossierId", dossierId);
        return "agent/affaires/vue2";
    }
}