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

/**
 * ════════════════════════════════════════════════════════════════════
 * KeycloakUserService — Service d'administration des utilisateurs Keycloak
 * ════════════════════════════════════════════════════════════════════
 *
 * Ce service est le pont entre Spring Boot et Keycloak.
 * Il utilise l'API Admin REST de Keycloak via la librairie keycloak-admin-client
 * pour gérer le cycle de vie complet des utilisateurs :
 *
 *   ✅ Création         → createUser()
 *   ✅ Suppression      → deleteUser()
 *   ✅ Mise à jour      → updateUser()
 *   ✅ Mot de passe     → changeUserPassword()
 *   ✅ Activation       → toggleUserStatus()
 *   ✅ Rôles            → assignRoleToUser()
 *   ✅ Email vérif      → sendVerificationEmail()
 *
 * ARCHITECTURE :
 *   Ce service se connecte au realm "master" avec les credentials admin
 *   pour avoir les droits d'administration sur le realm "contentieux-realm".
 *
 * UTILISÉ PAR :
 *   - AgentService          → création/suppression/toggle agents
 *   - ValidateurService     → création/suppression/toggle validateurs
 *   - PrestataireService    → création/suppression/toggle prestataires
 * ════════════════════════════════════════════════════════════════════
 */
@Service
public class KeycloakUserService {

    // ── Instance Keycloak Admin Client ────────────────────────────
    // Initialisée dans @PostConstruct après injection des @Value
    private Keycloak keycloak;
    private String realm;

    // ── Configuration injectée depuis application.properties ──────
    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String serverUrl;          // URL du serveur Keycloak

    @Value("${keycloak.realm:contentieux-realm}")
    private String realmName;          // Realm cible de l'application

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;      // Username admin Keycloak (realm master)

    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;      // Password admin Keycloak (realm master)

    /**
     * Initialisation du client Keycloak Admin après injection des propriétés.
     *
     * ⚠️ IMPORTANT : On se connecte au realm "master" (pas "contentieux-realm")
     * car seul le realm master possède les droits d'administration globaux.
     * Le realm "contentieux-realm" est le realm CIBLE dans lequel on crée
     * les utilisateurs via this.realm.
     */
    @PostConstruct
    public void init() {
        this.realm = realmName;
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master")           // ← connexion admin via realm master
                .username(adminUsername)
                .password(adminPassword)
                .clientId("admin-cli")     // ← client admin Keycloak standard
                .build();
        System.out.println("✅ KeycloakUserService initialisé — Realm: " + realm);
    }

    // ══════════════════════════════════════════════════════════════
    //  CRÉER UTILISATEUR + RÔLE
    // ══════════════════════════════════════════════════════════════

