import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

interface StatCard {
  label: string;
  value: number | string;
  icon: string;
  color: string;
}

@Component({
  selector: 'app-agent-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, LayoutComponent],
  template: `
    <app-layout pageTitle="Dashboard">

      <!-- Bannière de bienvenue -->
      <div class="welcome-banner">
        <div>
          <h3>Bienvenue, {{ user?.prenom }} 👋</h3>
          <p>Vous êtes connecté en tant qu'agent bancaire — {{ user?.username }}</p>
        </div>
        <a routerLink="/agent/dossiers/creer" class="btn-new">
          ➕ Nouveau dossier
        </a>
      </div>

      <!-- Statistiques -->
      <div class="stats-grid">
        <div class="stat-card" *ngFor="let s of stats"
             [style.border-left-color]="s.color">
          <span class="stat-icon">{{ s.icon }}</span>
          <div>
            <b class="stat-value">{{ s.value }}</b>
            <p class="stat-label">{{ s.label }}</p>
          </div>
        </div>
      </div>

      <!-- Actions rapides -->
      <div class="section-title">Actions rapides</div>
      <div class="quick-actions">
        <a routerLink="/agent/dossiers" class="action-card">
          <span class="action-icon">📁</span>
          <span>Mes dossiers</span>
        </a>
        <a routerLink="/agent/dossiers/creer" class="action-card">
          <span class="action-icon">➕</span>
          <span>Nouveau dossier</span>
        </a>
        <a routerLink="/agent/prestataires" class="action-card">
          <span class="action-icon">👔</span>
          <span>Prestataires</span>
        </a>
        <a routerLink="/agent/notifications" class="action-card">
          <span class="action-icon">🔔</span>
          <span>Notifications</span>
        </a>
      </div>

    </app-layout>
  `,
  styles: [`
    .welcome-banner {
      background: linear-gradient(135deg, #1e3a5f, #2563eb);
      color: white;
      border-radius: 10px;
      padding: 24px 28px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }
    .welcome-banner h3 { font-size: 20px; margin-bottom: 6px; }
    .welcome-banner p  { opacity: 0.85; font-size: 14px; }

    .btn-new {
      padding: 10px 20px;
      background: white;
      color: #1e3a5f;
      border-radius: 8px;
      text-decoration: none;
      font-weight: 600;
      font-size: 14px;
      white-space: nowrap;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;
      margin-bottom: 28px;
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

    .stat-icon  { font-size: 28px; }
    .stat-value { font-size: 26px; font-weight: bold; color: #1e3a5f; display: block; }
    .stat-label { font-size: 13px; color: #6b7280; margin-top: 2px; }

    .section-title {
      font-size: 15px;
      font-weight: 600;
      color: #1e3a5f;
      margin-bottom: 14px;
      padding-bottom: 8px;
      border-bottom: 2px solid #e5e7eb;
    }

    .quick-actions {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 14px;
    }

    .action-card {
      background: white;
      border-radius: 10px;
      padding: 24px 16px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 10px;
      text-decoration: none;
      color: #374151;
      font-size: 14px;
      font-weight: 500;
      box-shadow: 0 1px 4px rgba(0,0,0,.06);
      border: 1px solid #e5e7eb;
      transition: all 0.2s;
      text-align: center;
    }

    .action-card:hover {
      border-color: #2563eb;
      box-shadow: 0 4px 12px rgba(37,99,235,.15);
      transform: translateY(-2px);
    }

    .action-icon { font-size: 28px; }

    @media (max-width: 900px) {
      .stats-grid, .quick-actions { grid-template-columns: repeat(2, 1fr); }
    }
  `]
})
export class AgentDashboardComponent implements OnInit {

  user = this.authService.getCurrentUser();

  stats: StatCard[] = [
    { label: 'Dossiers',   value: '–', icon: '📁', color: '#2563eb' },
    { label: 'Validés',    value: '–', icon: '✅', color: '#059669' },
    { label: 'En attente', value: '–', icon: '⏳', color: '#d97706' },
    { label: 'Rejetés',    value: '–', icon: '❌', color: '#dc2626' },
  ];

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    // TODO: charger les stats depuis l'API
    // this.dossierService.getStats().subscribe(s => {
    //   this.stats[0].value = s.total;
    //   this.stats[1].value = s.valides;
    //   this.stats[2].value = s.enAttente;
    //   this.stats[3].value = s.rejetes;
    // });
  }
}