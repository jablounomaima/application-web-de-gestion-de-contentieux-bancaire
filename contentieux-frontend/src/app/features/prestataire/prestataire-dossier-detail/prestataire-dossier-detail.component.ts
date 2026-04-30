import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { PrestataireService } from '../../../core/services/prestataire.service';

@Component({
  selector: 'app-prestataire-dossier-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './prestataire-dossier-detail.component.html',
  styleUrls: ['./prestataire-dossier-detail.component.scss']
})
export class PrestataireDossierDetailComponent implements OnInit {

  missionId!: number;
  mission: any = null;
  dossier: any = null;

  chargement = true;
  erreur: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private prestataireService: PrestataireService
  ) {}

  ngOnInit(): void {
    this.missionId = Number(this.route.snapshot.paramMap.get('id'));
    this.charger();
  }

  charger(): void {
    this.prestataireService.getDossierMission(this.missionId).subscribe({
      next: (res: any) => {
        this.mission   = res.mission ?? null;
        this.dossier   = res.dossier ?? null;
        this.chargement = false;
      },
      error: (err: any) => {
        this.erreur = err?.error?.error ?? 'Erreur chargement dossier';
        this.chargement = false;
      }
    });
  }

  retour(): void {
    this.router.navigate(['/prestataire/dashboard']);
  }

  nomClient(): string {
    const c = this.dossier?.client;
    if (!c) return '—';
    return [c.nom, c.prenom].filter(Boolean).join(' ') || '—';
  }

  montantTotal(): number {
    return this.dossier?.montant
      ?? this.dossier?.risques?.reduce((s: number, r: any) => s + (r.montantImpaye ?? 0), 0)
      ?? 0;
  }

  statutClass(s: string): string {
    const m: Record<string, string> = {
      OUVERT: 'pill--blue', EN_TRAITEMENT: 'pill--orange',
      VALIDE: 'pill--green', REJETE: 'pill--red',
      CLOS: 'pill--grey', EN_PROCEDURE: 'pill--purple'
    };
    return m[s] ?? 'pill--grey';
  }

  statutLabel(s: string): string {
    const m: Record<string, string> = {
      OUVERT: 'Ouvert', EN_TRAITEMENT: 'En traitement',
      VALIDE: 'Validé', REJETE: 'Rejeté',
      CLOS: 'Clos', EN_PROCEDURE: 'En procédure'
    };
    return m[s] ?? s;
  }

  missionStatutClass(s: string): string {
    const m: Record<string, string> = {
      EN_COURS: 'pill--blue', ASSIGNEE: 'pill--orange',
      PV_SOUMIS: 'pill--purple', FACTURE_SOUMISE: 'pill--green',
      TERMINEE: 'pill--green', ANNULEE: 'pill--grey'
    };
    return m[s] ?? 'pill--grey';
  }

  formatRisqueType(t: string): string {
    const m: Record<string, string> = {
      CREDIT_IMMOBILIER:    '🏠 Crédit Immobilier',
      CREDIT_CONSOMMATION:  '🛍️ Crédit Consommation',
      CREDIT_AUTO:          '🚗 Crédit Auto',
      CREDIT_PROFESSIONNEL: '💼 Crédit Professionnel',
      LEASING:              '📋 Leasing',
      DECOUVERT:            '🏦 Découvert'
    };
    return m[t] ?? t;
  }

  // ── même pattern que l'avocat ──
  risques(): any[] { return this.dossier?.risques ?? []; }
}