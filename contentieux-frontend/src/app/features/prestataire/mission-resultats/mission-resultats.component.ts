import { Component, OnInit, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { PrestataireService } from '../../../core/services/prestataire.service';

@Component({
  selector: 'app-mission-resultats',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-container">

      <!-- Header -->
      <div class="page-header">
        <button class="btn-back" (click)="retour()">← Retour au dashboard</button>
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

      <!-- Succès -->
      <div class="success-banner" *ngIf="successMsg">{{ successMsg }}</div>

      <!-- Bannière rejet facture -->
      <div class="rejet-banner" *ngIf="factureRejetee">
        <div class="rejet-icon">❌</div>
        <div class="rejet-content">
          <strong>Votre facture a été rejetée</strong>
          <p *ngIf="motifRejet">Motif : <em>{{ motifRejet }}</em></p>
          <p>Veuillez corriger les informations ci-dessous et resoumettre votre facture.</p>
        </div>
      </div>

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
                <span class="info-value">{{ mission.dateAssignation | date:'dd/MM/yyyy' }}</span>
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
            <ng-container *ngIf="!editPV">
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
                <div class="empty-state-inline">Aucun procès-verbal soumis.</div>
              </ng-template>
              <button class="btn-edit" *ngIf="peutModifier" (click)="editPV = true; pvForm = resultat?.pvTexte || ''">
                ✏️ {{ resultat?.pvTexte ? 'Modifier' : 'Saisir' }} le PV
              </button>
            </ng-container>
            <ng-container *ngIf="editPV">
              <textarea class="textarea-field" [(ngModel)]="pvForm"
                placeholder="Saisissez le texte du procès-verbal..." rows="6"></textarea>
              <div class="btn-row">
                <button class="btn-cancel" (click)="editPV = false">Annuler</button>
                <button class="btn-save" (click)="soumettreResultat()" [disabled]="envoiEnCours">
                  {{ envoiEnCours ? '⏳ Envoi...' : '💾 Enregistrer' }}
                </button>
              </div>
            </ng-container>
          </div>
        </div>

        <!-- Carte Commentaire -->
        <div class="card commentaire-card">
          <div class="card-header orange-header">
            <h2>💬 Commentaire</h2>
          </div>
          <div class="card-body">
            <ng-container *ngIf="!editCommentaire">
              <div class="text-content" *ngIf="resultat?.commentaire; else noComment">
                {{ resultat.commentaire }}
              </div>
              <ng-template #noComment>
                <div class="empty-state-inline">Aucun commentaire.</div>
              </ng-template>
              <button class="btn-edit" *ngIf="peutModifier"
                (click)="editCommentaire = true; commentaireForm = resultat?.commentaire || ''">
                ✏️ {{ resultat?.commentaire ? 'Modifier' : 'Ajouter' }} un commentaire
              </button>
            </ng-container>
            <ng-container *ngIf="editCommentaire">
              <textarea class="textarea-field" [(ngModel)]="commentaireForm"
                placeholder="Ajoutez un commentaire..." rows="4"></textarea>
              <div class="btn-row">
                <button class="btn-cancel" (click)="editCommentaire = false">Annuler</button>
                <button class="btn-save" (click)="soumettreResultat()" [disabled]="envoiEnCours">
                  {{ envoiEnCours ? '⏳ Envoi...' : '💾 Enregistrer' }}
                </button>
              </div>
            </ng-container>
          </div>
        </div>

        <!-- Carte Facture -->
        <div class="card facture-card" [class.facture-rejetee-card]="factureRejetee" #factureCard>
          <div class="card-header" [ngClass]="factureRejetee ? 'red-header' : 'green-header'">
            <h2>💳 Facture</h2>
            <div class="header-badges">
              <span class="badge-rejet"   *ngIf="factureRejetee">❌ Rejetée</span>
              <span class="badge-present" *ngIf="resultat?.factureRef && !factureRejetee">✅ Soumise</span>
              <span class="badge-absent"  *ngIf="!resultat?.factureRef && !factureRejetee">⏳ Non soumise</span>
            </div>
          </div>
          <div class="card-body">

            <!-- Mode lecture -->
            <ng-container *ngIf="!editFacture">
              <div *ngIf="resultat?.factureRef; else noFacture">
                <div class="facture-grid">
                  <div class="facture-bloc">
                    <span class="facture-label">Référence</span>
                    <span class="facture-value ref">{{ resultat.factureRef }}</span>
                  </div>
                  <div class="facture-bloc">
                    <span class="facture-label">Montant</span>
                    <span class="facture-value montant">{{ resultat.montant | number:'1.2-2' }} TND</span>
                  </div>
                </div>
                <div class="meta-row" *ngIf="resultat.dateSoumissionFacture">
                  📅 Soumise le {{ resultat.dateSoumissionFacture | date:'dd/MM/yyyy à HH:mm' }}
                </div>
              </div>
              <ng-template #noFacture>
                <div class="empty-state-inline">Aucune facture soumise.</div>
              </ng-template>
              <button class="btn-edit" [class.btn-edit-rejet]="factureRejetee" *ngIf="peutModifier"
                (click)="ouvrirEditionFacture()">
                ✏️ {{ resultat?.factureRef ? (factureRejetee ? 'Corriger la facture' : 'Modifier') : 'Saisir' }} la facture
              </button>
            </ng-container>

            <!-- Mode édition -->
            <ng-container *ngIf="editFacture">
              <div class="motif-inline" *ngIf="factureRejetee && motifRejet">
                ⚠️ Motif du rejet : <strong>{{ motifRejet }}</strong>
              </div>
              <div class="form-group">
                <label class="form-label">Référence facture</label>
                <input class="input-field" type="text" [(ngModel)]="factureRefForm" placeholder="FAC-2024-001" />
              </div>
              <div class="form-group">
                <label class="form-label">Montant (TND)</label>
                <input class="input-field" type="number" [(ngModel)]="montantForm" placeholder="0.00" min="0" step="0.01" />
              </div>
              <div class="btn-row">
                <button class="btn-cancel" (click)="editFacture = false">Annuler</button>
                <button class="btn-save" (click)="soumettreResultat()" [disabled]="envoiEnCours">
                  {{ envoiEnCours ? '⏳ Envoi...' : (factureRejetee ? '🔄 Resoumettre' : '💾 Enregistrer') }}
                </button>
              </div>
            </ng-container>
          </div>
        </div>

        <!-- Carte Documents -->
        <div class="card docs-card">
          <div class="card-header teal-header">
            <h2>📎 Documents</h2>
            <span class="doc-count">{{ fichiers.length }} fichier(s)</span>
          </div>
          <div class="card-body">
            <div *ngIf="fichiers.length > 0">
              <div class="doc-item" *ngFor="let d of fichiers">
                <div class="doc-left">
                  <span class="doc-icone">{{ iconeType(d.typeMime) }}</span>
                  <div class="doc-info">
                    <span class="doc-nom">{{ d.nomFichierOriginal }}</span>
                    <span class="doc-meta">
                      {{ formatTaille(d.tailleFichier) }} · {{ d.dateUpload | date:'dd/MM/yyyy HH:mm' }}
                    </span>
                  </div>
                </div>
                <button class="btn-dl"
                  (click)="telecharger(d.nomFichierServeur, d.nomFichierOriginal)"
                  [disabled]="telechargementEnCours[d.nomFichierServeur]">
                  {{ telechargementEnCours[d.nomFichierServeur] ? '⏳' : '⬇️' }} Télécharger
                </button>
              </div>
            </div>
            <div *ngIf="fichiers.length === 0" class="empty-state-inline">
              Aucun document envoyé pour cette mission.
            </div>
            <div class="upload-zone" *ngIf="peutModifier">
              <label class="upload-label">
                📤 Ajouter un document
                <input type="file" multiple (change)="onFichiersChoisis($event)" class="file-input" />
              </label>
              <div *ngIf="fichiersAUploader.length > 0" class="fichiers-en-attente">
                <div class="fichier-attente" *ngFor="let f of fichiersAUploader">
                  {{ iconeType(f.type) }} {{ f.name }} ({{ formatTaille(f.size) }})
                </div>
                <button class="btn-save mt-8" (click)="uploadDocuments()" [disabled]="uploadEnCours">
                  {{ uploadEnCours ? '⏳ Upload...' : '📤 Envoyer' }}
                </button>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  `,
  styles: [`
    * { box-sizing: border-box; }
    .page-container { font-family: 'Inter', sans-serif; padding: 30px; max-width: 1100px; margin: 0 auto; }

    .page-header { display: flex; align-items: flex-start; gap: 20px; margin-bottom: 30px; }
    .btn-back { padding: 10px 18px; background: #f5f5f5; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; color: #555; transition: all .2s; white-space: nowrap; }
    .btn-back:hover { background: #e0e0e0; }
    .header-info h1 { margin: 0; font-size: 1.7rem; color: #1a237e; }
    .subtitle { margin: 6px 0 0; color: #777; display: flex; align-items: center; gap: 10px; }

    .loading-box { display: flex; flex-direction: column; align-items: center; padding: 60px; color: #777; gap: 16px; }
    .spinner { width: 40px; height: 40px; border: 4px solid #e0e0e0; border-top-color: #1a237e; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    .content-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
    .docs-card { grid-column: 1 / -1; }

    .card { background: white; border-radius: 14px; box-shadow: 0 4px 20px rgba(0,0,0,0.07); overflow: hidden; transition: box-shadow .3s; }
    .card-header { padding: 18px 24px; display: flex; align-items: center; justify-content: space-between; }
    .card-header h2 { margin: 0; color: white; font-size: 1.05rem; }
    .header-badges { display: flex; align-items: center; gap: 8px; }
    .blue-header   { background: linear-gradient(135deg, #1565c0, #1976d2); }
    .indigo-header { background: linear-gradient(135deg, #1a237e, #283593); }
    .orange-header { background: linear-gradient(135deg, #e65100, #f57c00); }
    .green-header  { background: linear-gradient(135deg, #1b5e20, #2e7d32); }
    .teal-header   { background: linear-gradient(135deg, #004d40, #00695c); }
    .red-header    { background: linear-gradient(135deg, #b71c1c, #c62828); }
    .card-body { padding: 24px; }

    .facture-rejetee-card { box-shadow: 0 0 0 2px #e53935, 0 4px 20px rgba(229,57,53,0.15); }

    .rejet-banner {
      display: flex; align-items: flex-start; gap: 16px;
      background: #fff3f3; border: 1.5px solid #e53935; border-radius: 12px;
      padding: 18px 22px; margin-bottom: 24px;
      animation: slideDown .3s ease;
    }
    @keyframes slideDown { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }
    .rejet-icon { font-size: 2rem; flex-shrink: 0; }
    .rejet-content { flex: 1; }
    .rejet-content strong { display: block; font-size: 1rem; color: #b71c1c; margin-bottom: 6px; }
    .rejet-content p { margin: 4px 0; font-size: .9rem; color: #555; }
    .rejet-content em { color: #c62828; font-style: normal; font-weight: 500; }

    .badge-rejet { background: rgba(255,255,255,0.25); color: white; padding: 4px 10px; border-radius: 20px; font-size: .8rem; font-weight: 600; }
    .motif-inline { background: #fff3f3; border-left: 4px solid #e53935; border-radius: 6px; padding: 10px 14px; margin-bottom: 16px; font-size: .88rem; color: #555; }
    .btn-edit-rejet { background: #fce4e4 !important; color: #b71c1c !important; }
    .btn-edit-rejet:hover { background: #ffcdd2 !important; }

    .badge-present { background: rgba(255,255,255,0.25); color: white; padding: 4px 10px; border-radius: 20px; font-size: .8rem; font-weight: 600; }
    .badge-absent  { background: rgba(0,0,0,0.15); color: rgba(255,255,255,0.85); padding: 4px 10px; border-radius: 20px; font-size: .8rem; }
    .doc-count     { background: rgba(255,255,255,0.25); color: white; padding: 4px 12px; border-radius: 20px; font-size: .85rem; font-weight: 600; }

    .statut-badge { padding: 4px 12px; border-radius: 15px; font-size: .8rem; font-weight: bold; }
    .s-assignee         { background: #fff3cd; color: #856404; }
    .s-en-cours         { background: #cce5ff; color: #004085; }
    .s-pv-soumis        { background: #d1ecf1; color: #0c5460; }
    .s-facture          { background: #d4edda; color: #155724; }
    .s-terminee         { background: #e2e3e5; color: #383d41; }
    .s-facture-rejetee  { background: #fdecea; color: #c62828; }

    .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    .info-item { display: flex; flex-direction: column; gap: 4px; }
    .info-label { font-size: .8rem; color: #999; text-transform: uppercase; letter-spacing: .5px; }
    .info-value { font-size: .95rem; font-weight: 600; color: #333; }

    .text-content { background: #f8f9fa; border-radius: 8px; padding: 16px; line-height: 1.7; color: #333; white-space: pre-wrap; font-size: .95rem; margin-bottom: 12px; border-left: 4px solid #1a237e; }
    .meta-row { font-size: .82rem; color: #999; margin-top: 6px; }

    .facture-grid { display: flex; gap: 40px; margin-bottom: 16px; }
    .facture-bloc { display: flex; flex-direction: column; gap: 6px; }
    .facture-label { font-size: .8rem; color: #999; text-transform: uppercase; }
    .facture-value { font-size: 1.1rem; font-weight: 700; color: #333; }
    .facture-value.ref     { color: #1a237e; }
    .facture-value.montant { color: #2e7d32; font-size: 1.4rem; }

    .doc-item { display: flex; justify-content: space-between; align-items: center; padding: 14px 0; border-bottom: 1px solid #f0f0f0; }
    .doc-item:last-child { border-bottom: none; }
    .doc-left  { display: flex; align-items: center; gap: 14px; }
    .doc-icone { font-size: 1.8rem; }
    .doc-info  { display: flex; flex-direction: column; gap: 3px; }
    .doc-nom   { font-weight: 600; color: #333; font-size: .95rem; }
    .doc-meta  { font-size: .8rem; color: #999; }
    .btn-dl { padding: 8px 16px; background: #e3f2fd; color: #0d47a1; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: .85rem; transition: all .2s; white-space: nowrap; }
    .btn-dl:hover    { background: #bbdefb; }
    .btn-dl:disabled { opacity: .6; cursor: not-allowed; }

    .textarea-field { width: 100%; border: 1.5px solid #e0e0e0; border-radius: 8px; padding: 12px; font-size: .95rem; font-family: inherit; resize: vertical; outline: none; transition: border .2s; }
    .textarea-field:focus { border-color: #1a237e; }
    .input-field { width: 100%; border: 1.5px solid #e0e0e0; border-radius: 8px; padding: 10px 14px; font-size: .95rem; outline: none; transition: border .2s; }
    .input-field:focus { border-color: #1a237e; }
    .form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
    .form-label { font-size: .82rem; font-weight: 600; color: #555; text-transform: uppercase; letter-spacing: .4px; }

    .btn-row { display: flex; gap: 10px; margin-top: 14px; justify-content: flex-end; }
    .btn-edit { margin-top: 14px; padding: 8px 16px; background: #ede7f6; color: #4527a0; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: .85rem; transition: all .2s; }
    .btn-edit:hover { background: #d1c4e9; }
    .btn-cancel { padding: 9px 18px; background: #f5f5f5; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; color: #555; }
    .btn-cancel:hover { background: #e0e0e0; }
    .btn-save { padding: 9px 20px; background: #1a237e; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: .9rem; transition: background .2s; }
    .btn-save:hover    { background: #283593; }
    .btn-save:disabled { opacity: .6; cursor: not-allowed; }
    .mt-8 { margin-top: 8px; }

    .upload-zone { margin-top: 20px; border: 2px dashed #b0bec5; border-radius: 10px; padding: 16px; }
    .upload-label { display: block; cursor: pointer; color: #1565c0; font-weight: 600; font-size: .9rem; text-align: center; }
    .file-input { display: none; }
    .fichiers-en-attente { margin-top: 12px; display: flex; flex-direction: column; gap: 6px; }
    .fichier-attente { font-size: .85rem; color: #555; background: #f5f5f5; padding: 6px 10px; border-radius: 6px; }

    .error-banner   { background: #fdecea; color: #c62828; padding: 14px 18px; border-radius: 8px; border-left: 4px solid #c62828; margin-bottom: 20px; }
    .success-banner { background: #e8f5e9; color: #2e7d32; padding: 14px 18px; border-radius: 8px; border-left: 4px solid #2e7d32; margin-bottom: 20px; }
    .empty-state-inline { color: #bbb; font-style: italic; padding: 10px 0; font-size: .9rem; }

    @media (max-width: 768px) {
      .content-grid { grid-template-columns: 1fr; }
      .docs-card    { grid-column: 1; }
      .info-grid    { grid-template-columns: 1fr; }
      .facture-grid { flex-direction: column; gap: 16px; }
    }
  `]
})
export class MissionResultatsComponent implements OnInit, AfterViewInit {

  missionId!: number;
  mission:    any = null;
  resultat:   any = null;
  fichiers:   any[] = [];
  loading    = true;
  erreur     = '';
  successMsg = '';
  telechargementEnCours: Record<string, boolean> = {};

  // ✅ factureRejetee est désormais dérivé du statut backend + query param
  factureRejetee = false;
  motifRejet     = '';

  editPV          = false;
  editFacture     = false;
  editCommentaire = false;

  pvForm          = '';
  commentaireForm = '';
  factureRefForm  = '';
  montantForm:    number | null = null;

  envoiEnCours  = false;
  uploadEnCours = false;
  fichiersAUploader: File[] = [];

  private readonly STATUTS_EDITABLES = [
    'ASSIGNEE', 'EN_COURS', 'PV_SOUMIS', 'FACTURE_SOUMISE', 'FACTURE_REJETEE'
  ];

  @ViewChild('factureCard') factureCard!: ElementRef;

  // ✅ peutModifier : true si statut éditable OU si facture rejetée
  get peutModifier(): boolean {
    if (this.factureRejetee) return true;
    return this.mission && this.STATUTS_EDITABLES.includes(this.mission.statut);
  }

  constructor(
    private route:              ActivatedRoute,
    private router:             Router,
    private prestataireService: PrestataireService
  ) {}

  ngOnInit() {
    this.missionId = Number(this.route.snapshot.paramMap.get('id'));

    // Query params (navigation depuis notification)
    const action = this.route.snapshot.queryParamMap.get('action');
    const motif  = this.route.snapshot.queryParamMap.get('motif');
    if (action === 'edit-facture') {
      this.motifRejet = motif ? decodeURIComponent(motif) : '';
      // factureRejetee sera confirmé après le chargement via mission.statut
    }

    this.chargerDonnees();
  }

  ngAfterViewInit(): void {
    // ViewChild disponible ici — scroll déclenché depuis chargerDonnees() via setTimeout
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

        // ✅ SOURCE UNIQUE DE VÉRITÉ : détecter le rejet depuis mission.statut
        // Fonctionne que l'on vienne d'une notification OU d'une navigation directe
        if (mission?.statut === 'FACTURE_REJETEE') {
          this.factureRejetee = true;
          // motifRejet peut venir du query param (notification) ou du résultat (si le backend l'expose)
          if (!this.motifRejet && resultat?.motifRejet) {
            this.motifRejet = resultat.motifRejet;
          }
        } else {
          // Réinitialiser si le statut a changé (ex : après re-soumission)
          this.factureRejetee = false;
          this.motifRejet     = '';
        }

        // Ouvrir automatiquement l'édition + scroll si rejet
        if (this.factureRejetee) {
          setTimeout(() => {
            this.ouvrirEditionFacture();
            setTimeout(() => {
              this.factureCard?.nativeElement?.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }, 150);
          }, 50);
        }
      },
      error: (err) => {
        this.erreur  = err?.error?.error || 'Erreur lors du chargement des résultats.';
        this.loading = false;
      }
    });
  }

  ouvrirEditionFacture() {
    this.editFacture    = true;
    this.factureRefForm = this.resultat?.factureRef || '';
    this.montantForm    = this.resultat?.montant    ?? null;
  }

  soumettreResultat() {
    this.envoiEnCours = true;
    this.erreur       = '';
    this.successMsg   = '';

    const payload = new FormData();
    payload.append('pvTexte',     this.editPV          ? this.pvForm          : (this.resultat?.pvTexte     ?? ''));
    payload.append('commentaire', this.editCommentaire ? this.commentaireForm : (this.resultat?.commentaire ?? ''));
    payload.append('factureRef',  this.editFacture     ? this.factureRefForm  : (this.resultat?.factureRef  ?? ''));
    payload.append('montant',     String(this.editFacture ? (this.montantForm ?? 0) : (this.resultat?.montant ?? 0)));

    this.fichiersAUploader.forEach(f => payload.append('fichiers', f));

    const appel$ = this.resultat?.id
      ? this.prestataireService.modifierResultat(this.missionId, payload)
      : this.prestataireService.soumettreResultat(this.missionId, payload);

    appel$.subscribe({
      next: (res: any) => {
        this.resultat          = res;
        this.editPV            = false;
        this.editFacture       = false;
        this.editCommentaire   = false;
        this.envoiEnCours      = false;
        this.fichiersAUploader = [];
        this.successMsg        = '✅ Facture resoumise avec succès.';
        setTimeout(() => this.successMsg = '', 4000);
        // chargerDonnees va remettre factureRejetee à false si le statut a changé
        this.chargerDonnees();
      },
      error: (err: any) => {
        this.erreur       = err?.error?.message || err?.error?.error || 'Erreur lors de la soumission.';
        this.envoiEnCours = false;
      }
    });
  }

  onFichiersChoisis(event: Event) {
    const input = event.target as HTMLInputElement;
    this.fichiersAUploader = input.files ? Array.from(input.files) : [];
  }

  uploadDocuments() {
    if (!this.fichiersAUploader.length) return;
    this.uploadEnCours = true;
    this.erreur        = '';

    const formData = new FormData();
    this.fichiersAUploader.forEach(f => formData.append('fichiers', f));

    this.prestataireService.uploaderDocuments(this.missionId, formData).subscribe({
      next: () => {
        this.uploadEnCours     = false;
        this.fichiersAUploader = [];
        this.successMsg        = '✅ Documents envoyés avec succès.';
        setTimeout(() => this.successMsg = '', 4000);
        this.chargerDonnees();
      },
      error: (err) => {
        this.erreur        = err?.error?.message || 'Erreur lors de l\'upload.';
        this.uploadEnCours = false;
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
      FACTURE_REJETEE: 's-facture-rejetee',
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
    if (!taille)            return '0 B';
    if (taille < 1024)      return taille + ' B';
    if (taille < 1024*1024) return (taille / 1024).toFixed(1) + ' KB';
    return (taille / (1024*1024)).toFixed(1) + ' MB';
  }
}