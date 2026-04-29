package com.example.contentieux_security.service;

import com.example.contentieux_security.dto.AgentBancaireDTO;
import com.example.contentieux_security.dto.AgentCreationRequest;
import com.example.contentieux_security.dto.AgentProfileUpdateRequest;
import com.example.contentieux_security.dto.PasswordChangeRequest;
import com.example.contentieux_security.entity.Agence;
import com.example.contentieux_security.entity.AgentBancaire;
import com.example.contentieux_security.repository.AgenceRepository;
import com.example.contentieux_security.repository.AgentBancaireRepository;
import com.example.contentieux_security.config.KeycloakUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.repository.MissionRepository;



import java.time.LocalDateTime;
@Service
public class AgentBancaireService {

    private final AgentBancaireRepository agentRepository;
    private final AgenceRepository        agenceRepository;
    private final PasswordEncoder         passwordEncoder;
    private final KeycloakUserService     keycloakUserService;
    private final MissionRepository missionRepository;
    private final EmailService emailService; // ✅ injecter

    public AgentBancaireService(AgentBancaireRepository agentRepository,
                                AgenceRepository agenceRepository,
                                PasswordEncoder passwordEncoder,
                                KeycloakUserService keycloakUserService,
                                MissionRepository missionRepository,
                                EmailService emailService) {
        this.agentRepository    = agentRepository;
        this.agenceRepository   = agenceRepository;
        this.passwordEncoder    = passwordEncoder;
        this.keycloakUserService = keycloakUserService;
        this.missionRepository = missionRepository;
        this.emailService=emailService;
    } 

    // ── Recherche par username ────────────────────────────────────

    /**
     * Recherche un agent par son username avec chargement de l'agence.
     * @Transactional(readOnly = true) permet d'accéder aux relations lazy (agence)
     * même après la fermeture de la session Hibernate.
     */
    @Transactional(readOnly = true)  // ✅ AJOUTÉ : Charge l'agence en lazy loading
    public AgentBancaire findAgentByUsername(String username) {
        return agentRepository.findByUsername(username)
                .map(agent -> {
                    // Forcer le chargement de l'agence pour éviter LazyInitializationException
                    if (agent.getAgence() != null) {
                        agent.getAgence().getNom(); // Touche la propriété pour charger
                    }
                    return agent;
                })
                .orElse(null);
    }


@Transactional
    public AgentBancaire getAgentByUsername(String username) {
        return agentRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé: " + username));
    }

    // ── CRUD ──────────────────────────────────────────────────────
    @Transactional(readOnly = true)

    public List<AgentBancaireDTO> getAllAgents() {
        return agentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
@Transactional
    public List<AgentBancaireDTO> getAgentsByAgence(Long agenceId) {
        return agentRepository.findByAgenceId(agenceId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public AgentBancaireDTO getAgentById(Long id) {
        AgentBancaire agent = agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));
        return convertToDTO(agent);
    }

    @Transactional
    public AgentBancaireDTO createAgent(AgentCreationRequest request) {
    
        // 1. Validations — password RETIRÉ des validations obligatoires
        if (request.getUsername() == null || request.getUsername().isBlank())
            throw new RuntimeException("Nom d'utilisateur obligatoire");
        if (request.getEmail() == null || request.getEmail().isBlank())
            throw new RuntimeException("Email obligatoire");
        if (agentRepository.existsByUsername(request.getUsername()))
            throw new RuntimeException("Nom d'utilisateur déjà existant");
    
        // 2. ✅ Génération OBLIGATOIRE — jamais fourni par le formulaire
        String motDePasse = genererMotDePasse();
    
        // 3. Agence
        Agence agence = agenceRepository.findById(request.getAgenceId())
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));
    
