import { Component } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-validateur-financier-dashboard',
  standalone: true,
  template: `
    <div style="padding:40px;font-family:Arial">
      <h2 style="color:#064e3b">💰 Dashboard Validateur Financier</h2>
      <p>Bienvenue {{ user?.nom }}</p>
      <button (click)="logout()"
        style="padding:9px 18px;background:#dc2626;color:white;
               border:none;border-radius:6px;cursor:pointer;margin-top:16px">
        🚪 Déconnexion
      </button>
    </div>
  `
})
export class ValidateurFinancierDashboardComponent {
  user = this.authService.getCurrentUser();
  constructor(private authService: AuthService) {}
  logout() { this.authService.logout(); }
}
