import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ValidateurService } from '../../../core/services/validateur.service';

@Component({
  selector: 'app-validateur-juridique-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './validateur-juridique-dashboard.component.html',
  styleUrls:  ['./validateur-juridique-dashboard.component.scss']

})
export class ValidateurJuridiqueDashboardComponent implements OnInit {

  dossiers: any[] = [];
  loading = true;
  recherche = '';
  valides = 0;
  rejetes = 0;
  dossierSelectionne: any = null;
  dossierExpanded: number | null = null;
  actionType: 'valider' | 'rejeter' = 'valider';
  commentaire = '';
  erreur = '';
  soumission = false;

  constructor(private validateurService: ValidateurService) {}

  ngOnInit(): void { this.charger(); }

  charger(): void {
    this.loading = true;
    this.validateurService.getDossiersJuridique(this.recherche).subscribe({
      next: (data: any) => {
        this.dossiers = data.dossiers || data || [];
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  toggleExpand(d: any): void {
    this.dossierExpanded = this.dossierExpanded === d.id ? null : d.id;
  }

  getMontantTotal(d: any): number {
    return (d.risques || []).reduce(
      (sum: number, r: any) => sum + (r.montantImpaye || 0), 0
    );
  }

  formatType(type: string): string {
    const map: Record<string, string> = {
      'CREDIT_IMMOBILIER':    '🏠 Crédit Immobilier',
      'CREDIT_CONSOMMATION':  '🛍️ Crédit Consommation',
      'CREDIT_AUTO':          '🚗 Crédit Auto',
      'CREDIT_PROFESSIONNEL': '💼 Crédit Professionnel',
      'LEASING':              '📋 Leasing',
      'DECOUVERT':            '🏦 Découvert Bancaire'
    };
    return map[type] || type;
  }

  ouvrirAction(dossier: any, type: 'valider' | 'rejeter'): void {
    this.dossierSelectionne = dossier;
    this.actionType = type;
    this.commentaire = '';
    this.erreur = '';
  }

  fermerModal(): void {
    if (!this.soumission) this.dossierSelectionne = null;
  }

  confirmerAction(): void {
    this.soumission = true;
    this.erreur = '';
    const obs = this.actionType === 'valider'
      ? this.validateurService.validerJuridique(this.dossierSelectionne.id, this.commentaire)
      : this.validateurService.rejeterJuridique(this.dossierSelectionne.id, this.commentaire);

    obs.subscribe({
      next: () => {
        this.soumission = false;
        this.dossierSelectionne = null;
        this.dossierExpanded = null;
        if (this.actionType === 'valider') this.valides++;
        else this.rejetes++;
        this.charger();
      },
      error: (err: any) => {
        this.erreur = err.error?.error || 'Erreur lors de l\'opération.';
        this.soumission = false;
      }
    });
  }
}