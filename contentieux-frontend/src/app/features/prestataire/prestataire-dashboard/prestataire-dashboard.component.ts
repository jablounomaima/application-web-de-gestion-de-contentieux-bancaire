import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PrestataireService } from '../../../core/services/prestataire.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-prestataire-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <div>
          <h1>Mon Espace Prestataire</h1>
          <p class="subtitle">Gérez vos missions et soumettez vos rapports</p>
        </div>
      </div>

      <!-- Stats -->
      <div class="stats-row" *ngIf="stats">
        <div class="stat-card blue">
          <div class="stat-icon">📋</div>
          <div><h4>Total</h4><span>{{ stats.totalMissions }}</span></div>
        </div>
        <div class="stat-card orange">
          <div class="stat-icon">⚡</div>
          <div><h4>En Cours</h4><span>{{ stats.missionsEnCours }}</span></div>
        </div>
        <div class="stat-card purple">
          <div class="stat-icon">📄</div>
          <div><h4>PV Soumis</h4><span>{{ stats.pvSoumis }}</span></div>
        </div>
        <div class="stat-card green">
          <div class="stat-icon">✅</div>
          <div><h4>Terminées</h4><span>{{ stats.missionsTerminees }}</span></div>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tabs">
        <button [class.active]="activeTab === 'enCours'" (click)="activeTab = 'enCours'">⚡ Missions en Cours</button>
        <button [class.active]="activeTab === 'toutes'" (click)="activeTab = 'toutes'">📋 Toutes les Missions</button>
      </div>

      <!-- Loading -->
      <div class="loading-skeleton" *ngIf="loading">
        <div class="skeleton-row" *ngFor="let i of [1,2,3]"></div>
      </div>

      <!-- Liste des missions -->
      <div class="table-card" *ngIf="!loading">
        <table class="data-table">
          <thead>
            <tr>
              <th>Mission</th>
              <th>Dossier</th>
              <th>Type</th>
              <th>Date assignation</th>
              <th>Statut</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngIf="missionsFiltrees.length === 0">
              <td colspan="6" class="empty-state">Aucune mission {{ activeTab === 'enCours' ? 'en cours' : '' }}</td>
            </tr>
            <tr *ngFor="let m of missionsFiltrees" class="table-row">
              <td><strong>#{{ m.id }}</strong></td>
              <td>
  <div *ngIf="m.prestation?.dossier as d; else noDossier">
    <strong>{{ d.numeroDossier }}</strong><br>

    <small>👤 {{ d.client?.nom }} {{ d.client?.prenom }}</small><br>

    <small>📄 {{ d.libelle || '—' }}</small><br>

    <small>💰 {{ d.montant || '—' }} TND</small><br>

    <small>📅 {{ d.dateCreation | date:'dd/MM/yyyy' }}</small>
  </div>

  <ng-template #noDossier>
    <span>N/A</span>
  </ng-template>
