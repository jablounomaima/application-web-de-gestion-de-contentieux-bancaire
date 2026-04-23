import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'app-admin-overview',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="overview-container">
      <div class="header-section">
        <span class="admin-badge">⚙️ ADMIN PANEL</span>
        <h1>Vue Globale</h1>
        <p class="subtitle">Tableau de bord général du système Contentieux</p>
      </div>

      <div class="stats-row">
        <div class="stat-card blue" routerLink="/admin/dashboard" [queryParams]="{tab: 'agents'}">
          <div class="stat-icon">👥</div>
          <div class="stat-info">
            <h4>Agents</h4>
            <span class="stat-value">{{ stats.agents }}</span>
            <small>Voir les agents →</small>
          </div>
        </div>
        <div class="stat-card purple" routerLink="/admin/dashboard" [queryParams]="{tab: 'agences'}">
          <div class="stat-icon">🏢</div>
          <div class="stat-info">
            <h4>Agences</h4>
            <span class="stat-value">{{ stats.agences }}</span>
            <small>Voir les agences →</small>
          </div>
        </div>
        <div class="stat-card orange" routerLink="/admin/dashboard" [queryParams]="{tab: 'validateurs'}">
          <div class="stat-icon">⚖️</div>
          <div class="stat-info">
            <h4>Validateurs Juridiques</h4>
            <span class="stat-value">{{ stats.validateursJuridiques }}</span>
            <small>Voir les validateurs →</small>
          </div>
        </div>
        <div class="stat-card green" routerLink="/admin/dashboard" [queryParams]="{tab: 'validateurs'}">
          <div class="stat-icon">💰</div>
          <div class="stat-info">
            <h4>Validateurs Financiers</h4>
            <span class="stat-value">{{ stats.validateursFinanciers }}</span>
            <small>Voir les validateurs →</small>
          </div>
        </div>
      </div>

      <div class="quick-actions">
        <h2>Accès Rapide</h2>
        <div class="actions-grid">
          <div class="action-card" routerLink="/admin/dashboard" [queryParams]="{tab: 'agents'}">
            <span class="action-icon">👥</span>
            <strong>Gérer les Agents</strong>
            <p>Créer, modifier ou désactiver des agents bancaires</p>
          </div>
          <div class="action-card" routerLink="/admin/dashboard" [queryParams]="{tab: 'agences'}">
            <span class="action-icon">🏢</span>
            <strong>Gérer les Agences</strong>
            <p>Administrer le réseau d'agences bancaires</p>
          </div>
          <div class="action-card" routerLink="/admin/dashboard" [queryParams]="{tab: 'validateurs'}">
            <span class="action-icon">⚖️</span>
            <strong>Gérer les Validateurs</strong>
            <p>Configurer les validateurs juridiques et financiers</p>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .overview-container { padding: 30px; background: #f8fafc; min-height: 100vh; font-family: 'Segoe UI', sans-serif; }
    .header-section { margin-bottom: 36px; }
    .admin-badge { background: #fee2e2; color: #dc2626; padding: 4px 14px; border-radius: 6px; font-weight: 800; font-size: 0.75rem; letter-spacing: 1px; display: inline-block; margin-bottom: 10px; }
    h1 { font-size: 2rem; color: #0f172a; margin: 0; font-weight: 800; }
    .subtitle { color: #64748b; margin: 6px 0 0; font-size: 1rem; }

    .stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 36px; }
    .stat-card { background: white; border-radius: 16px; padding: 22px; display: flex; align-items: center; gap: 18px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); border: 1px solid #f1f5f9; transition: all 0.3s; cursor: pointer; }
    .stat-card:hover { transform: translateY(-4px); box-shadow: 0 12px 24px rgba(0,0,0,0.1); }
    .stat-icon { width: 54px; height: 54px; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 1.7rem; flex-shrink: 0; }
    .stat-card.blue .stat-icon  { background: #eff6ff; }
    .stat-card.purple .stat-icon { background: #faf5ff; }
    .stat-card.orange .stat-icon { background: #fff7ed; }
    .stat-card.green .stat-icon  { background: #f0fdf4; }
    .stat-info h4 { margin: 0 0 4px; color: #64748b; font-size: 0.8rem; font-weight: 700; text-transform: uppercase; }
    .stat-value { font-size: 1.8rem; font-weight: 800; color: #1e293b; line-height: 1; }
    .stat-info small { color: #94a3b8; font-size: 0.78rem; cursor: pointer; }

    .quick-actions h2 { font-size: 1.2rem; font-weight: 800; color: #1e293b; margin-bottom: 20px; }
    .actions-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
    .action-card { background: white; border-radius: 16px; padding: 28px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); border: 1px solid #f1f5f9; cursor: pointer; transition: all 0.3s; display: flex; flex-direction: column; gap: 10px; }
    .action-card:hover { transform: translateY(-4px); box-shadow: 0 12px 24px rgba(0,0,0,0.1); border-color: #e0e7ff; }
    .action-icon { font-size: 2rem; }
    .action-card strong { font-size: 1rem; color: #1e293b; font-weight: 700; }
    .action-card p { margin: 0; color: #64748b; font-size: 0.85rem; line-height: 1.5; }
  `]
})
export class AdminOverviewComponent implements OnInit {

  stats = { agents: 0, agences: 0, validateursJuridiques: 0, validateursFinanciers: 0 };

  constructor(private adminService: AdminService) {}

  ngOnInit() {
    this.adminService.getAgents().subscribe({
      next: (data) => {
        this.stats.agents = data.agents.length;
        this.stats.agences = data.agences.length;
      }
    });
    this.adminService.getValidateurs().subscribe({
      next: (data) => {
        this.stats.validateursJuridiques = data.validateursJuridiques.length;
        this.stats.validateursFinanciers = data.validateursFinanciers.length;
      }
    });
  }
}