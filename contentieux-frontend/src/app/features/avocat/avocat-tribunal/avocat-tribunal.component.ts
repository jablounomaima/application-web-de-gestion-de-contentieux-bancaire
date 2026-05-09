import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AvocatService } from '../../../core/services/avocat.service';

@Component({
  selector: 'app-avocat-tribunal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-container">

      <!-- ══ Header ══ -->
      <div class="page-header">
        <button class="btn-back" (click)="retour()">← Retour</button>
        <div class="title-group">
          <span class="role-badge">🏛️ TRIBUNAL</span>
          <h1>Informations du Tribunal</h1>
          <p class="subtitle" *ngIf="affaire">
            Affaire <strong>#{{ affaire.id }}</strong>
            <span *ngIf="affaire.dossier?.numeroDossier">
              — {{ affaire.dossier.numeroDossier }}
            </span>
            <span class="status-pill" *ngIf="affaire.statut"
                  [ngClass]="statutClass(affaire.statut)">
              {{ affaire.statut }}
            </span>
          </p>
        </div>
      </div>

      <!-- ══ Loader ══ -->
      <div class="loader-state" *ngIf="loading">
        <div class="spinner"></div>
        <p>Chargement des données...</p>
      </div>

      <div class="content-grid" *ngIf="!loading">

        <!-- ════════ COLONNE GAUCHE : Récapitulatif ════════ -->
        <div class="side-panel">

          <!-- Informations actuelles -->
          <div class="info-card tribunal-summary" *ngIf="hasCurrentData()">
            <h3 class="panel-title">🏛️ Informations actuelles</h3>

            <div class="info-row" *ngIf="affaire?.tribunal">
              <span class="info-label">Tribunal</span>
              <span class="info-value">{{ affaire.tribunal }}</span>
            </div>
            <div class="info-row" *ngIf="affaire?.chambre">
              <span class="info-label">Chambre</span>
              <span class="info-value">{{ affaire.chambre }}</span>
            </div>
            <div class="info-row" *ngIf="affaire?.numeroRole">
              <span class="info-label">N° Rôle</span>
              <span class="info-value mono">{{ affaire.numeroRole }}</span>
            </div>
            <div class="info-row" *ngIf="affaire?.degreJuridiction">
              <span class="info-label">Degré</span>
              <span class="info-value">{{ degreLabel(affaire.degreJuridiction) }}</span>
            </div>
          </div>

          <!-- Aucune info -->
          <div class="info-card empty-card" *ngIf="!hasCurrentData()">
            <div class="empty-icon">🏛️</div>
            <p class="empty-text">Aucune information enregistrée</p>
            <p class="empty-sub">
              Utilisez le formulaire ci-contre pour renseigner le tribunal.
            </p>
          </div>

        </div>

        <!-- ════════ COLONNE DROITE : Formulaire ════════ -->
        <div class="form-panel">

          <div class="form-card">
            <div class="form-card-header blue-header">
              <div class="header-num">01</div>
              <div>
                <h2>{{ hasCurrentData() ? '✏️ Modifier le Tribunal' : '➕ Renseigner le Tribunal' }}</h2>
                <p class="form-subtitle">Coordonnées juridictionnelles de l'affaire</p>
              </div>
            </div>

            <div class="form-body">

              <!-- Tribunal -->
              <div class="field">
                <label class="field-label">
                  Nom du Tribunal <span class="required">*</span>
                </label>
                <select
                  [(ngModel)]="form.tribunal"
                  (ngModelChange)="onTribunalChange($event)"
                  class="field-input field-select"
                  [class.field-error]="submitted && !form.tribunal">
                  <option value="" disabled>-- Sélectionner un tribunal --</option>
                  <optgroup *ngFor="let groupe of juridictions" [label]="groupe.groupe">
                    <option *ngFor="let t of groupe.items" [value]="t">{{ t }}</option>
                  </optgroup>
                </select>
                <span class="error-msg" *ngIf="submitted && !form.tribunal">
                  Le nom du tribunal est obligatoire
                </span>
              </div>

              <!-- Chambre + Rôle -->
              <div class="form-grid-2">
                <div class="field">
                  <label class="field-label">Chambre</label>
                  <input
                    type="text"
                    [(ngModel)]="form.chambre"
                    placeholder="Ex: Chambre Civile 2"
                    class="field-input">
                </div>
                <div class="field">
                  <label class="field-label">Numéro de Rôle</label>
                  <input
                    type="text"
                    [(ngModel)]="form.numeroRole"
                    placeholder="Ex: 12345/2024"
                    class="field-input mono-input">
                </div>
              </div>

              <!-- Degré de juridiction -->
              <div class="field">
                <label class="field-label">
                  Degré de Juridiction
                  <span class="auto-badge" *ngIf="degreAutoDetecte">⚡ Détecté automatiquement</span>
                </label>
                <div class="degree-selector">
                  <button
                    *ngFor="let deg of degresJuridiction"
                    type="button"
                    class="degree-btn"
                    [class.selected]="form.degreJuridiction === deg.value"
                    [class.auto]="degreAutoDetecte && form.degreJuridiction === deg.value"
                    [disabled]="degreAutoDetecte"
                    (click)="!degreAutoDetecte && (form.degreJuridiction = deg.value)">
                    <span class="deg-number">{{ deg.number }}</span>
                    <span class="deg-label">{{ deg.label }}</span>
                  </button>
                </div>
                <p class="field-hint" *ngIf="degreAutoDetecte">
                  Degré déduit du tribunal sélectionné. Choisissez un autre tribunal pour modifier manuellement.
                </p>
              </div>

              <!-- Aperçu -->
              <div class="preview-banner" *ngIf="form.tribunal">
                <span class="preview-label">Aperçu</span>
                <span class="preview-content">
                  <strong>{{ form.tribunal }}</strong>
                  <span *ngIf="form.chambre"> — {{ form.chambre }}</span>
                  <span *ngIf="form.numeroRole"> | Rôle : <code>{{ form.numeroRole }}</code></span>
                </span>
              </div>

              <!-- Actions -->
              <div class="form-actions">
                <button class="btn-secondary" (click)="resetForm()">
                  Réinitialiser
                </button>
                <button
                  class="btn-primary blue-btn"
                  (click)="sauvegarder()"
                  [disabled]="saving">
                  <span *ngIf="!saving">✓ Mettre à jour le Tribunal</span>
                  <span *ngIf="saving">Enregistrement...</span>
                </button>
              </div>

            </div>
          </div>

        </div>
      </div>

      <!-- ══ Toast ══ -->
      <div class="toast"
           [class.show]="toastVisible"
           [ngClass]="toastType">
        {{ toastMessage }}
      </div>

    </div>
  `,
  styles: [`
    * { box-sizing: border-box; }

    /* ── Page ── */
    .page-container { padding: 30px; background: #f0f2f5; min-height: 100vh; font-family: 'Inter', sans-serif; }

    /* ── Header ── */
    .page-header { display: flex; align-items: flex-start; gap: 20px; margin-bottom: 35px; flex-wrap: wrap; }
    .btn-back { background: white; border: 1px solid #e2e8f0; color: #475569; padding: 10px 18px; border-radius: 10px; font-weight: 700; cursor: pointer; transition: all 0.2s; margin-top: 4px; }
    .btn-back:hover { background: #f1f5f9; }
    .title-group { flex: 1; }
    .role-badge { background: #dbeafe; color: #1d4ed8; padding: 4px 10px; border-radius: 6px; font-weight: 800; font-size: 0.7rem; letter-spacing: 1px; display: inline-block; margin-bottom: 6px; }
    h1 { margin: 0; font-size: 2rem; color: #1e293b; font-weight: 800; }
    .subtitle { color: #64748b; margin-top: 6px; font-size: 0.95rem; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
    .status-pill { padding: 4px 10px; border-radius: 8px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; }
    .pill-blue  { background: #e0f2fe; color: #0369a1; }
    .pill-green { background: #dcfce7; color: #15803d; }
    .pill-grey  { background: #f1f5f9; color: #64748b; }

    /* ── Loader ── */
    .loader-state { display: flex; flex-direction: column; align-items: center; padding: 80px 0; color: #94a3b8; }
    .spinner { width: 40px; height: 40px; border: 3px solid #e2e8f0; border-top-color: #3b82f6; border-radius: 50%; animation: spin 0.8s linear infinite; margin-bottom: 15px; }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* ── Layout ── */
    .content-grid { display: grid; grid-template-columns: 340px 1fr; gap: 25px; align-items: start; }
    @media (max-width: 900px) { .content-grid { grid-template-columns: 1fr; } }

    /* ── Side cards ── */
    .info-card { background: white; border-radius: 20px; padding: 24px; box-shadow: 0 4px 15px rgba(0,0,0,0.04); border: 1px solid #e2e8f0; margin-bottom: 20px; }
    .tribunal-summary { border-left: 4px solid #3b82f6; }
    .panel-title { margin: 0 0 16px; font-size: 1rem; font-weight: 700; color: #1e293b; padding-bottom: 12px; border-bottom: 1px solid #f1f5f9; }

    .info-row { display: flex; justify-content: space-between; align-items: center; padding: 9px 0; border-bottom: 1px solid #f8fafc; }
    .info-row:last-of-type { border-bottom: none; }
    .info-label { font-size: 0.75rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; }
    .info-value { font-size: 0.9rem; font-weight: 600; color: #1e293b; }
    .info-value.mono { font-family: monospace; }

    /* ── Empty card ── */
    .empty-card { text-align: center; padding: 28px 16px; }
    .empty-icon { font-size: 2.5rem; margin-bottom: 10px; }
    .empty-text { font-weight: 700; color: #374151; margin: 0 0 6px; }
    .empty-sub  { color: #94a3b8; font-size: 0.82rem; margin: 0; line-height: 1.5; }

    /* ── Form card ── */
    .form-card { background: white; border-radius: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); border: 1px solid #e2e8f0; overflow: hidden; margin-bottom: 24px; }
    .form-card-header { padding: 24px 30px 18px; border-bottom: 1px solid #f1f5f9; display: flex; align-items: center; gap: 16px; }
    .blue-header { background: linear-gradient(135deg, #eff6ff, #dbeafe); border-bottom-color: #bfdbfe; }
    .header-num { width: 40px; height: 40px; border-radius: 12px; background: rgba(255,255,255,0.8); display: flex; align-items: center; justify-content: center; font-weight: 900; font-size: 1rem; color: #1d4ed8; flex-shrink: 0; }
    .form-card-header h2 { margin: 0; font-size: 1.1rem; font-weight: 800; color: #1e293b; }
    .form-subtitle { margin: 4px 0 0; color: #64748b; font-size: 0.85rem; }
    .form-body { padding: 28px 30px; }

    /* ── Fields ── */
    .field { margin-bottom: 20px; }
    .field-label { display: block; font-size: 0.8rem; font-weight: 700; color: #475569; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 8px; }
    .required { color: #dc2626; }
    .field-input { width: 100%; padding: 12px 16px; border: 1.5px solid #e2e8f0; border-radius: 10px; outline: none; font-family: inherit; font-size: 0.95rem; color: #1e293b; background: #fafafa; transition: all 0.2s; }
    .field-input:focus { border-color: #3b82f6; background: white; box-shadow: 0 0 0 3px rgba(59,130,246,0.08); }
    .field-input.field-error { border-color: #dc2626; }
    .field-input.mono-input { font-family: monospace; letter-spacing: 0.5px; }
    .form-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    @media (max-width: 600px) { .form-grid-2 { grid-template-columns: 1fr; } }

    /* ── Error ── */
    .error-msg { display: block; color: #dc2626; font-size: 0.78rem; font-weight: 600; margin-top: 6px; }

    /* ── Select ── */
    .field-select { appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='8' viewBox='0 0 12 8'%3E%3Cpath d='M1 1l5 5 5-5' stroke='%2364748b' stroke-width='1.5' fill='none'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 16px center; background-color: #fafafa; padding-right: 44px; cursor: pointer; }
    .field-select:focus { border-color: #3b82f6; background-color: white; box-shadow: 0 0 0 3px rgba(59,130,246,0.08); }

    /* ── Auto-badge & hint ── */
    .auto-badge { background: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe; padding: 2px 8px; border-radius: 20px; font-size: 0.68rem; font-weight: 700; letter-spacing: 0.3px; margin-left: 8px; vertical-align: middle; }
    .field-hint { margin: 8px 0 0; font-size: 0.78rem; color: #64748b; font-style: italic; }
    .degree-btn:disabled { cursor: not-allowed; opacity: 0.55; }
    .degree-btn.auto { border-color: #3b82f6; background: #eff6ff; box-shadow: 0 0 0 3px rgba(59,130,246,0.1); opacity: 1; }
    .degree-btn.auto .deg-number { color: #1d4ed8; }
    .degree-btn.auto .deg-label  { color: #3b82f6; }

    /* ── Degree selector ── */
    .degree-selector { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
    @media (max-width: 600px) { .degree-selector { grid-template-columns: 1fr; } }
    .degree-btn { display: flex; flex-direction: column; align-items: center; padding: 14px 10px; border: 1.5px solid #e2e8f0; border-radius: 12px; background: #fafafa; cursor: pointer; transition: all 0.2s; gap: 5px; font-family: inherit; }
    .degree-btn:hover { border-color: #93c5fd; background: #eff6ff; }
    .degree-btn.selected { border-color: #3b82f6; background: #eff6ff; box-shadow: 0 0 0 3px rgba(59,130,246,0.1); }
    .deg-number { font-size: 1.3rem; font-weight: 800; color: #94a3b8; font-family: monospace; line-height: 1; }
    .degree-btn.selected .deg-number { color: #1d4ed8; }
    .deg-label { font-size: 0.75rem; font-weight: 700; color: #64748b; text-align: center; text-transform: uppercase; letter-spacing: 0.5px; }
    .degree-btn.selected .deg-label { color: #3b82f6; }

    /* ── Preview ── */
    .preview-banner { display: flex; align-items: center; gap: 14px; background: #eff6ff; border: 1px solid #bfdbfe; border-left: 4px solid #3b82f6; border-radius: 10px; padding: 12px 16px; margin-bottom: 20px; }
    .preview-label { font-size: 0.7rem; font-weight: 800; text-transform: uppercase; letter-spacing: 1px; color: #3b82f6; white-space: nowrap; }
    .preview-content { font-size: 0.88rem; color: #1e40af; font-weight: 500; }
    .preview-content code { font-family: monospace; background: #bfdbfe; padding: 2px 6px; border-radius: 4px; font-size: 0.82rem; }

    /* ── Form actions ── */
    .form-actions { display: flex; gap: 12px; justify-content: flex-end; padding-top: 16px; border-top: 1px solid #f1f5f9; margin-top: 8px; }
    .btn-primary { border: none; padding: 13px 28px; border-radius: 10px; font-weight: 700; font-size: 0.95rem; cursor: pointer; transition: all 0.2s; min-width: 220px; color: white; }
    .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
    .blue-btn { background: linear-gradient(135deg, #2563eb, #3b82f6); }
    .blue-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 15px rgba(59,130,246,0.35); }
    .btn-secondary { background: white; color: #64748b; border: 1.5px solid #e2e8f0; padding: 13px 20px; border-radius: 10px; font-weight: 700; cursor: pointer; transition: all 0.2s; }
    .btn-secondary:hover { background: #f8fafc; }

    /* ── Toast ── */
    .toast { position: fixed; bottom: 30px; right: 30px; padding: 14px 24px; border-radius: 12px; font-weight: 700; font-size: 0.9rem; opacity: 0; transform: translateY(20px); transition: all 0.3s; z-index: 9999; pointer-events: none; min-width: 260px; text-align: center; }
    .toast.show    { opacity: 1; transform: translateY(0); }
    .toast.success { background: #059669; color: white; }
    .toast.error   { background: #dc2626; color: white; }
  `]
})
export class AvocatTribunalComponent implements OnInit {

  affaireId!: number;
  affaire: any = null;
  loading = true;
  saving = false;
  submitted = false;

  toastVisible = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

  degreAutoDetecte = false;

  degresJuridiction = [
    { value: '1',    number: '1er',  label: 'Première Instance' },
    { value: '2',    number: '2ème', label: 'Appel'             },
    { value: 'CASS', number: 'C',    label: 'Cassation'         },
  ];

  juridictions: { groupe: string; items: string[] }[] = [
    {
      groupe: 'Juridictions supérieures',
      items: [
        'Cour de Cassation',
        'Tribunal Administratif',
        'Cour des Comptes',
      ]
    },
    {
      groupe: "Cours d'appel",
      items: [
        "Cour d'Appel de Tunis",
        "Cour d'Appel de Bizerte",
        "Cour d'Appel de Nabeul",
        "Cour d'Appel du Kef",
        "Cour d'Appel de Sousse",
        "Cour d'Appel de Monastir",
        "Cour d'Appel de Sfax",
        "Cour d'Appel de Gafsa",
        "Cour d'Appel de Gabès",
        "Cour d'Appel de Médenine",
        "Cour d'Appel de Kairouan",
        "Cour d'Appel de Jendouba",
      ]
    },
    {
      groupe: 'Tribunaux de première instance',
      items: [
        'Tribunal de Première Instance de Tunis',
        'Tribunal de Première Instance de Tunis 2',
        "Tribunal de Première Instance de l'Ariana",
        'Tribunal de Première Instance de Ben Arous',
        'Tribunal de Première Instance de La Manouba',
        'Tribunal de Première Instance de Nabeul',
        'Tribunal de Première Instance de Grombalia',
        'Tribunal de Première Instance de Zaghouan',
        'Tribunal de Première Instance de Bizerte',
        'Tribunal de Première Instance de Béja',
        'Tribunal de Première Instance du Kef',
        'Tribunal de Première Instance de Jendouba',
        'Tribunal de Première Instance de Siliana',
        'Tribunal de Première Instance de Kasserine',
        'Tribunal de Première Instance de Sousse',
        'Tribunal de Première Instance de Sousse 2',
        'Tribunal de Première Instance de Kairouan',
        'Tribunal de Première Instance de Monastir',
        'Tribunal de Première Instance de Mahdia',
        'Tribunal de Première Instance de Sfax',
        'Tribunal de Première Instance de Sfax 2',
        'Tribunal de Première Instance de Gabès',
        'Tribunal de Première Instance de Kébili',
        'Tribunal de Première Instance de Gafsa',
        'Tribunal de Première Instance de Sidi Bouzid',
        'Tribunal de Première Instance de Tozeur',
        'Tribunal de Première Instance de Médenine',
        'Tribunal de Première Instance de Tataouine',
      ]
    },
    {
      groupe: 'Tribunaux cantonaux',
      items: [
        'Tribunal Cantonal de Tunis',
        'Tribunal Cantonal du Bardo',
        'Tribunal Cantonal de Carthage',
        'Tribunal Cantonal de Sousse',
        'Tribunal Cantonal de Sfax',
        'Tribunal Cantonal de Gafsa',
        'Tribunal Cantonal de Msaken',
        'Tribunal Cantonal de Jbeniana',
        'Tribunal Cantonal de Hammam Lif',
      ]
    },
  ];

  form = {
    tribunal:         '',
    chambre:          '',
    numeroRole:       '',
    degreJuridiction: '1',
  };

  private formOriginal = { ...this.form };

  constructor(
    private route:         ActivatedRoute,
    private router:        Router,
    private avocatService: AvocatService
  ) {}

  ngOnInit(): void {
    this.affaireId = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.affaireId || isNaN(this.affaireId)) {
      this.showToast('ID affaire invalide.', 'error');
      return;
    }
    this.chargerAffaire();
  }

  // ════════════════════════════════════════
  // Chargement
  // ════════════════════════════════════════

  chargerAffaire(): void {
    this.loading = true;
    this.avocatService.getAffaireDetail(this.affaireId).subscribe({
      next: (aff: any) => {
        this.affaire = aff;
        this.form = {
          tribunal:         aff.tribunal         || '',
          chambre:          aff.chambre          || '',
          numeroRole:       aff.numeroRole        || '',
          degreJuridiction: aff.degreJuridiction  || '1',
        };
        this.formOriginal = { ...this.form };
        this.onTribunalChange(this.form.tribunal);
        this.loading = false;
      },
      error: () => {
        this.showToast('Impossible de charger les données.', 'error');
        this.loading = false;
      }
    });
  }

  // ════════════════════════════════════════
  // Helpers affichage
  // ════════════════════════════════════════

  hasCurrentData(): boolean {
    return !!(this.affaire?.tribunal || this.affaire?.chambre || this.affaire?.numeroRole);
  }

  statutClass(statut: string): string {
    const map: Record<string, string> = {
      'EN_COURS':       'pill-blue',
      'JUGEMENT_RENDU': 'pill-green',
    };
    return map[statut] ?? 'pill-grey';
  }

  degreLabel(value: string): string {
    const found = this.degresJuridiction.find(d => d.value === value);
    return found ? `${found.number} — ${found.label}` : value;
  }

  onTribunalChange(tribunal: string): void {
    if (!tribunal) {
      this.degreAutoDetecte = false;
      return;
    }
    const t = tribunal.toLowerCase();
    if (
      t.includes('cour de cassation') ||
      t.includes('tribunal administratif') ||
      t.includes('cour des comptes')
    ) {
      this.form.degreJuridiction = 'CASS';
      this.degreAutoDetecte = true;
    } else if (t.includes("cour d'appel")) {
      this.form.degreJuridiction = '2';
      this.degreAutoDetecte = true;
    } else if (
      t.includes('tribunal de première instance') ||
      t.includes('tribunal cantonal')
    ) {
      this.form.degreJuridiction = '1';
      this.degreAutoDetecte = true;
    } else {
      this.degreAutoDetecte = false;
    }
  }

  resetForm(): void {
    this.form = { ...this.formOriginal };
    this.submitted = false;
    this.onTribunalChange(this.form.tribunal);
  }

  retour(): void {
    this.router.navigate(['/avocat/dashboard']);
  }

  // ════════════════════════════════════════
  // Sauvegarde
  // ════════════════════════════════════════

  sauvegarder(): void {
    this.submitted = true;

    if (!this.form.tribunal) {
      this.showToast('Le nom du tribunal est obligatoire.', 'error');
      return;
    }

    this.saving = true;

    const body = {
      tribunal:   this.form.tribunal,
      chambre:    this.form.chambre    || null,
      numeroRole: this.form.numeroRole || null,
    };

    this.avocatService.modifierTribunal(this.affaireId, body).subscribe({
      next: () => {
        this.saving    = false;
        this.submitted = false;
        this.formOriginal = { ...this.form };
        this.showToast('✓ Tribunal mis à jour avec succès !', 'success');
        setTimeout(() => this.chargerAffaire(), 600);
      },
      error: (e: any) => {
        this.saving = false;
        const msg = e?.error?.error || e?.error?.message || e?.message
                    || 'Une erreur est survenue.';
        this.showToast(msg, 'error');
      }
    });
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    this.toastMessage = message;
    this.toastType    = type;
    this.toastVisible = true;
    setTimeout(() => this.toastVisible = false, 3500);
  }
}