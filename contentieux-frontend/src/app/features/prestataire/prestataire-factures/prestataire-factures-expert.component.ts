import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PrestataireService } from '../../../core/services/prestataire.service';

@Component({
  selector: 'app-prestataire-factures-expert',
  standalone: true,
  imports: [CommonModule],
  template: `
<div class="pf-shell">

  <!-- ══ TOPBAR ═══════════════════════════════════════════════════ -->
  <div class="pf-topbar">
    <div>
      <div class="pf-eyebrow">Espace prestataire</div>
      <h1 class="pf-title">Mes Factures</h1>
    </div>
    <div class="pf-counter" *ngIf="!loading && factures.length > 0">
      {{ factures.length }} facture{{ factures.length > 1 ? 's' : '' }}
    </div>
  </div>

  <!-- ══ LOADING ══════════════════════════════════════════════════ -->
  <div class="pf-loading" *ngIf="loading">
    <div class="pf-loading-track"><div class="pf-loading-bar"></div></div>
    <p class="pf-loading-label">Chargement des factures…</p>
  </div>

  <!-- ══ CONTENU ══════════════════════════════════════════════════ -->
  <ng-container *ngIf="!loading">

    <!-- Vide -->
    <div class="pf-empty" *ngIf="factures.length === 0">
      <div class="pf-empty-icon">
        <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="1.2">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
          <line x1="16" y1="13" x2="8" y2="13"/>
          <line x1="16" y1="17" x2="8" y2="17"/>
        </svg>
      </div>
      <p class="pf-empty-msg">Aucune facture soumise pour le moment.</p>
    </div>

    <!-- Table -->
    <div class="pf-table-wrap" *ngIf="factures.length > 0">
      <table class="pf-table">
        <thead>
          <tr>
            <th>Référence</th>
            <th>Mission</th>
            <th>Dossier / Client</th>
            <th class="pf-th-num">Montant HT</th>
            <th>Soumise le</th>
            <th>Statut</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let f of factures" class="pf-row">

            <!-- Référence -->
            <td>
              <span class="pf-ref">{{ f.factureRef || f.reference || '—' }}</span>
            </td>

            <!-- Mission -->
            <td>
              <span class="pf-mission-num" *ngIf="f.mission?.numeroMission">
                {{ f.mission.numeroMission }}
              </span>
              <span class="pf-dash" *ngIf="!f.mission?.numeroMission">—</span>
            </td>

            <!-- Dossier / Client -->
            <td>
              <ng-container *ngIf="f.mission?.prestation?.dossier as d">
                <span class="pf-dossier-num">{{ d.numeroDossier }}</span>
                <span class="pf-client">{{ d.client?.nom }} {{ d.client?.prenom }}</span>
              </ng-container>
              <span class="pf-dash" *ngIf="!f.mission?.prestation?.dossier">—</span>
            </td>

            <!-- Montant -->
            <td class="pf-td-num">
              <span class="pf-montant">{{ f.montant | number:'1.3-3' }}</span>
              <span class="pf-devise">TND</span>
            </td>

            <!-- Date -->
            <td>
              <span class="pf-date">{{ f.dateSoumission | date:'dd/MM/yyyy' }}</span>
            </td>

            <!-- Statut -->
            <td>
              <span class="pf-statut" [ngClass]="getStatutClass(f.statut)">
                <span class="pf-statut-dot"></span>
                {{ getStatutLabel(f.statut) }}
              </span>
            </td>

          </tr>
        </tbody>
      </table>
    </div>

  </ng-container>

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
      --font:     'Inter', -apple-system, sans-serif;
    }

    /* ── Shell ───────────────────────────────────────────────────── */
    .pf-shell {
      font-family: var(--font);
      padding: 0 0 4rem;
      max-width: 1080px;
      margin: 0 auto;
      color: var(--ink);
    }

    /* ── Topbar ──────────────────────────────────────────────────── */
    .pf-topbar {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      padding: 2rem 0 1.5rem;
      border-bottom: 1px solid var(--border);
      margin-bottom: 1.75rem;
    }
    .pf-eyebrow {
      font-size: .72rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: .08em;
      color: var(--mist);
      margin-bottom: 4px;
    }
    .pf-title {
      margin: 0;
      font-size: 1.65rem;
      font-weight: 700;
      color: var(--navy);
      letter-spacing: -.025em;
    }
    .pf-counter {
      font-size: .78rem;
      font-weight: 600;
      color: var(--mist);
      background: var(--surface);
      border: 1px solid var(--border);
      padding: 6px 14px;
      border-radius: 99px;
    }

    /* ── Loading ─────────────────────────────────────────────────── */
    .pf-loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 6rem 2rem;
      gap: 1.25rem;
    }
    .pf-loading-track {
      width: 220px; height: 3px;
      background: var(--border);
      border-radius: 99px;
      overflow: hidden;
    }
    .pf-loading-bar {
      height: 100%;
      background: linear-gradient(90deg, var(--navy-mid), #2563eb);
      border-radius: 99px;
      animation: pf-slide 1.4s ease-in-out infinite;
    }
    @keyframes pf-slide {
      0%   { width: 0; margin-left: 0; }
      50%  { width: 60%; margin-left: 20%; }
      100% { width: 0; margin-left: 100%; }
    }
    .pf-loading-label {
      font-size: .85rem;
      color: var(--mist);
      margin: 0;
      letter-spacing: .02em;
    }

    /* ── Vide ────────────────────────────────────────────────────── */
    .pf-empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 5rem 2rem;
      gap: .75rem;
      text-align: center;
    }
    .pf-empty-msg {
      margin: 0;
      color: var(--mist);
      font-size: .95rem;
    }

    /* ── Table wrapper ───────────────────────────────────────────── */
    .pf-table-wrap {
      background: var(--white);
      border: 1px solid var(--border);
      border-radius: 12px;
      overflow: hidden;
    }
    .pf-table {
      width: 100%;
      border-collapse: collapse;
    }

    /* ── Thead ───────────────────────────────────────────────────── */
    .pf-table thead {
      background: var(--navy);
    }
    .pf-table th {
      padding: 14px 18px;
      text-align: left;
      font-size: .72rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: .07em;
      color: rgba(255,255,255,.75);
      white-space: nowrap;
    }
    .pf-th-num { text-align: right; }

    /* ── Rows ────────────────────────────────────────────────────── */
    .pf-row {
      border-bottom: 1px solid var(--border);
      transition: background .12s;
      &:last-child { border-bottom: none; }
      &:hover { background: var(--surface); }
    }
    .pf-table td {
      padding: 14px 18px;
      vertical-align: middle;
      font-size: .87rem;
    }
    .pf-td-num { text-align: right; }

    /* ── Cellules ────────────────────────────────────────────────── */
    .pf-ref {
      font-family: 'SF Mono', 'Fira Code', monospace;
      font-size: .82rem;
      font-weight: 600;
      color: var(--navy-mid);
      background: #eef2ff;
      padding: 3px 8px;
      border-radius: 4px;
      letter-spacing: .02em;
    }
    .pf-mission-num {
      font-weight: 700;
      color: var(--navy);
      font-size: .88rem;
    }
    .pf-dash { color: var(--mist); }

    .pf-dossier-num {
      display: block;
      font-weight: 600;
      color: var(--ink);
      font-size: .87rem;
    }
    .pf-client {
      display: block;
      font-size: .78rem;
      color: var(--mist);
      margin-top: 2px;
    }

    .pf-montant {
      font-weight: 700;
      font-size: .97rem;
      color: #16a34a;
      letter-spacing: -.01em;
    }
    .pf-devise {
      font-size: .72rem;
      font-weight: 600;
      color: var(--mist);
      margin-left: 4px;
      text-transform: uppercase;
    }

    .pf-date {
      color: var(--steel);
      font-size: .85rem;
    }

    /* ── Statut pills ────────────────────────────────────────────── */
    .pf-statut {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 11px;
      border-radius: 99px;
      font-size: .72rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: .04em;
      white-space: nowrap;
    }
    .pf-statut-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: currentColor;
      flex-shrink: 0;
    }

    /* Variantes statut */
    .pf-s-attente {
      background: #fffbeb;
      color: #92400e;
      border: 1px solid #fde68a;
    }
    .pf-s-approuvee {
      background: #f0fdf4;
      color: #14532d;
      border: 1px solid #bbf7d0;
    }
    .pf-s-rejetee {
      background: #fef2f2;
      color: #991b1b;
      border: 1px solid #fecaca;
    }
    .pf-s-payee {
      background: #f0f9ff;
      color: #0c4a6e;
      border: 1px solid #bae6fd;
    }

    /* ── Responsive ──────────────────────────────────────────────── */
    @media (max-width: 700px) {
      .pf-table th:nth-child(3),
      .pf-table td:nth-child(3) { display: none; }
      .pf-topbar { flex-direction: column; gap: 10px; align-items: flex-start; }
    }
  `]
})
export class PrestataireFacturesComponent implements OnInit {
  factures: any[] = [];
  loading = true;

  constructor(private prestataireService: PrestataireService) {}

  ngOnInit() {
    this.prestataireService.getMesFactures().subscribe({
      next: (data) => { this.factures = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  getStatutClass(statut: string): string {
    const map: Record<string, string> = {
      EN_ATTENTE: 'pf-s-attente',
      APPROUVEE:  'pf-s-approuvee',
      REJETEE:    'pf-s-rejetee',
      PAYEE:      'pf-s-payee',
    };
    return map[statut] || 'pf-s-attente';
  }

  getStatutLabel(statut: string): string {
    const map: Record<string, string> = {
      EN_ATTENTE: 'En attente',
      APPROUVEE:  'Approuvée',
      REJETEE:    'Rejetée',
      PAYEE:      'Payée',
    };
    return map[statut] || statut || '—';
  }
}