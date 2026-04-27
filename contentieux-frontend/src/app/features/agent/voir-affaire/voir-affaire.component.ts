import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-voir-affaire',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './voir-affaire.component.html',
  styleUrls: ['./voir-affaire.component.scss']
})
export class VoirAffaireComponent implements OnInit {

  dossierId!: number;

  affaire: any = null;
  dossier: any = null;
  mission: any = null;

  chargement = true;
  erreur: string | null = null;
  pasDAffaire = false;

  private apiUrl = `${environment.apiUrl}/api/agent/dossiers`;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.dossierId = Number(this.route.snapshot.paramMap.get('dossierId'));
    this.charger();
  }

  /** GET /api/agent/dossiers/{dossierId}/affaire */
  charger(): void {
    this.http.get<any>(`${this.apiUrl}/${this.dossierId}/affaire`).subscribe({
      next: (res) => {
        if (res.pasDAffaire) {
          this.pasDAffaire = true;
        } else {
          this.affaire = res.affaire;
        }
        this.chargement = false;
      },
      error: (err) => {
        this.erreur = err?.error?.error ?? 'Impossible de charger l\'affaire.';
        this.chargement = false;
      }
    });
  }

  retour(): void {
    this.router.navigate(['/agent/dossiers', this.dossierId]);
  }

  // ── Helpers statut affaire ────────────────────────────────────────

  statutClass(statut: string): string {
    const map: Record<string, string> = {
      'EN_COURS':         'badge--blue',
      'JUGEMENT_RENDU':   'badge--green',
      'EXECUTION_FORCEE': 'badge--orange',
      'TRANSACTION':      'badge--purple',
      'CLOSE':            'badge--grey'
    };
    return map[statut] ?? 'badge--grey';
  }

  statutLabel(statut: string): string {
    const map: Record<string, string> = {
      'EN_COURS':         'En cours',
      'JUGEMENT_RENDU':   'Jugement rendu',
      'EXECUTION_FORCEE': 'Exécution forcée',
      'TRANSACTION':      'Transaction',
      'CLOSE':            'Clôturée'
    };
    return map[statut] ?? statut;
  }

  // ── Helpers jugement ──────────────────────────────────────────────

  jugementClass(type: string): string {
    const map: Record<string, string> = {
      'CONDAMNATION': 'badge--red',
      'REJET':        'badge--grey',
      'PARTIEL':      'badge--orange',
      'MIXTE':        'badge--purple',
    };
    return map[type] ?? 'badge--grey';
  }

  jugementLabel(type: string): string {
    const map: Record<string, string> = {
      'CONDAMNATION': '⚖️ Condamnation',
      'REJET':        '❌ Rejet',
      'PARTIEL':      '⚠️ Partiel',
      'MIXTE':        '🔀 Mixte',
    };
    return map[type] ?? type;
  }

  // ── Helpers audience ──────────────────────────────────────────────

  audienceStatutClass(s: string): string {
    const map: Record<string, string> = {
      'PLANIFIEE': 'badge--blue',
      'TENUE':     'badge--green',
      'RENVOYEE':  'badge--orange',
      'ANNULEE':   'badge--red'
    };
    return map[s] ?? 'badge--grey';
  }

  audienceStatutLabel(s: string): string {
    const map: Record<string, string> = {
      'PLANIFIEE': 'Planifiée',
      'TENUE':     'Tenue',
      'RENVOYEE':  'Renvoyée',
      'ANNULEE':   'Annulée'
    };
    return map[s] ?? s;
  }

  audienceEmoji(statut: string): string {
    const map: Record<string, string> = {
      'PLANIFIEE': '📅',
      'TENUE':     '✅',
      'RENVOYEE':  '🔄',
      'ANNULEE':   '❌',
    };
    return map[statut] ?? '⚖️';
  }

  // ── Helpers booléens ──────────────────────────────────────────────

  hasJugement(): boolean {
    return !!this.affaire?.typeJugement;
  }

  hasTribunal(): boolean {
    return !!(this.affaire?.tribunal || this.affaire?.chambre || this.affaire?.numeroRole);
  }

  audiences(): any[] {
    return this.affaire?.audiences ?? [];
  }

  countAudience(statut: string): number {
    return this.audiences().filter(a => a.statut === statut).length;
  }

  // ── Helpers avocat ────────────────────────────────────────────────

  initiales(avocat: any): string {
    if (!avocat) return 'AV';
    const parts = [avocat.prenom, avocat.nom].filter(Boolean);
    return parts.map((p: string) => p[0]?.toUpperCase()).join('') || 'AV';
  }
}