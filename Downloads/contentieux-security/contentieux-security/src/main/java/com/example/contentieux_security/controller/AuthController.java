package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.LoginRequest;
import com.example.contentieux_security.entity.AgentBancaire;
import com.example.contentieux_security.entity.Utilisateur;
import com.example.contentieux_security.repository.AgentBancaireRepository;
import com.example.contentieux_security.repository.UtilisateurRepository;
import com.example.contentieux_security.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AgentBancaireRepository agentBancaireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 1. Check AgentBancaire table
        AgentBancaire agent = agentBancaireRepository.findAll().stream()
                .filter(a -> a.getUsername().equals(request.getUsername()))
                .findFirst()
                .orElse(null);

        if (agent != null && agent.getPassword() != null && agent.getPassword().equals(request.getPassword())) {
            String token = authService.generateToken(agent.getUsername(), Collections.singletonList("AGENT"));
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", token);
            response.put("username", agent.getUsername());
            response.put("role", "AGENT");
            return ResponseEntity.ok(response);
        }

        // 2. Check Utilisateur table (Avocat, Huissier, Expert, etc.)
        Utilisateur utilisateur = utilisateurRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(request.getUsername()))
                .findFirst()
                .orElse(null);

        if (utilisateur != null && utilisateur.getPassword() != null
                && utilisateur.getPassword().equals(request.getPassword())) {
            // Map enum role to the role string used in the frontend
            String roleStr = mapRole(utilisateur.getRole().name());
            String token = authService.generateToken(utilisateur.getUsername(), Collections.singletonList(roleStr));

            Map<String, Object> response = new HashMap<>();
            response.put("access_token", token);
            response.put("username", utilisateur.getUsername());
            response.put("role", roleStr);
            response.put("nom", utilisateur.getNom());
            response.put("prenom", utilisateur.getPrenom());
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(401).body("Identifiants incorrects");
    }

    private String mapRole(String roleEnum) {
        switch (roleEnum) {
            case "AVOCAT":
                return "AVOCAT";
            case "HUISSIER":
                return "HUISSIER";
            case "EXPERT":
                return "EXPERT";
            case "VALIDATEUR_FINANCIER":
                return "VALID_FINANCIER";
            case "VALIDATEUR_JURIDIQUE":
                return "VALID_JURIDIQUE";
            default:
                return roleEnum;
        }
    }
}
