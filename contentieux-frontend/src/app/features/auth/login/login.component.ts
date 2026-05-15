import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <div class="login-logo">
          <span class="logo-icon">🏦</span>
          <h1>Contentieux Bancaire</h1>
          <p>Plateforme de gestion des dossiers contentieux</p>
        </div>

        <!-- ── Bouton connexion — masqué si formulaire mdp oublié ouvert ── -->
        <button class="login-btn" (click)="login()" *ngIf="!mdpOublieMode">
          <span>🔐</span>
          Se connecter
        </button>

        <!-- ── Lien mot de passe oublié ── -->
        <div class="mdp-oublie-link" *ngIf="!mdpOublieMode">
          <a href="#"
             (click)="$event.preventDefault(); ouvrirMdpOublie()">
            Mot de passe oublié ?
          </a>
        </div>

        <!-- ── Formulaire mot de passe oublié ── -->
        <div class="mdp-oublie-form" *ngIf="mdpOublieMode">

          <h4>🔐 Mot de passe oublié</h4>

          <!-- Formulaire de saisie -->
          <ng-container *ngIf="!mdpOublieMsg">
            <p class="mdp-oublie-desc">
              Saisissez votre email ou nom d'utilisateur. Un administrateur sera
              notifié et vous enverra un nouveau mot de passe.
            </p>

            <!-- ✅ type="text" — accepte email OU username -->
            <input type="text"
                   [(ngModel)]="emailOublie"
                   placeholder="Email ou nom d'utilisateur"
                   class="mdp-oublie-input" />

            <!-- Message d'erreur -->
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

          <!-- Message de confirmation -->
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
    .login-container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #001f3f 0%, #003d7a 100%);
    }
    .login-card {
      background: white;
      border-radius: 16px;
      padding: 50px 40px;
      text-align: center;
      box-shadow: 0 20px 60px rgba(0,0,0,0.3);
      width: 100%;
      max-width: 420px;
    }
    .login-logo { margin-bottom: 35px; }
    .logo-icon { font-size: 3rem; display: block; margin-bottom: 15px; }
    h1 { color: #001f3f; font-size: 1.6rem; font-weight: 700; margin: 0 0 10px 0; }
    p { color: #666; font-size: 0.9rem; margin: 0; }

    /* ── Bouton connexion ── */
    .login-btn {
      width: 100%;
      padding: 14px 20px;
      background: linear-gradient(135deg, #001f3f, #007bff);
      color: white;
      border: none;
      border-radius: 10px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      transition: all 0.3s ease;
    }
    .login-btn:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(0,123,255,0.4);
    }

    /* ── Lien mot de passe oublié ── */
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

    /* ── Formulaire mot de passe oublié ── */
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

    /* ── Confirmation ── */
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

  // ── Mot de passe oublié ───────────────────────────────────────
  mdpOublieMode   = false;
  emailOublie     = '';
  mdpOublieMsg    = '';
  mdpOublieErreur = '';
  envoi           = false;

  constructor(
    private keycloak: KeycloakService,
    private router:   Router,
    private http:     HttpClient,
    private route:    ActivatedRoute,  // ✅ injecté
  ) {}

  async ngOnInit() {

    // ✅ Détecter ?mdpOublie=true → ouvrir automatiquement le formulaire
    this.route.queryParams.subscribe(params => {
      if (params['mdpOublie'] === 'true') {
        this.mdpOublieMode = true;
      }
    });

    const isLoggedIn = await Promise.resolve(
      this.keycloak.isLoggedIn() as unknown as boolean | Promise<boolean>
    );

    if (!isLoggedIn) return;

    // ✅ Vérifier UPDATE_PASSWORD avant de rediriger
    const keycloakInstance = this.keycloak.getKeycloakInstance();
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
  }

  async login() {
    await this.keycloak.login({
      redirectUri: window.location.origin + '/login'
    });
  }

  // ── Mot de passe oublié ───────────────────────────────────────

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
    // ✅ Nettoyer le paramètre URL
    this.router.navigate(['/login'], { replaceUrl: true });
  }

  envoyerDemandeMdpOublie(): void {
    this.mdpOublieErreur = '';

    if (!this.emailOublie.trim()) {
      this.mdpOublieErreur = "Veuillez saisir votre email ou nom d'utilisateur.";
      return;
    }

    // ✅ Pas de validation regex — on accepte email OU username

    this.envoi = true;

    this.http.post<any>(
      `${environment.apiUrl}/api/public/mot-de-passe-oublie`,
      { email: this.emailOublie.trim() }
    ).subscribe({
      next: (res) => {
        this.mdpOublieMsg = res.message;
        this.envoi        = false;
        this.emailOublie  = '';
      },
      error: () => {
        this.mdpOublieMsg = "Si cet identifiant existe, un administrateur a été notifié.";
        this.envoi        = false;
      }
    });
  }

  // ── Redirection par rôle ──────────────────────────────────────

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