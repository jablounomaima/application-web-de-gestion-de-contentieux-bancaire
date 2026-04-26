import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ValidateurService } from '../../../core/services/validateur.service';

@Component({
  selector: 'app-validateur-juridique-liste',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './validateur-juridique-liste.component.html',
  styleUrls: ['./validateur-juridique-liste.component.scss']
})
export class ValidateurJuridiqueListeComponent implements OnInit {

  @Output() actionDemandee = new EventEmitter<{ dossier: any; type: 'valider' | 'rejeter' }>();

  dossiers:       any[]            = [];
  dossierDetails: Map<number, any> = new Map();
  loadingDetail:  Set<number>      = new Set();
  loading         = true;
  recherche       = '';
  dossierExpanded: number | null   = null;

  constructor(private validateurService: ValidateurService) {}

  ngOnInit(): void { this.charger(); }

  charger(): void {
    this.loading = true;
    this.dossierDetails.clear();
    this.dossierExpanded = null;

    this.validateurService.getDossiersJuridique(this.recherche).subscribe({
      next: (data: any) => {
        this.dossiers = Array.isArray(data) ? data : (data?.dossiers ?? []);
        this.loading  = false;
      },
      error: (err: any) => {
        console.error('❌ erreur chargement liste juridique =', err);
        this.loading = false;
      }
    });
  }

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
  isValide(d: any): boolean {
    return d.validationJuridique === true;
  }

  isRejete(d: any): boolean {
    return d.validationJuridique === false && d.statut !== 'EN_TRAITEMENT';
  }

  isEnAttente(d: any): boolean {
    return !this.isValide(d) && !this.isRejete(d);
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
    return (this.getDetail(d).risques ?? []).reduce(
      (sum: number, r: any) => sum + (r.montantImpaye ?? 0), 0
    );
  }

  getClientName(d: any): string {
    const c = this.getDetail(d).client;
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

  // ─── Émission vers le parent ─────────────────────────────────
  demanderAction(dossier: any, type: 'valider' | 'rejeter'): void {
    this.actionDemandee.emit({ dossier, type });
  }
}