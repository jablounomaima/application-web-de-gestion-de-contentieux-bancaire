package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.*;
import com.example.contentieux_security.enums.TypeValidateur;
import com.example.contentieux_security.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur d'administration pour la gestion des ressources du système en mode REST API.
 * Toutes les routes nécessitent le rôle ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AgenceService agenceService;
    private final AgentBancaireService agentService;
    private final ValidateurService validateurService;

    // ═══════════════════════════════════════════════════════════════════════
    // DASHBOARD ADMIN
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        response.put("username", principal != null ? principal.getName() : "Admin");
        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GESTION DES AGENCES
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/agences")
    public ResponseEntity<List<AgenceDTO>> listAgences() {
        return ResponseEntity.ok(agenceService.getAllAgences());
    }

    @PostMapping("/agences")
    public ResponseEntity<?> createAgence(@RequestBody AgenceDTO dto) {
        try {
            agenceService.createAgence(dto);
            return ResponseEntity.ok(Map.of("message", "Agence créée avec succès !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erreur création agence: " + e.getMessage()));
        }
    }

    @PutMapping("/agences/{id}")
    public ResponseEntity<?> updateAgence(@PathVariable Long id, @RequestBody AgenceDTO dto) {
        try {
            agenceService.updateAgence(id, dto);
            return ResponseEntity.ok(Map.of("message", "Agence mise à jour !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erreur mise à jour: " + e.getMessage()));
        }
    }

    @DeleteMapping("/agences/{id}")
    public ResponseEntity<?> deleteAgence(@PathVariable Long id) {
        try {
            agenceService.deleteAgence(id);
            return ResponseEntity.ok(Map.of("message", "Agence supprimée !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erreur suppression: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GESTION DES AGENTS BANCAIRES
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/agents")
    public ResponseEntity<?> listAgents() {
        Map<String, Object> response = new HashMap<>();
        response.put("agents", agentService.getAllAgents());
        response.put("agences", agenceService.getAllAgences());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/agents/{id}")
    public ResponseEntity<?> getAgent(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(agentService.getAgentById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/agents")
    public ResponseEntity<?> createAgent(@RequestBody AgentCreationRequest request) {
        try {
            agentService.createAgent(request);
            return ResponseEntity.ok(Map.of("message", "Agent '" + request.getUsername() + "' créé avec succès !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erreur création agent: " + e.getMessage()));
        }
    }

    @PutMapping("/agents/{id}")
    public ResponseEntity<?> updateAgent(@PathVariable Long id, @RequestBody AgentCreationRequest request) {
        try {
            agentService.updateAgent(id, request);
            return ResponseEntity.ok(Map.of("message", "Agent mis à jour !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erreur mise à jour: " + e.getMessage()));
        }
    }

    @DeleteMapping("/agents/{id}")
    public ResponseEntity<?> deleteAgent(@PathVariable Long id) {
        try {
            agentService.deleteAgent(id);
            return ResponseEntity.ok(Map.of("message", "Agent supprimé !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erreur suppression: " + e.getMessage()));
        }
    }

    @PatchMapping("/agents/{id}/toggle")
    public ResponseEntity<?> toggleAgent(@PathVariable Long id) {
        try {
            agentService.toggleAgentStatus(id);
            return ResponseEntity.ok(Map.of("message", "Statut modifié !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GESTION DES VALIDATEURS
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/validateurs")
    public ResponseEntity<?> listValidateurs() {
        Map<String, Object> response = new HashMap<>();
        response.put("validateursFinanciers", validateurService.getByTypeAndActif(TypeValidateur.VALIDATEUR_FINANCIER));
        response.put("validateursJuridiques", validateurService.getByTypeAndActif(TypeValidateur.VALIDATEUR_JURIDIQUE));
        response.put("agences", agenceService.getAllAgences());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validateurs/{id}")
    public ResponseEntity<?> getValidateur(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(validateurService.getById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/validateurs")
    public ResponseEntity<?> createValidateur(@RequestBody ValidateurCreationRequest request) {
        try {
            validateurService.creerValidateur(request);
            return ResponseEntity.ok(Map.of("message", "Validateur créé avec succès !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erreur création validateur: " + e.getMessage()));
        }
    }

    @PutMapping("/validateurs/{id}")
    public ResponseEntity<?> updateValidateur(@PathVariable Long id, @RequestBody ValidateurCreationRequest request) {
        try {
            ValidateurDTO dto = new ValidateurDTO();
            dto.setUsername(request.getUsername());
            dto.setEmail(request.getEmail());
            dto.setNom(request.getNom());
            dto.setPrenom(request.getPrenom());
            dto.setMatricule(request.getMatricule());
            dto.setTelephone(request.getTelephone());
            dto.setTypeValidateur(request.getType());
            dto.setAgenceId(request.getAgenceId());
            dto.setActif(true);

            validateurService.update(id, dto);
            return ResponseEntity.ok(Map.of("message", "Validateur '" + request.getUsername() + "' mis à jour avec succès !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erreur mise à jour validateur: " + e.getMessage()));
        }
    }

    @PatchMapping("/validateurs/{id}/toggle")
    public ResponseEntity<?> toggleValidateur(@PathVariable Long id) {
        try {
            validateurService.toggleActif(id);
            return ResponseEntity.ok(Map.of("message", "Statut du validateur modifié !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/validateurs/{id}")
    public ResponseEntity<?> deleteValidateur(@PathVariable Long id) {
        try {
            validateurService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Validateur supprimé !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}