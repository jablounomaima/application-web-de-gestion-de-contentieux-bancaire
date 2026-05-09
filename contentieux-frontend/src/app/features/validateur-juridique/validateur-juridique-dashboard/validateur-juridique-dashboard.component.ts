import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ValidateurService } from '../../../core/services/validateur.service';
import { ValidateurJuridiqueListeComponent } from '../validateur-juridique-liste/validateur-juridique-liste.component';

@Component({
  selector: 'app-validateur-juridique-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, ValidateurJuridiqueListeComponent],
  templateUrl: './validateur-juridique-dashboard.component.html',
  styleUrls: ['./validateur-juridique-dashboard.component.scss']
})
export class ValidateurJuridiqueDashboardComponent implements OnInit {

  dossiers:           any[]                 = [];
  dossierDetails:     Map<number, any>      = new Map();
  loadingDetail:      Set<number>           = new Set();
  loading             = true;
  recherche           = '';
  valides             = 0;
  rejetes             = 0;
  dossierSelectionne: any                   = null;
  dossierExpanded:    number | null         = null;
  actionType:         'valider' | 'rejeter' = 'valider';
  commentaire         = '';
  erreur              = '';
  soumission          = false;

  // ── Signal de rechargement pour la liste enfant ───────────────
  recharger = false;

  constructor(private validateurService: ValidateurService) {}

  ngOnInit(): void {
    this.loading = true; // la liste enfant va émettre les données
  }

  // ══════════════════════════════════════════════════════════════
  // STATS — alimentées par l'Output du composant liste
  // ══════════════════════════════════════════════════════════════

  mettreAJourStats(dossiers: any[]): void {
    this.dossiers = dossiers;
    this.valides  = dossiers.filter(d => this.isValide(d)).length;
    this.rejetes  = dossiers.filter(d => this.isRejete(d)).length;
    this.loading  = false;
  }

  // ── Helpers statut ────────────────────────────────────────────

  isValide(d: any): boolean {
    return d.validationJuridique === true;
  }

  isRejete(d: any): boolean {
    return d.validationJuridique === false && d.statut !== 'EN_TRAITEMENT';
  }

  isEnAttente(d: any): boolean {
    return !this.isValide(d) && !this.isRejete(d);
  }

  getNbEnAttente(): number {
    return this.dossiers.filter(d => this.isEnAttente(d)).length;
  }

  // ── Calculs ───────────────────────────────────────────────────

  getMontantTotal(d: any): number {
    const detail = this.dossierDetails.get(d.id) ?? d;
    return (detail.risques ?? []).reduce(
      (sum: number, r: any) => sum + (r.montantImpaye ?? 0), 0
    );
  }

  getClientName(d: any): string {
    const detail = this.dossierDetails.get(d.id) ?? d;
    const c = detail.client;
    if (!c) return 'Client inconnu';
    if (c.typeClient === 'ENTREPRISE') return c.raisonSociale || 'Entreprise';
    return `${c.nom ?? ''} ${c.prenom ?? ''}`.trim() || 'Client inconnu';
  }

  // ══════════════════════════════════════════════════════════════
  // ACTIONS
  // ══════════════════════════════════════════════════════════════

  ouvrirAction(dossier: any, type: 'valider' | 'rejeter'): void {
    this.dossierSelectionne = dossier;
    this.actionType         = type;
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
      ? this.validateurService.validerJuridique(this.dossierSelectionne.id, this.commentaire)
      : this.validateurService.rejeterJuridique(this.dossierSelectionne.id, this.commentaire);

    obs.subscribe({
      next: () => {
        this.soumission         = false;
        this.dossierSelectionne = null;
        this.dossierExpanded    = null;
        // Déclenche le rechargement du composant liste
        // qui réémettra les nouvelles données via (dossiersCharges)
        // et mettra à jour les stats automatiquement
        this.recharger = !this.recharger;
      },
      error: (err: any) => {
        this.erreur     = err.error?.error ?? 'Erreur lors de l\'opération.';
        this.soumission = false;
      }
    });
  }
}