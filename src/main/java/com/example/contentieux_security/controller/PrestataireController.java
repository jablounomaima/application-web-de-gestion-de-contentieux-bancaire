package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.StatutMission;
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
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
@Controller
@RequestMapping("/prestataire")
@RequiredArgsConstructor
@Slf4j
public class PrestataireController {

    private final PrestationService      prestationService;
    private final FichierResultatService fichierResultatService;
    private final DossierService dossierService;
    @Value("${app.upload.dir:uploads/resultats}")
    private String uploadDir;

    // =========================================================
    //  📊 DASHBOARD
    // =========================================================

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('AVOCAT','EXPERT','PRESTATAIRE')")
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