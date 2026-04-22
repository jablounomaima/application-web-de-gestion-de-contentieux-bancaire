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

    public AgentBancaireService(AgentBancaireRepository agentRepository,
                                AgenceRepository agenceRepository,
                                PasswordEncoder passwordEncoder,
                                KeycloakUserService keycloakUserService,
                                MissionRepository missionRepository) {
        this.agentRepository    = agentRepository;
        this.agenceRepository   = agenceRepository;
        this.passwordEncoder    = passwordEncoder;
        this.keycloakUserService = keycloakUserService;
        this.missionRepository = missionRepository;
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
        if (agentRepository.existsByUsername(request.getUsername()))
            throw new RuntimeException("Nom d'utilisateur déjà existant");
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("Email obligatoire");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("Mot de passe obligatoire");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new RuntimeException("Nom d'utilisateur obligatoire");
        }

        Agence agence = agenceRepository.findById(request.getAgenceId())
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));

        // ✅ Créer d'abord dans Keycloak (si ça échoue, on n'a pas pollué la base)
        try {
            keycloakUserService.createUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getNom(),
                    request.getPrenom(),
                    request.getPassword(),
                    "AGENT"
            );
            System.out.println("✅ Keycloak: Utilisateur créé - " + request.getUsername());
        } catch (Exception e) {
            System.err.println("❌ Keycloak: Échec création - " + e.getMessage());
            String lower = (e.getMessage() == null ? "" : e.getMessage().toLowerCase());
            if (lower.contains("déjà existant") || lower.contains("already exists") || lower.contains("[409]") || lower.contains("conflit")) {
                throw new RuntimeException("Utilisateur déjà existant dans Keycloak (username ou email). Choisissez un autre username/email.");
            }
            throw new RuntimeException("Erreur création Keycloak: " + e.getMessage(), e);
        }

        // ✅ Puis créer dans la base
        try {
            AgentBancaire agent = new AgentBancaire();
            agent.setUsername(request.getUsername());
            agent.setPassword(passwordEncoder.encode(request.getPassword()));
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
            System.out.println("✅ Base de données: Agent créé - ID: " + saved.getId());
            return convertToDTO(saved);
            
        } catch (Exception e) {
            // ⚠️ Compensation: supprimer de Keycloak si la base échoue
            System.err.println("❌ Base de données: Échec création - " + e.getMessage());
            System.err.println("⚠️ Compensation: Suppression Keycloak...");
            try {
                keycloakUserService.deleteUser(request.getUsername());
                System.out.println("✅ Compensation: Utilisateur Keycloak supprimé");
            } catch (Exception deleteEx) {
                System.err.println("❌ Compensation échouée: " + deleteEx.getMessage());
            }
            throw new RuntimeException("Erreur création base de données: " + e.getMessage(), e);
        }
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
        keycloakUserService.deleteUser(agent.getUsername());
        agentRepository.deleteById(id);
    }

    @Transactional
    public void toggleAgentStatus(Long id) {
        AgentBancaire agent = agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));
        agent.setActif(!agent.isActif());
        agentRepository.save(agent);
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




    


}