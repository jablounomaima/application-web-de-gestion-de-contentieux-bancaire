import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AvocatService } from '../../../core/services/avocat.service';

@Component({
  selector: 'app-avocat-honoraires',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  template: `
    <div class="page-container">

      <!-- Fond animé -->
      <div class="bg-mesh">
        <div class="mesh-blob b1"></div>
        <div class="mesh-blob b2"></div>
        <div class="mesh-blob b3"></div>
      </div>

      <!-- Header -->
      <div class="page-header">
        <button class="btn-back" (click)="retour()">← Retour</button>
        <div class="title-group">
          <span class="role-badge">💰 HONORAIRES</span>
          <h1>Prestations & Honoraires</h1>
          <p class="subtitle" *ngIf="affaire">
            Affaire <strong>#{{ affaire.id }}</strong> —
            {{ affaire.dossier?.numeroDossier || 'N/A' }}
          </p>
        </div>
      </div>

      <!-- Loader -->
      <div class="loader-state" *ngIf="loading">
        <div class="coin-spin">💰</div>
        <p>Chargement des données...</p>
      </div>

      <div class="content-wrapper" *ngIf="!loading">

        <!-- Bande de statut mission -->
        <div class="mission-banner" [class]="getMissionClass()">
          <div class="mission-left">
            <span class="mission-icon">{{ getMissionIcon() }}</span>
            <div>
              <span class="mission-label">Statut de la Mission</span>
              <span class="mission-status">{{ affaire?.mission?.statut || 'Aucune mission assignée' }}</span>
            </div>
          </div>
          <div class="mission-right" *ngIf="affaire?.mission">
            <div class="mission-stat">
              <span class="ms-label">Réf. Mission</span>
              <span class="ms-value mono">#{{ affaire.mission.id }}</span>
            </div>
            <div class="mission-stat" *ngIf="affaire.mission.montantFacture">
              <span class="ms-label">Montant Facturé</span>
              <span class="ms-value green">{{ affaire.mission.montantFacture | number:'1.3-3' }} TND</span>
            </div>
          </div>
        </div>

        <!-- Alerte si pas de mission -->
        <div class="no-mission-alert" *ngIf="!affaire?.mission">
          <span class="alert-icon">⚠️</span>
          <div>
            <strong>Aucune mission active</strong>
            <p>La soumission du PV et de la facture nécessite une mission assignée par le gestionnaire.</p>
          </div>
        </div>

        <div class="two-col">

          <!-- ══════════════ COLONNE 1 : PV de Mission ══════════════ -->
          <div class="section-card pv-card">
            <div class="card-header-strip green-strip">
              <div class="strip-number">01</div>
              <div>
                <h3>Rapport de Mission</h3>
                <p>Procès-verbal détaillant vos diligences</p>
              </div>
              <span class="strip-badge" *ngIf="affaire?.mission?.pvTexte">Soumis ✓</span>
            </div>

            <div class="card-body">

              <!-- PV existant -->
              <div class="existing-pv" *ngIf="affaire?.mission?.pvTexte && !editPV">
                <div class="existing-label">PV actuellement enregistré</div>
                <div class="pv-text-display">{{ affaire.mission.pvTexte }}</div>
                <button class="btn-edit-inline" (click)="editPV = true">✏️ Modifier le PV</button>
              </div>

              <!-- Formulaire PV -->
              <div *ngIf="!affaire?.mission?.pvTexte || editPV">
                <label class="field-label">Contenu du Procès-Verbal</label>
                <textarea
                  [(ngModel)]="pvTexte"
                  rows="8"
                  class="field-textarea"
                  placeholder="Décrivez en détail les diligences accomplies :
• Consultations et conseils juridiques
• Rédaction d'actes et mémoires
• Représentation aux audiences
• Recherches jurisprudentielles
• Correspondances et négociations..."
                  [disabled]="!affaire?.mission"
                ></textarea>
                <div class="char-row">
                  <span class="char-count">{{ pvTexte.length || 0 }} caractères</span>
                  <span class="min-hint" [class.ok]="(pvTexte.length || 0) >= 50">
                    {{ (pvTexte.length || 0) >= 50 ? '✓ Longueur suffisante' : 'Minimum recommandé : 50 car.' }}
                  </span>
                </div>

                <div class="btn-row">
                  <button
                    class="btn-cancel-inline"
                    *ngIf="editPV"
                    (click)="editPV = false"
                  >Annuler</button>
                  <button
                    class="btn-submit green-btn"
                    (click)="soumettrePV()"
                    [disabled]="!affaire?.mission || savingPV || !pvTexte"
                  >
                    <span *ngIf="!savingPV">📄 Soumettre le PV</span>
                    <span *ngIf="savingPV" class="saving-dots">Envoi<span>.</span><span>.</span><span>.</span></span>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- ══════════════ COLONNE 2 : Facture ══════════════ -->
          <div class="section-card facture-card">
            <div class="card-header-strip gold-strip">
              <div class="strip-number">02</div>
              <div>
                <h3>Facture d'Honoraires</h3>
                <p>Soumettez votre note d'honoraires officielle</p>
              </div>
              <span class="strip-badge gold-badge" *ngIf="affaire?.mission?.factureRef">Émise ✓</span>
            </div>

            <div class="card-body">

              <!-- Facture existante -->
              <div class="existing-facture" *ngIf="affaire?.mission?.factureRef && !editFacture">
                <div class="facture-display">
                  <div class="facture-ref-block">
                    <span class="fd-label">Référence</span>
                    <span class="fd-value mono">{{ affaire.mission.factureRef }}</span>
                  </div>
                  <div class="facture-amount-block">
                    <span class="fd-label">Montant</span>
                    <span class="fd-amount">{{ affaire.mission.montantFacture | number:'1.3-3' }} <small>TND</small></span>
                  </div>
                </div>
                <button class="btn-edit-inline gold-edit" (click)="editFacture = true; prefillFacture()">
                  ✏️ Modifier la Facture
                </button>
              </div>

              <!-- Formulaire Facture -->
              <div *ngIf="!affaire?.mission?.factureRef || editFacture">

                <div class="form-grid-2">
                  <div class="field-group">
                    <label class="field-label">Référence Facture <span class="req">*</span></label>
                    <input
                      type="text"
                      [(ngModel)]="facture.ref"
                      placeholder="Ex: FAC-2024-001"
                      class="field-input mono-input"
                      [class.err]="factureSubmitted && !facture.ref"
                      [disabled]="!affaire?.mission"
                    >
                    <span class="err-msg" *ngIf="factureSubmitted && !facture.ref">Référence obligatoire</span>
                  </div>
                  <div class="field-group">
                    <label class="field-label">Montant (TND) <span class="req">*</span></label>
                    <div class="amount-input-wrap">
                      <input
                        type="number"
                        [(ngModel)]="facture.montant"
                        placeholder="0.000"
                        class="field-input amount-input"
                        [class.err]="factureSubmitted && !facture.montant"
                        [disabled]="!affaire?.mission"
                        min="0"
                        step="0.001"
                      >
                      <span class="currency-tag">TND</span>
                    </div>
                    <span class="err-msg" *ngIf="factureSubmitted && !facture.montant">Montant obligatoire</span>
                  </div>
                </div>

                <!-- Aperçu montant -->
                <div class="amount-preview" *ngIf="facture.montant > 0">
                  <span class="ap-label">Montant en lettres (approximatif)</span>
                  <span class="ap-value">{{ facture.montant | number:'1.3-3' }} dinars tunisiens</span>
                </div>

                <!-- TVA indicative -->
                <div class="tva-hint" *ngIf="facture.montant > 0">
                  <div class="tva-row">
                    <span>Montant HT</span>
                    <span>{{ facture.montant | number:'1.3-3' }} TND</span>
                  </div>
                  <div class="tva-row">
                    <span>TVA 19%</span>
                    <span>{{ facture.montant * 0.19 | number:'1.3-3' }} TND</span>
                  </div>
                  <div class="tva-row total-row">
                    <span>Total TTC</span>
                    <span>{{ facture.montant * 1.19 | number:'1.3-3' }} TND</span>
                  </div>
                </div>

                <div class="btn-row">
                  <button
                    class="btn-cancel-inline"
                    *ngIf="editFacture"
                    (click)="editFacture = false"
                  >Annuler</button>
                  <button
                    class="btn-submit gold-btn"
                    (click)="soumettreFacture()"
                    [disabled]="!affaire?.mission || savingFacture"
                  >
                    <span *ngIf="!savingFacture">💳 Soumettre la Facture</span>
                    <span *ngIf="savingFacture" class="saving-dots">Envoi<span>.</span><span>.</span><span>.</span></span>
                  </button>
                </div>
              </div>
            </div>
          </div>

        </div>

        <!-- Récapitulatif global -->
        <div class="recap-card" *ngIf="affaire?.mission?.pvTexte || affaire?.mission?.factureRef">
          <h4>📊 Récapitulatif des Prestations</h4>
          <div class="recap-grid">
            <div class="recap-item">
              <span class="ri-icon">📄</span>
              <span class="ri-label">Rapport PV</span>
              <span class="ri-status" [class.done]="affaire?.mission?.pvTexte">
                {{ affaire?.mission?.pvTexte ? '✓ Soumis' : '⏳ En attente' }}
              </span>
            </div>
            <div class="recap-item">
              <span class="ri-icon">💳</span>
              <span class="ri-label">Facture</span>
              <span class="ri-status" [class.done]="affaire?.mission?.factureRef">
                {{ affaire?.mission?.factureRef ? '✓ ' + affaire.mission.factureRef : '⏳ En attente' }}
              </span>
            </div>
            <div class="recap-item" *ngIf="affaire?.mission?.montantFacture">
              <span class="ri-icon">💰</span>
              <span class="ri-label">Total Honoraires</span>
              <span class="ri-amount">{{ affaire.mission.montantFacture | number:'1.3-3' }} TND</span>
            </div>
          </div>
        </div>

      </div>

      <!-- Toast -->
      <div class="toast"
        [class.show]="toastVisible"
        [class.success]="toastType === 'success'"
        [class.error]="toastType === 'error'"
      >
        <span class="toast-icon">{{ toastType === 'success' ? '✓' : '✕' }}</span>
        {{ toastMessage }}
      </div>

    </div>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=Sora:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500&display=swap');

    * { box-sizing: border-box; }

    .page-container {
      padding: 35px;
      min-height: 100vh;
      font-family: 'Sora', sans-serif;
      background: #f7f8fa;
      position: relative;
      overflow-x: hidden;
    }

    /* ── Background mesh ── */
    .bg-mesh { position: fixed; inset: 0; pointer-events: none; z-index: 0; }
    .mesh-blob {
      position: absolute;
      border-radius: 50%;
      filter: blur(80px);
      opacity: 0.07;
    }
    .b1 { width: 500px; height: 500px; background: #16a34a; top: -150px; right: -100px; }
    .b2 { width: 400px; height: 400px; background: #ca8a04; bottom: 0; left: -100px; }
    .b3 { width: 300px; height: 300px; background: #0f172a; top: 40%; left: 35%; }

    .page-header, .loader-state, .content-wrapper { position: relative; z-index: 1; }

    /* ── Header ── */
    .page-header {
      display: flex;
      align-items: flex-start;
      gap: 20px;
      margin-bottom: 30px;
      flex-wrap: wrap;
    }

    .btn-back {
      background: white;
      border: 1.5px solid #e2e8f0;
      color: #475569;
      padding: 10px 20px;
      border-radius: 12px;
      font-family: 'Sora', sans-serif;
      font-weight: 700;
      font-size: 0.88rem;
      cursor: pointer;
      transition: all 0.2s;
      margin-top: 4px;
      box-shadow: 0 2px 6px rgba(0,0,0,0.04);
    }
    .btn-back:hover { background: #f8fafc; transform: translateX(-2px); }

    .title-group { flex: 1; }

    .role-badge {
      background: #14532d;
      color: #bbf7d0;
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

    .subtitle { color: #64748b; margin-top: 5px; font-size: 0.92rem; }

    /* ── Loader ── */
    .loader-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 100px 0;
      gap: 15px;
      color: #94a3b8;
    }
    .coin-spin {
      font-size: 3rem;
      animation: spin 2s linear infinite;
      display: inline-block;
    }
    @keyframes spin { to { transform: rotateY(360deg); } }

    /* ── Mission Banner ── */
    .mission-banner {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 18px 28px;
      border-radius: 16px;
      margin-bottom: 24px;
      flex-wrap: wrap;
      gap: 16px;
    }
    .mission-banner.active   { background: #f0fdf4; border: 1.5px solid #86efac; }
    .mission-banner.inactive { background: #f8fafc; border: 1.5px solid #e2e8f0; }
    .mission-banner.pending  { background: #fffbeb; border: 1.5px solid #fde68a; }

    .mission-left { display: flex; align-items: center; gap: 14px; }
    .mission-icon { font-size: 1.8rem; }
    .mission-label { display: block; font-size: 0.72rem; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 3px; }
    .mission-status { font-size: 1rem; font-weight: 800; color: #0f172a; }

    .mission-right { display: flex; gap: 30px; flex-wrap: wrap; }
    .mission-stat { display: flex; flex-direction: column; gap: 3px; }
    .ms-label { font-size: 0.72rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; }
    .ms-value { font-size: 0.95rem; font-weight: 700; color: #0f172a; }
    .ms-value.mono { font-family: 'JetBrains Mono', monospace; }
    .ms-value.green { color: #16a34a; font-size: 1.05rem; }

    /* ── No Mission Alert ── */
    .no-mission-alert {
      display: flex;
      align-items: flex-start;
      gap: 14px;
      background: #fefce8;
      border: 1.5px solid #fde047;
      border-radius: 14px;
      padding: 18px 22px;
      margin-bottom: 24px;
    }
    .alert-icon { font-size: 1.5rem; flex-shrink: 0; }
    .no-mission-alert strong { display: block; color: #713f12; margin-bottom: 4px; }
    .no-mission-alert p { margin: 0; font-size: 0.85rem; color: #92400e; }

    /* ── Two Col ── */
    .two-col {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
      margin-bottom: 24px;
    }
    @media (max-width: 860px) { .two-col { grid-template-columns: 1fr; } }

    /* ── Section Cards ── */
    .section-card {
      background: white;
      border-radius: 20px;
      box-shadow: 0 6px 24px rgba(0,0,0,0.06);
      border: 1px solid #e8edf2;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    .card-header-strip {
      display: flex;
      align-items: flex-start;
      gap: 16px;
      padding: 22px 26px;
      position: relative;
    }
    .green-strip { background: linear-gradient(135deg, #f0fdf4, #dcfce7); border-bottom: 1px solid #bbf7d0; }
    .gold-strip  { background: linear-gradient(135deg, #fffbeb, #fef3c7); border-bottom: 1px solid #fde68a; }

    .strip-number {
      font-size: 2.5rem;
      font-weight: 800;
      line-height: 1;
      opacity: 0.15;
      color: #0f172a;
      font-family: 'JetBrains Mono', monospace;
      flex-shrink: 0;
    }

    .card-header-strip h3 { margin: 0 0 4px; font-size: 1.05rem; font-weight: 800; color: #0f172a; }
    .card-header-strip p  { margin: 0; font-size: 0.8rem; color: #64748b; }

    .strip-badge {
      position: absolute;
      top: 18px; right: 18px;
      background: #16a34a;
      color: white;
      padding: 4px 10px;
      border-radius: 20px;
      font-size: 0.7rem;
      font-weight: 800;
    }
    .gold-badge { background: #ca8a04; }

    .card-body { padding: 24px 26px; flex: 1; }

    /* ── Fields ── */
    .field-group { margin-bottom: 18px; }

    .field-label {
      display: block;
      font-size: 0.75rem;
      font-weight: 700;
      color: #475569;
      text-transform: uppercase;
      letter-spacing: 0.6px;
      margin-bottom: 8px;
    }
    .req { color: #dc2626; }

    .field-textarea {
      width: 100%;
      padding: 14px 16px;
      border: 1.5px solid #e2e8f0;
      border-radius: 12px;
      outline: none;
      font-family: 'Sora', sans-serif;
      font-size: 0.88rem;
      color: #0f172a;
      background: #fafbfc;
      resize: vertical;
      min-height: 180px;
      line-height: 1.7;
      transition: border-color 0.2s, box-shadow 0.2s;
    }
    .field-textarea:focus {
      border-color: #16a34a;
      background: white;
      box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.08);
    }
    .field-textarea:disabled { opacity: 0.5; cursor: not-allowed; }

    .field-input {
      width: 100%;
      padding: 12px 15px;
      border: 1.5px solid #e2e8f0;
      border-radius: 10px;
      outline: none;
      font-family: 'Sora', sans-serif;
      font-size: 0.92rem;
      color: #0f172a;
      background: #fafbfc;
      transition: border-color 0.2s, box-shadow 0.2s;
    }
    .field-input:focus { border-color: #ca8a04; background: white; box-shadow: 0 0 0 3px rgba(202, 138, 4, 0.08); }
    .field-input.err   { border-color: #dc2626; }
    .field-input:disabled { opacity: 0.5; cursor: not-allowed; }
    .field-input.mono-input { font-family: 'JetBrains Mono', monospace; letter-spacing: 0.5px; }

    .amount-input-wrap { position: relative; display: flex; align-items: center; }
    .amount-input-wrap .field-input { padding-right: 55px; }
    .currency-tag {
      position: absolute; right: 14px;
      font-size: 0.75rem; font-weight: 800;
      color: #94a3b8; pointer-events: none;
    }

    .err-msg { display: block; color: #dc2626; font-size: 0.75rem; font-weight: 600; margin-top: 5px; }

    .form-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 16px; }
    @media (max-width: 500px) { .form-grid-2 { grid-template-columns: 1fr; } }

    /* ── Char count ── */
    .char-row { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; margin-bottom: 16px; }
    .char-count { font-size: 0.72rem; color: #94a3b8; }
    .min-hint { font-size: 0.72rem; color: #dc2626; font-weight: 600; }
    .min-hint.ok { color: #16a34a; }

    /* ── Amount Preview ── */
    .amount-preview {
      background: #f0fdf4;
      border: 1px solid #bbf7d0;
      border-radius: 10px;
      padding: 12px 16px;
      margin-bottom: 14px;
      display: flex;
      flex-direction: column;
      gap: 3px;
    }
    .ap-label { font-size: 0.7rem; font-weight: 700; color: #15803d; text-transform: uppercase; letter-spacing: 0.5px; }
    .ap-value { font-size: 0.9rem; font-weight: 700; color: #14532d; font-family: 'JetBrains Mono', monospace; }

    /* ── TVA Hint ── */
    .tva-hint {
      background: #fffbeb;
      border: 1px solid #fde68a;
      border-radius: 10px;
      padding: 14px 16px;
      margin-bottom: 20px;
    }
    .tva-row {
      display: flex;
      justify-content: space-between;
      font-size: 0.82rem;
      color: #78350f;
      padding: 4px 0;
      border-bottom: 1px dashed #fde68a;
    }
    .tva-row:last-child { border-bottom: none; }
    .tva-row.total-row { font-weight: 800; font-size: 0.9rem; color: #92400e; padding-top: 8px; }

    /* ── Existing display ── */
    .existing-pv { margin-bottom: 10px; }
    .existing-label {
      font-size: 0.7rem;
      font-weight: 700;
      color: #16a34a;
      text-transform: uppercase;
      letter-spacing: 1px;
      margin-bottom: 10px;
    }
    .pv-text-display {
      background: #f0fdf4;
      border: 1px solid #bbf7d0;
      border-left: 4px solid #16a34a;
      border-radius: 10px;
      padding: 16px;
      font-size: 0.88rem;
      color: #14532d;
      line-height: 1.7;
      white-space: pre-wrap;
      margin-bottom: 14px;
      max-height: 200px;
      overflow-y: auto;
    }

    .facture-display {
      display: flex;
      gap: 20px;
      background: #fffbeb;
      border: 1px solid #fde68a;
      border-radius: 12px;
      padding: 18px 20px;
      margin-bottom: 14px;
      flex-wrap: wrap;
    }
    .facture-ref-block, .facture-amount-block { display: flex; flex-direction: column; gap: 5px; }
    .fd-label { font-size: 0.7rem; font-weight: 700; color: #92400e; text-transform: uppercase; letter-spacing: 0.5px; }
    .fd-value { font-size: 1rem; font-weight: 700; color: #78350f; }
    .fd-value.mono { font-family: 'JetBrains Mono', monospace; }
    .fd-amount { font-size: 1.4rem; font-weight: 800; color: #ca8a04; }
    .fd-amount small { font-size: 0.8rem; font-weight: 600; }

    .btn-edit-inline {
      background: none;
      border: 1.5px solid #16a34a;
      color: #16a34a;
      padding: 8px 16px;
      border-radius: 8px;
      font-family: 'Sora', sans-serif;
      font-weight: 700;
      font-size: 0.8rem;
      cursor: pointer;
      transition: all 0.2s;
      margin-bottom: 16px;
    }
    .btn-edit-inline:hover { background: #f0fdf4; }
    .gold-edit { border-color: #ca8a04; color: #ca8a04; }
    .gold-edit:hover { background: #fffbeb; }

    /* ── Buttons ── */
    .btn-row { display: flex; gap: 10px; align-items: center; justify-content: flex-end; margin-top: 6px; }

    .btn-submit {
      padding: 12px 24px;
      border: none;
      border-radius: 10px;
      font-family: 'Sora', sans-serif;
      font-weight: 700;
      font-size: 0.88rem;
      cursor: pointer;
      transition: all 0.2s;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .btn-submit:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

    .green-btn { background: #16a34a; color: white; }
    .green-btn:hover:not(:disabled) { background: #15803d; transform: translateY(-1px); box-shadow: 0 4px 14px rgba(22,163,74,0.3); }

    .gold-btn { background: #ca8a04; color: white; }
    .gold-btn:hover:not(:disabled) { background: #a16207; transform: translateY(-1px); box-shadow: 0 4px 14px rgba(202,138,4,0.3); }

    .btn-cancel-inline {
      background: #f1f5f9;
      border: 1px solid #e2e8f0;
      color: #64748b;
      padding: 10px 18px;
      border-radius: 10px;
      font-family: 'Sora', sans-serif;
      font-weight: 700;
      font-size: 0.85rem;
      cursor: pointer;
      transition: all 0.2s;
    }
    .btn-cancel-inline:hover { background: #e2e8f0; }

    /* ── Saving dots ── */
    .saving-dots span {
      animation: blink 1s infinite;
    }
    .saving-dots span:nth-child(2) { animation-delay: 0.2s; }
    .saving-dots span:nth-child(3) { animation-delay: 0.4s; }
    @keyframes blink { 0%, 80%, 100% { opacity: 0; } 40% { opacity: 1; } }

    /* ── Recap Card ── */
    .recap-card {
      background: white;
      border-radius: 18px;
      padding: 24px 28px;
      box-shadow: 0 4px 16px rgba(0,0,0,0.05);
      border: 1px solid #e8edf2;
    }
    .recap-card h4 { margin: 0 0 18px; font-size: 1rem; color: #0f172a; font-weight: 800; }

    .recap-grid { display: flex; gap: 30px; flex-wrap: wrap; }
    .recap-item { display: flex; align-items: center; gap: 12px; }
    .ri-icon { font-size: 1.3rem; }
    .ri-label { font-size: 0.8rem; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; }
    .ri-status { font-size: 0.85rem; font-weight: 700; color: #94a3b8; margin-left: 8px; }
    .ri-status.done { color: #16a34a; }
    .ri-amount { font-size: 1rem; font-weight: 800; color: #ca8a04; margin-left: 8px; font-family: 'JetBrains Mono', monospace; }

    /* ── Toast ── */
    .toast {
      position: fixed;
      bottom: 30px; right: 30px;
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px 24px;
      border-radius: 14px;
      font-family: 'Sora', sans-serif;
      font-weight: 700;
      font-size: 0.88rem;
      opacity: 0;
      transform: translateY(20px) scale(0.97);
      transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
      z-index: 9999;
      pointer-events: none;
      min-width: 260px;
      box-shadow: 0 10px 30px rgba(0,0,0,0.2);
    }
    .toast.show { opacity: 1; transform: translateY(0) scale(1); }
    .toast.success { background: #14532d; color: #bbf7d0; }
    .toast.error   { background: #dc2626; color: white; }
    .toast-icon {
      width: 26px; height: 26px;
      border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-weight: 900; font-size: 0.85rem;
      background: rgba(255,255,255,0.15);
      flex-shrink: 0;
    }
  `]
})
export class AvocatHonorairesComponent implements OnInit {

