package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.Agence;
import com.example.contentieux_security.entity.AgentBancaire;
import com.example.contentieux_security.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.example.contentieux_security.repository.UtilisateurRepository;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final UtilisateurRepository utilisateurRepository;
    private final com.example.contentieux_security.service.KeycloakService keycloakService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'AVOCAT', 'HUISSIER', 'EXPERT', 'VALID_FINANCIER', 'VALID_JURIDIQUE')")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");

        // 1. Chercher dans AgentBancaire (Rôle Agent principal)
        var agentCandidate = adminService.getAllAgents().stream()
                .filter(a -> a.getUsername().equals(username))
                .findFirst();

        if (agentCandidate.isPresent()) {
            return ResponseEntity.ok(agentCandidate.get());
        }

        // 2. Chercher dans Utilisateur (pour les autres rôles comme Avocat, Expert,
        // etc.)
        Optional<com.example.contentieux_security.entity.Utilisateur> user = utilisateurRepository
                .findByUsername(username);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }

        // 3. Fallback : Retourner les infos du token si non trouvé en base
        return ResponseEntity.ok(Map.of(
                "username", username,
                "roles", jwt.getClaim("realm_access") != null ? jwt.getClaim("realm_access") : List.of()));
    }

    // Agence Endpoints
    @GetMapping("/agences")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Agence> getAgences() {
        return adminService.getAllAgences();
    }

    @PostMapping("/agences")
    @PreAuthorize("hasRole('ADMIN')")
    public Agence createAgence(@RequestBody Agence agence) {
        return adminService.saveAgence(agence);
    }

    @PutMapping("/agences/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Agence updateAgence(@PathVariable Long id, @RequestBody Agence agenceData) {
        return adminService.getAllAgences().stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .map(agence -> {
                    agence.setNom(agenceData.getNom());
                    agence.setCode(agenceData.getCode());
                    agence.setVille(agenceData.getVille());
                    return adminService.saveAgence(agence);
                })
                .orElseThrow(() -> new RuntimeException("Agence not found"));
    }

    @DeleteMapping("/agences/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAgence(@PathVariable Long id) {
        adminService.deleteAgence(id);
        return ResponseEntity.ok().build();
    }

    // Agent Endpoints
    @GetMapping("/agents")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AgentBancaire> getAgents() {
        return adminService.getAllAgents();
    }

    @PostMapping("/agents")
    @PreAuthorize("hasRole('ADMIN')")
    public AgentBancaire createAgent(@RequestBody AgentBancaire agent) {
        // Synchronisation KEYCLOAK
        if (agent.getUsername() != null && agent.getPassword() != null) {
            keycloakService.createKeycloakUser(agent.getUsername(), agent.getPassword(), "AGENT");
        }
        return adminService.saveAgent(agent);
    }

    @PutMapping("/agents/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AgentBancaire updateAgent(@PathVariable Long id, @RequestBody AgentBancaire agentData) {
        return adminService.getAllAgents().stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .map(agent -> {
                    agent.setNom(agentData.getNom());
                    agent.setPrenom(agentData.getPrenom());
                    agent.setEmail(agentData.getEmail());
                    agent.setTelephone(agentData.getTelephone());
                    if (agentData.getPassword() != null && !agentData.getPassword().isEmpty()) {
                        agent.setPassword(agentData.getPassword());
                        // Synchronisation KEYCLOAK (Update Password)
                        keycloakService.updateKeycloakPassword(agent.getUsername(), agentData.getPassword());
                    }
                    if (agentData.getAgence() != null) {
                        agent.setAgence(agentData.getAgence());
                    }
                    return adminService.saveAgent(agent);
                })
                .orElseThrow(() -> new RuntimeException("Agent not found"));
    }

    @DeleteMapping("/agents/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAgent(@PathVariable Long id) {
        AgentBancaire agent = adminService.getAllAgents().stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (agent != null) {
            // Synchronisation KEYCLOAK (Delete)
            keycloakService.deleteKeycloakUser(agent.getUsername());
        }
        adminService.deleteAgent(id);
        return ResponseEntity.ok().build();
    }

    // Stats Endpoints
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getStats() {
        return adminService.getStats();
    }
}