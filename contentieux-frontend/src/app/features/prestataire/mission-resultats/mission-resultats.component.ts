import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { PrestataireService } from '../../../core/services/prestataire.service';

@Component({
  selector: 'app-mission-resultats',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-container">

      <!-- Header -->
      <div class="page-header">
        <button class="btn-back" (click)="retour()">
          ← Retour au dashboard
        </button>
        <div class="header-info">
          <h1>📊 Résultats de la Mission</h1>
          <p class="subtitle" *ngIf="mission">
            {{ mission.numeroMission }} —
            <span class="statut-badge" [ngClass]="getStatutClass(mission.statut)">
              {{ mission.statut }}
            </span>
          </p>
        </div>
      </div>

      <!-- Loading -->
      <div class="loading-box" *ngIf="loading">
        <div class="spinner"></div>
        <p>Chargement des résultats...</p>
      </div>

      <!-- Erreur -->
      <div class="error-banner" *ngIf="erreur">{{ erreur }}</div>

      <!-- Contenu -->
      <div class="content-grid" *ngIf="!loading && !erreur">

        <!-- Carte Mission Info -->
        <div class="card mission-card">
          <div class="card-header blue-header">
            <h2>📋 Informations Mission</h2>
          </div>
          <div class="card-body" *ngIf="mission">
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">Numéro mission</span>
                <span class="info-value">{{ mission.numeroMission }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">Statut</span>
                <span class="statut-badge" [ngClass]="getStatutClass(mission.statut)">
                  {{ mission.statut }}
                </span>
              </div>
              <div class="info-item">
                <span class="info-label">Date assignation</span>
                <span class="info-value">
                  {{ mission.dateAssignation | date:'dd/MM/yyyy' }}
                </span>
              </div>
              <div class="info-item">
                <span class="info-label">Type prestation</span>
                <span class="info-value">{{ mission.prestation?.type || '—' }}</span>
              </div>
              <div class="info-item" *ngIf="mission.prestation?.dossier as d">
                <span class="info-label">Dossier</span>
                <span class="info-value">{{ d.numeroDossier }}</span>
              </div>
              <div class="info-item" *ngIf="mission.prestation?.dossier?.client as c">
                <span class="info-label">Client</span>
                <span class="info-value">{{ c.nom }} {{ c.prenom }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Carte PV -->
        <div class="card pv-card">
          <div class="card-header indigo-header">
            <h2>📄 Procès-Verbal</h2>
            <span class="badge-present" *ngIf="resultat?.pvTexte">✅ Soumis</span>
            <span class="badge-absent"  *ngIf="!resultat?.pvTexte">⏳ Non soumis</span>
          </div>
          <div class="card-body">
            <div *ngIf="resultat?.pvTexte; else noPV">
              <div class="text-content">{{ resultat.pvTexte }}</div>
              <div class="meta-row" *ngIf="resultat.dateSoumissionPV">
                📅 Soumis le {{ resultat.dateSoumissionPV | date:'dd/MM/yyyy à HH:mm' }}
              </div>
              <div class="meta-row" *ngIf="resultat.soumisePar">
                👤 Par {{ resultat.soumisePar }}
              </div>
            </div>
            <ng-template #noPV>
              <div class="empty-state-inline">
                Aucun procès-verbal soumis pour cette mission.
              </div>
            </ng-template>
          </div>
        </div>

        <!-- Carte Commentaire -->
        <div class="card commentaire-card" *ngIf="resultat?.commentaire">
          <div class="card-header orange-header">
            <h2>💬 Commentaire</h2>
          </div>
          <div class="card-body">
            <div class="text-content">{{ resultat.commentaire }}</div>
          </div>
        </div>

        <!-- Carte Facture -->
        <div class="card facture-card">
          <div class="card-header green-header">
            <h2>💳 Facture</h2>
            <span class="badge-present" *ngIf="resultat?.factureRef">✅ Soumise</span>
            <span class="badge-absent"  *ngIf="!resultat?.factureRef">⏳ Non soumise</span>
          </div>
          <div class="card-body">
            <div *ngIf="resultat?.factureRef; else noFacture">
              <div class="facture-grid">
                <div class="facture-bloc">
                  <span class="facture-label">Référence</span>
                  <span class="facture-value ref">{{ resultat.factureRef }}</span>
                </div>
                <div class="facture-bloc">
                  <span class="facture-label">Montant</span>
                  <span class="facture-value montant">
                    {{ resultat.montant | number:'1.2-2' }} TND
                  </span>
                </div>
              </div>
              <div class="meta-row" *ngIf="resultat.dateSoumissionFacture">
                📅 Soumise le
                {{ resultat.dateSoumissionFacture | date:'dd/MM/yyyy à HH:mm' }}
              </div>
            </div>
            <ng-template #noFacture>
              <div class="empty-state-inline">
                Aucune facture soumise pour cette mission.
              </div>
            </ng-template>
          </div>
        </div>

        <!-- Carte Documents -->
        <div class="card docs-card">
          <div class="card-header teal-header">
            <h2>📎 Documents</h2>
            <span class="doc-count">{{ fichiers.length }} fichier(s)</span>
          </div>
          <div class="card-body">
            <div *ngIf="fichiers.length > 0; else noDocs">
              <div class="doc-item" *ngFor="let d of fichiers">
                <div class="doc-left">
                  <span class="doc-icone">{{ iconeType(d.typeMime) }}</span>
                  <div class="doc-info">
                    <span class="doc-nom">{{ d.nomFichierOriginal }}</span>
                    <span class="doc-meta">
                      {{ formatTaille(d.tailleFichier) }} ·
                      {{ d.dateUpload | date:'dd/MM/yyyy HH:mm' }}
                    </span>
                  </div>
                </div>
                <button class="btn-dl"
                        (click)="telecharger(d.nomFichierServeur, d.nomFichierOriginal)"
                        [disabled]="telechargementEnCours[d.nomFichierServeur]">
                  {{ telechargementEnCours[d.nomFichierServeur] ? '⏳' : '⬇️' }}
                  Télécharger
                </button>
              </div>
            </div>
            <ng-template #noDocs>
              <div class="empty-state-inline">
                Aucun document envoyé pour cette mission.
              </div>
            </ng-template>
          </div>
        </div>

      </div>
    </div>
  `,
  styles: [`
    * { box-sizing: border-box; }
    .page-container {
      font-family: 'Inter', sans-serif;
      padding: 30px;
      max-width: 1100px;
      margin: 0 auto;
    }

    /* Header */
    .page-header { display: flex; align-items: flex-start; gap: 20px; margin-bottom: 30px; }
    .btn-back {
      padding: 10px 18px; background: #f5f5f5; border: none;
      border-radius: 8px; cursor: pointer; font-weight: 600;
      color: #555; transition: all .2s; white-space: nowrap;
    }
    .btn-back:hover { background: #e0e0e0; }
    .header-info h1 { margin: 0; font-size: 1.7rem; color: #1a237e; }
    .subtitle { margin: 6px 0 0; color: #777; display: flex; align-items: center; gap: 10px; }

    /* Loading */
    .loading-box {
      display: flex; flex-direction: column; align-items: center;
      padding: 60px; color: #777; gap: 16px;
    }
    .spinner {
      width: 40px; height: 40px; border: 4px solid #e0e0e0;
      border-top-color: #1a237e; border-radius: 50%;
      animation: spin 1s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* Grid */
    .content-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
    }
    .docs-card   { grid-column: 1 / -1; }

    /* Cards */
    .card {
      background: white; border-radius: 14px;
      box-shadow: 0 4px 20px rgba(0,0,0,0.07);
      overflow: hidden;
    }
    .card-header {
      padding: 18px 24px;
      display: flex; align-items: center;
      justify-content: space-between;
    }
    .card-header h2 { margin: 0; color: white; font-size: 1.05rem; }
    .blue-header   { background: linear-gradient(135deg, #1565c0, #1976d2); }
    .indigo-header { background: linear-gradient(135deg, #1a237e, #283593); }
    .orange-header { background: linear-gradient(135deg, #e65100, #f57c00); }
    .green-header  { background: linear-gradient(135deg, #1b5e20, #2e7d32); }
    .teal-header   { background: linear-gradient(135deg, #004d40, #00695c); }

    .card-body { padding: 24px; }

    /* Badges */
    .badge-present {
      background: rgba(255,255,255,0.25); color: white;
      padding: 4px 10px; border-radius: 20px; font-size: .8rem; font-weight: 600;
    }
    .badge-absent {
      background: rgba(0,0,0,0.15); color: rgba(255,255,255,0.85);
      padding: 4px 10px; border-radius: 20px; font-size: .8rem;
    }
    .doc-count {
      background: rgba(255,255,255,0.25); color: white;
      padding: 4px 12px; border-radius: 20px; font-size: .85rem; font-weight: 600;
    }

    /* Statut badges */
    .statut-badge { padding: 4px 12px; border-radius: 15px; font-size: .8rem; font-weight: bold; }
    .s-assignee      { background: #fff3cd; color: #856404; }
    .s-en-cours      { background: #cce5ff; color: #004085; }
    .s-pv-soumis     { background: #d1ecf1; color: #0c5460; }
    .s-facture       { background: #d4edda; color: #155724; }
    .s-terminee      { background: #e2e3e5; color: #383d41; }

    /* Info grid */
    .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    .info-item { display: flex; flex-direction: column; gap: 4px; }
    .info-label { font-size: .8rem; color: #999; text-transform: uppercase; letter-spacing: .5px; }
    .info-value { font-size: .95rem; font-weight: 600; color: #333; }

    /* PV / Commentaire */
    .text-content {
      background: #f8f9fa; border-radius: 8px; padding: 16px;
      line-height: 1.7; color: #333; white-space: pre-wrap;
      font-size: .95rem; margin-bottom: 12px;
      border-left: 4px solid #1a237e;
    }
    .meta-row { font-size: .82rem; color: #999; margin-top: 6px; }

    /* Facture */
    .facture-grid { display: flex; gap: 40px; margin-bottom: 16px; }
    .facture-bloc { display: flex; flex-direction: column; gap: 6px; }
    .facture-label { font-size: .8rem; color: #999; text-transform: uppercase; }
    .facture-value { font-size: 1.1rem; font-weight: 700; color: #333; }
    .facture-value.ref    { color: #1a237e; }
    .facture-value.montant { color: #2e7d32; font-size: 1.4rem; }

    /* Documents */
    .doc-item {
      display: flex; justify-content: space-between; align-items: center;
      padding: 14px 0; border-bottom: 1px solid #f0f0f0;
    }
    .doc-item:last-child { border-bottom: none; }
    .doc-left { display: flex; align-items: center; gap: 14px; }
    .doc-icone { font-size: 1.8rem; }
    .doc-info  { display: flex; flex-direction: column; gap: 3px; }
    .doc-nom   { font-weight: 600; color: #333; font-size: .95rem; }
    .doc-meta  { font-size: .8rem; color: #999; }
    .btn-dl {
      padding: 8px 16px; background: #e3f2fd; color: #0d47a1;
      border: none; border-radius: 8px; cursor: pointer;
      font-weight: 600; font-size: .85rem; transition: all .2s;
      white-space: nowrap;
    }
    .btn-dl:hover    { background: #bbdefb; }
    .btn-dl:disabled { opacity: .6; cursor: not-allowed; }

    /* Divers */
    .empty-state-inline { color: #bbb; font-style: italic; padding: 10px 0; font-size: .9rem; }
    .error-banner {
      background: #fdecea; color: #c62828; padding: 14px 18px;
      border-radius: 8px; border-left: 4px solid #c62828; margin-bottom: 20px;
    }

    @media (max-width: 768px) {
      .content-grid { grid-template-columns: 1fr; }
      .docs-card    { grid-column: 1; }
      .info-grid    { grid-template-columns: 1fr; }
      .facture-grid { flex-direction: column; gap: 16px; }
    }
  `]
})
export class MissionResultatsComponent implements OnInit {

  missionId!: number;
  mission:    any = null;
  resultat:   any = null;
  fichiers:   any[] = [];
  loading  = true;
  erreur   = '';
  telechargementEnCours: Record<string, boolean> = {};

  constructor(
    private route:              ActivatedRoute,
    private router:             Router,
    private prestataireService: PrestataireService
  ) {}

  ngOnInit() {
    this.missionId = Number(this.route.snapshot.paramMap.get('id'));
    this.chargerDonnees();
  }

  chargerDonnees() {
    this.loading = true;
    this.erreur  = '';

    forkJoin({
      resultat:  this.prestataireService.getResultatMission(this.missionId),
      documents: this.prestataireService.getDocuments(this.missionId),
      mission:   this.prestataireService.getMissionDetail(this.missionId)
    }).subscribe({
      next: ({ resultat, documents, mission }) => {
        this.mission  = mission;
        this.resultat = resultat;
        this.fichiers = documents?.fichiers || [];
        this.loading  = false;
      },
      error: (err) => {
        this.erreur  = err?.error?.error || 'Erreur lors du chargement des résultats.';
        this.loading = false;
      }
    });
  }

  telecharger(nomServeur: string, nomOriginal: string) {
    this.telechargementEnCours[nomServeur] = true;
    this.prestataireService.telechargerFichier(nomServeur).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href     = url;
        a.download = nomOriginal;
        a.click();
        URL.revokeObjectURL(url);
        this.telechargementEnCours[nomServeur] = false;
      },
      error: () => { this.telechargementEnCours[nomServeur] = false; }
    });
  }

  retour() { this.router.navigate(['/prestataire/dashboard']); }

  getStatutClass(statut: string): string {
    const map: Record<string, string> = {
      ASSIGNEE:        's-assignee',
      EN_COURS:        's-en-cours',
      PV_SOUMIS:       's-pv-soumis',
      FACTURE_SOUMISE: 's-facture',
      TERMINEE:        's-terminee'
    };
    return map[statut] || '';
  }

  iconeType(mime: string): string {
    if (!mime) return '📄';
    if (mime === 'application/pdf')                        return '📕';
    if (mime.startsWith('image/'))                         return '🖼️';
    if (mime.includes('word'))                             return '📝';
    if (mime.includes('sheet') || mime.includes('excel')) return '📊';
    if (mime.includes('zip'))                              return '🗜️';
    return '📄';
  }

  formatTaille(taille: number): string {
    if (!taille)             return '0 B';
    if (taille < 1024)       return taille + ' B';
    if (taille < 1024*1024)  return (taille / 1024).toFixed(1) + ' KB';
    return (taille / (1024*1024)).toFixed(1) + ' MB';
  }
}