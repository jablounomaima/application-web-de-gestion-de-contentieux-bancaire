package com.example.contentieux_security.controller;
 
import com.example.contentieux_security.entity.AffaireJudiciaire;
import com.example.contentieux_security.entity.Audience;
import com.example.contentieux_security.entity.Audience.StatutAudience;
import com.example.contentieux_security.enums.StatutMission;
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

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;


import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.service.MissionService;
@Controller
@RequestMapping("/avocat/affaires")
@PreAuthorize("hasAnyRole('AVOCAT')")
@RequiredArgsConstructor
@Slf4j
public class AvocatAffaireController {

    private final AffaireJudiciaireService affaireService;
    private final DossierService dossierService;
private final HistoriqueService historiqueService;
// Ajouter ce champ avec les autres dans AvocatAffaireController
private final AffaireJudiciaireService affaireJudiciaireService;
private final MissionService missionService;  // ← ajouter cette ligne
@GetMapping
public String mesAffaires(Model model, Principal principal) {
    String username = principal.getName();
    log.info("=== LISTE AFFAIRES - username : '{}'", username);

    List<AffaireJudiciaire> affaires =
            affaireService.getAffairesParAvocat(username);
    log.info("=== {} affaires trouvées", affaires.size());

    java.util.Map<Long, String> missionStatuts = new java.util.HashMap<>();
    java.util.Map<Long, Long>   missionIds     = new java.util.HashMap<>();

    for (AffaireJudiciaire affaire : affaires) {
        if (affaire.getMission() != null) {
            try {
                Long affaireId = affaire.getId();
                Long missionId = affaire.getMission().getId();
                String statut  = affaire.getMission().getStatut() != null
                        ? affaire.getMission().getStatut().name()
                        : "INCONNU";

                log.info("  affaireId={} missionId={} statut={}",
                        affaireId, missionId, statut);

                missionStatuts.put(affaireId, statut);
                missionIds.put(affaireId, missionId);

            } catch (Exception e) {
                log.warn("Mission lazy affaire {} : {}",
                        affaire.getId(), e.getMessage());
            }
        } else {
            log.warn("  affaireId={} — pas de mission", affaire.getId());
        }
    }

    model.addAttribute("affaires",       affaires);
    model.addAttribute("missionStatuts", missionStatuts);
    model.addAttribute("missionIds",     missionIds);

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

    // Charger la mission avec JOIN FETCH pour éviter LazyInitializationException
    AffaireJudiciaire affaireAvecMission = affaireJudiciaireService.findById(affaireId);
    Mission mission = affaireAvecMission.getMission();

    model.addAttribute("affaire", affaire);
    model.addAttribute("dossier", dossier);
    model.addAttribute("mission", mission);
    return "avocat/affaires/dossier-detail";
}
// ─────────────────────────────────────────────
//  AUDIENCES — POST (supprimer une audience)
// ─────────────────────────────────────────────
@PostMapping("/{affaireId}/audiences/{audienceId}/supprimer")
public String supprimerAudience(@PathVariable Long affaireId,
                                 @PathVariable Long audienceId,
                                 Principal principal,
                                 RedirectAttributes ra) {
    try {
        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
        if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
            return "redirect:/avocat/affaires";
        }
        affaireService.supprimerAudience(audienceId);
        ra.addFlashAttribute("successMsg", "Audience supprimée avec succès.");
    } catch (Exception e) {
        log.error("Erreur suppression audience {} : {}", audienceId, e.getMessage(), e);
        ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
    }
    return "redirect:/avocat/affaires/" + affaireId + "/audiences";
}

// ─────────────────────────────────────────────
//  AUDIENCES — POST (modifier une audience)
// ─────────────────────────────────────────────
@PostMapping("/{affaireId}/audiences/{audienceId}/modifier")
public String modifierAudience(@PathVariable Long affaireId,
                                @PathVariable Long audienceId,
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
        affaireService.modifierAudienceComplete(audienceId, dateAudience, heure,
                                                 salle, motif, resultat,
                                                 prochaineAudience, statut);
        ra.addFlashAttribute("successMsg", "Audience modifiée avec succès.");
    } catch (Exception e) {
        log.error("Erreur modification audience {} : {}", audienceId, e.getMessage(), e);
        ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
    }
    return "redirect:/avocat/affaires/" + affaireId + "/audiences";
}

// ─────────────────────────────────────────────
//  JUGEMENT — POST (supprimer le jugement)
// ─────────────────────────────────────────────
@PostMapping("/{affaireId}/jugement/supprimer")
public String supprimerJugement(@PathVariable Long affaireId,
                                 Principal principal,
                                 RedirectAttributes ra) {
    try {
        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
        if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
            return "redirect:/avocat/affaires";
        }
        affaireService.supprimerJugement(affaireId);
        ra.addFlashAttribute("successMsg", "Jugement supprimé avec succès.");
    } catch (Exception e) {
        log.error("Erreur suppression jugement affaire {} : {}", affaireId, e.getMessage(), e);
        ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
    }
    return "redirect:/avocat/affaires/" + affaireId + "/jugement";
}

