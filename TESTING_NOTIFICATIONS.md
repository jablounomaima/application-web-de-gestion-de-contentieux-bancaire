# Guide de Test - Notifications pour Avocat

## État de l'implémentation

### ✅ Complété
1. **Backend - Création de Notifications**
   - `NotificationService.notifier()` crée une notification avec destinataire avocat
   - `DossierMissionController.creerMission()` envoie notification NOUVELLE_AFFAIRE à l'avocat
   - Les notifications sont sauvegardées en base avec destinataire = username prestataire

2. **Backend - WebSocket STOMP**
   - `WebSocketConfig` configure SimpleBroker pour `/user` prefix
   - STOMP CONNECT interceptor extrait username du JWT Keycloak
   - `NotificationEventListener` publie notifications après commit via `SimpMessagingTemplate.convertAndSendToUser()`

3. **Frontend - WebSocket Client**
   - `NotificationService.connectWebSocket()` établit connexion STOMP
   - Subscribe à `/user/queue/notifications` pour recevoir messages temps réel
   - `nouvelleNotif$` Subject diffuse les notifications aux composants Angular

4. **Logs détaillés ajoutés**
   - `NotificationService.notifier()` - trace destinataire et type
   - `NotificationEventListener.envoyerNotificationWebSocket()` - trace envoi via WebSocket
   - `WebSocketConfig` - trace STOMP CONNECT avec username
   - Frontend `NotificationService` - trace WebSocket subscription et message reçu

## Comment tester manuellement

### Étape 1: Lancer le serveur
```bash
cd contentieux_security
java -jar target/contentieux-security-0.0.1-SNAPSHOT.jar
```

### Étape 2: Se connecter comme Agent
- Aller sur http://localhost:4200/
- Se connecter avec un compte Agent dans Keycloak
- Ouvrir Browser Console (F12) pour voir les logs WebSocket

### Étape 3: Créer une Mission avec Avocat
1. Dans le dashboard Agent, créer un dossier
2. Sur le dossier, créer une Prestation (AVOCAT)
3. Assigner un avocat à cette prestation (mission)
4. Envoyer le dossier aux validateurs

### Étape 4: Vérifier les logs
**Backend logs à chercher:**
```
>>> [notifier] destinataire='<username_avocat>' type='NOUVELLE_AFFAIRE'
>>> [WS AFTER_COMMIT] Notification reçue destinataire: '<username_avocat>'
✅ [WS AFTER_COMMIT] Notification livrée à '<username_avocat>'
```

**Frontend logs (Browser Console):**
```
>>> [WS] S'abonnant à /user/queue/notifications
>>> [WS] Message reçu sur /user/queue/notifications
>>> [ajouterNotification] Notification normalisée
```

### Étape 5: Vérifier la notification côté Avocat
1. Se déconnecter de l'Agent
2. Se connecter comme l'Avocat qui a reçu la mission
3. Vérifier que:
   - ✅ WebSocket est connecté (voir dans Browser Console)
   - ✅ Dashboard affiche la nouvelle affaire
   - ✅ Une notification est visible dans le panneau

## Points clés à vérifier

### Si la notification N'arrive PAS:
1. **Vérifier le username:**
   - Avocat en base: `SELECT username FROM prestataire WHERE id=<id>`
   - Avocat à la connexion: console.log(username) dans navbar.component.ts
   - Doivent correspondre!

2. **Vérifier le WebSocket:**
   - Chercher dans logs backend: `✅ [STOMP CONNECT] Utilisateur connecté (token)`
   - Si pas de log CONNECT, WebSocket ne s'est pas établi

3. **Vérifier la publication d'événement:**
   - Si log `>>> [notifier] destinataire=...` mais PAS `>>> [WS AFTER_COMMIT]`
   - Cela signifie l'event listener n'est pas appelé (problema transactionnel)

4. **Vérifier l'envoi SimpMessagingTemplate:**
   - Si log `>>> [WS AFTER_COMMIT]` mais PAS `✅ [WS AFTER_COMMIT] Notification livrée`
   - Cela signifie l'exception dans convertAndSendToUser

## Architecture de flux

```
Agent crée Mission
    ↓
DossierMissionController.creerMission()
    ↓
NotificationService.notifier(avocat.username, ...)
    ↓
Notification.builder().destinataire(avocat.username)...
    ↓
sauvegarderEtEnvoyer(n)
    ↓
sauvegarderDansNouvelleTransaction(n) → SAVES TO DB
    ↓
eventPublisher.publishEvent(new NotificationEvent(n))
    ↓
[TRANSACTION COMMITS]
    ↓
NotificationEventListener.envoyerNotificationWebSocket() [ASYNC]
    ↓
SimpMessagingTemplate.convertAndSendToUser(avocat.username, "/queue/notifications", dto)
    ↓
[WEBSOCKET DELIVERS TO AVOCAT SESSION]
    ↓
Frontend NotificationService.subscribe("/user/queue/notifications")
    ↓
receiveNotification → ajouterNotification()
    ↓
nouvelleNotif$.next() → Broadcasts to components
    ↓
avocat-dashboard component → Recharger les affaires
```

## Commandes utiles pour déboguer

### Afficher les notifications en base:
```sql
SELECT * FROM notifications ORDER BY date_creation DESC LIMIT 10;
```

### Afficher les prestataires:
```sql
SELECT id, nom, prenom, username, type, actif FROM prestataire;
```

### Afficher les missions:
```sql
SELECT m.id, m.numero_mission, p.username, p.nom FROM mission m 
JOIN prestataire p ON m.prestataire_id = p.id 
ORDER BY m.date_assignation DESC LIMIT 10;
```
