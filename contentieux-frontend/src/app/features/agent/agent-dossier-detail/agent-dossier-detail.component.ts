import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DossierService } from '../../../core/services/dossier.service';

@Component({
  selector: 'app-agent-dossier-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="page-container" *ngIf="dossier">
      <!-- Breadcrumb -->
      <nav class="breadcrumb-nav">
        <a routerLink="/agent/dossiers">Dossiers</a>
        <span class="sep">/</span>
        <span class="current">{{ dossier.numeroDossier }}</span>
      </nav>

      <!-- Dashboard Header -->
      <header class="dossier-header">
        <div class="title-section">
          <div class="ref-badge">DOSSIER #{{ dossier.id }}</div>
          <h1>{{ dossier.numeroDossier }}</h1>
          <p class="client-name">{{ getClientName() }}</p>
        </div>
        <div class="actions-section">
          <span class="statut-badge" [ngClass]="getStatutClass(dossier.statut)">{{ dossier.statut }}</span>
          <button class="btn-outline" (click)="telechargerPdf()">📄 PDF</button>
          <button class="btn-primary" (click)="showModalPrestation = true" *ngIf="dossier.statut === 'VALIDE'">
            <span class="icon">🚀</span> Lancer une Prestation
          </button>
        </div>
      </header>

      <!-- Key Metrics -->
      <div class="metrics-grid">
        <div class="metric-card">
          <label>Type de Dossier</label>
          <div class="value">{{ dossier.typeDossier }}</div>
        </div>
        <div class="metric-card">
          <label>Montant Créance</label>
          <div class="value money">{{ dossier.montantCreance | number:'1.2-2' }} <span>TND</span></div>
        </div>
        <div class="metric-card">
          <label>Date Création</label>
          <div class="value">{{ dossier.dateCreation | date:'dd/MM/yyyy' }}</div>
        </div>
        <div class="metric-card">
          <label>Responsable</label>
          <div class="value agent">{{ dossier.agentUsername || 'N/A' }}</div>
        </div>
      </div>

      <!-- Content Layout -->
      <div class="content-layout">
        
        <!-- Left Column: Prestations & Missions -->
        <div class="main-column">
          <div class="section-card">
            <div class="section-header">
              <h2>📋 Prestations & Interventions</h2>
            </div>

            <div class="prestations-list">
              <div class="empty-state" *ngIf="!prestations || prestations.length === 0">
                <div class="empty-icon">📭</div>
                <p>Aucune prestation lancée pour le moment.</p>
              </div>

              <div class="prestation-item" *ngFor="let p of prestations">
                <div class="p-header">
                  <div class="p-title">
                    <h3>{{ p.type }}</h3>
                    <span class="p-date">Lancée le {{ p.dateLancement | date:'dd MMM yyyy' }}</span>
                  </div>
                  <div class="p-actions">
                    <span class="badge-mini">{{ p.statut }}</span>
                    <button class="btn-mini" (click)="ouvrirDesigner(p)">+ Désigner</button>
                  </div>
                </div>
                
                <p class="p-desc" *ngIf="p.description">{{ p.description }}</p>

                <!-- Missions Grid -->
                <div class="missions-grid" *ngIf="p.missions && p.missions.length > 0">
                  <div class="mission-pill" *ngFor="let m of p.missions">
                    <div class="m-avatar">{{ m.prestataire?.nom?.[0] || '?' }}</div>
                    <div class="m-body">
                      <strong>{{ m.prestataire?.nom }} {{ m.prestataire?.prenom }}</strong>
                      <small>{{ m.prestataire?.type }} • {{ m.statut }}</small>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Right Column: Documents or History (Optional for now) -->
        <div class="side-column">
          <div class="section-card compact">
            <div class="section-header">
              <h3>📄 Documents</h3>
            </div>
            <div class="doc-list">
              <div class="doc-item">
                <span class="doc-icon">📄</span>
                <div class="doc-info">
                  <strong>Contrat Client</strong>
                  <small>PDF • 1.2 MB</small>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- ═══ MODAL PRESTATION ═══ -->
    <div class="modal-overlay" *ngIf="showModalPrestation" (click)="showModalPrestation = false">
      <div class="modal-card" (click)="$event.stopPropagation()">
        <div class="modal-header header-dark">
          <h2>🚀 Nouvelle Prestation</h2>
          <button class="modal-close" (click)="showModalPrestation = false">✕</button>
        </div>
        <form class="modal-form" (ngSubmit)="lancerPrestation()">
          <div class="form-group">
            <label>Nature de l\\'intervention</label>
            <select [(ngModel)]="formPrestation.type" name="type" required>
              <option value="RECOUVREMENT_AMIABLE">Recouvrement Amiable</option>
              <option value="PROCEDURE_JUDICIAIRE">Procédure Judiciaire</option>
              <option value="EXPERTISE">Expertise</option>
              <option value="SAISIE">Saisie</option>
              <option value="HUISSIER">Huissier</option>
            </select>
          </div>
          <div class="form-group">
            <label>Instructions & Détails</label>
            <textarea [(ngModel)]="formPrestation.description" name="description" rows="4" placeholder="Précisez les objectifs..."></textarea>
          </div>
          <div class="error-banner" *ngIf="erreurPrestation">{{ erreurPrestation }}</div>
          <div class="modal-footer">
            <button type="button" class="btn-cancel" (click)="showModalPrestation = false">Annuler</button>
            <button type="submit" class="btn-submit">Lancer la Prestation</button>
          </div>
        </form>
      </div>
    </div>

    <!-- ═══ MODAL DESIGNER PRESTATAIRE ═══ -->
    <div class="modal-overlay" *ngIf="selectedPrestation" (click)="selectedPrestation = null">
      <div class="modal-card" (click)="$event.stopPropagation()">
        <div class="modal-header header-blue">
          <h2>👤 Désigner un Prestataire</h2>
          <button class="modal-close" (click)="selectedPrestation = null">✕</button>
        </div>
        <div class="modal-body p-30" *ngIf="loadingDesigner">Chargement des prestataires...</div>
        <form class="modal-form" (ngSubmit)="designerPrestataire()" *ngIf="!loadingDesigner">
          <div class="form-group">
            <label>Choisir un Prestataire ({{ selectedPrestation.type }})</label>
            <select [(ngModel)]="formMission.prestataireId" name="prestataireId" required>
              <option *ngFor="let pr of listPrestataires" [value]="pr.id">
                {{ pr.nom }} {{ pr.prenom }} ({{ pr.type }})
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>Instructions spécifiques</label>
            <textarea [(ngModel)]="formMission.description" name="description" rows="3"></textarea>
          </div>
          <div class="form-group">
            <label>Échéance prévue</label>
            <input type="date" [(ngModel)]="formMission.dateFinPrevue" name="dateFinPrevue">
          </div>
          <div class="modal-footer">
            <button type="button" class="btn-cancel" (click)="selectedPrestation = null">Annuler</button>
            <button type="submit" class="btn-submit">Assigner la Mission</button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .page-container { padding: 30px; background: #f8fafc; min-height: 100vh; font-family: 'Inter', sans-serif; }
    
    .breadcrumb-nav { display: flex; align-items: center; gap: 10px; margin-bottom: 25px; font-size: 0.9rem; }
    .breadcrumb-nav a { color: #64748b; text-decoration: none; font-weight: 500; transition: color 0.2s; }
    .breadcrumb-nav a:hover { color: #0f172a; }
    .breadcrumb-nav .sep { color: #cbd5e1; }
    .breadcrumb-nav .current { color: #0f172a; font-weight: 700; }

    .dossier-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 35px; }
    .ref-badge { background: #e2e8f0; color: #475569; padding: 4px 10px; border-radius: 6px; font-weight: 800; font-size: 0.7rem; letter-spacing: 0.5px; margin-bottom: 8px; display: inline-block; }
    h1 { font-size: 2.2rem; color: #0f172a; margin: 0; font-weight: 800; }
    .client-name { color: #64748b; font-size: 1.1rem; margin: 5px 0 0; }
    .actions-section { display: flex; align-items: center; gap: 12px; }
    
    .statut-badge { padding: 8px 16px; border-radius: 12px; font-weight: 800; font-size: 0.85rem; text-transform: uppercase; }
    .statut-valide { background: #dcfce7; color: #15803d; }
    .statut-en-cours { background: #fef9c3; color: #854d0e; }
    .btn-outline { background: white; border: 1.5px solid #e2e8f0; padding: 10px 18px; border-radius: 10px; font-weight: 700; cursor: pointer; transition: all 0.2s; }
    .btn-outline:hover { background: #f8fafc; border-color: #cbd5e1; }
    .btn-primary { background: #0f172a; color: white; border: none; padding: 12px 24px; border-radius: 12px; font-weight: 700; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 8px; }
    .btn-primary:hover { background: #334155; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

    .metrics-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 40px; }
    .metric-card { background: white; padding: 20px; border-radius: 16px; border: 1px solid #f1f5f9; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
    .metric-card label { display: block; font-size: 0.75rem; color: #94a3b8; text-transform: uppercase; font-weight: 700; margin-bottom: 8px; }
    .metric-card .value { font-size: 1.2rem; font-weight: 800; color: #1e293b; }
    .metric-card .value.money { color: #0f172a; }
    .metric-card .value.money span { font-size: 0.9rem; color: #94a3b8; }
    .metric-card .value.agent { color: #6366f1; }

    .content-layout { display: grid; grid-template-columns: 1fr 300px; gap: 30px; }
    .section-card { background: white; border-radius: 20px; border: 1px solid #f1f5f9; box-shadow: 0 1px 3px rgba(0,0,0,0.05); overflow: hidden; }
    .section-header { padding: 24px 30px; border-bottom: 1px solid #f1f5f9; }
    .section-header h2 { margin: 0; font-size: 1.2rem; color: #1e293b; font-weight: 700; }
    .section-header h3 { margin: 0; font-size: 1rem; color: #1e293b; font-weight: 700; }

    .prestations-list { padding: 20px; }
    .prestation-item { border: 1px solid #f1f5f9; border-radius: 16px; padding: 20px; margin-bottom: 15px; background: #fff; transition: all 0.2s; }
    .prestation-item:hover { border-color: #e2e8f0; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
    .p-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 15px; }
    .p-title h3 { margin: 0; font-size: 1.1rem; color: #0f172a; }
    .p-date { font-size: 0.8rem; color: #94a3b8; }
    .p-actions { display: flex; align-items: center; gap: 10px; }
    .badge-mini { background: #f1f5f9; color: #475569; padding: 4px 10px; border-radius: 6px; font-size: 0.75rem; font-weight: 700; }
    .btn-mini { background: #eff6ff; color: #2563eb; border: none; padding: 6px 12px; border-radius: 8px; font-weight: 700; font-size: 0.75rem; cursor: pointer; transition: all 0.2s; }
    .btn-mini:hover { background: #dbeafe; }
    .p-desc { color: #64748b; font-size: 0.95rem; margin-bottom: 20px; line-height: 1.5; }

    .missions-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px; }
    .mission-pill { display: flex; align-items: center; gap: 12px; background: #f8fafc; padding: 10px; border-radius: 12px; border: 1px solid #f1f5f9; }
    .m-avatar { width: 32px; height: 32px; border-radius: 8px; background: #e0e7ff; color: #4338ca; display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 0.8rem; }
    .m-body { display: flex; flex-direction: column; }
    .m-body strong { font-size: 0.85rem; color: #0f172a; }
    .m-body small { font-size: 0.75rem; color: #64748b; }

    .empty-state { text-align: center; padding: 60px 20px; color: #94a3b8; }
    .empty-icon { font-size: 3rem; margin-bottom: 15px; opacity: 0.3; }

    .doc-list { padding: 20px; }
    .doc-item { display: flex; align-items: center; gap: 12px; padding: 12px; border-radius: 12px; transition: background 0.2s; cursor: pointer; }
    .doc-item:hover { background: #f8fafc; }
    .doc-icon { font-size: 1.5rem; }
    .doc-info { display: flex; flex-direction: column; }
    .doc-info strong { font-size: 0.9rem; color: #1e293b; }
    .doc-info small { font-size: 0.75rem; color: #94a3b8; }

    /* Modal Styling */
    .modal-overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.7); backdrop-filter: blur(4px); z-index: 1000; display: flex; align-items: center; justify-content: center; }
    .modal-card { background: white; border-radius: 24px; width: 550px; max-width: 95vw; box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1); overflow: hidden; animation: zoomIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
    @keyframes zoomIn { from { transform: scale(0.9); opacity: 0; } to { transform: scale(1); opacity: 1; } }
    .modal-header { padding: 20px 30px; display: flex; justify-content: space-between; align-items: center; }
    .header-dark { background: #0f172a; color: white; }
    .header-blue { background: #1e40af; color: white; }
    .modal-header h2 { margin: 0; font-size: 1.2rem; }
    .modal-close { background: rgba(255,255,255,0.2); border: none; color: white; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; }
    .modal-form { padding: 30px; }
    .form-group { margin-bottom: 20px; }
    .form-group label { display: block; margin-bottom: 8px; font-weight: 700; color: #334155; font-size: 0.9rem; }
    .form-group input, .form-group select, .form-group textarea { width: 100%; box-sizing: border-box; padding: 12px 16px; border: 1.5px solid #e2e8f0; border-radius: 12px; font-size: 1rem; outline: none; transition: all 0.2s; font-family: inherit; }
    .form-group input:focus, .form-group select:focus, .form-group textarea:focus { border-color: #0f172a; box-shadow: 0 0 0 4px rgba(15, 23, 42, 0.05); }
    .modal-footer { display: flex; justify-content: flex-end; gap: 12px; margin-top: 10px; }
    .btn-cancel { background: #f1f5f9; color: #475569; border: none; padding: 12px 24px; border-radius: 12px; font-weight: 700; cursor: pointer; }
    .btn-submit { background: #0f172a; color: white; border: none; padding: 12px 24px; border-radius: 12px; font-weight: 700; cursor: pointer; }
  `]
})
export class AgentDossierDetailComponent implements OnInit {
  dossier: any = null;
  prestations: any[] = [];
  loading = true;
  
  // Modals state
  showModalPrestation = false;
  selectedPrestation: any = null;
  loadingDesigner = false;
  listPrestataires: any[] = [];
  
  erreurPrestation = '';
  formPrestation = { type: 'RECOUVREMENT_AMIABLE', description: '' };
  formMission = { prestataireId: '', description: '', dateFinPrevue: '' };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private dossierService: DossierService
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.refreshData(id);
  }

  refreshData(id: number) {
    this.loading = true;
    this.dossierService.getDossierDetails(id).subscribe({
      next: (data) => {
        this.dossier = data.dossier || data;
        this.prestations = data.prestations || [];
        this.loading = false;
      },
      error: (err) => { console.error(err); this.loading = false; }
    });
  }

  getClientName(): string {
    if (!this.dossier?.client) return 'N/A';
    const c = this.dossier.client;
    return c.typeClient === 'MORALE' ? (c.raisonSociale || 'N/A') : `${c.nom || ''} ${c.prenom || ''}`.trim();
  }

  getStatutClass(statut: string): string {
    return { 'EN_COURS': 'statut-en-cours', 'VALIDE': 'statut-valide', 'CLOS': 'statut-clos' }[statut] || '';
  }

  telechargerPdf() {
    this.dossierService.telechargerPdf(this.dossier.id).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = `dossier-${this.dossier.numeroDossier}.pdf`; a.click();
      URL.revokeObjectURL(url);
    });
  }

  lancerPrestation() {
    this.erreurPrestation = '';
    this.dossierService.lancerPrestation(this.dossier.id, this.formPrestation).subscribe({
      next: () => {
        this.showModalPrestation = false;
        this.refreshData(this.dossier.id);
      },
      error: (err) => { this.erreurPrestation = err.error?.error || 'Erreur lors du lancement.'; }
    });
  }

  ouvrirDesigner(prestation: any) {
    this.selectedPrestation = prestation;
    this.loadingDesigner = true;
    this.dossierService.getDetailPrestation(this.dossier.id, prestation.id).subscribe({
      next: (data) => {
        this.listPrestataires = data.prestataires || [];
        this.loadingDesigner = false;
      },
      error: () => this.loadingDesigner = false
    });
  }

  designerPrestataire() {
    this.dossierService.designerPrestataire(this.dossier.id, this.selectedPrestation.id, this.formMission).subscribe({
      next: () => {
        this.selectedPrestation = null;
        this.refreshData(this.dossier.id);
        this.formMission = { prestataireId: '', description: '', dateFinPrevue: '' };
      },
      error: (err) => alert(err.error?.error || 'Erreur')
    });
  }
}
