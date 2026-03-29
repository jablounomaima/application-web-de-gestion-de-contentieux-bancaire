import { Routes } from '@angular/router';

export const VALIDATEUR_FINANCIER_ROUTES: Routes = [
  { path: 'dashboard', loadComponent: () =>
      import('./financier/validateur-financier-dashboard.component')
        .then(m => m.ValidateurFinancierDashboardComponent) }
];
