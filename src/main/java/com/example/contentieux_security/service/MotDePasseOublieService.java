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
    private final NotificationService       notificationService;

    @Value("${app.admin.email:admin@banque.tn}")
    private String adminEmail;

    @Value("${app.admin.username:admin1}")
    private String adminUsername;

    // ══════════════════════════════════════════════════════════════
    // Traiter demande mot de passe oublié
    // Accepte email OU username
    // ══════════════════════════════════════════════════════════════
    public void traiterDemande(String emailOuUsername) {

        System.out.println("🔍 traiterDemande appelé: [" + emailOuUsername + "]");

        String username;
        String email;

        // ─── Détecter si c'est un email ou un username ───────────
        if (emailOuUsername.contains("@")) {
            username = trouverUsernameParEmail(emailOuUsername);
            email    = emailOuUsername;
        } else {
            email    = trouverEmailParUsername(emailOuUsername);
            username = emailOuUsername;
        }

        if (username == null || email == null) {
            System.out.println("⚠️ Demande mdp oublié — inconnu: " + emailOuUsername);
            return;
        }

        System.out.println("✅ Utilisateur trouvé — username: " + username + " | email: " + email);

        // ─── Email à l'admin ─────────────────────────────────────
        try {
            emailService.envoyerNotificationAdminMdpOublie(adminEmail, username, email);
            System.out.println("✅ Email admin envoyé pour: " + username);
        } catch (Exception e) {
            System.err.println("❌ Erreur email admin: " + e.getMessage());
        }

        // ─── Notification WebSocket à l'admin ────────────────────
        try {
            // ✅ Détecter le type d'utilisateur → bon onglet
            String tab = "agents";  // défaut
            if (validateurRepository.findByUsername(username).isPresent()) {
                tab = "validateurs";
            }
            notificationService.notifierSansDossier(
                adminUsername,
                "🔐 Demande réinitialisation mot de passe",
                "L'utilisateur " + username + " (" + email
                + ") a oublié son mot de passe.",
                "MOT_DE_PASSE_OUBLIE",
                "/admin/dashboard?tab=" + tab + "&username=" + username
            );
            System.out.println("✅ Notification WS admin envoyée pour: " + username);
        } catch (Exception e) {
            System.err.println("❌ Erreur notification WS: " + e.getMessage());
        }

        // ─── Accusé de réception à l'utilisateur ─────────────────
        try {
            emailService.envoyerAccuseReceptionMdpOublie(email, username);
            System.out.println("✅ Demande mdp oublié traitée pour: " + username);
        } catch (Exception e) {
            System.err.println("❌ Erreur accusé réception pour " + username + ": " + e.getMessage());
            throw new RuntimeException("Échec envoi email à l'utilisateur: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Réinitialiser mot de passe — appelé par l'admin
    // ══════════════════════════════════════════════════════════════
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

    // ══════════════════════════════════════════════════════════════
    // Helpers privés
    // ══════════════════════════════════════════════════════════════

    private String trouverUsernameParEmail(String email) {
        return agentRepository.findByEmail(email)
            .map(a -> a.getUsername())
            .orElseGet(() ->
                validateurRepository.findByEmail(email)
                    .map(v -> v.getUsername())
                    .orElseGet(() ->
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