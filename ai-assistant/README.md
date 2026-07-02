# AI Assistant Contentieux Bancaire Intelligent — Microservice Flask

## 1. Lancement local

```bash
cd ai-assistant
cp .env.example .env        # puis renseigner GEMINI_API_KEY ou DEEPSEEK_API_KEY
pip install -r requirements.txt
python app.py                # démarre sur http://localhost:5001
```

Test rapide :
```bash
curl -X POST http://localhost:5001/api/assistant/chat \
  -H "Content-Type: application/json" \
  -H "X-Internal-Api-Key: change-me-dev-key" \
  -d '{
        "message": "Quelle est la prochaine étape pour ce dossier ?",
        "role": "AGENT",
        "context": {"numeroDossier": "DOS-2026-0123", "statut": "EN_ATTENTE_VALIDATION"}
      }'
```

---

## 2. Intégration côté Spring Boot (`contentieux-backend`)

### 2.1 Configuration (`application.yml`)
```yaml
ai-assistant:
  base-url: http://localhost:5001
  internal-api-key: change-me-dev-key
```

### 2.2 Bean WebClient
```java
@Configuration
public class AiAssistantConfig {

    @Value("${ai-assistant.base-url}")
    private String baseUrl;

    @Bean
    public WebClient aiAssistantWebClient() {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
```

### 2.3 Service proxy
```java
@Service
public class AiAssistantService {

    private final WebClient webClient;

    @Value("${ai-assistant.internal-api-key}")
    private String internalApiKey;

    public AiAssistantService(WebClient aiAssistantWebClient) {
        this.webClient = aiAssistantWebClient;
    }

    public Map<String, Object> chat(Map<String, Object> body) {
        return webClient.post()
                .uri("/api/assistant/chat")
                .header("X-Internal-Api-Key", internalApiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}
```

### 2.4 Contrôleur exposé à Angular
```java
@RestController
@RequestMapping("/api/assistant")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        // Injecter automatiquement le rôle depuis Keycloak,
        // sans faire confiance au front pour ce champ.
        String role = extractRole(authentication);
        body.put("role", role);

        return ResponseEntity.ok(aiAssistantService.chat(body));
    }

    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("DEFAULT")
                .replace("ROLE_", "");
    }
}
```

> Avantage : Angular n'appelle que le backend Spring Boot (déjà sécurisé par
> Keycloak). Spring Boot fait l'appel sortant vers Flask avec la clé interne,
> ce qui évite d'exposer le service Flask ou les clés Gemini/DeepSeek au
> front-end.

---

## 3. Intégration côté Angular (`contentieux-frontend`)

### 3.1 Service Angular
```typescript
// ai-assistant.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface ChatRequest {
  message: string;
  context?: Record<string, any>;
  history?: ChatMessage[];
}

export interface ChatResponse {
  response: string;
}

@Injectable({ providedIn: 'root' })
export class AiAssistantService {
  private readonly apiUrl = '/api/assistant/chat';

  constructor(private http: HttpClient) {}

  chat(payload: ChatRequest): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(this.apiUrl, payload);
  }
}
```

### 3.2 Exemple d'utilisation dans un composant (panneau d'assistance)
```typescript
this.aiAssistantService.chat({
  message: this.userInput,
  context: {
    numeroDossier: this.dossier?.numero,
    statut: this.dossier?.statut,
    montant: this.dossier?.montant,
  },
  history: this.conversationHistory,
}).subscribe({
  next: (res) => this.conversationHistory.push(
    { role: 'user', content: this.userInput },
    { role: 'assistant', content: res.response }
  ),
  error: (err) => this.notificationService.showError("Assistant IA indisponible"),
});
```

Vous pouvez placer ce panneau d'assistance dans `AppComponent` (comme votre
panneau de notifications), accessible depuis tous les dashboards par rôle.

---

## 4. Endpoint complémentaire : analyse de dossier

`POST /api/assistant/analyze-dossier`
```json
{
  "documentText": "Texte extrait d'une mise en demeure, échéancier, jugement...",
  "role": "AVOCAT"
}
```
Retourne :
```json
{ "analysis": "1. Résumé... 2. Éléments clés... 3. Actions recommandées..." }
```

Utile pour : lorsque l'utilisateur ouvre un document PDF du dossier,
afficher un bouton "Résumer avec l'IA" qui envoie le texte extrait
(via une lib comme `pdf.js` côté front, ou extraction côté backend Spring).

---

## 5. Déploiement

- Conteneuriser le service Flask (Dockerfile simple basé sur `python:3.12-slim`).
- Ajouter `ai-assistant` comme service dans le `docker-compose.yml` existant,
  sur le même réseau Docker que le backend Spring Boot.
- Ne jamais exposer le port 5001 publiquement ; seul Spring Boot doit y accéder.
- Stocker `GEMINI_API_KEY` / `DEEPSEEK_API_KEY` / `INTERNAL_API_KEY` dans un
  gestionnaire de secrets (variables d'environnement du conteneur, pas dans
  le code).

---

## 6. Évolutions possibles
- Streaming de la réponse (SSE) pour un effet "texte qui s'affiche en direct".
- Cache des réponses fréquentes (ex. "quelles sont les étapes d'une saisie ?").
- Ajout d'un mode RAG : indexer les procédures internes pour des réponses
  spécifiques à votre établissement plutôt que des connaissances générales.