    /**
     * Crée un nouvel utilisateur dans Keycloak et lui assigne un rôle.
     *
     * FLUX D'EXÉCUTION :
     *   1. Vérifier que le username n'existe pas déjà
     *   2. Construire la représentation de l'utilisateur
     *   3. Appeler l'API Keycloak pour créer l'utilisateur
     *   4. Extraire l'ID de l'utilisateur créé depuis la réponse HTTP
     *   5. Définir le mot de passe
     *   6. Assigner le rôle (AGENT, VALIDATEUR_FINANCIER, etc.)
     *
     * ✅ À AJOUTER POUR FORCER CHANGEMENT MDP À LA PREMIÈRE CONNEXION :
     *   user.setRequiredActions(List.of("UPDATE_PASSWORD"));
     *   credential.setTemporary(true);
     *
     * @param username  Identifiant unique de connexion
     * @param email     Email de l'utilisateur
     * @param firstName Prénom
     * @param lastName  Nom
     * @param password  Mot de passe initial (temporaire ou permanent)
     * @param roleName  Rôle Keycloak à assigner (ex: "AGENT", "VALIDATEUR_FINANCIER")
     */
    public void createUser(String username, String email, String firstName,
                           String lastName, String password, String roleName) {
        try {
            // ── Étape 1 : Vérification unicité du username ─────────
            // On vérifie le username mais pas l'email (commenté volontairement)
            // car certains utilisateurs peuvent partager un email dans certains cas
            if (usernameExists(username)) {
                throw new RuntimeException("Nom d'utilisateur déjà existant dans Keycloak");
            }
            // if (emailExists(email)) {
            //     throw new RuntimeException("Email déjà existant dans Keycloak");
            // }

            // ── Étape 2 : Construction de la représentation utilisateur ──
            UserRepresentation user = new UserRepresentation();
            user.setUsername(username);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(true);          // compte actif dès la création
            user.setEmailVerified(false);   // email non vérifié — envoi manuel possible


             // ✅ ACTIVÉ : Forcer changement de mot de passe à la 1ère connexion
        // S'applique à TOUS les rôles : AVOCAT, HUISSIER, EXPERT, AGENT, etc.
        user.setRequiredActions(List.of("UPDATE_PASSWORD"));
        
            // ✅ POUR FORCER CHANGEMENT MDP À LA PREMIÈRE CONNEXION :
            // Décommentez les deux lignes suivantes :
            // user.setRequiredActions(List.of("UPDATE_PASSWORD"));
            // → Keycloak redirige automatiquement vers page changement mdp

            // ── Étape 3 : Appel API Keycloak — création ───────────
            Response response = keycloak.realm(realm).users().create(user);

            // Gestion des erreurs HTTP Keycloak
            if (response.getStatus() == 409) {
                // 409 Conflict = username ou email déjà utilisé dans Keycloak
                throw new RuntimeException("Conflit Keycloak: username ou email déjà utilisé");
            }
            if (response.getStatus() != 201) {
                // 201 Created = seul statut de succès attendu
                throw new RuntimeException("Erreur Keycloak [" + response.getStatus() + "]");
            }

            // ── Étape 4 : Extraction de l'userId depuis l'URL de réponse ──
            // Keycloak retourne l'URL du nouvel utilisateur dans le header Location
            // Format : /auth/admin/realms/{realm}/users/{userId}
            // On extrait la dernière partie avec le regex
            String userId = response.getLocation().getPath()
                    .replaceAll(".*/([^/]+)$", "$1");

            // ── Étape 5 : Définition du mot de passe ──────────────
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(true); // ✅ ACTIVÉ : temporaire = obligatoire de changer
            // ✅ POUR FORCER CHANGEMENT MDP : mettre setTemporary(true)
            // → L'utilisateur DOIT changer son mdp à la première connexion
            // credential.setTemporary(true);

            keycloak.realm(realm).users().get(userId).resetPassword(credential);

            // ── Étape 6 : Assignation du rôle ─────────────────────
            // Le rôle détermine les accès dans l'application :
            // AGENT, VALIDATEUR_FINANCIER, VALIDATEUR_JURIDIQUE, AVOCAT, EXPERT, HUISSIER
            assignRoleToUser(userId, roleName);

        } catch (Exception e) {
            throw new RuntimeException("Erreur création utilisateur: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  VÉRIFICATIONS D'EXISTENCE
    // ══════════════════════════════════════════════════════════════

    /**
     * Vérifie si un username existe déjà dans Keycloak.
     * Utilisé avant création pour éviter les conflits.
     *
     * @param username Username à vérifier
     * @return true si existe, false sinon (ou en cas d'erreur)
     */
    public boolean usernameExists(String username) {
        try {
            // search(username, true) = recherche exacte (exact=true)
            return keycloak.realm(realm)
                    .users()
                    .search(username, true)
                    .stream()
                    .anyMatch(u -> u.getUsername() != null
                            && u.getUsername().equalsIgnoreCase(username));
        } catch (Exception e) {
            // En cas d'erreur réseau → on retourne false pour ne pas bloquer
            return false;
        }
    }

    /**
     * Vérifie si un email existe déjà dans Keycloak.
     * Peut être utilisé pour empêcher les doublons d'email.
     *
     * @param email Email à vérifier
     * @return true si existe, false sinon (ou en cas d'erreur)
     */
    public boolean emailExists(String email) {
        try {
            // searchByEmail(email, true) = recherche exacte
            return keycloak.realm(realm)
                    .users()
                    .searchByEmail(email, true)
                    .stream()
                    .anyMatch(u -> u.getEmail() != null
                            && u.getEmail().equalsIgnoreCase(email));
        } catch (Exception e) {
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ASSIGNER RÔLE
    // ══════════════════════════════════════════════════════════════

    /**
     * Assigne un rôle Realm à un utilisateur Keycloak.
     *
     * ⚠️ NOTE : On assigne des rôles au niveau REALM (realmLevel)
     * et non au niveau CLIENT — ce qui correspond à la configuration
     * de SecurityConfig.java qui lit realm_access.roles dans le JWT.
     *
     * @param userId   ID Keycloak de l'utilisateur (UUID)
     * @param roleName Nom du rôle à assigner (ex: "AGENT")
     */
    public void assignRoleToUser(String userId, String roleName) {
        // Récupérer ou créer le rôle automatiquement
        RoleRepresentation role = getOrCreateRole(roleName);
        keycloak.realm(realm)
                .users()
                .get(userId)
                .roles()
                .realmLevel()              // ← rôle Realm (pas Client)
                .add(Collections.singletonList(role));
        System.out.println("✅ Rôle assigné: " + roleName + " → userId: " + userId);
    }

    // ══════════════════════════════════════════════════════════════
    //  RÉCUPÉRER OU CRÉER LE RÔLE AUTOMATIQUEMENT
    // ══════════════════════════════════════════════════════════════

    /**
     * Récupère un rôle existant ou le crée s'il n'existe pas.
     *
     * Cette approche "get or create" évite les erreurs si un rôle
     * n'a pas été créé manuellement dans Keycloak Admin Console.
     * Utile en environnement de développement ou lors des premiers déploiements.
     *
     * @param roleName Nom du rôle (ex: "VALIDATEUR_FINANCIER")
     * @return RoleRepresentation du rôle trouvé ou créé
     */
    private RoleRepresentation getOrCreateRole(String roleName) {
        try {
            // Tentative de récupération du rôle existant
            RoleRepresentation role = keycloak.realm(realm)
                    .roles().get(roleName).toRepresentation();
            System.out.println("✅ Rôle trouvé: " + roleName);
            return role;
        } catch (Exception e) {
            // Rôle inexistant → création automatique
            System.out.println("⚠️ Rôle '" + roleName + "' inexistant → création automatique");
            RoleRepresentation newRole = new RoleRepresentation();
            newRole.setName(roleName);
            keycloak.realm(realm).roles().create(newRole);
            System.out.println("✅ Rôle créé: " + roleName);
            // Re-récupérer après création car create() ne retourne pas l'objet
            return keycloak.realm(realm).roles().get(roleName).toRepresentation();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ENVOYER EMAIL DE VÉRIFICATION / RESET PASSWORD
    // ══════════════════════════════════════════════════════════════

    /**
     * Envoie un email Keycloak demandant à l'utilisateur de mettre à jour
     * son mot de passe via un lien sécurisé.
     *
     * ⚠️ PRÉREQUIS : Le realm Keycloak doit avoir un serveur SMTP configuré
     * dans Realm Settings → Email pour que cet envoi fonctionne.
     *
     * DIFFÉRENCE avec EmailService.envoyerCredentiels() :
     *   - sendVerificationEmail() = email envoyé PAR Keycloak avec lien sécurisé
     *   - envoyerCredentiels()    = email envoyé PAR Spring Boot avec le mdp en clair
     *
     * @param username Username de l'utilisateur concerné
     */
    public void sendVerificationEmail(String username) {
        try {
            String userId = getUserId(username);
            // executeActionsEmail = déclenche une action utilisateur par email
            // UPDATE_PASSWORD = l'utilisateur reçoit un lien pour changer son mdp
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

    /**
     * Supprime un utilisateur de Keycloak par son username.
     *
     * APPELÉ PAR :
     *   - AgentService.deleteAgent()           → supprime l'agent
     *   - ValidateurService.delete()           → supprime le validateur
     *   - PrestataireService.supprimerPrestataire() → supprime le prestataire
     *
     * ⚠️ Si l'utilisateur n'existe pas dans Keycloak, la méthode
     * ne lève pas d'exception — elle log simplement l'info.
     *
     * @param username Username de l'utilisateur à supprimer
     */
    public void deleteUser(String username) {
        try {
            // Recherche exacte de l'utilisateur par username
            List<UserRepresentation> users = keycloak.realm(realm)
                    .users().search(username, true);
            if (!users.isEmpty()) {
                keycloak.realm(realm).users().delete(users.get(0).getId());
                System.out.println("✅ Utilisateur supprimé: " + username);
            } else {
                System.out.println("⚠️ Utilisateur non trouvé dans Keycloak: " + username);
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur suppression: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  CHANGER MOT DE PASSE
    // ══════════════════════════════════════════════════════════════

    /**
     * Change le mot de passe d'un utilisateur Keycloak.
     *
     * APPELÉ PAR :
     *   - PrestataireService.updatePrestataire() → si nouveau mdp fourni
     *   - PrestatairePasswordController          → changement mdp prestataire
     *
     * ⚠️ setTemporary(false) = le nouveau mdp est permanent
     * Pour forcer un changement à la prochaine connexion : setTemporary(true)
     *
     * @param username    Username de l'utilisateur
     * @param newPassword Nouveau mot de passe en clair (sera hashé par Keycloak)
     */
    public void changeUserPassword(String username, String newPassword) {
        String userId = getUserId(username);
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(false); // false = permanent, true = temporaire
        keycloak.realm(realm).users().get(userId).resetPassword(credential);
        System.out.println("✅ Mot de passe changé pour: " + username);
    }

    /**
     * Alias de changeUserPassword() — conservé pour compatibilité
     * avec l'ancien code.
     *
     * @param username    Username de l'utilisateur
     * @param newPassword Nouveau mot de passe
     */
    public void updatePassword(String username, String newPassword) {
        changeUserPassword(username, newPassword);
    }

    // ══════════════════════════════════════════════════════════════
    //  METTRE À JOUR UTILISATEUR
    // ══════════════════════════════════════════════════════════════

    /**
     * Met à jour les informations de base d'un utilisateur dans Keycloak.
     *
     * ⚠️ NOTE : Le username n'est PAS modifiable ici — il sert d'identifiant
     * unique et ne doit jamais changer après création.
     *
     * APPELÉ PAR :
     *   - PrestataireService.updatePrestataire() → sync Keycloak après modif DB
     *   - AgentService.updateProfile()           → sync Keycloak après modif DB
     *
     * @param username  Username (pour identifier l'utilisateur)
     * @param email     Nouvel email
     * @param firstName Nouveau prénom
     * @param lastName  Nouveau nom
     */
    public void updateUser(String username, String email,
                           String firstName, String lastName) {
        String userId = getUserId(username);
        UserRepresentation user = new UserRepresentation();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        // ⚠️ username non modifié volontairement — identifiant permanent
        keycloak.realm(realm).users().get(userId).update(user);
        System.out.println("✅ Utilisateur mis à jour: " + username);
    }

    // ══════════════════════════════════════════════════════════════
    //  ACTIVER / DÉSACTIVER
    // ══════════════════════════════════════════════════════════════

    /**
     * Active ou désactive un compte utilisateur dans Keycloak.
     *
     * COMPORTEMENT :
     *   - enabled=false → l'utilisateur ne peut plus se connecter
     *                     même avec un token valide (Keycloak rejette le login)
     *   - enabled=true  → l'utilisateur peut à nouveau se connecter
     *
     * APPELÉ PAR :
     *   - AgentService.toggleAgentStatus()       → activation/désactivation agent
     *   - ValidateurService.toggleActif()        → activation/désactivation validateur
     *   - PrestataireService.toggleActif()       → activation/désactivation prestataire
     *
     * ⚠️ IMPORTANT : Cette méthode doit TOUJOURS être appelée en même temps
     * que la mise à jour du champ actif en base de données pour maintenir
     * la cohérence entre Keycloak et la DB.
     *
     * @param username Username de l'utilisateur
     * @param enabled  true = activer, false = désactiver
     */
    public void toggleUserStatus(String username, boolean enabled) {
        try {
            String userId = getUserId(username);

            UserRepresentation user = new UserRepresentation();
            user.setEnabled(enabled); // true = actif, false = inactif

            keycloak.realm(realm).users().get(userId).update(user);
            System.out.println("✅ Keycloak: " + username
                    + " → enabled=" + enabled);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Erreur Keycloak toggleUserStatus: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  UTILITAIRE PRIVÉ
    // ══════════════════════════════════════════════════════════════

    /**
     * Récupère l'ID Keycloak (UUID) d'un utilisateur par son username.
     *
     * ⚠️ USAGE INTERNE UNIQUEMENT — méthode privée utilisée par
     * toutes les autres méthodes du service qui nécessitent l'ID Keycloak.
     *
     * Keycloak identifie les utilisateurs par un UUID interne
     * (ex: "a3f2b1c4-...") et non par leur username — cette méthode
     * fait la conversion username → UUID.
     *
     * @param username Username de l'utilisateur à chercher
     * @return UUID Keycloak de l'utilisateur
     * @throws RuntimeException si l'utilisateur n'est pas trouvé
     */
    private String getUserId(String username) {
        List<UserRepresentation> users = keycloak.realm(realm)
                .users().search(username, true); // exact=true
        if (users.isEmpty())
            throw new RuntimeException("Utilisateur Keycloak non trouvé: " + username);
        return users.get(0).getId();
    }




    /**
 * Réinitialise le mot de passe avec un mot de passe temporaire.
 * Force le changement à la prochaine connexion.
 */
public void reinitialiserAvecTemporaire(String username, String newPassword) {
    String userId = getUserId(username);

    // Mot de passe temporaire
    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(newPassword);
    credential.setTemporary(true); // ✅ force changement à la connexion

    keycloak.realm(realm).users().get(userId).resetPassword(credential);

    // ✅ Ajouter l'action UPDATE_PASSWORD
    UserRepresentation user = keycloak.realm(realm)
            .users().get(userId).toRepresentation();
    user.setRequiredActions(List.of("UPDATE_PASSWORD"));
    keycloak.realm(realm).users().get(userId).update(user);

    System.out.println("✅ Mot de passe temporaire défini pour: " + username);
}
}