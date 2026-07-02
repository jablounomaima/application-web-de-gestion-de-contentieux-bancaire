import { Component, OnInit, Output, EventEmitter, Input, OnChanges, SimpleChanges } from '@angular/core';
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
export class ValidateurJuridiqueListeComponent implements OnInit, OnChanges {
  @Input()  recharger      = false;
  @Input()  dossierId:      number | null = null;
  @Output() dossiersCharges = new EventEmitter<any[]>();
  @Output() actionDemandee = new EventEmitter<{ dossier: any; type: 'valider' | 'rejeter' }>();
// ✅ Ajouter dans les composants liste
@Input() highlightedDossierId: number | null = null;
  dossiers:       any[]            = [];
  dossierDetails: Map<number, any> = new Map();
  loadingDetail:  Set<number>      = new Set();
  loading         = true;
  recherche       = '';
  dossierExpanded: number | null   = null;

  constructor(private validateurService: ValidateurService) {}

  ngOnInit(): void { this.charger(); }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['recharger'] && !changes['recharger'].firstChange) {
      this.charger();
    }
    if (changes['dossierId'] && changes['dossierId'].currentValue) {
      const targetId = Number(changes['dossierId'].currentValue);
      const existe = this.dossiers.some(d => Number(d.id) === targetId);
      if (existe) {
        // Dossiers déjà chargés et dossier cible présent → ouvrir directement
        setTimeout(() => this._ouvrirDossierCible(), 100);
      } else {
        // Dossier cible non trouvé localement (ex: nouvelle soumission reçue en temps réel)
        // On recharge la liste depuis le serveur pour le récupérer
        console.log('📌 [DEBUG] Dossier ID (juridique)', targetId, 'non trouvé en local. Rechargement...');
        this.charger();
      }
    }
  }

  charger(): void {
    this.loading = true;
    this.dossierDetails.clear();
    this.dossierExpanded = null;
  
    this.validateurService.getDossiersJuridique(this.recherche).subscribe({
      next: (data: any) => {
        this.dossiers = Array.isArray(data) ? data : (data?.dossiers ?? []);
        this.dossiersCharges.emit(this.dossiers);
        this.loading  = false;

        setTimeout(() => {
          if (this.dossierId) {
            this._ouvrirDossierCible();
          }
        }, 200);
      },
      error: (err: any) => {
        console.error('❌ erreur chargement liste juridique =', err);
        this.loading = false;
      }
    });
  }

  private _ouvrirDossierCible(): void {
    if (!this.dossierId) return;
    const cible = this.dossiers.find(d => d.id === this.dossierId);
    if (!cible) return;

    this.dossierExpanded = cible.id;
    if (!this.dossierDetails.has(cible.id)) {
      this.loadingDetail.add(cible.id);
      this.validateurService.getDossierDetailJuridique(cible.id).subscribe({
        next: (detail: any) => {
          this.dossierDetails.set(cible.id, detail);
          this.loadingDetail.delete(cible.id);
        },
        error: () => this.loadingDetail.delete(cible.id)
      });
    }

    setTimeout(() => {
      const el = document.getElementById('dossier-' + this.dossierId);
      if (!el) return;
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.classList.add('dossier-highlight');
      setTimeout(() => el.classList.remove('dossier-highlight'), 4000);
    }, 400);
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