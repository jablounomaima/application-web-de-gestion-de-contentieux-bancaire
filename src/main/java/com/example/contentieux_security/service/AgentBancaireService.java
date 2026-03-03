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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentBancaireService {

    private final AgentBancaireRepository agentRepository;
    private final AgenceRepository agenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakUserService keycloakUserService;

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
        // Vérifications
        if (agentRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Nom d'utilisateur déjà existant");
        }

        Agence agence = agenceRepository.findById(request.getAgenceId())
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));

        // ✅ CRÉATION UNIQUE (pas de duplication)
        AgentBancaire agent = AgentBancaire.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .role(request.getRole() != null ? request.getRole() : "AGENT")
                .matricule(request.getMatricule())
                .dateEmbauche(request.getDateEmbauche())
                .agence(agence)
                .actif(true)
                .build();

        // Sauvegarder dans la base locale
        AgentBancaire savedAgent = agentRepository.save(agent);

        // Créer dans Keycloak
        try {
          keycloakUserService.createUser(
    request.getUsername(), // username
    request.getEmail(),    // email correct
    request.getNom(),      // firstName
    request.getPrenom(),   // lastName
    request.getPassword(), // mot de passe
    request.getRole()      // rôle
);
        } catch (Exception e) {
            agentRepository.delete(savedAgent);
            throw new RuntimeException("Erreur création Keycloak: " + e.getMessage());
        }

        return convertToDTO(savedAgent);
    }

    @Transactional
    public AgentBancaireDTO updateAgent(Long id, AgentCreationRequest request) {
        AgentBancaire agent = agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

        Agence agence = agenceRepository.findById(request.getAgenceId())
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));

        // Mise à jour des champs
        agent.setNom(request.getNom());
        agent.setPrenom(request.getPrenom());
        agent.setEmail(request.getEmail());
        agent.setTelephone(request.getTelephone());
        agent.setMatricule(request.getMatricule());
        agent.setDateEmbauche(request.getDateEmbauche());
        
        if (request.getRole() != null) {
            agent.setRole(request.getRole());
        }
        
        agent.setAgence(agence);

       // Mise à jour du mot de passe si fourni
if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {

    String encodedPassword = passwordEncoder.encode(request.getPassword());
    agent.setPassword(encodedPassword);

    // Mise à jour dans Keycloak
    keycloakUserService.updatePassword(agent.getUsername(), request.getPassword());
}
        return convertToDTO(agentRepository.save(agent));
    }

    @Transactional
    public void deleteAgent(Long id) {
        AgentBancaire agent = agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

        try {
            keycloakUserService.deleteUser(agent.getUsername());
        } catch (Exception e) {
            System.err.println("Warning: Impossible de supprimer de Keycloak: " + e.getMessage());
        }

        agentRepository.delete(agent);
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
        dto.setRole(agent.getRole());
        dto.setActif(agent.getActif());
        
        // ✅ Utilisation correcte des méthodes
        if (agent.getAgence() != null) {
            dto.setAgenceId(agent.getAgence().getId());
            dto.setNomAgence(agent.getNomAgence()); // Méthode de l'entité
        }
        
        return dto;
    }
}