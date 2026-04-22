import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ValidateurService } from '../../../core/services/validateur.service';

@Component({
  selector: 'app-validateur-financier-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <div>
          <h1>💰 Validation Financière</h1>
          <p class="subtitle">Gérez les factures et validez les paiements des prestataires</p>
        </div>
      </div>

      <div class="stats-row">
        <div class="stat-card pending"><div class="stat-icon">🕒</div><div><h4>En Attente</h4><span class="stat-value">{{ dossiers.length }}</span></div></div>
        <div class="stat-card success"><div class="stat-icon">✅</div><div><h4>Validés</h4><span class="stat-value">{{ valides }}</span></div></div>
        <div class="stat-card danger"><div class="stat-icon">❌</div><div><h4>Rejetés</h4><span class="stat-value">{{ rejetes }}</span></div></div>
      </div>

      <div class="filters-bar">
        <input type="text" [(ngModel)]="recherche" (ngModelChange)="charger()" placeholder="🔍 Rechercher par N° dossier ou client..." class="search-input">
      </div>

      <div class="table-card">
        <div class="table-header-bar"><h2>Dossiers en attente de validation financière</h2></div>
        <table class="data-table">
          <thead><tr>
            <th>N° Dossier</th><th>Client</th><th>Type</th><th>Montant</th><th>Date</th><th>Actions</th>
          </tr></thead>
          <tbody>
            <tr *ngIf="loading"><td colspan="6" class="empty-state">Chargement...</td></tr>
            <tr *ngIf="!loading && dossiers.length === 0"><td colspan="6" class="empty-state">Aucun dossier en attente.</td></tr>
            <tr *ngFor="let d of dossiers" class="table-row">
              <td><strong class="num">{{ d.numeroDossier }}</strong></td>
              <td>{{ d.client?.nom || d.client?.raisonSociale || 'N/A' }}</td>
              <td><span class="type-badge">{{ d.typeDossier }}</span></td>
              <td class="money">{{ d.montantCreance | number:'1.2-2' }} TND</td>
              <td>{{ d.dateCreation | date:'dd/MM/yyyy' }}</td>
              <td class="actions">
                <button class="btn-valider" (click)="ouvrirAction(d, 'valider')">✅ Valider</button>
                <button class="btn-rejeter" (click)="ouvrirAction(d, 'rejeter')">❌ Rejeter</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- MODAL -->
    <div class="modal-overlay" *ngIf="dossierSelectionne" (click)="fermerModal()">
      <div class="modal-card" (click)="$event.stopPropagation()">
        <div class="modal-header" [class.header-valider]="actionType === 'valider'" [class.header-rejeter]="actionType === 'rejeter'">
          <h2>{{ actionType === 'valider' ? '✅ Valider' : '❌ Rejeter' }} le Paiement</h2>
          <button class="modal-close" (click)="fermerModal()">✕</button>
        </div>
        <div class="modal-body">
          <p class="dossier-ref">Dossier : <strong>{{ dossierSelectionne.numeroDossier }}</strong></p>
          <p class="montant-ref">Montant : <strong class="money">{{ dossierSelectionne.montantCreance | number:'1.2-2' }} TND</strong></p>
          <div class="form-group">
            <label>{{ actionType === 'valider' ? 'Commentaire (optionnel)' : 'Motif de rejet *' }}</label>
            <textarea [(ngModel)]="commentaire" rows="4" [placeholder]="actionType === 'valider' ? 'Ex: Montant conforme au contrat...' : 'Expliquez le motif du rejet...'"></textarea>
          </div>
          <div class="error-banner" *ngIf="erreur">{{ erreur }}</div>
          <div class="modal-footer">
            <button class="btn-cancel" (click)="fermerModal()">Annuler</button>
            <button [class]="actionType === 'valider' ? 'btn-confirm-valider' : 'btn-confirm-rejeter'" (click)="confirmerAction()" [disabled]="soumission">
              {{ soumission ? '⏳...' : (actionType === 'valider' ? '✅ Confirmer le paiement' : '❌ Rejeter') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-container { font-family: 'Inter', sans-serif; }
    .page-header { margin-bottom: 25px; }
    h1 { margin: 0; font-size: 1.8rem; color: #1b5e20; }
    .subtitle { margin: 5px 0 0; color: #777; }
    .stats-row { display: flex; gap: 20px; margin-bottom: 25px; }
    .stat-card { flex: 1; background: white; border-radius: 12px; padding: 20px; display: flex; align-items: center; gap: 16px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); border-left: 5px solid; transition: transform .2s; }
    .stat-card:hover { transform: translateY(-4px); }
    .stat-card.pending { border-color: #ffb300; } .stat-card.success { border-color: #43a047; } .stat-card.danger { border-color: #e53935; }
    .stat-icon { font-size: 2rem; } .stat-card h4 { margin: 0 0 4px; color: #757575; font-size: .85rem; text-transform: uppercase; }
    .stat-value { font-size: 1.8rem; font-weight: bold; color: #212121; }
    .filters-bar { margin-bottom: 20px; }
    .search-input { width: 100%; box-sizing: border-box; padding: 12px 16px; border: 1px solid #ddd; border-radius: 8px; font-size: .95rem; outline: none; }
    .search-input:focus { border-color: #1b5e20; box-shadow: 0 0 0 3px rgba(27,94,32,.1); }
    .table-card { background: white; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); overflow: hidden; }
    .table-header-bar { padding: 20px 25px; border-bottom: 1px solid #f0f0f0; }
    h2 { margin: 0; color: #333; font-size: 1.2rem; }
    .data-table { width: 100%; border-collapse: collapse; }
    .data-table thead { background: #1b5e20; }
    .data-table th { padding: 14px 15px; text-align: left; color: white; font-weight: 600; font-size: .9rem; }
    .data-table td { padding: 14px 15px; border-bottom: 1px solid #f0f0f0; font-size: .9rem; }
    .table-row:hover { background: #f1f8e9; }
    .empty-state { text-align: center; padding: 40px !important; color: #aaa; font-style: italic; }
    .num { color: #1b5e20; font-family: monospace; font-weight: bold; }
    .money { color: #0277bd; font-weight: 700; }
    .type-badge { background: #e8f5e9; color: #1b5e20; padding: 4px 10px; border-radius: 12px; font-size: .8rem; font-weight: 600; }
    .actions { display: flex; gap: 8px; }
    .btn-valider { background: #e8f5e9; color: #1b5e20; border: 1px solid #c8e6c9; padding: 7px 14px; border-radius: 6px; cursor: pointer; font-weight: 600; font-size: .85rem; transition: all .2s; }
    .btn-valider:hover { background: #c8e6c9; }
    .btn-rejeter { background: #ffebee; color: #b71c1c; border: 1px solid #ffcdd2; padding: 7px 14px; border-radius: 6px; cursor: pointer; font-weight: 600; font-size: .85rem; transition: all .2s; }
    .btn-rejeter:hover { background: #ffcdd2; }
    .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); backdrop-filter: blur(4px); z-index: 1000; display: flex; align-items: center; justify-content: center; }
    .modal-card { background: white; border-radius: 16px; width: 520px; max-width: 95vw; box-shadow: 0 25px 50px rgba(0,0,0,0.25); animation: slideUp .3s ease; }
    @keyframes slideUp { from{opacity:0;transform:translateY(20px)} to{opacity:1;transform:translateY(0)} }
    .modal-header { padding: 22px 28px; display: flex; justify-content: space-between; align-items: center; border-radius: 16px 16px 0 0; }
    .header-valider { background: #2e7d32; } .header-rejeter { background: #c62828; }
    .modal-header h2 { margin: 0; color: white; font-size: 1.2rem; }
    .modal-close { background: rgba(255,255,255,.2); border: none; color: white; width: 30px; height: 30px; border-radius: 50%; cursor: pointer; }
    .modal-body { padding: 28px; }
    .dossier-ref, .montant-ref { margin: 0 0 12px; font-size: 1rem; }
    .form-group { margin-top: 15px; }
    .form-group label { display: block; margin-bottom: 8px; font-weight: 600; color: #333; }
    .form-group textarea { width: 100%; box-sizing: border-box; padding: 11px 14px; border: 1px solid #ddd; border-radius: 8px; font-size: .95rem; font-family: 'Inter',sans-serif; outline: none; }
    .error-banner { background: #fdecea; color: #c62828; padding: 12px; border-radius: 8px; margin-top: 10px; font-size: .9rem; }
    .modal-footer { display: flex; justify-content: flex-end; gap: 12px; padding-top: 20px; border-top: 1px solid #f0f0f0; margin-top: 20px; }
    .btn-cancel { background: #f5f5f5; color: #555; border: none; padding: 11px 22px; border-radius: 8px; cursor: pointer; font-weight: 600; }
    .btn-confirm-valider { background: #2e7d32; color: white; border: none; padding: 11px 26px; border-radius: 8px; cursor: pointer; font-weight: 700; }
    .btn-confirm-rejeter { background: #c62828; color: white; border: none; padding: 11px 26px; border-radius: 8px; cursor: pointer; font-weight: 700; }
  `]
})
export class ValidateurFinancierDashboardComponent implements OnInit {
  dossiers: any[] = [];
  loading = true;
  recherche = '';
  valides = 0; rejetes = 0;
  dossierSelectionne: any = null;
  actionType: 'valider' | 'rejeter' = 'valider';
  commentaire = '';
  erreur = '';
  soumission = false;

  constructor(private validateurService: ValidateurService) {}

  ngOnInit() { this.charger(); }

  charger() {
    this.loading = true;
    this.validateurService.getDossiersFinancier(this.recherche).subscribe({
      next: (data: any) => {
        this.dossiers = data.dossiers || data || [];
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  ouvrirAction(dossier: any, type: 'valider' | 'rejeter') {
    this.dossierSelectionne = dossier;
    this.actionType = type;
    this.commentaire = '';
    this.erreur = '';
  }

  fermerModal() {
    if (!this.soumission) this.dossierSelectionne = null;
  }

  confirmerAction() {
    this.soumission = true; this.erreur = '';
    const obs = this.actionType === 'valider'
      ? this.validateurService.validerFinancier(this.dossierSelectionne.id, this.commentaire)
      : this.validateurService.rejeterFinancier(this.dossierSelectionne.id, this.commentaire);

    obs.subscribe({
      next: () => {
        this.soumission = false;
        this.dossierSelectionne = null;
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
