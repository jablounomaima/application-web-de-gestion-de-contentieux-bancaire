import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AvocatService } from '../../../core/services/avocat.service';

@Component({
  selector: 'app-avocat-audiences',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './avocat-audiences.component.html',
  styleUrls: ['./avocat-audiences.component.scss']
})
export class AvocatAudiencesComponent implements OnInit {

  // ── Données ────────────────────────────────────────────────────
  affaireId!: number;
  affaire: any = null;
  audiences: any[] = [];
  audiencesFiltrees: any[] = [];

  // ── États ──────────────────────────────────────────────────────
  loading = true;
  submitting = false;
  showForm = false;
  modeEdition = false;
  audienceEnEdition: any = null;
  audienceASupprimer: any = null;
  filtreStatut: string | null = null;

  // ── Messages ───────────────────────────────────────────────────
  successMsg = '';
  errorMsg = '';
  formError = '';

  // ── Date du jour ───────────────────────────────────────────────
  today: string = new Date().toISOString().split('T')[0];

  // ── Options statut (alignées avec l'enum Java StatutAudience) ──
  statutOptions = [
    { value: 'PLANIFIEE', label: 'Planifiée' },
    { value: 'TENUE',     label: 'Tenue'     },
    { value: 'RENVOYEE',  label: 'Renvoyée'  },
    { value: 'ANNULEE',   label: 'Annulée'   },
  ];

  // ── Formulaire ─────────────────────────────────────────────────
  form = {
    dateAudience:      '',
    heure:             '',
    salle:             '',
    motif:             '',
    statut:            'PLANIFIEE',
    resultat:          '',
    prochaineAudience: ''
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private avocatService: AvocatService
  ) {}

  // ══════════════════════════════════════════════════════════════
  // INIT — appels parallèles pour minimiser le temps de chargement
  // ══════════════════════════════════════════════════════════════

  ngOnInit(): void {
    this.affaireId = Number(this.route.snapshot.paramMap.get('affaireId'));
    if (this.affaireId) {
      this.chargerInfosAffaire();
      this.chargerAudiences();
    } else {
      this.loading = false;
    }
  }

  // ══════════════════════════════════════════════════════════════
  // CHARGEMENT
  // ══════════════════════════════════════════════════════════════

  /** Charge uniquement les métadonnées de l'affaire (tribunal, ref…) */
  chargerInfosAffaire(): void {
    this.avocatService.getAffaireDetail(this.affaireId).subscribe({
      next: (data: any) => this.affaire = data,
      error: () => { /* non bloquant */ }
    });
  }

