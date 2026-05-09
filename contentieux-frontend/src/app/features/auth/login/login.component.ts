import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <div class="login-logo">
          <span class="logo-icon">🏦</span>
          <h1>Contentieux Bancaire</h1>
          <p>Plateforme de gestion des dossiers contentieux</p>
        </div>
        <button class="login-btn" (click)="login()">
          <span>🔐</span>
          Se connecter avec Keycloak
        </button>
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
  `]
})
export class LoginComponent implements OnInit {

  constructor(
    private keycloak: KeycloakService,
    private router: Router
  ) {}

  async ngOnInit() {
    const isLoggedIn = await Promise.resolve(
      this.keycloak.isLoggedIn() as unknown as boolean | Promise<boolean>
    );

    if (!isLoggedIn) return; // ← pas connecté, on affiche le bouton

    // ✅ CORRECTION : vérifier UPDATE_PASSWORD AVANT de rediriger
    const keycloakInstance = this.keycloak.getKeycloakInstance();
    const requiredActions: string[] =
      keycloakInstance?.tokenParsed?.['required_actions'] ?? [];

    if (requiredActions.includes('UPDATE_PASSWORD')) {
      console.log('🔐 UPDATE_PASSWORD détecté → redirection Keycloak');

      // Redirige vers la page Keycloak de changement de mot de passe
      // Après changement → retour vers /login → redirectByRole() s'exécute
      await keycloakInstance.login({
        action: 'UPDATE_PASSWORD',
        redirectUri: window.location.origin + '/login'
      });
      return; // ← important : stopper la suite
    }

    // ← Ici UPDATE_PASSWORD n'existe pas → redirection normale
    this.redirectByRole();
  }

  async login() {
    await this.keycloak.login({
      redirectUri: window.location.origin + '/login' // ← retour sur /login après auth
    });
  }

  private redirectByRole() {
    const roles = this.keycloak.getUserRoles();
    const hasRole = (expected: string): boolean => {
      const normalize = (r: string) => r.replace(/^ROLE_/, '').toUpperCase();
      return roles.some(r => normalize(r) === normalize(expected));
    };

    if (hasRole('ADMIN'))                  this.router.navigate(['/admin/dashboard']);
    else if (hasRole('AGENT'))             this.router.navigate(['/agent/dashboard']);
    else if (hasRole('AVOCAT'))            this.router.navigate(['/avocat/dashboard']);
    else if (hasRole('VALIDATEUR_JURIDIQUE')) this.router.navigate(['/validateur/juridique/dashboard']);
    else if (hasRole('VALIDATEUR_FINANCIER')) this.router.navigate(['/validateur/financier/dashboard']);
    else if (hasRole('PRESTATAIRE') ||
             hasRole('EXPERT')     ||
             hasRole('HUISSIER'))          this.router.navigate(['/prestataire/dashboard']);
    else                                   this.router.navigate(['/access-denied']);
  }
}