# 🚀 GUIDE RAPIDE - Comment Afficher les Dashboards

## ✅ Étape 1 : Vérifier que l'Application Tourne

Votre application doit tourner sur le **port 8092**.

Dans un terminal, vérifiez :
```bash
mvn spring-boot:run
```

Attendez de voir ce message :
```
Started DemoApplication in X.XXX seconds
```

---

## 🌐 Étape 2 : Ouvrir votre Navigateur

### Option 1 : Connexion Directe (RECOMMANDÉ)

1. **Ouvrez votre navigateur** (Chrome, Firefox, Edge, etc.)

2. **Allez sur la page de login** :
   ```
   http://localhost:8092/login.html
   ```

3. **Connectez-vous avec** :
   - **Username** : `admin`
   - **Password** : `admin123`
   
   OU
   
   - **Username** : `agent`
   - **Password** : `agent123`

4. **Cliquez sur "Se connecter"**

5. **Vous serez automatiquement redirigé** vers votre dashboard !
   - Admin → http://localhost:8092/dashboards/admin.html
   - Agent → http://localhost:8092/dashboards/agent.html

---

### Option 2 : Accès Direct aux Dashboards (Après Connexion)

Si vous êtes déjà connecté, vous pouvez accéder directement :

**Dashboards disponibles** :

| Rôle | URL Directe |
|------|-------------|
| Administrateur | http://localhost:8092/dashboards/admin.html |
| Agent Bancaire | http://localhost:8092/dashboards/agent.html |
| Validateur Juridique | http://localhost:8092/dashboards/juridique.html |
| Avocat | http://localhost:8092/dashboards/avocat.html |
| Huissier | http://localhost:8092/dashboards/huissier.html |
| Expert | http://localhost:8092/dashboards/expert.html |
| Validateur Financier | http://localhost:8092/dashboards/financier.html |

---

## ⚠️ Problèmes Courants

### Problème 1 : "This site can't be reached" ou "Impossible de se connecter"

**Solution** :
- Vérifiez que l'application tourne (voir Étape 1)
- Vérifiez le port dans `application.properties` (doit être 8092)
- Attendez 30 secondes après le démarrage

### Problème 2 : Page blanche ou erreur 403/404

**Solution** :
1. Supprimez le cache du navigateur (Ctrl+Shift+Delete)
2. Supprimez le localStorage :
   - Ouvrez la console (F12)
   - Tapez : `localStorage.clear()`
   - Actualisez la page (F5)

### Problème 3 : Pas de redirection après login

**Solution** :
1. Vérifiez la console du navigateur (F12 → Console)
2. Vérifiez qu'il n'y a pas d'erreurs JavaScript
3. Essayez en navigation privée

### Problème 4 : "Access Denied" ou "Forbidden"

**Solution** :
- Déconnectez-vous d'abord (bouton déconnexion ou `localStorage.clear()`)
- Reconnectez-vous via http://localhost:8092/login.html

---

## 📝 Étapes Détaillées avec Captures

### 1️⃣ Page de Login
```
http://localhost:8092/login.html
```
- Formulaire avec username et password
- Bouton "Se connecter"
- Bouton "S'inscrire" (optionnel)

### 2️⃣ Après Connexion
- Vous êtes automatiquement redirigé
- La sidebar affiche votre rôle
- Votre nom d'utilisateur apparaît en haut

### 3️⃣ Navigation
- Utilisez la sidebar à gauche pour naviguer
- Cliquez sur les sections
- Bouton "Déconnexion" en bas

---

## 🧪 Test Rapide

### Test Admin
```bash
1. Ouvrir : http://localhost:8092/login.html
2. Username: admin
3. Password: admin123
4. Cliquer "Se connecter"
5. → Vous voyez le Dashboard Admin (violet) avec logo "⚙️ ADMIN"
```

### Test Agent
```bash
1. Cliquer sur "Déconnexion"
2. Ouvrir : http://localhost:8092/login.html
3. Username: agent
4. Password: agent123
5. Cliquer "Se connecter"
6. → Vous voyez le Dashboard Agent (bleu) avec logo "🏦 AGENT"
```

---

## 🔧 Créer d'Autres Utilisateurs

Pour tester les autres dashboards (Juridique, Avocat, Huissier, etc.) :

1. **Connectez-vous en tant qu'agent**
2. **Allez sur** :
   ```
   http://localhost:8092/manage-users.html
   ```
3. **Créez un utilisateur** :
   - Sélectionnez le type (ex: "Validateur Juridique")
   - Entrez un username (ex: "juridique")
   - Entrez un mot de passe (ex: "test123")
   - Cliquez "Ajouter l'intervenant"

4. **Déconnectez-vous** et **reconnectez-vous** avec :
   - Username: juridique
   - Password: test123

5. **Vous verrez le dashboard Validateur Juridique !**

---

## 📱 Raccourcis Rapides

**Connexion Admin** :
```
URL: http://localhost:8092/login.html
User: admin / admin123
```

**Connexion Agent** :
```
URL: http://localhost:8092/login.html
User: agent / agent123
```

**Gestion Utilisateurs** :
```
URL: http://localhost:8092/manage-users.html
(Nécessite d'être connecté en tant qu'agent ou admin)
```

---

## ✅ Checklist de Vérification

Avant de signaler un problème, vérifiez :

- [ ] L'application Spring Boot tourne (aucune erreur dans le terminal)
- [ ] Le port 8092 est accessible
- [ ] Vous utilisez la bonne URL : `http://localhost:8092/login.html`
- [ ] Vous avez essayé avec `admin` / `admin123`
- [ ] Vous avez vidé le cache du navigateur
- [ ] Vous avez essayé en navigation privée
- [ ] La console du navigateur (F12) n'affiche pas d'erreurs

---

## 🎯 En Résumé

**La méthode la plus simple** :

1. Ouvrir navigateur
2. Taper : `http://localhost:8092/login.html`
3. Username: `admin`
4. Password: `admin123`
5. Cliquer "Se connecter"
6. ✅ **LE DASHBOARD APPARAÎT !**

---

**Besoin d'aide ?** Vérifiez les logs dans le terminal où tourne `mvn spring-boot:run`
