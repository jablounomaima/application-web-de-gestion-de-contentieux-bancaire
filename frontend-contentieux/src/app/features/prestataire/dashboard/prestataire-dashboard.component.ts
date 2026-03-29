import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-prestataire-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="layout">

      <!-- Sidebar -->
      <aside class="sidebar">
        <div class="sidebar-logo">
          <span>⚖</span>
          <span>Contentieux</span>
        </div>
        <nav>
          <a class="nav-item active">📊 Dashboard</a>
          <a class="nav-item" (click)="navigate('/prestataire/missions')">📋 Mes missions</a>
          <a class="nav-item" (click)="navigate('/prestataire/change-password')">🔐 Mot de passe</a>
          <a class="nav-item logout" (click)="logout()">🚪 Déconnexion</a>
        </nav>
      </aside>

      <!-- Main -->
      <main class="main">

        <!-- Topbar -->
        <header class="topbar">
          <h2>Dashboard {{ roleLabel }}</h2>
          <div class="user-info">
            <span class="user-avatar">{{ initiale }}</span>
            <span>{{ user?.prenom }} {{ user?.nom }}</span>
          </div>
        </header>

        <!-- Content -->
        <div class="content">

          <!-- Stats -->
          <div class="stats-grid">
            <div class="stat-card blue">
              <span class="stat-icon">📋</span>
              <div>
                <b class="stat-value">–</b>
                <p class="stat-label">Missions assignées</p>
              </div>
            </div>
            <div class="stat-card orange">
              <span class="stat-icon">⏳</span>
              <div>
                <b class="stat-value">–</b>
                <p class="stat-label">En cours</p>
              </div>
            </div>
            <div class="stat-card green">
              <span class="stat-icon">✅</span>
              <div>
                <b class="stat-value">–</b>
                <p class="stat-label">Terminées</p>
              </div>
            </div>
            <div class="stat-card purple">
              <span class="stat-icon">💰</span>
              <div>
                <b class="stat-value">–</b>
                <p class="stat-label">Factures soumises</p>
              </div>
            </div>
          </div>

          <!-- Welcome -->
          <div class="welcome-banner">
            <h3>Bienvenue, {{ user?.prenom }} 👋</h3>
            <p>Vous êtes connecté en tant que <b>{{ roleLabel }}</b>.</p>
            <button class="btn-missions" (click)="navigate('/prestataire/missions')">
              📋 Voir mes missions →
            </button>
          </div>

          <!-- Info carte -->
          <div class="info-card">
            <h4>📌 Vos actions disponibles</h4>
            <ul>
              <li>✅ Consulter vos missions assignées</li>
              <li>📄 Soumettre un PV de mission</li>
              <li>💳 Soumettre une facture d'honoraires</li>
              <li>🔐 Changer votre mot de passe</li>
            </ul>
          </div>

        </div>
      </main>
    </div>
  `,
  styles: [`
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Segoe UI', Arial, sans-serif; }

    .layout {
      display: flex;
      min-height: 100vh;
      font-family: 'Segoe UI', Arial, sans-serif;
    }

    /* ── Sidebar ── */
    .sidebar {
      width: 240px;
      background: #1e3a5f;
      color: white;
      display: flex;
      flex-direction: column;
      padding: 20px 0;
      position: fixed;
      height: 100vh;
      overflow-y: auto;
    }
    .sidebar-logo {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 16px;
      font-weight: bold;
      padding: 0 20px 24px;
      border-bottom: 1px solid rgba(255,255,255,.1);
    }
    .nav-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px 20px;
      color: rgba(255,255,255,.8);
      cursor: pointer;
      font-size: 14px;
      transition: all .2s;
      user-select: none;
    }
    .nav-item:hover,
    .nav-item.active {
      background: rgba(255,255,255,.1);
      color: white;
    }
    .nav-item.logout {
      color: #fca5a5;
      margin-top: auto;
      position: absolute;
      bottom: 20px;
      width: 100%;
    }

    /* ── Main ── */
    .main {
      margin-left: 240px;
      flex: 1;
      background: #f8fafc;
    }

    /* ── Topbar ── */
    .topbar {
      background: white;
      padding: 16px 28px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid #e5e7eb;
      box-shadow: 0 1px 4px rgba(0,0,0,.05);
    }
    .topbar h2 { font-size: 18px; color: #1e3a5f; }
    .user-info {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 14px;
      color: #374151;
    }
    .user-avatar {
      width: 34px; height: 34px;
      background: #2563eb;
      color: white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      font-size: 14px;
    }

    /* ── Content ── */
    .content { padding: 28px; }

    /* ── Stats ── */
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;
      margin-bottom: 24px;
    }
    .stat-card {
      background: white;
      border-radius: 10px;
      padding: 20px;
      display: flex;
      align-items: center;
      gap: 16px;
      box-shadow: 0 1px 4px rgba(0,0,0,.06);
      border-left: 4px solid transparent;
    }
    .stat-card.blue   { border-color: #2563eb; }
    .stat-card.green  { border-color: #059669; }
    .stat-card.orange { border-color: #d97706; }
    .stat-card.purple { border-color: #7c3aed; }
    .stat-icon { font-size: 28px; }
    .stat-value {
      display: block;
      font-size: 24px;
      font-weight: bold;
      color: #1e3a5f;
    }
    .stat-label { font-size: 13px; color: #6b7280; }

    /* ── Welcome ── */
    .welcome-banner {
      background: linear-gradient(135deg, #1e3a5f, #2563eb);
      color: white;
      border-radius: 10px;
      padding: 24px 28px;
      margin-bottom: 20px;
    }
    .welcome-banner h3 { font-size: 18px; margin-bottom: 6px; }
    .welcome-banner p  { opacity: .85; font-size: 14px; margin-bottom: 16px; }
    .btn-missions {
      background: white;
      color: #1e3a5f;
      border: none;
      padding: 9px 20px;
      border-radius: 6px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: opacity .2s;
    }
    .btn-missions:hover { opacity: .9; }

    /* ── Info card ── */
    .info-card {
      background: white;
      border-radius: 10px;
      padding: 24px;
      box-shadow: 0 1px 4px rgba(0,0,0,.06);
    }
    .info-card h4 {
      font-size: 15px;
      color: #1e3a5f;
      margin-bottom: 14px;
      padding-bottom: 10px;
      border-bottom: 2px solid #e5e7eb;
    }
    .info-card ul {
      list-style: none;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .info-card li {
      font-size: 14px;
      color: #374151;
      padding: 8px 12px;
      background: #f8fafc;
      border-radius: 6px;
    }
  `]
})
export class PrestataireDashboardComponent {

  user = this.authService.getCurrentUser();

  get roleLabel(): string {
    const roles = this.authService.getRoles();
    if (roles.includes('avocat'))   return 'Avocat';
    if (roles.includes('huissier')) return 'Huissier';
    if (roles.includes('expert'))   return 'Expert';
    return 'Prestataire';
  }

  get initiale(): string {
    return this.user?.prenom?.charAt(0)?.toUpperCase() ?? 'P';
  }

  constructor(private authService: AuthService, private router: Router) {}

  navigate(path: string): void {
    this.router.navigate([path]);
  }

  logout(): void {
    this.authService.logout();
  }
}
