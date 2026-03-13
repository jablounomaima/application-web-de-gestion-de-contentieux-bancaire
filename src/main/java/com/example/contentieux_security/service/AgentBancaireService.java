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

@Service
public class AgentBancaireService {

    private final AgentBancaireRepository agentRepository;
    private final AgenceRepository        agenceRepository;
    private final PasswordEncoder         passwordEncoder;
    private final KeycloakUserService     keycloakUserService;

    public AgentBancaireService(AgentBancaireRepository agentRepository,
                                AgenceRepository agenceRepository,
                                PasswordEncoder passwordEncoder,
                                KeycloakUserService keycloakUserService) {
        this.agentRepository    = agentRepository;
        this.agenceRepository   = agenceRepository;
        this.passwordEncoder    = passwordEncoder;
        this.keycloakUserService = keycloakUserService;
    }

    // ── Recherche par username ────────────────────────────────────

    public AgentBancaire findAgentByUsername(String username) {
        // ✅ findByUsername retourne Optional maintenant
        return agentRepository.findByUsername(username).orElse(null);
    }

    public AgentBancaire getAgentByUsername(String username) {
        return agentRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé: " + username));
    }

    // ── CRUD ──────────────────────────────────────────────────────

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
        if (agentRepository.existsByUsername(request.getUsername()))
            throw new RuntimeException("Nom d'utilisateur déjà existant");

        Agence agence = agenceRepository.findById(request.getAgenceId())
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));

        keycloakUserService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getNom(),
                request.getPrenom(),
                request.getPassword(),
                "AGENT"
        );

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