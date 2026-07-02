package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.DossierCreationRequest;
import com.example.contentieux_security.dto.DossierDetailDTO;
import com.example.contentieux_security.dto.RisqueAjoutRequest;
import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.entity.Garantie;
import com.example.contentieux_security.enums.TypeValidateur;
import com.example.contentieux_security.repository.ClientRepository;
import com.example.contentieux_security.repository.DossierRepository;
import com.example.contentieux_security.repository.GarantieRepository;
import com.example.contentieux_security.repository.RisqueRepository;
import com.example.contentieux_security.repository.ValidateurRepository;
import com.example.contentieux_security.service.DossierService;
import com.example.contentieux_security.service.HistoriqueService;
import com.example.contentieux_security.service.NotificationService;
import com.example.contentieux_security.service.RisqueService;
import com.example.contentieux_security.entity.Client;
import com.example.contentieux_security.entity.Risque;
import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.entity.Prestation;
import com.example.contentieux_security.service.MissionService;
import com.example.contentieux_security.service.PrestationService;
import com.example.contentieux_security.repository.AffaireJudiciaireRepository;
import com.example.contentieux_security.repository.AgentBancaireRepository;
import com.example.contentieux_security.entity.AgentBancaire;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.contentieux_security.enums.TypeRisque;
@RestController
@RequestMapping("/api/agent/dossiers")
@RequiredArgsConstructor
public class DossierController {

    private final DossierService dossierService;
    private final HistoriqueService historiqueService;
    private final DossierRepository dossierRepository;
    private final ValidateurRepository validateurRepository;
    private final ClientRepository clientRepository;
    private final NotificationService notificationService;
    private final GarantieRepository garantieRepository;
    private final RisqueRepository risqueRepository;
    private final RisqueService risqueService;
    private final AgentBancaireRepository agentBancaireRepository;
    private final MissionService missionService;
    private final PrestationService prestationService;
    private final AffaireJudiciaireRepository affaireRepository;

