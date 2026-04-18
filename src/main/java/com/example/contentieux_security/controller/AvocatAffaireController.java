package com.example.contentieux_security.controller;
 
import com.example.contentieux_security.entity.AffaireJudiciaire;
import com.example.contentieux_security.entity.Audience;
import com.example.contentieux_security.entity.Audience.StatutAudience;
import com.example.contentieux_security.service.AffaireJudiciaireService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
 // Ajouter ces imports
import com.example.contentieux_security.dto.DossierDetailDTO;
import com.example.contentieux_security.service.DossierService;
import com.example.contentieux_security.service.HistoriqueService;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
@Controller
@RequestMapping("/avocat/affaires")
@PreAuthorize("hasAnyRole('AVOCAT')")
@RequiredArgsConstructor
@Slf4j
public class AvocatAffaireController {

    private final AffaireJudiciaireService affaireService;
    private final DossierService dossierService;
private final HistoriqueService historiqueService;
    @GetMapping
    public String mesAffaires(Model model, Principal principal) {
        String username = principal.getName();
        List<AffaireJudiciaire> affaires = affaireService.getAffairesParAvocat(username);
        model.addAttribute("affaires", affaires);
        return "avocat/affaires/liste";
    }
    @GetMapping("/{affaireId}")
    public String detailAffaire(@PathVariable Long affaireId,
                                Model model,
                                Principal principal) {
        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);

        if (affaire == null) {
            return "redirect:/avocat/affaires";
        }

        // ✅ Utiliser affaire.avocat (champ direct) — jamais mission.prestataire (LAZY)
        String avocatUsername = affaire.getAvocat() != null
                ? affaire.getAvocat().getUsername()
                : null;

        if (avocatUsername == null || !principal.getName().equals(avocatUsername)) {
            log.warn("Accès refusé : {} tente d'accéder à l'affaire {}",
                    principal.getName(), affaireId);
            return "redirect:/avocat/affaires";
        }

        model.addAttribute("affaire", affaire);
        return "avocat/affaires/detail";
    }








     // ─────────────────────────────────────────────
    //  AUDIENCES — GET (liste + formulaire ajout)
    // ─────────────────────────────────────────────
 
    @GetMapping("/{affaireId}/audiences")
    public String gererAudiences(@PathVariable Long affaireId,
                                 Model model,
                                 Principal principal) {
        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
 
        if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
            return "redirect:/avocat/affaires";
        }
 
        model.addAttribute("affaire", affaire);
        model.addAttribute("audiences", affaire.getAudiences());
        model.addAttribute("statutsAudience", Audience.StatutAudience.values());
        return "avocat/affaires/audiences";
    }
 
    // ─────────────────────────────────────────────
    //  AUDIENCES — POST (ajouter une audience)
    // ─────────────────────────────────────────────
 
    @PostMapping("/{affaireId}/audiences")
    public String ajouterAudience(@PathVariable Long affaireId,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateAudience,
                                  @RequestParam(required = false) String heure,
                                  @RequestParam(required = false) String salle,
                                  @RequestParam(required = false) String motif,
                                  @RequestParam(required = false) String resultat,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prochaineAudience,
                                  @RequestParam Audience.StatutAudience statut,
                                  Principal principal,
                                  RedirectAttributes ra) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
 
            if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
                return "redirect:/avocat/affaires";
            }
 
            affaireService.ajouterAudience(
                    affaireId, dateAudience, heure, salle, motif, resultat, prochaineAudience, statut
            );
            ra.addFlashAttribute("successMsg", "Audience ajoutée avec succès.");
        } catch (Exception e) {
            log.error("Erreur ajout audience affaire {} : {}", affaireId, e.getMessage(), e);
            ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
        }
        return "redirect:/avocat/affaires/" + affaireId + "/audiences";
    }
 
    // ─────────────────────────────────────────────
    //  AUDIENCES — POST (modifier le statut d'une audience)
    // ─────────────────────────────────────────────
 
    @PostMapping("/{affaireId}/audiences/{audienceId}/statut")
    public String modifierStatutAudience(@PathVariable Long affaireId,
                                         @PathVariable Long audienceId,
                                         @RequestParam Audience.StatutAudience statut,
                                         @RequestParam(required = false) String resultat,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prochaineAudience,
                                         Principal principal,
                                         RedirectAttributes ra) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
 
            if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
                return "redirect:/avocat/affaires";
            }
 
            affaireService.modifierStatutAudience(audienceId, statut, resultat, prochaineAudience);
            ra.addFlashAttribute("successMsg", "Audience mise à jour.");
        } catch (Exception e) {
            log.error("Erreur modification audience {} : {}", audienceId, e.getMessage(), e);
            ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
        }
        return "redirect:/avocat/affaires/" + affaireId + "/audiences";
    }
 
    // ─────────────────────────────────────────────
    //  JUGEMENT — GET (formulaire saisie)
    // ─────────────────────────────────────────────
 
    @GetMapping("/{affaireId}/jugement")
    public String formulaireJugement(@PathVariable Long affaireId,
                                     Model model,
                                     Principal principal) {
        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
 
        if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
            return "redirect:/avocat/affaires";
        }
 
        model.addAttribute("affaire", affaire);
        model.addAttribute("typesJugement", AffaireJudiciaire.TypeJugement.values());
        return "avocat/affaires/jugement";
    }
 
    // ─────────────────────────────────────────────
    //  JUGEMENT — POST (enregistrer le jugement)
    // ─────────────────────────────────────────────
 
    @PostMapping("/{affaireId}/jugement")
    public String saisirJugement(@PathVariable Long affaireId,
                                 @RequestParam AffaireJudiciaire.TypeJugement typeJugement,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateJugement,
                                 @RequestParam(required = false) String montantJuge,
                                 @RequestParam(required = false) String delaiPaiementJuge,
                                 @RequestParam(required = false) String descriptionJugement,
                                 Principal principal,
                                 RedirectAttributes ra) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
 
            if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
                return "redirect:/avocat/affaires";
            }
 
            affaireService.enregistrerJugement(
                    affaireId, typeJugement, dateJugement,
                    montantJuge, delaiPaiementJuge, descriptionJugement
            );
            ra.addFlashAttribute("successMsg", "Jugement enregistré avec succès.");
        } catch (Exception e) {
            log.error("Erreur enregistrement jugement affaire {} : {}", affaireId, e.getMessage(), e);
            ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
        }
        return "redirect:/avocat/affaires/" + affaireId;
    }
 
    // ─────────────────────────────────────────────
    //  UTILITAIRE PRIVÉ
    // ─────────────────────────────────────────────
 
    private boolean isAvocatOwner(AffaireJudiciaire affaire, String username) {
        return affaire.getAvocat() != null
                && username.equals(affaire.getAvocat().getUsername());
    }



    // ══════════════════════════════════════════════════════════════════
