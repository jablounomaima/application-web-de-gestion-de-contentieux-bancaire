package com.example.contentieux_security.config;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class KeycloakUserService {

    private Keycloak keycloak;
    private String realm;

    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String serverUrl;

    @Value("${keycloak.realm:contentieux-realm}")
    private String realmName;

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;

    @PostConstruct
    public void init() {
        this.realm = realmName;
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master")
                .username(adminUsername)
                .password(adminPassword)
                .clientId("admin-cli")
                .build();
        System.out.println("✅ KeycloakUserService initialisé — Realm: " + realm);
    }

    // ══════════════════════════════════════════════════════════════
    //  CRÉER UTILISATEUR + RÔLE
    // ══════════════════════════════════════════════════════════════

    public void createUser(String username, String email, String firstName,
                           String lastName, String password, String roleName) {
        try {
            UserRepresentation user = new UserRepresentation();
            user.setUsername(username);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(true);
            user.setEmailVerified(true);

            Response response = keycloak.realm(realm).users().create(user);
            if (response.getStatus() != 201)
                throw new RuntimeException("Erreur Keycloak [" + response.getStatus() + "]");

            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

            // Mot de passe temporaire
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(true);
            keycloak.realm(realm).users().get(userId).resetPassword(credential);

            // Assigner le rôle
            assignRoleToUser(userId, roleName);

        } catch (Exception e) {
            throw new RuntimeException("Erreur création utilisateur: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ASSIGNER RÔLE
    // ══════════════════════════════════════════════════════════════

    public void assignRoleToUser(String userId, String roleName) {
        RoleRepresentation role = getOrCreateRole(roleName);
        keycloak.realm(realm)
                .users()
                .get(userId)
                .roles()
                .realmLevel()
                .add(Collections.singletonList(role));
        System.out.println("✅ Rôle assigné: " + roleName + " → userId: " + userId);
    }

    // ══════════════════════════════════════════════════════════════
    //  RÉCUPÉRER OU CRÉER LE RÔLE AUTOMATIQUEMENT
    // ══════════════════════════════════════════════════════════════

    private RoleRepresentation getOrCreateRole(String roleName) {
        try {
            RoleRepresentation role = keycloak.realm(realm)
                    .roles().get(roleName).toRepresentation();
            System.out.println("✅ Rôle trouvé: " + roleName);
            return role;
        } catch (Exception e) {
            System.out.println("⚠️ Rôle '" + roleName + "' inexistant → création automatique");
            RoleRepresentation newRole = new RoleRepresentation();
            newRole.setName(roleName);
            keycloak.realm(realm).roles().create(newRole);
            System.out.println("✅ Rôle créé: " + roleName);
            return keycloak.realm(realm).roles().get(roleName).toRepresentation();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ENVOYER EMAIL
    // ══════════════════════════════════════════════════════════════

    public void sendVerificationEmail(String username) {
        try {
            String userId = getUserId(username);
            keycloak.realm(realm).users().get(userId)
                    .executeActionsEmail(Arrays.asList("UPDATE_PASSWORD"));
            System.out.println("✅ Email envoyé à: " + username);
        } catch (Exception e) {
            System.out.println("⚠️ Email non envoyé pour " + username + ": " + e.getMessage());
            throw new RuntimeException("Erreur envoi email", e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SUPPRIMER UTILISATEUR
    // ══════════════════════════════════════════════════════════════

    public void deleteUser(String username) {
        try {
            List<UserRepresentation> users = keycloak.realm(realm)
                    .users().search(username, true);
            if (!users.isEmpty()) {
                keycloak.realm(realm).users().delete(users.get(0).getId());
                System.out.println("✅ Utilisateur supprimé: " + username);
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur suppression: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  CHANGER MOT DE PASSE
    // ══════════════════════════════════════════════════════════════

    public void changeUserPassword(String username, String newPassword) {
        String userId = getUserId(username);
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(false);
        keycloak.realm(realm).users().get(userId).resetPassword(credential);
        System.out.println("✅ Mot de passe changé pour: " + username);
    }

    public void updatePassword(String username, String newPassword) {
        changeUserPassword(username, newPassword);
    }

    // ══════════════════════════════════════════════════════════════
    //  METTRE À JOUR UTILISATEUR
    // ══════════════════════════════════════════════════════════════

    public void updateUser(String username, String email, String firstName, String lastName) {
        String userId = getUserId(username);
        UserRepresentation user = new UserRepresentation();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        keycloak.realm(realm).users().get(userId).update(user);
        System.out.println("✅ Utilisateur mis à jour: " + username);
    }

    // ══════════════════════════════════════════════════════════════
    //  ACTIVER / DÉSACTIVER
    // ══════════════════════════════════════════════════════════════

    public void toggleUserStatus(String username, boolean enabled) {
        String userId = getUserId(username);
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(enabled);
        keycloak.realm(realm).users().get(userId).update(user);
        System.out.println("✅ Statut mis à jour: " + username + " (enabled=" + enabled + ")");
    }

    // ══════════════════════════════════════════════════════════════
    //  UTILITAIRE PRIVÉ
    // ══════════════════════════════════════════════════════════════

    private String getUserId(String username) {
        List<UserRepresentation> users = keycloak.realm(realm)
                .users().search(username, true);
        if (users.isEmpty())
            throw new RuntimeException("Utilisateur Keycloak non trouvé: " + username);
        return users.get(0).getId();
    }
}