  /** Charge la liste des audiences depuis GET /{affaireId}/audiences */
  chargerAudiences(): void {
    this.loading = true;
    this.avocatService.getAudiences(this.affaireId).subscribe({
      next: (data: any) => {
        this.audiences = data.audiences || [];
        this.appliquerFiltre();
        this.loading = false;
      },
      error: () => {
        this.errorMsg = 'Impossible de charger les audiences.';
        this.loading  = false;
      }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // FILTRAGE
  // ══════════════════════════════════════════════════════════════

  filtrerParStatut(statut: string | null): void {
    this.filtreStatut = statut;
    this.appliquerFiltre();
  }

  appliquerFiltre(): void {
    const base = this.filtreStatut
      ? this.audiences.filter(a => a.statut === this.filtreStatut)
      : [...this.audiences];

    this.audiencesFiltrees = base.sort(
      (a, b) => new Date(b.dateAudience).getTime() - new Date(a.dateAudience).getTime()
    );
  }

  countByStatut(statut: string): number {
    return this.audiences.filter(a => a.statut === statut).length;
  }

  // ══════════════════════════════════════════════════════════════
  // STATS
  // ══════════════════════════════════════════════════════════════

// statsAudiences
get statsAudiences() {
    return [
      { label: 'Planifiées', value: 'PLANIFIEE', color: '#3b82f6', count: this.countByStatut('PLANIFIEE') },
      { label: 'Tenues',     value: 'TENUE',     color: '#16a34a', count: this.countByStatut('TENUE')     },
      { label: 'Renvoyées',  value: 'RENVOYEE',  color: '#d97706', count: this.countByStatut('RENVOYEE')  },
      { label: 'Annulées',   value: 'ANNULEE',   color: '#dc2626', count: this.countByStatut('ANNULEE')   },
    ];
  }

  // ══════════════════════════════════════════════════════════════
  // FORMULAIRE
  // ══════════════════════════════════════════════════════════════

  ouvrirFormulaire(): void {
    this.modeEdition       = false;
    this.audienceEnEdition = null;
    this.resetForm();
    this.showForm  = true;
    this.formError = '';
  }

  ouvrirEdition(aud: any): void {
    this.modeEdition       = true;
    this.audienceEnEdition = aud;
    this.form = {
      dateAudience:      aud.dateAudience      || '',
      heure:             aud.heure             || '',
      salle:             aud.salle             || '',
      motif:             aud.motif             || '',
      statut:            aud.statut            || 'PROGRAMMEE',
      resultat:          aud.resultat          || '',
      prochaineAudience: aud.prochaineAudience || ''
    };
    this.showForm  = true;
    this.formError = '';
  }

  fermerFormulaire(): void {
    this.showForm  = false;
    this.formError = '';
    this.resetForm();
  }

  resetForm(): void {
    this.form = {
      dateAudience:      '',
      heure:             '',
      salle:             '',
      motif:             '',
      statut:            'PLANIFIEE',
      resultat:          '',
      prochaineAudience: ''
    };
  }

  // ══════════════════════════════════════════════════════════════
  // SOUMETTRE — mise à jour locale instantanée (zéro re-fetch)
  // ══════════════════════════════════════════════════════════════

  soumettre(): void {
    this.formError = '';

    if (!this.form.dateAudience) {
      this.formError = 'La date de l\'audience est obligatoire.';
      return;
    }
    if (!this.form.motif?.trim()) {
      this.formError = 'Le motif est obligatoire.';
      return;
    }

    const body: any = {
      dateAudience:      this.form.dateAudience,
      heure:             this.form.heure            || null,
      salle:             this.form.salle            || null,
      motif:             this.form.motif.trim(),
      statut:            this.form.statut,
      resultat:          this.form.resultat?.trim() || null,
      prochaineAudience: this.form.prochaineAudience || null,
    };

    this.submitting = true;

    if (this.modeEdition && this.audienceEnEdition) {
      // ── Modification ──────────────────────────────────────────
      this.avocatService
        .modifierAudience(this.affaireId, this.audienceEnEdition.id, body)
        .subscribe({
          next: () => {
            // Mise à jour locale : remplacer l'audience modifiée dans le tableau
            const updated = this.audiences.map(a =>
              a.id === this.audienceEnEdition.id ? { ...a, ...body } : a
            );
            this.onSuccess('Audience modifiée avec succès.', updated);
          },
          error: (e) => this.onError(e)
        });

    } else {
      // ── Ajout ─────────────────────────────────────────────────
      this.avocatService
        .ajouterAudience(this.affaireId, body)
        .subscribe({
          next: (res: any) => {
            // Priorité à l'objet retourné par le backend (avec vrai ID)
            // Fallback : objet local avec ID temporaire
            const nouvelleAudience = res?.audience ?? { ...body, id: Date.now() };
            this.onSuccess('Audience ajoutée avec succès.', [...this.audiences, nouvelleAudience]);
          },
          error: (e) => this.onError(e)
        });
    }
  }

  // ══════════════════════════════════════════════════════════════
  // SUPPRESSION — mise à jour locale instantanée
  // ══════════════════════════════════════════════════════════════

  confirmerSuppression(aud: any): void {
    this.audienceASupprimer = aud;
  }

  annulerSuppression(): void {
    this.audienceASupprimer = null;
  }

  supprimerAudience(): void {
    if (!this.audienceASupprimer) return;
    this.submitting = true;
    const idASupprimer = this.audienceASupprimer.id;

    this.avocatService
      .supprimerAudience(this.affaireId, idASupprimer)
      .subscribe({
        next: () => {
          // Retirer localement sans re-fetch
          const updated = this.audiences.filter(a => a.id !== idASupprimer);
          this.audienceASupprimer = null;
          this.onSuccess('Audience supprimée avec succès.', updated);
        },
        error: (e) => {
          this.audienceASupprimer = null;
          this.onError(e);
        }
      });
  }

  // ══════════════════════════════════════════════════════════════
  // CALLBACKS
  // ══════════════════════════════════════════════════════════════

  private onSuccess(msg: string, updatedAudiences?: any[]): void {
    this.submitting = false;
    this.showForm   = false;
    this.successMsg = msg;

    if (updatedAudiences !== undefined) {
      // Mise à jour locale instantanée — aucun appel réseau supplémentaire
      this.audiences = updatedAudiences;
      this.appliquerFiltre();
    } else {
      // Fallback : re-fetch si pas de données locales fournies
      this.chargerAudiences();
    }

    setTimeout(() => this.successMsg = '', 4000);
  }

  private onError(e: any): void {
    this.submitting = false;
    const msg = e?.error?.error || e?.error?.message || 'Une erreur est survenue.';
    if (this.showForm) {
      this.formError = msg;
    } else {
      this.errorMsg = msg;
      setTimeout(() => this.errorMsg = '', 5000);
    }
  }

  // ══════════════════════════════════════════════════════════════
  // HELPERS
  // ══════════════════════════════════════════════════════════════

  retour(): void {
    this.router.navigate(['/avocat/affaires']);
  }

  trackById(_: number, item: any): number {
    return item.id;
  }

  getStatutLabel(statut: string): string {
    return this.statutOptions.find(s => s.value === statut)?.label || statut;
  }

  // getStatutEmoji
getStatutEmoji(statut: string): string {
    const map: Record<string, string> = {
      PLANIFIEE: '📅',
      TENUE:     '✅',
      RENVOYEE:  '🔄',
      ANNULEE:   '❌',
    };
    return map[statut] || '⚖️';
  }
}