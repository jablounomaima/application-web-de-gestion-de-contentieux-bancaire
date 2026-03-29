import { Routes } from '@angular/router';

export const VALIDATEUR_JURIDIQUE_ROUTES: Routes = [
  { path: 'dashboard', loadComponent: () =>
      import('./juridique/validateur-juridique-dashboard.component')
        .then(m => m.ValidateurJuridiqueDashboardComponent) }
];
