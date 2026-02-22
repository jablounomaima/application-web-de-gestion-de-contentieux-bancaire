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

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/me")
    public ResponseEntity<AgentBancaire> getAgentProfile(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return adminService.getAllAgents().stream()
                .filter(a -> a.getUsername().equals(username))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Agence Endpoints
    @GetMapping("/agences")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Agence> getAgences() {
        return adminService.getAllAgences();
    }

    @PostMapping("/agences")
    public Agence createAgence(@RequestBody Agence agence) {
        return adminService.saveAgence(agence);
    }

    @DeleteMapping("/agences/{id}")
    public ResponseEntity<?> deleteAgence(@PathVariable Long id) {
        adminService.deleteAgence(id);
        return ResponseEntity.ok().build();
    }

    // Agent Endpoints
    @GetMapping("/agents")
    public List<AgentBancaire> getAgents() {
        return adminService.getAllAgents();
    }

    @PostMapping("/agents")
    public AgentBancaire createAgent(@RequestBody AgentBancaire agent) {
        return adminService.saveAgent(agent);
    }

    @DeleteMapping("/agents/{id}")
    public ResponseEntity<?> deleteAgent(@PathVariable Long id) {
        adminService.deleteAgent(id);
        return ResponseEntity.ok().build();
    }

    // Stats Endpoints
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return adminService.getStats();
    }
}