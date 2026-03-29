import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  { path: 'dashboard', loadComponent: () =>
      import('./dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent) }
];
