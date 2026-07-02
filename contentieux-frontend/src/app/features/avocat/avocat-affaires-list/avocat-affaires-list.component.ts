import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AvocatService } from '../../../core/services/avocat.service';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-avocat-affaires-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './avocat-affaires-list.component.html',
  styleUrls: ['./avocat-affaires-list.component.scss']
})
export class AvocatAffairesListComponent implements OnInit, OnDestroy {

  // ── Data ────────────────────────────────────────────────────────
  stats: any        = null;
  affaires: any[]   = [];
  filtrees: any[]   = [];
  loading           = true;
  search            = '';
  filtreStatut      = '';

  // ── Stats secondaires ────────────────────────────────────────────
  statuts = [
    { key: '',                 label: 'Toutes',           icon: '📁' },
    { key: 'EN_COURS',         label: 'En cours',         icon: '⚖️' },
    { key: 'JUGEMENT_RENDU',   label: 'Jugement rendu',   icon: '📜' },
    { key: 'EXECUTION_FORCEE', label: 'Exécution forcée', icon: '⚡' },
    { key: 'TRANSACTION',      label: 'Transaction',      icon: '🤝' },
    { key: 'CLOSE',            label: 'Clôturée',         icon: '✅' }
  ];

  // ── Gestion du cycle de vie ───────────────────────────────────────
  private destroy$ = new Subject<void>();

  _cibleDossierId: number | null = null;

  constructor(
    private avocatService: AvocatService,
    private router: Router,
    private route: ActivatedRoute,
    private notifService: NotificationService
  ) {}

  private _surlignerAffaire(dossierId: number): void {
    const affaire = this.affaires.find((a: any) => a.dossierId === dossierId);
    if (!affaire) {
      setTimeout(() => this._surlignerAffaire(dossierId), 300);
      return;
    }
    setTimeout(() => {
      const el = document.getElementById('affaire-' + affaire.id);
      if (!el) return;
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.classList.add('affaire-highlight');
      setTimeout(() => el.classList.remove('affaire-highlight'), 4000);
    }, 300);
  }

