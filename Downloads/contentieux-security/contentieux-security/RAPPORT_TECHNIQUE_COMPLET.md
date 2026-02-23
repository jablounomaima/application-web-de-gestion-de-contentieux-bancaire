# Rapport Technique Complet
## Application Web de Gestion du Contentieux Bancaire
### Spring Boot 3 + JWT + MySQL + Vanilla JavaScript (SPA)

---

> **Auteur :** Projet académique  
> **Date :** Février 2026  
> **Technologies :** Java 21 · Spring Boot 3.5 · Spring Security 6 · JJWT · JPA/Hibernate · MySQL · JavaScript (SPA)

---

## Table des Matières

1. [Introduction et Contexte du Projet](#1-introduction-et-contexte-du-projet)
2. [Architecture Globale](#2-architecture-globale)
3. [Configuration du Projet (pom.xml)](#3-configuration-du-projet-pomxml)
4. [Configuration de l'Application (application.properties)](#4-configuration-de-lapplication-applicationproperties)
5. [Modélisation des Données (Entités JPA)](#5-modélisation-des-données-entités-jpa)
6. [Couche Repository (Accès aux Données)](#6-couche-repository-accès-aux-données)
7. [Couche Service (Logique Métier)](#7-couche-service-logique-métier)
8. [Architecture de Sécurité (JWT Hybride)](#8-architecture-de-sécurité-jwt-hybride)
9. [Couche Contrôleur (API REST)](#9-couche-contrôleur-api-rest)
10. [Gestion des Utilisateurs Externes](#10-gestion-des-utilisateurs-externes)
11. [Interface Utilisateur (Frontend SPA)](#11-interface-utilisateur-frontend-spa)
12. [Initialisation des Données](#12-initialisation-des-données)
13. [Flux Complets de l'Application](#13-flux-complets-de-lapplication)
14. [Justification des Choix Techniques](#14-justification-des-choix-techniques)
15. [Guide de Déploiement](#15-guide-de-déploiement)

---

## 1. Introduction et Contexte du Projet

### 1.1 Problématique Métier

Les banques tunisiennes gèrent un volume important de **contentieux** (dossiers de créances impayées ou de litiges juridiques). Ce processus implique plusieurs acteurs avec des responsabilités distinctes :

| Acteur | Rôle |
|--------|------|
| **Agent Bancaire** | Crée et gère les dossiers de contentieux |
| **Validateur Financier** | Vérifie les aspects financiers (montants, risques) |
| **Validateur Juridique** | Vérifie les aspects légaux (garanties, sûretés) |
| **Avocat** | Assure la représentation légale |
| **Huissier** | Exécute les actes de recouvrement |
| **Expert** | Évalue les biens ou situations |

### 1.2 Objectifs Techniques

L'application doit :
- **Sécuriser** l'accès selon le rôle de l'utilisateur (RBAC)
- **Gérer** un workflow de validation multi-étapes
- **Fournir** une interface moderne et réactive
- **Prendre en charge** deux modes d'authentification (Keycloak ou JWT local)

---

## 2. Architecture Globale

### 2.1 Pattern Architectural : MVC en 3 couches

```
┌─────────────────────────────────────────────────────────┐
│                   FRONTEND (SPA)                        │
│         index.html + app.js + style.css                 │
│   Vanilla JavaScript · Fetch API · LocalStorage JWT    │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP/REST (JSON)
                         │ Header: Authorization: Bearer <JWT>
┌────────────────────────▼────────────────────────────────┐
│              BACKEND (Spring Boot 3)                    │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │ Controllers │  │   Services   │  │  Repositories │  │
│  │  (API REST) │→ │  (Business)  │→ │  (JPA/Hibernate│  │
│  └─────────────┘  └──────────────┘  └───────────────┘  │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │         Spring Security (JWT Filter)            │   │
│  │  • Décoder le token JWT                        │   │
│  │  • Extraire le rôle → SecurityContext           │   │
│  │  • Bloquer l'accès si rôle insuffisant          │   │
│  └─────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────┘
                         │ JDBC / JPA
┌────────────────────────▼────────────────────────────────┐
│              BASE DE DONNÉES (MySQL)                    │
│   Tables : agent_bancaire · utilisateur · dossiers     │
│            clients · agences · risques · garanties     │
│            missions · prestations · affaires_judiciaires│
└─────────────────────────────────────────────────────────┘
```

### 2.2 Structure des Packages

```
com.example.contentieux_security/
├── ContentieuxSecurityApplication.java  ← Point d'entrée Spring Boot
├── config/
│   └── DataInitializer.java             ← Données initiales (agences, clients)
├── controller/                          ← Endpoints REST (couche Présentation)
│   ├── AuthController.java              ← Login local
│   ├── AdminController.java             ← Gestion admin (agences, agents)
│   ├── AgentController.java             ← Tableau de bord agent
│   ├── DossierController.java           ← CRUD dossiers + validation
│   ├── ClientController.java            ← CRUD clients
│   ├── ValidationController.java        ← Endpoints de validation
│   ├── UtilisateurController.java        ← CRUD utilisateurs externes
│   ├── AvocatController.java            ← Espace avocat
│   ├── HuissierController.java          ← Espace huissier
│   └── ExpertController.java            ← Espace expert
├── dto/                                 ← Data Transfer Objects
│   ├── LoginRequest.java
│   ├── DossierRequest.java
│   ├── RisqueRequest.java
│   ├── PrestationRequest.java
│   └── MissionRequest.java
├── entity/                              ← Entités JPA (couche Modèle)
│   ├── AgentBancaire.java
│   ├── Utilisateur.java
│   ├── RoleUtilisateur.java (enum)
│   ├── Dossier.java
│   ├── DossierStatus.java (enum)
│   ├── Client.java
│   ├── Agence.java
│   ├── Risque.java
│   ├── Garantie.java
│   ├── Mission.java
│   ├── Prestation.java
│   └── AffaireJudiciaire.java
├── repository/                          ← Interfaces JPA Repository
├── security/
│   └── SecurityConfig.java             ← Configuration Spring Security
└── service/                            ← Logique métier
    ├── AuthService.java                 ← Génération JWT
    ├── DossierService.java              ← Workflow dossiers
    ├── AdminService.java                ← Stats et administration
    └── ClientService.java               ← Gestion clients
```

---

## 3. Configuration du Projet (pom.xml)

### 3.1 Pourquoi ces dépendances ?

```xml
<!-- Spring Boot Starter Web : Serveur HTTP embarqué Tomcat + Jackson (JSON) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Security : Filtre de sécurité, annotations @PreAuthorize -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- OAuth2 Resource Server : Support natif JWT dans Spring Security 6 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<!-- Spring Data JPA : Couche d'abstraction ORM, repositories automatiques -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- MySQL Connector : Driver JDBC pour MySQL (XAMPP) -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- JJWT (io.jsonwebtoken) : Création et signature des tokens JWT locaux -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId> <!-- Implémentation runtime -->
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId> <!-- Sérialisation JSON du JWT -->
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>

<!-- Lombok : Génération automatique de getters/setters/constructeurs -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

**Justification** : L'utilisation de `spring-boot-starter-oauth2-resource-server` permet de traiter l'application comme un **Resource Server** qui valide les tokens JWT — qu'ils viennent de Keycloak ou soient générés localement.

---

## 4. Configuration de l'Application (application.properties)

```properties
# Port du serveur
server.port=8097

# Source d'autorité JWT (Keycloak) - utilisé comme fallback principal
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/contentieux-realm

# Base de données MySQL (XAMPP)
spring.datasource.url=jdbc:mysql://localhost:3307/contentieux_db9?createDatabaseIfNotExist=true
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=

# Hibernate : mise à jour automatique du schéma sans perte de données
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

**Points clés :**
- `ddl-auto=update` : Hibernate lit les entités `@Entity` et **crée ou modifie** les tables en conséquence. Aucun script SQL manuel n'est nécessaire.
- `createDatabaseIfNotExist=true` : La base de données est créée automatiquement si elle n'existe pas.
- Le port `8097` évite les conflits avec Keycloak (port 8080).

---

## 5. Modélisation des Données (Entités JPA)

### 5.1 Entité `AgentBancaire`

```java
@Entity
@Table(name = "agent_bancaire")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgentBancaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incrément MySQL
    private Long id;

    @Column(nullable = false, unique = true) // Index unicité en BDD
    private String username;

    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String password; // Stocké en clair (simplification académique)

    @ManyToOne                          // Relation Many-to-One
    @JoinColumn(name = "agence_id")    // Clé étrangère dans la table
    private Agence agence;
}
```

**Explications :**
- `@Entity` : Mappe la classe Java à une table MySQL
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` : Utilise l'auto-incrément de MySQL
- `@ManyToOne` : Plusieurs agents peuvent appartenir à une même agence
- `@JoinColumn(name = "agence_id")` : Nom explicite de la colonne de clé étrangère

### 5.2 Entité `Utilisateur` (Acteurs Externes)

```java
@Entity
@Table(name = "utilisateur")
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    private String nom, prenom, email, telephone, password;

    @Enumerated(EnumType.STRING)            // Stocké comme texte "AVOCAT", pas 0,1,2
    @Column(name = "role_utilisateur", nullable = false) // "role" est réservé dans MySQL 8
    private RoleUtilisateur role;

    private String specialite;
}
```

**Note importante :** Le nom de colonne `role_utilisateur` est nécessaire car `ROLE` est un **mot-clé réservé** dans MySQL 8.x. Si on utilisait `role` directement, Hibernate générerait une requête SQL invalide. C'est un exemple de conflit entre nommage Java et SQL.

### 5.3 Enum `RoleUtilisateur`

```java
public enum RoleUtilisateur {
    AVOCAT,
    HUISSIER,
    EXPERT,
    VALIDATEUR_FINANCIER,
    VALIDATEUR_JURIDIQUE
}
```

**Pourquoi un enum ?** Les enums en Java offrent une validation à la compilation — il est impossible d'assigner une valeur non définie. Avec `@Enumerated(EnumType.STRING)`, la valeur stockée en BDD est lisible ("AVOCAT" plutôt qu'un entier 0, 1, 2).

### 5.4 Entité `Dossier` (Entité Centrale)

```java
@Entity
@Table(name = "dossiers")
public class Dossier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroDossier;       // Format : "DOS-A1B2C3D4"

    @Enumerated(EnumType.STRING)
    private DossierStatus statut;       // BROUILLON → ATTENTE_VALIDATION → VALIDE

    private LocalDateTime dateCreation;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;              // Le client concerné par le contentieux

    @ManyToOne
    @JoinColumn(name = "agence_id")
    private Agence agence;              // L'agence qui gère le dossier

    private String crééPar;            // Username de l'agent créateur

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<Risque> risques;      // Risques financiers associés

    private boolean validationFinanciere;  // Flag de validation financière
    private boolean validationJuridique;   // Flag de validation juridique
}
```

**Workflow d'état :**
```
BROUILLON → (Agent soumet) → ATTENTE_VALIDATION → (2 validations) → VALIDE
```

La double validation est un **pattern de workflow métier** : les deux validateurs (financier et juridique) sont indépendants. `validationFinanciere` et `validationJuridique` sont deux booléens indépendants — le statut `VALIDE` n'est attribué que quand les deux sont `true`.

### 5.5 Entité `Client` (Flexibilité Physique/Morale)

```java
public class Client {
    private String type;           // "PHYSIQUE" ou "MORALE"

    // Champs Personne Physique
    private String cin;            // Carte d'Identité Nationale (Tunisie)
    private String passeport;
    private String carteSejour;
    private String nom, prenom;
    private String dateNaissance;
    private String adresse;

    // Champs Personne Morale (Entreprise)
    private String raisonSociale;  // Nom légal de l'entreprise
    private String rne;            // Registre National des Entreprises
    private String matriculeFiscal;
    private String adresseSiege;
    private String representantLegal;
}
```

**Choix de concéption :** On aurait pu utiliser l'héritage JPA (`@Inheritance`) avec des sous-classes `ClientPhysique` et `ClientMorale`. Cependant, on a préféré une **table unique avec champs optionnels** car :
- Plus simple à maintenir
- Pas de jointures complexes
- Le champ `type` discrimine les cas dans le code

### 5.6 Enum `DossierStatus`

```java
public enum DossierStatus {
    BROUILLON,
    ATTENTE_VALIDATION,
    VALIDE,
    REJETE,
    RECOUVREMENT
}
```

---

## 6. Couche Repository (Accès aux Données)

### 6.1 Principe des Repositories Spring Data JPA

```java
public interface DossierRepository extends JpaRepository<Dossier, Long> {
    // findAll(), findById(), save(), delete() → GÉNÉRÉS AUTOMATIQUEMENT
    // Spring Data JPA crée l'implémentation à l'exécution par proxy dynamique
}
```

**Magie de Spring Data JPA :** On déclare une interface qui étend `JpaRepository<T, ID>` et Spring génère automatiquement, par réflexion Java, une implémentation complète contenant toutes les opérations CRUD. On n'écrit aucune ligne SQL.

### 6.2 Repository avec Requête Personnalisée

```java
public interface AgentBancaireRepository extends JpaRepository<AgentBancaire, Long> {
    Optional<AgentBancaire> findByUsername(String username);
    // Spring analyse "findByUsername" → génère : SELECT * FROM agent_bancaire WHERE username = ?
}
```

### 6.3 Explication de `Optional<T>`

`Optional` est un conteneur Java 8+ qui évite les `NullPointerException`. Au lieu de retourner `null` si l'entité n'est pas trouvée, on retourne un `Optional.empty()`. Cela force le code appelant à gérer explicitement le cas "non trouvé" :
```java
agentRepository.findByUsername("omaima")
    .orElseThrow(() -> new RuntimeException("Agent non trouvé"));
    // Lève une exception explicite plutôt qu'un NPE aléatoire
```

---

## 7. Couche Service (Logique Métier)

### 7.1 `DossierService` — Le Cœur du Workflow

#### Création d'un Dossier

```java
@Transactional
public Dossier createDossier(Long clientId, Long agenceId, String agentUsername) {
    Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new RuntimeException("Client non trouvé"));

    // Si l'agence n'est pas précisée, utiliser celle de l'agent
    Agence agence = agenceId != null
        ? agenceRepository.findById(agenceId).orElseThrow(...)
        : agentBancaireRepository.findByUsername(agentUsername)
                                 .orElseThrow(...).getAgence();

    Dossier dossier = Dossier.builder()
            .numeroDossier("DOS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .client(client)
            .agence(agence)
            .statut(DossierStatus.BROUILLON)   // Toujours commencer en BROUILLON
            .dateCreation(LocalDateTime.now())
            .crééPar(agentUsername)            // Traçabilité : qui a créé le dossier
            .validationFinanciere(false)
            .validationJuridique(false)
            .build();

    return dossierRepository.save(dossier);
}
```

**Explication `@Transactional` :**  
Cette annotation garantit que toutes les opérations BDD dans la méthode sont atomiques : soit tout réussit, soit tout est annulé (rollback). Si `dossierRepository.save()` échoue après que `clientRepository.findById()` a réussi, aucune modification ne persistera en base.

**Explication du numéro de dossier :**  
`UUID.randomUUID().toString().substring(0, 8).toUpperCase()` génère un identifiant aléatoire unique de 8 caractères hexadécimaux (ex: "A1B2C3D4"), formaté comme "DOS-A1B2C3D4". Cela évite d'avoir des IDs séquentiels prédictibles.

#### Workflow de Double Validation

```java
@Transactional
public void validateFinanciere(Long dossierId) {
    Dossier dossier = dossierRepository.findById(dossierId).orElseThrow(...);
    dossier.setValidationFinanciere(true);  // Marquer la validation financière
    checkFinalValidation(dossier);          // Vérifier si les deux validations sont faites
}

@Transactional
public void validateJuridique(Long dossierId) {
    Dossier dossier = dossierRepository.findById(dossierId).orElseThrow(...);
    dossier.setValidationJuridique(true);   // Marquer la validation juridique
    checkFinalValidation(dossier);          // Même vérification
}

private void checkFinalValidation(Dossier dossier) {
    // Pattern : valider seulement quand les DEUX conditions sont réunies
    if (dossier.isValidationFinanciere() && dossier.isValidationJuridique()) {
        dossier.setStatut(DossierStatus.VALIDE);
        dossierRepository.save(dossier);
    }
}
```

**Design Pattern :** C'est une implémentation du pattern **State Machine** — le dossier transite d'un état à l'autre selon des conditions précises. La méthode privée `checkFinalValidation` encapsule la règle métier : "les deux validations sont nécessaires".

### 7.2 `AuthService` — Génération de Tokens JWT

```java
@Service
public class AuthService {
    // Clé secrète de 32 octets = algorithme HS256
    // IMPORTANT : 32 octets = 256 bits = taille exacte requise pour HS256
    private final String SECRET = "contentieux-secret-key-32-chars!";
    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
    private final long EXPIRATION_TIME = 86400000; // 24 heures en millisecondes

    public String generateToken(String username, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();

        // *** Format Keycloak simulé ***
        // Le décodeur JWT Spring Security 6 lit les rôles dans "realm_access.roles"
        // (structure spécifique à Keycloak). On reproduit cette structure pour
        // que le même décodeur fonctionne avec les tokens locaux ET Keycloak.
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", roles);
        claims.put("realm_access", realmAccess);
        claims.put("preferred_username", username); // Identifiant de l'utilisateur

        return Jwts.builder()
                .setClaims(claims)           // Payload du JWT
                .setSubject(username)        // Champ standard "sub"
                .setIssuedAt(new Date())     // Champ standard "iat"
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // "exp"
                .signWith(key, SignatureAlgorithm.HS256) // Signature HMAC-SHA256
                .compact();                  // Encode en Base64Url : header.payload.signature
    }
}
```

**Anatomie d'un JWT :**
```
eyJhbGciOiJIUzI1NiJ9                           ← Header (Base64Url)
.eyJzdWIiOiJvbWFpbWEiLCJyZWFsbV9hY2Nlc3MiOn... ← Payload (Base64Url)
.xK9mBzXvT2a...                                ← Signature HMAC-SHA256
```

**Pourquoi HS256 (et pas HS512) ?**  
L'algorithme HMAC utilisé dépend de la longueur de la clé :
- 32 octets (256 bits) → **HS256**
- 48 octets (384 bits) → **HS384**
- 64 octets (512 bits) → **HS512**

Notre `NimbusJwtDecoder` (côté Spring Security) construit avec `withSecretKey()` utilise par défaut **HS256**. Si la clé fait plus de 32 octets, JJWT choisit automatiquement HS512, créant une **incompatibilité** avec le décodeur. La clé de 32 caractères exactement garantit l'alignement des algorithmes.

---

## 8. Architecture de Sécurité (JWT Hybride)

### 8.1 Vue d'Ensemble du Flux de Sécurité

```
Requête HTTP
    │
    ▼
Spring Security Filter Chain
    │
    ├─ Chemin public ? → /index.html, /app.js, /api/auth/login
    │       └─ Autoriser sans token
    │
    └─ Chemin protégé ?
            │
            ▼
        JwtDecoder (double décodage)
            ├─ Essai Keycloak decoder → échec si Keycloak absent
            └─ Fallback → Local decoder (HMAC-SHA256)
                    │
                    ▼
            JwtAuthenticationConverter
                    │ Extrait realm_access.roles → ['AGENT']
                    │ Crée SimpleGrantedAuthority('ROLE_AGENT')
                    │
                    ▼
            SecurityContext
                    │
                    ▼
                @PreAuthorize("hasRole('AGENT')") → OK / Forbidden
```

### 8.2 `SecurityConfig.java` — Décodeur JWT Hybride

```java
@Bean
public org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
    String issuerUri = "http://localhost:8080/realms/contentieux-realm";

    // Décodeur Keycloak : valide avec la clé publique JWKS de Keycloak
    NimbusJwtDecoder keycloakDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

    // Décodeur local : valide avec notre clé secrète symétrique
    NimbusJwtDecoder localDecoder = NimbusJwtDecoder
            .withSecretKey((javax.crypto.SecretKey) authService.getSigningKey())
            .build();

    // Stratégie "Try Keycloak first, then local"
    return token -> {
        try {
            return keycloakDecoder.decode(token); // Keycloak en production
        } catch (Exception e) {
            return localDecoder.decode(token);    // Local en développement
        }
    };
}
```

**Justification du décodeur hybride :** En production, Keycloak serait le gestionnaire d'identité central. En développement, Keycloak n'est pas toujours démarré. Le décodeur hybride permet les **deux modes sans modifier le code** : si Keycloak est disponible, il est utilisé en priorité ; sinon, le décodeur local prend le relais.

### 8.3 Extraction des Rôles depuis le JWT

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
        // Lire la structure Keycloak : { "realm_access": { "roles": ["AGENT"] } }
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        List<String> roles;

        if (realmAccess != null && realmAccess.get("roles") != null) {
            roles = (List<String>) realmAccess.get("roles");
        } else {
            roles = jwt.getClaim("roles"); // Format alternatif
            if (roles == null) roles = Collections.emptyList();
        }

        // Spring Security préfixe les rôles avec "ROLE_"
        // "AGENT" devient "ROLE_AGENT" → @PreAuthorize("hasRole('AGENT')") fonctionne
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    });
    return converter;
}
```

**Convention Spring Security :** L'annotation `hasRole('AGENT')` vérifie l'existence de `ROLE_AGENT` dans les autorités. Le préfixe `ROLE_` est ajouté automatiquement par `hasRole()` mais pas par `hasAuthority()`. On ajoute le préfixe dans le converter pour maintenir la compatibilité.

### 8.4 Règles d'Autorisation

```java
.authorizeHttpRequests(auth -> auth
    // Ressources publiques : accessibles sans authentication
    .requestMatchers("/", "/index.html", "/style.css", "/app.js",
                    "/public/**", "/api/auth/login", "/h2-console/**")
    .permitAll()
    // Tout le reste : authentication JWT requise
    .anyRequest().authenticated()
)
```

Les endpoints spécifiques sont ensuite sécurisés par `@PreAuthorize` au niveau des méthodes :

| Endpoint | Rôle requis |
|----------|-------------|
| `POST /api/dossiers` | `AGENT` |
| `POST /api/dossiers/{id}/validate-fin` | `VALID_FINANCIER` |
| `POST /api/dossiers/{id}/validate-jur` | `VALID_JURIDIQUE` |
| `GET /api/admin/stats` | `ADMIN` |
| `GET /avocat/dashboard` | `AVOCAT` |
| `POST /api/utilisateurs` | Authentifié (AGENT) |

---

## 9. Couche Contrôleur (API REST)

### 9.1 `AuthController` — Authentification Hybride

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AgentBancaireRepository agentBancaireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        // === ÉTAPE 1 : Vérifier dans AgentBancaire ===
        AgentBancaire agent = agentBancaireRepository.findAll().stream()
                .filter(a -> a.getUsername().equals(request.getUsername()))
                .findFirst().orElse(null);

        if (agent != null && agent.getPassword().equals(request.getPassword())) {
            String token = authService.generateToken(agent.getUsername(),
                                                     List.of("AGENT"));
            return ResponseEntity.ok(Map.of(
                "access_token", token,
                "username", agent.getUsername(),
                "role", "AGENT"
            ));
        }

        // === ÉTAPE 2 : Vérifier dans Utilisateur (acteurs externes) ===
        Utilisateur utilisateur = utilisateurRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(request.getUsername()))
                .findFirst().orElse(null);

        if (utilisateur != null && utilisateur.getPassword().equals(request.getPassword())) {
            String roleStr = mapRole(utilisateur.getRole().name());
            // mapRole convertit VALIDATEUR_FINANCIER → VALID_FINANCIER (attendu par le frontend)
            String token = authService.generateToken(utilisateur.getUsername(), List.of(roleStr));
            return ResponseEntity.ok(Map.of(
                "access_token", token,
                "username", utilisateur.getUsername(),
                "role", roleStr,
                "nom", utilisateur.getNom(),
                "prenom", utilisateur.getPrenom()
            ));
        }

        return ResponseEntity.status(401).body("Identifiants incorrects");
    }

    // Mapping entre l'enum Java et le rôle attendu par Spring Security / Frontend
    private String mapRole(String roleEnum) {
        return switch (roleEnum) {
            case "VALIDATEUR_FINANCIER" -> "VALID_FINANCIER";
            case "VALIDATEUR_JURIDIQUE" -> "VALID_JURIDIQUE";
            default -> roleEnum; // AVOCAT, HUISSIER, EXPERT → inchangé
        };
    }
}
```

**Pourquoi deux tables séparées (AgentBancaire vs Utilisateur) ?**  
Les agents bancaires sont des **employés internes** de la banque — ils ont un lien organisationnel avec une agence. Les acteurs externes (avocats, huissiers, experts) sont des **partenaires externes** sans affectation organisationnelle. Traiter ces deux populations différemment permet d'avoir des attributs spécifiques à chaque type (agence pour l'agent, spécialité pour l'expert).

### 9.2 `DossierController` — API des Dossiers

```java
@PostMapping
@PreAuthorize("hasRole('AGENT')")
public ResponseEntity<Dossier> createDossier(@RequestBody DossierRequest request,
        @AuthenticationPrincipal Jwt jwt) {
    // @AuthenticationPrincipal Jwt jwt → injection automatique du token décodé
    // jwt.getClaimAsString("preferred_username") → récupère le username depuis le JWT
    String username = jwt.getClaimAsString("preferred_username");
    return ResponseEntity.ok(dossierService.createDossier(
            request.getClientId(), request.getAgenceId(), username));
}
```

**`@AuthenticationPrincipal Jwt jwt` :** Spring Security injecte automatiquement l'objet JWT décodé dans le paramètre de méthode. Cela évite de parser manuellement le header Authorization, et garantit que le token est valide (sinon, la requête n'aurait pas passé le filtre de sécurité).

### 9.3 DTO (Data Transfer Objects)

```java
// DTO pour la création de dossier
public class DossierRequest {
    private Long clientId;   // Référence au client existant
    private Long agenceId;   // Référence à l'agence (optionnel)
}
```

**Pourquoi des DTOs et non les entités directement ?**  
Envoyer l'entité `Dossier` directement dans le body de la requête poserait des problèmes :
1. **Sécurité** : le client pourrait injecter des champs non attendus (ex: `statut = VALIDE`)
2. **Circularité JSON** : les relations `@OneToMany` / `@ManyToOne` créent des cycles infinis lors de la sérialisation Jackson
3. **Découplage** : le contrat API est indépendant du modèle de données interne

---

## 10. Gestion des Utilisateurs Externes

### 10.1 `UtilisateurController` — CRUD avec Gestion d'Erreurs

```java
@PostMapping
public ResponseEntity<?> createUtilisateur(@RequestBody Utilisateur utilisateur) {
    try {
        Utilisateur saved = utilisateurRepository.save(utilisateur);
        return ResponseEntity.ok(saved);
    } catch (Exception e) {
        // Capturer toute exception (contrainte unique, SQLSyntaxError, etc.)
        // et retourner un message détaillé au lieu d'une erreur 500 opaque
        StringBuilder sb = new StringBuilder();
        sb.append("Erreur: ").append(e.getMessage());
        Throwable cause = e.getCause();
        while (cause != null) {
            sb.append(" → Cause: ").append(cause.getMessage());
            cause = cause.getCause();
        }
        return ResponseEntity.status(500).body(sb.toString());
    }
}
```

**Pourquoi enchaîner les causes ?** Les exceptions JPA/Hibernate enveloppent souvent l'exception SQL d'origine dans 2-3 niveaux d'enveloppement (DataIntegrityViolationException → ConstraintViolationException → SQLIntegrityConstraintViolationException). En parcourer toutes les `getCause()`, on atteint le message SQL brut qui donne l'information réelle.

### 10.2 Update Sécurisé

```java
@PutMapping("/{id}")
public ResponseEntity<Utilisateur> updateUtilisateur(@PathVariable Long id,
        @RequestBody Utilisateur details) {
    return utilisateurRepository.findById(id)
            .map(utilisateur -> {
                utilisateur.setNom(details.getNom());
                // ... autres champs ...

                // Ne modifier le mot de passe que s'il est fourni
                // Évite de réinitialiser le mot de passe accidentellement
                if (details.getPassword() != null && !details.getPassword().isEmpty()) {
                    utilisateur.setPassword(details.getPassword());
                }
                return ResponseEntity.ok(utilisateurRepository.save(utilisateur));
            })
            .orElse(ResponseEntity.notFound().build()); // 404 si l'ID n'existe pas
}
```

---

## 11. Interface Utilisateur (Frontend SPA)

### 11.1 Architecture Single Page Application (SPA)

L'application utilise une architecture **SPA (Single Page Application)** : une seule page HTML (`index.html`) et tout le contenu est rendu dynamiquement par JavaScript (`app.js`). Il n'y a pas de rechargement de page lors de la navigation.

```
index.html (squelette fixe)
    ├── Sidebar navigation (rôles cachés/affichés selon JWT)
    ├── #loader           → Spinner pendant l'init Keycloak
    ├── #login-overlay    → Formulaire de connexion
    └── #app              → Interface principale
            ├── Sidebar
            └── #dynamic-view → Contenu rendu par renderRoleView()
```

### 11.2 Flux d'Initialisation de l'Application

```javascript
async function init() {
    // PRIORITÉ 1 : Session locale (token stocké en localStorage)
    const localToken = localStorage.getItem('local_token');
    if (localToken) {
        appToken = localToken;         // Réutiliser le token existant
        appProfile = JSON.parse(localStorage.getItem('local_profile'));
        await startApp();
        return;
    }

    // PRIORITÉ 2 : Authentification Keycloak SSO
    try {
        const authenticated = await keycloak.init({ onLoad: 'check-sso' });
        if (authenticated) {
            appToken = keycloak.token;
            appProfile = { roles: keycloak.realmAccess.roles, ... };
            await startApp();
        } else {
            showLogin(false); // Afficher le formulaire de login local
        }
    } catch (error) {
        showLogin(true); // Keycloak inaccessible → mode local
    }
}
```

**`check-sso` vs `login-required` :** Le mode `check-sso` vérifie silencieusement si une session Keycloak existe (dans un iframe) sans rediriger vers Keycloak si l'utilisateur n'est pas connecté. Cela permet de gérer gracieusement le cas où Keycloak n'est pas disponible.

### 11.3 Gestion du Token JWT côté Frontend

```javascript
// Wrapper fetch avec gestion automatique du 401
async function fetchWithAuth(url, options = {}) {
    if (!options.headers) options.headers = {};
    options.headers['Authorization'] = `Bearer ${appToken}`;
    
    const res = await fetch(url, options);
    
    if (res.status === 401) {
        // Token expiré ou invalide → forcer la reconnexion
        localStorage.removeItem('local_token');
        localStorage.removeItem('local_profile');
        alert('Votre session a expiré. Veuillez vous reconnecter.');
        location.reload();
    }
    return res;
}
```

**Pourquoi un wrapper ?** Centraliser la gestion des tokens évite de dupliquer le code de vérification du 401 dans chaque appel `fetch`. C'est le pattern **Intercepteur** (inspiré d'Axios interceptors).

### 11.4 Affichage Conditionnel selon le Rôle

```javascript
function updateUI() {
    const roles = appProfile.roles; // Ex: ['AGENT']

    // Montrer uniquement les boutons de navigation pertinents
    const roleSelectors = {
        'AGENT': '.role-agent',        // Bouton "Agent Bancaire"
        'ADMIN': '.role-admin',        // Bouton "Administration"
        'VALID_JURIDIQUE': '.role-juridique',
        'VALID_FINANCIER': '.role-financier',
        'AVOCAT': '.role-avocat',
        'HUISSIER': '.role-huissier',
        'EXPERT': '.role-expert'
    };

    // Tous les boutons sont cachés par défaut (style="display:none" dans HTML)
    // On affiche seulement ceux correspondant aux rôles de l'utilisateur
    Object.keys(roleSelectors).forEach(role => {
        if (roles.includes(role)) {
            document.querySelectorAll(roleSelectors[role])
                    .forEach(el => el.style.display = 'flex');
        }
    });
}
```

### 11.5 Redirection Automatique vers la Vue de Rôle

```javascript
function autoRedirect() {
    const roles = appProfile.roles;
    let targetRole = 'public';

    // Ordre de priorité des rôles
    if (roles.includes('ADMIN'))            targetRole = 'admin';
    else if (roles.includes('AGENT'))       targetRole = 'agent';
    else if (roles.includes('VALID_JURIDIQUE')) targetRole = 'juridique';
    else if (roles.includes('VALID_FINANCIER')) targetRole = 'financier';
    else if (roles.includes('AVOCAT'))      targetRole = 'avocat';
    else if (roles.includes('HUISSIER'))    targetRole = 'huissier';
    else if (roles.includes('EXPERT'))      targetRole = 'expert';

    // Déclencher le click sur le bouton de navigation correspondant
    const btn = document.querySelector(`.nav-item[data-role="${targetRole}"]`);
    if (btn) btn.click();
}
```

### 11.6 Vue Agent — Gestion des Dossiers

```javascript
function renderAgentView(container) {
    container.innerHTML = `
        <div class="admin-tabs">
            <button onclick="showAgentTab('dossiers')">📁 Dossiers & Clients</button>
            <button onclick="showAgentTab('users')">👥 Gérer Utilisateurs</button>
        </div>
        <div id="agent-content"></div>
    `;
    showAgentTab('dossiers'); // Afficher l'onglet par défaut
}
```

---

## 12. Initialisation des Données

### 12.1 `DataInitializer` — Chargement au Démarrage

```java
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    // CommandLineRunner : Spring exécute la méthode run() après le démarrage

    @Override
    public void run(String... args) throws Exception {
        // Vérifier avant d'insérer pour éviter les doublons lors des redémarrages
        if (agenceRepository.count() == 0) {
            agenceRepository.save(Agence.builder()
                    .nom("Agence Nabeul").code("NAB001").ville("Nabeul").build());
            agenceRepository.save(Agence.builder()
                    .nom("Agence Tunis").code("TUN001").ville("Tunis").build());
        }

        if (clientRepository.count() == 0) {
            clientRepository.save(Client.builder()
                    .nom("Ben Ali").prenom("Mohamed").cin("01234567")
                    .adresse("Route de Tunis, Nabeul").build());
        }
    }
}
```

**Pourquoi `count() == 0` ?** Le `ddl-auto=update` de Hibernate ne supprime pas les données existantes entre les redémarrages. Sans cette vérification, les données seraient dupliquées à chaque démarrage.

---

## 13. Flux Complets de l'Application

### 13.1 Flux de Connexion Agent

```
1. Agent entre username + password dans le formulaire HTML
2. app.js → fetch POST /api/auth/login avec { username, password }
3. AuthController.login() :
   a. Cherche dans AgentBancaire par username
   b. Compare le password
   c. Génère JWT avec payload { realm_access.roles: ["AGENT"], preferred_username: "omaima" }
4. Retourne { access_token: "eyJ...", username: "omaima", role: "AGENT" }
5. app.js stocke le token dans localStorage
6. startApp() → fetchAgentProfile() → updateUI() → autoRedirect()
7. L'utilisateur est redirigé vers renderAgentView()
```

### 13.2 Flux de Création d'un Dossier

```
1. Agent remplit le formulaire de création (client + agence)
2. app.js → fetch POST /api/dossiers avec { clientId: 1, agenceId: 2 }
   Header: Authorization: Bearer eyJ...
3. Spring Security Filter Chain :
   a. Extrait le token du header
   b. Décode avec localDecoder (Keycloak absent)
   c. Extrait role "AGENT" → ROLE_AGENT dans SecurityContext
4. @PreAuthorize("hasRole('AGENT')") → OK
5. DossierController.createDossier() :
   a. Extrait username depuis JWT : jwt.getClaimAsString("preferred_username")
   b. Appelle dossierService.createDossier(1L, 2L, "omaima")
6. DossierService.createDossier() :
   a. Charge client ID=1 depuis DB
   b. Charge agence ID=2 depuis DB
   c. Crée Dossier { statut: BROUILLON, numeroDossier: "DOS-A1B2C3D4" }
   d. Sauvegarde en DB → ID généré automatiquement
7. Retourne le Dossier sérialisé en JSON
8. app.js affiche le nouveau dossier dans la liste
```

### 13.3 Flux de Validation Double

```
Validateur Financier se connecte → token avec rôle VALID_FINANCIER
↓
Voit les dossiers en ATTENTE_VALIDATION
↓
Clique "Valider Financièrement"
↓
app.js → POST /api/dossiers/{id}/validate-fin
↓
@PreAuthorize("hasRole('VALID_FINANCIER')") → OK
↓
dossierService.validateFinanciere(id) :
    dossier.setValidationFinanciere(true)
    checkFinalValidation() → validationJuridique = false → statut inchangé

(Plus tard)
Validateur Juridique → POST /api/dossiers/{id}/validate-jur
dossierService.validateJuridique(id) :
    dossier.setValidationJuridique(true)
    checkFinalValidation() → DEUX VALIDATIONS = true → statut → VALIDE ✓
```

### 13.4 Flux de Connexion Utilisateur Externe (Avocat)

```
1. Avocat créé par l'Agent : { username: "maitre.ayari", role: AVOCAT, password: "1234" }
   Sauvegardé dans table "utilisateur" avec role_utilisateur = "AVOCAT"

2. Avocat accède à http://localhost:8097/index.html
3. Saisit ses identifiants dans le formulaire de login
4. POST /api/auth/login → AuthController vérifie dans UtilisateurRepository
5. Trouve l'utilisateur, mappe AVOCAT → "AVOCAT"
6. Génère JWT { realm_access.roles: ["AVOCAT"], preferred_username: "maitre.ayari" }
7. Retourne { access_token: ..., role: "AVOCAT", nom: "Ayari", prenom: "Hamza" }
8. app.js stocke le token, lit role "AVOCAT"
9. updateUI() affiche le bouton "Avocat" dans la sidebar
10. autoRedirect() → renderExternalRoleView() → Espace Avocat
```

---

## 14. Justification des Choix Techniques

### 14.1 Pourquoi Spring Boot ?

| Critère | Justification |
|---------|---------------|
| **Convention over Configuration** | Zéro XML de configuration. Les annotations suffisent. |
| **Serveur embarqué** | Tomcat intégré, pas besoin de déployer un WAR sur un serveur externe |
| **Écosystème** | Spring Data JPA, Spring Security s'intègrent sans friction |
| **Productivité** | DevTools pour le rechargement à chaud, `@Transactional` gérée automatiquement |

### 14.2 Pourquoi JWT (et pas les sessions serveur) ?

| Sessions | JWT |
|----------|-----|
| État côté serveur (session en mémoire) | **Stateless** : l'état est dans le token |
| Problème de scalabilité (sticky sessions) | Chaque serveur peut valider le token indépendamment |
| Difficile avec APIs mobiles | **Universel** : même token pour Web, mobile, API |
| Expire côté serveur uniquement | Expiration dans le token lui-même (`exp` claim) |

Dans une architecture microservices ou avec des APIs RESTful, **JWT est le standard industriel**.

### 14.3 Pourquoi l'Authentification Hybride Keycloak + Local ?

- **Keycloak** est un Identity Provider (IdP) de niveau entreprise, utilisé en production pour gérer des milliers d'utilisateurs avec SSO, MFA, etc.
- **Local JWT** est pratique en développement quand Keycloak n'est pas disponible.
- Le **décodeur hybride** (`try Keycloak → catch → local`) permet les **deux modes sans aucune modification de configuration**.

### 14.4 Pourquoi MySQL plutôt que H2 ?

- **H2** est une base en mémoire : les données sont perdues à chaque arrêt (utilisation : tests unitaires)
- **MySQL** est persistant, proche des conditions réelles de production
- XAMPP permet d'avoir MySQL facilement sur Windows sans installation complexe

### 14.5 Pourquoi Vanilla JavaScript (SPA) et non React/Angular ?

| Framework | Inconvénients dans ce contexte |
|-----------|-------------------------------|
| React | Nécessite Node.js, npm, bundler (Webpack/Vite) |
| Angular | Courbe d'apprentissage elevée, TypeScript requis |
| Vanilla JS | **Aucune dépendance externe**, directement servi par Spring Boot |

L'application est un fichier HTML + un fichier JS statiques **servis directement par Spring Boot** depuis `src/main/resources/static/`. Il n'y a pas de serveur front-end séparé.

### 14.6 Pourquoi Lombok ?

Sans Lombok, chaque entité nécessiterait plusieurs dizaines de lignes de **boilerplate** Java :
```java
// SANS Lombok → 20+ lignes pour une entité simple
public String getNom() { return this.nom; }
public void setNom(String nom) { this.nom = nom; }
public AgentBancaire() {}
public AgentBancaire(Long id, String username, ...) { this.id = id; ... }

// AVEC Lombok → 4 annotations
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
```

### 14.7 Pourquoi `@Column(name = "role_utilisateur")` ?

En MySQL 8.x, `ROLE` est un [mot-clé réservé](https://dev.mysql.com/doc/refman/8.0/en/reserved-words.html) utilisé pour la gestion des privilèges (`CREATE ROLE`, `DROP ROLE`). Hibernate génèrerait :
```sql
-- ÉCHEC : "role" est réservé
INSERT INTO utilisateur (username, role, ...) VALUES ('aicha', 'AVOCAT', ...)
-- SUCCÈS : nom de colonne explicite
INSERT INTO utilisateur (username, role_utilisateur, ...) VALUES ('aicha', 'AVOCAT', ...)
```

---

## 15. Guide de Déploiement

### Prérequis

| Outil | Version | Usage |
|-------|---------|-------|
| Java JDK | 21 | Compilation et exécution |
| Apache Maven | 3.9+ | Build et gestion des dépendances |
| XAMPP | Dernière | MySQL server sur port 3307 |

### Étapes de Déploiement

#### 1. Démarrer MySQL via XAMPP
```
XAMPP Control Panel → Start MySQL
Port par défaut modifié : 3307 (configuré dans application.properties)
La base "contentieux_db9" sera créée automatiquement
```

#### 2. Lancer l'Application Spring Boot
```bash
# Dans le répertoire du projet
cd contentieux-security/
mvn spring-boot:run

# Spring Boot va :
# 1. Télécharger les dépendances (1ère fois)
# 2. Compiler le code Java
# 3. Démarrer Tomcat sur le port 8097
# 4. Exécuter Hibernate ddl-auto=update (créer/mettre à jour les tables)
# 5. Exécuter DataInitializer (agences et clients par défaut)
```

#### 3. Accéder à l'Application
```
URL : http://localhost:8097/index.html
```

#### 4. Créer un Agent Bancaire (via H2 Console ou API)
L'application utilise MySQL → administrer via un client SQL (phpMyAdmin via XAMPP) :
```sql
INSERT INTO agence (nom, code, ville) VALUES ('Agence Test', 'TEST001', 'Tunis');
INSERT INTO agent_bancaire (username, nom, prenom, password, agence_id)
VALUES ('omaima', 'Oumaima', 'Ben Ali', '1234', 1);
```

#### 5. Se Connecter et Créer des Utilisateurs
1. Accéder à http://localhost:8097/index.html
2. Se connecter avec `omaima` / `1234`
3. Aller dans l'onglet **"👥 Gérer Utilisateurs"**
4. Créer des acteurs externes (Avocat, Huissier, Expert, etc.)
5. Ces acteurs peuvent ensuite se connecter avec leur propre compte

---

## Annexe : Glossaire Technique

| Terme | Définition |
|-------|-----------|
| **JWT** | JSON Web Token — token auto-contenu encodé en Base64 et signé |
| **HMAC-SHA256** | Algorithme de signature symétrique utilisant une clé secrète partagée |
| **RBAC** | Role-Based Access Control — contrôle d'accès basé sur les rôles |
| **JPA** | Java Persistence API — standard de mapping objet-relationnel |
| **ORM** | Object-Relational Mapper — traduction Java ↔ SQL (Hibernate ici) |
| **DTO** | Data Transfer Object — objet de transfert pour les APIs |
| **SPA** | Single Page Application — application web sans rechargement de page |
| **SSO** | Single Sign-On — connexion unique pour plusieurs applications |
| **Keycloak** | Identity Provider open-source de Red Hat |
| **Resource Server** | Serveur qui protège des ressources en validant les tokens JWT |
| **Stateless** | Sans état : chaque requête est indépendante et auto-suffisante |
| **`@Transactional`** | Garantit l'atomicité des opérations BDD (tout ou rien) |
| **`@PreAuthorize`** | Vérification du rôle avant l'exécution de la méthode |
| **`Optional<T>`** | Conteneur Java 8+ pour gérer les valeurs potentiellement nulles |

---

*Ce rapport documente l'implémentation complète du système de gestion du contentieux bancaire, couvrant l'ensemble des choix architecturaux, techniques et métier effectués lors du développement.*
