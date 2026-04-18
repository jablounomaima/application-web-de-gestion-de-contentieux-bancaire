package com.example.contentieux_security.controller;


import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.service.AffaireJudiciaireService;
import com.example.contentieux_security.service.MissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.contentieux_security.entity.Mission;
import java.security.Principal;

@Controller
@RequestMapping("/agent/dossiers")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AgentAffaireController {

    private final AffaireJudiciaireService affaireService;
    private final MissionService missionService;


    // ─────────────────────────────────────────────
    //  LANCER UNE AFFAIRE (depuis le dossier)
    // ─────────────────────────────────────────────

    @GetMapping("/{dossierId}/affaire/lancer")
    public String formulaireLancerAffaire(@PathVariable Long dossierId, Model model) {
        System.out.println(">>> GET /agent/dossiers/" + dossierId + "/affaire/lancer");
        
        Mission missionAvocat = missionService.getMissionAvocatDuDossier(dossierId);
        if (missionAvocat == null) {
            System.out.println(">>> Pas de mission avocat trouvée");
            model.addAttribute("errorMsg",
                "Aucune mission avocat trouvée. Désignez d'abord un avocat.");
            return "redirect:/agent/dossiers/" + dossierId;
        }
        
        model.addAttribute("dossierId", dossierId);
        model.addAttribute("missionId", missionAvocat.getId());
        model.addAttribute("avocat", missionAvocat.getPrestataire());
        
        System.out.println(">>> Redirection vers formulaire");
        return "agent/affaires/lancer";
    }
    @PostMapping("/{dossierId}/affaire/lancer")
    public String lancerAffaire(@PathVariable Long dossierId,
                                 @RequestParam Long missionId,
                                 @RequestParam(required = false) String tribunal,
                                 @RequestParam(required = false) String numeroRole,
                                 @RequestParam(required = false) String chambre,
                                 Principal principal,
                                 RedirectAttributes ra) {
        try {
            System.out.println(">>> LANCER AFFAIRE - dossierId=" + dossierId + " missionId=" + missionId);
            AffaireJudiciaire affaire = affaireService.creerAffaire(
                missionId, tribunal, numeroRole, chambre, principal.getName()
            );
            System.out.println(">>> AFFAIRE CREEE : id=" + affaire.getId() + " num=" + affaire.getNumeroAffaire());
            ra.addFlashAttribute("successMsg",
                "Affaire " + affaire.getNumeroAffaire() + " lancée avec succès");
        } catch (Exception e) {
            System.out.println(">>> ERREUR LANCEMENT : " + e.getClass().getName() + " : " + e.getMessage());
            e.printStackTrace(); // stack trace complète
            ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
        }
        return "redirect:/agent/dossiers/" + dossierId + "/affaire/lancer";    }
    // ─────────────────────────────────────────────
    //  CONSULTER L'AFFAIRE D'UN DOSSIER
    // ─────────────────────────────────────────────

    @GetMapping("/{dossierId}/affaire")
    public String voirAffaire(@PathVariable Long dossierId, Model model) {
        AffaireJudiciaire affaire = affaireService.getAffaireParDossier(dossierId);
        if (affaire == null) {
            model.addAttribute("pasDAffaire", true);
        } else {
            // Recharger avec tous les détails
            affaire = affaireService.getAffaireById(affaire.getId());
            model.addAttribute("affaire", affaire);
        }
        model.addAttribute("dossierId", dossierId);
        return "agent/affaires/vue2";
    }








    
}