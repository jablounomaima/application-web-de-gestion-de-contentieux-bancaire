package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.repository.MissionRepository;
import com.example.contentieux_security.service.*;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Controller
@RequestMapping("/prestataire")
@RequiredArgsConstructor
@Slf4j
public class PrestataireController {

    private final PrestationService      prestationService;
    private final FichierResultatService fichierResultatService;
    private final DossierService         dossierService;
    private final MissionService         missionService;  // ← ajouter
    private final MissionRepository missionRepository;
    @Value("${app.upload.dir:uploads/resultats}")
    private String uploadDir;

    // =========================================================
    //  📊 DASHBOARD
    // =========================================================
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('AVOCAT','EXPERT','PRESTATAIRE','HUISSIER')")
    public String dashboard(Model model, Authentication auth) {
        List<Mission> missions = prestationService.getMissionsPrestataire(auth.getName());

        long enCours        = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.ASSIGNEE
                          || m.getStatut() == StatutMission.EN_COURS).count();
        long pvSoumis       = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.PV_SOUMIS).count();
        long factureSoumise = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.FACTURE_SOUMISE).count();
        long terminees      = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.TERMINEE
                          || m.getStatut() == StatutMission.REALISEE).count();

        model.addAttribute("dernieresMissions", missions);
        model.addAttribute("totalMissions",     missions.size());
        model.addAttribute("missionsEnCours",   enCours);
        model.addAttribute("pvSoumis",          pvSoumis);
        model.addAttribute("factureSoumise",    factureSoumise);
        model.addAttribute("missionsTerminees", terminees);

        return "prestataire/dashboard";
    }

  
// =========================================================
//  📄 PV — GET formulaire (prestataire uniquement)
// =========================================================
@GetMapping("/missions/{missionId}/pv")
@PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
public String formulairePV(@PathVariable Long missionId,
                            Model model,
                            Principal principal) {

    Mission mission = missionService.getMissionWithDetails(missionId);

    if (mission.getPrestataire() == null ||
        !mission.getPrestataire().getUsername().equals(principal.getName())) {
        return "redirect:/prestataire/missions?error=acces-refuse";
    }

    model.addAttribute("mission", mission);
    return "avocat/missions/pv";
}

// =========================================================
//  📄 PV — POST soumettre (prestataire uniquement)
// =========================================================
// =========================================================
//  🧾 FACTURE — GET formulaire (prestataire uniquement)
// =========================================================
@GetMapping("/missions/{missionId}/facture")
@PreAuthorize("hasAnyRole('HUISSIER','EXPERT','PRESTATAIRE')")
public String formulaireFacture(@PathVariable Long missionId,
                                 Model model,
                                 Principal principal) {

    Mission mission = missionService.getMissionWithDetails(missionId);

    if (mission.getPrestataire() == null ||
        !mission.getPrestataire().getUsername().equals(principal.getName())) {
        return "redirect:/prestataire/missions";
    }

    model.addAttribute("mission", mission);
    return "avocat/missions/facture";
}

// ✅ Corriger dans PrestataireController
@PostMapping("/missions/{id}/pv")  // ← ajouter /missions/
public String soumettrePV(@PathVariable Long id,
                           @RequestParam String pvTexte,
                           Authentication authentication,
                           RedirectAttributes ra) {
    try {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mission introuvable"));

        if (mission.getStatut() != StatutMission.ASSIGNEE
                && mission.getStatut() != StatutMission.EN_COURS) {
            ra.addFlashAttribute("error", "PV déjà soumis ou mission non active.");
            return "redirect:/prestataire/missions/" + id;
        }

          // ✅ CORRECTION : Accepter ASSIGNEE, EN_COURS et REJETEE
          StatutMission statut = mission.getStatut();
          if (statut != StatutMission.ASSIGNEE
                  && statut != StatutMission.EN_COURS
                  && statut != StatutMission.REJETEE) {  // ← AJOUTÉ
              ra.addFlashAttribute("error", "PV déjà soumis ou mission non active.");
              return "redirect:/prestataire/missions/" + id;
          }

        mission.setPvMission(pvTexte);
        mission.setStatut(StatutMission.PV_SOUMIS);
        mission.setDateValidationPv(java.time.LocalDateTime.now());
        missionRepository.save(mission);

        ra.addFlashAttribute("success", "PV soumis avec succès.");
    } catch (Exception e) {
        log.error("Erreur soumission PV mission {} : {}", id, e.getMessage());
        ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
    }
    return "redirect:/prestataire/missions/" + id;
}

// ✅ Corriger dans PrestataireController
@PostMapping("/missions/{id}/facture")  // ← ajouter /missions/
public String soumettreFacture(@PathVariable Long id,
                                @RequestParam String factureRef,
                                @RequestParam Double montant,
                                Authentication authentication,
                                RedirectAttributes ra) {
    try {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mission introuvable"));

        if (mission.getStatut() != StatutMission.PV_SOUMIS && mission.getStatut() != StatutMission.REJETEE) {
            ra.addFlashAttribute("error", "Soumettez d'abord le PV.");
            return "redirect:/prestataire/missions/" + id;
        }

        mission.setFactureRef(factureRef);
        mission.setMontantFacture(montant);
        mission.setStatut(StatutMission.FACTURE_SOUMISE);
        mission.setDateValidationFacture(java.time.LocalDateTime.now());
        missionRepository.save(mission);

        ra.addFlashAttribute("success", "Facture soumise avec succès.");
    } catch (Exception e) {
        log.error("Erreur soumission facture mission {} : {}", id, e.getMessage());
        ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
    }
    return "redirect:/prestataire/missions/" + id;
}

// =========================================================
    //  ⬇️ TÉLÉCHARGER FICHIER PAR ID
    // =========================================================
    @GetMapping("/missions/fichier/id/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> telechargerFichierById(@PathVariable Long id)
            throws IOException {
        FichierResultat fichier = fichierResultatService.findById(id);
        if (fichier == null) return ResponseEntity.notFound().build();

        Path chemin = Paths.get(uploadDir, fichier.getNomFichierServeur());
        byte[] contenu = Files.readAllBytes(chemin);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fichier.getTypeMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fichier.getNomFichierOriginal() + "\"")
                .body(contenu);
    }

    // =========================================================
    //  ⬇️ TÉLÉCHARGER FICHIER PAR NOM
    // =========================================================
    @GetMapping("/missions/fichier/{nomFichierServeur}")
    @ResponseBody
    public ResponseEntity<byte[]> telechargerFichier(
            @PathVariable String nomFichierServeur) throws IOException {
        FichierResultat fichier =
                fichierResultatService.findByNomFichierServeur(nomFichierServeur);
        if (fichier == null) return ResponseEntity.notFound().build();

        Path chemin = Paths.get(uploadDir, nomFichierServeur);
        byte[] contenu = Files.readAllBytes(chemin);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fichier.getTypeMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fichier.getNomFichierOriginal() + "\"")
                .body(contenu);
    }


    
}