</td>
              <td><span class="type-badge">{{ m.prestation?.type || 'N/A' }}</span></td>
              <td>{{ m.dateAssignation | date:'dd/MM/yyyy' }}</td>
              <td><span class="statut-badge" [ngClass]="getStatutClass(m.statut)">{{ m.statut }}</span></td>
              <td class="actions">
              <button class="btn-action info"
          (click)="voirDossier(m)">
    👁️ Voir dossier
  </button>
                <button class="btn-action pv" (click)="ouvrirModalPV(m)" *ngIf="peutSoumettrePV(m)">📄 Soumettre PV</button>
                <button class="btn-action facture" (click)="ouvrirModalFacture(m)" *ngIf="peutSoumettreFacture(m)">💳 Facture</button>
                <span class="statut-final" *ngIf="!peutSoumettrePV(m) && !peutSoumettreFacture(m)">—</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- MODAL PV -->
    <div class="modal-overlay" *ngIf="missionSelectionnee && modalType === 'pv'" (click)="fermerModal()">
      <div class="modal-card" (click)="$event.stopPropagation()">
        <div class="modal-header indigo">
          <h2>📄 Soumettre un PV de Mission</h2>
          <button class="modal-close" (click)="fermerModal()">✕</button>
        </div>
        <div class="modal-body">
          <div class="mission-info-box">
            <p><strong>Mission :</strong> #{{ missionSelectionnee.id }}</p>
            <p><strong>Dossier :</strong> {{ missionSelectionnee.prestation?.dossier?.numeroDossier }}</p>
          </div>
          <div class="form-group">
            <label>Contenu du Procès-Verbal *</label>
            <textarea [(ngModel)]="formPV.pvTexte" rows="6" placeholder="Rédigez votre compte rendu de mission ici..."></textarea>
          </div>
          <div class="error-banner" *ngIf="erreur">{{ erreur }}</div>
          <div class="success-banner" *ngIf="succes">{{ succes }}</div>
          <div class="modal-footer">
            <button class="btn-cancel" (click)="fermerModal()">Annuler</button>
            <button class="btn-submit" (click)="soumettreResultat()" [disabled]="soumission">
              {{ soumission ? '⏳ En cours...' : '✅ Soumettre le PV' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- MODAL FACTURE -->
    <div class="modal-overlay" *ngIf="missionSelectionnee && modalType === 'facture'" (click)="fermerModal()">
      <div class="modal-card" (click)="$event.stopPropagation()">
        <div class="modal-header green">
          <h2>💳 Soumettre une Facture</h2>
          <button class="modal-close" (click)="fermerModal()">✕</button>
        </div>
        <div class="modal-body">
          <div class="mission-info-box">
            <p><strong>Mission :</strong> #{{ missionSelectionnee.id }}</p>
          </div>
          <div class="form-group">
            <label>Référence Facture *</label>
            <input type="text" [(ngModel)]="formFacture.factureRef" placeholder="Ex: FAC-2024-001">
          </div>
          <div class="form-group">
            <label>Montant (TND) *</label>
            <input type="number" [(ngModel)]="formFacture.montant" placeholder="0.00" min="0" step="0.01">
          </div>
          <div class="error-banner" *ngIf="erreur">{{ erreur }}</div>
          <div class="success-banner" *ngIf="succes">{{ succes }}</div>
          <div class="modal-footer">
            <button class="btn-cancel" (click)="fermerModal()">Annuler</button>
            <button class="btn-submit green-btn" (click)="soumettreFacture()" [disabled]="soumission">
              {{ soumission ? '⏳ En cours...' : '💳 Soumettre la Facture' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-container { font-family: 'Inter', sans-serif; }
    .page-header { margin-bottom: 25px; }
    h1 { margin: 0; font-size: 1.8rem; color: #1a237e; }
    .subtitle { margin: 5px 0 0; color: #777; }

    .stats-row { display: grid; grid-template-columns: repeat(4,1fr); gap: 20px; margin-bottom: 25px; }
    .stat-card { background: white; border-radius: 12px; padding: 20px; display: flex; align-items: center; gap: 16px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); border-left: 5px solid; transition: transform .2s; }
    .stat-card:hover { transform: translateY(-4px); }
    .stat-card.blue { border-color: #1976d2; } .stat-card.orange { border-color: #f57c00; }
    .stat-card.purple { border-color: #7b1fa2; } .stat-card.green { border-color: #388e3c; }
    .stat-icon { font-size: 2.2rem; }
    .stat-card h4 { margin: 0 0 4px; color: #757575; font-size: 0.85rem; text-transform: uppercase; }
    .stat-card span { font-size: 1.8rem; font-weight: bold; color: #212121; }

    .tabs { display: flex; gap: 10px; margin-bottom: 20px; }
    .tabs button { padding: 10px 22px; border: 2px solid #ddd; border-radius: 8px; cursor: pointer; background: white; color: #555; font-weight: 600; transition: all .2s; }
    .tabs button.active { border-color: #1a237e; background: #1a237e; color: white; }

    .loading-skeleton { display: flex; flex-direction: column; gap: 12px; }
    .skeleton-row { height: 55px; border-radius: 8px; background: linear-gradient(90deg,#f0f0f0 25%,#e0e0e0 50%,#f0f0f0 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; }
    @keyframes shimmer { 0%{background-position:200% 0} 100%{background-position:-200% 0} }

    .table-card { background: white; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); overflow: hidden; }
    .data-table { width: 100%; border-collapse: collapse; }
    .data-table thead { background: #1a237e; }
    .data-table th { padding: 15px; text-align: left; color: white; font-weight: 600; font-size: .9rem; }
    .data-table td { padding: 14px 15px; border-bottom: 1px solid #f0f0f0; font-size: .9rem; }
    .table-row:hover { background: #f8f9ff; }
    .empty-state { text-align: center; padding: 40px !important; color: #aaa; font-style: italic; }
    .type-badge { background: #e8eaf6; color: #3949ab; padding: 4px 10px; border-radius: 12px; font-size: .8rem; font-weight: 600; }
    .statut-badge { padding: 5px 12px; border-radius: 15px; font-size: .8rem; font-weight: bold; }
    .s-assignee { background: #fff3cd; color: #856404; }
    .s-en-cours { background: #cce5ff; color: #004085; }
    .s-pv-soumis { background: #d1ecf1; color: #0c5460; }
    .s-facture { background: #d4edda; color: #155724; }
    .s-terminee { background: #e2e3e5; color: #383d41; }
    .actions { display: flex; gap: 8px; align-items: center; }
    .btn-action { padding: 7px 14px; border: none; border-radius: 6px; cursor: pointer; font-size: .85rem; font-weight: 600; transition: all .2s; }
    .btn-action.pv { background: #e8eaf6; color: #283593; }
    .btn-action.pv:hover { background: #c5cae9; }
    .btn-action.facture { background: #e8f5e9; color: #1b5e20; }
    .btn-action.facture:hover { background: #c8e6c9; }
    .statut-final { color: #ccc; }

    .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); backdrop-filter: blur(4px); z-index: 1000; display: flex; align-items: center; justify-content: center; }
    .modal-card { background: white; border-radius: 16px; width: 560px; max-width: 95vw; box-shadow: 0 25px 50px rgba(0,0,0,0.25); animation: slideUp .3s ease; }
    @keyframes slideUp { from{opacity:0;transform:translateY(30px)} to{opacity:1;transform:translateY(0)} }
    .modal-header { padding: 22px 28px; display: flex; justify-content: space-between; align-items: center; border-radius: 16px 16px 0 0; }
    .modal-header.indigo { background: #1a237e; } .modal-header.green { background: #2e7d32; }
    .modal-header h2 { margin: 0; color: white; font-size: 1.2rem; }
    .modal-close { background: rgba(255,255,255,.2); border: none; color: white; width: 30px; height: 30px; border-radius: 50%; cursor: pointer; }
    .modal-body { padding: 28px; }
    .mission-info-box { background: #f8f9fa; border-radius: 8px; padding: 14px; margin-bottom: 20px; border-left: 4px solid #1a237e; }
    .mission-info-box p { margin: 4px 0; font-size: .9rem; }
    .form-group { margin-bottom: 18px; }
    .form-group label { display: block; margin-bottom: 8px; font-weight: 600; color: #333; font-size: .9rem; }
    .form-group input, .form-group textarea { width: 100%; box-sizing: border-box; padding: 11px 14px; border: 1px solid #ddd; border-radius: 8px; font-size: .95rem; font-family: 'Inter',sans-serif; outline: none; }
    .form-group input:focus, .form-group textarea:focus { border-color: #1a237e; box-shadow: 0 0 0 3px rgba(26,35,126,.1); }
    .error-banner { background: #fdecea; color: #c62828; padding: 12px 16px; border-radius: 8px; margin-bottom: 15px; border-left: 4px solid #c62828; font-size: .9rem; }
    .success-banner { background: #e8f5e9; color: #2e7d32; padding: 12px 16px; border-radius: 8px; margin-bottom: 15px; border-left: 4px solid #2e7d32; font-size: .9rem; }
    .modal-footer { display: flex; justify-content: flex-end; gap: 12px; padding-top: 15px; border-top: 1px solid #f0f0f0; }
    .btn-cancel { background: #f5f5f5; color: #555; border: none; padding: 11px 22px; border-radius: 8px; cursor: pointer; font-weight: 600; }
    .btn-submit { background: #1a237e; color: white; border: none; padding: 11px 26px; border-radius: 8px; cursor: pointer; font-weight: 700; }
    .btn-submit.green-btn { background: #2e7d32; }
    .btn-submit[disabled] { opacity: .6; cursor: not-allowed; }
  `]
})
export class PrestataireDashboardComponent implements OnInit {
  stats: any = null;
  missions: any[] = [];
  loading = true;
  activeTab: 'enCours' | 'toutes' = 'enCours';

  missionSelectionnee: any = null;
  modalType: 'pv' | 'facture' | null = null;
  formPV = { pvTexte: '' };
  formFacture = { factureRef: '', montant: 0 };
  erreur = '';
  succes = '';
  soumission = false;

  constructor(private prestataireService: PrestataireService, private router: Router) {}

  ngOnInit() {
    this.prestataireService.getDashboard().subscribe({
      next: (data) => {
        this.stats = data;
        this.missions = data.dernieresMissions || [];
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  get missionsFiltrees(): any[] {
    if (this.activeTab === 'enCours') {
      return this.missions.filter(m => ['ASSIGNEE', 'EN_COURS', 'PV_SOUMIS'].includes(m.statut));
    }
    return this.missions;
  }

  getStatutClass(statut: string): string {
    const map: Record<string, string> = {
      'ASSIGNEE': 's-assignee', 'EN_COURS': 's-en-cours',
      'PV_SOUMIS': 's-pv-soumis', 'FACTURE_SOUMISE': 's-facture', 'TERMINEE': 's-terminee'
    };
    return map[statut] || '';
  }

  peutSoumettrePV(m: any): boolean {
    return ['ASSIGNEE', 'EN_COURS'].includes(m.statut);
  }

  peutSoumettreFacture(m: any): boolean {
    return m.statut === 'PV_SOUMIS';
  }

  ouvrirModalPV(mission: any) {
    this.missionSelectionnee = mission;
    this.modalType = 'pv';
    this.formPV = { pvTexte: '' };
    this.erreur = ''; this.succes = '';
  }

  ouvrirModalFacture(mission: any) {
    this.missionSelectionnee = mission;
    this.modalType = 'facture';
    this.formFacture = { factureRef: '', montant: 0 };
    this.erreur = ''; this.succes = '';
  }

  fermerModal() {
    if (!this.soumission) {
      this.missionSelectionnee = null;
      this.modalType = null;
    }
  }

  soumettreResultat() {
    this.erreur = ''; this.soumission = true;
    const fd = new FormData();
    fd.append('pvTexte', this.formPV.pvTexte);

    this.prestataireService.soumettreResultat(this.missionSelectionnee.id, fd).subscribe({
      next: () => {
        this.succes = '✅ PV soumis avec succès !';
        this.soumission = false;
        setTimeout(() => { this.fermerModal(); this.ngOnInit(); }, 1500);
      },
      error: (err) => { this.erreur = err.error?.error || 'Erreur lors de la soumission.'; this.soumission = false; }
    });
  }

  soumettreFacture() {
    this.erreur = ''; this.soumission = true;
    this.prestataireService.soumettreFacture(this.missionSelectionnee.id, this.formFacture).subscribe({
      next: () => {
        this.succes = '✅ Facture soumise !';
        this.soumission = false;
        setTimeout(() => { this.fermerModal(); this.ngOnInit(); }, 1500);
      },
      error: (err) => { this.erreur = err.error?.error || 'Erreur lors de la soumission.'; this.soumission = false; }
    });
  }

  voirDossier(m: any) {
    this.router.navigate(['/prestataire/mission', m.id, 'dossier']);
  }
}


