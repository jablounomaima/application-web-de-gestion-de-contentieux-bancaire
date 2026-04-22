import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

@Component({
    selector: 'app-access-denied',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="denied-container">
      <div class="denied-card">
        <span class="denied-icon">🚫</span>
        <h1>Accès Refusé</h1>
        <p>Vous n'avez pas les permissions nécessaires pour accéder à cette page.</p>
        <div class="actions">
          <button class="btn-back" (click)="goBack()">← Retour</button>
          <button class="btn-logout" (click)="logout()">Se déconnecter</button>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .denied-container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f4f7f6;
    }
    .denied-card {
      background: white;
      border-radius: 16px;
      padding: 50px 40px;
      text-align: center;
      box-shadow: 0 10px 30px rgba(0,0,0,0.1);
      max-width: 420px;
      width: 100%;
    }
    .denied-icon {
      font-size: 4rem;
      display: block;
      margin-bottom: 20px;
    }
    h1 {
      color: #c0392b;
      font-size: 1.8rem;
      font-weight: 700;
      margin: 0 0 15px 0;
      font-family: 'Inter', sans-serif;
    }
    p {
      color: #666;
      font-size: 0.95rem;
      line-height: 1.6;
      margin-bottom: 30px;
    }
    .actions {
      display: flex;
      gap: 15px;
      justify-content: center;
    }
    .btn-back {
      padding: 12px 24px;
      background: #001f3f;
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 0.95rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      font-family: 'Inter', sans-serif;
    }
    .btn-back:hover { background: #003d7a; }
    .btn-logout {
      padding: 12px 24px;
      background: transparent;
      color: #c0392b;
      border: 2px solid #c0392b;
      border-radius: 8px;
      font-size: 0.95rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      font-family: 'Inter', sans-serif;
    }
    .btn-logout:hover {
      background: #c0392b;
      color: white;
    }
  `]
})
export class AccessDeniedComponent {

    constructor(
        private router: Router,
        private keycloak: KeycloakService
    ) { }

    goBack() {
        this.router.navigate(['/login']);
    }

    logout() {
        this.keycloak.logout(window.location.origin);
    }
}