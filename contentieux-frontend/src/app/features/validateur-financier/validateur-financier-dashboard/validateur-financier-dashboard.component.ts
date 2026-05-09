import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ValidateurService } from '../../../core/services/validateur.service';
import { ValidateurFinancierListeComponent } from '../validateur-financier-liste/validateur-financier-liste.component';

@Component({
  selector: 'app-validateur-financier-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, ValidateurFinancierListeComponent],
  templateUrl: './validateur-financier-dashboard.component.html',
  styleUrls: ['./validateur-financier-dashboard.component.scss']
})
export class ValidateurFinancierDashboardComponent implements OnInit {

  dossiers:           any[]                 = [];
  loading             = true;
  valides             = 0;
  rejetes             = 0;
  dossierSelectionne: any                   = null;
  actionType:         'valider' | 'rejeter' = 'valider';
  commentaire         = '';
  erreur              = '';
  soumission          = false;
  recharger           = false;

  constructor(private validateurService: ValidateurService) {}

  ngOnInit(): void {
    this.loading = true; // la liste enfant va émettre les données
  }

  // ── Stats alimentées par l'Output de la liste ─────────────────
  mettreAJourStats(dossiers: any[]): void {
    this.dossiers = dossiers;
    this.valides  = dossiers.filter(d => d.validationFinanciere === true).length;
    this.rejetes  = dossiers.filter(d => d.validationFinanciere === false && d.statut !== 'EN_TRAITEMENT').length;
    this.loading  = false;
  }

  getNbEnAttente(): number {
    return this.dossiers.filter(d =>
      d.validationFinanciere !== true && d.validationFinanciere !== false
    ).length;
  }

  ouvrirAction(event: { dossier: any; type: 'valider' | 'rejeter' }): void {
    this.dossierSelectionne = event.dossier;
    this.actionType         = event.type;
    this.commentaire        = '';
    this.erreur             = '';
  }

  fermerModal(): void {
    if (!this.soumission) this.dossierSelectionne = null;
  }

  confirmerAction(): void {
    if (this.actionType === 'rejeter' && !this.commentaire.trim()) {
      this.erreur = 'Le motif de rejet est obligatoire.';
      return;
    }

    this.soumission = true;
    this.erreur     = '';

    const obs = this.actionType === 'valider'
      ? this.validateurService.validerFinancier(this.dossierSelectionne.id, this.commentaire)
      : this.validateurService.rejeterFinancier(this.dossierSelectionne.id, this.commentaire);

    obs.subscribe({
      next: () => {
        this.soumission         = false;
        this.dossierSelectionne = null;
        this.recharger          = !this.recharger; // déclenche rechargement liste
      },
      error: (err: any) => {
        this.erreur     = err.error?.error ?? 'Erreur lors de l\'opération.';
        this.soumission = false;
      }
    });
  }
}