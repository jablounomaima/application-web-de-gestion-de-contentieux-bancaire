package com.example.demo.config;

import com.example.demo.user.entity.Utilisateur;
import com.example.demo.user.repository.UtilisateurRepository;
import com.example.demo.user.role.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository repo;
    private final PasswordEncoder encoder;

    public DataInitializer(UtilisateurRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (repo.findByUsername("admin").isEmpty()) {
            Utilisateur admin = new Utilisateur();
            admin.setUsername("admin");
            admin.setPassword(encoder.encode("admin123"));
            admin.setRole(Role.ROLE_ADMIN);
            repo.save(admin);
            System.out.println("✅ Admin ajouté avec succès");
        } else {
            System.out.println("ℹ️ Admin déjà présent en base");
        }
    }
}
