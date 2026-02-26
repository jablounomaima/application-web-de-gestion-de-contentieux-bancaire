package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.Utilisateur;
import com.example.contentieux_security.entity.AgentBancaire;
import com.example.contentieux_security.repository.UtilisateurRepository;
import com.example.contentieux_security.repository.AgentBancaireRepository;
import com.example.contentieux_security.service.KeycloakService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
public class UtilisateurController {

    private final UtilisateurRepository utilisateurRepository;
    private final AgentBancaireRepository agentBancaireRepository;
    private final KeycloakService keycloakService;

    @GetMapping
    public List<Utilisateur> getAllUtilisateurs(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        List<String> roles = (realmAccess != null && realmAccess.get("roles") instanceof List)
                ? (List<String>) realmAccess.get("roles")
                : List.of();
        boolean isAdmin = roles.contains("ADMIN");

        if (isAdmin) {
            return utilisateurRepository.findAll();
        }

        // Si c'est un AGENT, on filtre par son agence
        return agentBancaireRepository.findByUsername(username)
                .map(agent -> utilisateurRepository.findByAgenceId(agent.getAgence().getId()))
                .orElse(List.of());
    }

    @PostMapping
    public ResponseEntity<?> createUtilisateur(@RequestBody Utilisateur utilisateur, @AuthenticationPrincipal Jwt jwt) {
        try {
            String currentUser = jwt.getClaimAsString("preferred_username");
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            List<String> roles = (realmAccess != null && realmAccess.get("roles") instanceof List)
                    ? (List<String>) realmAccess.get("roles")
                    : List.of();
            boolean isAdmin = roles.contains("ADMIN");

            if (!isAdmin) {
                // Associer l'utilisateur à l'agence de l'agent créateur
                AgentBancaire agent = agentBancaireRepository.findByUsername(currentUser)
                        .orElseThrow(() -> new RuntimeException("Agent non trouvé"));
                utilisateur.setAgence(agent.getAgence());
            }

            Utilisateur saved = utilisateurRepository.save(utilisateur);

            // Synchronisation KEYCLOAK
            keycloakService.createKeycloakUser(utilisateur.getUsername(), utilisateur.getPassword(),
                    utilisateur.getRole().name());

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Utilisateur> updateUtilisateur(@PathVariable Long id,
            @RequestBody Utilisateur utilisateurDetails, @AuthenticationPrincipal Jwt jwt) {
        return utilisateurRepository.findById(id)
                .map(utilisateur -> {
                    String currentUser = jwt.getClaimAsString("preferred_username");
                    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                    List<String> roles = (realmAccess != null && realmAccess.get("roles") instanceof List)
                            ? (List<String>) realmAccess.get("roles")
                            : List.of();
                    boolean isAdmin = roles.contains("ADMIN");

                    if (!isAdmin) {
                        AgentBancaire agent = agentBancaireRepository.findByUsername(currentUser).orElse(null);
                        if (agent == null || utilisateur.getAgence() == null
                                || !utilisateur.getAgence().getId().equals(agent.getAgence().getId())) {
                            return ResponseEntity.status(403).<Utilisateur>build();
                        }
                    }

                    utilisateur.setNom(utilisateurDetails.getNom());
                    utilisateur.setPrenom(utilisateurDetails.getPrenom());
                    utilisateur.setEmail(utilisateurDetails.getEmail());
                    utilisateur.setTelephone(utilisateurDetails.getTelephone());
                    utilisateur.setRole(utilisateurDetails.getRole());
                    utilisateur.setSpecialite(utilisateurDetails.getSpecialite());

                    if (utilisateurDetails.getPassword() != null && !utilisateurDetails.getPassword().isEmpty()) {
                        utilisateur.setPassword(utilisateurDetails.getPassword());
                        // Synchronisation KEYCLOAK (Password)
                        keycloakService.updateKeycloakPassword(utilisateur.getUsername(),
                                utilisateurDetails.getPassword());
                    }

                    return ResponseEntity.ok(utilisateurRepository.save(utilisateur));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUtilisateur(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return utilisateurRepository.findById(id)
                .map(utilisateur -> {
                    String currentUser = jwt.getClaimAsString("preferred_username");
                    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                    List<String> roles = (realmAccess != null && realmAccess.get("roles") instanceof List)
                            ? (List<String>) realmAccess.get("roles")
                            : List.of();
                    boolean isAdmin = roles.contains("ADMIN");

                    if (!isAdmin) {
                        AgentBancaire agent = agentBancaireRepository.findByUsername(currentUser).orElse(null);
                        if (agent == null || utilisateur.getAgence() == null
                                || !utilisateur.getAgence().getId().equals(agent.getAgence().getId())) {
                            return ResponseEntity.status(403).build();
                        }
                    }

                    // Synchronisation KEYCLOAK (Delete)
                    keycloakService.deleteKeycloakUser(utilisateur.getUsername());

                    utilisateurRepository.delete(utilisateur);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
