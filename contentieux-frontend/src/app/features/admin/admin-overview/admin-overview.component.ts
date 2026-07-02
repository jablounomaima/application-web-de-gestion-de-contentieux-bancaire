import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminService, Agent } from '../../../core/services/admin.service';

@Component({
  selector: 'app-admin-overview',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="overview-container">
      <section class="hero-card">
        <div class="hero-copy">
          <span class="admin-badge">⚙️ ADMIN PANEL</span>
          <h1>Vue globale</h1>
          <p class="subtitle">Supervision centralisée des agents, agences et validateurs.</p>
        </div>
        <div class="hero-summary">
          <div class="summary-pill">
            <span class="summary-dot"></span>
            <span>En ligne</span>
          </div>
          <div class="summary-value">{{ totalAccounts }}</div>
          <div class="summary-label">comptes administrés</div>
        </div>
      </section>

      <section class="summary-grid">
        <div class="summary-card primary">
          <span class="summary-label">Total géré</span>
          <strong class="summary-value">{{ totalAccounts }}</strong>
          <p>Comptes et profils administrés</p>
        </div>
        <div class="summary-card accent">
          <span class="summary-label">Agents actifs</span>
          <strong class="summary-value">{{ activeAgents }}</strong>
          <p>{{ activeAgentRatio }}% de la base agents</p>
        </div>
        <div class="summary-card neutral">
          <span class="summary-label">État du système</span>
          <strong class="summary-value">En ligne</strong>
          <p>Supervision disponible en temps réel</p>
        </div>
      </section>

     

      <section class="charts-panel">
        <div class="chart-card">
          <div class="chart-header">
            <h3>Répartition des entités</h3>
            <span>Vue synthétique</span>
          </div>
          <div class="bars">
            <div class="bar-item" *ngFor="let bar of chartBars">
              <div class="bar-track">
                <div class="bar-fill" [style.height.%]="bar.percent"></div>
              </div>
              <div class="bar-meta">
                <strong>{{ bar.value }}</strong>
                <span>{{ bar.label }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="chart-card">
          <div class="chart-header">
            <h3>Activité des agents</h3>
            <span>Actifs / total</span>
          </div>
          <div class="ring-card">
            <svg class="ring" viewBox="0 0 140 140">
              <circle cx="70" cy="70" r="54" class="ring-bg"></circle>
              <circle cx="70" cy="70" r="54" class="ring-progress"
                      [style.strokeDasharray]="ringCircumference"
                      [style.strokeDashoffset]="ringOffset"></circle>
            </svg>
            <div class="ring-center">
              <strong>{{ activeAgentRatio }}%</strong>
              <span>{{ agentsActifs }} actifs</span>
            </div>
          </div>
        </div>
      </section>

      <section class="quick-actions">
        <div class="section-title-row">
          <h2>Accès rapide</h2>
          <span class="section-hint">Gestion centralisée</span>
        </div>
        <div class="actions-grid">
          <div class="action-card" routerLink="/admin/dashboard" [queryParams]="{tab: 'agents'}">
            <span class="action-icon">👥</span>
            <strong>Gérer les Agents</strong>
            <p>Créer, modifier ou désactiver des agents bancaires.</p>
          </div>
          <div class="action-card" routerLink="/admin/dashboard" [queryParams]="{tab: 'agences'}">
            <span class="action-icon">🏢</span>
            <strong>Gérer les Agences</strong>
            <p>Administrer le réseau d’agences bancaires.</p>
          </div>
          <div class="action-card" routerLink="/admin/dashboard" [queryParams]="{tab: 'validateurs'}">
            <span class="action-icon">⚖️</span>
            <strong>Gérer les Validateurs</strong>
            <p>Configurer les validateurs juridiques et financiers.</p>
          </div>
        </div>
      </section>

      <div class="status-row" *ngIf="isLoading">Chargement des données…</div>
      <div class="status-row error" *ngIf="errorMessage">{{ errorMessage }}</div>
    </div>
  `,
  styles: [`
    .overview-container{padding:24px;background:linear-gradient(135deg,#f8fbff 0%,#eef4ff 100%);min-height:100vh;font-family:'Segoe UI',sans-serif}
    .hero-card{display:flex;justify-content:space-between;align-items:center;gap:16px;margin-bottom:24px;padding:22px 24px;background:#fff;border:1px solid #e5edf7;border-radius:20px;box-shadow:0 10px 30px rgba(15,23,42,.06)}
    .admin-badge{background:#eff6ff;color:#2563eb;padding:6px 12px;border-radius:999px;font-weight:800;font-size:.75rem;letter-spacing:.08em;display:inline-block;margin-bottom:10px}
    h1{font-size:1.8rem;color:#0f172a;margin:0;font-weight:800}
    .subtitle,.summary-card p,.section-hint,.status-row{color:#64748b}
    .subtitle{margin:6px 0 0;font-size:1rem}
    .hero-summary{min-width:190px;text-align:right;padding:16px 18px;border-radius:18px;background:linear-gradient(135deg,#2563eb 0%,#60a5fa 100%);color:#fff;box-shadow:0 12px 28px rgba(37,99,235,.22)}
    .summary-pill{display:inline-flex;align-items:center;gap:8px;padding:6px 10px;border-radius:999px;background:rgba(255,255,255,.16);font-size:.76rem;font-weight:700;margin-bottom:10px}
    .summary-dot{width:8px;height:8px;border-radius:50%;background:#34d399}
    .summary-value{font-size:1.7rem;font-weight:800;line-height:1}
    .summary-label{font-size:.82rem;opacity:.9;margin-top:6px}
    .summary-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:20px}
    .summary-card,.stat-card,.chart-card,.quick-actions{background:#fff;border:1px solid #e5edf7;border-radius:18px;box-shadow:0 8px 24px rgba(15,23,42,.04)}
    .summary-card{padding:16px 18px}
    .summary-card.primary{background:linear-gradient(135deg,#eff6ff 0%,#fff 100%)}
    .summary-card.accent{background:linear-gradient(135deg,#f0fdf4 0%,#fff 100%)}
    .summary-card.neutral{background:linear-gradient(135deg,#f8fafc 0%,#fff 100%)}
    .summary-card .summary-label{display:block;color:#64748b;font-size:.78rem;font-weight:700;text-transform:uppercase;letter-spacing:.06em;margin-bottom:8px;opacity:1}
    .summary-card .summary-value{display:block;font-size:1.35rem;font-weight:800;color:#0f172a;margin-bottom:6px}
    .summary-card p{margin:0;font-size:.9rem}
    .stats-row{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:24px}
    .stat-card{padding:16px;display:flex;align-items:center;gap:12px;transition:all .3s;cursor:pointer}
    .stat-card:hover{transform:translateY(-3px);box-shadow:0 12px 24px rgba(15,23,42,.1)}
    .stat-icon{width:46px;height:46px;border-radius:12px;display:flex;align-items:center;justify-content:center;font-size:1.3rem;flex-shrink:0}
    .stat-card.blue .stat-icon{background:#eff6ff}.stat-card.purple .stat-icon{background:#faf5ff}.stat-card.orange .stat-icon{background:#fff7ed}.stat-card.green .stat-icon{background:#f0fdf4}
    .stat-info h4{margin:0 0 4px;color:#64748b;font-size:.8rem;font-weight:700;text-transform:uppercase}
    .stat-value{font-size:1.45rem;font-weight:800;color:#1e293b;line-height:1}
    .stat-info small{color:#94a3b8;font-size:.78rem;cursor:pointer}
    .charts-panel{display:grid;grid-template-columns:1.2fr .8fr;gap:16px;margin-bottom:20px}
    .chart-card{padding:16px 18px}
    .chart-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:12px}
    .chart-header h3{margin:0;font-size:1rem;color:#0f172a}
    .chart-header span{color:#64748b;font-size:.8rem;font-weight:600}
    .bars{display:flex;align-items:flex-end;justify-content:space-between;gap:10px;height:170px}
    .bar-item{display:flex;flex-direction:column;align-items:center;gap:6px;flex:1}
    .bar-track{width:100%;max-width:42px;height:114px;display:flex;align-items:flex-end;background:#f1f5f9;border-radius:999px;overflow:hidden}
    .bar-fill{width:100%;background:linear-gradient(180deg,#60a5fa 0%,#2563eb 100%);border-radius:999px;min-height:8px}
    .bar-meta{display:flex;flex-direction:column;align-items:center;gap:2px}
    .bar-meta strong{font-size:.9rem;color:#0f172a}
    .bar-meta span{font-size:.72rem;color:#64748b}
    .ring-card{display:flex;align-items:center;justify-content:center;gap:14px;padding:8px 0}
    .ring{width:132px;height:132px;transform:rotate(-90deg)}
    .ring-bg,.ring-progress{fill:none;stroke-width:12}
    .ring-bg{stroke:#e2e8f0}
    .ring-progress{stroke:#2563eb;stroke-linecap:round;transition:stroke-dashoffset .3s ease}
    .ring-center{display:flex;flex-direction:column;align-items:flex-start}
    .ring-center strong{font-size:1.45rem;color:#0f172a}
    .ring-center span{color:#64748b;font-size:.82rem}
    .quick-actions{border-radius:20px;padding:18px 20px}
    .section-title-row{display:flex;justify-content:space-between;align-items:center;margin-bottom:14px}
    .quick-actions h2{font-size:1rem;font-weight:800;color:#1e293b;margin:0}
    .actions-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:14px}
    .action-card{background:linear-gradient(180deg,#fff 0%,#f8fbff 100%);border-radius:16px;padding:16px;box-shadow:0 6px 16px rgba(15,23,42,.04);border:1px solid #e5edf7;cursor:pointer;transition:all .3s;display:flex;flex-direction:column;gap:8px}
    .action-card:hover{transform:translateY(-3px);box-shadow:0 12px 24px rgba(15,23,42,.08);border-color:#c7d2fe}
    .action-icon{font-size:1.55rem}
    .action-card strong{font-size:.95rem;color:#1e293b;font-weight:700}
    .action-card p{margin:0;color:#64748b;font-size:.82rem;line-height:1.4}
    .status-row{margin-top:12px;font-size:.9rem}.status-row.error{color:#dc2626;font-weight:600}
    @media (max-width:1024px){.stats-row,.summary-grid,.actions-grid{grid-template-columns:repeat(2,1fr)}.charts-panel{grid-template-columns:1fr}}
    @media (max-width:640px){.overview-container{padding:16px}.hero-card{flex-direction:column;align-items:flex-start}.hero-summary{width:100%;text-align:left}.stats-row,.summary-grid,.actions-grid{grid-template-columns:1fr}}
  `]
})
export class AdminOverviewComponent implements OnInit {

  stats = { agents: 0, agences: 0, validateursJuridiques: 0, validateursFinanciers: 0 };
  agentsList: Agent[] = [];
  isLoading = true;
  errorMessage = '';

  constructor(private adminService: AdminService) {}

  get totalAccounts(): number {
    return this.stats.agents + this.stats.agences + this.stats.validateursJuridiques + this.stats.validateursFinanciers;
  }

  get activeAgents(): number {
    return this.agentsList.filter(agent => agent.actif).length;
  }

  get agentsActifs(): number {
    return this.activeAgents;
  }

  get activeAgentRatio(): number {
    if (this.stats.agents === 0) return 0;
    return Math.round((this.agentsActifs / this.stats.agents) * 100);
  }

  get ringCircumference(): number {
    return 2 * Math.PI * 54;
  }

  get ringOffset(): number {
    const progress = this.activeAgentRatio / 100;
    return this.ringCircumference * (1 - progress);
  }

  get chartSeries(): Array<{ label: string; value: number }> {
    return [
      { label: 'Agents', value: this.stats.agents },
      { label: 'Agences', value: this.stats.agences },
      { label: 'Valid. J', value: this.stats.validateursJuridiques },
      { label: 'Valid. F', value: this.stats.validateursFinanciers }
    ];
  }

  get chartMaxValue(): number {
    const values = this.chartSeries.map(item => item.value);
    return Math.max(...values, 1);
  }

  get chartBars(): Array<{ label: string; value: number; percent: number }> {
    return this.chartSeries.map(item => ({
      label: item.label,
      value: item.value,
      percent: Math.round((item.value / this.chartMaxValue) * 100)
    }));
  }

  ngOnInit(): void {
    this.loadOverview();
  }

  private loadOverview(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.adminService.getAgents().subscribe({
      next: (data) => {
        this.agentsList = Array.isArray(data?.agents) ? data.agents : [];
        this.stats.agents = this.agentsList.length;
        this.stats.agences = Array.isArray(data?.agences) ? data.agences.length : 0;
        this.loadValidateurs();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les statistiques.';
        this.isLoading = false;
      }
    });
  }

  private loadValidateurs(): void {
    this.adminService.getValidateurs().subscribe({
      next: (data) => {
        this.stats.validateursJuridiques = Array.isArray(data?.validateursJuridiques) ? data.validateursJuridiques.length : 0;
        this.stats.validateursFinanciers = Array.isArray(data?.validateursFinanciers) ? data.validateursFinanciers.length : 0;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les statistiques.';
        this.isLoading = false;
      }
    });
  }
}