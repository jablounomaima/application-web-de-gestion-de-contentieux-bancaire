package com.example.contentieux_security.service;

import com.example.contentieux_security.dto.AgentBancaireDTO;
import com.example.contentieux_security.dto.AgentCreationRequest;
import com.example.contentieux_security.entity.Agence;
import com.example.contentieux_security.entity.AgentBancaire;
import com.example.contentieux_security.repository.AgenceRepository;
import com.example.contentieux_security.repository.AgentBancaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.contentieux_security.dto.PasswordChangeRequest;  // AJOUTER CETTE LIGNE

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentBancaireService {

    private final AgentBancaireRepository agentRepository;
    private final AgenceRepository agenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakUserService keycloakUserService;

    /**
     * Récupère un agent par username (gère les doublons)
     */
    public AgentBancaire findAgentByUsername(String username) {
        List<AgentBancaire> agents = agentRepository.findByUsername(username);
        
        if (agents.isEmpty()) {
            System.out.println("Aucun agent trouvé avec username: " + username);
            return null;
        }
        
        if (agents.size() > 1) {
            System.out.println("⚠️ ATTENTION: " + agents.size() + " agents avec username '" + username + "'");
            System.out.println("IDs: " + agents.stream().map(AgentBancaire::getId).toList());
            System.out.println("Utilisation du premier (ID: " + agents.get(0).getId() + ")");
        }
        
        return agents.get(0);
    }

    /**
     * Récupère un agent par username (lance exception si non trouvé)
     */
    public AgentBancaire getAgentByUsername(String username) {
        AgentBancaire agent = findAgentByUsername(username);
        if (agent == null) {
            throw new RuntimeException("Agent non trouvé dans la base locale: " + username);
        }
        return agent;
    }

    public List<AgentBancaireDTO> getAllAgents() {
        return agentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

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
        if (agentRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Nom d'utilisateur déjà existant");
        }

        Agence agence = agenceRepository.findById(request.getAgenceId())
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));

        // Créer dans Keycloak d'abord
        keycloakUserService.createUser(
            request.getUsername(),
            request.getEmail(),
            request.getNom(),
            request.getPrenom(),
            request.getPassword(),
            "AGENT"
        );

        // Créer dans la base locale
        AgentBancaire agent = AgentBancaire.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .role("AGENT")
                .matricule(request.getMatricule())
                .dateEmbauche(request.getDateEmbauche())
                .agence(agence)
                .actif(true)
                .build();

        return convertToDTO(agentRepository.save(agent));
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
        agent.setActif(!agent.getActif());
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
        dto.setActif(agent.getActif());
        return dto;
    }

    @Transactional
public void changePassword(String username, PasswordChangeRequest request) {
    // Validation
    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
        throw new RuntimeException("Les nouveaux mots de passe ne correspondent pas");
    }
    
    if (request.getNewPassword().length() < 6) {
        throw new RuntimeException("Le mot de passe doit faire au moins 6 caractères");
    }
    
    // Récupérer l'agent
    AgentBancaire agent = findAgentByUsername(username);
    if (agent == null) {
        throw new RuntimeException("Agent non trouvé");
    }
    
    // Vérifier l'ancien mot de passe (dans la base locale)
    if (!passwordEncoder.matches(request.getCurrentPassword(), agent.getPassword())) {
        throw new RuntimeException("Mot de passe actuel incorrect");
    }
    
    // 1. Changer dans Keycloak
    keycloakUserService.changeUserPassword(username, request.getNewPassword());
    
    // 2. Changer dans la base locale
    agent.setPassword(passwordEncoder.encode(request.getNewPassword()));
    agentRepository.save(agent);
    
    System.out.println("✅ Mot de passe changé avec succès pour: " + username);
}
}