// ─────────────────────────────────────────────
//  TRIBUNAL — POST (supprimer)
// ─────────────────────────────────────────────
@PostMapping("/{affaireId}/tribunal/supprimer")
public String supprimerTribunal(@PathVariable Long affaireId,
                                 Principal principal,
                                 RedirectAttributes ra) {
    try {
        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
        if (affaire == null || !isOwner(affaire, principal.getName())) {
            return "redirect:/avocat/affaires";
        }
        affaireService.modifierTribunal(affaireId, null, null, null);
        ra.addFlashAttribute("successMsg", "Informations du tribunal supprimées.");
    } catch (Exception e) {
        log.error("Erreur suppression tribunal affaire {} : {}", affaireId, e.getMessage(), e);
        ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
    }
    return "redirect:/avocat/affaires/" + affaireId + "/tribunal";
}









@GetMapping("/dashboard")
public String dashboard(Model model, Principal principal) {
    String username = principal.getName();

    List<AffaireJudiciaire> affaires =
            affaireService.getAffairesParAvocat(username);

    // ── Stats missions via affaires ──────────────
    long totalMissions  = affaires.stream()
            .filter(a -> a.getMission() != null).count();
    long enCours        = affaires.stream()
            .filter(a -> a.getMission() != null &&
                    a.getMission().getStatut() == StatutMission.EN_COURS).count();
    long pvSoumis       = affaires.stream()
            .filter(a -> a.getMission() != null &&
                    a.getMission().getStatut() == StatutMission.PV_SOUMIS).count();
    long factureSoumise = affaires.stream()
            .filter(a -> a.getMission() != null &&
                    a.getMission().getStatut() == StatutMission.FACTURE_SOUMISE).count();
    long terminees      = affaires.stream()
            .filter(a -> a.getMission() != null &&
                    a.getMission().getStatut() == StatutMission.TERMINEE).count();
    long annulees       = affaires.stream()
            .filter(a -> a.getMission() != null &&
                    a.getMission().getStatut() == StatutMission.ANNULEE).count();

    double totalHonoraires = affaires.stream()
            .filter(a -> a.getMission() != null &&
                    a.getMission().getMontantFacture() != null)
            .mapToDouble(a -> a.getMission().getMontantFacture())
            .sum();

    // ── Stats affaires ───────────────────────────
    long totalAffaires   = affaires.size();
    long affairesEnCours = affaires.stream()
            .filter(a -> a.getStatut() ==
                    AffaireJudiciaire.StatutAffaire.EN_COURS).count();
    long jugementRendu   = affaires.stream()
            .filter(a -> a.getStatut() ==
                    AffaireJudiciaire.StatutAffaire.JUGEMENT_RENDU).count();
    long audiencesAVenir = affaireService
            .getAudiencesAVenir(username).size();

    // ── Affaires récentes (5 dernières) ──────────
    List<AffaireJudiciaire> affairesRecentes = affaires.stream()
            .limit(5)
            .toList();

    model.addAttribute("totalMissions",    totalMissions);
    model.addAttribute("enCours",          enCours);
    model.addAttribute("pvSoumis",         pvSoumis);
    model.addAttribute("factureSoumise",   factureSoumise);
    model.addAttribute("terminees",        terminees);
    model.addAttribute("annulees",         annulees);
    model.addAttribute("totalHonoraires",  totalHonoraires);
    model.addAttribute("totalAffaires",    totalAffaires);
    model.addAttribute("affairesEnCours",  affairesEnCours);
    model.addAttribute("jugementRendu",    jugementRendu);
    model.addAttribute("audiencesAVenir",  audiencesAVenir);
    model.addAttribute("affairesRecentes", affairesRecentes);

    return "avocat/dashboard";
}













// =========================================================
//  📄 PV — GET formulaire
// =========================================================
// =========================================================
//  📄 PV — GET formulaire
// =========================================================
@GetMapping("/{affaireId}/missions/{missionId}/pv")
@PreAuthorize("hasAnyRole('AVOCAT')")
public String voirPV(@PathVariable Long affaireId,
                     @PathVariable Long missionId,
                     Model model) {
    AffaireJudiciaire affaire = affaireJudiciaireService.findById(affaireId);
    Mission mission = affaire.getMission(); // déjà chargée avec JOIN FETCH
    model.addAttribute("affaire", affaire);
    model.addAttribute("mission", mission);
    return "avocat/missions/pv";
}

