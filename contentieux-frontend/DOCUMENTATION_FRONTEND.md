# Documentation Technique - Frontend Contentieux Bancaire

Cette documentation détaille l'architecture et le rôle de chaque composant/page de l'application Angular 19 pour faciliter la reprise et l'extension du projet par un autre développeur.

## 🏗 Architecture & Stack
- **Framework** : Angular 19 (Standalone Components).
- **Sécurité** : Keycloak-Angular (Authentification OIDC).
- **Style** : Vanilla CSS avec design moderne (Inter Font, Flexbox/Grid).
- **API** : Intégration avec un backend Spring Boot via `HttpClient`.

---

## 🏛 Structure du Projet
L'application suit une structure modulaire par "features" :
- `src/app/core/` : Services globaux (Auth, Notifications, Interceptors).
- `src/app/features/` : Modules métier découpés par rôle utilisateur.
- `src/app/shared/` : Composants et interfaces partagés.

---

## 📄 Description des Pages par Rôle

### 1. Administration (`features/admin`)
*   **AdminDashboardComponent** :
    *   **Rôle** : Centre de contrôle global du système.
    *   **Contenu** : 
        *   Gestion des **Agents** (CRUD).
        *   Gestion des **Agences** bancaires.
        *   Gestion des **Validateurs** (Juridiques et Financiers).
    *   **Fonctionnalités** : Création de comptes, activation/désactivation, filtrage par agence.

### 2. Agent Bancaire (`features/agent`)
*   **AgentDashboardComponent** : 
    *   **Rôle** : Vue d'ensemble de l'activité de l'agent.
    *   **Contenu** : Statistiques sur les dossiers créés et dossiers en attente de validation.
*   **AgentDossiersComponent** :
    *   **Rôle** : Gestion de la base des dossiers.
    *   **Contenu** : Formulaire de création de nouveau dossier (Client Physique/Morale) et liste de recherche.
*   **AgentDossierDetailComponent** :
    *   **Rôle** : Pilotage opérationnel d'un dossier.
    *   **Contenu** : 
        *   Lancement de nouvelles **Prestations** (Judiciaire, Amiable, etc.).
        *   **Désignation de Prestataires** : Interface pour assigner une mission à un Avocat ou un Expert spécifique.
        *   Suivi des missions en cours.

### 3. Avocat (`features/avocat`)
*   **AvocatDashboardComponent** :
    *   **Rôle** : Gestion autonome des affaires judiciaires.
    *   **Contenu** : 
        *   Liste des affaires assignées.
        *   **Gestion des Audiences** : Calendrier CRUD des audiences avec résultats.
        *   **Saisie du Jugement** : Enregistrement des décisions de justice et montants jugés.
        *   **Honoraires** : Soumission du PV de fin de mission et de la facture.

### 4. Prestataires (Expert / Huissier) (`features/prestataire`)
*   **PrestataireDashboardComponent** :
    *   **Rôle** : Exécution des missions techniques.
    *   **Contenu** : 
        *   Liste des missions assignées par les agents.
        *   Soumission directe du texte du rapport (PV) et des références de facturation.

### 5. Validateurs (`features/validateur-juridique` & `features/validateur-financier`)
*   **ValidateurJuridiqueDashboardComponent** :
    *   **Rôle** : Validation de la conformité des rapports.
    *   **Contenu** : Liste des PV soumis par les prestataires avec options "Approuver" ou "Rejeter".
*   **ValidateurFinancierDashboardComponent** :
    *   **Rôle** : Contrôle et mise en paiement.
    *   **Contenu** : Liste des factures d'honoraires soumises. Permet la validation finale pour paiement.

---

## 🔐 Sécurité & Gestion des Rôles
Le contrôle d'accès est centralisé :
1.  **Authentification** : Initialisée dans `core/auth/keycloak.init.ts`.
2.  **Extraction des Rôles** : Réalisée dans `AppComponent` via le parsing du token JWT (`keycloakService.getKeycloakInstance().tokenParsed`).
3.  **Guards** : `AuthGuard` protège les routes en vérifiant les rôles Keycloak requis définis dans `app.routes.ts`.
4.  **Interface Dynamique** : Le menu latéral (`sidebar`) utilise la méthode `hasRole()` pour n'afficher que les menus pertinents.

---

## 📡 Services API (`core/services`)
- **AdminService** : Endpoints de gestion des utilisateurs.
- **DossierService** : Gestion des dossiers, prestations et assignations.
- **AvocatService** : Gestion des audiences et jugements.
- **ValidateurService** : Endpoints de validation (Accept/Reject).
- **NotificationService** : Récupération des alertes système.

---

## 💡 Notes pour le Développeur
- **Variables d'environnement** : L'URL du backend est configurée dans `src/environments/environment.ts`.
- **Modales** : L'application utilise un pattern de modales intégrées dans les templates (via `*ngIf`) pour éviter les rechargements de page complexes.
- **Flux de données** : Les composants utilisent principalement des appels `Observable` directs. Pour des évolutions complexes, envisager l'ajout d'un Store (RxJS/Signals).
