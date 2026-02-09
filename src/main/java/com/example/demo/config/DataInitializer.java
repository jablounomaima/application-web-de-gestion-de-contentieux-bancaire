package com.example.demo.config;

import com.example.demo.agence.entity.Agence;
import com.example.demo.agence.repository.AgenceRepository;
import com.example.demo.client.entity.ClientMorale;
import com.example.demo.client.entity.ClientPhysique;
import com.example.demo.client.repository.ClientRepository;
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

    private final UtilisateurRepository utilisateurRepository;
    private final AgenceRepository agenceRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Admin
        createOrUpdateUser("admin", "admin123", Role.ROLE_ADMIN, "Admin", "System");
        
        // Agent
        createOrUpdateUser("agent", "agent123", Role.ROLE_AGENT_BANCAIRE, "Dupont", "Jean");

        // Initialisation des Agences
        if (agenceRepository.count() == 0) {
            createAgence("AG001", "Agence Casablanca Zkt", "Casablanca", "Boulevard Zerktouni");
            createAgence("AG002", "Agence Rabat Agdal", "Rabat", "Avenue de France");
            System.out.println("✅ Agences initialisées");
        }

        // Initialisation des Clients
        if (clientRepository.count() == 0) {
            createClientPhysique("Ali", "Amrani", "AA123456", "Casablanca");
            createClientMorale("Tech Solutions SARL", "RC-998877", "Casablanca");
            System.out.println("✅ Clients initialisés");
        }
    }

    private void createOrUpdateUser(String username, String rawPassword, Role role, String nom, String prenom) {
        Utilisateur user = utilisateurRepository.findByUsername(username)
                .orElseGet(() -> {
                    if (role == Role.ROLE_ADMIN) return new com.example.demo.user.entity.impl.Admin();
                    if (role == Role.ROLE_AGENT_BANCAIRE) return new com.example.demo.user.entity.impl.AgentBancaire();
                    return new com.example.demo.user.entity.impl.AgentBancaire();
                });

        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setEmail(username + "@banque.com");

        utilisateurRepository.save(user);
        System.out.println("✅ Utilisateur prêt : " + username);
    }

    private void createAgence(String code, String nom, String ville, String adresse) {
        Agence agence = new Agence();
        agence.setCode(code);
        agence.setNom(nom);
        agence.setVille(ville);
        agence.setAdresse(adresse);
        agenceRepository.save(agence);
    }

    private void createClientPhysique(String nom, String prenom, String cin, String ville) {
        ClientPhysique cp = new ClientPhysique();
        cp.setNom(nom);
        cp.setPrenom(prenom);
        cp.setCin(cin);
        cp.setVille(ville);
        cp.setAdresse("Adresse " + nom);
        cp.setTelephone("0600000000");
        clientRepository.save(cp);
    }

    private void createClientMorale(String raison, String rc, String ville) {
        ClientMorale cm = new ClientMorale();
        cm.setRaisonSociale(raison);
        cm.setNumeroRC(rc);
        cm.setVille(ville);
        cm.setAdresse("Zone Industrielle " + ville);
        cm.setTelephone("0500000000");
        clientRepository.save(cm);
    }
}
