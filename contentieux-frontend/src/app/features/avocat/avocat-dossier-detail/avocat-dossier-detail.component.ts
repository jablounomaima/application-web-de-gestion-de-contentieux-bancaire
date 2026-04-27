import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AvocatService } from '../../../core/services/avocat.service';

@Component({
  selector: 'app-avocat-dossier-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './avocat-dossier-detail.component.html',
  styleUrls: ['./avocat-dossier-detail.component.scss']
})
export class AvocatDossierDetailComponent implements OnInit {

  affaireId!: number;

  affaire: any  = null;
  dossier: any  = null;
  mission: any  = null;

  chargement = true;
  erreur: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private avocatService: AvocatService
  ) {}

  ngOnInit(): void {
    this.affaireId = Number(this.route.snapshot.paramMap.get('affaireId'));
    this.charger();
  }

  /** GET /api/avocat/affaires/{affaireId}/dossier */
  charger(): void {
    this.avocatService.getDossierAffaire(this.affaireId).subscribe({
      next: (res: any) => {
        this.affaire = res.affaire;
        this.dossier = res.dossier;
        this.mission = res.mission;
        this.chargement = false;
      },
      error: (err: any) => {
        this.erreur = err?.error?.error ?? 'Impossible de charger le dossier.';
        this.chargement = false;
      }
    });
  }

  retour(): void {
    this.router.navigate(['/avocat/affaires']);
  }

  // ── Helpers ──────────────────────────────────────────────────────

  nomClient(): string {
    const c = this.dossier?.client;
    if (!c) return '—';
    if (c.typeClient === 'ENTREPRISE') return c.raisonSociale || '—';
    return [c.nom, c.prenom].filter(Boolean).join(' ') || '—';
  }

  montantTotal(): number {
    return this.dossier?.montantTotalEngagement
      ?? this.dossier?.risques?.reduce((s: number, r: any) => s + (r.montantImpaye ?? 0), 0)
      ?? 0;
  }

  statutClass(s: string): string {
    const m: Record<string, string> = {
      OUVERT:        'pill--blue',
      EN_TRAITEMENT: 'pill--orange',
      VALIDE:        'pill--green',
      REJETE:        'pill--red',
      CLOS:          'pill--grey',
      EN_PROCEDURE:  'pill--purple'
    };
    return m[s] ?? 'pill--grey';
  }

  statutLabel(s: string): string {
    const m: Record<string, string> = {
      OUVERT:        'Ouvert',
      EN_TRAITEMENT: 'En traitement',
      VALIDE:        'Validé',
      REJETE:        'Rejeté',
      CLOS:          'Clos',
      EN_PROCEDURE:  'En procédure'
    };
    return m[s] ?? s;
  }

  validIcon(v: boolean | null): string {
    if (v === true)  return '✅';
    if (v === false) return '❌';
    return '⏳';
  }

  formatRisqueType(t: string): string {
    const m: Record<string, string> = {
      CREDIT_IMMOBILIER:   '🏠 Crédit Immobilier',
      CREDIT_CONSOMMATION: '🛍️ Crédit Consommation',
      CREDIT_AUTO:         '🚗 Crédit Auto',
      CREDIT_PROFESSIONNEL:'💼 Crédit Professionnel',
      LEASING:             '📋 Leasing',
      DECOUVERT:           '🏦 Découvert'
    };
    return m[t] ?? t;
  }

  missionStatutClass(s: string): string {
    const m: Record<string, string> = {
      EN_COURS:        'pill--blue',
      PV_SOUMIS:       'pill--orange',
      FACTURE_SOUMISE: 'pill--purple',
      TERMINEE:        'pill--green',
      ANNULEE:         'pill--grey'
    };
    return m[s] ?? 'pill--grey';
  }

  risques(): any[] { return this.dossier?.risques ?? []; }
  historique(): any[] { return this.dossier?.historique ?? []; }
}