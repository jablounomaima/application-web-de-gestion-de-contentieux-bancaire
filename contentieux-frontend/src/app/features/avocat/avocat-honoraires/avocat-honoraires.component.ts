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

      <!-- ══ Header ══ -->
      <div class="page-header">
        <button class="btn-back" (click)="retour()">← Retour</button>
        <div class="title-group">
          <span class="role-badge">HONORAIRES</span>
          <h1>PV & Facture d'Honoraires</h1>
          <p class="subtitle" *ngIf="affaire">
            Affaire <strong>#{{ affaire.numeroAffaire }}</strong>
            <span class="status-pill" [ngClass]="statutClass(affaire.statut)">
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

          <!-- PV soumis -->
          <div class="info-card pv-summary" *ngIf="hasPV()">
            <h3 class="panel-title">📋 PV soumis</h3>

            <div class="statut-row">
              <span class="statut-badge" [ngClass]="pvBadgeClass()">
                {{ pvStatutLabel() }}
              </span>
            </div>

            <div class="pv-texte-box">
              <span class="info-label">Contenu</span>
              <p class="pv-texte">{{ affaire.pvTexte }}</p>
            </div>

            <!-- Fichiers -->
            <div class="pj-section" *ngIf="pvFichiers().length > 0">
              <span class="info-label">
                📎 Pièces jointes ({{ pvFichiers().length }})
              </span>
              <div class="pj-list">
                <div *ngFor="let f of pvFichiers(); let i = index"
                     class="pj-item">
                  <span class="pj-icon">{{ getFileIcon(f.nom) }}</span>
                  <a [href]="buildDownloadUrl(f)"
                     [download]="f.nom"
                     target="_blank"
                     class="pj-nom">{{ f.nom }}</a>
                  <span class="pj-dl">⬇️</span>
                  <button class="pj-del"
                          (click)="supprimerFichierPV(i)"
                          title="Supprimer">✕</button>
                </div>
              </div>
            </div>
            <div class="pj-empty"
                 *ngIf="pvFichiers().length === 0">
              Aucune pièce jointe.
            </div>

            <!-- Actions PV -->
            <div class="card-actions">
              <button class="btn-edit"
                      (click)="editerPV()"
                      *ngIf="pvStatut !== 'VALIDE'">
                ✏️ Modifier le PV
              </button>
            </div>
          </div>

          <!-- Aucun PV -->
          <div class="info-card empty-card" *ngIf="!hasPV()">
            <div class="empty-icon">📋</div>
            <p class="empty-text">Aucun PV soumis</p>
            <p class="empty-sub">
              Utilisez le formulaire ci-contre pour soumettre votre PV.
            </p>
          </div>

          <!-- Facture soumise -->
          <div class="info-card facture-summary" *ngIf="hasFacture()">
            <h3 class="panel-title">🧾 Facture soumise</h3>

            <div class="statut-row">
              <span class="statut-badge" [ngClass]="factureBadgeClass()">
                {{ factureStatutLabel() }}
              </span>
            </div>

            <div class="info-row">
              <span class="info-label">Référence</span>
              <span class="info-value mono">{{ affaire.factureRef }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Montant HT</span>
              <span class="info-value">
                {{ affaire.montantFacture | number:'1.3-3' }} TND
              </span>
            </div>
            <div class="info-row">
              <span class="info-label">TVA 19%</span>
              <span class="info-value">
                {{ affaire.montantFacture * 0.19 | number:'1.3-3' }} TND
              </span>
            </div>
            <div class="info-row total-row">
              <span class="info-label">Total TTC</span>
              <span class="info-value ttc">
                {{ affaire.montantFacture * 1.19 | number:'1.3-3' }} TND
              </span>
            </div>

            <!-- Actions Facture -->
            <div class="card-actions" *ngIf="factureStatut !== 'PAYEE'">
              <button class="btn-edit" (click)="editerFacture()">
                ✏️ Modifier la facture
              </button>
            </div>
          </div>

          <!-- Aucune facture -->
          <div class="info-card empty-card" *ngIf="!hasFacture()">
            <div class="empty-icon">🧾</div>
            <p class="empty-text">Aucune facture soumise</p>
            <p class="empty-sub">
              Utilisez le formulaire ci-contre pour soumettre votre facture.
            </p>
          </div>

        </div>

        <!-- ════════ COLONNE DROITE : Formulaires ════════ -->
        <div class="form-panel">

          <!-- ══ Formulaire PV ══ -->
          <div class="form-card" *ngIf="pvStatut !== 'VALIDE'">
            <div class="form-card-header green-header">
              <div class="header-num">01</div>
              <div>
                <h2>{{ hasPV() ? '✏️ Modifier le PV' : '➕ Soumettre un PV' }}</h2>
                <p class="form-subtitle">
                  Procès-verbal détaillant vos diligences
                </p>
              </div>
            </div>

            <div class="form-body">

              <!-- PV refusé -->
              <div class="alert-refus" *ngIf="pvStatut === 'REFUSE'">
                ⚠️ PV refusé — soumettez un nouveau PV ci-dessous.
              </div>

              <div class="field">
                <label class="field-label">
                  Contenu du Procès-Verbal <span class="required">*</span>
                </label>
                <textarea [(ngModel)]="pvTexte"
                          rows="7"
                          maxlength="5000"
                          class="field-input textarea"
                          placeholder="Saisir le contenu du PV...">
                </textarea>
                <div class="char-row">
                  <span>{{ pvTexte.length }}/5000 caractères</span>
                  <span class="min-hint" [class.ok]="pvTexte.length >= 50">
                    {{ pvTexte.length >= 50
                       ? '✓ Longueur suffisante'
                       : 'Minimum 50 caractères recommandé' }}
                  </span>
                </div>
              </div>

              <!-- Upload -->
              <div class="field">
                <label class="field-label">
                  Pièces jointes
                  <span class="pj-count-badge" *ngIf="pvFiles.length > 0">
                    {{ pvFiles.length }}
                  </span>
                </label>

                <label class="pj-drop-area" *ngIf="pvFiles.length === 0">
                  <input type="file" multiple
                         accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
                         (change)="onPVFilesSelected($event)"
                         style="display:none">
                  <div class="drop-content">
                    <span class="drop-icon">📂</span>
                    <span class="drop-text">
                      Cliquez pour ajouter des fichiers
                    </span>
                    <span class="drop-hint">PDF, Word, Images — max 10 Mo</span>
                  </div>
                </label>

                <div class="pj-files-preview" *ngIf="pvFiles.length > 0">
                  <div class="pj-file-item"
                       *ngFor="let f of pvFiles; let i = index">
                    <span class="pj-icon-sm">{{ getFileIcon(f.name) }}</span>
                    <span class="pj-name">{{ f.name }}</span>
                    <span class="pj-size">{{ formatSize(f.size) }}</span>
                    <button type="button" class="pj-remove"
                            (click)="removePVFile(i, $event)">✕</button>
                  </div>
                  <label style="cursor:pointer; margin-top:8px; display:inline-block">
                    <input type="file" multiple
                           accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
                           (change)="onPVFilesSelected($event)"
                           style="display:none">
                    <span class="btn-add-more">+ Ajouter d'autres fichiers</span>
                  </label>
                </div>
              </div>

              <div class="form-actions">
                <button class="btn-secondary"
                        *ngIf="hasPV()"
                        (click)="annulerPV()">
                  Annuler
                </button>
                <button class="btn-primary green-btn"
                        (click)="soumettrePV()"
                        [disabled]="savingPV || !pvTexte.trim()">
                  <span *ngIf="!savingPV">
                    ✓ {{ hasPV() ? 'Mettre à jour le PV' : 'Soumettre le PV' }}
                  </span>
                  <span *ngIf="savingPV">Envoi en cours...</span>
                </button>
              </div>

            </div>
          </div>

          <!-- PV validé -->
          <div class="success-card" *ngIf="pvStatut === 'VALIDE'">
            <div class="success-icon">✅</div>
            <h3>PV validé</h3>
            <p>Votre PV a été validé par l'agent bancaire.</p>
          </div>

          <!-- ══ Formulaire Facture ══ -->
          <div class="form-card" *ngIf="factureStatut !== 'PAYEE'">
            <div class="form-card-header gold-header">
              <div class="header-num gold-num">02</div>
              <div>
                <h2>
                  {{ hasFacture()
                     ? '✏️ Modifier la Facture'
                     : '➕ Soumettre une Facture' }}
                </h2>
                <p class="form-subtitle">
                  Note d'honoraires officielle
                </p>
              </div>
            </div>

            <div class="form-body">

              <!-- Facture rejetée -->
              <div class="alert-refus" *ngIf="factureStatut === 'REJETEE'">
                ⚠️ Facture rejetée — soumettez une nouvelle facture.
              </div>

              <div class="form-grid-2">
                <div class="field">
                  <label class="field-label">
                    Référence <span class="required">*</span>
                  </label>
                  <input type="text"
                         [(ngModel)]="facture.ref"
                         placeholder="Ex: FACT-2024-001"
                         class="field-input">
                </div>
                <div class="field">
                  <label class="field-label">
                    Montant HT (TND) <span class="required">*</span>
                  </label>
                  <div class="input-suffix">
                    <input type="number"
                           [(ngModel)]="facture.montant"
                           min="0" step="0.001"
                           placeholder="0.000"
                           class="field-input">
                    <span class="suffix">TND</span>
                  </div>
                </div>
              </div>

              <!-- TVA -->
              <div class="tva-box"
                   *ngIf="facture.montant > 0">
                <div class="tva-row">
                  <span>Montant HT</span>
                  <span>{{ facture.montant | number:'1.3-3' }} TND</span>
                </div>
                <div class="tva-row">
                  <span>TVA 19%</span>
                  <span>{{ facture.montant * 0.19 | number:'1.3-3' }} TND</span>
                </div>
                <div class="tva-row tva-total">
                  <span>Total TTC</span>
                  <span>{{ facture.montant * 1.19 | number:'1.3-3' }} TND</span>
                </div>
              </div>

              <div class="form-actions">
                <button class="btn-secondary"
                        *ngIf="hasFacture()"
                        (click)="annulerFacture()">
                  Annuler
                </button>
                <button class="btn-primary gold-btn"
                        (click)="soumettreFacture()"
                        [disabled]="savingFacture
                                    || !facture.ref
                                    || !facture.montant
                                    || facture.montant <= 0">
                  <span *ngIf="!savingFacture">
                    ✓ {{ hasFacture()
                         ? 'Mettre à jour la Facture'
                         : 'Soumettre la Facture' }}
                  </span>
                  <span *ngIf="savingFacture">Envoi en cours...</span>
                </button>
              </div>

            </div>
          </div>

          <!-- Facture payée -->
          <div class="success-card" *ngIf="factureStatut === 'PAYEE'">
            <div class="success-icon">✅</div>
            <h3>Facture payée</h3>
            <p>Votre facture a été réglée par l'agent bancaire.</p>
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
    .role-badge { background: #d1fae5; color: #065f46; padding: 4px 10px; border-radius: 6px; font-weight: 800; font-size: 0.7rem; letter-spacing: 1px; display: inline-block; margin-bottom: 6px; }
    h1 { margin: 0; font-size: 2rem; color: #1e293b; font-weight: 800; }
    .subtitle { color: #64748b; margin-top: 6px; font-size: 0.95rem; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
    .status-pill { padding: 4px 10px; border-radius: 8px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; }
    .pill-blue   { background: #e0f2fe; color: #0369a1; }
    .pill-green  { background: #dcfce7; color: #15803d; }
    .pill-grey   { background: #f1f5f9; color: #64748b; }

    /* ── Loader ── */
    .loader-state { display: flex; flex-direction: column; align-items: center; padding: 80px 0; color: #94a3b8; }
    .spinner { width: 40px; height: 40px; border: 3px solid #e2e8f0; border-top-color: #10b981; border-radius: 50%; animation: spin 0.8s linear infinite; margin-bottom: 15px; }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* ── Layout ── */
    .content-grid { display: grid; grid-template-columns: 340px 1fr; gap: 25px; align-items: start; }
    @media (max-width: 900px) { .content-grid { grid-template-columns: 1fr; } }

    /* ── Side cards ── */
    .info-card { background: white; border-radius: 20px; padding: 24px; box-shadow: 0 4px 15px rgba(0,0,0,0.04); border: 1px solid #e2e8f0; margin-bottom: 20px; }
    .pv-summary      { border-left: 4px solid #10b981; }
    .facture-summary { border-left: 4px solid #f59e0b; }
    .panel-title { margin: 0 0 16px; font-size: 1rem; font-weight: 700; color: #1e293b; padding-bottom: 12px; border-bottom: 1px solid #f1f5f9; }

    .statut-row { margin-bottom: 14px; }
    .statut-badge { display: inline-block; padding: 5px 14px; border-radius: 20px; font-size: 0.75rem; font-weight: 800; }
    .badge-success { background: #dcfce7; color: #15803d; }
    .badge-warning { background: #fef9c3; color: #a16207; }
    .badge-danger  { background: #fee2e2; color: #991b1b; }

    .info-row { display: flex; justify-content: space-between; align-items: center; padding: 9px 0; border-bottom: 1px solid #f8fafc; }
    .info-row:last-of-type { border-bottom: none; }
    .info-row.total-row { border-top: 2px solid #f1f5f9; margin-top: 4px; padding-top: 12px; }
    .info-label { font-size: 0.75rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; }
    .info-value { font-size: 0.9rem; font-weight: 600; color: #1e293b; }
    .info-value.ttc { font-size: 1rem; font-weight: 800; color: #d97706; }
    .info-value.mono { font-family: monospace; }

    /* ── PV texte ── */
    .pv-texte-box { background: #f8fafc; border-radius: 8px; padding: 12px; margin: 10px 0 14px; }
    .pv-texte { margin: 6px 0 0; font-size: 0.85rem; color: #374151; line-height: 1.6; white-space: pre-wrap; max-height: 140px; overflow-y: auto; }

    /* ── Pièces jointes (side) ── */
    .pj-section { margin-top: 14px; }
    .pj-list { display: flex; flex-direction: column; gap: 5px; margin-top: 8px; }
    .pj-item { display: flex; align-items: center; gap: 7px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 7px; padding: 7px 10px; }
    .pj-icon { font-size: 1.1rem; flex-shrink: 0; }
    .pj-nom  { flex: 1; font-size: 0.8rem; color: #4338ca; text-decoration: none; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .pj-nom:hover { text-decoration: underline; }
    .pj-dl  { font-size: 0.75rem; }
    .pj-del { background: #fee2e2; border: none; color: #ef4444; width: 20px; height: 20px; border-radius: 5px; cursor: pointer; font-size: 0.65rem; font-weight: 800; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
    .pj-del:hover { background: #fecaca; }
    .pj-empty { font-size: 0.8rem; color: #94a3b8; font-style: italic; margin-top: 10px; }

    /* ── Card actions ── */
    .card-actions { margin-top: 16px; padding-top: 14px; border-top: 1px solid #f1f5f9; }
    .btn-edit { background: #eef2ff; color: #4338ca; border: none; padding: 8px 16px; border-radius: 8px; font-weight: 700; font-size: 0.84rem; cursor: pointer; width: 100%; transition: all 0.2s; }
    .btn-edit:hover { background: #e0e7ff; }

    /* ── Empty card ── */
    .empty-card { text-align: center; padding: 28px 16px; }
    .empty-icon { font-size: 2.5rem; margin-bottom: 10px; }
    .empty-text { font-weight: 700; color: #374151; margin: 0 0 6px; }
    .empty-sub  { color: #94a3b8; font-size: 0.82rem; margin: 0; line-height: 1.5; }

    /* ── Form cards ── */
    .form-card { background: white; border-radius: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); border: 1px solid #e2e8f0; overflow: hidden; margin-bottom: 24px; }
    .form-card-header { padding: 24px 30px 18px; border-bottom: 1px solid #f1f5f9; display: flex; align-items: center; gap: 16px; }
    .green-header { background: linear-gradient(135deg, #ecfdf5, #d1fae5); border-bottom-color: #a7f3d0; }
    .gold-header  { background: linear-gradient(135deg, #fffbeb, #fef3c7); border-bottom-color: #fde68a; }
    .header-num { width: 40px; height: 40px; border-radius: 12px; background: rgba(255,255,255,0.8); display: flex; align-items: center; justify-content: center; font-weight: 900; font-size: 1rem; color: #374151; flex-shrink: 0; }
    .gold-num { color: #92400e; }
    .form-card-header h2 { margin: 0; font-size: 1.1rem; font-weight: 800; color: #1e293b; }
    .form-subtitle { margin: 4px 0 0; color: #64748b; font-size: 0.85rem; }
    .form-body { padding: 28px 30px; }

    /* ── Fields ── */
    .field { margin-bottom: 20px; }
    .field-label { display: block; font-size: 0.8rem; font-weight: 700; color: #475569; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 8px; }
    .required { color: #dc2626; }
    .field-input { width: 100%; padding: 12px 16px; border: 1.5px solid #e2e8f0; border-radius: 10px; outline: none; font-family: inherit; font-size: 0.95rem; color: #1e293b; background: #fafafa; transition: all 0.2s; }
    .field-input:focus { border-color: #4338ca; background: white; box-shadow: 0 0 0 3px rgba(67,56,202,0.08); }
    .field-input.textarea { resize: vertical; min-height: 150px; line-height: 1.6; }
    .form-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    .input-suffix { position: relative; }
    .input-suffix .field-input { padding-right: 52px; }
    .suffix { position: absolute; right: 14px; top: 50%; transform: translateY(-50%); font-size: 0.8rem; font-weight: 700; color: #94a3b8; }

    /* ── Char count ── */
    .char-row { display: flex; justify-content: space-between; margin-top: 6px; font-size: 0.75rem; color: #94a3b8; }
    .min-hint { transition: color 0.2s; }
    .min-hint.ok { color: #059669; font-weight: 600; }

    /* ── TVA ── */
    .tva-box { background: #fffbeb; border: 1px solid #fde68a; border-radius: 10px; padding: 14px 16px; margin-bottom: 20px; }
    .tva-row { display: flex; justify-content: space-between; font-size: 0.85rem; color: #92400e; padding: 3px 0; }
    .tva-total { font-weight: 800; border-top: 1px solid #fde68a; margin-top: 6px; padding-top: 8px; color: #78350f; }

    /* ── Upload ── */
    .pj-count-badge { background: #4338ca; color: white; font-size: 0.7rem; font-weight: 800; padding: 1px 7px; border-radius: 10px; margin-left: 6px; }
    .pj-drop-area { display: block; border: 2px dashed #cbd5e1; border-radius: 12px; cursor: pointer; transition: all 0.2s; min-height: 80px; }
    .pj-drop-area:hover { border-color: #4338ca; background: #f5f3ff; }
    .drop-content { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 20px; gap: 6px; }
    .drop-icon { font-size: 1.8rem; }
    .drop-text { font-size: 0.85rem; font-weight: 600; color: #374151; }
    .drop-hint { font-size: 0.75rem; color: #94a3b8; }
    .pj-files-preview { border: 1px solid #e2e8f0; border-radius: 12px; padding: 10px; display: flex; flex-direction: column; gap: 6px; }
    .pj-file-item { display: flex; align-items: center; gap: 8px; background: white; border: 1px solid #e2e8f0; border-radius: 8px; padding: 8px 12px; }
    .pj-icon-sm { font-size: 1rem; flex-shrink: 0; }
    .pj-name { flex: 1; font-size: 0.82rem; color: #374151; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .pj-size { font-size: 0.72rem; color: #94a3b8; white-space: nowrap; }
    .pj-remove { background: #fee2e2; border: none; color: #ef4444; width: 22px; height: 22px; border-radius: 6px; cursor: pointer; font-size: 0.7rem; font-weight: 800; display: flex; align-items: center; justify-content: center; }
    .pj-remove:hover { background: #fecaca; }
    .btn-add-more { border: 1px solid #4338ca; color: #4338ca; font-size: 0.8rem; font-weight: 700; padding: 5px 12px; border-radius: 7px; cursor: pointer; display: inline-block; }
    .btn-add-more:hover { background: #e0e7ff; }

    /* ── Alert ── */
    .alert-refus { background: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; padding: 10px 14px; font-size: 0.82rem; color: #991b1b; margin-bottom: 18px; }

    /* ── Form actions ── */
    .form-actions { display: flex; gap: 12px; justify-content: flex-end; padding-top: 16px; border-top: 1px solid #f1f5f9; margin-top: 8px; }
    .btn-primary { border: none; padding: 13px 28px; border-radius: 10px; font-weight: 700; font-size: 0.95rem; cursor: pointer; transition: all 0.2s; min-width: 200px; color: white; }
    .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
    .green-btn { background: linear-gradient(135deg, #059669, #10b981); }
    .green-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 15px rgba(16,185,129,0.3); }
    .gold-btn { background: linear-gradient(135deg, #d97706, #f59e0b); }
    .gold-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 15px rgba(245,158,11,0.3); }
    .btn-secondary { background: white; color: #64748b; border: 1.5px solid #e2e8f0; padding: 13px 20px; border-radius: 10px; font-weight: 700; cursor: pointer; transition: all 0.2s; }
    .btn-secondary:hover { background: #f8fafc; }

    /* ── Success card ── */
    .success-card { background: white; border-radius: 20px; padding: 40px 30px; text-align: center; box-shadow: 0 4px 15px rgba(0,0,0,0.04); border: 1px solid #e2e8f0; margin-bottom: 24px; }
    .success-icon { font-size: 3.5rem; margin-bottom: 12px; }
    .success-card h3 { margin: 0 0 8px; font-size: 1.2rem; color: #1e293b; }
    .success-card p { color: #64748b; margin: 0; }

    /* ── Toast ── */
    .toast { position: fixed; bottom: 30px; right: 30px; padding: 14px 24px; border-radius: 12px; font-weight: 700; font-size: 0.9rem; opacity: 0; transform: translateY(20px); transition: all 0.3s; z-index: 9999; pointer-events: none; min-width: 260px; text-align: center; }
    .toast.show    { opacity: 1; transform: translateY(0); }
    .toast.success { background: #059669; color: white; }
    .toast.error   { background: #dc2626; color: white; }
  `]
})
export class AvocatHonorairesComponent implements OnInit {

  affaire:    any  = null;
  affaireId!: number;
  loading   = false;

  // ── PV ──
  pvTexte      = '';
  pvStatut     = '';
  pvHistorique: any[] = [];
  pvFiles:      File[] = [];
  savingPV     = false;

  // ── Facture ──
  facture          = { ref: '', montant: 0 };
  factureStatut    = '';
  savingFacture    = false;
  editFacture      = false;

  // ── Toast ──
  toastVisible = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

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
      next: (res: any) => {
        this.affaire       = res;
        this.pvStatut      = res.pvStatut      || '';
        this.pvTexte       = res.pvTexte       || '';
        this.factureStatut = res.factureStatut || '';
        this.facture = {
          ref:     res.factureRef     || '',
          montant: res.montantFacture || 0
        };

        // Historique PV avec fichiers
        if (res.pvTexte) {
          this.pvHistorique = [{
            statut:   res.pvStatut || 'EN_ATTENTE',
            texte:    res.pvTexte  || '',
            fichiers: (res.pvFichiers ?? []).map((f: any) => ({
              nom:      f.nom,
              typeMime: f.typeMime,
              base64:   f.base64
            }))
          }];
        } else {
          this.pvHistorique = [];
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
  // Helpers affichage
  // ════════════════════════════════════════

  hasPV(): boolean {
    return !!this.affaire?.pvTexte;
  }

  hasFacture(): boolean {
    return !!this.affaire?.factureRef;
  }

  pvFichiers(): any[] {
    return this.pvHistorique[0]?.fichiers ?? [];
  }

  pvBadgeClass(): string {
    const map: Record<string, string> = {
      'EN_ATTENTE': 'badge-warning',
      'VALIDE':     'badge-success',
      'REFUSE':     'badge-danger'
    };
    return map[this.pvStatut] ?? 'badge-warning';
  }

  factureBadgeClass(): string {
    const map: Record<string, string> = {
      'EN_ATTENTE': 'badge-warning',
      'PAYEE':      'badge-success',
      'REJETEE':    'badge-danger'
    };
    return map[this.factureStatut] ?? 'badge-warning';
  }

  pvStatutLabel(): string {
    const map: Record<string, string> = {
      'EN_ATTENTE': '⏳ En attente',
      'VALIDE':     '✅ Validé',
      'REFUSE':     '❌ Refusé'
    };
    return map[this.pvStatut] ?? this.pvStatut;
  }

  factureStatutLabel(): string {
    const map: Record<string, string> = {
      'EN_ATTENTE': '⏳ En attente',
      'PAYEE':      '✅ Payée',
      'REJETEE':    '❌ Rejetée'
    };
    return map[this.factureStatut] ?? this.factureStatut;
  }

  statutClass(statut: string): string {
    const map: Record<string, string> = {
      'EN_COURS':       'pill-blue',
      'JUGEMENT_RENDU': 'pill-green'
    };
    return map[statut] ?? 'pill-grey';
  }

  // ════════════════════════════════════════
  // Actions PV
  // ════════════════════════════════════════

  editerPV(): void {
    this.pvTexte = this.affaire?.pvTexte || '';
    this.pvFiles = [];
  }

  annulerPV(): void {
    this.pvTexte = this.affaire?.pvTexte || '';
    this.pvFiles = [];
  }

  soumettrePV(): void {
    if (!this.pvTexte.trim()) {
      this.showToast('Le contenu du PV est obligatoire.', 'error');
      return;
    }
    this.savingPV = true;

    if (this.pvFiles.length === 0) {
      this.avocatService.soumettrePV(this.affaireId, this.pvTexte).subscribe({
        next:  () => this.onPVSuccess(),
        error: (e) => this.onPVError(e)
      });
    } else {
      const formData = new FormData();
      formData.append('pvTexte', this.pvTexte.trim());
      this.pvFiles.forEach(f => formData.append('piecesJointes', f, f.name));
      this.avocatService.soumettrePVAvecFichiers(this.affaireId, formData).subscribe({
        next:  () => this.onPVSuccess(),
        error: (e) => this.onPVError(e)
      });
    }
  }

  private onPVSuccess(): void {
    this.savingPV = false;
    this.pvFiles  = [];
    this.showToast('✓ PV soumis avec succès !', 'success');
    setTimeout(() => this.chargerAffaire(), 500);
  }

  private onPVError(e: any): void {
    this.savingPV = false;
    const msg = e?.error?.error || e?.error?.message || e?.message
                || 'Erreur lors de la soumission du PV.';
    this.showToast(msg, 'error');
  }

  supprimerFichierPV(index: number): void {
    if (!this.pvHistorique[0]?.fichiers) return;
    this.pvHistorique[0].fichiers.splice(index, 1);
    this.avocatService.supprimerFichierPV(this.affaireId, index).subscribe({
      next:  () => this.showToast('Fichier supprimé.', 'success'),
      error: () => this.showToast('Erreur suppression.', 'error')
    });
  }

  // ════════════════════════════════════════
  // Actions Facture
  // ════════════════════════════════════════

  editerFacture(): void {
    this.facture = {
      ref:     this.affaire?.factureRef     || '',
      montant: this.affaire?.montantFacture || 0
    };
  }

  annulerFacture(): void {
    this.facture = {
      ref:     this.affaire?.factureRef     || '',
      montant: this.affaire?.montantFacture || 0
    };
  }

  soumettreFacture(): void {
    const ref     = this.facture.ref?.trim();
    const montant = Number(this.facture.montant);

    if (!ref) {
      this.showToast('La référence est obligatoire.', 'error');
      return;
    }
    if (!montant || montant <= 0) {
      this.showToast('Le montant doit être supérieur à 0.', 'error');
      return;
    }

    this.savingFacture = true;
    this.avocatService.soumettreFacture(
      this.affaireId,
      { factureRef: ref, montantFacture: montant }
    ).subscribe({
      next: () => {
        this.savingFacture = false;
        this.facture       = { ref: '', montant: 0 };
        this.showToast('✓ Facture soumise avec succès !', 'success');
        setTimeout(() => this.chargerAffaire(), 500);
      },
      error: (e: any) => {
        this.savingFacture = false;
        const msg = e?.error?.error || e?.error?.message || e?.message
                    || 'Erreur soumission facture.';
        this.showToast(msg, 'error');
      }
    });
  }

  // ════════════════════════════════════════
  // Fichiers
  // ════════════════════════════════════════

  onPVFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    const newFiles = Array.from(input.files).filter(f => {
      if (f.size > 10 * 1024 * 1024) {
        this.showToast(`"${f.name}" dépasse 10 Mo.`, 'error');
        return false;
      }
      return true;
    });
    this.pvFiles = [...this.pvFiles, ...newFiles];
    input.value  = '';
  }

  removePVFile(index: number, event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.pvFiles = this.pvFiles.filter((_, i) => i !== index);
  }

  buildDownloadUrl(fichier: any): string {
    return `data:${fichier.typeMime};base64,${fichier.base64}`;
  }

  getFileIcon(filename: string): string {
    const ext = filename?.split('.').pop()?.toLowerCase();
    const icons: Record<string, string> = {
      pdf: '📄', doc: '📝', docx: '📝',
      jpg: '🖼️', jpeg: '🖼️', png: '🖼️',
      xls: '📊', xlsx: '📊', txt: '📃'
    };
    return icons[ext ?? ''] ?? '📎';
  }

  formatSize(bytes: number): string {
    if (bytes < 1024)        return bytes + ' o';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' Ko';
    return (bytes / (1024 * 1024)).toFixed(1) + ' Mo';
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