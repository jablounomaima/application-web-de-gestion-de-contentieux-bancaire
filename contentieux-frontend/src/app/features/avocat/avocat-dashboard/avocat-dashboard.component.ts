import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AvocatService } from '../../../core/services/avocat.service';
import { Router } from '@angular/router';
import { NotificationService, NotificationDTO  } from '../../../core/services/notification.service';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-avocat-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  template: `
<div class="page-container">
  <div class="page-header">
    <div class="title-group">
      <span class="role-badge">AVOCAT</span>
      <h1>Espace d'avocat</h1>
      <p class="subtitle">Pilotez vos affaires, planifiez vos audiences et soumettez vos honoraires</p>
    </div>
  </div>



  <!-- ═══ DIAGRAMMES STATIQUES ═══ -->
  <section class="charts-panel" *ngIf="!loading">
    <div class="chart-card">
      <div class="chart-header">
        <h3>Répartition des dossiers</h3>
        <span>Vue synthétique</span>
      </div>
      <div class="bars">
        <div class="bar-item" *ngFor="let bar of chartBars">
          <div class="bar-track">
            <div class="bar-fill" [style.height.%]="bar.percent" [style.background]="bar.color"></div>
          </div>
          <div class="bar-meta">
            <strong>{{ bar.value }}</strong>
            <span>{{ bar.label }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="chart-card">
      <div class="chart-header">
        <h3>Taux de validation des PV</h3>
        <span>Validés / soumis</span>
      </div>
      <div class="ring-card">
        <svg class="ring" viewBox="0 0 140 140" width="110" height="110">
          <circle cx="70" cy="70" r="54" class="ring-bg"></circle>
          <circle cx="70" cy="70" r="54" class="ring-progress"
                  [style.strokeDasharray]="ringCircumference"
                  [style.strokeDashoffset]="ringOffset"></circle>
        </svg>
        <div class="ring-center">
          <strong>{{ tauxValidation }}%</strong>
          <span>{{ valides }} validé(s)</span>
        </div>
      </div>
    </div>
  </section>

  <div class="loader-state" *ngIf="loading">
    <span class="spin">⚖️</span>
    <p>Chargement des affaires...</p>
  </div>

  <div class="content-card" *ngIf="!loading">
    <div class="card-header">
      <h2>Mes Affaires <span class="count-badge">{{ affairesFiltrees.length }}</span></h2>
      <div class="search-box">
        <span class="search-icon">🔍</span>
        <input type="text" [(ngModel)]="search" (input)="filtrer()" placeholder="Rechercher par N° affaire, tribunal..."/>
      </div>
    </div>
    <div class="empty-state" *ngIf="affairesFiltrees.length === 0">
      <div class="empty-icon">⚖️</div>
      <p>Aucune affaire trouvée.</p>
    </div>
    <table class="premium-table" *ngIf="affairesFiltrees.length > 0">
      <thead>
        <tr>
          <th>Référence</th><th>Tribunal</th><th>Prochaine audience</th>
          <th>PV</th><th>Facture</th><th>Statut affaire</th>
        </tr>
      </thead>
      <tbody>
        <tr *ngFor="let aff of affairesFiltrees" class="data-row">
          <td>
            <div class="aff-ref">
              <code>#{{ aff.id }}</code>
              <span class="aff-num">{{ aff.numeroAffaire }}</span>
              <span class="aff-date" *ngIf="aff.dateLancement">{{ aff.dateLancement | date:'dd/MM/yyyy' }}</span>
            </div>
          </td>
          <td>
            <div class="court-info" *ngIf="aff.tribunal; else noTribunal">
              <span>{{ aff.tribunal }}</span>
              <small *ngIf="aff.chambre">{{ aff.chambre }}</small>
              <small *ngIf="aff.numeroRole" class="role-num">Rôle : {{ aff.numeroRole }}</small>
            </div>
            <ng-template #noTribunal><span class="empty-val">—</span></ng-template>
          </td>
          <td>
            <span class="audience-date" *ngIf="aff.dateProchainAudience; else noAudience">
              📅 {{ aff.dateProchainAudience | date:'dd/MM/yyyy' }}
            </span>
            <ng-template #noAudience><span class="empty-val">—</span></ng-template>
          </td>
          <td>
            <span *ngIf="aff.pvTexte; else noPV" class="status-pill"
              [ngClass]="{'pill-success':aff.pvStatut==='VALIDE','pill-warning':aff.pvStatut==='EN_ATTENTE','pill-danger':aff.pvStatut==='REFUSE'}">
              {{ aff.pvStatut || 'EN_ATTENTE' }}
            </span>
            <ng-template #noPV><span class="none-badge">Non soumis</span></ng-template>
          </td>
          <td>
            <div *ngIf="aff.factureRef; else noFacture">
              <span class="status-pill"
                [ngClass]="{'pill-success':aff.factureStatut==='PAYEE','pill-warning':aff.factureStatut==='EN_ATTENTE','pill-danger':aff.factureStatut==='REJETEE'}">
                {{ aff.factureStatut || 'EN_ATTENTE' }}
              </span>
              <div class="facture-montant" *ngIf="aff.montantFacture">{{ aff.montantFacture | number:'1.3-3' }} TND</div>
            </div>
            <ng-template #noFacture><span class="none-badge">Non soumise</span></ng-template>
          </td>
          <td>
            <span class="status-pill"
              [ngClass]="{'pill-blue':aff.statut==='EN_COURS','pill-green':aff.statut==='JUGEMENT_RENDU','pill-orange':aff.statut==='EXECUTION_FORCEE','pill-purple':aff.statut==='TRANSACTION','pill-grey':aff.statut==='CLOSE'}">
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
    .page-container { padding: 30px; background: #f0f2f5; min-height: 100vh; font-family: 'Inter', sans-serif; }
    .page-header { margin-bottom: 30px; }
    .role-badge { background: #e0e7ff; color: #4338ca; padding: 4px 10px; border-radius: 6px; font-weight: 800; font-size: 0.7rem; letter-spacing: 1px; display: inline-block; margin-bottom: 8px; }
    h1 { margin: 0; font-size: 2rem; color: #1e293b; font-weight: 800; }
    .subtitle { color: #64748b; margin-top: 6px; font-size: 1rem; }
    .stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 28px; }
    @media (max-width: 1100px) { .stats-row { grid-template-columns: repeat(3, 1fr); } }
    @media (max-width: 700px)  { .stats-row { grid-template-columns: repeat(2, 1fr); } }
    .stat-card { background: white; border-radius: 16px; padding: 20px; display: flex; align-items: center; gap: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.04); border: 1px solid #e2e8f0; transition: transform 0.2s; }
    .stat-card:hover { transform: translateY(-2px); }
    .stat-icon { width: 52px; height: 52px; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 1.6rem; flex-shrink: 0; }
    .blue-icon { background: #e0f2fe; } .indigo-icon { background: #e0e7ff; } .amber-icon { background: #fef3c7; }
    .green-icon { background: #dcfce7; } .teal-icon { background: #ccfbf1; } .purple-icon { background: #f3e8ff; }
    .orange-icon { background: #ffedd5; } .rose-icon { background: #ffe4e6; }
    .stat-info h4 { margin: 2px 0 0; color: #64748b; font-size: 0.78rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
    .stat-value { font-size: 1.6rem; font-weight: 800; color: #1e293b; line-height: 1; }
    .stat-value small { font-size: 0.7rem; color: #94a3b8; font-weight: 600; }
    .payees-hint { font-size: 0.65rem; color: #0d9488; font-weight: 700; display: block; margin-top: 2px; }
    .loader-state { text-align: center; padding: 60px; color: #94a3b8; }
    .spin { font-size: 2.5rem; display: block; animation: spin 1s linear infinite; margin-bottom: 12px; }
    @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

    /* ═══ Charts ═══ */
    .charts-panel { display: grid; grid-template-columns: 1.4fr 1fr; gap: 16px; margin-bottom: 28px; }
    @media (max-width: 900px) { .charts-panel { grid-template-columns: 1fr; } }
    .chart-card { background: white; border-radius: 16px; padding: 22px; border: 1px solid #e2e8f0; box-shadow: 0 2px 10px rgba(0,0,0,0.04); }
    .chart-header { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 18px; }
    .chart-header h3 { margin: 0; font-size: 1rem; color: #1e293b; font-weight: 700; }
    .chart-header span { font-size: 0.75rem; color: #94a3b8; }

    .bars { display: flex; align-items: flex-end; gap: 10px; height: 140px; }
    .bar-item { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; }
    .bar-track { flex: 1; width: 22px; background: #f1f5f9; border-radius: 8px; display: flex; align-items: flex-end; overflow: hidden; }
    .bar-fill { width: 100%; border-radius: 8px 8px 0 0; min-height: 4px; }
    .bar-meta { margin-top: 8px; display: flex; flex-direction: column; align-items: center; }
    .bar-meta strong { font-size: 0.8rem; color: #1e293b; font-weight: 800; }
    .bar-meta span { font-size: 0.6rem; color: #94a3b8; text-align: center; line-height: 1.2; }

    .ring-card { display: flex; align-items: center; justify-content: center; height: 140px; position: relative; }
    .ring { transform: rotate(-90deg); }
    .ring-bg { fill: none; stroke: #f1f5f9; stroke-width: 12; }
    .ring-progress { fill: none; stroke: #4338ca; stroke-width: 12; stroke-linecap: round; }
    .ring-center { position: absolute; display: flex; flex-direction: column; align-items: center; }
    .ring-center strong { font-size: 1.3rem; color: #1e293b; font-weight: 800; }
    .ring-center span { font-size: 0.7rem; color: #94a3b8; }

    .content-card { background: white; border-radius: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); border: 1px solid #e2e8f0; overflow: hidden; }
    .card-header { padding: 22px 28px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; }
    .card-header h2 { margin: 0; font-size: 1.2rem; color: #1e293b; font-weight: 700; display: flex; align-items: center; gap: 10px; }
    .count-badge { background: #e0e7ff; color: #4338ca; font-size: 0.75rem; font-weight: 800; padding: 2px 10px; border-radius: 20px; }
    .search-box { position: relative; display: flex; align-items: center; }
    .search-icon { position: absolute; left: 12px; font-size: 0.9rem; }
    .search-box input { padding: 10px 16px 10px 36px; border: 1.5px solid #e2e8f0; border-radius: 10px; width: 280px; outline: none; font-size: 0.88rem; transition: border 0.2s; }
    .search-box input:focus { border-color: #4338ca; box-shadow: 0 0 0 3px rgba(67,56,202,0.08); }
    .empty-state { text-align: center; padding: 60px; color: #94a3b8; }
    .empty-icon { font-size: 2.5rem; margin-bottom: 12px; }
    .empty-val { color: #cbd5e1; font-size: 0.9rem; }
    .premium-table { width: 100%; border-collapse: collapse; font-size: 0.85rem; }
    .premium-table th { text-align: left; padding: 14px 20px; background: #f8fafc; color: #64748b; font-weight: 700; font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 2px solid #e2e8f0; white-space: nowrap; }
    .premium-table td { padding: 14px 20px; border-bottom: 1px solid #f1f5f9; color: #475569; vertical-align: middle; }
    .data-row:last-child td { border-bottom: none; }
    .data-row:hover { background: #f8fafc; }
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
  `]
})
export class AvocatDashboardComponent implements OnInit, OnDestroy {

