import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <div class="login-logo">
          <div class="login-badge">Sécurisé • Moderne</div>
          <div class="logo-mark">CB</div>
          <h1>Contentieux Bancaire</h1>
          <p>Plateforme de gestion des dossiers contentieux</p>
        </div>

        <!-- ── Bouton connexion ── -->
        <button class="login-btn" (click)="login()" *ngIf="!mdpOublieMode">
          <span>🔐</span>
          Se connecter
        </button>

        <!-- ── Lien mot de passe oublié ── -->
        <div class="mdp-oublie-link" *ngIf="!mdpOublieMode">
          <a href="#" (click)="$event.preventDefault(); ouvrirMdpOublie()">
            Mot de passe oublié ?
          </a>
        </div>

        <!-- ── Lien Assistant IA ── -->
        <div class="ai-link" *ngIf="!mdpOublieMode">
          <a href="http://localhost:5001" target="_blank" rel="noopener">
            🤖 Assistant IA Contentieux
          </a>
        </div>

        <!-- ── Formulaire mot de passe oublié ── -->
        <div class="mdp-oublie-form" *ngIf="mdpOublieMode">

          <h4>🔐 Mot de passe oublié</h4>

          <ng-container *ngIf="!mdpOublieMsg">
            <p class="mdp-oublie-desc">
              Saisissez votre email ou nom d'utilisateur. Un administrateur sera
              notifié et vous enverra un nouveau mot de passe.
            </p>

            <input type="text"
                   [(ngModel)]="emailOublie"
                   placeholder="Email ou nom d'utilisateur"
                   class="mdp-oublie-input"
                   (keyup.enter)="envoyerDemandeMdpOublie()" />

            <p class="mdp-erreur" *ngIf="mdpOublieErreur">
              ⚠️ {{ mdpOublieErreur }}
            </p>

            <div class="mdp-oublie-actions">
              <button class="btn-annuler"
                      (click)="fermerMdpOublie()"
                      [disabled]="envoi">
                Annuler
              </button>
              <button class="btn-envoyer"
                      (click)="envoyerDemandeMdpOublie()"
                      [disabled]="envoi || !emailOublie.trim()">
                {{ envoi ? 'Envoi...' : 'Envoyer la demande' }}
              </button>
            </div>
          </ng-container>

          <div class="mdp-confirmation" *ngIf="mdpOublieMsg">
            <p>✅ {{ mdpOublieMsg }}</p>
            <button class="btn-retour" (click)="fermerMdpOublie()">
              Retour à la connexion
            </button>
          </div>

        </div>
      </div>
    </div>
  `,
  styles: [`
    .ai-link {
      margin-top: 16px;
      text-align: center;
    }
    .ai-link a {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 8px 18px;
      font-size: 12px;
      font-weight: 500;
      color: #1d4ed8;
      border: 1px solid #bfdbfe;
      border-radius: 999px;
      text-decoration: none;
      background: #eff6ff;
      transition: all .2s;
    }
    .ai-link a:hover {
      background: #dbeafe;
      border-color: #2563eb;
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(37,99,235,.15);
    }
    .login-container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      background: radial-gradient(circle at top left, #dbeafe 0%, #eff6ff 35%, #f8fbff 100%);
    }
    .login-card {
      background: rgba(255,255,255,0.95);
      backdrop-filter: blur(18px);
      border-radius: 24px;
      padding: 48px 40px;
      text-align: center;
      box-shadow: 0 24px 80px rgba(15, 23, 42, 0.16);
      width: 100%;
      max-width: 430px;
      border: 1px solid rgba(148,163,184,0.2);
    }
    .login-logo { margin-bottom: 28px; }
    .login-badge {
      display: inline-flex;
      padding: 6px 12px;
      border-radius: 999px;
      background: #eff6ff;
      color: #2563eb;
      font-size: 0.74rem;
      font-weight: 700;
      letter-spacing: 0.06em;
      text-transform: uppercase;
      margin-bottom: 14px;
    }
    .logo-mark {
      width: 56px;
      height: 56px;
      border-radius: 16px;
      margin: 0 auto 14px;
      display: grid;
      place-items: center;
      background: linear-gradient(135deg, #1d4ed8, #60a5fa);
      color: white;
      font-weight: 800;
      font-size: 1.1rem;
      box-shadow: 0 14px 24px rgba(37,99,235,.24);
    }
    h1 { color: #0f172a; font-size: 1.55rem; font-weight: 800; margin: 0 0 10px 0; }
    p { color: #64748b; font-size: 0.95rem; margin: 0; }
    .login-btn {
      width: 100%;
      padding: 14px 20px;
      background: linear-gradient(135deg, #0f172a, #2563eb);
      color: white;
      border: none;
      border-radius: 999px;
      font-size: 1rem;
      font-weight: 700;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      transition: all 0.3s ease;
    }
    .login-btn:hover {
      transform: translateY(-2px);
      box-shadow: 0 10px 24px rgba(37,99,235,0.25);
    }
    .mdp-oublie-link {
      margin-top: 14px;
      text-align: center;
    }
    .mdp-oublie-link a {
      color: #1a237e;
      font-size: 13px;
      text-decoration: none;
      cursor: pointer;
    }
    .mdp-oublie-link a:hover { text-decoration: underline; }
    .mdp-oublie-form {
      margin-top: 16px;
      padding: 20px;
      background: #f5f5f5;
      border-radius: 10px;
      border: 1px solid #e0e0e0;
      text-align: left;
    }
    .mdp-oublie-form h4 {
      margin: 0 0 12px;
      color: #1a237e;
      font-size: 15px;
    }
    .mdp-oublie-desc {
      font-size: 13px;
      color: #555;
      margin: 0 0 12px;
      line-height: 1.5;
    }
    .mdp-oublie-input {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid #ddd;
      border-radius: 6px;
      font-size: 14px;
      box-sizing: border-box;
      margin-bottom: 12px;
      outline: none;
    }
    .mdp-oublie-input:focus { border-color: #1a237e; }
    .mdp-erreur {
      color: #c62828;
      font-size: 12px;
      margin: -8px 0 8px;
    }
    .mdp-oublie-actions {
      display: flex;
      gap: 8px;
    }
    .btn-annuler {
      flex: 1;
      padding: 10px;
      background: #fff;
      border: 1px solid #ddd;
      border-radius: 6px;
      cursor: pointer;
      font-size: 13px;
    }
    .btn-annuler:hover { background: #f0f0f0; }
    .btn-envoyer {
      flex: 2;
      padding: 10px;
      background: #1a237e;
      color: #fff;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      font-size: 13px;
      font-weight: 600;
    }
    .btn-envoyer:hover:not(:disabled) { background: #283593; }
    .btn-envoyer:disabled { opacity: 0.6; cursor: not-allowed; }
    .mdp-confirmation {
      text-align: center;
      padding: 12px;
      background: #e8f5e9;
      border-radius: 8px;
      color: #2e7d32;
    }
    .mdp-confirmation p { margin: 0 0 12px; font-size: 14px; }
    .btn-retour {
      padding: 8px 24px;
      background: #2e7d32;
      color: #fff;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      font-size: 13px;
    }
    .btn-retour:hover { background: #1b5e20; }
  `]
})
export class LoginComponent implements OnInit {

  mdpOublieMode   = false;
  emailOublie     = '';
  mdpOublieMsg    = '';
  mdpOublieErreur = '';
  envoi           = false;

  constructor(
    private keycloak: KeycloakService,
    private router:   Router,
    private http:     HttpClient,
    private route:    ActivatedRoute,
  ) {}

  async ngOnInit() {
    // ── Lire les query params EN PREMIER ────────────────────────
    const params = await new Promise<any>(resolve => {
      this.route.queryParams.subscribe(p => resolve(p));
    });
  
    // ✅ Si mode mdp oublié → NE PAS vérifier Keycloak du tout
    if (params['mdpOublie'] === 'true') {
      this.mdpOublieMode = true;
      return;  // ← STOP — pas de redirection Keycloak
    }
  
    // ── Sinon → comportement normal Keycloak ────────────────────
    try {
      const isLoggedIn = await this.keycloak.isLoggedIn();
      if (!isLoggedIn) return;
  
      const keycloakInstance = this.keycloak.getKeycloakInstance();
      if (!keycloakInstance) {
        this.redirectByRole();
        return;
      }
  
      const requiredActions: string[] =
        keycloakInstance?.tokenParsed?.['required_actions'] ?? [];
  
      if (requiredActions.includes('UPDATE_PASSWORD')) {
        console.log('🔐 UPDATE_PASSWORD détecté → redirection Keycloak');
        await keycloakInstance.login({
          action: 'UPDATE_PASSWORD',
          redirectUri: window.location.origin + '/login'
        });
        return;
      }
  
      this.redirectByRole();
  
    } catch (err) {
      console.warn('Erreur ngOnInit login:', err);
    }
  }

  async login() {
    await this.keycloak.login({
      redirectUri: window.location.origin + '/login'
    });
  }

  ouvrirMdpOublie(): void {
    this.mdpOublieMode   = true;
    this.mdpOublieMsg    = '';
    this.mdpOublieErreur = '';
    this.emailOublie     = '';
  }

  fermerMdpOublie(): void {
    this.mdpOublieMode   = false;
    this.emailOublie     = '';
    this.mdpOublieMsg    = '';
    this.mdpOublieErreur = '';
    this.router.navigate(['/login'], { replaceUrl: true });
  }

  envoyerDemandeMdpOublie(): void {
    console.log('🔥 envoyerDemandeMdpOublie appelé');
    console.log('📧 emailOublie:', this.emailOublie);
    console.log('🌐 apiUrl:', environment.apiUrl);

    this.mdpOublieErreur = '';

    if (!this.emailOublie.trim()) {
      this.mdpOublieErreur = "Veuillez saisir votre email ou nom d'utilisateur.";
      return;
    }

    this.envoi = true;

    // ✅ Header JSON explicite pour éviter text/plain
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

    this.http.post<any>(
      `${environment.apiUrl}/api/public/mot-de-passe-oublie`,
      { email: this.emailOublie.trim() },
      { headers }
    ).subscribe({
      next: (res) => {
        console.log('✅ Réponse reçue:', res);
        this.mdpOublieMsg = res.message;
        this.envoi        = false;
        this.emailOublie  = '';
      },
      error: (err) => {
        console.error('❌ Erreur HTTP:', err);
        this.mdpOublieMsg = "Si cet identifiant existe, un administrateur a été notifié.";
        this.envoi        = false;
      }
    });
  }

  private redirectByRole() {
    const roles = this.keycloak.getUserRoles();
    const hasRole = (expected: string): boolean => {
      const normalize = (r: string) => r.replace(/^ROLE_/, '').toUpperCase();
      return roles.some(r => normalize(r) === normalize(expected));
    };

    if (hasRole('ADMIN'))
      this.router.navigate(['/admin/dashboard']);
    else if (hasRole('AGENT'))
      this.router.navigate(['/agent/dashboard']);
    else if (hasRole('AVOCAT'))
      this.router.navigate(['/avocat/dashboard']);
    else if (hasRole('VALIDATEUR_JURIDIQUE'))
      this.router.navigate(['/validateur/juridique/dashboard']);
    else if (hasRole('VALIDATEUR_FINANCIER'))
      this.router.navigate(['/validateur/financier/dashboard']);
    else if (hasRole('PRESTATAIRE') || hasRole('EXPERT') || hasRole('HUISSIER'))
      this.router.navigate(['/prestataire/dashboard']);
    else
      this.router.navigate(['/access-denied']);
  }
}