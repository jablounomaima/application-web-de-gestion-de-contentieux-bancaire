import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PrestataireService } from '../../../core/services/prestataire.service';
import { Router } from '@angular/router';
import { ActivatedRoute } from '@angular/router';

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
              <td colspan="6" class="empty-state">
                Aucune mission {{ activeTab === 'enCours' ? 'en cours' : '' }}
              </td>
            </tr>
            <tr *ngFor="let m of missionsFiltrees" class="table-row">
              <td>
                <strong>{{ m.numeroMission }}</strong><br>
                <small style="color:#888">#{{ m.id }}</small>
              </td>
              <td>
                <div *ngIf="m.prestation?.dossier as d; else noDossier">
                  <strong>{{ d.numeroDossier }}</strong><br>
                  <small>👤 {{ d.client?.nom }} {{ d.client?.prenom }}</small><br>
                  <small>📄 {{ d.libelle || '—' }}</small><br>
                  <small>💰 {{ d.montant | number:'1.0-0' }} TND</small><br>
                  <small>📅 {{ d.dateCreation | date:'dd/MM/yyyy' }}</small>
                </div>
                <ng-template #noDossier><span>N/A</span></ng-template>
              </td>
              <td>
                <span class="type-badge">{{ m.prestation?.type || 'N/A' }}</span>
              </td>
              <td>{{ m.dateAssignation | date:'dd/MM/yyyy' }}</td>
              <td>
                <span class="statut-badge" [ngClass]="getStatutClass(m.statut)">
                  {{ m.statut }}
                </span>
              </td>
              <td class="actions">
                <!-- ✅ Voir dossier -->
                <button class="btn-action info" (click)="voirDossier(m)">
                  👁️ Voir dossier
                </button>

                <!-- ✅ Bouton unique vers détail mission (soumettre résultat, PV, facture) -->
                <button class="btn-action detail" (click)="voirMission(m)">
                  📋 Voir mission
                </button>

                <!-- ✅ Documents -->
                <button class="btn-action doc" (click)="ouvrirModalDocuments(m)">
                  📎 Documents
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ✅ MODAL DOCUMENTS -->
    <div class="modal-overlay"
         *ngIf="missionSelectionnee && modalType === 'documents'"
         (click)="fermerModal()">
      <div class="modal-card modal-card--wide" (click)="$event.stopPropagation()">
        <div class="modal-header teal">
          <h2>📎 Documents de la mission</h2>
          <button class="modal-close" (click)="fermerModal()">✕</button>
        </div>
        <div class="modal-body">
          <div class="mission-info-box">
            <p><strong>Mission :</strong> {{ missionSelectionnee.numeroMission }}</p>
            <p><strong>Statut :</strong> {{ missionSelectionnee.statut }}</p>
          </div>
          <!-- Upload -->
          <div class="upload-zone">
            <label class="upload-label">
              <input type="file"
                     multiple
                     (change)="onFichiersSelectionnes($event)"
                     style="display:none">
              <div class="upload-btn">
                📂 Choisir des fichiers
              </div>
            </label>
            <!-- Fichiers sélectionnés -->
            <div class="fichiers-choisis" *ngIf="fichierSelectionnes.length > 0">
              <div class="fichier-choisi" *ngFor="let f of fichierSelectionnes">
                <span>{{ iconeType(f.type) }} {{ f.name }}</span>
                <span class="taille">{{ formatTaille(f.size) }}</span>
              </div>
            </div>
            <button class="btn-upload"
                    (click)="uploaderDocuments()"
                    [disabled]="uploadEnCours || fichierSelectionnes.length === 0">
              {{ uploadEnCours ? '⏳ Upload en cours...' : '⬆️ Envoyer' }}
            </button>
          </div>
          <div class="error-banner" *ngIf="erreur">{{ erreur }}</div>
          <div class="success-banner" *ngIf="succes">{{ succes }}</div>
          <!-- Documents existants -->
          <div class="docs-existants">
            <h3>Documents envoyés ({{ documentsExistants.length }})</h3>
            <div *ngIf="documentsExistants.length === 0" class="empty-docs">
              Aucun document envoyé pour cette mission.
            </div>
            <div class="doc-item" *ngFor="let d of documentsExistants">
              <div class="doc-left">
                <span class="doc-icone">{{ iconeType(d.typeMime) }}</span>
                <div class="doc-info">
                  <span class="doc-nom">{{ d.nomFichierOriginal }}</span>
                  <span class="doc-meta">
                    {{ formatTaille(d.tailleFichier) }} ·
                    {{ d.dateUpload | date:'dd/MM/yyyy HH:mm' }}
                  </span>
                </div>
              </div>
              <button class="btn-dl"
                      (click)="telechargerDocument(d.nomFichierServeur, d.nomFichierOriginal)">
                ⬇️ Télécharger
              </button>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" (click)="fermerModal()">Fermer</button>
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
    .data-table td { padding: 14px 15px; border-bottom: 1px solid #f0f0f0; font-size: .9rem; vertical-align: top; }
    .table-row:hover { background: #f8f9ff; }
    .empty-state { text-align: center; padding: 40px !important; color: #aaa; font-style: italic; }
    .type-badge { background: #e8eaf6; color: #3949ab; padding: 4px 10px; border-radius: 12px; font-size: .8rem; font-weight: 600; }
    .statut-badge { padding: 5px 12px; border-radius: 15px; font-size: .8rem; font-weight: bold; }
    .s-assignee { background: #fff3cd; color: #856404; }
    .s-en-cours { background: #cce5ff; color: #004085; }
    .s-pv-soumis { background: #d1ecf1; color: #0c5460; }
    .s-facture { background: #d4edda; color: #155724; }
    .s-terminee { background: #e2e3e5; color: #383d41; }
    .actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .btn-action { padding: 7px 14px; border: none; border-radius: 6px; cursor: pointer; font-size: .85rem; font-weight: 600; transition: all .2s; }
    .btn-action.info   { background: #e3f2fd; color: #0d47a1; }
    .btn-action.info:hover { background: #bbdefb; }
    .btn-action.detail { background: #f3e5f5; color: #6a1b9a; }
    .btn-action.detail:hover { background: #e1bee7; }
    .btn-action.doc    { background: #e0f2f1; color: #00695c; }
    .btn-action.doc:hover { background: #b2dfdb; }

    /* ── Modal ── */
    .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); backdrop-filter: blur(4px); z-index: 1000; display: flex; align-items: center; justify-content: center; }
    .modal-card { background: white; border-radius: 16px; width: 560px; max-width: 95vw; box-shadow: 0 25px 50px rgba(0,0,0,0.25); animation: slideUp .3s ease; }
    .modal-card--wide { width: 680px; }
    @keyframes slideUp { from{opacity:0;transform:translateY(30px)} to{opacity:1;transform:translateY(0)} }
    .modal-header { padding: 22px 28px; display: flex; justify-content: space-between; align-items: center; border-radius: 16px 16px 0 0; }
    .modal-header.teal { background: #00695c; }
    .modal-header h2 { margin: 0; color: white; font-size: 1.2rem; }
    .modal-close { background: rgba(255,255,255,.2); border: none; color: white; width: 30px; height: 30px; border-radius: 50%; cursor: pointer; font-size: 1rem; }
    .modal-body { padding: 28px; }
    .mission-info-box { background: #f8f9fa; border-radius: 8px; padding: 14px; margin-bottom: 20px; border-left: 4px solid #00695c; }
    .mission-info-box p { margin: 4px 0; font-size: .9rem; }
    .error-banner { background: #fdecea; color: #c62828; padding: 12px 16px; border-radius: 8px; margin-bottom: 15px; border-left: 4px solid #c62828; font-size: .9rem; }
    .success-banner { background: #e8f5e9; color: #2e7d32; padding: 12px 16px; border-radius: 8px; margin-bottom: 15px; border-left: 4px solid #2e7d32; font-size: .9rem; }
    .modal-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 16px 28px; border-top: 1px solid #f0f0f0; }
    .btn-cancel { background: #f5f5f5; color: #555; border: none; padding: 11px 22px; border-radius: 8px; cursor: pointer; font-weight: 600; }

    /* ── Upload zone ── */
    .upload-zone { border: 2px dashed #b2dfdb; border-radius: 10px; padding: 20px; margin-bottom: 20px; text-align: center; }
    .upload-btn { display: inline-block; background: #e0f2f1; color: #00695c; padding: 10px 20px; border-radius: 8px; cursor: pointer; font-weight: 600; margin-bottom: 12px; }
    .fichiers-choisis { margin: 10px 0; text-align: left; }
    .fichier-choisi { display: flex; justify-content: space-between; padding: 6px 10px; background: #f5f5f5; border-radius: 6px; margin-bottom: 4px; font-size: .85rem; }
    .taille { color: #888; }
    .btn-upload { background: #00695c; color: white; border: none; padding: 10px 22px; border-radius: 8px; cursor: pointer; font-weight: 600; margin-top: 8px; }
    .btn-upload:disabled { opacity: .5; cursor: not-allowed; }

    /* ── Docs existants ── */
    .docs-existants h3 { font-size: 1rem; color: #333; margin-bottom: 12px; }
    .empty-docs { color: #aaa; font-style: italic; font-size: .9rem; }
    .doc-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; border: 1px solid #e0e0e0; border-radius: 8px; margin-bottom: 8px; }
    .doc-left { display: flex; align-items: center; gap: 10px; }
    .doc-icone { font-size: 1.4rem; }
    .doc-info { display: flex; flex-direction: column; }
    .doc-nom { font-size: .9rem; font-weight: 600; color: #333; }
    .doc-meta { font-size: .75rem; color: #888; }
    .btn-dl { background: #e3f2fd; color: #0d47a1; border: none; padding: 6px 14px; border-radius: 6px; cursor: pointer; font-size: .82rem; font-weight: 600; }
    .btn-dl:hover { background: #bbdefb; }
  `]
})
export class PrestataireDashboardComponent implements OnInit {

  stats: any = null;
  missions: any[] = [];
  loading = true;
  activeTab: 'enCours' | 'toutes' = 'enCours';

  missionSelectionnee: any = null;
  modalType: 'documents' | null = null;
  erreur  = '';
  succes  = '';

  fichierSelectionnes: File[] = [];
  uploadEnCours = false;
  documentsExistants: any[] = [];

  constructor(
    private prestataireService: PrestataireService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.loading = true;
    this.prestataireService.getDashboard().subscribe({
      next: (data) => {
        this.stats    = data;
        this.missions = data.dernieresMissions || [];
        this.loading  = false;
      },
      error: () => { this.loading = false; }
    });
  }

  get missionsFiltrees(): any[] {
    if (this.activeTab === 'enCours') {
      return this.missions.filter(m =>
        ['ASSIGNEE', 'EN_COURS', 'PV_SOUMIS'].includes(m.statut)
      );
    }
    return this.missions;
  }

  getStatutClass(statut: string): string {
    const map: Record<string, string> = {
      ASSIGNEE:        's-assignee',
      EN_COURS:        's-en-cours',
      PV_SOUMIS:       's-pv-soumis',
      FACTURE_SOUMISE: 's-facture',
      TERMINEE:        's-terminee'
    };
    return map[statut] || '';
  }

  // ✅ Navigation vers détail mission (soumettre résultat depuis là)
  voirMission(m: any) {
    this.router.navigate(['/prestataire/missions', m.id]);
  }

  voirDossier(m: any) {
    this.router.navigate(['/prestataire/mission', m.id, 'dossier']);
  }

  fermerModal() {
    this.missionSelectionnee = null;
    this.modalType = null;
    this.erreur = '';
    this.succes = '';
    this.fichierSelectionnes = [];
    this.documentsExistants = [];
  }

  // ── Documents ─────────────────────────────────────────────
  ouvrirModalDocuments(mission: any) {
    this.missionSelectionnee = mission;
    this.modalType = 'documents';
    this.fichierSelectionnes = [];
    this.erreur = '';
    this.succes = '';
    this.documentsExistants = [];

    this.prestataireService.getDocuments(mission.id).subscribe({
      next: (res) => this.documentsExistants = res.fichiers || [],
      error: () => {}
    });
  }

  onFichiersSelectionnes(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.fichierSelectionnes = Array.from(input.files);
    }
  }

  uploaderDocuments() {
    if (this.fichierSelectionnes.length === 0) {
      this.erreur = 'Sélectionnez au moins un fichier.';
      return;
    }
    this.erreur = '';
    this.succes = '';
    this.uploadEnCours = true;

    const formData = new FormData();
    this.fichierSelectionnes.forEach(f => formData.append('fichiers', f, f.name));

    this.prestataireService.uploaderDocuments(
      this.missionSelectionnee.id,
      formData
    ).subscribe({
      next: (res: any) => {
        this.succes = `✅ ${res.fichiers?.length ?? this.fichierSelectionnes.length} document(s) ajouté(s) avec succès !`;
        this.uploadEnCours = false;
        this.fichierSelectionnes = [];
        this.prestataireService.getDocuments(this.missionSelectionnee.id).subscribe({
          next: (r: any) => this.documentsExistants = r.fichiers || [],
          error: () => {}
        });
      },
      error: (err: any) => {
        this.erreur = err?.error?.message || err?.error?.error || 'Erreur lors de l\'upload.';
        this.uploadEnCours = false;
      }
    });
  }

  formatTaille(taille: number): string {
    if (!taille) return '0 B';
    if (taille < 1024) return taille + ' B';
    if (taille < 1024 * 1024) return (taille / 1024).toFixed(1) + ' KB';
    return (taille / (1024 * 1024)).toFixed(1) + ' MB';
  }

  iconeType(mime: string): string {
    if (!mime) return '📄';
    if (mime === 'application/pdf') return '📕';
    if (mime.startsWith('image/')) return '🖼️';
    if (mime.includes('word')) return '📝';
    if (mime.includes('sheet') || mime.includes('excel')) return '📊';
    if (mime.includes('zip')) return '🗜️';
    return '📄';
  }

  telechargerDocument(nomServeur: string, nomOriginal: string) {
    this.prestataireService.telechargerFichier(nomServeur).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nomOriginal;
        a.click();
        URL.revokeObjectURL(url);
      }
    });
  }
}