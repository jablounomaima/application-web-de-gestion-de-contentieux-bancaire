// resultats-prestataires.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Component({
  selector: 'app-resultats-prestataires',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page">

      <div class="page-header">
        <button class="btn-back" (click)="retour()">← Retour</button>
        <div>
          <h1>📊 Résultats Prestataires</h1>
          <p class="sub" *ngIf="numeroDossier">Dossier : <strong>{{ numeroDossier }}</strong> — {{ libelle }}</p>
        </div>
      </div>

      <div class="loading" *ngIf="loading">
        <div class="spinner"></div><p>Chargement...</p>
      </div>

      <div class="error" *ngIf="erreur">{{ erreur }}</div>

      <div *ngIf="!loading && !erreur">

        <div class="empty" *ngIf="resultats.length === 0">
          Aucune mission assignée pour ce dossier.
        </div>

        <div class="mission-card" *ngFor="let r of resultats">

          <!-- Header mission -->
          <div class="mission-header" [ngClass]="getStatutClass(r.statut)">
            <div class="mission-title">
              <span class="numero">{{ r.numeroMission }}</span>
              <span class="type-badge">{{ r.typePrestation }}</span>
            </div>
            <span class="statut-label">{{ formatStatut(r.statut) }}</span>
          </div>

          <div class="mission-body">

            <!-- Prestataire -->
            <div class="section" *ngIf="r.prestataire">
              <h3>👤 Prestataire</h3>
              <div class="info-row">
                <span>{{ r.prestataire.nom }} {{ r.prestataire.prenom }}</span>
                <span class="meta">{{ r.prestataire.type }} · {{ r.prestataire.specialite }}</span>
              </div>
            </div>

            <!-- PV -->
            <div class="section">
              <h3>
                📄 Procès-Verbal
                <span class="badge-ok"  *ngIf="r.pvTexte">✅ Soumis</span>
                <span class="badge-non" *ngIf="!r.pvTexte">⏳ Non soumis</span>
              </h3>
              <div class="text-box" *ngIf="r.pvTexte">{{ r.pvTexte }}</div>
              <div class="meta" *ngIf="r.dateValidationPv">
                📅 {{ r.dateValidationPv | date:'dd/MM/yyyy HH:mm' }}
              </div>
            </div>

            <!-- Commentaire -->
            <div class="section" *ngIf="r.commentaire">
              <h3>💬 Commentaire</h3>
              <div class="text-box comment">{{ r.commentaire }}</div>
              <div class="meta">Par {{ r.soumisePar }} · {{ r.dateSoumission | date:'dd/MM/yyyy HH:mm' }}</div>
            </div>

            <!-- Facture -->
            <div class="section">
              <h3>
                💳 Facture
                <span class="badge-ok"  *ngIf="r.factureRef">✅ Soumise</span>
                <span class="badge-non" *ngIf="!r.factureRef">⏳ Non soumise</span>
              </h3>
              <div class="facture-row" *ngIf="r.factureRef">
                <div class="facture-item">
                  <span class="flabel">Référence</span>
                  <span class="fval ref">{{ r.factureRef }}</span>
                </div>
                <div class="facture-item">
                  <span class="flabel">Montant</span>
                  <span class="fval montant">{{ r.montantFacture | number:'1.2-2' }} TND</span>
                </div>
              </div>
              <div class="meta" *ngIf="r.dateValidationFacture">
                📅 {{ r.dateValidationFacture | date:'dd/MM/yyyy HH:mm' }}
              </div>
            </div>

            <!-- Fichiers -->
            <div class="section" *ngIf="r.fichiers?.length > 0">
              <h3>📎 Documents ({{ r.fichiers.length }})</h3>
              <div class="fichier-item" *ngFor="let f of r.fichiers">
                <div class="fichier-left">
                  <span class="fichier-icon">{{ icone(f.typeMime) }}</span>
                  <div>
                    <div class="fichier-nom">{{ f.nomFichierOriginal }}</div>
                    <div class="meta">{{ formatTaille(f.tailleFichier) }} · {{ f.dateUpload | date:'dd/MM/yyyy' }}</div>
                  </div>
                </div>
                <button class="btn-dl" (click)="telecharger(f.id, f.nomFichierOriginal)"
                        [disabled]="dlEnCours[f.id]">
                  {{ dlEnCours[f.id] ? '⏳' : '⬇️' }} Télécharger
                </button>
              </div>
            </div>

            <!-- Actions agent -->
            <div class="actions" *ngIf="peutValider(r.statut)">
              <button class="btn-valider" (click)="valider(r.missionId)">✅ Valider</button>
              <button class="btn-rejeter" (click)="rejeter(r.missionId)">❌ Rejeter</button>
            </div>

            <!-- Commentaire agent -->
            <div class="agent-comment" *ngIf="r.commentaireAgent">
              💬 Commentaire agent : {{ r.commentaireAgent }}
            </div>

          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    * { box-sizing: border-box; }
    .page { font-family: Inter, sans-serif; padding: 30px; max-width: 1000px; margin: 0 auto; }

    .page-header { display: flex; align-items: flex-start; gap: 20px; margin-bottom: 30px; }
    .btn-back { padding: 10px 18px; background: #f0f0f0; border: none; border-radius: 8px;
      cursor: pointer; font-weight: 600; color: #555; }
    .btn-back:hover { background: #e0e0e0; }
    h1 { margin: 0; font-size: 1.6rem; color: #1a237e; }
    .sub { margin: 4px 0 0; color: #777; font-size: .9rem; }

    .loading { display: flex; flex-direction: column; align-items: center; padding: 60px; gap: 16px; color: #999; }
    .spinner { width: 36px; height: 36px; border: 4px solid #eee; border-top-color: #1a237e;
      border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .error { background: #fdecea; color: #c62828; padding: 14px; border-radius: 8px; border-left: 4px solid #c62828; }
    .empty { text-align: center; color: #bbb; padding: 60px; font-style: italic; }

    .mission-card { background: white; border-radius: 14px; box-shadow: 0 4px 20px rgba(0,0,0,.07);
      margin-bottom: 24px; overflow: hidden; }

    .mission-header { padding: 16px 24px; display: flex; justify-content: space-between; align-items: center; }
    .mission-header.s-assignee      { background: linear-gradient(135deg,#f57f17,#f9a825); }
    .mission-header.s-en-cours      { background: linear-gradient(135deg,#1565c0,#1976d2); }
    .mission-header.s-pv-soumis     { background: linear-gradient(135deg,#00695c,#00897b); }
    .mission-header.s-facture       { background: linear-gradient(135deg,#2e7d32,#388e3c); }
    .mission-header.s-terminee      { background: linear-gradient(135deg,#424242,#616161); }
    .mission-header.s-rejetee       { background: linear-gradient(135deg,#b71c1c,#c62828); }
    .mission-title { display: flex; align-items: center; gap: 12px; }
    .numero { color: white; font-weight: 700; font-size: 1rem; }
    .type-badge { background: rgba(255,255,255,.25); color: white; padding: 3px 10px;
      border-radius: 20px; font-size: .8rem; }
    .statut-label { color: rgba(255,255,255,.9); font-size: .85rem; font-weight: 600; }

    .mission-body { padding: 24px; display: flex; flex-direction: column; gap: 20px; }

    .section h3 { margin: 0 0 10px; font-size: .95rem; color: #333;
      display: flex; align-items: center; gap: 8px; }
    .info-row { display: flex; flex-direction: column; gap: 2px; }
    .info-row span:first-child { font-weight: 600; color: #333; }
    .meta { font-size: .8rem; color: #999; margin-top: 4px; }

    .text-box { background: #f8f9fa; border-left: 4px solid #1a237e; border-radius: 8px;
      padding: 14px; white-space: pre-wrap; font-size: .9rem; color: #333; line-height: 1.6; }
    .text-box.comment { border-left-color: #f57c00; }

    .badge-ok  { background: #e8f5e9; color: #2e7d32; padding: 2px 10px; border-radius: 20px; font-size: .78rem; }
    .badge-non { background: #fff3e0; color: #e65100; padding: 2px 10px; border-radius: 20px; font-size: .78rem; }

    .facture-row { display: flex; gap: 40px; margin-bottom: 8px; }
    .facture-item { display: flex; flex-direction: column; gap: 4px; }
    .flabel { font-size: .78rem; color: #999; text-transform: uppercase; }
    .fval { font-size: 1rem; font-weight: 700; color: #333; }
    .fval.ref     { color: #1a237e; }
    .fval.montant { color: #2e7d32; font-size: 1.2rem; }

    .fichier-item { display: flex; justify-content: space-between; align-items: center;
      padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
    .fichier-item:last-child { border-bottom: none; }
    .fichier-left { display: flex; align-items: center; gap: 12px; }
    .fichier-icon { font-size: 1.6rem; }
    .fichier-nom  { font-weight: 600; color: #333; font-size: .9rem; }
    .btn-dl { padding: 7px 14px; background: #e3f2fd; color: #0d47a1;
      border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: .82rem; }
    .btn-dl:hover    { background: #bbdefb; }
    .btn-dl:disabled { opacity: .6; cursor: not-allowed; }

    .actions { display: flex; gap: 12px; padding-top: 8px; }
    .btn-valider { padding: 10px 24px; background: #2e7d32; color: white;
      border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: .9rem; }
    .btn-valider:hover { background: #1b5e20; }
    .btn-rejeter { padding: 10px 24px; background: #c62828; color: white;
      border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: .9rem; }
    .btn-rejeter:hover { background: #b71c1c; }

    .agent-comment { background: #fff8e1; border-left: 4px solid #f9a825;
      padding: 10px 14px; border-radius: 6px; font-size: .88rem; color: #555; }
  `]
})
export class ResultatsPrestatairesComponent implements OnInit {

  dossierId!: number;
  numeroDossier = '';
  libelle = '';
  resultats: any[] = [];
  loading = true;
  erreur = '';
  dlEnCours: Record<number, boolean> = {};

  private apiUrl = 'http://localhost:8098';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.dossierId = Number(this.route.snapshot.paramMap.get('dossierId'));
    this.charger();
  }

  charger() {
    this.loading = true;
    const token = localStorage.getItem('access_token') || '';
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

    this.http.get<any>(
      `${this.apiUrl}/api/agent/dossiers/${this.dossierId}/resultats-prestataires`,
      { headers }
    ).subscribe({
      next: (data) => {
        this.numeroDossier = data.numeroDossier;
        this.libelle       = data.libelle;
        this.resultats     = data.resultats;
        this.loading       = false;
      },
      error: (err) => {
        this.erreur  = err?.error?.error || 'Erreur chargement';
        this.loading = false;
      }
    });
  }

  valider(missionId: number) {
    const commentaire = prompt('Commentaire de validation (optionnel) :') ?? '';
    const token = localStorage.getItem('access_token') || '';
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

    this.http.post(
      `${this.apiUrl}/api/agent/missions/${missionId}/valider`,
      { commentaire },
      { headers }
    ).subscribe({
      next: () => { alert('Mission validée ✅'); this.charger(); },
      error: (e) => alert('Erreur : ' + (e?.error?.error || e.message))
    });
  }

  rejeter(missionId: number) {
    const commentaire = prompt('Motif de rejet :');
    if (!commentaire) return;
    const token = localStorage.getItem('access_token') || '';
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

    this.http.post(
      `${this.apiUrl}/api/agent/missions/${missionId}/rejeter`,
      { commentaire },
      { headers }
    ).subscribe({
      next: () => { alert('Mission rejetée ❌'); this.charger(); },
      error: (e) => alert('Erreur : ' + (e?.error?.error || e.message))
    });
  }

  telecharger(fichierId: number, nomOriginal: string) {
    this.dlEnCours[fichierId] = true;
    const token = localStorage.getItem('access_token') || '';
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

    this.http.get(
      `${this.apiUrl}/api/prestataire/missions/fichier/id/${fichierId}`,
      { headers, responseType: 'blob' }
    ).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href = url; a.download = nomOriginal; a.click();
        URL.revokeObjectURL(url);
        this.dlEnCours[fichierId] = false;
      },
      error: () => { this.dlEnCours[fichierId] = false; }
    });
  }

  retour() { this.router.navigate(['/agent/dossiers', this.dossierId]); }

  peutValider(statut: string): boolean {
    return statut === 'PV_SOUMIS' || statut === 'FACTURE_SOUMISE';
  }

  getStatutClass(statut: string): string {
    const map: Record<string, string> = {
      ASSIGNEE: 's-assignee', EN_COURS: 's-en-cours',
      PV_SOUMIS: 's-pv-soumis', FACTURE_SOUMISE: 's-facture',
      TERMINEE: 's-terminee', REJETEE: 's-rejetee'
    };
    return map[statut] || '';
  }

  formatStatut(s: string): string {
    const map: Record<string, string> = {
      ASSIGNEE: 'Assignée', EN_COURS: 'En cours', PV_SOUMIS: 'PV soumis',
      FACTURE_SOUMISE: 'Facture soumise', TERMINEE: 'Terminée', REJETEE: 'Rejetée'
    };
    return map[s] || s;
  }

  icone(mime: string): string {
    if (!mime) return '📄';
    if (mime === 'application/pdf') return '📕';
    if (mime.startsWith('image/')) return '🖼️';
    if (mime.includes('word')) return '📝';
    if (mime.includes('sheet') || mime.includes('excel')) return '📊';
    return '📄';
  }

  formatTaille(t: number): string {
    if (!t) return '0 B';
    if (t < 1024) return t + ' B';
    if (t < 1024 * 1024) return (t / 1024).toFixed(1) + ' KB';
    return (t / (1024 * 1024)).toFixed(1) + ' MB';
  }
}