//  À AJOUTER dans AvocatAffaireController
//  — avant la méthode privée isOwner()
// ══════════════════════════════════════════════════════════════════

    // ─────────────────────────────────────────────
    //  TRIBUNAL — GET (formulaire)
    // ─────────────────────────────────────────────

    @GetMapping("/{affaireId}/tribunal")
    public String formulaireTribunal(@PathVariable Long affaireId,
                                     Model model,
                                     Principal principal) {
        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);

        if (affaire == null || !isOwner(affaire, principal.getName())) {
            return "redirect:/avocat/affaires";
        }

        model.addAttribute("affaire", affaire);
        return "avocat/affaires/tribunal";
    }

    // ─────────────────────────────────────────────
    //  TRIBUNAL — POST (enregistrer)
    // ─────────────────────────────────────────────

    @PostMapping("/{affaireId}/tribunal")
    public String modifierTribunal(
            @PathVariable Long affaireId,
            @RequestParam(required = false) String tribunal,
            @RequestParam(required = false) String chambre,
            @RequestParam(required = false) String numeroRole,
            Principal principal,
            RedirectAttributes ra) {

        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);

        if (affaire == null || !isOwner(affaire, principal.getName())) {
            return "redirect:/avocat/affaires";
        }

        try {
            affaireService.modifierTribunal(affaireId, tribunal, chambre, numeroRole);
            ra.addFlashAttribute("successMsg", "Informations du tribunal mises à jour.");
        } catch (Exception e) {
            log.error("Erreur modification tribunal affaire {} : {}", affaireId, e.getMessage(), e);
            ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
        }
        return "redirect:/avocat/affaires/" + affaireId + "/tribunal";
    }


    private boolean isOwner(AffaireJudiciaire affaire, String username) {
        return affaire.getAvocat() != null
                && username.equals(affaire.getAvocat().getUsername());
    }






// ─────────────────────────────────────────────
//  DOSSIER — GET (détails complets) afficher les details de chaque dossier a l avocat assignee
// ─────────────────────────────────────────────
@GetMapping("/{affaireId}/dossier")
public String voirDossier(@PathVariable Long affaireId,
                           Model model,
                           Principal principal) {

    AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);

    if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
        return "redirect:/avocat/affaires";
    }

    if (affaire.getDossier() == null) {
        return "redirect:/avocat/affaires/" + affaireId;
    }

    Long dossierId = affaire.getDossier().getId();
    DossierDetailDTO dossier = dossierService.getDossierDetail(dossierId);

    model.addAttribute("affaire", affaire);
    model.addAttribute("dossier", dossier);
    return "avocat/affaires/dossier-detail";
}
}