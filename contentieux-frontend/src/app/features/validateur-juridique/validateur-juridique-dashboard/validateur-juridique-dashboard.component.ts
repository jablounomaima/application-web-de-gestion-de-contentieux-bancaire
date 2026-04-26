import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ValidateurService } from '../../../core/services/validateur.service';
import { ValidateurJuridiqueListeComponent } from '../validateur-juridique-liste/validateur-juridique-liste.component';

@Component({
  selector: 'app-validateur-juridique-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule,ValidateurJuridiqueListeComponent],
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

  constructor(private validateurService: ValidateurService) {}

  ngOnInit(): void { this.charger(); }

  // ─── Liste ───────────────────────────────────────────────────
  charger(): void {
    this.loading = true;
    this.dossierDetails.clear();
    this.dossierExpanded = null;

    this.validateurService.getDossiersJuridique(this.recherche).subscribe({
      next: (data: any) => {
        const raw     = Array.isArray(data) ? data : (data?.dossiers ?? []);
        this.dossiers = raw;
        // Recalcul des compteurs à partir des données reçues
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
      this.validateurService.getDossierDetailJuridique(d.id).subscribe({
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

  /** Dossier validé juridiquement */
  isValide(d: any): boolean {
    return d.validationJuridique === true;
  }

  /** Dossier rejeté juridiquement */
  isRejete(d: any): boolean {
    return d.validationJuridique === false && d.statut !== 'EN_TRAITEMENT';
  }

  /** Dossier encore en attente de décision */
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
      ? this.validateurService.validerJuridique(this.dossierSelectionne.id, this.commentaire)
      : this.validateurService.rejeterJuridique(this.dossierSelectionne.id, this.commentaire);

    obs.subscribe({
      next: () => {
        this.soumission         = false;
        this.dossierSelectionne = null;
        this.dossierExpanded    = null;
        this.charger(); // recharge — les compteurs se recalculent dans charger()
      },
      error: (err: any) => {
        this.erreur     = err.error?.error ?? 'Erreur lors de l\'opération.';
        this.soumission = false;
      }
    });
  }
}