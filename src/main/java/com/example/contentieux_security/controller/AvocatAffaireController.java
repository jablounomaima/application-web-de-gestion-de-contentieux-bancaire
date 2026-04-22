package com.example.contentieux_security.controller;
 
import com.example.contentieux_security.entity.AffaireJudiciaire;
import com.example.contentieux_security.entity.Audience;
import com.example.contentieux_security.entity.Audience.StatutAudience;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.service.AffaireJudiciaireService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.contentieux_security.dto.DossierDetailDTO;
import com.example.contentieux_security.service.DossierService;
import com.example.contentieux_security.service.HistoriqueService;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.service.MissionService;

@RestController
@RequestMapping("/api/avocat/affaires")
@PreAuthorize("hasAnyRole('AVOCAT')")
@RequiredArgsConstructor
@Slf4j
public class AvocatAffaireController {

    private final AffaireJudiciaireService affaireService;
    private final DossierService dossierService;
    private final HistoriqueService historiqueService;
    private final AffaireJudiciaireService affaireJudiciaireService;
    private final MissionService missionService;

    @GetMapping
    public ResponseEntity<?> mesAffaires(Principal principal) {
        String username = principal.getName();
        List<AffaireJudiciaire> affaires = affaireService.getAffairesParAvocat(username);
        Map<Long, String> missionStatuts = new HashMap<>();
        Map<Long, Long> missionIds = new HashMap<>();

        for (AffaireJudiciaire affaire : affaires) {
            if (affaire.getMission() != null) {
                try {
                    Long affaireId = affaire.getId();
                    Long missionId = affaire.getMission().getId();
                    String statut = affaire.getMission().getStatut() != null ? affaire.getMission().getStatut().name() : "INCONNU";
                    missionStatuts.put(affaireId, statut);
                    missionIds.put(affaireId, missionId);
                } catch (Exception e) {
                    log.warn("Mission lazy affaire {} : {}", affaire.getId(), e.getMessage());
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("affaires", affaires);
        response.put("missionStatuts", missionStatuts);
        response.put("missionIds", missionIds);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{affaireId}")
    public ResponseEntity<?> detailAffaire(@PathVariable Long affaireId, Principal principal) {
        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
        if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        }
        return ResponseEntity.ok(affaire);
    }

    @GetMapping("/{affaireId}/audiences")
    public ResponseEntity<?> gererAudiences(@PathVariable Long affaireId, Principal principal) {
        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
        if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        }
        return ResponseEntity.ok(Map.of("audiences", affaire.getAudiences(), "statutsAudience", Audience.StatutAudience.values()));
    }

    @PostMapping("/{affaireId}/audiences")
    public ResponseEntity<?> ajouterAudience(@PathVariable Long affaireId, @RequestBody Map<String, Object> body, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }
            LocalDate dateAudience = LocalDate.parse(body.get("dateAudience").toString());
            String heure = (String) body.get("heure");
            String salle = (String) body.get("salle");
            String motif = (String) body.get("motif");
            String resultat = (String) body.get("resultat");
            LocalDate prochaineAudience = body.get("prochaineAudience") != null ? LocalDate.parse(body.get("prochaineAudience").toString()) : null;
            Audience.StatutAudience statut = Audience.StatutAudience.valueOf((String) body.get("statut"));

            affaireService.ajouterAudience(affaireId, dateAudience, heure, salle, motif, resultat, prochaineAudience, statut);
            return ResponseEntity.ok(Map.of("message", "Audience ajoutée avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{affaireId}/audiences/{audienceId}/statut")
    public ResponseEntity<?> modifierStatutAudience(@PathVariable Long affaireId, @PathVariable Long audienceId, @RequestBody Map<String, Object> body, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }
            Audience.StatutAudience statut = Audience.StatutAudience.valueOf((String) body.get("statut"));
            String resultat = (String) body.get("resultat");
            LocalDate prochaineAudience = body.get("prochaineAudience") != null ? LocalDate.parse(body.get("prochaineAudience").toString()) : null;
            
            affaireService.modifierStatutAudience(audienceId, statut, resultat, prochaineAudience);
            return ResponseEntity.ok(Map.of("message", "Audience mise à jour."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{affaireId}/jugement")
    public ResponseEntity<?> saisirJugement(@PathVariable Long affaireId, @RequestBody Map<String, Object> body, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }
            AffaireJudiciaire.TypeJugement typeJugement = AffaireJudiciaire.TypeJugement.valueOf((String) body.get("typeJugement"));
            LocalDate dateJugement = LocalDate.parse(body.get("dateJugement").toString());
            String montantJuge = (String) body.get("montantJuge");
            String delaiPaiementJuge = (String) body.get("delaiPaiementJuge");
            String descriptionJugement = (String) body.get("descriptionJugement");

            affaireService.enregistrerJugement(affaireId, typeJugement, dateJugement, montantJuge, delaiPaiementJuge, descriptionJugement);
            return ResponseEntity.ok(Map.of("message", "Jugement enregistré avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{affaireId}/tribunal")
    public ResponseEntity<?> modifierTribunal(@PathVariable Long affaireId, @RequestBody Map<String, String> body, Principal principal) {
        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
        if (affaire == null || !isOwner(affaire, principal.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        }
        try {
            affaireService.modifierTribunal(affaireId, body.get("tribunal"), body.get("chambre"), body.get("numeroRole"));
            return ResponseEntity.ok(Map.of("message", "Informations du tribunal mises à jour."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private boolean isAvocatOwner(AffaireJudiciaire affaire, String username) {
        return affaire.getAvocat() != null && username.equals(affaire.getAvocat().getUsername());
    }

    private boolean isOwner(AffaireJudiciaire affaire, String username) {
        return isAvocatOwner(affaire, username);
    }

    @GetMapping("/{affaireId}/dossier")
    public ResponseEntity<?> voirDossier(@PathVariable Long affaireId, Principal principal) {
        AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
        if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        }
        if (affaire.getDossier() == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Dossier introuvable"));
        }
        Long dossierId = affaire.getDossier().getId();
        DossierDetailDTO dossier = dossierService.getDossierDetail(dossierId);
        AffaireJudiciaire affaireAvecMission = affaireJudiciaireService.findById(affaireId);
        Mission mission = affaireAvecMission.getMission();

        return ResponseEntity.ok(Map.of("affaire", affaire, "dossier", dossier, "mission", mission));
    }

    @DeleteMapping("/{affaireId}/audiences/{audienceId}")
    public ResponseEntity<?> supprimerAudience(@PathVariable Long affaireId, @PathVariable Long audienceId, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }
            affaireService.supprimerAudience(audienceId);
            return ResponseEntity.ok(Map.of("message", "Audience supprimée avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{affaireId}/audiences/{audienceId}")
    public ResponseEntity<?> modifierAudience(@PathVariable Long affaireId, @PathVariable Long audienceId, @RequestBody Map<String, Object> body, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }
            LocalDate dateAudience = LocalDate.parse(body.get("dateAudience").toString());
            String heure = (String) body.get("heure");
            String salle = (String) body.get("salle");
            String motif = (String) body.get("motif");
            String resultat = (String) body.get("resultat");
            LocalDate prochaineAudience = body.get("prochaineAudience") != null ? LocalDate.parse(body.get("prochaineAudience").toString()) : null;
            Audience.StatutAudience statut = Audience.StatutAudience.valueOf((String) body.get("statut"));

            affaireService.modifierAudienceComplete(audienceId, dateAudience, heure, salle, motif, resultat, prochaineAudience, statut);
            return ResponseEntity.ok(Map.of("message", "Audience modifiée avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{affaireId}/jugement")
    public ResponseEntity<?> supprimerJugement(@PathVariable Long affaireId, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isAvocatOwner(affaire, principal.getName())) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }
            affaireService.supprimerJugement(affaireId);
            return ResponseEntity.ok(Map.of("message", "Jugement supprimé avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{affaireId}/tribunal")
    public ResponseEntity<?> supprimerTribunal(@PathVariable Long affaireId, Principal principal) {
        try {
            AffaireJudiciaire affaire = affaireService.getAffaireById(affaireId);
            if (affaire == null || !isOwner(affaire, principal.getName())) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }
            affaireService.modifierTribunal(affaireId, null, null, null);
            return ResponseEntity.ok(Map.of("message", "Informations du tribunal supprimées."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Principal principal) {
        String username = principal.getName();
        List<AffaireJudiciaire> affaires = affaireService.getAffairesParAvocat(username);

        long totalMissions = affaires.stream().filter(a -> a.getMission() != null).count();
        long enCours = affaires.stream().filter(a -> a.getMission() != null && a.getMission().getStatut() == StatutMission.EN_COURS).count();
        long pvSoumis = affaires.stream().filter(a -> a.getMission() != null && a.getMission().getStatut() == StatutMission.PV_SOUMIS).count();
        long factureSoumise = affaires.stream().filter(a -> a.getMission() != null && a.getMission().getStatut() == StatutMission.FACTURE_SOUMISE).count();
        long terminees = affaires.stream().filter(a -> a.getMission() != null && a.getMission().getStatut() == StatutMission.TERMINEE).count();
        long annulees = affaires.stream().filter(a -> a.getMission() != null && a.getMission().getStatut() == StatutMission.ANNULEE).count();

        double totalHonoraires = affaires.stream()
                .filter(a -> a.getMission() != null && a.getMission().getMontantFacture() != null)
                .mapToDouble(a -> a.getMission().getMontantFacture())
                .sum();

        long totalAffaires = affaires.size();
        long affairesEnCours = affaires.stream().filter(a -> a.getStatut() == AffaireJudiciaire.StatutAffaire.EN_COURS).count();
        long jugementRendu = affaires.stream().filter(a -> a.getStatut() == AffaireJudiciaire.StatutAffaire.JUGEMENT_RENDU).count();
        long audiencesAVenir = affaireService.getAudiencesAVenir(username).size();

        List<AffaireJudiciaire> affairesRecentes = affaires.stream().limit(5).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("totalMissions", totalMissions);
        response.put("enCours", enCours);
        response.put("pvSoumis", pvSoumis);
        response.put("factureSoumise", factureSoumise);
        response.put("terminees", terminees);
        response.put("annulees", annulees);
        response.put("totalHonoraires", totalHonoraires);
        response.put("totalAffaires", totalAffaires);
        response.put("affairesEnCours", affairesEnCours);
        response.put("jugementRendu", jugementRendu);
        response.put("audiencesAVenir", audiencesAVenir);
        response.put("affairesRecentes", affairesRecentes);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{affaireId}/missions/{missionId}/pv")
    @PreAuthorize("hasAnyRole('AVOCAT')")
    public ResponseEntity<?> voirPV(@PathVariable Long affaireId, @PathVariable Long missionId) {
        AffaireJudiciaire affaire = affaireJudiciaireService.findById(affaireId);
        return ResponseEntity.ok(Map.of("affaire", affaire, "mission", affaire.getMission()));
    }

    @PostMapping("/{affaireId}/missions/{missionId}/pv")
    @PreAuthorize("hasAnyRole('AVOCAT','EXPERT','HUISSIER')")
    public ResponseEntity<?> soumettrePV(@PathVariable Long affaireId, @PathVariable Long missionId, @RequestBody Map<String, String> body) {
        try {
            affaireJudiciaireService.soumettreAvocatPV(affaireId, body.get("pvTexte"));
            return ResponseEntity.ok(Map.of("message", "PV soumis avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{affaireId}/missions/{missionId}/facture")
    @PreAuthorize("hasAnyRole('AVOCAT')")
    public ResponseEntity<?> voirFacture(@PathVariable Long affaireId, @PathVariable Long missionId) {
        AffaireJudiciaire affaire = affaireJudiciaireService.findById(affaireId);
        return ResponseEntity.ok(Map.of("affaire", affaire, "mission", affaire.getMission()));
    }

    @PostMapping("/{affaireId}/missions/{missionId}/facture")
    @PreAuthorize("hasAnyRole('AVOCAT')")
    public ResponseEntity<?> soumettreFacture(@PathVariable Long affaireId, @PathVariable Long missionId, @RequestBody Map<String, Object> body) {
        try {
            affaireJudiciaireService.soumettreAvocatFacture(affaireId, (String) body.get("factureRef"), Double.valueOf(body.get("montantFacture").toString()));
            return ResponseEntity.ok(Map.of("message", "Facture soumise avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{affaireId}/missions/{missionId}/pv")
    @PreAuthorize("hasAnyRole('AVOCAT')")
    public ResponseEntity<?> supprimerPV(@PathVariable Long affaireId, @PathVariable Long missionId) {
        try {
            affaireJudiciaireService.supprimerAvocatPV(affaireId);
            return ResponseEntity.ok(Map.of("message", "PV supprimé avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{affaireId}/missions/{missionId}/pv")
    @PreAuthorize("hasAnyRole('AVOCAT')")
    public ResponseEntity<?> modifierPV(@PathVariable Long affaireId, @PathVariable Long missionId, @RequestBody Map<String, String> body) {
        try {
            affaireJudiciaireService.modifierAvocatPV(affaireId, body.get("pvTexte"));
            return ResponseEntity.ok(Map.of("message", "PV modifié avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{affaireId}/missions/{missionId}/facture")
    @PreAuthorize("hasAnyRole('AVOCAT')")
    public ResponseEntity<?> supprimerFacture(@PathVariable Long affaireId, @PathVariable Long missionId) {
        try {
            affaireJudiciaireService.supprimerAvocatFacture(affaireId);
            return ResponseEntity.ok(Map.of("message", "Facture supprimée avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{affaireId}/missions/{missionId}/facture")
    @PreAuthorize("hasAnyRole('AVOCAT')")
    public ResponseEntity<?> modifierFacture(@PathVariable Long affaireId, @PathVariable Long missionId, @RequestBody Map<String, Object> body) {
        try {
            affaireJudiciaireService.modifierAvocatFacture(affaireId, (String) body.get("factureRef"), Double.valueOf(body.get("montantFacture").toString()));
            return ResponseEntity.ok(Map.of("message", "Facture modifiée avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}