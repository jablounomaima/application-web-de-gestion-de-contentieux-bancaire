import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AvocatService } from '../../../core/services/avocat.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-avocat-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  template: `
<div class="page-container">

  <!-- ══ Header ══ -->
  <div class="page-header">
    <div class="title-group">
      <span class="role-badge">AVOCAT</span>
      <h1>Espace d'avocat</h1>
      <p class="subtitle">Pilotez vos affaires, planifiez vos audiences et soumettez vos honoraires</p>
    </div>
  </div>

  <!-- ══ Métriques ══ -->
  <div class="stats-row">

    <div class="stat-card">
      <div class="stat-icon blue-icon">📁</div>
      <div class="stat-info">
        <span class="stat-value">{{ totalAffaires }}</span>
        <h4>Total affaires</h4>
      </div>
    </div>

    <div class="stat-card">
      <div class="stat-icon indigo-icon">⚖️</div>
      <div class="stat-info">
        <span class="stat-value">{{ affairesEnCours }}</span>
        <h4>En cours</h4>
      </div>
    </div>

    <div class="stat-card">
      <div class="stat-icon amber-icon">🗓️</div>
      <div class="stat-info">
        <span class="stat-value">{{ audiencesAVenir }}</span>
        <h4>Audiences à venir</h4>
      </div>
    </div>
    <div class="stat-card">
  <div class="stat-icon green-icon">💰</div>
  <div class="stat-info">
    <span class="stat-value">
      {{ totalHonoraires | number:'1.3-3' }} <small>TND</small>
    </span>
    <h4>Total Honoraires HT</h4>
  </div>
</div>

<div class="stat-card">
  <div class="stat-icon teal-icon">🧾</div>
  <div class="stat-info">
    <span class="stat-value">
      {{ nombreFactures }}
      <small *ngIf="facturesPayees > 0" class="payees-hint">
        dont {{ facturesPayees }} payée(s)
      </small>
    </span>
    <h4>Factures soumises</h4>
  </div>
