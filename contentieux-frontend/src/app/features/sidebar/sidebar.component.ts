import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {

  @Input() username: string = '';
  @Input() roles: string[] = [];

  /**
   * Vérifie si l'utilisateur possède un rôle spécifique.
   * Normalise les rôles (ex: 'ROLE_AGENT' devient 'AGENT') pour une comparaison robuste.
   */
  hasRole(role: string): boolean {
    if (!this.roles || this.roles.length === 0) return false;
    const normalize = (r: string) => r.replace(/^ROLE_/, '').toUpperCase();
    const targetRole = normalize(role);
    return this.roles.some(r => normalize(r) === targetRole);
  }

  /**
   * Vérifie si l'utilisateur possède au moins un des rôles gérés par l'application.
   * Utilisé pour afficher le menu par défaut si aucun rôle n'est reconnu.
   */
  isAnyRoleMatched(): boolean {
    const managedRoles = [
      'AGENT', 'ADMIN', 'AVOCAT', 'PRESTATAIRE', 
      'EXPERT', 'HUISSIER', 'VALIDATEUR_JURIDIQUE', 'VALIDATEUR_FINANCIER'
    ];
    return managedRoles.some(role => this.hasRole(role));
  }

  /**
   * Retourne le libellé du rôle principal pour l'affichage.
   */
  getRoleDisplayName(): string {
    if (this.hasRole('ADMIN')) return 'Administrateur';
    if (this.hasRole('AGENT')) return 'Agent Bancaire';
    if (this.hasRole('AVOCAT')) return 'Avocat';
    if (this.hasRole('EXPERT')) return 'Expert';
    if (this.hasRole('HUISSIER')) return 'Huissier';
    if (this.hasRole('PRESTATAIRE')) return 'Prestataire';
    if (this.hasRole('VALIDATEUR_JURIDIQUE')) return 'Validateur Juridique';
    if (this.hasRole('VALIDATEUR_FINANCIER')) return 'Validateur Financier';
    return 'Utilisateur';
  }
}