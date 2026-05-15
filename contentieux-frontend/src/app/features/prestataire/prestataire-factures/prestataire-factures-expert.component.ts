import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PrestataireService } from '../../../core/services/prestataire.service';

@Component({
  selector: 'app-prestataire-factures-expert',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>💳 Mes Factures</h1>
        <p class="subtitle">Historique de vos factures soumises</p>
      </div>

      <div class="loading-skeleton" *ngIf="loading">
        <div class="skeleton-row" *ngFor="let i of [1,2,3]"></div>
      </div>

      <div class="table-card" *ngIf="!loading">
        <table class="data-table">
          <thead>
            <tr>
              <th>Référence</th>
              <th>Mission</th>
              <th>Dossier</th>
              <th>Montant</th>
              <th>Date soumission</th>
              <th>Statut</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngIf="factures.length === 0">
              <td colspan="6" class="empty-state">Aucune facture soumise.</td>
            </tr>
            <tr *ngFor="let f of factures" class="table-row">
              <td><strong class="mono">{{ f.factureRef || f.reference || '—' }}</strong></td>
              <td>
                <strong>{{ f.mission?.numeroMission || '—' }}</strong>
              </td>
              <td>
                <span *ngIf="f.mission?.prestation?.dossier as d">
                  {{ d.numeroDossier }}<br>
                  <small style="color:#888">{{ d.client?.nom }} {{ d.client?.prenom }}</small>
                </span>
                <span *ngIf="!f.mission?.prestation?.dossier">—</span>
              </td>
              <td>
                <strong class="montant">{{ f.montant | number:'1.2-2' }} TND</strong>
              </td>
              <td>{{ f.dateSoumission | date:'dd/MM/yyyy' }}</td>
              <td>
                <span class="statut-badge" [ngClass]="getStatutClass(f.statut)">
                  {{ f.statut || '—' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .page-container { font-family: 'Inter', sans-serif; padding: 28px; }
    .page-header { margin-bottom: 25px; }
    h1 { margin: 0; font-size: 1.8rem; color: #1a237e; }
    .subtitle { margin: 5px 0 0; color: #777; }

    .loading-skeleton { display: flex; flex-direction: column; gap: 12px; }
    .skeleton-row { height: 55px; border-radius: 8px;
      background: linear-gradient(90deg,#f0f0f0 25%,#e0e0e0 50%,#f0f0f0 75%);
      background-size: 200% 100%; animation: shimmer 1.5s infinite; }
    @keyframes shimmer { 0%{background-position:200% 0} 100%{background-position:-200% 0} }

    .table-card { background: white; border-radius: 12px;
      box-shadow: 0 4px 15px rgba(0,0,0,0.05); overflow: hidden; }
    .data-table { width: 100%; border-collapse: collapse; }
    .data-table thead { background: #1a237e; }
    .data-table th { padding: 15px; text-align: left; color: white;
      font-weight: 600; font-size: .9rem; }
    .data-table td { padding: 14px 15px; border-bottom: 1px solid #f0f0f0;
      font-size: .9rem; vertical-align: middle; }
    .table-row:hover { background: #f8f9ff; }
    .empty-state { text-align: center; padding: 40px !important;
      color: #aaa; font-style: italic; }

    .mono { font-family: monospace; font-size: .88rem; }
    .montant { color: #2e7d32; font-size: 1rem; }

    .statut-badge { padding: 5px 12px; border-radius: 15px;
      font-size: .8rem; font-weight: bold; }
    .s-en-attente  { background: #fff3cd; color: #856404; }
    .s-approuvee   { background: #d4edda; color: #155724; }
    .s-rejetee     { background: #f8d7da; color: #721c24; }
    .s-payee       { background: #e2e3e5; color: #383d41; }
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
      EN_ATTENTE:  's-en-attente',
      APPROUVEE:   's-approuvee',
      REJETEE:     's-rejetee',
      PAYEE:       's-payee',
    };
    return map[statut] || '';
  }
}