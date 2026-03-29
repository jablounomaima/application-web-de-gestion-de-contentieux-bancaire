import { Routes } from '@angular/router';

export const PRESTATAIRE_ROUTES: Routes = [
  { path: 'dashboard', loadComponent: () =>
      import('./dashboard/prestataire-dashboard.component')
        .then(m => m.PrestataireDashboardComponent) }
];