  affaireId!: number;
  affaire: any = null;
  loading = true;

  savingPV = false;
  savingFacture = false;
  factureSubmitted = false;

  editPV = false;
  editFacture = false;

  pvTexte = '';
  facture = { ref: '', montant: 0 };

  toastVisible = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

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
        this.pvTexte = aff.mission?.pvTexte || '';
        this.facture = {
          ref: aff.mission?.factureRef || '',
          montant: aff.mission?.montantFacture || 0
        };
        this.loading = false;
      },
      error: () => {
        this.showToast('Impossible de charger les données.', 'error');
        this.loading = false;
      }
    });
  }

  getMissionClass(): string {
    const statut = this.affaire?.mission?.statut;
    if (!statut) return 'inactive';
    if (['ACCEPTEE', 'EN_COURS', 'ACTIVE'].includes(statut)) return 'active';
    if (['EN_ATTENTE', 'SOUMISE'].includes(statut)) return 'pending';
    return 'inactive';
  }

  getMissionIcon(): string {
    const statut = this.affaire?.mission?.statut;
    if (!statut) return '⚪';
    if (['ACCEPTEE', 'EN_COURS', 'ACTIVE'].includes(statut)) return '🟢';
    if (['EN_ATTENTE', 'SOUMISE'].includes(statut)) return '🟡';
    return '⚫';
  }

  prefillFacture() {
    this.facture = {
      ref: this.affaire?.mission?.factureRef || '',
      montant: this.affaire?.mission?.montantFacture || 0
    };
  }

  soumettrePV() {
    if (!this.pvTexte?.trim()) {
      this.showToast('Le contenu du PV est obligatoire.', 'error');
      return;
    }
    this.savingPV = true;
    this.avocatService.soumettrePV(
      this.affaireId,
      this.affaire.mission.id,
      this.pvTexte
    ).subscribe({
      next: () => {
        this.savingPV = false;
        this.editPV = false;
        this.showToast('✓ PV soumis avec succès !', 'success');
        setTimeout(() => this.chargerAffaire(), 500);
      },
      error: (e: any) => {
        this.savingPV = false;
        this.showToast(e.error?.error || e.error?.message || 'Erreur lors de la soumission.', 'error');
      }
    });
  }

  soumettreFacture() {
    this.factureSubmitted = true;
    if (!this.facture.ref || !this.facture.montant) {
      this.showToast('Référence et montant sont obligatoires.', 'error');
      return;
    }
    this.savingFacture = true;
    const body = { factureRef: this.facture.ref, montantFacture: this.facture.montant };
    this.avocatService.soumettreFacture(
      this.affaireId,
      this.affaire.mission.id,
      body
    ).subscribe({
      next: () => {
        this.savingFacture = false;
        this.factureSubmitted = false;
        this.editFacture = false;
        this.showToast('✓ Facture soumise avec succès !', 'success');
        setTimeout(() => this.chargerAffaire(), 500);
      },
      error: (e: any) => {
        this.savingFacture = false;
        this.showToast(e.error?.error || e.error?.message || 'Erreur lors de la soumission.', 'error');
      }
    });
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
}