import { Component } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-validateur-juridique-dashboard',
  standalone: true,
  template: `
    <div style="padding:40px;font-family:Arial">
      <h2 style="color:#2e1065">⚖ Dashboard Validateur Juridique</h2>
      <p>Bienvenue {{ user?.nom }}</p>
      <button (click)="logout()"
        style="padding:9px 18px;background:#dc2626;color:white;
               border:none;border-radius:6px;cursor:pointer;margin-top:16px">
        🚪 Déconnexion
      </button>
    </div>
  `
})
export class ValidateurJuridiqueDashboardComponent {
  user = this.authService.getCurrentUser();
  constructor(private authService: AuthService) {}
  logout() { this.authService.logout(); }
}
