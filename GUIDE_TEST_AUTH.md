# 🔐 GUIDE DE TEST - Module d'Authentification

## ✅ Configuration Actuelle

### Rôles Disponibles
1. **ROLE_ADMIN** - Administrateur système
2. **ROLE_AGENT_BANCAIRE** - Agent bancaire
3. **ROLE_VALIDATEUR_JURIDIQUE** - Validateur juridique
4. **ROLE_AVOCAT** - Avocat
5. **ROLE_HUISSIER** - Huissier de justice
6. **ROLE_EXPERT** - Expert technique
7. **ROLE_VALIDATEUR_FINANCIER** - Validateur financier

### Utilisateurs de Test Préchargés
```
admin / admin123 → ROLE_ADMIN
agent / agent123 → ROLE_AGENT_BANCAIRE
```

---

## 🧪 Tests à Effectuer

### Test 1 : Accès Page de Login
**URL** : http://localhost:8090/login.html
**Résultat Attendu** : Page de login s'affiche correctement

### Test 2 : Connexion Admin
1. Aller sur http://localhost:8090/login.html
2. Entrer : `admin` / `admin123`
3. Cliquer sur "Se connecter"
**Résultat Attendu** : Redirection vers `/dashboards/admin.html`

###Test 3 : Connexion Agent
1. Aller sur http://localhost:8090/login.html
2. Entrer : `agent` / `agent123`
3. Cliquer sur "Se connecter"
**Résultat Attendu** : Redirection vers `/dashboards/agent.html`

### Test 4 : Création d'un Nouvel Intervenant (par Agent)
1. Se connecter en tant qu'`agent`
2. Aller sur http://localhost:8090/manage-users.html
3. Créer un utilisateur avec rôle "Validateur Juridique"
**Résultat Attendu** : Utilisateur créé et visible dans la liste

### Test 5 : Accès API Protégée
**Requête** :
```bash
curl -H "Authorization: Bearer {TOKEN}" http://localhost:8090/api/users
```
**Résultat Attendu** : Liste des utilisateurs (si connecté en tant qu'agent ou admin)

### Test 6 : Accès Sans Token
**URL** : http://localhost:8090/api/users (sans header Authorization)
**Résultat Attendu** : 401 Unauthorized

---

## 📊 Endpoints Disponibles

### Publics (sans authentification)
- `GET /` - Page d'accueil
- `GET /login.html` - Page de connexion
- `POST /auth/login` - Authentification
- `POST /auth/register` - Inscription (si activé)
- `GET /h2-console/**` - Console H2

### Protégés (nécessitent JWT)
- `GET /api/auth/me` - Informations utilisateur connecté
- `GET /api/users` - Liste utilisateurs (Admin + Agent)
- `POST /api/users` - Créer utilisateur (Admin + Agent)
- `DELETE /api/users/{id}` - Supprimer utilisateur (Admin uniquement)

### Dashboards (nécessitent authentification)
- `/dashboards/admin.html` - Dashboard Admin
- `/dashboards/agent.html` - Dashboard Agent Bancaire
- `/dashboards/juridique.html` - Dashboard Validateur Juridique
- `/dashboards/avocat.html` - Dashboard Avocat
- `/dashboards/huissier.html` - Dashboard Huissier
- `/dashboards/expert.html` - Dashboard Expert
- `/dashboards/financier.html` - Dashboard Validateur Financier

---

## 🔍 Vérifications de Sécurité

### ✅ Authentification
- [x] Login avec username/password
- [x] Génération JWT après succès
- [x] Stockage JWT dans localStorage
- [x] Envoi JWT dans header Authorization

### ✅ Autorisation
- [x] Vérification du rôle pour chaque endpoint
- [x] RedirectionPersonnalisée selon le rôle
- [x] Accès refusé si rôle insuffisant

### ✅ Sécurité
- [x] CSRF désactivé (API REST)
- [x] Sessions stateless
- [x] Protection XSS (validation entrées)
- [x] Mot de passe hashé avec BCrypt

---

## 🐛 Problèmes Résolus

1. **HTTP 403 sur ressources statiques** ✅
   - Solution : Ajout de `/dashboards/**`, `/css/**`, `/js/**` dans permitAll()

2. **Utilisateurs en base pas reconnus** ✅
   - Solution : Configuration DaoAuthenticationProvider avec PasswordEncoder

3. **JWT Filter bloque tout** ✅
   - Solution : Amélioration de shouldNotFilter()

4. **Pas de redirection après login** ✅
   - Solution : Endpoint `/api/auth/me` + routing côté client

---

## 📝 Prochaines Étapes

1. ✅ Créer les 5 dashboards restants
2. ⏳ Implémenter les entités métier (Dossier, Affaire, Mission, etc.)
3. ⏳ Créer les APIs CRUD pour chaque module
4. ⏳ Ajouter système de notifications temps réel
5. ⏳ Implémenter les workflows de validation
6. ⏳ Ajouter rapports et statistiques

---

## 🚀 Commandes Utiles

```bash
# Lancer l'application
mvn spring-boot:run

# Compiler
mvn clean compile

# Tester un endpoint
curl -X POST http://localhost:8090/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Vérifier utilisateur connecté
curl -H "Authorization: Bearer {TOKEN}" \
  http://localhost:8090/api/auth/me
```
