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
    path: 'agent/liste',
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_AGENT', 'ROLE_ADMIN'] },
    loadComponent: () =>
      import('./features/agent/agent-dossiers-liste/agent-dossiers-liste.component')
        .then(m => m.AgentDossiersListeComponent)
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
    path: 'agent/nouveau-dossier',
    redirectTo: 'agent/dashboard',
    pathMatch: 'full'
  },
  
  // ⚠️ IMPORTANT: Specific routes MUST come BEFORE generic ones
  // AND they must be BEFORE the fallback route
  {
    path: 'agent/dossiers/:id/modifier',    // ← Modifier route (specific)
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_AGENT', 'ROLE_ADMIN'] },
    loadComponent: () =>
      import('./features/agent/agent-dossier-modifier/agent-dossier-modifier.component')
        .then(m => m.AgentDossierModifierComponent)
  },
  {
    path: 'agent/dossiers/:id',             // ← Detail route (generic)
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_AGENT', 'ROLE_ADMIN'] },
    loadComponent: () =>
      import('./features/agent/agent-dossier-detail/agent-dossier-detail.component')
        .then(m => m.AgentDossierDetailComponent)
  },
  {
    path: 'agent/dossier/:id',              // ← Keep old pattern for backward compatibility
    redirectTo: 'agent/dossiers/:id',
    pathMatch: 'full'
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
    pathMatch: 'full'
  },
  {
    path: 'prestataire/factures',
    redirectTo: 'prestataire/dashboard',
    pathMatch: 'full'
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
    pathMatch: 'full'
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
    pathMatch: 'full'
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
    path: 'validateur/juridique/liste',
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_VALIDATEUR_JURIDIQUE'] },
    loadComponent: () =>
      import('./features/validateur-juridique/validateur-juridique-liste/validateur-juridique-liste.component')
        .then(m => m.ValidateurJuridiqueListeComponent)
  },
  {
    path: 'validateur/juridique/a-valider',
    redirectTo: 'validateur/juridique/dashboard',
    pathMatch: 'full'
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
    path: 'validateur/financier/liste',
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_VALIDATEUR_FINANCIER'] },
    loadComponent: () =>
      import('./features/validateur-financier/validateur-financier-liste/validateur-financier-liste.component')
        .then(m => m.ValidateurFinancierListeComponent)
  },
  {
    path: 'validateur/financier/factures',
    redirectTo: 'validateur/financier/dashboard',
    pathMatch: 'full'
  },

  // ── Fallback ───────────────────────────────────────
  { path: '**', redirectTo: 'login', pathMatch: 'full' }
];