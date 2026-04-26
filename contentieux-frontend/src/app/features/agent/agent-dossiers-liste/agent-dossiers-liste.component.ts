import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { DossierService } from '../../../core/services/dossier.service';

@Component({
  selector: 'app-agent-dossiers-liste',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './agent-dossiers-liste.component.html',
  styleUrls: ['./agent-dossiers-liste.component.scss']
})
export class AgentDossiersListeComponent implements OnInit {

  dossiers:         any[]   = [];
  dossiersFiltres:  any[]   = [];
  loading                   = true;
  recherche                 = '';
  filtreStatut              = '';
  dossierExpanded: number | null = null;

  statuts = [
    { value: '',             label: 'Tous les statuts' },
    { value: 'OUVERT',       label: 'Ouvert' },
    { value: 'EN_TRAITEMENT',label: 'En traitement' },
    { value: 'VALIDE',       label: 'Validé' },
    { value: 'REJETE',       label: 'Rejeté' },
    { value: 'CLOS',         label: 'Clos' }
  ];

  constructor(
    private dossierService: DossierService,
    private router: Router
  ) {}

  ngOnInit(): void { this.charger(); }

  charger(): void {
    this.loading = true;
    this.dossierService.getAllDossiers().subscribe({
      next: (data: any) => {
        this.dossiers        = Array.isArray(data) ? data : (data?.dossiers ?? []);
        this.appliquerFiltres();
        this.loading = false;
      },
      error: (err: any) => {
        console.error('❌ erreur chargement dossiers =', err);
        this.loading = false;
      }
    });
  }

  appliquerFiltres(): void {
    let result = [...this.dossiers];

    if (this.recherche.trim()) {
      const q = this.recherche.toLowerCase();
      result = result.filter(d =>
        d.numeroDossier?.toLowerCase().includes(q) ||
        d.libelle?.toLowerCase().includes(q)       ||
        this.getClientName(d).toLowerCase().includes(q)
      );
    }

    if (this.filtreStatut) {
      result = result.filter(d => d.statut === this.filtreStatut);
    }

    this.dossiersFiltres = result;
  }

  toggleExpand(d: any): void {
    this.dossierExpanded = this.dossierExpanded === d.id ? null : d.id;
  }

  voirDetails(id: number): void {
    this.router.navigate(['/agent/dossiers', id]);
  }

  // ─── Helpers ─────────────────────────────────────────────────
  getClientName(d: any): string {
    const type = d.clientTypeClient || d.clientType || d.client?.typeClient || '';
    if (type === 'ENTREPRISE') {
      return d.clientRaisonSociale || d.client?.raisonSociale || '—';
    }
    const nom    = d.clientNom    || d.client?.nom    || '';
    const prenom = d.clientPrenom || d.client?.prenom || '';
    return `${nom} ${prenom}`.trim() || '—';
  }

  getClientInitial(d: any): string {
    const name = this.getClientName(d);
    return name !== '—' ? name[0].toUpperCase() : '?';
  }

  getMontantTotal(d: any): number {
    return (d.risques ?? []).reduce(
      (sum: number, r: any) => sum + (r.montantImpaye ?? 0), 0
    );
  }

  getStatutClass(statut: string): string {
    const map: Record<string, string> = {
      'OUVERT':         'statut-ouvert',
      'EN_TRAITEMENT':  'statut-en-traitement',
      'VALIDE':         'statut-valide',
      'REJETE':         'statut-rejete',
      'CLOS':           'statut-clos'
    };
    return map[statut] || '';
  }

  getStatutLabel(statut: string): string {
    const map: Record<string, string> = {
      'OUVERT':         'Ouvert',
      'EN_TRAITEMENT':  'En traitement',
      'VALIDE':         'Validé',
      'REJETE':         'Rejeté',
      'CLOS':           'Clos'
    };
    return map[statut] || statut;
  }

  formatType(type: string): string {
    const map: Record<string, string> = {
      CREDIT_IMMOBILIER:    '🏠 Crédit Immobilier',
      CREDIT_CONSOMMATION:  '🛍️ Crédit Consommation',
      CREDIT_AUTO:          '🚗 Crédit Auto',
      CREDIT_PROFESSIONNEL: '💼 Crédit Professionnel',
      LEASING:              '📋 Leasing',
      DECOUVERT:            '🏦 Découvert Bancaire'
    };
    return map[type] ?? type;
  }

  getValidationLabel(val: boolean | null): string {
    if (val === true)  return '✅';
    if (val === false) return '❌';
    return '⏳';
  }
}