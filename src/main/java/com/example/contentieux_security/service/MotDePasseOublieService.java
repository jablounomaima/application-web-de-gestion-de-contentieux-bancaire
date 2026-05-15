package com.example.contentieux_security.service;

import com.example.contentieux_security.config.KeycloakUserService;
import com.example.contentieux_security.repository.AgentBancaireRepository;
import com.example.contentieux_security.repository.ValidateurRepository;
import com.example.contentieux_security.repository.PrestataireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MotDePasseOublieService {

    private final AgentBancaireRepository   agentRepository;
    private final ValidateurRepository      validateurRepository;
    private final PrestataireRepository     prestataireRepository;
    private final KeycloakUserService       keycloakUserService;
    private final EmailService              emailService;

    // Email de l'admin — injecté depuis application.properties
    @Value("${app.admin.email:admin@banque.tn}")
    private String adminEmail;

    /**
     * Traite la demande de mot de passe oublié.
     * 1. Cherche l'utilisateur par email dans toutes les tables
     * 2. Envoie une notification à l'admin
     * 3. Envoie un accusé de réception à l'utilisateur
     */
   /**
 * Accepte email OU username.
 */
public void traiterDemande(String emailOuUsername) {

    String username;
    String email;

    // ─── Détecter si c'est un email ou un username ───────────────
    if (emailOuUsername.contains("@")) {
        // C'est un email → chercher le username
        username = trouverUsernameParEmail(emailOuUsername);
        email    = emailOuUsername;
    } else {
        // C'est un username → chercher l'email
        email    = trouverEmailParUsername(emailOuUsername);
        username = emailOuUsername;
    }

    if (username == null || email == null) {
        // ✅ Ne pas révéler que l'utilisateur n'existe pas
        System.out.println("⚠️ Demande mdp oublié — inconnu: " + emailOuUsername);
        return;
    }

    // Notifier l'admin
    emailService.envoyerNotificationAdminMdpOublie(adminEmail, username, email);

    // Accusé de réception à l'utilisateur
    emailService.envoyerAccuseReceptionMdpOublie(email, username);

    System.out.println("✅ Demande mdp oublié traitée pour: " + username);
}
    /**
     * Réinitialise le mot de passe d'un utilisateur.
     * Appelé par l'admin depuis son dashboard.
     * 1. Génère un nouveau mot de passe temporaire
     * 2. Met à jour dans Keycloak
     * 3. Envoie le nouveau mot de passe par email
     */
    public void reinitialiserMotDePasse(String username) {

        // 1. Vérifier que l'utilisateur existe dans Keycloak
        if (!keycloakUserService.usernameExists(username)) {
            throw new RuntimeException("Utilisateur introuvable: " + username);
        }

        // 2. Trouver l'email de l'utilisateur
        String email = trouverEmailParUsername(username);
        if (email == null) {
            throw new RuntimeException("Email introuvable pour: " + username);
        }

        // 3. Générer un nouveau mot de passe temporaire
        String nouveauMotDePasse = genererMotDePasse();

        // 4. Mettre à jour dans Keycloak + forcer changement à la connexion
        keycloakUserService.reinitialiserAvecTemporaire(username, nouveauMotDePasse);

        // 5. Envoyer le nouveau mot de passe à l'utilisateur
        emailService.envoyerNouveauMotDePasse(email, username, nouveauMotDePasse);

        System.out.println("✅ Mot de passe réinitialisé pour: " + username);
    }

    // ── Helpers privés ────────────────────────────────────────────

    private String trouverUsernameParEmail(String email) {
        // Chercher dans agents
        return agentRepository.findByEmail(email)
            .map(a -> a.getUsername())
            .orElseGet(() ->
                // Chercher dans validateurs
                validateurRepository.findByEmail(email)
                    .map(v -> v.getUsername())
                    .orElseGet(() ->
                        // Chercher dans prestataires
                        prestataireRepository.findByEmail(email)
                            .map(p -> p.getUsername())
                            .orElse(null)
                    )
            );
    }

    private String trouverEmailParUsername(String username) {
        return agentRepository.findByUsername(username)
            .map(a -> a.getEmail())
            .orElseGet(() ->
                validateurRepository.findByUsername(username)
                    .map(v -> v.getEmail())
                    .orElseGet(() ->
                        prestataireRepository.findByUsername(username)
                            .map(p -> p.getEmail())
                            .orElse(null)
                    )
            );
    }

    private String genererMotDePasse() {
        return "Reset@" + UUID.randomUUID().toString()
                .substring(0, 6).toUpperCase();
    }
}