package com.example.contentieux_security.service;

import com.example.contentieux_security.config.KeycloakUserService;
import com.example.contentieux_security.dto.PrestataireCreationRequest;
import com.example.contentieux_security.dto.PrestataireDTO;
import com.example.contentieux_security.entity.AgentBancaire;
import com.example.contentieux_security.entity.Prestataire;
import com.example.contentieux_security.entity.Utilisateur;
import com.example.contentieux_security.enums.TypePrestataire;
import com.example.contentieux_security.repository.AgentBancaireRepository;
import com.example.contentieux_security.repository.PrestataireRepository;
import com.example.contentieux_security.repository.UtilisateurRepository;

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
    private final UtilisateurRepository utilisateurRepository; // ✅ injecter
    private final EmailService emailService; // ✅ injecter

    // ════════════════════════════════════════
    // ✅ CRÉATION
    // ════════════════════════════════════════

    @Transactional
    public Prestataire creerPrestataire(PrestataireCreationRequest request, String agentUsername) {

        // 1. Vérifications unicité
        if (prestataireRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username déjà utilisé");
        }
         if (prestataireRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        // 2. ✅ Génération automatique du mot de passe
        //    Si l'agent en fournit un → on l'utilise
        //    Sinon → on génère automatiquement
        String motDePasse = (request.getMotDePasse() != null && !request.getMotDePasse().isBlank())
                ? request.getMotDePasse()
                : genererMotDePasseTemporaire();

        // 3. Créer le compte dans Keycloak
        keycloakUserService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPrenom(),
                request.getNom(),
                motDePasse,
                request.getTypePrestataire().toKeycloakRole()
        );

        // 4. ✅ Envoyer username + mot de passe par email au prestataire
        //    Le prestataire reçoit ses credentials directement dans sa boîte mail
        emailService.envoyerCredentiels(
                request.getEmail(),   // destinataire
                request.getUsername(), // username
                motDePasse            // mot de passe généré ou fourni
        );

        // 5. Récupérer l'agent responsable
        AgentBancaire agent = agentRepository.findByUsername(agentUsername)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

        // 6. Sauvegarder en base de données
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
    
        // 1. Récupérer le prestataire en DB
        Prestataire p = findById(id);
    
        // 2. Vérifier que c'est bien l'agent responsable qui fait la modification
        if (!p.getAgentResponsable().getUsername().equals(agentUsername)) {
            throw new RuntimeException("Accès refusé");
        }
    
        // 3. Mettre à jour les infos de base dans Keycloak (nom, prénom, email)
        //    Le mot de passe N'est PAS géré ici — c'est fait séparément en étape 4
        keycloakUserService.updateUser(
            p.getUsername(),
            request.getEmail(),
            request.getPrenom(),
            request.getNom()
        );
    
        // 4. ✅ CORRECTION — Changer le mot de passe dans Keycloak si fourni
        //    Sans ce bloc, le motDePasse envoyé par Angular était complètement ignoré
        //    On vérifie null ET isBlank() pour éviter de changer avec une valeur vide
        if (request.getMotDePasse() != null && !request.getMotDePasse().isBlank()) {
            keycloakUserService.changeUserPassword(p.getUsername(), request.getMotDePasse());
        }
    
        // 5. Mettre à jour les infos dans la base de données locale
        p.setPrenom(request.getPrenom());
        p.setNom(request.getNom());
        p.setEmail(request.getEmail());
        p.setTelephone(request.getTelephone());
        p.setAdresse(request.getAdresse());
        p.setSpecialite(request.getSpecialite());
        p.setNumeroCartePro(request.getNumeroCartePro());
        p.setNiveauValidation(request.getNiveauValidation());
        p.setPlafondValidation(request.getPlafondValidation());
    
        // 6. Sauvegarder et retourner le prestataire mis à jour
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
    public boolean supprimerPrestataire(Long id, String agentUsername) {
    
        Prestataire p = findById(id);
    
        if (!p.getAgentResponsable().getUsername().equals(agentUsername)) {
            throw new RuntimeException("Accès refusé");
        }
    
        // ✅ Supprime dans Keycloak (= supprime le login)
        keycloakUserService.deleteUser(p.getUsername());
    
        // ✅ Supprime dans la DB
        prestataireRepository.delete(p);
    
        return true;
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



    public Prestataire findByUsername(String username) {
        // Méthode publique qui retourne un objet Prestataire
        // Paramètre : username - le nom d'utilisateur du prestataire à rechercher
        
        // Appel au repository pour trouver le prestataire par son username
        // prestataireRepository.findByUsername(username) retourne un Optional<Prestataire>
        return prestataireRepository.findByUsername(username)
        
                // Si le prestataire existe, .get() le retourne
                // Si le prestataire n'existe pas, on lance une exception
                .orElseThrow(() -> new RuntimeException("Prestataire non trouvé: " + username));
                // Crée une nouvelle exception avec un message d'erreur personnalisé
                // qui indique quel username n'a pas été trouvé
    }


}