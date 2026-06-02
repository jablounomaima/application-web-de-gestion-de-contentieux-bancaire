import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    // ✅ NotificationsComponent retiré — la cloche est dans AppComponent (navbar)
  ],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent implements OnInit {

  @Input() username: string = '';
  @Input() roles: string[]  = [];

  constructor(
    private keycloak: KeycloakService,
    private router:   Router
  ) {}

  ngOnInit(): void {
    // ✅ WebSocket + notifications initialisés dans AppComponent uniquement.
    // La sidebar ne gère que la navigation et l'affichage du menu.
  }

  // ── Rôles ─────────────────────────────────────────────────────

  hasRole(role: string): boolean {
    if (!this.roles || this.roles.length === 0) return false;
    const normalize = (r: string) => r.replace(/^ROLE_/, '').toUpperCase();
    return this.roles.some(r => normalize(r) === normalize(role));
  }

  isAnyRoleMatched(): boolean {
    const managedRoles = [
      'AGENT', 'ADMIN', 'AVOCAT', 'PRESTATAIRE',
      'EXPERT', 'HUISSIER', 'VALIDATEUR_JURIDIQUE', 'VALIDATEUR_FINANCIER'
    ];
    return managedRoles.some(role => this.hasRole(role));
  }

  getRoleDisplayName(): string {
    if (this.hasRole('ADMIN'))                return 'Administrateur';
    if (this.hasRole('AGENT'))                return 'Agent Bancaire';
    if (this.hasRole('AVOCAT'))               return 'Avocat';
    if (this.hasRole('EXPERT'))               return 'Expert';
    if (this.hasRole('HUISSIER'))             return 'Huissier';
    if (this.hasRole('PRESTATAIRE'))          return 'Prestataire';
    if (this.hasRole('VALIDATEUR_JURIDIQUE')) return 'Validateur Juridique';
    if (this.hasRole('VALIDATEUR_FINANCIER')) return 'Validateur Financier';
    return 'Utilisateur';
  }

  
}