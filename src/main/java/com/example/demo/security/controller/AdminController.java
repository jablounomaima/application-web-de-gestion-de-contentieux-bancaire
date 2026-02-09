package com.example.demo.security.controller;

import com.example.demo.user.entity.Utilisateur;
import com.example.demo.user.repository.UtilisateurRepository;
import com.example.demo.user.role.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Endpoint pour réinitialiser les mots de passe des utilisateurs de test
     * ATTENTION : À supprimer en production !
     */
    @PostMapping("/reset-passwords")
    @Transactional
    public ResponseEntity<String> resetPasswords() {
        try {
            // Réinitialiser admin
            Utilisateur admin = utilisateurRepository.findByUsername("admin")
                    .orElse(new com.example.demo.user.entity.impl.Admin());
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ROLE_ADMIN);
            utilisateurRepository.save(admin);

            // Réinitialiser agent
            Utilisateur agent = utilisateurRepository.findByUsername("agent")
                    .orElse(new com.example.demo.user.entity.impl.AgentBancaire());
            agent.setUsername("agent");
            agent.setPassword(passwordEncoder.encode("agent123"));
            agent.setRole(Role.ROLE_AGENT_BANCAIRE);
            utilisateurRepository.save(agent);

            System.out.println("✅ MOTS DE PASSE RÉINITIALISÉS");
            System.out.println("Admin password hash: " + admin.getPassword());
            System.out.println("Agent password hash: " + agent.getPassword());

            return ResponseEntity.ok("Mots de passe réinitialisés avec succès pour admin et agent");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erreur: " + e.getMessage());
        }
    }
}