</div>

    <div class="stat-card">
      <div class="stat-icon purple-icon">📜</div>
      <div class="stat-info">
        <span class="stat-value">{{ jugementRendu }}</span>
        <h4>Jugements rendus</h4>
      </div>
    </div>

    <div class="stat-card">
      <div class="stat-icon orange-icon">📋</div>
      <div class="stat-info">
        <span class="stat-value">{{ pvEnAttente }}</span>
        <h4>PV en attente</h4>
      </div>
    </div>

    <div class="stat-card">
      <div class="stat-icon rose-icon">🧾</div>
      <div class="stat-info">
        <span class="stat-value">{{ facturesEnAttente }}</span>
        <h4>Factures en attente</h4>
      </div>
    </div>

  </div>

  <!-- ══ Loader ══ -->
  <div class="loader-state" *ngIf="loading">
    <span class="spin">⚖️</span>
    <p>Chargement des affaires...</p>
  </div>

  <!-- ══ Tableau affaires ══ -->
  <div class="content-card" *ngIf="!loading">

    <div class="card-header">
      <h2>Mes Affaires
        <span class="count-badge">{{ affairesFiltrees.length }}</span>
      </h2>
      <div class="search-box">
        <span class="search-icon">🔍</span>
        <input type="text" [(ngModel)]="search" (input)="filtrer()"
               placeholder="Rechercher par N° affaire, tribunal..."/>
      </div>
    </div>

    <!-- Vide -->
    <div class="empty-state" *ngIf="affairesFiltrees.length === 0">
      <div class="empty-icon">⚖️</div>
      <p>Aucune affaire trouvée.</p>
    </div>

    <table class="premium-table" *ngIf="affairesFiltrees.length > 0">
      <thead>
        <tr>
          <th>Référence</th>
          <th>Tribunal</th>
          <th>Prochaine audience</th>
          <th>PV</th>
          <th>Facture</th>
          <th>Statut affaire</th>
         
        </tr>
      </thead>
      <tbody>
        <tr *ngFor="let aff of affairesFiltrees" class="data-row">

          <!-- Référence -->
          <td>
            <div class="aff-ref">
              <code>#{{ aff.id }}</code>
              <span class="aff-num">{{ aff.numeroAffaire }}</span>
              <span class="aff-date" *ngIf="aff.dateLancement">
                {{ aff.dateLancement | date:'dd/MM/yyyy' }}
              </span>
            </div>
          </td>

          <!-- Tribunal -->
          <td>
            <div class="court-info" *ngIf="aff.tribunal; else noTribunal">
              <span>{{ aff.tribunal }}</span>
              <small *ngIf="aff.chambre">{{ aff.chambre }}</small>
              <small *ngIf="aff.numeroRole" class="role-num">Rôle : {{ aff.numeroRole }}</small>
            </div>
            <ng-template #noTribunal>
              <span class="empty-val">—</span>
            </ng-template>
          </td>

          <!-- Prochaine audience -->
          <td>
            <span class="audience-date" *ngIf="aff.dateProchainAudience; else noAudience">
              📅 {{ aff.dateProchainAudience | date:'dd/MM/yyyy' }}
            </span>
            <ng-template #noAudience>
              <span class="empty-val">—</span>
            </ng-template>
          </td>

          <!-- PV -->
          <td>
            <span *ngIf="aff.pvTexte; else noPV"
              class="status-pill"
              [ngClass]="{
                'pill-success': aff.pvStatut === 'VALIDE',
                'pill-warning': aff.pvStatut === 'EN_ATTENTE',
                'pill-danger':  aff.pvStatut === 'REFUSE'
              }">
              {{ aff.pvStatut || 'EN_ATTENTE' }}
            </span>
            <ng-template #noPV>
              <span class="none-badge">Non soumis</span>
            </ng-template>
          </td>

          <!-- Facture -->
          <td>
            <div *ngIf="aff.factureRef; else noFacture">
              <span class="status-pill"
                [ngClass]="{
                  'pill-success': aff.factureStatut === 'PAYEE',
                  'pill-warning': aff.factureStatut === 'EN_ATTENTE',
                  'pill-danger':  aff.factureStatut === 'REJETEE'
                }">
                {{ aff.factureStatut || 'EN_ATTENTE' }}
              </span>
              <div class="facture-montant" *ngIf="aff.montantFacture">
                {{ aff.montantFacture | number:'1.3-3' }} TND
              </div>
            </div>
            <ng-template #noFacture>
              <span class="none-badge">Non soumise</span>
            </ng-template>
          </td>

          <!-- Statut affaire -->
          <td>
            <span class="status-pill"
              [ngClass]="{
                'pill-blue':   aff.statut === 'EN_COURS',
                'pill-green':  aff.statut === 'JUGEMENT_RENDU',
                'pill-orange': aff.statut === 'EXECUTION_FORCEE',
                'pill-purple': aff.statut === 'TRANSACTION',
                'pill-grey':   aff.statut === 'CLOSE'
              }">
              {{ statutLabel(aff.statut) }}
            </span>
          </td>

        

        </tr>
      </tbody>
    </table>

  </div>

