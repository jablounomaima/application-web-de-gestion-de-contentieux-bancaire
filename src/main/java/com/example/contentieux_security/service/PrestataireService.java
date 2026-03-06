package com.example.contentieux_security.service;

import com.example.contentieux_security.config.KeycloakUserService;
import com.example.contentieux_security.dto.PrestataireCreationRequest;
import com.example.contentieux_security.dto.PrestataireDTO;
import com.example.contentieux_security.entity.AgentBancaire;
import com.example.contentieux_security.entity.Prestataire;
import com.example.contentieux_security.entity.TypePrestataire;
import com.example.contentieux_security.repository.PrestataireRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrestataireService {

    private final PrestataireRepository prestataireRepository;
    private final AgentBancaireService agentService;
    private final KeycloakUserService keycloakUserService;

    public PrestataireService(PrestataireRepository prestataireRepository,
                              AgentBancaireService agentService,
                              KeycloakUserService keycloakUserService) {
        this.prestataireRepository = prestataireRepository;
        this.agentService = agentService;
        this.keycloakUserService = keycloakUserService;
    }

    // ================== LECTURE ==================

    public List<PrestataireDTO> getPrestatairesByAgent(Long agentId) {
        return prestataireRepository.findByAgentResponsableId(agentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<PrestataireDTO> getPrestatairesByTypeAndAgent(String type, Long agentId) {

        TypePrestataire typeEnum;
        try {
            typeEnum = TypePrestataire.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Type de prestataire invalide");
        }

        return prestataireRepository
                .findByTypeAndAgentResponsableId(typeEnum, agentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PrestataireDTO getPrestataireByIdAndAgent(Long id, String agentUsername) {

        Prestataire prestataire = prestataireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prestataire non trouvé"));

        checkAuthorization(prestataire, agentUsername);

        return convertToDTO(prestataire);
    }

    // ================== CRÉATION ==================

    @Transactional
    public PrestataireDTO createPrestataire(PrestataireCreationRequest request,
                                            String agentUsername) {

        if (prestataireRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Ce nom d'utilisateur existe déjà");
        }

        AgentBancaire agent = agentService.findAgentByUsername(agentUsername);
        if (agent == null) {
            throw new RuntimeException("Agent responsable non trouvé");
        }

        TypePrestataire typeEnum;
        try {
            typeEnum = TypePrestataire.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Type de prestataire invalide");
        }

        String keycloakRole = mapTypeToRole(typeEnum);

        // Création dans Keycloak
        keycloakUserService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getNom(),
                request.getPrenom(),
                request.getPassword(),
                keycloakRole
        );

        Prestataire prestataire = new Prestataire();
        prestataire.setUsername(request.getUsername());
        prestataire.setNom(request.getNom());
        prestataire.setPrenom(request.getPrenom());
        prestataire.setEmail(request.getEmail());
        prestataire.setTelephone(request.getTelephone());
        prestataire.setAdresse(request.getAdresse());
        prestataire.setType(typeEnum);
        prestataire.setSpecialite(request.getSpecialite());
        prestataire.setNumeroCartePro(request.getNumeroCartePro());
        prestataire.setDateDebutCollaboration(LocalDate.now());
        prestataire.setActif(true);
        prestataire.setAgentResponsable(agent);
        prestataire.setAgence(agent.getAgence());
        prestataire.setNiveauValidation(request.getNiveauValidation());
        prestataire.setPlafondValidation(request.getPlafondValidation());

        return convertToDTO(prestataireRepository.save(prestataire));
    }

    // ================== MODIFICATION ==================

    @Transactional
    public void updatePrestataire(Long id,
                                  PrestataireCreationRequest request,
                                  String agentUsername) {

        Prestataire prestataire = prestataireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prestataire non trouvé"));

        checkAuthorization(prestataire, agentUsername);

        prestataire.setNom(request.getNom());
        prestataire.setPrenom(request.getPrenom());
        prestataire.setEmail(request.getEmail());
        prestataire.setTelephone(request.getTelephone());
        prestataire.setAdresse(request.getAdresse());
        prestataire.setSpecialite(request.getSpecialite());
        prestataire.setNumeroCartePro(request.getNumeroCartePro());
        prestataire.setNiveauValidation(request.getNiveauValidation());
        prestataire.setPlafondValidation(request.getPlafondValidation());

        // Mise à jour Keycloak
        keycloakUserService.updateUser(
                prestataire.getUsername(),
                request.getEmail(),
                request.getNom(),
                request.getPrenom()
        );

        prestataireRepository.save(prestataire);
    }

    // ================== SUPPRESSION ==================

    @Transactional
    public void deletePrestataire(Long id, String agentUsername) {

        Prestataire prestataire = prestataireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prestataire non trouvé"));

        checkAuthorization(prestataire, agentUsername);

        keycloakUserService.deleteUser(prestataire.getUsername());
        prestataireRepository.delete(prestataire);
    }

    // ================== ACTIVER / DÉSACTIVER ==================

    @Transactional
    public void togglePrestataireStatus(Long id, String agentUsername) {

        Prestataire prestataire = prestataireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prestataire non trouvé"));

        checkAuthorization(prestataire, agentUsername);

        boolean newStatus = !prestataire.isActif();
        prestataire.setActif(newStatus);

        keycloakUserService.toggleUserStatus(
                prestataire.getUsername(),
                newStatus
        );

        prestataireRepository.save(prestataire);
    }

    // ================== MÉTHODES UTILITAIRES ==================

    private void checkAuthorization(Prestataire prestataire,
                                    String agentUsername) {

        if (prestataire.getAgentResponsable() == null ||
                !prestataire.getAgentResponsable()
                        .getUsername()
                        .equals(agentUsername)) {

            throw new RuntimeException(
                    "Vous n'êtes pas autorisé à effectuer cette action");
        }
    }

    private String mapTypeToRole(TypePrestataire type) {
        return type.name(); // plus propre et automatique
    }

    private PrestataireDTO convertToDTO(Prestataire p) {

        PrestataireDTO dto = new PrestataireDTO();
        dto.setId(p.getId());
        dto.setUsername(p.getUsername());
        dto.setNom(p.getNom());
        dto.setPrenom(p.getPrenom());
        dto.setEmail(p.getEmail());
        dto.setTelephone(p.getTelephone());
        dto.setAdresse(p.getAdresse());
        dto.setType(p.getType() != null ? p.getType().name() : null);
        dto.setSpecialite(p.getSpecialite());
        dto.setNumeroCartePro(p.getNumeroCartePro());
        dto.setActif(p.isActif());
        dto.setNiveauValidation(p.getNiveauValidation());
        dto.setPlafondValidation(p.getPlafondValidation());

        if (p.getAgentResponsable() != null) {
            dto.setAgentResponsableNom(
                    p.getAgentResponsable().getNom() + " " +
                    p.getAgentResponsable().getPrenom()
            );
        }

        if (p.getAgence() != null) {
            dto.setAgenceNom(p.getAgence().getNom());
        }

        return dto;
    }
}