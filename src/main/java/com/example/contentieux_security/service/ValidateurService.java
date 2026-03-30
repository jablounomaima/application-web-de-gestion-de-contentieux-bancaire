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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ValidateurService {

    private final KeycloakUserService  keycloakUserService;
    private final ValidateurRepository validateurRepository;
    private final AgenceRepository     agenceRepository;

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
    public Validateur creerValidateur(ValidateurCreationRequest request) {

        // Validations
        if (request.getMatricule() == null || request.getMatricule().isBlank())
            throw new IllegalArgumentException("Matricule obligatoire");

        if (validateurRepository.existsByMatricule(request.getMatricule()))
            throw new IllegalArgumentException("Matricule déjà utilisé");

        if (validateurRepository.existsByEmail(request.getEmail()))
            throw new IllegalArgumentException("Email déjà utilisé");

        // ✅ Déterminer le rôle Keycloak selon le type de validateur
        String roleKeycloak = switch (request.getType()) {
            case VALIDATEUR_FINANCIER -> "VALIDATEUR_FINANCIER";
            case VALIDATEUR_JURIDIQUE -> "VALIDATEUR_JURIDIQUE";
        };

        System.out.println("=== Création validateur Keycloak : "
                + request.getUsername() + " → rôle : " + roleKeycloak);

        // ✅ Créer l'utilisateur dans Keycloak avec le bon rôle
        try {
            keycloakUserService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getNom(),        // ✔️ firstName
                request.getPrenom(),     // ✔️ lastName
                request.getPassword(),   // ✔️ password
                roleKeycloak
            );
            System.out.println("=== Utilisateur Keycloak créé ✅ : "
                    + request.getUsername());
        } catch (Exception e) {
            System.err.println("=== ERREUR Keycloak : " + e.getMessage());
            throw new RuntimeException(
                    "Erreur lors de la création dans Keycloak : "
                    + e.getMessage());
        }

        // ✅ Créer l'entité en base
        Agence agence = agenceRepository.findById(request.getAgenceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Agence introuvable : " + request.getAgenceId()));

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
        System.out.println("=== Validateur sauvegardé en base ✅ id="
                + saved.getId());
        return saved;
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

    public void delete(Long id) {
        Validateur v = findOrThrow(id);
        // ✅ Supprimer aussi dans Keycloak
        try {
            keycloakUserService.deleteUser(v.getUsername());
        } catch (Exception e) {
            System.err.println("=== WARN Keycloak delete : " + e.getMessage());
        }
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