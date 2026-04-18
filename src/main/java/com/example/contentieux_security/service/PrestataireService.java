package com.example.contentieux_security.service;

import com.example.contentieux_security.config.KeycloakUserService;
import com.example.contentieux_security.dto.PrestataireCreationRequest;
import com.example.contentieux_security.dto.PrestataireDTO;
import com.example.contentieux_security.entity.AgentBancaire;
import com.example.contentieux_security.entity.Prestataire;
import com.example.contentieux_security.enums.TypePrestataire;
import com.example.contentieux_security.repository.AgentBancaireRepository;
import com.example.contentieux_security.repository.PrestataireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrestataireService {

    private final PrestataireRepository prestataireRepository;
    private final AgentBancaireRepository agentRepository;
    private final KeycloakUserService keycloakUserService;
    
    // ════════════════════════════════════════
    // ✅ CRÉATION
    // ════════════════════════════════════════

    @Transactional
    public Prestataire creerPrestataire(PrestataireCreationRequest request, String agentUsername) {

        if (prestataireRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username déjà utilisé");
        }

        if (prestataireRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        String motDePasse = (request.getMotDePasse() != null && !request.getMotDePasse().isBlank())
                ? request.getMotDePasse()
                : genererMotDePasseTemporaire();

        // 🔹 Keycloak
        keycloakUserService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPrenom(),
                request.getNom(),
                motDePasse,
                request.getTypePrestataire().toKeycloakRole()
        );

        try {
            keycloakUserService.sendVerificationEmail(request.getUsername());
        } catch (Exception e) {
            System.out.println("Email non envoyé: " + e.getMessage());
        }

        // 🔹 Agent
        AgentBancaire agent = agentRepository.findByUsername(agentUsername)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

        // 🔹 Save DB
        Prestataire prestataire = Prestataire.builder()
                .username(request.getUsername())
                .prenom(request.getPrenom())
                .nom(request.getNom())
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .adresse(request.getAdresse())
                .type(request.getTypePrestataire())
                .specialite(request.getSpecialite())
                .numeroCartePro(request.getNumeroCartePro())
                .niveauValidation(request.getNiveauValidation())
                .plafondValidation(request.getPlafondValidation())
                .dateDebutCollaboration(LocalDate.now())
                .agentResponsable(agent)
                .agence(agent.getAgence())
                .actif(true)
                .build();

        return prestataireRepository.save(prestataire);
    }

    public Prestataire createPrestataire(PrestataireCreationRequest request, String agentUsername) {
        return creerPrestataire(request, agentUsername);
    }

    // ════════════════════════════════════════
    // ✅ LECTURE
    // ════════════════════════════════════════

    public List<Prestataire> getAllPrestataires() {
        return prestataireRepository.findAll();
    }

    public List<Prestataire> getPrestatairesParAgent(String agentUsername) {
        return prestataireRepository.findByAgentResponsable_Username(agentUsername);
    }

    public Prestataire findById(Long id) {
        return prestataireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prestataire non trouvé"));
    }

    public PrestataireDTO getPrestataireByIdAndAgent(Long id, String agentUsername) {
        Prestataire p = findById(id);

        if (!p.getAgentResponsable().getUsername().equals(agentUsername)) {
            throw new RuntimeException("Accès refusé");
        }

        return toDTO(p);
    }

    // ════════════════════════════════════════
    // ✅ UPDATE
    // ════════════════════════════════════════

    @Transactional
    public Prestataire updatePrestataire(Long id, PrestataireCreationRequest request, String agentUsername) {

        Prestataire p = findById(id);

        if (!p.getAgentResponsable().getUsername().equals(agentUsername)) {
            throw new RuntimeException("Accès refusé");
        }

        // Keycloak update
        keycloakUserService.updateUser(
                p.getUsername(),
                request.getEmail(),
                request.getPrenom(),
                request.getNom()
        );

        // DB update
        p.setPrenom(request.getPrenom());
        p.setNom(request.getNom());
        p.setEmail(request.getEmail());
        p.setTelephone(request.getTelephone());
        p.setAdresse(request.getAdresse());
        p.setSpecialite(request.getSpecialite());
        p.setNumeroCartePro(request.getNumeroCartePro());
        p.setNiveauValidation(request.getNiveauValidation());
        p.setPlafondValidation(request.getPlafondValidation());

        return prestataireRepository.save(p);
    }

    // ════════════════════════════════════════
    // ✅ ACTIF / INACTIF
    // ════════════════════════════════════════

    @Transactional
    public Prestataire toggleActif(Long id, String agentUsername) {

        Prestataire p = findById(id);

        if (!p.getAgentResponsable().getUsername().equals(agentUsername)) {
            throw new RuntimeException("Accès refusé");
        }

        p.setActif(!p.isActif());

        // sync Keycloak
        keycloakUserService.toggleUserStatus(p.getUsername(), p.isActif());

        return prestataireRepository.save(p);
    }

    // ════════════════════════════════════════
    // ✅ DELETE
    // ════════════════════════════════════════

    @Transactional
    public void supprimerPrestataire(Long id, String agentUsername) {

        Prestataire p = findById(id);

        if (!p.getAgentResponsable().getUsername().equals(agentUsername)) {
            throw new RuntimeException("Accès refusé");
        }

        keycloakUserService.deleteUser(p.getUsername());
        prestataireRepository.delete(p);
    }

    // ════════════════════════════════════════
    // ✅ UTIL
    // ════════════════════════════════════════

    private String genererMotDePasseTemporaire() {
        return "Prest@" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private PrestataireDTO toDTO(Prestataire p) {
        PrestataireDTO dto = new PrestataireDTO();

        dto.setId(p.getId());
        dto.setUsername(p.getUsername());
        dto.setPrenom(p.getPrenom());
        dto.setNom(p.getNom());
        dto.setEmail(p.getEmail());
        dto.setTelephone(p.getTelephone());
        dto.setAdresse(p.getAdresse());
        dto.setTypePrestataire(p.getType());
        dto.setSpecialite(p.getSpecialite());
        dto.setNumeroCartePro(p.getNumeroCartePro());
        dto.setNiveauValidation(p.getNiveauValidation());
        dto.setPlafondValidation(p.getPlafondValidation());
        dto.setActif(p.isActif());

        return dto;
    }


    public boolean deletePrestataire(Long id, String username) {
        Prestataire prestataire = prestataireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prestataire non trouvé"));

        // (optionnel) vérification utilisateur
        if (!prestataire.getAgentResponsable().getUsername().equals(username)) {
            throw new RuntimeException("Accès refusé");
        }

        prestataireRepository.delete(prestataire);
        return true;
    }

    
}