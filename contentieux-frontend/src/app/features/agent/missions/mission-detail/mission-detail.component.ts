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

  private apiUrl      = `${environment.apiUrl}/api/prestataire/missions`;
  private agentUrl    = `${environment.apiUrl}/api/agent/missions`;

  missionId!: number;
  today = new Date().toISOString().split('T')[0];

  // ── Onglet actif ───────────────────────────────────────────────
  onglet: 'detail' | 'resultats' | 'historique' | 'affaire' = 'detail';

  // ── Données ────────────────────────────────────────────────────
  mission:           any = null;
  resultats:         any[] = [];
  historique:        any[] = [];
  affaire:           any = null;
  resultatVerrouille = false;
  isAgent            = false;

  // ── États ──────────────────────────────────────────────────────
  loading    = true;
  submitting = false;

  // ── Messages ───────────────────────────────────────────────────
  successMsg = '';
  errorMsg   = '';

  // ── Modal Validation ───────────────────────────────────────────
  showModalValidation   = false;
  commentaireValidation = '';

  // ── Modal Rejet ────────────────────────────────────────────────
  showModalRejet    = false;
  commentaireRejet  = '';

  // ── Modal Résultat ─────────────────────────────────────────────
  showModalResultat  = false;
  submittingResultat = false;
  resultatForm = { commentaire: '', fichiers: [] as File[] };

  // ── Modal PV ──────────────────────────────────────────────────
  showModalPV  = false;
  submittingPV = false;
  pvForm       = { pvTexte: '' };

  // ── Modal Facture ─────────────────────────────────────────────
  showModalFacture  = false;
  submittingFacture = false;
  factureForm       = { factureRef: '', montant: 0 };

  // ── Modal Documents ───────────────────────────────────────────
  showModalDocuments  = false;
  uploadEnCours       = false;
  fichiersSelectionnes: File[] = [];
  documentsExistants: any[]   = [];

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
        this.mission            = res.mission ?? res;
        this.resultats          = res.resultats  || [];
        this.historique         = res.historique || [];
        this.affaire            = res.affaire    || null;
        this.resultatVerrouille = res.resultatVerrouille || false;
        this.isAgent            = res.isAgent    || false;
        this.loading            = false;
      },
      error: (err) => {
        this.errorMsg = err?.error?.error || 'Impossible de charger la mission.';
        this.loading  = false;
      }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // VALIDATION / REJET (agent)
  // ══════════════════════════════════════════════════════════════

  validerMission(): void {
    this.submitting = true;
    this.http.post<any>(`${this.agentUrl}/${this.missionId}/valider`,
      { commentaire: this.commentaireValidation }
    ).subscribe({
      next: () => {
        this.submitting          = false;
        this.showModalValidation = false;
        this.mission.statut      = 'VALIDEE_AGENT';
        this.afficherSucces('Mission validée avec succès.');
      },
      error: (err) => {
        this.submitting = false;
        this.afficherErreur(err?.error?.error || 'Erreur lors de la validation.');
      }
    });
  }

  rejeterMission(): void {
    if (!this.commentaireRejet.trim()) return;
    this.submitting = true;
    this.http.post<any>(`${this.agentUrl}/${this.missionId}/rejeter`,
      { commentaire: this.commentaireRejet }
    ).subscribe({
      next: () => {
        this.submitting     = false;
        this.showModalRejet = false;
        this.mission.statut = 'REJETEE';
        this.afficherSucces('Mission rejetée.');
      },
      error: (err) => {
        this.submitting = false;
        this.afficherErreur(err?.error?.error || 'Erreur lors du rejet.');
      }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // SOUMETTRE RÉSULTAT (prestataire)
  // ══════════════════════════════════════════════════════════════

  soumettreResultat(): void {
    if (!this.resultatForm.commentaire.trim() && this.resultatForm.fichiers.length === 0) return;
    this.submittingResultat = true;

    const formData = new FormData();
    formData.append('commentaire', this.resultatForm.commentaire);
    this.resultatForm.fichiers.forEach(f => formData.append('fichiers', f));

    this.http.post<any>(`${this.apiUrl}/${this.missionId}/resultats`, formData).subscribe({
      next: () => {
        this.submittingResultat = false;
        this.showModalResultat  = false;
        this.resultatForm       = { commentaire: '', fichiers: [] };
        this.afficherSucces('Résultat soumis avec succès.');
        this.charger();
      },
      error: (err) => {
        this.submittingResultat = false;
        this.afficherErreur(err?.error?.error || 'Erreur lors de la soumission.');
      }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // SOUMETTRE PV (prestataire)
  // ══════════════════════════════════════════════════════════════

  soumettrePV(): void {
    if (!this.pvForm.pvTexte.trim()) {
      this.afficherErreur('Le contenu du PV est obligatoire.');
      return;
    }
    this.submittingPV = true;
    this.http.post<any>(
      `${this.apiUrl}/${this.missionId}/pv`,
      { pvTexte: this.pvForm.pvTexte }
    ).subscribe({
      next: () => {
        this.submittingPV   = false;
        this.showModalPV    = false;
        this.pvForm         = { pvTexte: '' };
        this.mission.statut = 'PV_SOUMIS';
        this.afficherSucces('PV soumis avec succès.');
        this.charger();
      },
      error: (err) => {
        this.submittingPV = false;
        this.afficherErreur(err?.error?.error || 'Erreur lors de la soumission du PV.');
      }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // SOUMETTRE FACTURE (prestataire)
  // ══════════════════════════════════════════════════════════════

  soumettreFacture(): void {
    if (!this.factureForm.factureRef.trim()) {
      this.afficherErreur('La référence facture est obligatoire.');
      return;
    }
    if (!this.factureForm.montant || this.factureForm.montant <= 0) {
      this.afficherErreur('Le montant doit être supérieur à 0.');
      return;
    }
    this.submittingFacture = true;
    this.http.post<any>(
      `${this.apiUrl}/${this.missionId}/facture`,
      this.factureForm
    ).subscribe({
      next: () => {
        this.submittingFacture = false;
        this.showModalFacture  = false;
        this.factureForm       = { factureRef: '', montant: 0 };
        this.mission.statut    = 'FACTURE_SOUMISE';
        this.afficherSucces('Facture soumise avec succès.');
        this.charger();
      },
      error: (err) => {
        this.submittingFacture = false;
        this.afficherErreur(err?.error?.error || 'Erreur lors de la soumission de la facture.');
      }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // SUPPRESSION FICHIER RÉSULTAT
  // ══════════════════════════════════════════════════════════════

  supprimerResultat(fichierId: number): void {
    if (!confirm('Supprimer ce fichier ?')) return;
    this.http.delete<any>(
      `${environment.apiUrl}/api/missions/fichier/${fichierId}`
    ).subscribe({
      next: () => {
        this.afficherSucces('Fichier supprimé.');
        this.charger();
      },
      error: (err) => {
        this.errorMsg = err?.error?.error || 'Erreur lors de la suppression.';
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
        const url  = URL.createObjectURL(blob);
        const a    = document.createElement('a');
        a.href     = url;
        a.download = nomOriginal;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => { this.afficherErreur('Erreur lors du téléchargement.'); }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // NAVIGATION
  // ══════════════════════════════════════════════════════════════

  retour(): void {
    const dossierId = this.mission?.prestation?.dossier?.id;
    if (this.isAgent && dossierId) {
      this.router.navigate(['/agent/dossiers', dossierId, 'missions']);
    } else {
      this.router.navigate(['/prestataire/missions']);
    }
  }

  // ══════════════════════════════════════════════════════════════
  // GARDES / CONDITIONS
  // ══════════════════════════════════════════════════════════════

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

  peutSoumettreResultat(): boolean {
    return !this.isAgent &&
      ['ASSIGNEE', 'EN_COURS'].includes(this.mission?.statut) &&
      !this.resultatVerrouille;
  }

  /** PV soumettable si mission ASSIGNEE ou EN_COURS */
  peutSoumettrePV(): boolean {
    return !this.isAgent &&
      ['ASSIGNEE', 'EN_COURS'].includes(this.mission?.statut) &&
      !this.resultatVerrouille;
  }

  /** Facture soumettable si PV déjà soumis */
  peutSoumettreFacture(): boolean {
    return !this.isAgent &&
      this.mission?.statut === 'PV_SOUMIS' &&
      !this.resultatVerrouille;
  }

  peutSupprimerResultat(): boolean {
    return !this.isAgent &&
      ['ASSIGNEE', 'EN_COURS'].includes(this.mission?.statut) &&
      !this.resultatVerrouille;
  }
  // ══════════════════════════════════════════════════════════════
  // DOCUMENTS (upload / téléchargement)
  // ══════════════════════════════════════════════════════════════

  ouvrirModalDocuments(): void {
    this.showModalDocuments  = true;
    this.fichiersSelectionnes = [];
    this.documentsExistants  = [];
    this.errorMsg = '';
    this.successMsg = '';
    this.chargerDocuments();
  }

  chargerDocuments(): void {
    this.http.get<any>(
      `${this.apiUrl}/${this.missionId}/documents`
    ).subscribe({
      next: (res) => this.documentsExistants = res.fichiers || [],
      error: () => {}
    });
  }

  onDocumentsFichiersChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fichiersSelectionnes = input.files ? Array.from(input.files) : [];
  }

  uploaderDocuments(): void {
    if (this.fichiersSelectionnes.length === 0) return;
    this.uploadEnCours = true;
    const formData = new FormData();
    this.fichiersSelectionnes.forEach(f => formData.append('fichiers', f, f.name));
    this.http.post<any>(
      `${this.apiUrl}/${this.missionId}/documents`,
      formData
    ).subscribe({
      next: (res) => {
        this.uploadEnCours        = false;
        this.fichiersSelectionnes = [];
        this.afficherSucces(`✅ ${res.fichiers?.length ?? 1} document(s) ajouté(s).`);
        this.chargerDocuments();
      },
      error: (err) => {
        this.uploadEnCours = false;
        this.afficherErreur(err?.error?.error || 'Erreur lors de l\'upload.');
      }
    });
  }

  telechargerDocument(fichierId: number, nomOriginal: string): void {
    this.http.get(
      `${environment.apiUrl}/api/prestataire/missions/fichier/id/${fichierId}`,
      { responseType: 'blob' }
    ).subscribe({
      next: (blob) => {
        const url  = URL.createObjectURL(blob);
        const a    = document.createElement('a');
        a.href     = url;
        a.download = nomOriginal;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => { this.afficherErreur('Erreur lors du téléchargement.'); }
    });
  }

  iconeType(mime: string): string {
    if (!mime) return '📄';
    if (mime === 'application/pdf') return '📕';
    if (mime.startsWith('image/')) return '🖼️';
    if (mime.includes('word')) return '📝';
    if (mime.includes('sheet') || mime.includes('excel')) return '📊';
    if (mime.includes('zip')) return '🗜️';
    return '📄';
  }

  // ══════════════════════════════════════════════════════════════
  // HELPERS
  // ══════════════════════════════════════════════════════════════

  onFichiersChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.resultatForm.fichiers = input.files ? Array.from(input.files) : [];
  }

  private afficherSucces(msg: string): void {
    this.successMsg = msg;
    setTimeout(() => this.successMsg = '', 4000);
  }

  private afficherErreur(msg: string): void {
    this.errorMsg = msg;
    setTimeout(() => this.errorMsg = '', 4000);
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
      'ASSIGNEE':        'Assignée',
      'EN_COURS':        'En cours',
      'PV_SOUMIS':       'PV soumis',
      'FACTURE_SOUMISE': 'Facture soumise',
      'VALIDEE_AGENT':   'Validée',
      'TERMINEE':        'Terminée',
      'REJETEE':         'Rejetée',
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