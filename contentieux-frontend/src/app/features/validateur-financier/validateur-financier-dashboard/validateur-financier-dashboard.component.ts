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

  private destroy$ = new Subject<void>();

  constructor(
    private validateurService: ValidateurService,
    private route:             ActivatedRoute,
    private notifService:      NotificationService
  ) {}

  ngOnInit(): void {
    this.loading = true;

    // Premier chargement via queryParams
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const id = params['dossierId'] ? Number(params['dossierId']) : null;
        if (id) this.dossierId = id;
      });

    // Navigation depuis notification (même URL)
    this.notifService.dossierCible$
      .pipe(takeUntil(this.destroy$))
      .subscribe(id => {
        if (id !== null) {
          this.dossierId = null;
          setTimeout(() => {
            this.dossierId = id;
            this.notifService.signalerDossierCible(null);
          }, 50);
        }
      });

    // Rechargement temps réel — nouveau dossier à valider
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
    this.rejetes  = dossiers.filter(d => d.validationFinanciere === false
                                      && d.statut !== 'EN_TRAITEMENT').length;
    this.loading  = false;
    if (this.dossierId) {
      const dossier = dossiers.find(d => Number(d.id) === Number(this.dossierId));
      if (dossier) this.dossierSelectionne = dossier;
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
      ? this.validateurService.validerFinancier(this.dossierSelectionne.id, this.commentaire)
      : this.validateurService.rejeterFinancier(this.dossierSelectionne.id, this.commentaire);

    obs.subscribe({
      next: () => {
        this.soumission         = false;
        this.dossierSelectionne = null;
        this.recharger          = !this.recharger;
      },
      error: (err: any) => {
        this.erreur     = err.error?.error ?? 'Erreur lors de l\'opération.';
        this.soumission = false;
      }
    });
  }
}