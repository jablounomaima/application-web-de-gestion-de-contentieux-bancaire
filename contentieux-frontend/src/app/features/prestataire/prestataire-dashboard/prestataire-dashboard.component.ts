import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PrestataireService } from '../../../core/services/prestataire.service';
import { Router } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { NotificationService, NotificationDTO } from '../../../core/services/notification.service';

@Component({
  selector: 'app-prestataire-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div class="pd-shell">

  <!-- ══ TOPBAR ═══════════════════════════════════════════════════ -->
  <div class="pd-topbar">
    <div>
      <div class="pd-eyebrow">Espace prestataire</div>
      <h1 class="pd-title">Mon tableau de bord</h1>
    </div>
    <button class="pd-refresh" (click)="chargerDashboard()">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <polyline points="23 4 23 10 17 10"/>
        <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
      </svg>
      Actualiser
    </button>
  </div>

  <!-- ══ STATS DIAGRAM ═══════════════════════════════════════════════ -->
  <section class="pd-stats-panel" *ngIf="!loading">
  <div class="pd-chart-card">
    <div class="pd-chart-header">
      <h3>Répartition des missions</h3>
      <span>Vue synthétique</span>
    </div>
    <div class="pd-bars">
      <div class="pd-bar-item" *ngFor="let bar of chartBarsStatut">
        <div class="pd-bar-track">
          <div class="pd-bar-fill" [style.height.%]="bar.percent" [style.background]="bar.color"></div>
        </div>
        <div class="pd-bar-meta">
          <strong>{{ bar.value }}</strong>
          <span>{{ bar.label }}</span>
        </div>
      </div>
    </div>
  </div>

  <div class="pd-chart-card">
    <div class="pd-chart-header">
      <h3>Taux de validation des PV</h3>
      <span>Validés / soumis</span>
    </div>
    <div class="pd-ring-card">
      <svg class="pd-ring" viewBox="0 0 140 140" width="110" height="110">
        <circle cx="70" cy="70" r="54" class="pd-ring-bg"></circle>
        <circle cx="70" cy="70" r="54" class="pd-ring-progress"
                [style.strokeDasharray]="ringCircumference"
                [style.strokeDashoffset]="ringOffsetValidation"></circle>
      </svg>
      <div class="pd-ring-center">
        <strong>{{ tauxValidation }}%</strong>
        <span>{{ stats.missionsTerminees }} validé(s)</span>
      </div>
    </div>
  </div>
</section>

  <!-- ══ TABS ══════════════════════════════════════════════════════ -->
  <div class="pd-tabs">
    <button class="pd-tab" [class.pd-tab--on]="activeTab === 'enCours'" (click)="activeTab = 'enCours'">
      Missions en cours
    </button>
    <button class="pd-tab" [class.pd-tab--on]="activeTab === 'toutes'" (click)="activeTab = 'toutes'">
      Toutes les missions
    </button>
  </div>

  <!-- ══ LOADING ════════════════════════════════════════════════════ -->
  <div class="pd-loading" *ngIf="loading">
    <div class="pd-loading-track"><div class="pd-loading-bar"></div></div>
    <p class="pd-loading-label">Chargement…</p>
  </div>

  <!-- ══ TABLE ═════════════════════════════════════════════════════ -->
  <div class="pd-table-wrap" *ngIf="!loading">
    <table class="pd-table">
      <thead>
        <tr>
          <th>Mission</th>
          <th>Dossier / Client</th>
          <th>Type</th>
          <th>Assignée le</th>
          <th>Statut</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr *ngIf="missionsFiltrees.length === 0">
          <td colspan="6" class="pd-empty-cell">
            Aucune mission{{ activeTab === 'enCours' ? ' en cours' : '' }}
          </td>
        </tr>
        <tr *ngFor="let m of missionsFiltrees" class="pd-row">

          <!-- Mission -->
          <td>
            <span class="pd-mission-code">{{ m.numeroMission }}</span>
            <span class="pd-mission-id">#{{ m.id }}</span>
          </td>

          <!-- Dossier -->
          <td>
            <ng-container *ngIf="m.prestation?.dossier as d">
              <span class="pd-dossier-num">{{ d.numeroDossier }}</span>
              <span class="pd-dossier-meta">{{ d.client?.nom }} {{ d.client?.prenom }}</span>
              <span class="pd-dossier-meta">{{ d.libelle || '—' }}</span>
              <span class="pd-dossier-montant">{{ d.montant | number:'1.0-0' }} TND</span>
            </ng-container>
            <span class="pd-dash" *ngIf="!m.prestation?.dossier">—</span>
          </td>

          <!-- Type -->
          <td>
            <span class="pd-type-pill">{{ m.prestation?.type || '—' }}</span>
          </td>

          <!-- Date -->
          <td>
            <span class="pd-date">{{ m.dateAssignation | date:'dd/MM/yyyy' }}</span>
          </td>

          <!-- Statut -->
          <td>
            <span class="pd-statut" [ngClass]="getStatutClass(m.statut)">
              <span class="pd-statut-dot"></span>
              {{ getStatutLabel(m.statut) }}
            </span>
          </td>

          <!-- Actions -->
          <td>
            <div class="pd-actions">
              <button class="pd-btn-act pd-btn-act--ghost" (click)="voirDossier(m)" title="Voir le dossier">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
                Dossier
              </button>
              <button class="pd-btn-act pd-btn-act--primary" (click)="voirMission(m)" title="Voir la mission">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                </svg>
                Mission
              </button>
              <button class="pd-btn-act pd-btn-act--teal" (click)="ouvrirModalDocuments(m)" title="Documents">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66L9.41 17.41a2 2 0 0 1-2.83-2.83l8.49-8.48"/>
                </svg>
                Docs
              </button>
            </div>
          </td>

        </tr>
      </tbody>
    </table>
  </div>

</div>

<!-- ══════════════════════════════════════════════════════════════ -->
<!-- MODAL DOCUMENTS                                                 -->
<!-- ══════════════════════════════════════════════════════════════ -->
<div class="pd-modal-veil"
     *ngIf="missionSelectionnee && modalType === 'documents'"
     (click)="fermerModal()">
  <div class="pd-modal" (click)="$event.stopPropagation()">

    <div class="pd-modal-head">
      <div>
        <div class="pd-modal-eyebrow">Mission {{ missionSelectionnee.numeroMission }}</div>
        <h2 class="pd-modal-title">Documents joints</h2>
      </div>
      <button class="pd-modal-x" (click)="fermerModal()">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
        </svg>
      </button>
    </div>

    <div class="pd-modal-body">

      <!-- Feedback -->
      <div class="pd-feedback pd-feedback--ok"  *ngIf="succes">✓ {{ succes }}</div>
      <div class="pd-feedback pd-feedback--err" *ngIf="erreur">⚠ {{ erreur }}</div>

      <!-- Upload -->
      <label class="pd-drop-zone">
        <input type="file" multiple (change)="onFichiersSelectionnes($event)" style="display:none">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#aaa" stroke-width="1.5">
          <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66L9.41 17.41a2 2 0 0 1-2.83-2.83l8.49-8.48"/>
        </svg>
        <span>Glissez ou cliquez pour ajouter des fichiers</span>
      </label>

      <!-- Fichiers sélectionnés -->
      <div class="pd-file-list" *ngIf="fichierSelectionnes.length > 0">
        <div class="pd-file-row" *ngFor="let f of fichierSelectionnes">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
          </svg>
          <span class="pd-file-name">{{ f.name }}</span>
          <span class="pd-file-size">{{ formatTaille(f.size) }}</span>
        </div>
      </div>

      <button class="pd-upload-btn"
              (click)="uploaderDocuments()"
              [disabled]="uploadEnCours || fichierSelectionnes.length === 0">
        <span class="pd-spin" *ngIf="uploadEnCours"></span>
        {{ uploadEnCours ? 'Envoi en cours…' : 'Envoyer les fichiers' }}
      </button>

      <!-- Séparateur -->
      <div class="pd-sep">
        <span>Documents existants ({{ documentsExistants.length }})</span>
      </div>

      <!-- Docs existants -->
      <div class="pd-empty-docs" *ngIf="documentsExistants.length === 0">
        Aucun document envoyé pour cette mission.
      </div>
      <div class="pd-doc-item" *ngFor="let d of documentsExistants">
        <div class="pd-doc-left">
          <span class="pd-doc-icon">{{ iconeType(d.typeMime) }}</span>
          <div>
            <div class="pd-doc-name">{{ d.nomFichierOriginal }}</div>
            <div class="pd-doc-meta">
              {{ formatTaille(d.tailleFichier) }} · {{ d.dateUpload | date:'dd/MM/yyyy HH:mm' }}
            </div>
          </div>
        </div>
        <button class="pd-dl-btn" (click)="telechargerDocument(d.nomFichierServeur, d.nomFichierOriginal)">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          Télécharger
        </button>
      </div>

    </div>

    <div class="pd-modal-foot">
      <button class="pd-btn-outline" (click)="fermerModal()">Fermer</button>
    </div>

  </div>
</div>
  `,
  styles: [`
    /* ── Tokens ─────────────────────────────────────────────────── */
    :host {
      --navy:     #0d1b3e;
      --navy-mid: #1a2f5a;
      --ink:      #1c2333;
      --steel:    #4b5878;
      --mist:     #8b96ae;
      --border:   #e4e8f0;
      --surface:  #f7f8fc;
      --white:    #ffffff;
      --amber:    #f59e0b;
      --blue:     #2563eb;
      --green:    #16a34a;
      --purple:   #7c3aed;
      --teal:     #0d9488;
      --red:      #dc2626;
      --font:     'Inter', -apple-system, sans-serif;
    }

    /* ── Shell ───────────────────────────────────────────────────── */
    .pd-shell {
      font-family: var(--font);
      padding: 0 0 4rem;
      max-width: 1200px;
      margin: 0 auto;
      color: var(--ink);
    }

    /* ── Topbar ──────────────────────────────────────────────────── */
    .pd-topbar {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      padding: 2rem 0 1.5rem;
      border-bottom: 1px solid var(--border);
      margin-bottom: 1.75rem;
    }
    .pd-eyebrow {
      font-size: .72rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: .08em;
      color: var(--mist);
      margin-bottom: 4px;
    }
    .pd-title {
      margin: 0;
      font-size: 1.65rem;
      font-weight: 700;
      color: var(--navy);
      letter-spacing: -.025em;
    }
    .pd-refresh {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 8px 14px;
      background: var(--white);
      border: 1px solid var(--border);
      border-radius: 6px;
      font-size: .8rem;
      font-weight: 500;
      color: var(--steel);
      cursor: pointer;
      font-family: var(--font);
      transition: border-color .15s, color .15s;
      &:hover { border-color: var(--navy-mid); color: var(--navy); }
    }

    /* ── Stats diagram ───────────────────────────────────────────── */
    .pd-stats-diagram {
      margin-bottom: 1.75rem;
    }
    .pd-diagram-card {
      background: var(--white);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 22px 24px;
    }
    .pd-diagram-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 18px;
    }
    .pd-diagram-title-group {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .pd-diagram-icon {
      font-size: 1.4rem;
      width: 42px; height: 42px;
      display: flex; align-items: center; justify-content: center;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 10px;
      flex-shrink: 0;
    }
    .pd-diagram-title-group h3 {
      margin: 0;
      font-size: 1.02rem;
      font-weight: 700;
      color: var(--navy);
      letter-spacing: -.01em;
    }
    .pd-diagram-subtitle {
      font-size: .78rem;
      color: var(--mist);
      font-weight: 500;
    }

    .pd-diagram-body {
      display: flex;
      align-items: center;
      gap: 32px;
      flex-wrap: wrap;
    }

    /* Donut */
    .pd-donut {
      width: 140px;
      height: 140px;
      border-radius: 50%;
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
    }
    .pd-donut::before {
      content: '';
      position: absolute;
      width: 88px;
      height: 88px;
      background: var(--white);
      border-radius: 50%;
    }
    .pd-donut-center {
      position: relative;
      z-index: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    .pd-donut-center strong {
      font-size: 1.5rem;
      font-weight: 700;
      color: var(--navy);
      line-height: 1;
    }
    .pd-donut-center span {
      font-size: .68rem;
      text-transform: uppercase;
      letter-spacing: .06em;
      color: var(--mist);
      margin-top: 3px;
    }

    /* Legend */
    .pd-diagram-legend {
      list-style: none;
      margin: 0;
      padding: 0;
      flex: 1;
      min-width: 220px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .pd-legend-item {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .pd-legend-top {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .pd-legend-dot {
      width: 8px; height: 8px;
      border-radius: 50%;
      flex-shrink: 0;
    }
    .pd-legend--amber  .pd-legend-dot { background: var(--amber); }
    .pd-legend--purple .pd-legend-dot { background: var(--purple); }
    .pd-legend--green  .pd-legend-dot { background: var(--green); }
    .pd-legend--mist   .pd-legend-dot { background: var(--mist); }
    .pd-legend-label {
      font-size: .83rem;
      font-weight: 600;
      color: var(--ink);
      flex: 1;
    }
    .pd-legend-percent {
      font-size: .78rem;
      font-weight: 700;
      color: var(--steel);
    }
    .pd-legend-bar {
      width: 100%;
      height: 5px;
      background: var(--surface);
      border-radius: 99px;
      overflow: hidden;
    }
    .pd-legend-bar-fill {
      height: 100%;
      border-radius: 99px;
      transition: width .3s ease;
    }
    .pd-legend--amber  .pd-legend-bar-fill { background: var(--amber); }
    .pd-legend--purple .pd-legend-bar-fill { background: var(--purple); }
    .pd-legend--green  .pd-legend-bar-fill { background: var(--green); }
    .pd-legend--mist   .pd-legend-bar-fill { background: var(--mist); }
    .pd-legend-value {
      font-size: .72rem;
      color: var(--mist);
    }

    /* ── Tabs ────────────────────────────────────────────────────── */
    .pd-tabs {
      display: flex;
      gap: 6px;
      margin-bottom: 1.5rem;
    }
    .pd-tab {
      padding: 8px 18px;
      border: 1px solid var(--border);
      border-radius: 99px;
      background: var(--white);
      font-size: .82rem;
      font-weight: 500;
      color: var(--steel);
      cursor: pointer;
      font-family: var(--font);
      transition: all .15s;
    }
    .pd-tab:hover:not(.pd-tab--on) { border-color: var(--navy-mid); color: var(--navy); }
    .pd-tab--on { background: var(--navy); border-color: var(--navy); color: var(--white); font-weight: 600; }

    /* ── Loading ─────────────────────────────────────────────────── */
    .pd-loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 5rem;
      gap: 1.25rem;
    }
    .pd-loading-track {
      width: 220px; height: 3px;
      background: var(--border);
      border-radius: 99px;
      overflow: hidden;
    }
    .pd-loading-bar {
      height: 100%;
      background: linear-gradient(90deg, var(--navy-mid), var(--blue));
      border-radius: 99px;
      animation: pd-slide 1.4s ease-in-out infinite;
    }
    @keyframes pd-slide {
      0%   { width: 0; margin-left: 0; }
      50%  { width: 60%; margin-left: 20%; }
      100% { width: 0; margin-left: 100%; }
    }
    .pd-loading-label { font-size: .85rem; color: var(--mist); margin: 0; }

    /* ── Table ───────────────────────────────────────────────────── */
    .pd-table-wrap {
      background: var(--white);
      border: 1px solid var(--border);
      border-radius: 12px;
      overflow: hidden;
    }
    .pd-table {
      width: 100%;
      border-collapse: collapse;
    }
    .pd-table thead { background: var(--navy); }
    .pd-table th {
      padding: 13px 16px;
      text-align: left;
      font-size: .7rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: .07em;
      color: rgba(255,255,255,.7);
      white-space: nowrap;
    }
    .pd-row {
      border-bottom: 1px solid var(--border);
      transition: background .12s;
      &:last-child { border-bottom: none; }
      &:hover { background: var(--surface); }
    }
    .pd-table td {
      padding: 13px 16px;
      vertical-align: middle;
      font-size: .87rem;
    }
    .pd-empty-cell {
      text-align: center;
      padding: 3rem !important;
      color: var(--mist);
      font-style: italic;
    }

    /* ── Cellules ────────────────────────────────────────────────── */
    .pd-mission-code {
      display: block;
      font-weight: 700;
      color: var(--navy);
      font-size: .9rem;
    }
    .pd-mission-id {
      display: block;
      font-size: .72rem;
      color: var(--mist);
      margin-top: 2px;
    }
    .pd-dossier-num {
      display: block;
      font-weight: 600;
      color: var(--ink);
      font-size: .88rem;
    }
    .pd-dossier-meta {
      display: block;
      font-size: .75rem;
      color: var(--mist);
      margin-top: 1px;
    }
    .pd-dossier-montant {
      display: block;
      font-size: .75rem;
      font-weight: 600;
      color: var(--green);
      margin-top: 3px;
    }
    .pd-dash { color: var(--mist); }
    .pd-type-pill {
      background: #eef2ff;
      color: #3730a3;
      padding: 3px 10px;
      border-radius: 99px;
      font-size: .75rem;
      font-weight: 600;
      white-space: nowrap;
    }
    .pd-date { font-size: .84rem; color: var(--steel); }

    /* ── Statut ──────────────────────────────────────────────────── */
    .pd-statut {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 10px;
      border-radius: 99px;
      font-size: .72rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: .04em;
      white-space: nowrap;
    }
    .pd-statut-dot {
      width: 6px; height: 6px;
      border-radius: 50%;
      background: currentColor;
      flex-shrink: 0;
    }
    .pd-s-assignee       { background: #fffbeb; color: #92400e; border: 1px solid #fde68a; }
    .pd-s-en-cours       { background: #eff6ff; color: #1e40af; border: 1px solid #bfdbfe; }
    .pd-s-pv-soumis      { background: #f5f3ff; color: #5b21b6; border: 1px solid #ddd6fe; }
    .pd-s-facture        { background: #f0fdf4; color: #14532d; border: 1px solid #bbf7d0; }
    .pd-s-facture-rej    { background: #fff7ed; color: #9a3412; border: 1px solid #fed7aa; }
    .pd-s-facture-val    { background: #ecfdf5; color: #065f46; border: 1px solid #a7f3d0; }
    .pd-s-terminee       { background: #f8fafc; color: #475569; border: 1px solid #e2e8f0; }
    .pd-s-rejetee        { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
    .pd-s-annulee        { background: #f1f5f9; color: #64748b; border: 1px solid #e2e8f0; }

    /* ── Boutons actions ─────────────────────────────────────────── */
    .pd-actions {
      display: flex;
      gap: 6px;
      flex-wrap: nowrap;
    }
    .pd-btn-act {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 6px 11px;
      border-radius: 6px;
      border: none;
      font-size: .76rem;
      font-weight: 600;
      cursor: pointer;
      font-family: var(--font);
      white-space: nowrap;
      transition: all .15s;
    }
    .pd-btn-act--ghost {
      background: var(--surface);
      color: var(--steel);
      border: 1px solid var(--border);
    }
    .pd-btn-act--ghost:hover { background: var(--border); }
    .pd-btn-act--primary {
      background: var(--navy);
      color: var(--white);
    }
    .pd-btn-act--primary:hover { background: var(--navy-mid); }
    .pd-btn-act--teal {
      background: #ccfbf1;
      color: #115e59;
    }
    .pd-btn-act--teal:hover { background: #99f6e4; }

    /* ══ MODAL ══════════════════════════════════════════════════════ */
    .pd-modal-veil {
      position: fixed;
      inset: 0;
      background: rgba(13,27,62,.45);
      backdrop-filter: blur(2px);
      z-index: 1000;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;
    }
    .pd-modal {
      background: var(--white);
      border-radius: 14px;
      width: 620px;
      max-width: 100%;
      max-height: 90vh;
      display: flex;
      flex-direction: column;
      box-shadow: 0 24px 60px rgba(13,27,62,.22);
      animation: pd-modal-in .2s ease;
    }
    @keyframes pd-modal-in {
      from { opacity: 0; transform: translateY(14px) scale(.98); }
      to   { opacity: 1; transform: translateY(0) scale(1); }
    }
    .pd-modal-head {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding: 22px 24px 18px;
      border-bottom: 1px solid var(--border);
      background: var(--surface);
      flex-shrink: 0;
      border-radius: 14px 14px 0 0;
    }
    .pd-modal-eyebrow {
      font-size: .7rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: .08em;
      color: var(--mist);
      margin-bottom: 3px;
    }
    .pd-modal-title {
      margin: 0;
      font-size: 1.05rem;
      font-weight: 700;
      color: var(--navy);
      letter-spacing: -.015em;
    }
    .pd-modal-x {
      background: var(--border);
      border: none;
      width: 30px; height: 30px;
      border-radius: 50%;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--steel);
      transition: background .15s;
      &:hover { background: #d1d5db; }
    }
    .pd-modal-body {
      padding: 22px 24px;
      flex: 1;
      overflow-y: auto;
    }
    .pd-modal-foot {
      display: flex;
      justify-content: flex-end;
      padding: 14px 24px;
      border-top: 1px solid var(--border);
      background: var(--surface);
      border-radius: 0 0 14px 14px;
      flex-shrink: 0;
    }
    .pd-btn-outline {
      padding: 9px 18px;
      border: 1px solid var(--border);
      border-radius: 6px;
      background: var(--white);
      color: var(--steel);
      font-size: .85rem;
      font-weight: 600;
      cursor: pointer;
      font-family: var(--font);
      &:hover { background: var(--surface); }
    }

    /* ── Feedback ────────────────────────────────────────────────── */
    .pd-feedback {
      padding: 10px 14px;
      border-radius: 6px;
      font-size: .85rem;
      font-weight: 500;
      margin-bottom: 16px;
    }
    .pd-feedback--ok  { background: #f0fdf4; color: #15803d; border: 1px solid #bbf7d0; }
    .pd-feedback--err { background: #fef2f2; color: var(--red); border: 1px solid #fecaca; }

    /* ── Drop zone ───────────────────────────────────────────────── */
    .pd-drop-zone {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 26px;
      border: 1.5px dashed var(--border);
      border-radius: 10px;
      cursor: pointer;
      text-align: center;
      font-size: .83rem;
      color: var(--mist);
      background: var(--surface);
      margin-bottom: 12px;
      transition: border-color .15s, color .15s, background .15s;
      &:hover { border-color: var(--navy-mid); color: var(--navy); background: var(--white); }
    }
    .pd-file-list { margin-bottom: 12px; display: flex; flex-direction: column; gap: 5px; }
    .pd-file-row {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 7px 12px;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 6px;
      font-size: .8rem;
    }
    .pd-file-name { flex: 1; color: var(--ink); font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .pd-file-size { color: var(--mist); font-size: .74rem; }

    .pd-upload-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 9px 18px;
      background: var(--navy);
      color: var(--white);
      border: none;
      border-radius: 6px;
      font-size: .85rem;
      font-weight: 600;
      cursor: pointer;
      font-family: var(--font);
      margin-bottom: 20px;
      transition: background .15s;
      &:hover:not(:disabled) { background: var(--navy-mid); }
      &:disabled { opacity: .5; cursor: not-allowed; }
    }
    .pd-spin {
      width: 13px; height: 13px;
      border: 2px solid rgba(255,255,255,.3);
      border-top-color: var(--white);
      border-radius: 50%;
      animation: pd-spin .65s linear infinite;
      display: inline-block;
    }
    @keyframes pd-spin { to { transform: rotate(360deg); } }

    /* ── Séparateur ──────────────────────────────────────────────── */
    .pd-sep {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 14px;
      font-size: .72rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: .07em;
      color: var(--mist);
      &::before, &::after {
        content: '';
        flex: 1;
        height: 1px;
        background: var(--border);
      }
    }

    /* ── Docs existants ──────────────────────────────────────────── */
    .pd-empty-docs { color: var(--mist); font-size: .85rem; font-style: italic; padding: 12px 0; }
    .pd-doc-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 14px;
      border: 1px solid var(--border);
      border-radius: 8px;
      margin-bottom: 8px;
      &:last-child { margin-bottom: 0; }
    }
    .pd-doc-left { display: flex; align-items: center; gap: 10px; }
    .pd-doc-icon { font-size: 1.3rem; }
    .pd-doc-name { font-size: .88rem; font-weight: 600; color: var(--ink); }
    .pd-doc-meta { font-size: .74rem; color: var(--mist); margin-top: 2px; }
    .pd-dl-btn {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 6px 12px;
      background: #eff6ff;
      color: #1d4ed8;
      border: none;
      border-radius: 6px;
      font-size: .78rem;
      font-weight: 600;
      cursor: pointer;
      font-family: var(--font);
      transition: background .15s;
      &:hover { background: #dbeafe; }
    }

    /* ── Responsive ──────────────────────────────────────────────── */
    @media (max-width: 768px) {
      .pd-table th:nth-child(2),
      .pd-table td:nth-child(2) { display: none; }
      .pd-diagram-body { flex-direction: column; align-items: flex-start; }
      .pd-diagram-legend { min-width: 100%; }
      .pd-topbar { flex-direction: column; gap: 12px; align-items: flex-start; }
    }

    /* ── Stats panel (barres + anneau) ──────────────────────────────── */
.pd-stats-panel {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 1.75rem;
}
.pd-chart-card {
  background: var(--white);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 20px 22px;
}
.pd-chart-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 18px;
}
.pd-chart-header h3 {
  margin: 0;
  font-size: .95rem;
  font-weight: 600;
  color: var(--navy);
}
.pd-chart-header span {
  font-size: .74rem;
  color: var(--mist);
}

/* Barres */
.pd-bars {
  display: flex;
  align-items: flex-end;
  gap: 20px;
  height: 130px;
}
.pd-bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex: 1;
  height: 100%;
  justify-content: flex-end;
}
.pd-bar-track {
  width: 28px;
  height: 90px;
  background: var(--surface);
  border-radius: 6px;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}
.pd-bar-fill {
  width: 100%;
  border-radius: 6px 6px 0 0;
  transition: height .3s ease;
}
.pd-bar-meta {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.pd-bar-meta strong {
  font-size: .95rem;
  font-weight: 600;
  color: var(--navy);
}
.pd-bar-meta span {
  font-size: .7rem;
  color: var(--mist);
  text-align: center;
}

/* Anneau */
.pd-ring-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  height: 130px;
}
.pd-ring {
  transform: rotate(-90deg);
}
.pd-ring-bg {
  fill: none;
  stroke: var(--border);
  stroke-width: 10;
}
.pd-ring-progress {
  fill: none;
  stroke: var(--blue);
  stroke-width: 10;
  stroke-linecap: round;
  transition: stroke-dashoffset .4s ease;
}
.pd-ring-center {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.pd-ring-center strong {
  font-size: 1.3rem;
  font-weight: 600;
  color: var(--navy);
}
.pd-ring-center span {
  font-size: .7rem;
  color: var(--mist);
  margin-top: 2px;
}
  `]
})
export class PrestataireDashboardComponent implements OnInit, OnDestroy {

  stats: any = null;
  missions: any[] = [];
  loading = true;
  activeTab: 'enCours' | 'toutes' = 'enCours';

  missionSelectionnee: any = null;
  modalType: 'documents' | null = null;
  erreur  = '';
  succes  = '';

  fichierSelectionnes: File[] = [];
  uploadEnCours = false;
  documentsExistants: any[] = [];
  private destroy$ = new Subject<void>();

  constructor(
    private prestataireService: PrestataireService,
    private router: Router,
    private route: ActivatedRoute,
    private notifService: NotificationService
  ) {}

  ngOnInit() {
    this.chargerDashboard();

    this.notifService.nouvelleNotif$.pipe(
      takeUntil(this.destroy$)
    ).subscribe((notif: NotificationDTO) => {
      const types = [
        'NOUVELLE_MISSION', 'MISSION_MODIFIEE', 'MISSION_CLOTUREE',
        'MISSION_REJETEE', 'VALIDATION_FINANCIERE_OK'
      ];
      if (types.includes(notif.type)) {
        this.chargerDashboardAvecRetry();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  chargerDashboard(): void {
    this.loading = true;
    this.prestataireService.getDashboard().subscribe({
      next: (data) => {
        console.log('Dashboard data:', data); // ← vérifier les noms exacts des champs
        this.stats    = data;
        this.missions = data.dernieresMissions || [];
        this.loading  = false;
      },
      error: () => { this.loading = false; }
    });
  }

  private chargerDashboardAvecRetry(tentative = 0): void {
    const MAX      = 5;
    const DELAI_MS = [500, 1000, 2000, 3000, 5000];

    this.prestataireService.getDashboard().subscribe({
      next: (data) => {
        const nouvelles = data.dernieresMissions || [];
        if (nouvelles.length <= this.missions.length && tentative < MAX) {
          setTimeout(() => this.chargerDashboardAvecRetry(tentative + 1), DELAI_MS[tentative]);
          return;
        }
        this.stats    = data;
        this.missions = nouvelles;
        this.loading  = false;
      },
      error: () => {
        if (tentative < MAX) {
          setTimeout(() => this.chargerDashboardAvecRetry(tentative + 1), DELAI_MS[tentative]);
        }
      }
    });
  }

  get missionsFiltrees(): any[] {
    if (this.activeTab === 'enCours') {
      return this.missions.filter(m =>
        ['ASSIGNEE', 'EN_COURS', 'PV_SOUMIS'].includes(m.statut)
      );
    }
    return this.missions;
  }

  /** Missions ne rentrant pas dans en cours / PV soumis / terminées (assignée, facture soumise/rejetée, rejetée…) */
  get autresMissions(): number {
    if (!this.stats) return 0;
    const total = this.stats.totalMissions || 0;
    const compte = (this.stats.missionsEnCours || 0)
      + (this.stats.pvSoumis || 0)
      + (this.stats.missionsTerminees || 0);
    return Math.max(total - compte, 0);
  }

  /** Pourcentage d'une valeur par rapport au total de missions */
  getStatPercent(valeur: number): number {
    if (!this.stats || !this.stats.totalMissions) return 0;
    return Math.round(((valeur || 0) / this.stats.totalMissions) * 100);
  }

  /** Gradient conique pour le donut, basé sur les 4 segments */
  get donutGradient(): string {
    if (!this.stats || !this.stats.totalMissions) {
      return `conic-gradient(var(--border) 0deg 360deg)`;
    }
    const total = this.stats.totalMissions;
    const segments = [
      { valeur: this.stats.missionsEnCours || 0,   couleur: 'var(--amber)'  },
      { valeur: this.stats.pvSoumis || 0,          couleur: 'var(--purple)' },
      { valeur: this.stats.missionsTerminees || 0, couleur: 'var(--green)'  },
      { valeur: this.autresMissions,               couleur: 'var(--border)' },
    ];

    let curseur = 0;
    const stops: string[] = [];
    segments.forEach(seg => {
      const deg = (seg.valeur / total) * 360;
      if (deg > 0) {
        stops.push(`${seg.couleur} ${curseur}deg ${curseur + deg}deg`);
        curseur += deg;
      }
    });
    return stops.length ? `conic-gradient(${stops.join(', ')})` : `conic-gradient(var(--border) 0deg 360deg)`;
  }

  getStatutClass(statut: string): string {
    const map: Record<string, string> = {
      ASSIGNEE:        'pd-s-assignee',
      EN_COURS:        'pd-s-en-cours',
      PV_SOUMIS:       'pd-s-pv-soumis',
      FACTURE_SOUMISE: 'pd-s-facture',
      FACTURE_VALIDEE: 'pd-s-facture-val',
      FACTURE_PAYEE:   'pd-s-terminee',
      FACTURE_REJETEE: 'pd-s-facture-rej',
      REALISEE:        'pd-s-terminee',
      VALIDEE_AGENT:   'pd-s-terminee',
      TERMINEE:        'pd-s-terminee',
      REJETEE:         'pd-s-rejetee',
      ANNULEE:         'pd-s-annulee',
    };
    return map[statut] || '';
  }

  getStatutLabel(statut: string): string {
    const map: Record<string, string> = {
      ASSIGNEE:        'Assignée',
      EN_COURS:        'En cours',
      PV_SOUMIS:       'PV soumis',
      FACTURE_SOUMISE: 'Facture soumise',
      FACTURE_VALIDEE: 'Facture validée',
      FACTURE_PAYEE:   'Facture payée',
      FACTURE_REJETEE: 'Facture rejetée',
      REALISEE:        'Réalisée',
      VALIDEE_AGENT:   'Validée',
      TERMINEE:        'Terminée',
      REJETEE:         'Rejetée',
      ANNULEE:         'Annulée',
    };
    return map[statut] || statut;
  }

  voirMission(m: any) { this.router.navigate(['/prestataire/missions', m.id]); }
  voirDossier(m: any) { this.router.navigate(['/prestataire/mission', m.id, 'dossier']); }

  fermerModal() {
    this.missionSelectionnee = null;
    this.modalType = null;
    this.erreur = '';
    this.succes = '';
    this.fichierSelectionnes = [];
    this.documentsExistants = [];
  }

  ouvrirModalDocuments(mission: any) {
    this.missionSelectionnee = mission;
    this.modalType = 'documents';
    this.fichierSelectionnes = [];
    this.erreur = '';
    this.succes = '';
    this.documentsExistants = [];

    this.prestataireService.getDocuments(mission.id).subscribe({
      next: (res) => this.documentsExistants = res.fichiers || [],
      error: () => {}
    });
  }

  onFichiersSelectionnes(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files) this.fichierSelectionnes = Array.from(input.files);
  }

  uploaderDocuments() {
    if (this.fichierSelectionnes.length === 0) { this.erreur = 'Sélectionnez au moins un fichier.'; return; }
    this.erreur = '';
    this.succes = '';
    this.uploadEnCours = true;

    const formData = new FormData();
    this.fichierSelectionnes.forEach(f => formData.append('fichiers', f, f.name));

    this.prestataireService.uploaderDocuments(this.missionSelectionnee.id, formData).subscribe({
      next: (res: any) => {
        this.succes = `${res.fichiers?.length ?? this.fichierSelectionnes.length} document(s) ajouté(s) avec succès.`;
        this.uploadEnCours = false;
        this.fichierSelectionnes = [];
        this.prestataireService.getDocuments(this.missionSelectionnee.id).subscribe({
          next: (r: any) => this.documentsExistants = r.fichiers || [],
          error: () => {}
        });
      },
      error: (err: any) => {
        this.erreur = err?.error?.message || err?.error?.error || "Erreur lors de l'upload.";
        this.uploadEnCours = false;
      }
    });
  }

  formatTaille(taille: number): string {
    if (!taille) return '0 B';
    if (taille < 1024) return taille + ' B';
    if (taille < 1024 * 1024) return (taille / 1024).toFixed(1) + ' KB';
    return (taille / (1024 * 1024)).toFixed(1) + ' MB';
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

  telechargerDocument(nomServeur: string, nomOriginal: string) {
    this.prestataireService.telechargerFichier(nomServeur).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = nomOriginal; a.click();
        URL.revokeObjectURL(url);
      }
    });
  }



  /** Barres : répartition des missions par statut (remplace le donut) */
  get chartBarsStatut(): { label: string; value: number; percent: number; color: string }[] {
    if (!this.stats || !this.stats.totalMissions) return [];

    const data = [
      { label: 'En cours',  value: this.stats.missionsEnCours || 0,   color: 'var(--amber)'  },
      { label: 'PV soumis', value: this.stats.pvSoumis || 0,          color: 'var(--purple)' },
      { label: 'Terminées', value: this.stats.missionsTerminees || 0, color: 'var(--green)'  },
      { label: 'Autres',    value: this.autresMissions,               color: 'var(--mist)'   },
    ];

    const max = Math.max(...data.map(d => d.value), 1);

    return data.map(d => ({ ...d, percent: Math.round((d.value / max) * 100) }));
  }
/** Circonférence du cercle SVG (rayon = 54) */
readonly ringCircumference = 2 * Math.PI * 54; // ≈ 339.29
  /** Anneau : taux de validation des PV (missions terminées / PV soumis au total) */
  get tauxValidation(): number {
    if (!this.stats) return 0;
    const soumisTotal = (this.stats.pvSoumis || 0) + (this.stats.missionsTerminees || 0);
    if (!soumisTotal) return 0;
    return Math.round(((this.stats.missionsTerminees || 0) / soumisTotal) * 100);
  }

  get ringOffsetValidation(): number {
    const progress = this.tauxValidation / 100;
    return this.ringCircumference * (1 - progress);
  }
}