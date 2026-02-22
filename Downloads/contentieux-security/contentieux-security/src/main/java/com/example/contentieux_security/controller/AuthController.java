package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.LoginRequest;
import com.example.contentieux_security.entity.AgentBancaire;
import com.example.contentieux_security.repository.AgentBancaireRepository;
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
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Find agent locally
        AgentBancaire agent = agentBancaireRepository.findAll().stream()
                .filter(a -> a.getUsername().equals(request.getUsername()))
                .findFirst()
                .orElse(null);

        if (agent != null && agent.getPassword() != null && agent.getPassword().equals(request.getPassword())) {
            // Success - generate token with AGENT role
            String token = authService.generateToken(agent.getUsername(), Collections.singletonList("AGENT"));

            Map<String, Object> response = new HashMap<>();
            response.put("access_token", token);
            response.put("username", agent.getUsername());
            response.put("role", "AGENT");

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(401).body("Identifiants incorrects");
    }
}
