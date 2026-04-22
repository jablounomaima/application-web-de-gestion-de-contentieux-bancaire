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

  hasRole(role: string): boolean {
    const normalize = (r: string) => r.replace(/^ROLE_/, '').toUpperCase();
    return this.roles.some(r => normalize(r) === normalize(role));
  }

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
