import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-mission-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mission-detail.component.html',
  styleUrls: ['./mission-detail.component.scss']
})
export class MissionDetailComponent implements OnInit {

  private apiUrl = `${environment.apiUrl}/api/prestataire/missions`;
  private agentUrl = `${environment.apiUrl}/api/agent/missions`;

  missionId!: number;

  // ── Données ────────────────────────────────────────────────────
  mission:          any = null;
  resultats:        any[] = [];
  historique:       any[] = [];
  affaire:          any = null;
  resultatVerrouille = false;
  isAgent           = false;

  // ── États ──────────────────────────────────────────────────────
  loading    = true;
  submitting = false;

  // ── Modals ─────────────────────────────────────────────────────
  showModalValidation = false;
  showModalRejet      = false;
  commentaireValidation = '';
  commentaireRejet      = '';

  // ── Messages ───────────────────────────────────────────────────
  successMsg = '';
  errorMsg   = '';

  // ── Onglet actif ───────────────────────────────────────────────
  onglet: 'detail' | 'resultats' | 'historique' | 'affaire' = 'detail';

  constructor(
    private route:  ActivatedRoute,
    private router: Router,
    private http:   HttpClient
  ) {}

  ngOnInit(): void {
    this.missionId = Number(this.route.snapshot.paramMap.get('id'));
    this.charger();
  }

  // ══════════════════════════════════════════════════════════════
  // CHARGEMENT
  // ══════════════════════════════════════════════════════════════

  charger(): void {
    this.loading = true;
    this.http.get<any>(`${this.apiUrl}/${this.missionId}`).subscribe({
      next: (res) => {
        this.mission           = res.mission;
        this.resultats         = res.resultats         || [];
        this.historique        = res.historique        || [];
        this.affaire           = res.affaire           || null;
        this.resultatVerrouille = res.resultatVerrouille || false;
        this.isAgent           = res.isAgent           || false;
        this.loading           = false;
      },
      error: (err) => {
        this.errorMsg = err?.error?.error || 'Impossible de charger la mission.';
        this.loading  = false;
      }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // VALIDATION
  // ══════════════════════════════════════════════════════════════

  validerMission(): void {
    this.submitting = true;
    this.http.post<any>(`${this.agentUrl}/${this.missionId}/valider`,
      { commentaire: this.commentaireValidation }
    ).subscribe({
      next: () => {
        this.submitting           = false;
        this.showModalValidation  = false;
        this.mission.statut       = 'VALIDEE_AGENT';
        this.afficherSucces('Mission validée avec succès.');
      },
      error: (err) => {
        this.submitting = false;
        this.errorMsg   = err?.error?.error || 'Erreur lors de la validation.';
        setTimeout(() => this.errorMsg = '', 4000);
      }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // REJET
  // ══════════════════════════════════════════════════════════════

  rejeterMission(): void {
    if (!this.commentaireRejet.trim()) return;
    this.submitting = true;
    this.http.post<any>(`${this.agentUrl}/${this.missionId}/rejeter`,
      { commentaire: this.commentaireRejet }
    ).subscribe({
      next: () => {
        this.submitting   = false;
        this.showModalRejet = false;
        this.mission.statut = 'REJETEE';
        this.afficherSucces('Mission rejetée.');
      },
      error: (err) => {
        this.submitting = false;
        this.errorMsg   = err?.error?.error || 'Erreur lors du rejet.';
        setTimeout(() => this.errorMsg = '', 4000);
      }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // TÉLÉCHARGEMENT FICHIER
  // ══════════════════════════════════════════════════════════════

  telechargerFichier(fichierId: number, nomOriginal: string): void {
    this.http.get(
      `${environment.apiUrl}/api/prestataire/missions/fichier/id/${fichierId}`,
      { responseType: 'blob' }
    ).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href     = url;
        a.download = nomOriginal;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => { this.errorMsg = 'Erreur lors du téléchargement.'; }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // NAVIGATION
  // ══════════════════════════════════════════════════════════════

  retour(): void {
    const dossierId = this.mission?.prestation?.dossier?.id;
    if (dossierId) {
      this.router.navigate(['/agent/dossiers', dossierId, 'missions']);
    } else {
      this.router.navigate(['/agent/dashboard']);
    }
  }

  // ══════════════════════════════════════════════════════════════
  // HELPERS
  // ══════════════════════════════════════════════════════════════

  private afficherSucces(msg: string): void {
    this.successMsg = msg;
    setTimeout(() => this.successMsg = '', 4000);
  }

  peutValider(): boolean {
    return this.isAgent &&
      ['PV_SOUMIS', 'FACTURE_SOUMISE', 'EN_COURS'].includes(this.mission?.statut) &&
      !this.resultatVerrouille;
  }

  peutRejeter(): boolean {
    return this.isAgent &&
      ['PV_SOUMIS', 'FACTURE_SOUMISE', 'EN_COURS', 'ASSIGNEE'].includes(this.mission?.statut) &&
      !this.resultatVerrouille;
  }

  getStatutClass(s: string): string {
    const map: Record<string, string> = {
      'ASSIGNEE':        'statut--blue',
      'EN_COURS':        'statut--orange',
      'PV_SOUMIS':       'statut--purple',
      'FACTURE_SOUMISE': 'statut--pink',
      'VALIDEE_AGENT':   'statut--teal',
      'TERMINEE':        'statut--green',
      'REJETEE':         'statut--red',
    };
    return map[s] || 'statut--grey';
  }

  getStatutLabel(s: string): string {
    const map: Record<string, string> = {
      'ASSIGNEE': 'Assignée', 'EN_COURS': 'En cours',
      'PV_SOUMIS': 'PV soumis', 'FACTURE_SOUMISE': 'Facture soumise',
      'VALIDEE_AGENT': 'Validée', 'TERMINEE': 'Terminée', 'REJETEE': 'Rejetée',
    };
    return map[s] || s;
  }

  getEmojiType(type: string): string {
    const map: Record<string, string> = { 'AVOCAT': '⚖️', 'EXPERT': '🔬', 'HUISSIER': '📜' };
    return map[type] || '👤';
  }

  nomPrestataire(): string {
    const p = this.mission?.prestataire;
    if (!p) return '—';
    return `${p.prenom || ''} ${p.nom || ''}`.trim() || p.username;
  }

  formatTaille(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  getIconFichier(mime: string): string {
    if (mime?.includes('pdf'))   return '📄';
    if (mime?.includes('image')) return '🖼️';
    if (mime?.includes('word'))  return '📝';
    if (mime?.includes('excel') || mime?.includes('spreadsheet')) return '📊';
    return '📎';
  }
}