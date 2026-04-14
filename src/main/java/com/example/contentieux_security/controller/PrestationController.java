package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.TypePrestataire;
import com.example.contentieux_security.enums.TypePrestation;
import com.example.contentieux_security.repository.PrestataireRepository;
import com.example.contentieux_security.service.PrestationService;
import com.example.contentieux_security.service.DossierService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor // Injection automatique des dépendances via constructeur
public class PrestationController {

    // Services utilisés pour la logique métier
    private final PrestationService prestationService;
    private final DossierService dossierService;

    // Repository pour accéder aux prestataires
    private final PrestataireRepository prestataireRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // 🔹 GET : Afficher le formulaire de lancement d’une prestation
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/agent/dossiers/{dossierId}/prestations/lancer")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')") // Sécurité : seuls AGENT et ADMIN
    
    public String formLancer(@PathVariable Long dossierId, Model model) {

        // Récupération du dossier
        DossierContentieux dossier = dossierService.getDossierById(dossierId);

        // Vérification : le dossier doit être VALIDÉ
        if (!"VALIDE".equals(dossier.getStatut().name())) {
            return "redirect:/agent/dossiers/" + dossierId
                    + "?erreur=Le dossier doit être au statut VALIDE";
        }

        // Récupérer la liste des avocats actifs
        List<Prestataire> avocats =
                prestataireRepository.findByTypeAndActifTrue(TypePrestataire.AVOCAT);

        // Ajouter les données au modèle pour Thymeleaf
        model.addAttribute("dossier", dossier);
        model.addAttribute("typesPrestations", TypePrestation.values());
        model.addAttribute("avocats", avocats);

        // Retourner la vue
        return "agent/prestations/lancer";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 🔹 POST : Lancer une prestation
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/agent/dossiers/{dossierId}/prestations/lancer")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public String lancerPrestation(@PathVariable Long dossierId,
                                  @RequestParam TypePrestation type,
                                  @RequestParam(required = false) String description,
                                  Authentication authentication,
                                  RedirectAttributes ra) {
        try {
            // Appel au service métier
            Prestation p = prestationService.lancerPrestation(
                dossierId, type, description, authentication.getName());
            // Message succès
            ra.addFlashAttribute("success",
                    "Prestation " + p.getNumeroPrestation() + " lancée avec succès.");

            // Redirection vers détail prestation
            return "redirect:/agent/dossiers/" + dossierId + "/prestations/" + p.getId();

        } catch (IllegalStateException e) {
            // Cas métier (ex: dossier non valide)
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/dossiers/" + dossierId + "/prestations/lancer";

        } catch (Exception e) {
            // Erreur générale
            ra.addFlashAttribute("error", "Erreur inattendue : " + e.getMessage());
            return "redirect:/agent/dossiers/" + dossierId;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 🔹 GET : Afficher le détail d’une prestation
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/agent/dossiers/{dossierId}/prestations/{prestationId}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public String detailPrestation(@PathVariable Long dossierId,
                                  @PathVariable Long prestationId,
                                  Model model) {

        // Récupération des données
        DossierContentieux dossier = dossierService.getDossierById(dossierId);
        Prestation prestation = prestationService.getPrestationById(prestationId);
        List<Mission> missions = prestationService.getMissionsByPrestation(prestationId);

        // Vérification : la prestation appartient bien au dossier
        if (!prestation.getDossier().getId().equals(dossierId)) {
            return "redirect:/agent/dossiers/" + dossierId
                    + "?erreur=Prestation non liée à ce dossier";
        }

        // Sélection des prestataires selon le type de prestation
        List<Prestataire> prestataires;

        prestataires = prestataireRepository.findByTypeInAndActifTrue(
            List.of(TypePrestataire.AVOCAT, TypePrestataire.EXPERT, TypePrestataire.HUISSIER)
        );

        // Ajouter les données à la vue
        model.addAttribute("dossier", dossier);
        model.addAttribute("prestation", prestation);
        model.addAttribute("missions", missions);
        model.addAttribute("prestataires", prestataires);

        return "agent/prestations/detail";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 🔹 POST : Désigner un prestataire (création mission)
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/agent/dossiers/{dossierId}/prestations/{prestationId}/designer")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    
    public String designerPrestataire(@PathVariable Long dossierId,
                                      @PathVariable Long prestationId,
                                      @RequestParam Long prestataireId,
                                      @RequestParam(required = false) String description,
                                      @RequestParam(required = false) String dateFinPrevue,
                                      Authentication authentication,
                                      RedirectAttributes ra) {
        try {
            // Conversion String → LocalDate (sécurisée)
            LocalDate dateFin = (dateFinPrevue != null && !dateFinPrevue.isBlank())
                    ? LocalDate.parse(dateFinPrevue)
                    : null;

            // Appel service pour créer la mission
            Mission m = prestationService.designerPrestataire(
                    prestationId,
                    prestataireId,
                    description,
                    dateFin,
                    authentication.getName()
            );

            // Message succès
            ra.addFlashAttribute("success",
                    "Mission " + m.getNumeroMission() + " assignée avec succès.");

        } catch (IllegalArgumentException e) {
            // Erreur validation
            ra.addFlashAttribute("error", "Données invalides : " + e.getMessage());

        } catch (Exception e) {
            // Erreur générale
            ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }

        // Redirection vers la page détail
        return "redirect:/agent/dossiers/" + dossierId + "/prestations/" + prestationId;
    }

    // ✅ Les routes /prestataire/** sont gérées ailleurs (MissionController)
}