import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [

  // ── Page publique ──────────────────────────────────────
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login.component').then(m => m.LoginComponent)
  },

  // ── Admin ──────────────────────────────────────────────
  {
    path: 'admin',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/admin/admin.routes').then(m => m.adminRoutes)
  },

  // ── Agent bancaire ─────────────────────────────────────
  {
    path: 'agent',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['agent', 'admin'] },
    loadChildren: () =>
      import('./features/agent/agent.routes').then(m => m.AGENT_ROUTES)
  },

  // ── Validateur financier ───────────────────────────────
  {
    path: 'validateur/financier',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['validateur_financier'] },
    loadChildren: () =>
      import('./features/validateur/validateur-financier.routes')
        .then(m => m.VALIDATEUR_FINANCIER_ROUTES)
  },

  // ── Validateur juridique ───────────────────────────────
  {
    path: 'validateur/juridique',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['validateur_juridique'] },
    loadChildren: () =>
      import('./features/validateur/validateur-juridique.routes')
        .then(m => m.VALIDATEUR_JURIDIQUE_ROUTES)
  },

  // ── Avocat ─────────────────────────────────────────────
  {
    path: 'avocat',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['avocat'] },
    loadChildren: () =>
      import('./features/prestataire/prestataire.routes')
        .then(m => m.PRESTATAIRE_ROUTES)
  },

  // ── Huissier ───────────────────────────────────────────
  {
    path: 'huissier',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['huissier'] },
    loadChildren: () =>
      import('./features/prestataire/prestataire.routes')
        .then(m => m.PRESTATAIRE_ROUTES)
  },

  // ── Expert ─────────────────────────────────────────────
  {
    path: 'expert',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['expert'] },
    loadChildren: () =>
      import('./features/prestataire/prestataire.routes')
        .then(m => m.PRESTATAIRE_ROUTES)
  },

  // ── Accès refusé ───────────────────────────────────────
  {
    path: 'access-denied',
    loadComponent: () =>
      import('./features/auth/access-denied.component')
        .then(m => m.AccessDeniedComponent)
  },

  // ── Redirections ───────────────────────────────────────
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' }
];