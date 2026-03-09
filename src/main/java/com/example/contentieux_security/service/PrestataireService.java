package com.example.contentieux_security.service;

import com.example.contentieux_security.config.KeycloakUserService;
import com.example.contentieux_security.dto.PrestataireCreationRequest;
import com.example.contentieux_security.dto.PrestataireDTO;
import com.example.contentieux_security.entity.AgentBancaire;
import com.example.contentieux_security.entity.Prestataire;
import com.example.contentieux_security.entity.TypePrestataire;
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

    // ══════════════════════════════════════════════════════════════
    //  CRÉATION — Keycloak + base locale
    // ══════════════════════════════════════════════════════════════

    @Transactional
    public Prestataire creerPrestataire(PrestataireCreationRequest request,
                                        String agentUsername) {

        if (prestataireRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException(
                "Le nom d'utilisateur '" + request.getUsername() + "' est déjà utilisé.");
        }
        if (prestataireRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException(
                "L'email '" + request.getEmail() + "' est déjà utilisé.");
        }

        String motDePasse = (request.getMotDePasse() != null && !request.getMotDePasse().isBlank())
                ? request.getMotDePasse()
                : genererMotDePasseTemporaire();

        // 1 — Créer dans Keycloak
        String roleKeycloak = request.getType().toKeycloakRole();
        keycloakUserService.createUser(
                request.getUsername(), request.getEmail(),
                request.getPrenom(), request.getNom(),
                motDePasse, roleKeycloak);

        // 2 — Envoyer email (non bloquant)
        try {
            keycloakUserService.sendVerificationEmail(request.getUsername());
        } catch (Exception e) {
            System.out.println("⚠️ Email non envoyé: " + e.getMessage());
        }

        // 3 — Trouver l'agent
        AgentBancaire agent = agentRepository.findByUsername(agentUsername)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Agent non trouvé: " + agentUsername));

        // 4 — Sauvegarder en base
        Prestataire prestataire = Prestataire.builder()
                .username(request.getUsername())
                .prenom(request.getPrenom())
                .nom(request.getNom())
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .adresse(request.getAdresse())
                .type(request.getType())
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

    // Alias pour AgentPrestataireController existant
    @Transactional
    public Prestataire createPrestataire(PrestataireCreationRequest request,
                                         String agentUsername) {
        return creerPrestataire(request, agentUsername);
    }

    // ══════════════════════════════════════════════════════════════
    //  LECTURE
    // ══════════════════════════════════════════════════════════════

    public List<Prestataire> getAllPrestataires() {
        return prestataireRepository.findAll();
    }

    public List<Prestataire> getPrestatairesParAgent(String agentUsername) {
        return prestataireRepository.findByAgentResponsable_Username(agentUsername);
    }

    // Alias pour AgentPrestataireController existant (par ID agent)
    public List<Prestataire> getPrestatairesByAgent(Long agentId) {
        return prestataireRepository.findByAgentResponsable_Id(agentId);
    }

    // Alias pour AgentPrestataireController existant (par type + agent)
    public List<Prestataire> getPrestatairesByTypeAndAgent(String type, Long agentId) {
        TypePrestataire typeEnum = TypePrestataire.valueOf(type.toUpperCase());
        return prestataireRepository.findByTypeAndAgentResponsable_Id(typeEnum, agentId);
    }

    public Prestataire findById(Long id) {
        return prestataireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prestataire non trouvé: " + id));
    }

    // Pour AgentController — retourne un DTO vérifiant que l'agent est propriétaire
    public PrestataireDTO getPrestataireByIdAndAgent(Long id, String agentUsername) {
        Prestataire p = findById(id);
        if (!p.getAgentResponsable().getUsername().equals(agentUsername)) {
            throw new RuntimeException("Accès refusé à ce prestataire.");
        }
        return toDTO(p);
    }

    // ══════════════════════════════════════════════════════════════
    //  MISE À JOUR
    // ══════════════════════════════════════════════════════════════

    @Transactional
    public Prestataire updatePrestataire(Long id, PrestataireCreationRequest request,
                                          String agentUsername) {
        Prestataire p = findById(id);
        if (!p.getAgentResponsable().getUsername().equals(agentUsername)) {
            throw new RuntimeException("Accès refusé.");
        }

        // Mise à jour Keycloak
        keycloakUserService.updateUser(
                p.getUsername(), request.getEmail(),
                request.getPrenom(), request.getNom());

        // Mise à jour base locale
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

    // ══════════════════════════════════════════════════════════════
    //  TOGGLE ACTIF/INACTIF
    // ══════════════════════════════════════════════════════════════

    @Transactional
    public void toggleStatut(Long id) {
        Prestataire p = findById(id);
        p.setActif(!p.isActif());
        keycloakUserService.toggleUserStatus(p.getUsername(), p.isActif());
        prestataireRepository.save(p);
    }

    // Alias avec vérification agent
    @Transactional
    public void togglePrestataireStatus(Long id, String agentUsername) {
        Prestataire p = findById(id);
        if (!p.getAgentResponsable().getUsername().equals(agentUsername)) {
            throw new RuntimeException("Accès refusé.");
        }
        toggleStatut(id);
    }

    // ══════════════════════════════════════════════════════════════
    //  SUPPRESSION
    // ══════════════════════════════════════════════════════════════

    @Transactional
    public void supprimerPrestataire(Long id) {
        Prestataire p = findById(id);
        keycloakUserService.deleteUser(p.getUsername());
        prestataireRepository.delete(p);
    }

    // Alias avec vérification agent (pour AgentController)
    @Transactional
    public void deletePrestataire(Long id, String agentUsername) {
        Prestataire p = findById(id);
        if (!p.getAgentResponsable().getUsername().equals(agentUsername)) {
            throw new RuntimeException("Accès refusé.");
        }
        supprimerPrestataire(id);
    }

    // ══════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ══════════════════════════════════════════════════════════════

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
        dto.setType(p.getType());
        dto.setSpecialite(p.getSpecialite());
        dto.setNumeroCartePro(p.getNumeroCartePro());
        dto.setNiveauValidation(p.getNiveauValidation());
        dto.setPlafondValidation(p.getPlafondValidation());
        dto.setActif(p.isActif());
        return dto;
    }
}