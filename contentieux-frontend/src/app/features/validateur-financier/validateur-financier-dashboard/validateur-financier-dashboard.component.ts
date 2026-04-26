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
  recherche           = '';
  valides             = 0;
  rejetes             = 0;
  dossierSelectionne: any                   = null;
  actionType:         'valider' | 'rejeter' = 'valider';
  commentaire         = '';
  erreur              = '';
  soumission          = false;

  constructor(private validateurService: ValidateurService) {}

  ngOnInit(): void { this.charger(); }

  charger(): void {
    this.loading = true;
    this.validateurService.getDossiersFinancier(this.recherche).subscribe({
      next: (data: any) => {
        const raw     = Array.isArray(data) ? data : (data?.dossiers ?? []);
        this.dossiers = raw;
        this.valides  = raw.filter((d: any) => d.validationFinanciere === true).length;
        this.rejetes  = raw.filter((d: any) => d.validationFinanciere === false).length;
        this.loading  = false;
      },
      error: (err: any) => {
        console.error('❌ erreur chargement liste financier =', err);
        this.loading = false;
      }
    });
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
        this.charger();
      },
      error: (err: any) => {
        this.erreur     = err.error?.error ?? 'Erreur lors de l\'opération.';
        this.soumission = false;
      }
    });
  }
}