</div>
  `,
  styles: [`
    /* ── Page ── */
    .page-container { padding: 30px; background: #f0f2f5; min-height: 100vh; font-family: 'Inter', sans-serif; }

    /* ── Header ── */
    .page-header { margin-bottom: 30px; }
    .role-badge { background: #e0e7ff; color: #4338ca; padding: 4px 10px; border-radius: 6px; font-weight: 800; font-size: 0.7rem; letter-spacing: 1px; display: inline-block; margin-bottom: 8px; }
    h1 { margin: 0; font-size: 2rem; color: #1e293b; font-weight: 800; }
    .subtitle { color: #64748b; margin-top: 6px; font-size: 1rem; }

    /* ── Stats ── */
    .stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 28px; }
    @media (max-width: 1100px) { .stats-row { grid-template-columns: repeat(3, 1fr); } }
    @media (max-width: 700px)  { .stats-row { grid-template-columns: repeat(2, 1fr); } }
    .stat-card { background: white; border-radius: 16px; padding: 20px; display: flex; align-items: center; gap: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.04); border: 1px solid #e2e8f0; transition: transform 0.2s; }
    .stat-card:hover { transform: translateY(-2px); }
    .stat-icon { width: 52px; height: 52px; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 1.6rem; flex-shrink: 0; }
    .blue-icon   { background: #e0f2fe; }
    .indigo-icon { background: #e0e7ff; }
    .amber-icon  { background: #fef3c7; }
    .green-icon  { background: #dcfce7; }
    .purple-icon { background: #f3e8ff; }
    .orange-icon { background: #ffedd5; }
    .rose-icon   { background: #ffe4e6; }
    .stat-info h4 { margin: 2px 0 0; color: #64748b; font-size: 0.78rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
    .stat-value { font-size: 1.6rem; font-weight: 800; color: #1e293b; line-height: 1; }
    .stat-value small { font-size: 0.7rem; color: #94a3b8; font-weight: 600; }

    /* ── Loader ── */
    .loader-state { text-align: center; padding: 60px; color: #94a3b8; }
    .spin { font-size: 2.5rem; display: block; animation: spin 1s linear infinite; margin-bottom: 12px; }
    @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
    .teal-icon { background: #ccfbf1; }
    .payees-hint { font-size: 0.65rem; color: #0d9488; font-weight: 700; display: block; margin-top: 2px; }
    /* ── Content card ── */
    .content-card { background: white; border-radius: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); border: 1px solid #e2e8f0; overflow: hidden; }
    .card-header { padding: 22px 28px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; }
    .card-header h2 { margin: 0; font-size: 1.2rem; color: #1e293b; font-weight: 700; display: flex; align-items: center; gap: 10px; }
    .count-badge { background: #e0e7ff; color: #4338ca; font-size: 0.75rem; font-weight: 800; padding: 2px 10px; border-radius: 20px; }
    .search-box { position: relative; display: flex; align-items: center; }
    .search-icon { position: absolute; left: 12px; font-size: 0.9rem; }
    .search-box input { padding: 10px 16px 10px 36px; border: 1.5px solid #e2e8f0; border-radius: 10px; width: 280px; outline: none; font-size: 0.88rem; transition: border 0.2s; }
    .search-box input:focus { border-color: #4338ca; box-shadow: 0 0 0 3px rgba(67,56,202,0.08); }

    /* ── Empty ── */
    .empty-state { text-align: center; padding: 60px; color: #94a3b8; }
    .empty-icon { font-size: 2.5rem; margin-bottom: 12px; }
    .empty-val { color: #cbd5e1; font-size: 0.9rem; }

    /* ── Table ── */
    .premium-table { width: 100%; border-collapse: collapse; font-size: 0.85rem; }
    .premium-table th { text-align: left; padding: 14px 20px; background: #f8fafc; color: #64748b; font-weight: 700; font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 2px solid #e2e8f0; white-space: nowrap; }
    .premium-table td { padding: 14px 20px; border-bottom: 1px solid #f1f5f9; color: #475569; vertical-align: middle; }
    .data-row:last-child td { border-bottom: none; }
    .data-row:hover { background: #f8fafc; }

    /* ── Cellules ── */
    .aff-ref { display: flex; flex-direction: column; gap: 3px; }
    .aff-num { font-size: 0.82rem; font-weight: 700; color: #1e293b; }
    .aff-date { font-size: 0.72rem; color: #94a3b8; }
    code { background: #f1f5f9; padding: 3px 7px; border-radius: 6px; font-family: 'JetBrains Mono', monospace; font-size: 0.78rem; color: #4338ca; }
    .court-info { display: flex; flex-direction: column; gap: 2px; }
    .court-info span { font-weight: 600; color: #1e293b; }
    .court-info small { color: #94a3b8; font-size: 0.75rem; }
    .role-num { color: #64748b !important; }
    .audience-date { font-size: 0.82rem; font-weight: 600; color: #0369a1; }
    .facture-montant { font-size: 0.75rem; color: #64748b; margin-top: 3px; font-weight: 600; }

    /* ── Pills ── */
    .status-pill { display: inline-block; padding: 3px 10px; border-radius: 20px; font-size: 0.7rem; font-weight: 800; white-space: nowrap; }
    .pill-success { background: #dcfce7; color: #15803d; }
    .pill-warning { background: #fef9c3; color: #a16207; }
    .pill-danger  { background: #fee2e2; color: #991b1b; }
    .pill-blue    { background: #e0f2fe; color: #0369a1; }
    .pill-green   { background: #dcfce7; color: #15803d; }
    .pill-orange  { background: #ffedd5; color: #c2410c; }
    .pill-purple  { background: #f3e8ff; color: #7e22ce; }
    .pill-grey    { background: #f1f5f9; color: #64748b; }
    .none-badge { font-size: 0.75rem; color: #94a3b8; background: #f8fafc; border: 1px solid #e2e8f0; padding: 2px 8px; border-radius: 6px; display: inline-block; }

    /* ── Action buttons ── */
    .action-btns { display: flex; gap: 6px; }
    .btn-action { width: 32px; height: 32px; border: none; border-radius: 8px; cursor: pointer; font-size: 1rem; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
    .btn-audiences  { background: #e0f2fe; }
    .btn-audiences:hover  { background: #bae6fd; transform: scale(1.1); }
    .btn-jugement   { background: #e0e7ff; }
    .btn-jugement:hover   { background: #c7d2fe; transform: scale(1.1); }
    .btn-honoraires { background: #dcfce7; }
    .btn-honoraires:hover { background: #bbf7d0; transform: scale(1.1); }
    .btn-dossier    { background: #fef9c3; }
    .btn-dossier:hover    { background: #fef08a; transform: scale(1.1); }
  `]
})
export class AvocatDashboardComponent implements OnInit {

  stats: any       = null;
  affaires: any[]  = [];
  affairesFiltrees: any[] = [];
  loading          = true;
  search           = '';

  constructor(
    private avocatService: AvocatService,
    private router: Router
  ) {}

  // ════════════════════════════════════════
  // Lifecycle
  // ════════════════════════════════════════

  ngOnInit() {
    this.chargerStats();
    this.chargerAffaires();
  }

  // ════════════════════════════════════════
  // Chargement
  // ════════════════════════════════════════

  chargerStats() {
    this.avocatService.getDashboard().subscribe({
      next: (s) => this.stats = s,
      error: (e) => console.error('Erreur stats:', e)
    });
  }

  chargerAffaires() {
    this.loading = true;
    this.avocatService.getAffaires().subscribe({
      next: (data: any) => {
        this.affaires = (data.affaires || []).map((a: any) => ({
          ...a,
          missionStatut:  data.missionStatuts?.[a.id] ?? null,
          missionId:      data.missionIds?.[a.id]     ?? null,
          pvTexte:        a.pvTexte        ?? null,
          pvStatut:       a.pvStatut       ?? null,
          pvFichiers:     a.pvFichiers     ?? [],
          factureRef:     a.factureRef     ?? null,
          montantFacture: a.montantFacture ?? null,
          factureStatut:  a.factureStatut  ?? null,
        }));
        this.affairesFiltrees = [...this.affaires];
        this.loading = false;
      },
      error: (e) => {
        console.error('Erreur affaires:', e);
        this.loading = false;
      }
    });
  }

  // ════════════════════════════════════════
  // Getters métriques
  // ════════════════════════════════════════

  get totalAffaires(): number {
    return this.affaires.length;
  }

  get affairesEnCours(): number {
    return this.affaires.filter(a => a.statut === 'EN_COURS').length;
  }

  get jugementRendu(): number {
    return this.affaires.filter(a => a.statut === 'JUGEMENT_RENDU').length;
  }

  // ✅ Depuis backend (affaireService.getAudiencesAVenir)
  get audiencesAVenir(): number {
    return this.stats?.audiencesAVenir ?? 0;
  }

  // ✅ Depuis backend (mission.montantFacture)
  get totalHonoraires(): number {
    return this.stats?.totalHonoraires ?? 0;
  }

  get pvEnAttente(): number {
    return this.affaires.filter(a => a.pvStatut === 'EN_ATTENTE').length;
  }

  get facturesEnAttente(): number {
    return this.affaires.filter(a => a.factureStatut === 'EN_ATTENTE').length;
  }

  // ════════════════════════════════════════
  // Filtres
  // ════════════════════════════════════════

  filtrer() {
    const q = this.search.toLowerCase().trim();
    if (!q) {
      this.affairesFiltrees = [...this.affaires];
      return;
    }
    this.affairesFiltrees = this.affaires.filter(a =>
      a.numeroAffaire?.toLowerCase().includes(q) ||
      a.tribunal?.toLowerCase().includes(q)      ||
      a.statut?.toLowerCase().includes(q)        ||
      a.chambre?.toLowerCase().includes(q)
    );
  }

  // ════════════════════════════════════════
  // Navigation
  // ════════════════════════════════════════

  naviguer(aff: any, section: string) {
    this.router.navigate(['/avocat/affaires', aff.id, section]);
  }

  // ════════════════════════════════════════
  // Helpers affichage
  // ════════════════════════════════════════

  statutLabel(s: string): string {
    const m: Record<string, string> = {
      EN_COURS:          'En cours',
      JUGEMENT_RENDU:    'Jugement rendu',
      EXECUTION_FORCEE:  'Exécution forcée',
      TRANSACTION:       'Transaction',
      CLOSE:             'Clôturée'
    };
    return m[s] ?? s;
  }

  get nombreFactures(): number {
    return this.stats?.nombreFactures ?? 
      this.affaires.filter(a => a.factureRef).length;
  }
  
  get facturesPayees(): number {
    return this.stats?.facturesPayees ??
      this.affaires.filter(a => a.factureStatut === 'PAYEE').length;
  }
}