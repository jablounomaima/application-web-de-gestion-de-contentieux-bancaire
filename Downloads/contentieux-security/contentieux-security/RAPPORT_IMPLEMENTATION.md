# Rapport d'Implémentation : Système de Gestion du Contentieux Bancaire

Ce rapport détaille les étapes techniques réalisées pour construire l'application de gestion des dossiers de contentieux, de la sécurité à l'interface utilisateur.

## 1. Architecture de Sécurité (Keycloak & JWT)
**Objectif** : Sécuriser l'accès à l'application via une authentification robuste et une gestion des rôles.

*   **Intégration Keycloak** : Configuration de Spring Security pour agir en tant que Resource Server. Utilisation du protocole OIDC.
*   **Hybridation JWT** : Mise en place d'un mécanisme de secours permettant de valider des tokens JWT générés localement si Keycloak n'est pas utilisé.
*   **Contrôle d'Accès (RBAC)** : Utilisation de l'annotation `@PreAuthorize` sur les contrôleurs pour restreindre les actions selon le rôle (ex: `ROLE_AGENT`, `ROLE_VALID_JURIDIQUE`).

## 2. Modélisation des Données (JPA & MySQL)
**Objectif** : Structurer l'information pour refléter les processus bancaires réels.

*   **Dossier** : Entité centrale gérant le statut (`BROUILLON`, `VALIDE`, `RECOUVREMENT`) et les relations avec les autres entités.
*   **Client (Évolutif)** : Implémentation d'une structure flexible pour gérer les **Personnes Physiques** (CIN, Passeport) et les **Personnes Morales** (Raison sociale, RNE, Matricule fiscal).
*   **Relations JPA** :
    *   `OneToMany` entre Agence et Dossier.
    *   `OneToMany` entre Client et Dossier.
    *   `OneToMany` entre Dossier et Garantie.

## 3. Couche Service et Logique Métier
**Objectif** : Implémenter le workflow de décision.

*   **DossierService** : Centralise la création de dossiers, l'ajout de garanties et le processus de validation.
*   **Processus de Validation** :
    1.  L'Agent soumet le dossier.
    2.  Le Validateur Financier vérifie les montants.
    3.  Le Validateur Juridique vérifie les sûretés.
    4.  Le statut passe automatiquement à `VALIDE` après les deux approbations.

## 4. Développement de l'Interface Utilisateur (Frontend)
**Objectif** : Offrir une expérience utilisateur fluide et moderne.

*   **Technologie** : Vanilla JavaScript (Single Page Application via `app.js`) et HTML5/CSS3.
*   **Composants Dynamiques** :
    *   **Modaux** : Utilisation de overlays pour la création de clients et l'affectation de prestataires.
    *   **Gestion des Vues** : Affichage conditionnel selon le rôle de l'utilisateur connecté (Tableau de bord Agent vs Vue Validation).
    *   **Formulaires Intelligents** : Affichage dynamique des champs selon le type de client sélectionné (Physique/Morale).

## 5. Fonctionnalités Spécifiques
*   **Gestion des Agences** : Module d'administration complet pour gérer le parc des agences bancaires.
*   **Design Premium** : Utilisation d'une palette de couleurs "Dark Mode" professionnelle, avec des badges de statut colorés et une typographie "Inter".
*   **Statistiques** : Vue récapitulative des performances (nombre de dossiers, montants en recouvrement).

## 6. Guide de Déploiement
1.  **Base de données** : Création de la base `contentieux_db` sur MySQL (XAMPP).
2.  **Backend** : Compilation via `mvn clean install` et lancement du serveur Spring Boot.
3.  **Frontend** : Accès direct via `http://localhost:8080/index.html`.

---
*Ce rapport a été généré pour documenter l'état actuel de l'application et les choix de conception logicielle.*
