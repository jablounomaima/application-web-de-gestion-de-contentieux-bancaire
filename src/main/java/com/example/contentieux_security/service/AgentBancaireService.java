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
        if (agentRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Nom d'utilisateur déjà existant");
        }

        Agence agence = agenceRepository.findById(request.getAgenceId())
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));

        // Créer dans Keycloak d'abord
        keycloakUserService.createUser(
            request.getUsername(),
            request.getPassword(),
            request.getEmail(),
            request.getNom(),
            request.getPrenom(),
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

        // Supprimer de Keycloak
        keycloakUserService.deleteUser(agent.getUsername());

        // Supprimer de la base locale
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
        dto.setNomAgence(agent.getNomAgence());
        dto.setActif(agent.getActif());
        return dto;
    }
}