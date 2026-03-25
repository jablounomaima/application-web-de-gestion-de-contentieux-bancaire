package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.TypePrestataire;  // ← enums/
import com.example.contentieux_security.enums.TypePrestation;   // ← enums/
import com.example.contentieux_security.repository.PrestataireRepository;
import com.example.contentieux_security.service.PrestationService;
import com.example.contentieux_security.service.DossierService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PrestationController {

    private final PrestationService     prestationService;
    private final DossierService        dossierService;
    private final PrestataireRepository prestataireRepository;

    // ── Formulaire lancement prestation ──────────────────
    @GetMapping("/agent/dossiers/{dossierId}/prestations/lancer")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String formLancer(@PathVariable Long dossierId, Model model) {
        DossierContentieux dossier = dossierService.getDossierById(dossierId);
        model.addAttribute("dossier", dossier);
        model.addAttribute("typesPrestations", TypePrestation.values());
        return "agent/prestations/lancer";
    }

    // ── POST : lancer la prestation ───────────────────────
    @PostMapping("/agent/dossiers/{dossierId}/prestations/lancer")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String lancerPrestation(@PathVariable Long dossierId,
                                   @RequestParam TypePrestation type,
                                   @RequestParam(required = false) String description,
                                   Principal principal,
                                   RedirectAttributes ra) {
        try {
            Prestation p = prestationService.lancerPrestation(
                    dossierId, type, description, principal.getName());
            ra.addFlashAttribute("success",
                    "Prestation " + p.getNumeroPrestation() + " lancée avec succès.");
            return "redirect:/agent/dossiers/" + dossierId + "/prestations/" + p.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/dossiers/" + dossierId;
        }
    }

    // ── Page détail prestation ────────────────────────────
    @GetMapping("/agent/dossiers/{dossierId}/prestations/{prestationId}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String detailPrestation(@PathVariable Long dossierId,
                                   @PathVariable Long prestationId,
                                   Model model) {
        Prestation prestation = prestationService.getPrestationById(prestationId);
        List<Mission> missions = prestationService.getMissionsByPrestation(prestationId);

        List<Prestataire> prestataires;
        if (prestation.getType() == TypePrestation.PROCEDURE_JUDICIAIRE) {
            prestataires = prestataireRepository.findByTypeAndActifTrue(TypePrestataire.AVOCAT);
        } else {
            prestataires = prestataireRepository.findByTypeInAndActifTrue(
                    List.of(TypePrestataire.EXPERT, TypePrestataire.HUISSIER));
        }

        model.addAttribute("prestation", prestation);
        model.addAttribute("missions", missions);
        model.addAttribute("prestataires", prestataires);
        model.addAttribute("dossierId", dossierId);
        return "agent/prestations/detail";
    }

    // ── POST : désigner un prestataire ────────────────────
    @PostMapping("/agent/dossiers/{dossierId}/prestations/{prestationId}/designer")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public String designerPrestataire(@PathVariable Long dossierId,
                                      @PathVariable Long prestationId,
                                      @RequestParam Long prestataireId,
                                      @RequestParam(required = false) String description,
                                      @RequestParam(required = false) String dateFinPrevue,
                                      Principal principal,
                                      RedirectAttributes ra) {
        try {
            LocalDate dateFin = (dateFinPrevue != null && !dateFinPrevue.isBlank())
                    ? LocalDate.parse(dateFinPrevue) : null;
            Mission m = prestationService.designerPrestataire(
                    prestationId, prestataireId, description, dateFin, principal.getName());
            ra.addFlashAttribute("success",
                    "Mission " + m.getNumeroMission() + " assignée avec succès.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/dossiers/" + dossierId + "/prestations/" + prestationId;
    }

    // ── Mes missions (prestataire) ────────────────────────
    @GetMapping("/prestataire/missions")
    @PreAuthorize("hasAnyRole('AVOCAT','EXPERT','HUISSIER')")
    public String mesMissions(Model model, Principal principal) {
        model.addAttribute("missions",
                prestationService.getMissionsPrestataire(principal.getName()));
        return "prestataire/missions/liste";
    }

    @GetMapping("/prestataire/missions/{id}")
    @PreAuthorize("hasAnyRole('AVOCAT','EXPERT','HUISSIER')")
    public String detailMission(@PathVariable Long id, Model model) {
        model.addAttribute("mission", prestationService.getMissionById(id));
        return "prestataire/missions/detail";
    }

    // ── POST : soumettre PV ───────────────────────────────
    @PostMapping("/prestataire/missions/{id}/pv")
    @PreAuthorize("hasAnyRole('AVOCAT','EXPERT','HUISSIER')")
    public String soumettrePV(@PathVariable Long id,
                               @RequestParam String pvTexte,
                               Principal principal,
                               RedirectAttributes ra) {
        try {
            prestationService.soumettrePV(id, pvTexte, principal.getName());
            ra.addFlashAttribute("success", "PV soumis avec succès.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/prestataire/missions/" + id;
    }

    // ── POST : soumettre facture ──────────────────────────
    @PostMapping("/prestataire/missions/{id}/facture")
    @PreAuthorize("hasAnyRole('AVOCAT','EXPERT','HUISSIER')")
    public String soumettreFacture(@PathVariable Long id,
                                    @RequestParam Double montant,
                                    @RequestParam String factureRef,
                                    Principal principal,
                                    RedirectAttributes ra) {
        try {
            prestationService.soumettreFacture(id, montant, factureRef, principal.getName());
            ra.addFlashAttribute("success", "Facture soumise avec succès.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/prestataire/missions/" + id;
    }
}