        // 4. Keycloak
        try {
            keycloakUserService.createUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getNom(),
                    request.getPrenom(),
                    motDePasse,
                    "AGENT"
            );
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("déjà existant") || msg.contains("already exists")
                    || msg.contains("[409]") || msg.contains("conflit")) {
                throw new RuntimeException("Username ou email déjà utilisé dans Keycloak.");
            }
            throw new RuntimeException("Erreur Keycloak: " + e.getMessage(), e);
        }
    
        // 5. Base de données
        try {
            AgentBancaire agent = new AgentBancaire();
            agent.setUsername(request.getUsername());
            agent.setPassword(passwordEncoder.encode(motDePasse));
            agent.setNom(request.getNom());
            agent.setPrenom(request.getPrenom());
            agent.setEmail(request.getEmail());
            agent.setTelephone(request.getTelephone());
            agent.setRole("AGENT");
            agent.setMatricule(request.getMatricule());
            agent.setDateEmbauche(request.getDateEmbauche());
            agent.setAgence(agence);
            agent.setActif(true);
    
            AgentBancaire saved = agentRepository.save(agent);
    
            // 6. ✅ Email OBLIGATOIRE — lancé après sauvegarde réussie
            emailService.envoyerCredentiels(
                    request.getEmail(),
                    request.getUsername(),
                    motDePasse
            );
    
            return convertToDTO(saved);
    
        } catch (Exception e) {
            // Compensation Keycloak
            try { keycloakUserService.deleteUser(request.getUsername()); }
            catch (Exception ex) { System.err.println("❌ Compensation échouée: " + ex.getMessage()); }
            throw new RuntimeException("Erreur base de données: " + e.getMessage(), e);
        }
    }
    
    // ✅ Génération sécurisée — format: Agent@XXXXXX
    private String genererMotDePasse() {
        return "Agent@" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }


    @Transactional
    public AgentBancaireDTO updateAgent(Long id, AgentCreationRequest request) {
        AgentBancaire agent = agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

        Agence agence = agenceRepository.findById(request.getAgenceId())
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));

        agent.setNom(request.getNom());
        agent.setPrenom(request.getPrenom());
        agent.setEmail(request.getEmail());
        agent.setTelephone(request.getTelephone());
        agent.setAgence(agence);

        return convertToDTO(agentRepository.save(agent));
    }

    @Transactional
    public void deleteAgent(Long id) {
        AgentBancaire agent = agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));
    
        String username = agent.getUsername();
    
        // 1. ✅ Supprimer dans Keycloak EN PREMIER
        //    Si Keycloak échoue → on arrête, rien n'est supprimé en DB
        try {
            keycloakUserService.deleteUser(username);
            System.out.println("✅ Keycloak: Agent supprimé — " + username);
        } catch (Exception e) {
            throw new RuntimeException(
                "Erreur suppression Keycloak: " + e.getMessage());
        }
    
        // 2. ✅ Supprimer en base de données
        try {
            agentRepository.deleteById(id);
            System.out.println("✅ DB: Agent supprimé — id=" + id);
        } catch (Exception e) {
            // ⚠️ Compensation — recréer dans Keycloak si la DB échoue ?
            // Difficile à compenser — logger l'incohérence
            System.err.println("❌ DB: Échec suppression agent id=" + id
                + " — Keycloak déjà supprimé ! Incohérence possible.");
            throw new RuntimeException(
                "Erreur suppression base de données: " + e.getMessage());
        }
    }
    
    
    
    @Transactional
    public void toggleAgentStatus(Long id) {
        AgentBancaire agent = agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));
    
        // ✅ Inverser le statut
        boolean nouveauStatut = !agent.isActif();
        agent.setActif(nouveauStatut);
        agentRepository.save(agent);
    
        // ✅ Synchroniser avec Keycloak — activer OU désactiver
        try {
            keycloakUserService.toggleUserStatus(agent.getUsername(), nouveauStatut);
            System.out.println("✅ Keycloak: statut mis à jour → "
                + agent.getUsername() + " = " + nouveauStatut);
        } catch (Exception e) {
            // ⚠️ Rollback DB si Keycloak échoue
            agent.setActif(!nouveauStatut);
            agentRepository.save(agent);
            throw new RuntimeException("Erreur Keycloak toggle: " + e.getMessage());
        }
    }

    @Transactional
    public void changePassword(String username, PasswordChangeRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword()))
            throw new RuntimeException("La confirmation du mot de passe est incorrecte");

        AgentBancaire agent = agentRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), agent.getPassword()))
            throw new RuntimeException("Mot de passe actuel incorrect");

        agent.setPassword(passwordEncoder.encode(request.getNewPassword()));
        agentRepository.save(agent);
        keycloakUserService.updatePassword(username, request.getNewPassword());
    }

    @Transactional
    public void updateProfile(String username, AgentProfileUpdateRequest request) {
        AgentBancaire agent = agentRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

        agent.setNom(request.getNom());
        agent.setPrenom(request.getPrenom());
        agent.setEmail(request.getEmail());
        agent.setTelephone(request.getTelephone());
        agentRepository.save(agent);
    }

    private AgentBancaireDTO convertToDTO(AgentBancaire agent) {
        AgentBancaireDTO dto = new AgentBancaireDTO();
        dto.setId(agent.getId());
        dto.setUsername(agent.getUsername());
        dto.setNom(agent.getNom());
        dto.setPrenom(agent.getPrenom());
        dto.setEmail(agent.getEmail());
        dto.setTelephone(agent.getTelephone());
        dto.setMatricule(agent.getMatricule());
        dto.setDateEmbauche(agent.getDateEmbauche());
        dto.setAgenceId(agent.getAgence() != null ? agent.getAgence().getId() : null);
        dto.setNomAgence(agent.getAgence() != null ? agent.getAgence().getNom() : null);
        dto.setActif(agent.isActif());
        return dto;
    }

    @Transactional
    // ✅ Méthode à ajouter dans AgentService
public AgentBancaire findByUsername(String username) {
    return agentRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Agent non trouvé: " + username));
}



    


}