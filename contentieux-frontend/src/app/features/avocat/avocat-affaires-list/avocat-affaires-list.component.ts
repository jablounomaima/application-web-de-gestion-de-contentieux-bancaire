import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AvocatService } from '../../../core/services/avocat.service';

@Component({
  selector: 'app-avocat-affaires-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './avocat-affaires-list.component.html',
  styleUrls: ['./avocat-affaires-list.component.scss']
})
export class AvocatAffairesListComponent implements OnInit {

  // ── Data ────────────────────────────────────────────────────────
  stats: any        = null;
  affaires: any[]   = [];
  filtrees: any[]   = [];
  loading           = true;
  search            = '';
  filtreStatut      = '';

  // ── Stats secondaires ────────────────────────────────────────────
  statuts = [
    { key: '',                label: 'Toutes',          icon: '📁' },
    { key: 'EN_COURS',        label: 'En cours',        icon: '⚖️' },
    { key: 'JUGEMENT_RENDU',  label: 'Jugement rendu',  icon: '📜' },
    { key: 'EXECUTION_FORCEE',label: 'Exécution forcée',icon: '⚡' },
    { key: 'TRANSACTION',     label: 'Transaction',     icon: '🤝' },
    { key: 'CLOSE',           label: 'Clôturée',        icon: '✅' }
  ];

  constructor(
    private avocatService: AvocatService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.chargerStats();
    this.chargerAffaires();
  }

  // ── Chargement ───────────────────────────────────────────────────

  chargerStats(): void {
    this.avocatService.getDashboard().subscribe({
      next: (s) => this.stats = s,
      error: () => {}
    });
  }

  chargerAffaires(): void {
    this.loading = true;
    this.avocatService.getAffaires().subscribe({
      next: (data: any) => {
        this.affaires = (data.affaires || []).map((a: any) => ({
          ...a,
          missionStatut: data.missionStatuts?.[a.id] ?? null,
          missionId:     data.missionIds?.[a.id]     ?? null,
        }));
        this.appliquerFiltres();
        this.loading = false;
      },
      error: () => this.loading = false
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

  compterParStatut(key: string): number {
    if (!key) return this.affaires.length;
    return this.affaires.filter(a => a.statut === key).length;
  }

  voirDossier(aff: any): void {
    this.router.navigate(['/avocat/affaires', aff.id, 'dossier']);
  }
}