package com.example.contentieux_security.service;

import com.example.contentieux_security.config.KeycloakUserService;
import com.example.contentieux_security.dto.ValidateurCreationRequest;
import com.example.contentieux_security.dto.ValidateurDTO;
import com.example.contentieux_security.entity.Agence;
import com.example.contentieux_security.entity.Validateur;
import com.example.contentieux_security.enums.TypeValidateur;
import com.example.contentieux_security.repository.AgenceRepository;
import com.example.contentieux_security.repository.ValidateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.contentieux_security.dto.ValidateurDTO.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ValidateurService {

    private final KeycloakUserService  keycloakUserService;
    private final ValidateurRepository validateurRepository;
    private final AgenceRepository     agenceRepository;
    private final EmailService emailService;
    // ── Lecture ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ValidateurDTO> getAllValidateurs() {
        return validateurRepository.findAll()
                .stream().map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ValidateurDTO getById(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ValidateurDTO> getByTypeAndActif(TypeValidateur type) {
        return validateurRepository
                .findByTypeValidateurAndActifTrue(type)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── Création ──────────────────────────────────────────

    /**
     * Crée un validateur en base ET dans Keycloak.
     * Le rôle Keycloak est déterminé selon le type :
     *   VALIDATEUR_FINANCIER → ROLE_VALIDATEUR_FINANCIER
     *   VALIDATEUR_JURIDIQUE → ROLE_VALIDATEUR_JURIDIQUE
     */
   
    @Transactional
    public Validateur creerValidateur(ValidateurCreationRequest request) {
    
        // 1. Validations — password RETIRÉ
        if (request.getMatricule() == null || request.getMatricule().isBlank())
            throw new IllegalArgumentException("Matricule obligatoire");
        if (validateurRepository.existsByMatricule(request.getMatricule()))
            throw new IllegalArgumentException("Matricule déjà utilisé");
        if (validateurRepository.existsByEmail(request.getEmail()))
           throw new IllegalArgumentException("Email déjà utilisé");
    
        // 2. ✅ Génération OBLIGATOIRE
        String motDePasse = genererMotDePasse();
    
        // 3. Rôle Keycloak
        String roleKeycloak = switch (request.getType()) {
            case VALIDATEUR_FINANCIER -> "VALIDATEUR_FINANCIER";
            case VALIDATEUR_JURIDIQUE -> "VALIDATEUR_JURIDIQUE";
        };
    
        // 4. Keycloak
        try {
            keycloakUserService.createUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getNom(),
                    request.getPrenom(),
                    motDePasse,
                    roleKeycloak
            );
        } catch (Exception e) {
            throw new RuntimeException("Erreur Keycloak: " + e.getMessage());
        }
    
        // 5. Base de données
        Agence agence = agenceRepository.findById(request.getAgenceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Agence introuvable: " + request.getAgenceId()));
    
        try {
            Validateur v = new Validateur();
            v.setMatricule(request.getMatricule());
            v.setUsername(request.getUsername());
            v.setNom(request.getNom());
            v.setPrenom(request.getPrenom());
            v.setEmail(request.getEmail());
            v.setTelephone(request.getTelephone());
            v.setTypeValidateur(request.getType());
            v.setAgence(agence);
            v.setActif(true);
    
            Validateur saved = validateurRepository.save(v);
    
            // 6. ✅ Email OBLIGATOIRE
          emailService.envoyerCredentiels(
                    request.getEmail(),
                  request.getUsername(),
                    motDePasse
         
         
               );
    
            return saved;
    
        } catch (Exception e) {
            // Compensation Keycloak
            try { keycloakUserService.deleteUser(request.getUsername()); }
            catch (Exception ex) { System.err.println("❌ Compensation échouée: " + ex.getMessage()); }
            throw new RuntimeException("Erreur base de données: " + e.getMessage(), e);
        }
    }
    


    // ✅ Génération sécurisée — format: Valid@XXXXXX
    private String genererMotDePasse() {
        return "Valid@" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
    // ── Mise à jour ───────────────────────────────────────

    public ValidateurDTO update(Long id, ValidateurDTO dto) {
        Validateur v = findOrThrow(id);

        if (validateurRepository.existsByMatriculeAndIdNot(dto.getMatricule(), id))
            throw new IllegalArgumentException("Matricule déjà utilisé");

        if (validateurRepository.existsByEmailAndIdNot(dto.getEmail(), id))
            throw new IllegalArgumentException("Email déjà utilisé");

        mapToEntity(dto, v);
        return toDTO(validateurRepository.save(v));
    }

    // ── Suppression ───────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        Validateur validateur = validateurRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Validateur introuvable: " + id));
        
        // Supprimer dans Keycloak
        try {
            keycloakUserService.deleteUser(validateur.getUsername());
        } catch (Exception e) {
            System.err.println("⚠️ Suppression Keycloak échouée: " + e.getMessage());
            // On continue quand même pour supprimer en base
        }
    
        // Supprimer en base
        validateurRepository.deleteById(id);
    }

    // ── Toggle actif ──────────────────────────────────────

    public void toggleActif(Long id) {
        Validateur v = findOrThrow(id);
        v.setActif(!v.isActif());
        validateurRepository.save(v);
    }

    // ── Helpers ───────────────────────────────────────────

    private Validateur findOrThrow(Long id) {
        return validateurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Validateur introuvable"));
    }

    private void mapToEntity(ValidateurDTO dto, Validateur v) {
        v.setMatricule(dto.getMatricule());
        v.setUsername(dto.getUsername());
        v.setNom(dto.getNom());
        v.setPrenom(dto.getPrenom());
        v.setEmail(dto.getEmail());
        v.setTelephone(dto.getTelephone());
        v.setTypeValidateur(dto.getTypeValidateur());
        v.setActif(dto.isActif());

        if (dto.getAgenceId() != null) {
            Agence agence = agenceRepository.findById(dto.getAgenceId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Agence introuvable"));
            v.setAgence(agence);
        }
    }

    private ValidateurDTO toDTO(Validateur v) {
        ValidateurDTO dto = new ValidateurDTO();
        dto.setId(v.getId());
        dto.setMatricule(v.getMatricule());
        dto.setUsername(v.getUsername()); // ✅ add this

        dto.setNom(v.getNom());
        dto.setPrenom(v.getPrenom());
        dto.setEmail(v.getEmail());
        dto.setTelephone(v.getTelephone());
        dto.setTypeValidateur(v.getTypeValidateur());
        dto.setActif(v.isActif());
        if (v.getAgence() != null) {
            dto.setAgenceId(v.getAgence().getId());
            dto.setAgenceNom(v.getAgence().getNom());
        }
        return dto;
    }

    public TypeValidateur[] getTypesValidateur() {
        return TypeValidateur.values();
    }
}