  ngOnInit(): void {
    this.chargerStats();
    this.chargerAffaires();

    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const id = params['dossierId'] ? Number(params['dossierId']) : null;
        if (id) this._cibleDossierId = id;
      });

    this.notifService.dossierCible$
      .pipe(takeUntil(this.destroy$))
      .subscribe(id => {
        if (id !== null) {
          this._cibleDossierId = null;
          setTimeout(() => {
            this._cibleDossierId = id;
            this.notifService.signalerDossierCible(null);
            this._surlignerAffaire(id);
          }, 50);
        }
      });

    // ── Rechargement automatique via WebSocket ────────────────────
    const typesAvocat = [
      'NOUVELLE_AFFAIRE',
      'AFFAIRE_REASSIGNEE',
      'NOUVELLE_MISSION',
      'MISSION_MODIFIEE',
      'MISSION_CLOTUREE',
      'MISSION_REJETEE',
      'VALIDATION_FINANCIERE_OK',
      'PV_VALIDE',
      'PV_REFUSE',
      'REJET_FINANCIER'
    ];

    this.notifService.nouvelleNotif$
      .pipe(takeUntil(this.destroy$))
      .subscribe(notif => {
        if (typesAvocat.includes(notif.type)) {
          this.chargerAffairesAvecRetry();
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Chargement ───────────────────────────────────────────────────

  chargerStats(): void {
    this.avocatService.getDashboard().subscribe({
      next: (s) => this.stats = s,
      error: () => this.stats = null
    });
  }

  chargerAffaires(): void {
    this.loading = true;

    this.avocatService.getDashboard().subscribe({
      next: (s) => this.stats = s,
      error: () => this.stats = null
    });

    this.avocatService.getAffaires().subscribe({
      next: (data: any) => {
        this.affaires = (data.affaires || []).map((a: any) => ({
          ...a,
          dossierId:       data.dossierIds?.[a.id]     ?? null,
          pvTexte:         a.pvTexte        ?? null,
          pvStatut:        a.pvStatut       ?? null,
          pvFichiers:      a.pvFichiers     ?? [],
          factureRef:      a.factureRef     ?? null,
          montantFacture:  a.montantFacture ?? null,
          factureStatut:   a.factureStatut  ?? null,
          nombreAudiences: Array.isArray(a.audiences) ? a.audiences.length : 0,
        }));
        this.appliquerFiltres();
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  /**
   * Recharge la liste après une notification WebSocket, avec retry
   * jusqu'à ce que la liste d'IDs change réellement (ajout OU retrait).
   * Comparer les IDs (et pas juste la longueur) est indispensable :
   * une réassignation FAIT DIMINUER la longueur côté ancien avocat,
   * donc une simple comparaison de taille ne détecte jamais ce cas.
   */
  private chargerAffairesAvecRetry(tentative = 0): void {
    const MAX = 5;
    const DELAI_MS = [500, 1000, 2000, 3000, 5000];

    const idsAvant = this.affaires.map(a => a.id).sort().join(',');

    this.avocatService.getAffaires().subscribe({
      next: (data: any) => {
        const nouvelles = (data.affaires || []).map((a: any) => ({
          ...a,
          dossierId:       data.dossierIds?.[a.id]     ?? null,
          pvTexte:         a.pvTexte        ?? null,
          pvStatut:        a.pvStatut       ?? null,
          pvFichiers:      a.pvFichiers     ?? [],
          factureRef:      a.factureRef     ?? null,
          montantFacture:  a.montantFacture ?? null,
          factureStatut:   a.factureStatut  ?? null,
          nombreAudiences: Array.isArray(a.audiences) ? a.audiences.length : 0,
        }));

        const idsApres = nouvelles.map((a: any) => a.id).sort().join(',');
        const aChange = idsApres !== idsAvant;

        if (!aChange && tentative < MAX) {
          console.log(`🔄 [AffairesList] Retry ${tentative + 1}/${MAX} — aucun changement détecté`);
          setTimeout(
            () => this.chargerAffairesAvecRetry(tentative + 1),
            DELAI_MS[tentative]
          );
          return;
        }

        this.affaires = nouvelles;
        this.appliquerFiltres();
        console.log(`✅ [AffairesList] Affaires rechargées : ${this.affaires.length}`);
      },
      error: (e) => {
        if (tentative < MAX) {
          setTimeout(
            () => this.chargerAffairesAvecRetry(tentative + 1),
            DELAI_MS[tentative]
          );
        }
      }
    });
  }

  // ── Filtres ──────────────────────────────────────────────────────

  appliquerFiltres(): void {
    let result = [...this.affaires];

    if (this.filtreStatut) {
      result = result.filter(a => a.statut === this.filtreStatut);
    }

    const q = this.search.toLowerCase().trim();
    if (q) {
      result = result.filter(a =>
        a.numeroAffaire?.toLowerCase().includes(q) ||
        a.tribunal?.toLowerCase().includes(q)      ||
        a.statut?.toLowerCase().includes(q)
      );
    }

    this.filtrees = result;
  }

  setFiltreStatut(key: string): void {
    this.filtreStatut = key;
    this.appliquerFiltres();
  }

  // ── Navigation ───────────────────────────────────────────────────

  gererAudiences(aff: any): void {
    this.router.navigate(['/avocat/affaires', aff.id, 'audiences']);
  }

  gererJugement(aff: any): void {
    this.router.navigate(['/avocat/affaires', aff.id, 'jugement']);
  }

  gererTribunal(aff: any): void {
    this.router.navigate(['/avocat/affaires', aff.id, 'tribunal']);
  }

  gererHonoraires(aff: any): void {
    this.router.navigate(['/avocat/affaires', aff.id, 'honoraires']);
  }

  voirDossier(aff: any): void {
    this.router.navigate(['/avocat/affaires', aff.id, 'dossier']);
  }

  // ── Helpers affichage ────────────────────────────────────────────

  statutLabel(s: string): string {
    const m: Record<string, string> = {
      EN_COURS:         'En cours',
      JUGEMENT_RENDU:   'Jugement rendu',
      EXECUTION_FORCEE: 'Exécution forcée',
      TRANSACTION:      'Transaction',
      CLOSE:            'Clôturée'
    };
    return m[s] ?? s;
  }

  statutClass(s: string): string {
    const m: Record<string, string> = {
      EN_COURS:         'pill--blue',
      JUGEMENT_RENDU:   'pill--green',
      EXECUTION_FORCEE: 'pill--orange',
      TRANSACTION:      'pill--purple',
      CLOSE:            'pill--grey'
    };
    return m[s] ?? 'pill--grey';
  }

  pvStatutClass(s: string): string {
    const m: Record<string, string> = {
      EN_ATTENTE: 'pill--orange',
      VALIDE:     'pill--green',
      REFUSE:     'pill--red'
    };
    return m[s] ?? 'pill--grey';
  }

  factureStatutClass(s: string): string {
    const m: Record<string, string> = {
      EN_ATTENTE: 'pill--orange',
      PAYEE:      'pill--green',
      REJETEE:    'pill--red'
    };
    return m[s] ?? 'pill--grey';
  }

  compterParStatut(key: string): number {
    if (!key) return this.affaires.length;
    return this.affaires.filter(a => a.statut === key).length;
  }

  // ── Getters calculés ─────────────────────────────────────────────

  get totalAffaires(): number {
    return this.affaires.length;
  }

  get affairesEnCours(): number {
    return this.affaires.filter(a => a.statut === 'EN_COURS').length;
  }

  get jugementRendu(): number {
    return this.affaires.filter(a => a.statut === 'JUGEMENT_RENDU').length;
  }

  get totalAudiences(): number {
    return this.affaires.reduce((sum, a) => {
      const nb = a.nombreAudiences ?? a.audiences?.length ?? 0;
      return sum + nb;
    }, 0);
  }

  get totalHonoraires(): number {
    return this.affaires
      .filter(a => a.montantFacture && a.factureStatut !== 'REJETEE')
      .reduce((sum, a) => sum + (Number(a.montantFacture) || 0), 0);
  }

  get totalHonorairesCalcule(): number {
    return this.affaires
      .filter(a => a.montantFacture)
      .reduce((sum, a) => sum + (a.montantFacture || 0), 0);
  }

  get pvEnAttente(): number {
    return this.affaires.filter(a => a.pvStatut === 'EN_ATTENTE').length;
  }

  get facturesEnAttente(): number {
    return this.affaires.filter(a => a.factureStatut === 'EN_ATTENTE').length;
  }

  get audiencesAVenir(): number {
    return this.stats?.audiencesAVenir ?? 0;
  }
}