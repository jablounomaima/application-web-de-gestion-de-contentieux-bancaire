import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ValidateurService } from '../../../core/services/validateur.service';
import { ValidateurFinancierListeComponent } from '../validateur-financier-liste/validateur-financier-liste.component';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-validateur-financier-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, ValidateurFinancierListeComponent],
  templateUrl: './validateur-financier-dashboard.component.html',
  styleUrls: ['./validateur-financier-dashboard.component.scss']
})
export class ValidateurFinancierDashboardComponent implements OnInit, OnDestroy {

  dossiers:           any[]                 = [];
  loading             = true;
  valides             = 0;
  rejetes             = 0;
  dossierSelectionne: any                   = null;
  actionType:         'valider' | 'rejeter' = 'valider';
  commentaire         = '';
  erreur              = '';
  soumission          = false;
  recharger           = false;
  dossierId:          number | null         = null;

  // ✅ Surlignage — même principe que admin-dashboard
  highlightedDossierId: number | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private validateurService: ValidateurService,
    private route:             ActivatedRoute,
    private notifService:      NotificationService
  ) {}

  ngOnInit(): void {
    this.loading = true;

    // ── Query params — dossierId depuis URL ───────────────
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const id = params['dossierId'] ? Number(params['dossierId']) : null;
        if (id) {
          this.dossierId          = id;
          this.highlightedDossierId = id;  // ✅ surligner
        }
      });

    // ── Navigation depuis notification (même URL) ─────────
    this.notifService.dossierCible$
      .pipe(takeUntil(this.destroy$))
      .subscribe(id => {
        if (id !== null) {
          this.dossierId          = null;
          this.highlightedDossierId = null;
          setTimeout(() => {
            this.dossierId          = id;
            this.highlightedDossierId = id;  // ✅ surligner
            this.notifService.signalerDossierCible(null);
          }, 50);
        }
      });

    // ── Rechargement temps réel ───────────────────────────
    this.notifService.nouvelleNotif$
      .pipe(takeUntil(this.destroy$))
      .subscribe(notif => {
        if (notif.type === 'VALIDATION_FINANCIERE') {
          this.recharger = !this.recharger;
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  mettreAJourStats(dossiers: any[]): void {
    this.dossiers = dossiers;
    this.valides  = dossiers.filter(d => d.validationFinanciere === true).length;
    this.rejetes  = dossiers.filter(
      d => d.validationFinanciere === false && d.statut !== 'EN_TRAITEMENT'
    ).length;
    this.loading  = false;

    // ✅ Sélectionner + surligner le dossier cible
    if (this.dossierId) {
      const dossier = dossiers.find(
        d => Number(d.id) === Number(this.dossierId)
      );
      if (dossier) {
        this.dossierSelectionne   = null; // ne pas ouvrir le modal auto
        this.highlightedDossierId = dossier.id;

        // ✅ Scroll vers la ligne après rendu
        setTimeout(() => {
          const el = document.getElementById('dossier-row-' + dossier.id);
          el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }, 300);
      }
    }
  }

  getNbEnAttente(): number {
    return this.dossiers.filter(d =>
      d.validationFinanciere !== true && d.validationFinanciere !== false
    ).length;
  }

  ouvrirAction(event: { dossier: any; type: 'valider' | 'rejeter' }): void {
    this.dossierSelectionne = event.dossier;
    this.actionType         = event.type;
    this.commentaire        = '';
    this.erreur             = '';
  }

  fermerModal(): void {
    if (!this.soumission) this.dossierSelectionne = null;
  }

  confirmerAction(): void {
    if (this.actionType === 'rejeter' && !this.commentaire.trim()) {
      this.erreur = 'Le motif de rejet est obligatoire.';
      return;
    }

    this.soumission = true;
    this.erreur     = '';

    const obs = this.actionType === 'valider'
      ? this.validateurService.validerFinancier(
          this.dossierSelectionne.id, this.commentaire)
      : this.validateurService.rejeterFinancier(
          this.dossierSelectionne.id, this.commentaire);

    obs.subscribe({
      next: () => {
        this.soumission           = false;
        this.dossierSelectionne   = null;
        this.highlightedDossierId = null;  // ✅ enlever surlignage après action
        this.recharger            = !this.recharger;
      },
      error: (err: any) => {
        this.erreur     = err.error?.error ?? 'Erreur lors de l\'opération.';
        this.soumission = false;
      }
    });
  }



  get chartBars() {
    const total = this.getNbEnAttente() + this.valides + this.rejetes || 1;
    return [
      { label: 'En attente', value: this.getNbEnAttente(), percent: (this.getNbEnAttente() / total) * 100, color: '#ffb300' },
      { label: 'Validés',    value: this.valides,           percent: (this.valides / total) * 100,           color: '#43a047' },
      { label: 'Rejetés',    value: this.rejetes,            percent: (this.rejetes / total) * 100,            color: '#e53935' }
    ];
  }
  
  get tauxValidation(): number {
    const total = this.getNbEnAttente() + this.valides + this.rejetes;
    return total ? Math.round((this.valides / total) * 100) : 0;
  }
  
  private readonly ringRadius = 54;
  
  get ringCircumference(): number {
    return 2 * Math.PI * this.ringRadius;
  }
  
  get ringOffset(): number {
    const circ = this.ringCircumference;
    return circ - (this.tauxValidation / 100) * circ;
  }
}

