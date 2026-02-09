package com.example.demo.config;

import com.example.demo.user.entity.Utilisateur;
import com.example.demo.user.repository.UtilisateurRepository;
import com.example.demo.user.role.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Admin
        createOrUpdateUser("admin", "admin123", Role.ROLE_ADMIN);
        
        // Agent
        createOrUpdateUser("agent", "agent123", Role.ROLE_AGENT_BANCAIRE);
    }

    private void createOrUpdateUser(String username, String rawPassword, Role role) {
        Utilisateur user = repository.findByUsername(username)
                .orElse(new Utilisateur());

        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);

        repository.save(user);
        System.out.println("✅ Utilisateur à jour : " + username);
    }
}
