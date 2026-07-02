import {
  Component, OnInit, Output, EventEmitter,
  Input, OnChanges, SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ValidateurService } from '../../../core/services/validateur.service';

@Component({
  selector: 'app-validateur-financier-liste',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './validateur-financier-liste.component.html',
  styleUrls: ['./validateur-financier-liste.component.scss']
})
export class ValidateurFinancierListeComponent implements OnInit, OnChanges {

  @Input()  recharger       = false;
  @Input()  dossierId:      number | null = null;
  @Output() actionDemandee  = new EventEmitter<{ dossier: any; type: 'valider' | 'rejeter' }>();
  @Output() dossiersCharges = new EventEmitter<any[]>();
// ✅ Ajouter dans les composants liste
@Input() highlightedDossierId: number | null = null;
  dossiers:        any[]            = [];
  dossierDetails:  Map<number, any> = new Map();
  loadingDetail:   Set<number>      = new Set();
  loading          = true;
  recherche        = '';
  dossierExpanded: number | null    = null;

  constructor(private validateurService: ValidateurService) {}

  ngOnInit(): void { this.charger(); }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['recharger'] && !changes['recharger'].firstChange) {
      this.charger();
    }
    // ← dossierId reçu après chargement initial ou changement de query param
    if (changes['dossierId'] && changes['dossierId'].currentValue) {
      const targetId = Number(changes['dossierId'].currentValue);
      const existe = this.dossiers.some(d => Number(d.id) === targetId);
      if (existe) {
        // Dossiers déjà chargés et dossier cible présent → ouvrir directement
        setTimeout(() => this._ouvrirDossierCible(), 100);
      } else {
        // Dossier cible non trouvé localement (ex: nouvelle soumission reçue en temps réel)
        // On recharge la liste depuis le serveur pour le récupérer
        console.log('📌 [DEBUG] Dossier ID', targetId, 'non trouvé en local. Rechargement de la liste...');
        this.charger();
      }
    }
  }
  
  charger(): void {
    this.loading = true;
    this.dossierDetails.clear();
    this.dossierExpanded = null;
  
    this.validateurService.getDossiersFinancier(this.recherche).subscribe({
      next: (data: any) => {
        this.dossiers = Array.isArray(data) ? data : (data?.dossiers ?? []);
        this.dossiersCharges.emit(this.dossiers);
        this.loading = false;
  
        // ← Attendre que dossierId soit disponible
        setTimeout(() => {
          if (this.dossierId) {
            this._ouvrirDossierCible();
          }
        }, 200);
      },
      error: (err: any) => {
        console.error('❌ erreur chargement liste financier =', err);
        this.loading = false;
      }
    });
  }

  private _ouvrirDossierCible(): void {
    console.log('📌 [DEBUG] _ouvrirDossierCible appelé avec dossierId =', this.dossierId);
    console.log('📌 [DEBUG] dossiers chargés en mémoire =', this.dossiers.map(d => ({ id: d.id, ref: d.numeroDossier })));
    if (!this.dossierId) return;
    const cible = this.dossiers.find(d => Number(d.id) === Number(this.dossierId));
    console.log('📌 [DEBUG] dossier cible trouvé =', cible);
    if (!cible) {
      console.warn('⚠️ [DEBUG] dossier cible non trouvé dans la liste !');
      return;
    }

    // Ouvrir l'accordéon
    this.dossierExpanded = cible.id;
    if (!this.dossierDetails.has(cible.id)) {
      this.loadingDetail.add(cible.id);
      this.validateurService.getDossierDetailFinancier(cible.id).subscribe({
        next: (detail: any) => {
          this.dossierDetails.set(cible.id, detail);
          this.loadingDetail.delete(cible.id);
          console.log('📌 [DEBUG] détails dossier chargés avec succès pour ID =', cible.id);
        },
        error: (err: any) => {
          this.loadingDetail.delete(cible.id);
          console.error('❌ [DEBUG] erreur chargement détails pour ID =', cible.id, err);
        }
      });
    }

    // Scroller et surligner
    setTimeout(() => {
      const el = document.getElementById('dossier-' + this.dossierId);
      console.log('📌 [DEBUG] recherche de l\'élément DOM id dossier-' + this.dossierId + ' =', el);
      if (!el) {
        console.warn('⚠️ [DEBUG] élément DOM dossier-' + this.dossierId + ' non trouvé !');
        return;
      }
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

  isValide(d: any): boolean    { return d.validationFinanciere === true; }
  isRejete(d: any): boolean    { return d.validationFinanciere === false && d.statut !== 'EN_TRAITEMENT'; }
  isEnAttente(d: any): boolean { return !this.isValide(d) && !this.isRejete(d); }
  getDetail(d: any): any       { return this.dossierDetails.get(d.id) ?? d; }
  isLoadingDetail(d: any): boolean { return this.loadingDetail.has(d.id); }

  getMontantTotal(d: any): number {
    return (this.getDetail(d).risques ?? [])
      .reduce((sum: number, r: any) => sum + (r.montantImpaye ?? 0), 0);
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

  demanderAction(dossier: any, type: 'valider' | 'rejeter'): void {
    this.actionDemandee.emit({ dossier, type });
  }
}