  stats: any        = null;
  affaires: any[]   = [];
  affairesFiltrees: any[] = [];
  loading           = true;
  search            = '';

  private readonly ringRadius = 54;
  private destroy$ = new Subject<void>();

  constructor(
    private avocatService: AvocatService,
    private router: Router,
    private notifService: NotificationService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.chargerStats();
    this.chargerAffaires();
  
    this.notifService.dossierCible$.pipe(
      takeUntil(this.destroy$)
    ).subscribe((id: number | null) => {
      if (id !== null) {
        this.router.navigate(['/avocat/affaires'], { queryParams: { dossierId: id } });
      }
    });
  
    this.notifService.nouvelleNotif$.pipe(
      takeUntil(this.destroy$)
    ).subscribe((notif: NotificationDTO) => {
      console.log('🔔 [AvocatDashboard] notif reçue → type:', notif.type);
      this.appliquerNotifEnMemoire(notif);
    });
  }
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  chargerStats() {
    this.avocatService.getDashboard().subscribe({
      next: (s) => this.stats = s,
      error: (e) => console.error('Erreur stats:', e)
    });
  }
  private appliquerNotifEnMemoire(notif: NotificationDTO): void {
    const type = notif.type?.toUpperCase() ?? '';
  
    // Nouvelle affaire assignée → recharger avec retry
    if (type.includes('NOUVELLE_MISSION') || type.includes('NOUVELLE_AFFAIRE')) {
      this.chargerAffairesAvecRetry();
      return;
    }
  
    if (!notif.dossierId) return;
  
    const idx = this.affaires.findIndex(a =>
      a.id === notif.dossierId || a.dossierId === notif.dossierId
    );
  
    if (idx === -1) {
      this.chargerAffairesAvecRetry();
      return;
    }
  
    const affaire = { ...this.affaires[idx] };
  
    if (type.includes('PV')) {
      if (type.includes('VALIDE'))  affaire.pvStatut = 'VALIDE';
      if (type.includes('REFUSE'))  affaire.pvStatut = 'REFUSE';
      if (type.includes('ATTENTE')) affaire.pvStatut = 'EN_ATTENTE';
    }
    if (type.includes('FACTURE')) {
      if (type.includes('PAYEE'))   affaire.factureStatut = 'PAYEE';
      if (type.includes('REJET'))   affaire.factureStatut = 'REJETEE';
      if (type.includes('ATTENTE')) affaire.factureStatut = 'EN_ATTENTE';
    }
    if (type.includes('JUGEMENT')) affaire.statut = 'JUGEMENT_RENDU';
    if (type.includes('CLOS'))     affaire.statut = 'CLOSE';
  
    this.affaires = [
      ...this.affaires.slice(0, idx),
      affaire,
      ...this.affaires.slice(idx + 1)
    ];
    this.filtrer();
  }

