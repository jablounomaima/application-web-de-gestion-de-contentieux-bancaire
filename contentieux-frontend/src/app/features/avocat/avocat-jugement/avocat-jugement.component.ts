import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AvocatService } from '../../../core/services/avocat.service';

@Component({
  selector: 'app-avocat-jugement',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-container">

      <!-- Header -->
      <div class="page-header">
        <button class="btn-back" (click)="retour()">← Retour</button>
        <div class="title-group">
          <span class="role-badge">JUGEMENT</span>
          <h1>Gestion du Jugement</h1>
          <p class="subtitle" *ngIf="affaire">
            Affaire <strong>#{{ affaire.id }}</strong>
            <span class="status-pill" [class]="affaire.statut?.toLowerCase()">
              {{ affaire.statut }}
            </span>
          </p>
        </div>
      </div>

      <!-- Loader -->
      <div class="loader-state" *ngIf="loading">
        <div class="spinner"></div>
        <p>Chargement des données...</p>
      </div>

      <div class="content-grid" *ngIf="!loading">

        <!-- ══════════ Colonne gauche : Jugement enregistré ══════════ -->
        <div class="side-panel">

          <!-- Jugement existant -->
          <div class="info-card" *ngIf="hasJugement()">
            <h3 class="panel-title">⚖️ Jugement enregistré</h3>

            <div class="jugement-summary">
              <span class="jugement-badge"
                [ngClass]="jugementBadgeClass(affaire.typeJugement)">
                {{ jugementIcon(affaire.typeJugement) }} {{ jugementLabel(affaire.typeJugement) }}
              </span>

              <div class="info-row">
                <span class="info-label">Date</span>
                <span class="info-value">
                  {{ affaire.dateJugement | date:'dd/MM/yyyy' }}
                </span>
              </div>

              <div class="info-row" *ngIf="affaire.montantJuge">
                <span class="info-label">Montant jugé</span>
                <span class="info-value amount">{{ affaire.montantJuge }} TND</span>
              </div>

              <div class="info-row" *ngIf="affaire.delaiPaiementJuge">
                <span class="info-label">Délai paiement</span>
                <span class="info-value">{{ affaire.delaiPaiementJuge }}</span>
              </div>

              <div class="info-row" *ngIf="affaire.dateLimiteAppel">
                <span class="info-label">Limite d'appel</span>
                <span class="info-value">
                  {{ affaire.dateLimiteAppel | date:'dd/MM/yyyy' }}
                </span>
              </div>

              <div class="description-block" *ngIf="affaire.descriptionJugement">
                <span class="info-label">Dispositif</span>
                <p class="description-text">{{ affaire.descriptionJugement }}</p>
              </div>
            </div>

            <!-- Actions sur jugement existant -->
            <div class="jugement-actions">
              <button class="btn-edit"
                      (click)="editerJugement()"
                      *ngIf="!editMode">
                ✏️ Modifier
              </button>
              <button class="btn-danger"
                      (click)="supprimerJugement()"
                      [disabled]="deleting">
                {{ deleting ? '...' : '🗑️ Supprimer' }}
              </button>
            </div>
          </div>

          <!-- Pas de jugement -->
          <div class="info-card empty-jugement" *ngIf="!hasJugement()">
            <div class="empty-icon">⚖️</div>
            <p class="empty-text">Aucun jugement enregistré</p>
            <p class="empty-sub">Utilisez le formulaire ci-contre pour saisir le jugement rendu.</p>
          </div>

          <!-- Infos affaire -->
          <div class="info-card" *ngIf="affaire">
            <h3 class="panel-title">📋 Informations affaire</h3>
            <div class="info-row">
              <span class="info-label">Numéro</span>
              <span class="info-value"><code>{{ affaire.numeroAffaire }}</code></span>
            </div>
            <div class="info-row" *ngIf="affaire.tribunal">
              <span class="info-label">Tribunal</span>
              <span class="info-value">{{ affaire.tribunal }}</span>
            </div>
            <div class="info-row" *ngIf="affaire.chambre">
              <span class="info-label">Chambre</span>
              <span class="info-value">{{ affaire.chambre }}</span>
            </div>
            <div class="info-row" *ngIf="affaire.numeroRole">
              <span class="info-label">N° Rôle</span>
              <span class="info-value">{{ affaire.numeroRole }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Statut</span>
              <span class="info-value">
                <span class="status-pill" [class]="affaire.statut?.toLowerCase()">
                  {{ affaire.statut }}
                </span>
              </span>
            </div>
          </div>

        </div>

        <!-- ══════════ Colonne droite : Formulaire ══════════ -->
        <div class="form-panel" *ngIf="!hasJugement() || editMode">
          <div class="form-card">
            <div class="form-card-header">
              <h2>{{ editMode ? '✏️ Modifier le Jugement' : '➕ Saisir un Jugement' }}</h2>
              <p class="form-subtitle">
                Renseignez les informations relatives à la décision judiciaire rendue.
              </p>
            </div>

            <div class="form-body">

              <!-- Type de Jugement -->
              <div class="field">
                <label class="field-label">
                  Type de Jugement <span class="required">*</span>
                </label>
                <div class="type-selector">
                  <button *ngFor="let type of typesJugement"
                          class="type-btn"
                          [class.selected]="jugement.typeJugement === type.value"
                          (click)="jugement.typeJugement = type.value">
                    <span class="type-icon">{{ type.icon }}</span>
                    <span class="type-label">{{ type.label }}</span>
                    <span class="type-desc">{{ type.desc }}</span>
                  </button>
                </div>
              </div>

              <!-- Date + Montant -->
              <div class="form-grid-2">
                <div class="field">
                  <label class="field-label">
                    Date du Jugement <span class="required">*</span>
                  </label>
                  <input type="date"
                         [(ngModel)]="jugement.dateJugement"
                         class="field-input"
                         [class.error]="submitted && !jugement.dateJugement">
                  <span class="error-msg"
                        *ngIf="submitted && !jugement.dateJugement">
                    La date est obligatoire
                  </span>
                </div>
                <div class="field">
                  <label class="field-label">Montant Jugé (TND)</label>
                  <div class="input-with-suffix">
                    <input type="text"
                           [(ngModel)]="jugement.montantJuge"
                           placeholder="0.000"
                           class="field-input">
                    <span class="suffix">TND</span>
                  </div>
                </div>
              </div>

              <!-- Description -->
              <div class="field">
                <label class="field-label">
                  Résultat / Dispositif <span class="required">*</span>
                </label>
                <textarea [(ngModel)]="jugement.descriptionJugement"
                          rows="6"
                          class="field-input textarea"
                          placeholder="Décrivez le dispositif du jugement..."
                          [class.error]="submitted && !jugement.descriptionJugement">
                </textarea>
                <span class="error-msg"
                      *ngIf="submitted && !jugement.descriptionJugement">
                  Le dispositif est obligatoire
                </span>
                <span class="char-count">
                  {{ jugement.descriptionJugement.length || 0 }} caractères
                </span>
              </div>

              <!-- Date limite appel -->
              <div class="field">
                <label class="field-label">Date Limite d'Appel (indicatif)</label>
                <input type="date"
                       [(ngModel)]="jugement.dateLimiteAppel"
                       class="field-input">
                <span class="field-hint">
                  💡 En Tunisie, le délai d'appel est généralement de 30 jours.
                </span>
              </div>

              <!-- Calcul auto -->
              <div class="smart-hint"
                   *ngIf="jugement.dateJugement && !jugement.dateLimiteAppel">
                <span class="hint-icon">🔔</span>
                <div>
                  <strong>Calcul automatique disponible</strong>
                  <p>Date limite suggérée :
                    <strong>{{ getDateLimiteAuto() | date:'dd/MM/yyyy' }}</strong>
                    <button class="btn-link" (click)="appliquerDateAuto()">
                      Appliquer
                    </button>
                  </p>
                </div>
              </div>

              <!-- Actions -->
              <div class="form-actions">
                <button class="btn-secondary"
                        (click)="annuler()">
                  Annuler
                </button>
                <button class="btn-primary"
                        (click)="enregistrerJugement()"
                        [disabled]="saving">
                  <span *ngIf="!saving">
                    ✓ {{ editMode ? 'Mettre à jour' : 'Enregistrer' }}
                  </span>
                  <span *ngIf="saving">Enregistrement...</span>
                </button>
              </div>

            </div>
          </div>
        </div>

        <!-- Message si jugement existant et pas en mode édition -->
        <div class="form-panel" *ngIf="hasJugement() && !editMode">
          <div class="success-card">
            <div class="success-icon">✅</div>
            <h3>Jugement enregistré</h3>
            <p>Le jugement a été saisi avec succès. Vous pouvez le modifier ou le supprimer depuis le panneau de gauche.</p>
            <button class="btn-primary" (click)="editerJugement()">
              ✏️ Modifier le jugement
            </button>
          </div>
        </div>

      </div>

      <!-- Toast -->
      <div class="toast"
           [class.show]="toastVisible"
           [ngClass]="toastType">
        {{ toastMessage }}
      </div>

    </div>
  `,
  styles: [`
    * { box-sizing: border-box; }

    .page-container {
      padding: 30px;
      background: #f0f2f5;
      min-height: 100vh;
      font-family: 'Inter', sans-serif;
    }

    /* ── Header ── */
    .page-header { display: flex; align-items: flex-start; gap: 20px; margin-bottom: 35px; flex-wrap: wrap; }
    .btn-back { background: white; border: 1px solid #e2e8f0; color: #475569; padding: 10px 18px; border-radius: 10px; font-weight: 700; cursor: pointer; transition: all 0.2s; margin-top: 4px; }
    .btn-back:hover { background: #f1f5f9; }
    .title-group { flex: 1; }
    .role-badge { background: #fef3c7; color: #92400e; padding: 4px 10px; border-radius: 6px; font-weight: 800; font-size: 0.7rem; letter-spacing: 1px; display: inline-block; margin-bottom: 6px; }
    h1 { margin: 0; font-size: 2rem; color: #1e293b; font-weight: 800; }
    .subtitle { color: #64748b; margin-top: 6px; font-size: 0.95rem; display: flex; align-items: center; gap: 10px; }
    .status-pill { padding: 4px 10px; border-radius: 8px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; background: #f1f5f9; color: #64748b; }
    .status-pill.en_cours { background: #e0f2fe; color: #0369a1; }
    .status-pill.jugement_rendu { background: #dcfce7; color: #15803d; }

    /* ── Loader ── */
    .loader-state { display: flex; flex-direction: column; align-items: center; padding: 80px 0; color: #94a3b8; }
    .spinner { width: 40px; height: 40px; border: 3px solid #e2e8f0; border-top-color: #4338ca; border-radius: 50%; animation: spin 0.8s linear infinite; margin-bottom: 15px; }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* ── Layout ── */
    .content-grid { display: grid; grid-template-columns: 340px 1fr; gap: 25px; align-items: start; }
    @media (max-width: 900px) { .content-grid { grid-template-columns: 1fr; } }

    /* ── Cards ── */
    .info-card { background: white; border-radius: 20px; padding: 25px; box-shadow: 0 4px 15px rgba(0,0,0,0.04); border: 1px solid #e2e8f0; margin-bottom: 20px; }
    .panel-title { margin: 0 0 20px; font-size: 1rem; color: #1e293b; font-weight: 700; padding-bottom: 15px; border-bottom: 1px solid #f1f5f9; }

    /* ── Jugement affiché ── */
    .jugement-summary { margin-top: 5px; }
    .jugement-badge { display: inline-block; padding: 6px 14px; border-radius: 8px; font-size: 0.78rem; font-weight: 800; margin-bottom: 16px; }
    .jugement-badge.condamnation { background: #dcfce7; color: #15803d; }
    .jugement-badge.rejet        { background: #fee2e2; color: #b91c1c; }
    .jugement-badge.partiel      { background: #fef3c7; color: #92400e; }
    .jugement-badge.mixte        { background: #e0f2fe; color: #0369a1; }

    .info-row { display: flex; justify-content: space-between; align-items: flex-start; padding: 10px 0; border-bottom: 1px solid #f8fafc; gap: 10px; }
    .info-row:last-child { border-bottom: none; }
    .info-label { font-size: 0.78rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; white-space: nowrap; }
    .info-value { font-size: 0.9rem; color: #1e293b; font-weight: 600; text-align: right; }
    .info-value code { background: #f1f5f9; padding: 3px 8px; border-radius: 5px; font-family: monospace; font-size: 0.82rem; }
    .info-value.amount { color: #15803d; font-weight: 800; font-size: 1rem; }

    .description-block { margin-top: 12px; }
    .description-text { margin: 8px 0 0; font-size: 0.85rem; color: #475569; line-height: 1.6; background: #f8fafc; padding: 12px 14px; border-radius: 8px; border-left: 3px solid #4338ca; }

    /* ── Actions jugement ── */
    .jugement-actions { display: flex; gap: 10px; margin-top: 20px; padding-top: 16px; border-top: 1px solid #f1f5f9; }
    .btn-edit { background: #eef2ff; color: #4338ca; border: none; padding: 9px 18px; border-radius: 8px; font-weight: 700; font-size: 0.85rem; cursor: pointer; transition: all 0.2s; flex: 1; }
    .btn-edit:hover { background: #e0e7ff; }
    .btn-danger { background: #fee2e2; color: #dc2626; border: none; padding: 9px 18px; border-radius: 8px; font-weight: 700; font-size: 0.85rem; cursor: pointer; transition: all 0.2s; flex: 1; }
    .btn-danger:hover:not(:disabled) { background: #fecaca; }
    .btn-danger:disabled { opacity: 0.6; cursor: not-allowed; }

    /* ── Empty ── */
    .empty-jugement { text-align: center; padding: 30px 20px; }
    .empty-icon { font-size: 3rem; margin-bottom: 12px; }
    .empty-text { font-weight: 700; color: #374151; margin: 0 0 6px; font-size: 1rem; }
    .empty-sub  { color: #94a3b8; font-size: 0.85rem; margin: 0; line-height: 1.5; }

    /* ── Success card ── */
    .success-card { background: white; border-radius: 20px; padding: 50px 30px; text-align: center; box-shadow: 0 4px 15px rgba(0,0,0,0.04); border: 1px solid #e2e8f0; }
    .success-icon { font-size: 4rem; margin-bottom: 16px; }
    .success-card h3 { margin: 0 0 10px; font-size: 1.3rem; color: #1e293b; }
    .success-card p { color: #64748b; margin: 0 0 24px; line-height: 1.6; }

    /* ── Form ── */
    .form-card { background: white; border-radius: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); border: 1px solid #e2e8f0; overflow: hidden; }
    .form-card-header { padding: 28px 35px 20px; border-bottom: 1px solid #f1f5f9; background: linear-gradient(135deg, #fafafa 0%, #f1f5f9 100%); }
    .form-card-header h2 { margin: 0; font-size: 1.3rem; color: #1e293b; font-weight: 800; }
    .form-subtitle { margin: 6px 0 0; color: #64748b; font-size: 0.9rem; }
    .form-body { padding: 30px 35px; }

    .field { margin-bottom: 22px; }
    .field-label { display: block; font-size: 0.8rem; font-weight: 700; color: #475569; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 10px; }
    .required { color: #dc2626; }
    .field-input { width: 100%; padding: 12px 16px; border: 1.5px solid #e2e8f0; border-radius: 10px; outline: none; font-family: inherit; font-size: 0.95rem; color: #1e293b; background: #fafafa; transition: all 0.2s; }
    .field-input:focus { border-color: #4338ca; background: white; box-shadow: 0 0 0 3px rgba(67,56,202,0.08); }
    .field-input.error { border-color: #dc2626; }
    .field-input.textarea { resize: vertical; min-height: 130px; line-height: 1.6; }
    .input-with-suffix { position: relative; display: flex; align-items: center; }
    .input-with-suffix .field-input { padding-right: 52px; }
    .suffix { position: absolute; right: 14px; font-size: 0.8rem; font-weight: 700; color: #94a3b8; pointer-events: none; }
    .error-msg { display: block; color: #dc2626; font-size: 0.78rem; margin-top: 6px; font-weight: 600; }
    .char-count { display: block; text-align: right; font-size: 0.75rem; color: #94a3b8; margin-top: 5px; }
    .field-hint { display: block; margin-top: 7px; font-size: 0.78rem; color: #64748b; background: #f8fafc; padding: 8px 12px; border-radius: 7px; border: 1px solid #e2e8f0; }
    .form-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }

    /* ── Type selector ── */
    .type-selector { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
    .type-btn { display: flex; flex-direction: column; align-items: center; padding: 16px 12px; border: 2px solid #e2e8f0; border-radius: 14px; background: #fafafa; cursor: pointer; transition: all 0.2s; text-align: center; gap: 6px; }
    .type-btn:hover { border-color: #c7d2fe; background: #eef2ff; }
    .type-btn.selected { border-color: #4338ca; background: #eef2ff; box-shadow: 0 0 0 3px rgba(67,56,202,0.1); }
    .type-icon  { font-size: 1.8rem; }
    .type-label { font-weight: 800; font-size: 0.82rem; color: #1e293b; text-transform: uppercase; }
    .type-desc  { font-size: 0.72rem; color: #94a3b8; line-height: 1.4; }
    .type-btn.selected .type-label { color: #4338ca; }

    /* ── Smart hint ── */
    .smart-hint { display: flex; align-items: flex-start; gap: 14px; background: #fffbeb; border: 1px solid #fde68a; border-radius: 12px; padding: 16px 20px; margin-bottom: 22px; }
    .hint-icon { font-size: 1.4rem; flex-shrink: 0; }
    .smart-hint strong { display: block; color: #92400e; font-size: 0.85rem; margin-bottom: 4px; }
    .smart-hint p { margin: 0; font-size: 0.85rem; color: #78350f; }
    .btn-link { background: none; border: none; color: #4338ca; font-weight: 700; cursor: pointer; font-size: 0.85rem; padding: 0 0 0 8px; text-decoration: underline; }

    /* ── Form actions ── */
    .form-actions { display: flex; gap: 14px; justify-content: flex-end; padding-top: 10px; border-top: 1px solid #f1f5f9; margin-top: 10px; }
    .btn-primary { background: #1e293b; color: white; border: none; padding: 13px 28px; border-radius: 10px; font-weight: 700; font-size: 0.95rem; cursor: pointer; transition: all 0.2s; min-width: 180px; }
    .btn-primary:hover:not(:disabled) { background: #334155; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(30,41,59,0.3); }
    .btn-primary:disabled { opacity: 0.6; cursor: default; }
    .btn-secondary { background: white; color: #64748b; border: 1.5px solid #e2e8f0; padding: 13px 24px; border-radius: 10px; font-weight: 700; cursor: pointer; transition: all 0.2s; }
    .btn-secondary:hover { background: #f8fafc; }

    /* ── Toast ── */
    .toast { position: fixed; bottom: 30px; right: 30px; padding: 16px 24px; border-radius: 12px; font-weight: 700; font-size: 0.9rem; opacity: 0; transform: translateY(20px); transition: all 0.3s; z-index: 9999; pointer-events: none; min-width: 280px; text-align: center; }
    .toast.show    { opacity: 1; transform: translateY(0); }
    .toast.success { background: #1e293b; color: white; }
    .toast.error   { background: #dc2626; color: white; }
  `]
})
export class AvocatJugementComponent implements OnInit {

  affaireId!: number;
  affaire:    any  = null;
  loading  = true;
  saving   = false;
  deleting = false;
  submitted = false;
  editMode  = false; // ✅ mode édition

  toastVisible = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

  typesJugement = [
    { value: 'CONDAMNATION', icon: '⚖️',  label: 'Condamnation', desc: 'L\'adversaire est condamné' },
    { value: 'REJET',        icon: '🚫',  label: 'Rejet',        desc: 'La demande est rejetée' },
    { value: 'PARTIEL',      icon: '◑',   label: 'Partiel',      desc: 'Partiellement accordée' },
    { value: 'MIXTE',        icon: '🔀',  label: 'Mixte',        desc: 'Décision combinée' }
  ];

  jugement = {
    typeJugement:        'CONDAMNATION',
    dateJugement:        '',
    montantJuge:         '' as string,
    descriptionJugement: '',
    dateLimiteAppel:     ''
  };

  constructor(
    private route:         ActivatedRoute,
    private router:        Router,
    private avocatService: AvocatService
  ) {}

  ngOnInit(): void {
    this.affaireId = Number(this.route.snapshot.paramMap.get('id'));
    this.chargerAffaire();
  }

  // ════════════════════════════════════════
  // Chargement
  // ════════════════════════════════════════
  chargerAffaire(): void {
    this.loading = true;
    this.avocatService.getAffaireDetail(this.affaireId).subscribe({
      next: (aff: any) => {
        console.log('RÉPONSE AFFAIRE:', JSON.stringify(aff, null, 2)); // ✅
        this.affaire = aff;
        if (aff.typeJugement) {
          this.jugement = {
            typeJugement:        aff.typeJugement        || 'CONDAMNATION',
            dateJugement:        aff.dateJugement        || '',
            montantJuge:         aff.montantJuge         || '',
            descriptionJugement: aff.descriptionJugement || '',
            dateLimiteAppel:     aff.dateLimiteAppel     || ''
          };
        }
        this.loading = false;
      },
      error: () => {
        this.showToast('Impossible de charger les données.', 'error');
        this.loading = false;
      }
    });
  }

  // ════════════════════════════════════════
  // Helpers affichage jugement
  // ════════════════════════════════════════

  hasJugement(): boolean {
    return !!this.affaire?.typeJugement;
  }

  jugementLabel(type: string): string {
    const map: Record<string, string> = {
      'CONDAMNATION': 'Condamnation',
      'REJET':        'Rejet',
      'PARTIEL':      'Partiel',
      'MIXTE':        'Mixte'
    };
    return map[type] ?? type;
  }

  jugementIcon(type: string): string {
    const map: Record<string, string> = {
      'CONDAMNATION': '⚖️',
      'REJET':        '🚫',
      'PARTIEL':      '◑',
      'MIXTE':        '🔀'
    };
    return map[type] ?? '⚖️';
  }

  jugementBadgeClass(type: string): string {
    const map: Record<string, string> = {
      'CONDAMNATION': 'condamnation',
      'REJET':        'rejet',
      'PARTIEL':      'partiel',
      'MIXTE':        'mixte'
    };
    return map[type] ?? '';
  }

  // ════════════════════════════════════════
  // Actions
  // ════════════════════════════════════════

  editerJugement(): void {
    this.editMode  = true;
    this.submitted = false;
    // ✅ Pré-remplir le formulaire avec les données existantes
    this.jugement = {
      typeJugement:        this.affaire.typeJugement        || 'CONDAMNATION',
      dateJugement:        this.affaire.dateJugement        || '',
      montantJuge:         this.affaire.montantJuge         || '',
      descriptionJugement: this.affaire.descriptionJugement || '',
      dateLimiteAppel:     this.affaire.dateLimiteAppel     || ''
    };
  }

  annuler(): void {
    this.editMode  = false;
    this.submitted = false;
  }

  enregistrerJugement(): void {
    this.submitted = true;

    if (!this.jugement.dateJugement || !this.jugement.descriptionJugement) {
      this.showToast('Veuillez remplir tous les champs obligatoires.', 'error');
      return;
    }

    this.saving = true;
    const body = {
      typeJugement:        this.jugement.typeJugement,
      dateJugement:        this.jugement.dateJugement,
      montantJuge:         this.jugement.montantJuge || null,
      descriptionJugement: this.jugement.descriptionJugement,
      dateLimiteAppel:     this.jugement.dateLimiteAppel || null
    };

    this.avocatService.enregistrerJugement(this.affaireId, body).subscribe({
      next: () => {
        this.saving    = false;
        this.submitted = false;
        this.editMode  = false; // ✅ quitter le mode édition
        this.showToast('✓ Jugement enregistré avec succès !', 'success');
        setTimeout(() => this.chargerAffaire(), 500); // ✅ recharger l'affichage
      },
      error: (e: any) => {
        this.saving = false;
        this.showToast(
          e.error?.error || e.error?.message || 'Une erreur est survenue.',
          'error'
        );
      }
    });
  }

  supprimerJugement(): void {
    if (!confirm('Êtes-vous sûr de vouloir supprimer ce jugement ?')) return;
    this.deleting = true;

    this.avocatService.supprimerJugement(this.affaireId).subscribe({
      next: () => {
        this.deleting  = false;
        this.editMode  = false;
        this.showToast('Jugement supprimé.', 'success');
        setTimeout(() => this.chargerAffaire(), 500);
      },
      error: (e: any) => {
        this.deleting = false;
        this.showToast(
          e.error?.error || 'Erreur lors de la suppression.',
          'error'
        );
      }
    });
  }

  // ════════════════════════════════════════
  // Date limite appel
  // ════════════════════════════════════════

  getDateLimiteAuto(): Date | null {
    if (!this.jugement.dateJugement) return null;
    const d = new Date(this.jugement.dateJugement);
    d.setDate(d.getDate() + 30);
    return d;
  }

  appliquerDateAuto(): void {
    const d = this.getDateLimiteAuto();
    if (d) this.jugement.dateLimiteAppel = d.toISOString().split('T')[0];
  }

  retour(): void {
    this.router.navigate(['/avocat/dashboard']);
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    this.toastMessage = message;
    this.toastType    = type;
    this.toastVisible = true;
    setTimeout(() => this.toastVisible = false, 3500);
  }
}