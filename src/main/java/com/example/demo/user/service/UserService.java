package com.example.demo.user.service;

import com.example.demo.user.dto.CreateUserRequest;
import com.example.demo.user.dto.UserResponse;
import com.example.demo.user.entity.Utilisateur;
import com.example.demo.user.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UtilisateurRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // Vérifier si l'utilisateur existe déjà
        if (repository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Un utilisateur avec ce nom existe déjà");
        }

        Utilisateur user;
        switch (request.getRole()) {
            case ROLE_ADMIN:
                user = new com.example.demo.user.entity.impl.Admin();
                break;
            case ROLE_AGENT_BANCAIRE:
                user = new com.example.demo.user.entity.impl.AgentBancaire();
                break;
            case ROLE_VALIDATEUR_JURIDIQUE:
                user = new com.example.demo.user.entity.impl.ValidateurJuridique();
                break;
            case ROLE_VALIDATEUR_FINANCIER:
                user = new com.example.demo.user.entity.impl.ValidateurFinancier();
                break;
            case ROLE_AVOCAT:
                user = new com.example.demo.user.entity.impl.Avocat();
                break;
            case ROLE_HUISSIER:
                user = new com.example.demo.user.entity.impl.Huissier();
                break;
            case ROLE_EXPERT:
                user = new com.example.demo.user.entity.impl.Expert();
                break;
            default:
                throw new RuntimeException("Rôle non supporté");
        }

        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setEmail(request.getEmail());
        
        Utilisateur saved = repository.save(user);
        
        System.out.println("✅ Nouvel utilisateur créé : " + saved.getUsername() + " avec le rôle " + saved.getRole());
        
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        Utilisateur user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return mapToResponse(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Utilisateur non trouvé");
        }
        repository.deleteById(id);
        System.out.println("🗑️ Utilisateur supprimé : " + id);
    }

    @Transactional
    public UserResponse updateUser(Long id, CreateUserRequest request) {
        Utilisateur user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setEmail(request.getEmail());
        
        // Note: Changer le rôle peut être complexe si on change de classe d'implémentation (Admin vers Agent etc.)
        user.setRole(request.getRole());

        Utilisateur saved = repository.save(user);
        return mapToResponse(saved);
    }

    private UserResponse mapToResponse(Utilisateur user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setNom(user.getNom());
        response.setPrenom(user.getPrenom());
        response.setEmail(user.getEmail());
        response.setDateCreation(user.getDateCreation());
        return response;
    }
}
