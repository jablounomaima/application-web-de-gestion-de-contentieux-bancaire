import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ValidateurService } from '../../../core/services/validateur.service';

@Component({
  selector: 'app-validateur-financier-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './validateur-financier-dashboard.component.html',
  styleUrls: ['./validateur-financier-dashboard.component.scss']
})
export class ValidateurFinancierDashboardComponent implements OnInit {

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

  constructor(private validateurService: ValidateurService) {}

  ngOnInit(): void { this.charger(); }

  // ─── Liste ───────────────────────────────────────────────────
  charger(): void {
    this.loading = true;
    this.dossierDetails.clear();
    this.dossierExpanded = null;

    this.validateurService.getDossiersFinancier(this.recherche).subscribe({
      next: (data: any) => {
        const raw     = Array.isArray(data) ? data : (data?.dossiers ?? []);
        this.dossiers = raw;
        this.valides  = raw.filter((d: any) => this.isValide(d)).length;
        this.rejetes  = raw.filter((d: any) => this.isRejete(d)).length;
        this.loading  = false;
      },
      error: (err: any) => {
        console.error('❌ erreur chargement liste =', err);
        this.loading = false;
      }
    });
  }

  // ─── Expansion avec chargement du détail ─────────────────────
  toggleExpand(d: any): void {
    if (this.dossierExpanded === d.id) {
      this.dossierExpanded = null;
      return;
    }
    this.dossierExpanded = d.id;

    if (!this.dossierDetails.has(d.id)) {
      this.loadingDetail.add(d.id);
      this.validateurService.getDossierDetailFinancier(d.id).subscribe({
        next: (detail: any) => {
          this.dossierDetails.set(d.id, detail);
          this.loadingDetail.delete(d.id);
        },
        error: (err: any) => {
          console.error('❌ erreur détail', d.id, err);
          this.loadingDetail.delete(d.id);
        }
      });
    }
  }

  // ─── Helpers statut ──────────────────────────────────────────

  isValide(d: any): boolean {
    return d.validationFinanciere === true;
  }

  isRejete(d: any): boolean {
    return d.validationFinanciere === false && d.statut !== 'EN_TRAITEMENT';
  }

  isEnAttente(d: any): boolean {
    return !this.isValide(d) && !this.isRejete(d);
  }

  getNbEnAttente(): number {
    return this.dossiers.filter(d => this.isEnAttente(d)).length;
  }

  // ─── Accesseurs détail ───────────────────────────────────────
  getDetail(d: any): any {
    return this.dossierDetails.get(d.id) ?? d;
  }

  isLoadingDetail(d: any): boolean {
    return this.loadingDetail.has(d.id);
  }

  // ─── Calculs ─────────────────────────────────────────────────
  getMontantTotal(d: any): number {
    const detail = this.getDetail(d);
    return (detail.risques ?? []).reduce(
      (sum: number, r: any) => sum + (r.montantImpaye ?? 0), 0
    );
  }

  getClientName(d: any): string {
    const detail = this.getDetail(d);
    const c = detail.client;
    if (!c) return 'Client inconnu';
    if (c.typeClient === 'ENTREPRISE') return c.raisonSociale || 'Entreprise';
    return `${c.nom ?? ''} ${c.prenom ?? ''}`.trim() || 'Client inconnu';
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

  // ─── Actions ─────────────────────────────────────────────────
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
      ? this.validateurService.validerFinancier(this.dossierSelectionne.id, this.commentaire)
      : this.validateurService.rejeterFinancier(this.dossierSelectionne.id, this.commentaire);

    obs.subscribe({
      next: () => {
        this.soumission         = false;
        this.dossierSelectionne = null;
        this.dossierExpanded    = null;
        this.charger();
      },
      error: (err: any) => {
        this.erreur     = err.error?.error ?? 'Erreur lors de l\'opération.';
        this.soumission = false;
      }
    });
  }
}