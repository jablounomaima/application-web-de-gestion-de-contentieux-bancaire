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
  affaires: any[] = [];
  chargement = true;
  erreur: string | null = null;
  pasDAffaire = false;

  savingPV = false;
  savingFacture = false;

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

  charger(): void {
    this.http.get<any>(`${this.apiUrl}/${this.dossierId}/affaire`).subscribe({
      next: (res) => {
        if (res.pasDAffaire) {
          this.pasDAffaire = true;
          this.affaires = [];
        } else {
          // Support both: array of affaires or single affaire (backward compat)
          this.affaires = res.affaires ?? (res.affaire ? [res.affaire] : []);
          this.pasDAffaire = this.affaires.length === 0;
        }
        this.chargement = false;
      },
      error: (err) => {
        this.erreur = err?.error?.error ?? 'Impossible de charger les affaires.';
        this.chargement = false;
      }
    });
  }

  retour(): void {
    this.router.navigate(['/agent/dossiers', this.dossierId]);
  }

  // ── PV helpers ──────────────────────────────────────────────────

  pvStatutClassFor(aff: any): string {
    const map: Record<string, string> = {
      'EN_ATTENTE': 'badge--orange',
      'VALIDE':     'badge--green',
      'REFUSE':     'badge--red'
    };
    return map[aff?.pvStatut] ?? 'badge--grey';
  }

  pvStatutLabelFor(aff: any): string {
    const map: Record<string, string> = {
      'EN_ATTENTE': '⏳ En attente',
      'VALIDE':     '✅ Validé',
      'REFUSE':     '❌ Refusé'
    };
    return map[aff?.pvStatut] ?? aff?.pvStatut ?? '—';
  }

  validerPV(aff: any, accepte: boolean): void {
    const label = accepte ? 'accepter' : 'refuser';
    const avocatNom = aff.avocat ? `${aff.avocat.prenom} ${aff.avocat.nom}` : 'cet avocat';
    if (!confirm(`Voulez-vous ${label} le PV soumis par ${avocatNom} ?`)) return;

    this.savingPV = true;
    this.http.post<any>(`${this.apiUrl}/${this.dossierId}/affaire/${aff.id}/pv/valider`, { accepte }).subscribe({
      next: (res) => {
        alert(res.message);
        this.savingPV = false;
        this.charger();
      },
      error: (err) => {
        alert(err?.error?.error || 'Erreur lors de la validation du PV');
        this.savingPV = false;
      }
    });
  }

  // ── Facture helpers ─────────────────────────────────────────────

  factureStatutClassFor(aff: any): string {
    const map: Record<string, string> = {
      'EN_ATTENTE':            'badge--orange',
      'EN_ATTENTE_VALIDATION': 'badge--orange',
      'PAYEE':                 'badge--green',
      'REJETEE':               'badge--red'
    };
    return map[aff?.factureStatut] ?? 'badge--grey';
  }

  factureStatutLabelFor(aff: any): string {
    const map: Record<string, string> = {
      'EN_ATTENTE':            '⏳ En attente',
      'EN_ATTENTE_VALIDATION': '🟡 En attente du validateur financier',
      'PAYEE':                 '✅ Payée',
      'REJETEE':               '❌ Rejetée'
    };
    return map[aff?.factureStatut] ?? aff?.factureStatut ?? '—';
  }

  ttcFor(aff: any): number {
    return (aff?.montantFacture ?? 0) * 1.19;
  }

  // La validation facture est désormais gérée par le VALIDATEUR FINANCIER
  // Aucune action n'est nécessaire ici pour l'agent

  // ── Fichiers ──────────────────────────────────────────────────────

  getFileIcon(nom: string): string {
    const ext = nom?.split('.').pop()?.toLowerCase();
    const icons: Record<string, string> = {
      pdf: '📄', doc: '📝', docx: '📝',
      jpg: '🖼️', jpeg: '🖼️', png: '🖼️',
      xls: '📊', xlsx: '📊', txt: '📃'
    };
    return icons[ext ?? ''] ?? '📎';
  }

  downloadUrl(fichier: any): string {
    return `data:${fichier.typeMime};base64,${fichier.base64}`;
  }

  // ── Jugement ──────────────────────────────────────────────────────

  hasJugement(aff: any): boolean {
    return !!aff?.typeJugement;
  }

  jugementClass(type: string): string {
    const map: Record<string, string> = {
      'CONDAMNATION': 'badge--red',
      'REJET':        'badge--grey',
      'PARTIEL':      'badge--orange',
      'MIXTE':        'badge--purple'
    };
    return map[type] ?? 'badge--grey';
  }

  jugementLabel(type: string): string {
    const map: Record<string, string> = {
      'CONDAMNATION': '⚖️ Condamnation',
      'REJET':        '❌ Rejet',
      'PARTIEL':      '⚠️ Partiel',
      'MIXTE':        '🔀 Mixte'
    };
    return map[type] ?? type;
  }

  // ── Tribunal ──────────────────────────────────────────────────────

  hasTribunal(aff: any): boolean {
    return !!(aff?.tribunal || aff?.chambre || aff?.numeroRole);
  }

  // ── Audiences ─────────────────────────────────────────────────────

  getAudiences(aff: any): any[] {
    return aff?.audiences ?? [];
  }

  countAudienceFor(aff: any, statut: string): number {
    return this.getAudiences(aff).filter(a => a.statut === statut).length;
  }

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
      'ANNULEE':   '❌'
    };
    return map[statut] ?? '⚖️';
  }

  // ── Avocat ────────────────────────────────────────────────────────

  initiales(avocat: any): string {
    if (!avocat) return 'AV';
    const parts = [avocat.prenom, avocat.nom].filter(Boolean);
    return parts.map((p: string) => p[0]?.toUpperCase()).join('') || 'AV';
  }

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
}