    @GetMapping("/create-data")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> getCreationData(Principal principal) {
        try {
            AgentBancaire agent = agentBancaireRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Agent introuvable"));
            List<Client> clients = clientRepository.findByAgence_Id(agent.getAgence().getId());
            return ResponseEntity.ok(Map.of("clients", clients));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> creerDossier(@RequestBody DossierCreationRequest request, Principal principal) {
        try {
            System.out.println(">>> USERNAME Keycloak : [" + principal.getName() + "]");
            DossierContentieux dossier = dossierService.creerDossier(request, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Dossier créé avec succès", "dossierId", dossier.getId()));
        } catch (Exception e) {
            System.out.println(">>> ERREUR : " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detailDossier(@PathVariable Long id, Principal principal) {
        try {
            System.out.println(">>> detailDossier appelé id=" + id + " par=" + principal.getName());
    
            // ── 1. Charger le DTO du dossier ──────────────────────────────
            DossierDetailDTO dossier = dossierService.getDossierDetail(id);
            System.out.println(">>> DTO chargé : " + dossier.getNumeroDossier());
    
            // ── 2. Charger l'agent connecté ───────────────────────────────
            AgentBancaire agent = agentBancaireRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Agent introuvable : " + principal.getName()));
            System.out.println(">>> Agent trouvé : " + agent.getUsername());
    
            Long agenceId = agent.getAgence().getId();
    
            // ── 3. Charger les validateurs de l'agence ────────────────────
            List<Map<String, Object>> vf = validateurRepository
            .findByTypeValidateurAndActifTrueAndAgence_Id(
                    TypeValidateur.VALIDATEUR_FINANCIER, agenceId)
            .stream()
            .map(v -> Map.<String, Object>of(
                    "id",       v.getId(),
                    "username", v.getUsername(),
                    "nom",      v.getNom() != null ? v.getNom() : "",
                    "prenom",   v.getPrenom() != null ? v.getPrenom() : ""
            ))
            .toList();
            List<Map<String, Object>> vj = validateurRepository
            .findByTypeValidateurAndActifTrueAndAgence_Id(
                    TypeValidateur.VALIDATEUR_JURIDIQUE, agenceId)
            .stream()
            .map(v -> Map.<String, Object>of(
                    "id",       v.getId(),
                    "username", v.getUsername(),
                    "nom",      v.getNom() != null ? v.getNom() : "",
                    "prenom",   v.getPrenom() != null ? v.getPrenom() : ""
            ))
            .toList();
            // ── 4. Charger l'historique ───────────────────────────────────
            List<?> historique = historiqueService.getHistorique(id);
    
            // ── 5. Mission avocat (peut être null) ────────────────────────
            Object missionAvocat = null;
            try {
                missionAvocat = missionService.getMissionAvocatDuDossier(id);
            } catch (Exception e) {
                System.out.println(">>> Pas de mission avocat : " + e.getMessage());
            }
    
            // ── 6. Affaire judiciaire ──────────────────────────────────────
            boolean affaireExiste = false;
            try {
                affaireExiste = !affaireRepository.findByDossier_Id(id).isEmpty();
            } catch (Exception e) {
                System.out.println(">>> Erreur affaire : " + e.getMessage());
            }
    
            // ── 7. Prestation judiciaire ──────────────────────────────────
            Object prestationJudiciaire = null;
            try {
                prestationJudiciaire = prestationService.getPrestationJudiciaireParDossier(id);
            } catch (Exception e) {
                System.out.println(">>> Erreur prestation : " + e.getMessage());
            }
    
            // ── 8. Construire la réponse ──────────────────────────────────
            Map<String, Object> response = new HashMap<>();
            response.put("dossier",                dossier);
            response.put("validateurs_financiers",  vf);
            response.put("validateurs_juridiques",  vj);
            response.put("historique",             dossier.getHistorique()); // ← depuis le DTO
            response.put("missionAvocat",           missionAvocat);
            response.put("affaireExiste",           affaireExiste);
            response.put("prestationJudiciaire",    prestationJudiciaire);
    
            System.out.println(">>> Réponse construite avec succès");
            return ResponseEntity.ok(response);
    
        } catch (Exception e) {
            System.out.println(">>> ERREUR detailDossier: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/choisir-validateurs")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> choisirValidateurs(@PathVariable Long id, @RequestBody Map<String, String> body, Principal principal) {
        try {
            dossierService.choisirValidateurs(id, body.get("validateurFinancier"), body.get("validateurJuridique"), principal.getName());
            return ResponseEntity.ok(Map.of("message", "Validateurs assignés"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/soumettre")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> soumettre(@PathVariable Long id, Principal principal) {
        try {
            dossierService.soumettreAValidation(id, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Dossier envoyé aux validateurs"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> supprimer(@PathVariable Long id, Principal principal) {
        try {
            dossierService.supprimerDossier(id, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Dossier supprimé"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> modifier(@PathVariable Long id, @RequestBody DossierCreationRequest request, Principal principal) {
        try {
            dossierService.modifierDossier(id, request, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Dossier modifié"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{dossierId}/risques/{risqueId}/selectionner")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> selectionnerRisque(@PathVariable Long dossierId, @PathVariable Long risqueId, @RequestBody Map<String, Boolean> body, Principal principal) {
        try {
            boolean value = Boolean.TRUE.equals(body.get("selectionne"));
            dossierService.selectionnerRisque(dossierId, risqueId, value, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Risque mis à jour."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{dossierId}/risques")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> ajouterRisque(
            @PathVariable Long dossierId,
            @Valid @RequestBody RisqueAjoutRequest request,
            BindingResult bindingResult,
            Principal principal) {
    
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Données invalides"));
        }
    
        try {
            Long risqueId = dossierService.ajouterRisque(dossierId, request, principal.getName());
    
            return ResponseEntity.ok(Map.of(
                    "id", risqueId,
                    "message", "Risque ajouté avec succès"
            ));
    
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    
    
    
    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @Transactional(readOnly = true)  // ← AJOUTEZ
    public ResponseEntity<?> listeDossiers(Principal principal,
                                            @RequestParam(required = false) String recherche) {
        try {
            List<DossierContentieux> dossiers = dossierService.rechercherDossiers(
                principal.getName(), recherche
            );
    
            List<Map<String, Object>> result = dossiers.stream().map(d -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", d.getId());
                item.put("numeroDossier", d.getNumeroDossier());
                item.put("libelle", d.getLibelle());
                item.put("statut", d.getStatut() != null ? d.getStatut().name() : null);
                item.put("dateCreation", d.getDateCreation());
    
                if (d.getClient() != null) {
                    Client c = d.getClient();
                    String type = c.getTypeClient() != null ? c.getTypeClient().name() : "PARTICULIER";
                    item.put("clientType", type);
                    item.put("clientTypeClient", type);
                    item.put("clientNom", c.getNom());
                    item.put("clientPrenom", c.getPrenom());
                    item.put("clientRaisonSociale", c.getRaisonSociale());
                }
    
                // ← Risques accessibles car @Transactional maintient la session
                double montant = 0.0;
                if (d.getRisques() != null) {
                    montant = d.getRisques().stream()
                        .mapToDouble(r -> r.getMontantImpaye() != null ? r.getMontantImpaye() : 0)
                        .sum();
                }
                item.put("montantTotalEngagement", montant);
    
                return item;
            }).toList();
    
            return ResponseEntity.ok(Map.of(
                "dossiers", result,
                "totalDossiers", result.size()
            ));
    
        } catch (Exception e) {
            System.out.println(">>> ERREUR listeDossiers: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @PutMapping("/garanties/{gId}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> modifierGarantie(@PathVariable Long gId, @RequestBody Map<String, Object> body, Principal principal) {
        try {
            Double valeurEstimee = body.get("valeurEstimee") != null ? Double.valueOf(body.get("valeurEstimee").toString()) : null;
            dossierService.modifierGarantie(gId, (String) body.get("typeGarantie"), (String) body.get("description"), valeurEstimee, (String) body.get("documentRef"), principal.getName());
            return ResponseEntity.ok(Map.of("message", "Garantie modifiée avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/garanties/{gId}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> supprimerGarantie(@PathVariable Long gId) {
        try {
            garantieRepository.deleteById(gId);
            return ResponseEntity.ok(Map.of("message", "Garantie supprimée."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }



    // DossierController.java - Ajoutez ces méthodes

// Modifier un risque
@PutMapping("/{dossierId}/risques/{risqueId}")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
public ResponseEntity<?> modifierRisque(@PathVariable Long dossierId, @PathVariable Long risqueId, @RequestBody Map<String, Object> body) {
    try {
        Risque risque = risqueRepository.findById(risqueId)
            .orElseThrow(() -> new RuntimeException("Risque non trouvé"));
        
        // Utiliser String car le champ type est de type String dans l'entité
        if (body.containsKey("type") && body.get("type") != null) {
            risque.setType((String) body.get("type"));
        }
        if (body.containsKey("montantInitial") && body.get("montantInitial") != null) {
            risque.setMontantInitial(Double.valueOf(body.get("montantInitial").toString()));
        }
        if (body.containsKey("montantImpaye") && body.get("montantImpaye") != null) {
            risque.setMontantImpaye(Double.valueOf(body.get("montantImpaye").toString()));
        }
        if (body.containsKey("dateEcheance") && body.get("dateEcheance") != null) {
            String dateStr = (String) body.get("dateEcheance");
            if (!dateStr.isEmpty()) {
                risque.setDateEcheance(java.time.LocalDate.parse(dateStr));
            }
        }
        if (body.containsKey("description") && body.get("description") != null) {
            risque.setDescription((String) body.get("description"));
        }
        
        risqueRepository.save(risque);
        return ResponseEntity.ok(Map.of("message", "Risque modifié avec succès"));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}


// Supprimer un risque
// DossierController.java - Ajoutez cette méthode

/**
 * Supprimer un risque (crédit) et toutes ses garanties associées
 * DELETE /api/agent/dossiers/{dossierId}/risques/{risqueId}
 */
@DeleteMapping("/{dossierId}/risques/{risqueId}")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
@Transactional
public ResponseEntity<?> supprimerRisque(@PathVariable Long dossierId, @PathVariable Long risqueId) {
    try {
        Risque risque = risqueRepository.findById(risqueId)
            .orElseThrow(() -> new RuntimeException("Risque non trouvé avec l'ID: " + risqueId));
        
        if (!risque.getDossier().getId().equals(dossierId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ce risque n'appartient pas au dossier spécifié"));
        }
        
        risqueRepository.delete(risque);
        
        return ResponseEntity.ok(Map.of("message", "Risque supprimé avec succès"));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}



    
}