// =========================================================
//  📄 PV — POST soumettre
// =========================================================
@PostMapping("/{affaireId}/missions/{missionId}/pv")
@PreAuthorize("hasAnyRole('AVOCAT','EXPERT','HUISSIER')")
public String soumettrePV(@PathVariable Long affaireId,
                           @PathVariable Long missionId,
                           @RequestParam String pvTexte,
                           RedirectAttributes ra) {
    try {
        affaireJudiciaireService.soumettreAvocatPV(affaireId, pvTexte);
        ra.addFlashAttribute("successMsg", "PV soumis avec succès.");
    } catch (Exception e) {
        ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
    }
    return "redirect:/avocat/affaires/" + affaireId + "/missions/" + missionId + "/pv";
}
// =========================================================
//  🧾 FACTURE — GET formulaire
// =========================================================
@GetMapping("/{affaireId}/missions/{missionId}/facture")
@PreAuthorize("hasAnyRole('AVOCAT')")
public String voirFacture(@PathVariable Long affaireId,
                           @PathVariable Long missionId,
                           Model model) {
    AffaireJudiciaire affaire = affaireJudiciaireService.findById(affaireId);
    Mission mission = affaire.getMission();
    model.addAttribute("affaire", affaire);
    model.addAttribute("mission", mission);
    return "avocat/missions/facture";
}
// =========================================================
//  🧾 FACTURE — POST soumettre
// =========================================================
@PostMapping("/{affaireId}/missions/{missionId}/facture")
@PreAuthorize("hasAnyRole('AVOCAT')")
public String soumettreFacture(@PathVariable Long affaireId,
                                @PathVariable Long missionId,
                                @RequestParam String factureRef,
                                @RequestParam Double montantFacture,
                                RedirectAttributes ra) {
    try {
        affaireJudiciaireService.soumettreAvocatFacture(affaireId, factureRef, montantFacture);
        ra.addFlashAttribute("successMsg", "Facture soumise avec succès.");
    } catch (Exception e) {
        ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
    }
    return "redirect:/avocat/affaires/" + affaireId + "/missions/" + missionId + "/facture";
}





// =========================================================
//  📄 PV — DELETE supprimer
// =========================================================
@PostMapping("/{affaireId}/missions/{missionId}/pv/supprimer")
@PreAuthorize("hasAnyRole('AVOCAT')")
public String supprimerPV(@PathVariable Long affaireId,
                           @PathVariable Long missionId,
                           RedirectAttributes ra) {
    try {
        affaireJudiciaireService.supprimerAvocatPV(affaireId);
        ra.addFlashAttribute("successMsg", "PV supprimé avec succès.");
    } catch (Exception e) {
        ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
    }
    return "redirect:/avocat/affaires/" + affaireId + "/missions/" + missionId + "/pv";
}

// =========================================================
//  📄 PV — PUT modifier
// =========================================================
@PostMapping("/{affaireId}/missions/{missionId}/pv/modifier")
@PreAuthorize("hasAnyRole('AVOCAT')")
public String modifierPV(@PathVariable Long affaireId,
                          @PathVariable Long missionId,
                          @RequestParam String pvTexte,
                          RedirectAttributes ra) {
    try {
        affaireJudiciaireService.modifierAvocatPV(affaireId, pvTexte);
        ra.addFlashAttribute("successMsg", "PV modifié avec succès.");
    } catch (Exception e) {
        ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
    }
    return "redirect:/avocat/affaires/" + affaireId + "/missions/" + missionId + "/pv";
}

// =========================================================
//  🧾 FACTURE — DELETE supprimer
// =========================================================
@PostMapping("/{affaireId}/missions/{missionId}/facture/supprimer")
@PreAuthorize("hasAnyRole('AVOCAT')")
public String supprimerFacture(@PathVariable Long affaireId,
                                @PathVariable Long missionId,
                                RedirectAttributes ra) {
    try {
        affaireJudiciaireService.supprimerAvocatFacture(affaireId);
        ra.addFlashAttribute("successMsg", "Facture supprimée avec succès.");
    } catch (Exception e) {
        ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
    }
    return "redirect:/avocat/affaires/" + affaireId + "/missions/" + missionId + "/facture";
}

// =========================================================
//  🧾 FACTURE — PUT modifier
// =========================================================
@PostMapping("/{affaireId}/missions/{missionId}/facture/modifier")
@PreAuthorize("hasAnyRole('AVOCAT')")
public String modifierFacture(@PathVariable Long affaireId,
                               @PathVariable Long missionId,
                               @RequestParam String factureRef,
                               @RequestParam Double montantFacture,
                               RedirectAttributes ra) {
    try {
        affaireJudiciaireService.modifierAvocatFacture(affaireId, factureRef, montantFacture);
        ra.addFlashAttribute("successMsg", "Facture modifiée avec succès.");
    } catch (Exception e) {
        ra.addFlashAttribute("errorMsg", "Erreur : " + e.getMessage());
    }
    return "redirect:/avocat/affaires/" + affaireId + "/missions/" + missionId + "/facture";
}




}