  private chargerAffairesAvecRetry(tentative = 0): void {
    const MAX = 5;
    const DELAI_MS = [500, 1000, 2000, 3000, 5000];
  
    this.avocatService.getAffaires().subscribe({
      next: (data: any) => {
        const nouvelles = (data.affaires || []).map((a: any) => ({
          ...a,
          missionStatut:  data.missionStatuts?.[a.id] ?? null,
          missionId:      data.missionIds?.[a.id]     ?? null,
          dossierId:      data.dossierIds?.[a.id]      ?? null,
          pvTexte:        a.pvTexte        ?? null,
          pvStatut:       a.pvStatut       ?? null,
          pvFichiers:     a.pvFichiers     ?? [],
          factureRef:     a.factureRef     ?? null,
          montantFacture: a.montantFacture ?? null,
          factureStatut:  a.factureStatut  ?? null,
        }));
  
        // Si le nombre d'affaires n'a pas changé ET qu'on peut réessayer
        if (nouvelles.length <= this.affaires.length && tentative < MAX) {
          console.log(`🔄 Retry ${tentative + 1}/${MAX} — affaires inchangées`);
          setTimeout(
            () => this.chargerAffairesAvecRetry(tentative + 1),
            DELAI_MS[tentative]
          );
          return;
        }
  
        this.affaires = nouvelles;
        this.filtrer();
        console.log(`✅ Affaires rechargées (${this.affaires.length})`);
      },
      error: (e) => {
        if (tentative < MAX) {
          setTimeout(
            () => this.chargerAffairesAvecRetry(tentative + 1),
            DELAI_MS[tentative]
          );
        } else {
          console.error('❌ Echec rechargement affaires:', e);
        }
      }
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
          dossierId:      data.dossierIds?.[a.id]     ?? null,
          pvTexte:        a.pvTexte        ?? null,
          pvStatut:       a.pvStatut       ?? null,
          pvFichiers:     a.pvFichiers     ?? [],
          factureRef:     a.factureRef     ?? null,
          montantFacture: a.montantFacture ?? null,
          factureStatut:  a.factureStatut  ?? null,
        }));
        this.filtrer();
        this.loading = false;
        console.log('✅ [AvocatDashboard] Affaires rechargées :', this.affaires.length);
      },
      error: (e) => {
        console.error('Erreur affaires:', e);
        this.loading = false;
      }
    });
  }

  get totalAffaires(): number { return this.affaires.length; }
  get affairesEnCours(): number { return this.affaires.filter(a => a.statut === 'EN_COURS').length; }
  get jugementRendu(): number { return this.affaires.filter(a => a.statut === 'JUGEMENT_RENDU').length; }
  get audiencesAVenir(): number { return this.stats?.audiencesAVenir ?? 0; }
  get totalHonoraires(): number { return this.stats?.totalHonoraires ?? 0; }
  get pvEnAttente(): number { return this.affaires.filter(a => a.pvStatut === 'EN_ATTENTE').length; }
  get facturesEnAttente(): number { return this.affaires.filter(a => a.factureStatut === 'EN_ATTENTE').length; }
  get nombreFactures(): number { return this.stats?.nombreFactures ?? this.affaires.filter(a => a.factureRef).length; }
  get facturesPayees(): number { return this.stats?.facturesPayees ?? this.affaires.filter(a => a.factureStatut === 'PAYEE').length; }

  // ═══ Données pour les diagrammes statiques ═══

  /**
   * Répartition basée sur les mêmes métriques que les stat-card du haut.
   * Le Total Honoraires (montant en TND) est exclu : ce n'est pas un
   * effectif comptable, il ne peut pas être comparé sur la même échelle.
   * Les hauteurs sont calculées par rapport au MAX (pas au total), car
   * ces valeurs ne sont pas mutuellement exclusives (ex : totalAffaires
   * inclut déjà affairesEnCours).
   */
  get chartBars(): { key: string; label: string; color: string; value: number; percent: number }[] {
    const items = [
      { key: 'totalAffaires',     label: 'Total affaires',       color: '#2563eb', value: this.totalAffaires },
      { key: 'affairesEnCours',   label: 'En cours',             color: '#4338ca', value: this.affairesEnCours },
      { key: 'audiencesAVenir',   label: 'Audiences à venir',    color: '#d97706', value: this.audiencesAVenir },
      { key: 'nombreFactures',    label: 'Factures soumises',    color: '#0d9488', value: this.nombreFactures },
      { key: 'jugementRendu',     label: 'Jugements rendus',     color: '#7e22ce', value: this.jugementRendu },
      { key: 'pvEnAttente',       label: 'PV en attente',        color: '#ea580c', value: this.pvEnAttente },
      { key: 'facturesEnAttente', label: 'Factures en attente',  color: '#e11d48', value: this.facturesEnAttente },
    ];
    const max = Math.max(...items.map(i => i.value), 1);
    return items.map(i => ({
      ...i,
      percent: Math.round((i.value / max) * 100)
    }));
  }

  get valides(): number {
    return this.affaires.filter(a => a.pvStatut === 'VALIDE').length;
  }

  get totalPvSoumis(): number {
    return this.affaires.filter(a => a.pvTexte).length;
  }

  /** Taux de validation des PV = validés / soumis (0 si aucun PV soumis) */
  get tauxValidation(): number {
    return this.totalPvSoumis > 0
      ? Math.round((this.valides / this.totalPvSoumis) * 100)
      : 0;
  }

  get ringCircumference(): number {
    return 2 * Math.PI * this.ringRadius;
  }

  get ringOffset(): number {
    return this.ringCircumference - (this.tauxValidation / 100) * this.ringCircumference;
  }

  filtrer() {
    const q = this.search.toLowerCase().trim();
    if (!q) { this.affairesFiltrees = [...this.affaires]; return; }
    this.affairesFiltrees = this.affaires.filter(a =>
      a.numeroAffaire?.toLowerCase().includes(q) ||
      a.tribunal?.toLowerCase().includes(q)      ||
      a.statut?.toLowerCase().includes(q)        ||
      a.chambre?.toLowerCase().includes(q)
    );
  }

  naviguer(aff: any, section: string) {
    this.router.navigate(['/avocat/affaires', aff.id, section]);
  }

  statutLabel(s: string): string {
    const m: Record<string, string> = {
      EN_COURS: 'En cours', JUGEMENT_RENDU: 'Jugement rendu',
      EXECUTION_FORCEE: 'Exécution forcée', TRANSACTION: 'Transaction', CLOSE: 'Clôturée'
    };
    return m[s] ?? s;
  }
}