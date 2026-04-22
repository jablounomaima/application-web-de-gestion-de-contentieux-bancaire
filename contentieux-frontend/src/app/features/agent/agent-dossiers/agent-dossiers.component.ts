import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DossierService, Dossier, DossierCreation } from '../../../core/services/dossier.service';

@Component({
  selector: 'app-agent-dossiers',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="page-container">
      <!-- En-tête -->
      <div class="page-header">
        <div>
          <h1>📁 Gestion des Dossiers</h1>
          <p class="subtitle">{{ dossiers.length }} dossier(s) au total</p>
        </div>
        <button class="btn-create" (click)="ouvrirModal()">
          ➕ Nouveau Dossier
        </button>
      </div>

      <!-- Barre de recherche / filtre -->
      <div class="filters-bar">
        <input type="text" placeholder="🔍 Rechercher par N° ou client..." class="search-input" [(ngModel)]="recherche" />
        <select class="filter-select" [(ngModel)]="filtreStatut">
          <option value="">Tous les statuts</option>
          <option value="EN_COURS">En cours</option>
          <option value="VALIDE">Validé</option>
          <option value="CLOS">Clos</option>
          <option value="EN_ATTENTE">En attente</option>
        </select>
        <select class="filter-select" [(ngModel)]="filtreType">
          <option value="">Tous les types</option>
          <option value="RECOUVREMENT">Recouvrement</option>
          <option value="CONTENTIEUX">Contentieux</option>
          <option value="LITIGE">Litige</option>
        </select>
      </div>

      <!-- État de chargement -->
      <div class="loading-skeleton" *ngIf="loading">
        <div class="skeleton-row" *ngFor="let i of [1,2,3,4,5]"></div>
      </div>

      <!-- Tableau des dossiers -->
      <div class="table-card" *ngIf="!loading">
        <table class="data-table">
          <thead>
            <tr>
              <th>N° Dossier</th>
              <th>Client</th>
              <th>Type</th>
              <th>Montant Créance</th>
              <th>Date Création</th>
              <th>Statut</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngIf="dossiersFiltres.length === 0">
              <td colspan="7" class="empty-state">Aucun dossier trouvé.</td>
            </tr>
            <tr *ngFor="let d of dossiersFiltres" class="table-row">
              <td><strong class="dossier-num">{{ d.numeroDossier }}</strong></td>
              <td>{{ getClientName(d) }}</td>
              <td><span class="type-badge">{{ d.typeDossier }}</span></td>
              <td>{{ d.montantCreance | number:'1.2-2' }} TND</td>
              <td>{{ d.dateCreation | date:'dd/MM/yyyy' }}</td>
              <td>
                <span class="statut-badge" [ngClass]="getStatutClass(d.statut)">
                  {{ d.statut }}
                </span>
              </td>
              <td class="actions">
                <button class="action-btn view" (click)="voirDetails(d.id)">👁 Détails</button>
                <button class="action-btn pdf" (click)="telechargerPdf(d.id)">📄 PDF</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ═══════════════ MODAL CRÉATION ═══════════════ -->
    <div class="modal-overlay" *ngIf="showModal" (click)="fermerModal()">
      <div class="modal-card" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h2>➕ Créer un Nouveau Dossier</h2>
          <button class="modal-close" (click)="fermerModal()">✕</button>
        </div>

        <form class="modal-form" (ngSubmit)="creerDossier()">
          <!-- Type de client -->
          <div class="form-group">
            <label>Type de Client</label>
            <div class="radio-group">
              <label class="radio-label" [class.selected]="nouveauDossier.typeClient === 'PHYSIQUE'">
                <input type="radio" name="typeClient" value="PHYSIQUE" [(ngModel)]="nouveauDossier.typeClient"> 👤 Physique
              </label>
              <label class="radio-label" [class.selected]="nouveauDossier.typeClient === 'MORALE'">
                <input type="radio" name="typeClient" value="MORALE" [(ngModel)]="nouveauDossier.typeClient"> 🏢 Morale
              </label>
            </div>
          </div>

          <!-- Champs Client Physique -->
          <div class="form-row" *ngIf="nouveauDossier.typeClient === 'PHYSIQUE'">
            <div class="form-group">
              <label>Nom *</label>
              <input type="text" [(ngModel)]="nouveauDossier.nomClient" name="nom" required placeholder="Ex: Ben Ali">
            </div>
            <div class="form-group">
              <label>Prénom</label>
              <input type="text" [(ngModel)]="nouveauDossier.prenomClient" name="prenom" placeholder="Ex: Mohamed">
            </div>
          </div>
          <div class="form-group" *ngIf="nouveauDossier.typeClient === 'PHYSIQUE'">
            <label>CIN</label>
            <input type="text" [(ngModel)]="nouveauDossier.cin" name="cin" placeholder="Ex: 12345678">
          </div>

          <!-- Champs Client Morale -->
          <div class="form-group" *ngIf="nouveauDossier.typeClient === 'MORALE'">
            <label>Raison Sociale *</label>
            <input type="text" [(ngModel)]="nouveauDossier.raisonSociale" name="raisonSociale" required placeholder="Ex: Société XYZ">
          </div>
          <div class="form-group" *ngIf="nouveauDossier.typeClient === 'MORALE'">
            <label>Matricule Fiscal</label>
            <input type="text" [(ngModel)]="nouveauDossier.matriculeFiscal" name="matriculeFiscal" placeholder="Ex: 1234567A">
          </div>

          <div class="form-divider"></div>

          <!-- Infos Dossier -->
          <div class="form-row">
            <div class="form-group">
              <label>Type de Dossier *</label>
              <select [(ngModel)]="nouveauDossier.typeDossier" name="typeDossier" required>
                <option value="">-- Sélectionner --</option>
                <option value="RECOUVREMENT">Recouvrement</option>
                <option value="CONTENTIEUX">Contentieux</option>
                <option value="LITIGE">Litige</option>
              </select>
            </div>
            <div class="form-group">
              <label>Montant Créance (TND) *</label>
              <input type="number" [(ngModel)]="nouveauDossier.montantCreance" name="montant" required placeholder="0.00" min="0" step="0.01">
            </div>
          </div>

          <div class="form-group">
            <label>Description / Observations</label>
            <textarea [(ngModel)]="nouveauDossier.description" name="description" rows="3" placeholder="Détails supplémentaires..."></textarea>
          </div>

          <!-- Message d'erreur -->
          <div class="error-banner" *ngIf="erreurCreation">{{ erreurCreation }}</div>
          <div class="success-banner" *ngIf="succesCreation">{{ succesCreation }}</div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" (click)="fermerModal()">Annuler</button>
            <button type="submit" class="btn-submit" [disabled]="enCoursDeSoumission">
              <span *ngIf="!enCoursDeSoumission">✅ Créer le Dossier</span>
              <span *ngIf="enCoursDeSoumission">⏳ Création en cours...</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .page-container { font-family: 'Inter', sans-serif; }
    
    .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 25px; }
    h1 { margin: 0; font-size: 1.8rem; color: #1a237e; }
    .subtitle { margin: 5px 0 0 0; color: #777; }
    .btn-create { background: linear-gradient(135deg, #0056b3, #003366); color: white; border: none; padding: 12px 24px; border-radius: 8px; font-weight: bold; cursor: pointer; font-size: 0.95rem; box-shadow: 0 4px 10px rgba(0,86,179,0.3); transition: all 0.2s; }
    .btn-create:hover { transform: translateY(-2px); box-shadow: 0 6px 15px rgba(0,86,179,0.4); }

    .filters-bar { display: flex; gap: 15px; margin-bottom: 25px; }
    .search-input { flex: 2; padding: 12px 16px; border: 1px solid #ddd; border-radius: 8px; font-size: 0.95rem; outline: none; transition: border 0.2s; }
    .search-input:focus { border-color: #0056b3; box-shadow: 0 0 0 3px rgba(0,86,179,0.1); }
    .filter-select { flex: 1; padding: 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 0.9rem; background: white; cursor: pointer; }

    .loading-skeleton { display: flex; flex-direction: column; gap: 12px; }
    .skeleton-row { height: 55px; border-radius: 8px; background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; }
    @keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

    .table-card { background: white; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); overflow: hidden; }
    .data-table { width: 100%; border-collapse: collapse; }
    .data-table thead { background: #001f3f; }
    .data-table th { padding: 16px 15px; text-align: left; color: white; font-weight: 600; font-size: 0.9rem; letter-spacing: 0.5px; }
    .data-table td { padding: 14px 15px; border-bottom: 1px solid #f0f0f0; font-size: 0.9rem; }
    .table-row { transition: background 0.15s; }
    .table-row:hover { background: #f8fbff; }
    .empty-state { text-align: center; padding: 40px !important; color: #aaa; font-style: italic; }

    .dossier-num { color: #0056b3; font-family: monospace; }
    .type-badge { background: #e8eaf6; color: #3949ab; padding: 4px 10px; border-radius: 12px; font-size: 0.8rem; font-weight: 600; }
    .statut-badge { padding: 5px 12px; border-radius: 15px; font-size: 0.8rem; font-weight: bold; }
    .statut-en-cours { background: #fff3cd; color: #856404; }
    .statut-valide { background: #d4edda; color: #155724; }
    .statut-clos { background: #f8d7da; color: #721c24; }
    .statut-attente { background: #d1ecf1; color: #0c5460; }
    
    .actions { display: flex; gap: 8px; }
    .action-btn { padding: 6px 12px; border: none; border-radius: 6px; cursor: pointer; font-size: 0.85rem; font-weight: 500; transition: all 0.2s; }
    .action-btn.view { background: #e3f2fd; color: #1565c0; }
    .action-btn.view:hover { background: #bbdefb; }
    .action-btn.pdf { background: #fce4ec; color: #880e4f; }
    .action-btn.pdf:hover { background: #f8bbd9; }

    /* Modal */
    .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); backdrop-filter: blur(4px); z-index: 1000; display: flex; align-items: center; justify-content: center; animation: fadeIn 0.2s; }
    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
    .modal-card { background: white; border-radius: 16px; width: 650px; max-width: 95vw; max-height: 90vh; overflow-y: auto; box-shadow: 0 25px 50px rgba(0,0,0,0.25); animation: slideUp 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
    @keyframes slideUp { from { opacity: 0; transform: translateY(30px); } to { opacity: 1; transform: translateY(0); } }
    .modal-header { padding: 25px 30px; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; background: #001f3f; border-radius: 16px 16px 0 0; }
    .modal-header h2 { margin: 0; font-size: 1.3rem; color: white; }
    .modal-close { background: rgba(255,255,255,0.2); border: none; color: white; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; font-size: 1rem; display: flex; align-items: center; justify-content: center; transition: background 0.2s; }
    .modal-close:hover { background: rgba(255,255,255,0.4); }
    .modal-form { padding: 30px; }
    .form-divider { border: none; border-top: 1px dashed #eee; margin: 20px 0; }
    .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
    .form-group { margin-bottom: 20px; display: flex; flex-direction: column; }
    .form-group label { margin-bottom: 8px; font-weight: 600; color: #333; font-size: 0.9rem; }
    .form-group input, .form-group select, .form-group textarea { padding: 11px 14px; border: 1px solid #ddd; border-radius: 8px; font-size: 0.95rem; outline: none; transition: border 0.2s; font-family: 'Inter', sans-serif; }
    .form-group input:focus, .form-group select:focus, .form-group textarea:focus { border-color: #0056b3; box-shadow: 0 0 0 3px rgba(0,86,179,0.1); }
    .radio-group { display: flex; gap: 15px; }
    .radio-label { display: flex; align-items: center; gap: 8px; padding: 12px 20px; border: 2px solid #ddd; border-radius: 8px; cursor: pointer; transition: all 0.2s; font-weight: 500; }
    .radio-label.selected { border-color: #0056b3; background: #f0f7ff; color: #0056b3; }
    .radio-label input { display: none; }
    .error-banner { background: #fdecea; color: #c62828; padding: 12px 16px; border-radius: 8px; margin-bottom: 15px; font-size: 0.9rem; border-left: 4px solid #c62828; }
    .success-banner { background: #e8f5e9; color: #2e7d32; padding: 12px 16px; border-radius: 8px; margin-bottom: 15px; font-size: 0.9rem; border-left: 4px solid #2e7d32; }
    .modal-footer { display: flex; justify-content: flex-end; gap: 15px; padding-top: 10px; border-top: 1px solid #f0f0f0; }
    .btn-cancel { background: #f5f5f5; color: #555; border: none; padding: 12px 24px; border-radius: 8px; cursor: pointer; font-weight: 600; transition: background 0.2s; }
    .btn-cancel:hover { background: #e0e0e0; }
    .btn-submit { background: linear-gradient(135deg, #0056b3, #003366); color: white; border: none; padding: 12px 28px; border-radius: 8px; cursor: pointer; font-weight: 700; font-size: 0.95rem; box-shadow: 0 4px 10px rgba(0,86,179,0.3); transition: all 0.2s; }
    .btn-submit:hover:not([disabled]) { transform: translateY(-1px); box-shadow: 0 6px 15px rgba(0,86,179,0.4); }
    .btn-submit[disabled] { opacity: 0.6; cursor: not-allowed; }
  `]
})
export class AgentDossiersComponent implements OnInit {
  dossiers: Dossier[] = [];
  loading = true;
  recherche = '';
  filtreStatut = '';
  filtreType = '';
  showModal = false;
  enCoursDeSoumission = false;
  erreurCreation = '';
  succesCreation = '';

  nouveauDossier: DossierCreation = {
    typeClient: 'PHYSIQUE',
    nomClient: '',
    typeDossier: '',
    montantCreance: 0
  };

  constructor(
    private dossierService: DossierService,
    private router: Router
  ) {}

  ngOnInit() {
    this.chargerDossiers();
  }

  chargerDossiers() {
    this.loading = true;
    this.dossierService.getAllDossiers().subscribe({
      next: (data) => { this.dossiers = data; this.loading = false; },
      error: (err) => { console.error(err); this.loading = false; }
    });
  }

  get dossiersFiltres(): Dossier[] {
    return this.dossiers.filter(d => {
      const matchRecherche = !this.recherche ||
        d.numeroDossier.toLowerCase().includes(this.recherche.toLowerCase()) ||
        this.getClientName(d).toLowerCase().includes(this.recherche.toLowerCase());
      const matchStatut = !this.filtreStatut || d.statut === this.filtreStatut;
      const matchType = !this.filtreType || d.typeDossier === this.filtreType;
      return matchRecherche && matchStatut && matchType;
    });
  }

  getClientName(d: Dossier): string {
    if (!d.client) return 'N/A';
    return d.client.typeClient === 'MORALE'
      ? (d.client.raisonSociale || 'N/A')
      : `${d.client.nom || ''} ${d.client.prenom || ''}`.trim();
  }

  getStatutClass(statut: string): string {
    const map: Record<string, string> = {
      'EN_COURS': 'statut-en-cours', 'VALIDE': 'statut-valide',
      'CLOS': 'statut-clos', 'EN_ATTENTE': 'statut-attente'
    };
    return map[statut] || '';
  }

  voirDetails(id: number) {
    this.router.navigate(['/agent/dossier', id]);
  }

  telechargerPdf(id: number) {
    this.dossierService.telechargerPdf(id).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `dossier-${id}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  ouvrirModal() {
    this.nouveauDossier = { typeClient: 'PHYSIQUE', nomClient: '', typeDossier: '', montantCreance: 0 };
    this.erreurCreation = '';
    this.succesCreation = '';
    this.showModal = true;
  }

  fermerModal() {
    if (!this.enCoursDeSoumission) this.showModal = false;
  }

  creerDossier() {
    this.erreurCreation = '';
    this.enCoursDeSoumission = true;

    // Adapter les champs selon le type de client
    if (this.nouveauDossier.typeClient === 'MORALE') {
      this.nouveauDossier.nomClient = this.nouveauDossier.raisonSociale || '';
    }

    this.dossierService.creerDossier(this.nouveauDossier).subscribe({
      next: (res) => {
        this.succesCreation = '✅ Dossier créé avec succès !';
        this.enCoursDeSoumission = false;
        setTimeout(() => {
          this.showModal = false;
          this.chargerDossiers();
        }, 1500);
      },
      error: (err) => {
        this.erreurCreation = err.error?.error || '❌ Erreur lors de la création.';
        this.enCoursDeSoumission = false;
      }
    });
  }
}
