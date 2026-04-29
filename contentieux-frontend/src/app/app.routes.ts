import { Routes } from '@angular/router';
import { AuthGuard } from './core/auth/auth.guard';
import { AvocatAudiencesComponent } from './features/avocat/avocat-audiences/avocat-audiences.component';
import { AvocatJugementComponent } from './features/avocat/avocat-jugement/avocat-jugement.component';
import { AgentActifGuard } from './core/auth/agent-actif.guard';

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
    canActivate: [AuthGuard,AgentActifGuard],
    data: { roles: ['ROLE_AGENT', 'ROLE_ADMIN'] },
    loadComponent: () =>
      import('./features/agent/agent-dashboard/agent-dashboard.component')
        .then(m => m.AgentDashboardComponent)
  },

  {
    path: 'agent/liste',
    canActivate: [AuthGuard,AgentActifGuard],
    data: { roles: ['ROLE_AGENT', 'ROLE_ADMIN'] },
    loadComponent: () =>
      import('./features/agent/agent-dossiers-liste/agent-dossiers-liste.component')
        .then(m => m.AgentDossiersListeComponent)
  },

  {
    path: 'avocat/affaires',
    loadComponent: () =>
      import('./features/avocat/avocat-affaires-list/avocat-affaires-list.component')

        .then(m => m.AvocatAffairesListComponent),
  },
  {
    path: 'avocat/affaires/:affaireId/dossier',
    loadComponent: () =>
      import('./features/avocat/avocat-dossier-detail/avocat-dossier-detail.component')
        .then(m => m.AvocatDossierDetailComponent),
  },

  // ✅ NOUVEAU — avant la route générique :id
{
  path: 'agent/dossiers/:id/prestations/lancer',
  canActivate: [AuthGuard , AgentActifGuard],
  data: { roles: ['ROLE_AGENT', 'ROLE_ADMIN'] },
  loadComponent: () =>
    import('./features/agent/lancer-procedure/lancer-procedure.component')
      .then(m => m.LancerProcedureComponent)
},

{
  path: 'agent/dossiers/:dossierId/affaire/lancer',
  loadComponent: () =>
    import('./features/agent/lancer-affaire/lancer-affaire.component')
      .then(m => m.LancerAffaireComponent),
},
{
  path: 'agent/dossiers/:dossierId/affaire',
  loadComponent: () =>
    import('./features/agent/voir-affaire/voir-affaire.component')
      .then(m => m.VoirAffaireComponent),
},


{
  path: 'agent/dossiers/:dossierId/prestation/:prestationId/designer-avocat',
  loadComponent: () =>
    import('./features/agent/designer-avocat/designer-avocat.component')
      .then(m => m.DesignerAvocatComponent),
  // canActivate: [AuthGuard]   ← si vous avez un guard
},

{
  path: 'avocat/affaires/:id/tribunal',
  loadComponent: () =>
    import('./features/avocat/avocat-tribunal/avocat-tribunal.component')
      .then(m => m.AvocatTribunalComponent)
},

{
  path: 'avocat/affaires/:id/honoraires',
  loadComponent: () =>
    import('./features/avocat/avocat-honoraires/avocat-honoraires.component')
      .then(m => m.AvocatHonorairesComponent)
},


  {
    path: 'agent/dossiers',
    canActivate: [AuthGuard,, AgentActifGuard],
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
    canActivate: [AuthGuard, AgentActifGuard],
    data: { roles: ['ROLE_AGENT', 'ROLE_ADMIN'] },
    loadComponent: () =>
      import('./features/agent/agent-dossier-modifier/agent-dossier-modifier.component')
        .then(m => m.AgentDossierModifierComponent)
  },
  {
    path: 'agent/dossiers/:id',             // ← Detail route (generic)
    canActivate: [AuthGuard, AgentActifGuard],
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




   // ── Création d'un prestataire ────────────────────────────────
   {
    path: 'agent/prestataires/nouveau',
    canActivate: [AuthGuard , AgentActifGuard],
    data: { roles: ['ROLE_AGENT', 'ROLE_ADMIN'] },
    loadComponent: () =>
      import('./features/agent/creer-prestataire/creer-prestataire.component').then(
        m => m.CreerPrestataireComponent
      ),
  },

  

  {
    path: 'agent/prestataires/liste',
    canActivate: [AuthGuard ,, AgentActifGuard],
    data: { roles: ['ROLE_AGENT', 'ROLE_ADMIN'] },
    loadComponent: () =>
      import('./features/agent/prestataires-liste/prestataires-liste.component').then(
        m => m.PrestatairesListeComponent
      ),
  },

  {
    path: 'agent/prestataires/modifier/:id',
    loadComponent: () =>
      import('./features/agent/modifier-prestataire/modifier-prestataire.component').then(
        m => m.ModifierPrestataireComponent
      ),
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
  {
    path: 'avocat/mes-dossiers',
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_AVOCAT'] },
    loadComponent: () =>
      import('./features/avocat/dossiers-avocat/dossiers-avocat.component')
        .then(m => m.DossiersAvocatComponent)
  },

  {
    path: 'avocat/affaires/:affaireId/audiences',
    component: AvocatAudiencesComponent
  },


  {
    path: 'avocat/affaires/:id/jugement',
    component: AvocatJugementComponent
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




 // ✅ Route compte désactivé — ajouter avant le fallback **
 {
  path: 'compte-desactive',
  loadComponent: () =>
    import('./core/auth/compte-desactive/compte-desactive.component')
      .then(m => m.CompteDesactiveComponent)
},

  // ── Fallback ───────────────────────────────────────
  { path: '**', redirectTo: 'login', pathMatch: 'full' }
];