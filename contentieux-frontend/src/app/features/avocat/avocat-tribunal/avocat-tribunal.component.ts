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

      <!-- Background decoratif -->
      <div class="bg-decoration">
        <div class="bg-circle c1"></div>
        <div class="bg-circle c2"></div>
        <div class="bg-circle c3"></div>
      </div>

      <!-- Header -->
      <div class="page-header">
        <button class="btn-back" (click)="retour()">
          <span class="back-arrow">←</span> Retour
        </button>
        <div class="title-group">
          <span class="role-badge">🏛️ TRIBUNAL</span>
          <h1>Informations du Tribunal</h1>
          <p class="subtitle" *ngIf="affaire">
            Affaire <strong>#{{ affaire.id }}</strong> —
            {{ affaire.dossier?.numeroDossier || 'N/A' }}
          </p>
        </div>
      </div>

      <!-- Loader -->
      <div class="loader-state" *ngIf="loading">
        <div class="gavel-anim">🏛️</div>
        <p>Chargement des données...</p>
      </div>

      <div class="main-layout" *ngIf="!loading">

        <!-- Carte d'aperçu actuel -->
        <div class="current-card" *ngIf="hasCurrentData()">
          <div class="current-label">Informations actuelles</div>
          <div class="current-grid">
            <div class="current-item" *ngIf="affaire?.tribunal">
              <span class="ci-icon">🏛️</span>
              <div>
                <span class="ci-label">Tribunal</span>
                <span class="ci-value">{{ affaire.tribunal }}</span>
              </div>
            </div>
            <div class="current-item" *ngIf="affaire?.chambre">
              <span class="ci-icon">🚪</span>
              <div>
                <span class="ci-label">Chambre</span>
                <span class="ci-value">{{ affaire.chambre }}</span>
              </div>
            </div>
            <div class="current-item" *ngIf="affaire?.numeroRole">
              <span class="ci-icon">📋</span>
              <div>
                <span class="ci-label">N° Rôle</span>
                <span class="ci-value mono">{{ affaire.numeroRole }}</span>
              </div>
            </div>
            <div class="current-item" *ngIf="affaire?.statut">
              <span class="ci-icon">⚡</span>
              <div>
                <span class="ci-label">Statut</span>
                <span class="status-pill" [class]="affaire.statut?.toLowerCase()">{{ affaire.statut }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Formulaire principal -->
        <div class="form-card">

          <div class="form-card-header">
            <div class="header-icon">🏛️</div>
            <div>
              <h2>Modifier les Coordonnées du Tribunal</h2>
              <p>Mettez à jour les informations juridictionnelles de cette affaire.</p>
            </div>
          </div>

          <div class="form-body">

            <!-- Tribunal -->
            <div class="field-group">
              <label class="field-label">
                <span class="label-icon">🏛️</span>
                Nom du Tribunal
                <span class="required">*</span>
              </label>
              <div class="input-wrapper">
                <input
                  type="text"
                  [(ngModel)]="form.tribunal"
                  placeholder="Ex: Tribunal de Première Instance de Tunis"
                  class="field-input"
                  [class.error]="submitted && !form.tribunal"
                  (focus)="onFocus('tribunal')"
                  (blur)="onBlur()"
                >
                <div class="input-border-anim" [class.active]="focused === 'tribunal'"></div>
              </div>
              <span class="error-msg" *ngIf="submitted && !form.tribunal">
                Le nom du tribunal est obligatoire
              </span>
              <!-- Suggestions tribunaux tunisiens -->
              <div class="suggestions" *ngIf="focused === 'tribunal' && form.tribunal.length === 0">
                <span class="sugg-label">Suggestions :</span>
                <button
                  *ngFor="let t of tribunauxSuggeres"
                  class="sugg-chip"
                  (click)="appliquerSuggestion(t)"
                >{{ t }}</button>
              </div>
            </div>

            <!-- Chambre + Rôle -->
            <div class="form-grid-2">
              <div class="field-group">
                <label class="field-label">
                  <span class="label-icon">🚪</span>
                  Chambre
                </label>
                <div class="input-wrapper">
                  <input
                    type="text"
                    [(ngModel)]="form.chambre"
                    placeholder="Ex: Chambre Civile 2"
                    class="field-input"
                    (focus)="onFocus('chambre')"
                    (blur)="onBlur()"
                  >
                  <div class="input-border-anim" [class.active]="focused === 'chambre'"></div>
                </div>
              </div>

              <div class="field-group">
                <label class="field-label">
                  <span class="label-icon">📋</span>
                  Numéro de Rôle
                </label>
                <div class="input-wrapper">
                  <input
                    type="text"
                    [(ngModel)]="form.numeroRole"
                    placeholder="Ex: 12345/2024"
                    class="field-input mono-input"
                    (focus)="onFocus('role')"
                    (blur)="onBlur()"
                  >
                  <div class="input-border-anim" [class.active]="focused === 'role'"></div>
                </div>
              </div>
            </div>

            <!-- Degré de juridiction -->
            <div class="field-group">
              <label class="field-label">
                <span class="label-icon">⚖️</span>
                Degré de Juridiction
              </label>
              <div class="degree-selector">
                <button
                  *ngFor="let deg of degresJuridiction"
                  class="degree-btn"
                  [class.selected]="form.degreJuridiction === deg.value"
                  (click)="form.degreJuridiction = deg.value"
                >
                  <span class="deg-number">{{ deg.number }}</span>
                  <span class="deg-label">{{ deg.label }}</span>
                </button>
              </div>
            </div>

           

           

            <!-- Aperçu récapitulatif avant soumission -->
            <div class="preview-banner" *ngIf="form.tribunal">
              <span class="preview-label">Aperçu</span>
              <span class="preview-content">
                <strong>{{ form.tribunal }}</strong>
                <span *ngIf="form.chambre"> — {{ form.chambre }}</span>
                <span *ngIf="form.numeroRole"> | Rôle : <code>{{ form.numeroRole }}</code></span>
                <span *ngIf="form.ville"> | {{ form.ville }}</span>
              </span>
            </div>

            <!-- Actions -->
            <div class="form-actions">
              <button class="btn-secondary" (click)="resetForm()">
                ↺ Réinitialiser
              </button>
              <button
                class="btn-primary"
                (click)="sauvegarder()"
                [disabled]="saving"
                [class.saving]="saving"
              >
                <span *ngIf="!saving">✓ Mettre à jour le Tribunal</span>
                <span *ngIf="saving" class="saving-text">
                  <span class="dots"><span>.</span><span>.</span><span>.</span></span>
                  Enregistrement
                </span>
              </button>
            </div>

          </div>
        </div>

      </div>

      <!-- Toast -->
      <div class="toast" [class.show]="toastVisible" [class.success]="toastType==='success'" [class.error]="toastType==='error'">
        <span class="toast-icon">{{ toastType === 'success' ? '✓' : '✕' }}</span>
        {{ toastMessage }}
      </div>

    </div>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700;800&family=DM+Mono:wght@400;500&display=swap');

    * { box-sizing: border-box; }

    .page-container {
      padding: 35px;
      min-height: 100vh;
      font-family: 'DM Sans', sans-serif;
      background: #f4f6f9;
      position: relative;
      overflow-x: hidden;
    }

    /* ── Background décoration ── */
    .bg-decoration { position: fixed; inset: 0; pointer-events: none; z-index: 0; }
    .bg-circle {
      position: absolute;
      border-radius: 50%;
      opacity: 0.04;
    }
    .c1 { width: 600px; height: 600px; background: #1e3a5f; top: -200px; right: -200px; }
    .c2 { width: 400px; height: 400px; background: #4338ca; bottom: 0; left: -100px; }
    .c3 { width: 300px; height: 300px; background: #0f172a; top: 50%; left: 40%; }

    /* Tout le contenu au dessus du bg */
    .page-header, .loader-state, .main-layout { position: relative; z-index: 1; }

    /* ── Header ── */
    .page-header {
      display: flex;
      align-items: flex-start;
      gap: 20px;
      margin-bottom: 32px;
      flex-wrap: wrap;
    }

    .btn-back {
      display: flex;
      align-items: center;
      gap: 8px;
      background: white;
      border: 1.5px solid #e2e8f0;
      color: #475569;
      padding: 10px 20px;
      border-radius: 12px;
      font-family: 'DM Sans', sans-serif;
      font-weight: 700;
      font-size: 0.9rem;
      cursor: pointer;
      transition: all 0.2s;
      margin-top: 4px;
      box-shadow: 0 2px 6px rgba(0,0,0,0.04);
    }
    .btn-back:hover { background: #f8fafc; transform: translateX(-2px); border-color: #cbd5e1; }
    .back-arrow { font-size: 1.1rem; }

    .title-group { flex: 1; }

    .role-badge {
      background: #1e3a5f;
      color: #e2e8f0;
      padding: 5px 12px;
      border-radius: 8px;
      font-weight: 800;
      font-size: 0.7rem;
      letter-spacing: 1.5px;
      display: inline-block;
      margin-bottom: 8px;
    }

    h1 {
      margin: 0;
      font-size: 2rem;
      color: #0f172a;
      font-weight: 800;
      letter-spacing: -0.5px;
    }

    .subtitle {
      color: #64748b;
      margin-top: 5px;
      font-size: 0.95rem;
    }

    /* ── Loader ── */
    .loader-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 100px 0;
      gap: 15px;
      color: #94a3b8;
    }
    .gavel-anim {
      font-size: 3rem;
      animation: pulse 1.5s ease-in-out infinite;
    }
    @keyframes pulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.15); } }

    /* ── Layout ── */
    .main-layout {
      display: flex;
      flex-direction: column;
      gap: 24px;
      max-width: 820px;
      margin: 0 auto;
    }

    /* ── Carte actuelle ── */
    .current-card {
      background: #0f172a;
      border-radius: 20px;
      padding: 24px 30px;
      position: relative;
      overflow: hidden;
    }
    .current-card::before {
      content: '🏛️';
      position: absolute;
      right: 20px;
      top: 50%;
      transform: translateY(-50%);
      font-size: 5rem;
      opacity: 0.06;
    }

    .current-label {
      font-size: 0.7rem;
      font-weight: 800;
      letter-spacing: 2px;
      text-transform: uppercase;
      color: #64748b;
      margin-bottom: 18px;
    }

    .current-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      gap: 20px;
    }

    .current-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
    }

    .ci-icon {
      font-size: 1.3rem;
      flex-shrink: 0;
      margin-top: 2px;
    }

    .ci-label {
      display: block;
      font-size: 0.72rem;
      font-weight: 700;
      color: #475569;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: 4px;
    }

    .ci-value {
      display: block;
      font-size: 0.95rem;
      font-weight: 700;
      color: #e2e8f0;
    }
    .ci-value.mono { font-family: 'DM Mono', monospace; }

    /* ── Status Pills ── */
    .status-pill {
      display: inline-block;
      padding: 4px 10px;
      border-radius: 7px;
      font-size: 0.7rem;
      font-weight: 800;
      text-transform: uppercase;
      background: #334155;
      color: #94a3b8;
    }
    .status-pill.en_cours       { background: #0c4a6e; color: #7dd3fc; }
    .status-pill.jugement_rendu { background: #14532d; color: #86efac; }
    .status-pill.terminee       { background: #334155; color: #e2e8f0; }

    /* ── Form Card ── */
    .form-card {
      background: white;
      border-radius: 22px;
      box-shadow: 0 8px 30px rgba(0,0,0,0.07);
      border: 1px solid #e8edf2;
      overflow: hidden;
    }

    .form-card-header {
      display: flex;
      align-items: flex-start;
      gap: 18px;
      padding: 28px 35px 22px;
      background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
      border-bottom: 1px solid #e8edf2;
    }

    .header-icon {
      font-size: 2.2rem;
      flex-shrink: 0;
      background: #0f172a;
      width: 54px;
      height: 54px;
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .form-card-header h2 {
      margin: 0 0 5px;
      font-size: 1.25rem;
      color: #0f172a;
      font-weight: 800;
    }
    .form-card-header p {
      margin: 0;
      font-size: 0.88rem;
      color: #64748b;
    }

    .form-body { padding: 32px 35px; }

    /* ── Fields ── */
    .field-group { margin-bottom: 24px; }

    .field-label {
      display: flex;
      align-items: center;
      gap: 7px;
      font-size: 0.8rem;
      font-weight: 700;
      color: #374151;
      text-transform: uppercase;
      letter-spacing: 0.6px;
      margin-bottom: 10px;
    }
    .label-icon { font-size: 1rem; }
    .required { color: #dc2626; margin-left: 2px; }

    .input-wrapper { position: relative; }

    .field-input {
      width: 100%;
      padding: 13px 16px;
      border: 1.5px solid #e2e8f0;
      border-radius: 12px;
      outline: none;
      font-family: 'DM Sans', sans-serif;
      font-size: 0.95rem;
      color: #0f172a;
      background: #fafbfc;
      transition: border-color 0.2s, background 0.2s, box-shadow 0.2s;
    }
    .field-input:focus {
      border-color: #1e3a5f;
      background: white;
      box-shadow: 0 0 0 3px rgba(30, 58, 95, 0.08);
    }
    .field-input.error { border-color: #dc2626; }
    .field-input.textarea { resize: vertical; min-height: 90px; line-height: 1.6; }
    .field-input.mono-input { font-family: 'DM Mono', monospace; letter-spacing: 0.5px; }
    .field-select { appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='8' viewBox='0 0 12 8'%3E%3Cpath d='M1 1l5 5 5-5' stroke='%2364748b' stroke-width='1.5' fill='none'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 16px center; padding-right: 44px; cursor: pointer; }

    .input-border-anim {
      position: absolute;
      bottom: 0; left: 10%; right: 10%;
      height: 2px;
      background: #1e3a5f;
      transform: scaleX(0);
      transition: transform 0.3s ease;
      border-radius: 2px;
    }
    .input-border-anim.active { transform: scaleX(1); }

    .error-msg {
      display: block;
      color: #dc2626;
      font-size: 0.78rem;
      font-weight: 600;
      margin-top: 6px;
    }

    /* ── Suggestions ── */
    .suggestions {
      margin-top: 10px;
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      animation: fadeIn 0.2s ease;
    }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: translateY(0); } }

    .sugg-label {
      font-size: 0.75rem;
      color: #94a3b8;
      font-weight: 600;
    }

    .sugg-chip {
      background: #f1f5f9;
      border: 1px solid #e2e8f0;
      color: #475569;
      padding: 5px 12px;
      border-radius: 20px;
      font-size: 0.78rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.15s;
      font-family: 'DM Sans', sans-serif;
    }
    .sugg-chip:hover { background: #1e3a5f; color: white; border-color: #1e3a5f; }

    /* ── Form Grid ── */
    .form-grid-2 {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
    }
    @media (max-width: 600px) { .form-grid-2 { grid-template-columns: 1fr; } }

    /* ── Degree Selector ── */
    .degree-selector {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 12px;
    }
    @media (max-width: 600px) { .degree-selector { grid-template-columns: 1fr; } }

    .degree-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 16px 10px;
      border: 2px solid #e2e8f0;
      border-radius: 14px;
      background: #fafbfc;
      cursor: pointer;
      transition: all 0.2s;
      gap: 6px;
      font-family: 'DM Sans', sans-serif;
    }
    .degree-btn:hover { border-color: #94a3b8; background: #f8fafc; }
    .degree-btn.selected {
      border-color: #1e3a5f;
      background: #0f172a;
      box-shadow: 0 4px 14px rgba(15, 23, 42, 0.2);
    }

    .deg-number {
      font-size: 1.6rem;
      font-weight: 800;
      color: #94a3b8;
      font-family: 'DM Mono', monospace;
      line-height: 1;
    }
    .degree-btn.selected .deg-number { color: #e2e8f0; }

    .deg-label {
      font-size: 0.78rem;
      font-weight: 700;
      color: #64748b;
      text-align: center;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .degree-btn.selected .deg-label { color: #94a3b8; }

    /* ── Preview Banner ── */
    .preview-banner {
      display: flex;
      align-items: center;
      gap: 14px;
      background: #f0f9ff;
      border: 1px solid #bae6fd;
      border-left: 4px solid #0284c7;
      border-radius: 10px;
      padding: 14px 18px;
      margin-bottom: 24px;
      animation: fadeIn 0.3s ease;
    }

    .preview-label {
      font-size: 0.7rem;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 1px;
      color: #0284c7;
      white-space: nowrap;
    }

    .preview-content {
      font-size: 0.88rem;
      color: #0c4a6e;
      font-weight: 500;
    }
    .preview-content code {
      font-family: 'DM Mono', monospace;
      background: #bae6fd;
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 0.82rem;
    }

    /* ── Form Actions ── */
    .form-actions {
      display: flex;
      gap: 14px;
      justify-content: flex-end;
      padding-top: 14px;
      border-top: 1px solid #f1f5f9;
    }

    .btn-primary {
      background: #0f172a;
      color: white;
      border: none;
      padding: 13px 30px;
      border-radius: 12px;
      font-family: 'DM Sans', sans-serif;
      font-weight: 700;
      font-size: 0.95rem;
      cursor: pointer;
      transition: all 0.25s;
      min-width: 220px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      letter-spacing: 0.2px;
    }
    .btn-primary:hover:not(:disabled) {
      background: #1e3a5f;
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(15, 23, 42, 0.35);
    }
    .btn-primary:disabled { opacity: 0.65; cursor: default; transform: none; }
    .btn-primary.saving { background: #334155; }

    .btn-secondary {
      background: white;
      color: #64748b;
      border: 1.5px solid #e2e8f0;
      padding: 13px 22px;
      border-radius: 12px;
      font-family: 'DM Sans', sans-serif;
      font-weight: 700;
      font-size: 0.9rem;
      cursor: pointer;
      transition: all 0.2s;
    }
    .btn-secondary:hover { background: #f8fafc; border-color: #94a3b8; color: #334155; }

    /* Dots animation */
    .dots span {
      animation: blink 1.2s infinite;
      font-size: 1.2rem;
    }
    .dots span:nth-child(2) { animation-delay: 0.2s; }
    .dots span:nth-child(3) { animation-delay: 0.4s; }
    @keyframes blink { 0%, 80%, 100% { opacity: 0; } 40% { opacity: 1; } }

    /* ── Toast ── */
    .toast {
      position: fixed;
      bottom: 30px;
      right: 30px;
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px 24px;
      border-radius: 14px;
      font-family: 'DM Sans', sans-serif;
      font-weight: 700;
      font-size: 0.9rem;
      opacity: 0;
      transform: translateY(20px) scale(0.97);
      transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
      z-index: 9999;
      pointer-events: none;
      min-width: 280px;
      box-shadow: 0 10px 30px rgba(0,0,0,0.2);
    }
    .toast.show { opacity: 1; transform: translateY(0) scale(1); }
    .toast.success { background: #0f172a; color: #e2e8f0; }
    .toast.error   { background: #dc2626; color: white; }
    .toast-icon {
      width: 28px; height: 28px;
      border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 0.9rem; font-weight: 900;
      flex-shrink: 0;
    }
    .toast.success .toast-icon { background: #1e3a5f; }
    .toast.error   .toast-icon { background: rgba(255,255,255,0.2); }
  `]
})
export class AvocatTribunalComponent implements OnInit {

  affaireId!: number;
  affaire: any = null;
  loading = true;
  saving = false;
  submitted = false;
  focused: string | null = null;

  toastVisible = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

  degresJuridiction = [
    { value: '1',    number: '1er', label: 'Première Instance' },
    { value: '2',    number: '2ème', label: 'Appel'            },
    { value: 'CASS', number: 'C',   label: 'Cassation'         },
  ];

  tribunauxSuggeres = [
    'TPI Tunis',
    'TPI Sfax',
    'TPI Sousse',
    'TPI Bizerte',
    'Cour d\'Appel de Tunis',
    'Tribunal Administratif',
  ];

  gouvernorats = [
    'Tunis', 'Ariana', 'Ben Arous', 'Manouba',
    'Nabeul', 'Zaghouan', 'Bizerte', 'Béja',
    'Jendouba', 'Kef', 'Siliana', 'Sousse',
    'Monastir', 'Mahdia', 'Sfax', 'Kairouan',
    'Kasserine', 'Sidi Bouzid', 'Gabès', 'Médenine',
    'Tataouine', 'Gafsa', 'Tozeur', 'Kébili',
  ];

  form = {
    tribunal: '',
    chambre: '',
    numeroRole: '',
    degreJuridiction: '1',
    ville: '',
    notes: ''
  };

  private formOriginal = { ...this.form };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private avocatService: AvocatService
  ) {}

  ngOnInit() {
    this.affaireId = Number(this.route.snapshot.paramMap.get('id'));
    this.chargerAffaire();
  }

  chargerAffaire() {
    this.loading = true;
    this.avocatService.getAffaireDetail(this.affaireId).subscribe({
      next: (aff: any) => {
        this.affaire = aff;
        this.form = {
          tribunal:         aff.tribunal         || '',
          chambre:          aff.chambre          || '',
          numeroRole:       aff.numeroRole        || '',
          degreJuridiction: aff.degreJuridiction  || '1',
          ville:            aff.ville             || '',
          notes:            aff.notesTribunal     || ''
        };
        this.formOriginal = { ...this.form };
        this.loading = false;
      },
      error: () => {
        this.showToast('Impossible de charger les données.', 'error');
        this.loading = false;
      }
    });
  }

  hasCurrentData(): boolean {
    return !!(this.affaire?.tribunal || this.affaire?.chambre || this.affaire?.numeroRole);
  }

  onFocus(field: string) { this.focused = field; }
  onBlur() { setTimeout(() => this.focused = null, 200); }

  appliquerSuggestion(tribunal: string) {
    this.form.tribunal = tribunal;
    this.focused = null;
  }

  resetForm() {
    this.form = { ...this.formOriginal };
    this.submitted = false;
  }

 
  retour() {
    this.router.navigate(['/avocat/dashboard']);
  }

  private showToast(message: string, type: 'success' | 'error') {
    this.toastMessage = message;
    this.toastType = type;
    this.toastVisible = true;
    setTimeout(() => this.toastVisible = false, 3500);
  }

  sauvegarder() {
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
      // ville et notes retirés
    };
  
    this.avocatService.modifierTribunal(this.affaireId, body).subscribe({
      next: () => {
        this.saving = false;
        this.submitted = false;
        this.formOriginal = { ...this.form };
        this.showToast('✓ Tribunal mis à jour avec succès !', 'success');
        setTimeout(() => this.chargerAffaire(), 600);
      },
      error: (e: any) => {
        this.saving = false;
        this.showToast(e.error?.error || e.error?.message || 'Une erreur est survenue.', 'error');
      }
    });
  }
}