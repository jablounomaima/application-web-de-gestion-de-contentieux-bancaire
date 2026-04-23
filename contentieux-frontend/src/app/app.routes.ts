import { Routes } from '@angular/router';
import { AuthGuard } from './core/auth/auth.guard';

export const routes: Routes = [

  // ── Redirections ───────────────────────────────────
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },

  // ── Pages publiques ────────────────────────────────
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component')
        .then(m => m.LoginComponent)
  },
  {
    path: 'access-denied',
    loadComponent: () =>
      import('./features/auth/access-denied/access-denied.component')
        .then(m => m.AccessDeniedComponent)
  },

  // ── AGENT ──────────────────────────────────────────
  {
    path: 'agent/dashboard',
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_AGENT', 'ROLE_ADMIN'] },
    loadComponent: () =>
      import('./features/agent/agent-dashboard/agent-dashboard.component')
        .then(m => m.AgentDashboardComponent)
  },
  {
    path: 'agent/dossiers',
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_AGENT', 'ROLE_ADMIN'] },
    loadComponent: () =>
      import('./features/agent/agent-dossiers/agent-dossiers.component')
        .then(m => m.AgentDossiersComponent)
  },
  {
    // Nouveau dossier = ouvrir le dashboard avec le modal
    path: 'agent/nouveau-dossier',
    redirectTo: 'agent/dashboard',
    pathMatch: 'full' // Ajouté pour la cohérence
  },
  {
    path: 'agent/dossier/:id',
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_AGENT', 'ROLE_ADMIN'] },
    loadComponent: () =>
      import('./features/agent/agent-dossier-detail/agent-dossier-detail.component')
        .then(m => m.AgentDossierDetailComponent)
  },

  // ── PRESTATAIRE / EXPERT / HUISSIER ────────────────
  {
    path: 'prestataire/dashboard',
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_PRESTATAIRE', 'ROLE_EXPERT', 'ROLE_HUISSIER'] },
    loadComponent: () =>
      import('./features/prestataire/prestataire-dashboard/prestataire-dashboard.component')
        .then(m => m.PrestataireDashboardComponent)
  },
  {
    path: 'prestataire/missions',
    redirectTo: 'prestataire/dashboard',
    pathMatch: 'full' // Ajouté pour la cohérence
  },
  {
    path: 'prestataire/factures',
    redirectTo: 'prestataire/dashboard',
    pathMatch: 'full' // Ajouté pour la cohérence
  },

  // ── AVOCAT ─────────────────────────────────────────
  {
    path: 'avocat/dashboard',
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_AVOCAT'] },
    loadComponent: () =>
      import('./features/avocat/avocat-dashboard/avocat-dashboard.component')
        .then(m => m.AvocatDashboardComponent)
  },
  {
    path: 'avocat/dossiers',
    redirectTo: 'avocat/dashboard',
    pathMatch: 'full' // Ajouté pour la cohérence
  },

  // ── ADMIN ──────────────────────────────────────────
  {
    path: 'admin/overview',
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_ADMIN'] },
    loadComponent: () =>
      import('./features/admin/admin-overview/admin-overview.component')
        .then(m => m.AdminOverviewComponent)
  },
  {
    path: 'admin/dashboard',
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_ADMIN'] },
    loadComponent: () =>
      import('./features/admin/admin-dashboard/admin-dashboard.component')
        .then(m => m.AdminDashboardComponent)
  },
  {
    path: 'admin',
    redirectTo: 'admin/overview',
    pathMatch: 'full' // Ajouté pour la cohérence
  },

  // ── VALIDATEURS ────────────────────────────────────
  {
    path: 'validateur/juridique/dashboard',
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_VALIDATEUR_JURIDIQUE'] },
    loadComponent: () =>
      import('./features/validateur-juridique/validateur-juridique-dashboard/validateur-juridique-dashboard.component')
        .then(m => m.ValidateurJuridiqueDashboardComponent)
  },
  {
    path: 'validateur/juridique/a-valider',
    redirectTo: 'validateur/juridique/dashboard',
    pathMatch: 'full' // Ajouté pour la cohérence
  },
  {
    path: 'validateur/financier/dashboard',
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_VALIDATEUR_FINANCIER'] },
    loadComponent: () =>
      import('./features/validateur-financier/validateur-financier-dashboard/validateur-financier-dashboard.component')
        .then(m => m.ValidateurFinancierDashboardComponent)
  },
  {
    path: 'validateur/financier/factures',
    redirectTo: 'validateur/financier/dashboard',
    pathMatch: 'full' // Ajouté pour la cohérence
  },

  // ── Fallback ───────────────────────────────────────
  { path: '**', redirectTo: 'login', pathMatch: 'full' } // Ajouté pour la cohérence
];