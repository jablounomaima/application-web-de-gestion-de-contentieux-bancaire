import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DossierService } from '../../../core/services/dossier.service';

@Component({
  selector: 'app-agent-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dashboard-container">
      <h1>Tableau de Bord - Agent Bancaire</h1>
      
      <div *ngIf="loading" class="loading">Chargement des dossiers...</div>
      
      <div *ngIf="!loading && dossiers.length === 0" class="empty">
        Aucun dossier pour le moment.
      </div>

      <div class="cards" *ngIf="!loading && dossiers.length > 0">
        <div class="card" *ngFor="let dossier of dossiers">
          <h3>Dossier N° {{ dossier.numeroDossier }}</h3>
          <p><strong>Client:</strong> {{ dossier.client?.nom || dossier.client?.raisonSociale }}</p>
          <p><strong>Type:</strong> {{ dossier.typeDossier }}</p>
          <p><strong>Statut:</strong> {{ dossier.statut }}</p>
          <button (click)="voirDetails(dossier.id)">Voir détails</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container { padding: 20px; font-family: 'Inter', sans-serif; }
    h1 { color: #333; margin-bottom: 20px; }
    .cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }
    .card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); transition: transform 0.2s; }
    .card:hover { transform: translateY(-5px); }
    button { background: #0056b3; color: white; border: none; padding: 10px 15px; border-radius: 5px; cursor: pointer; margin-top: 10px; }
    button:hover { background: #004494; }
    .loading, .empty { color: #666; font-size: 1.1em; }
  `]
})
export class AgentDashboardComponent implements OnInit {
  dossiers: any[] = [];
  loading = true;

  constructor(private dossierService: DossierService) {}

  ngOnInit() {
    this.chargerDossiers();
  }

  chargerDossiers() {
    this.dossierService.getAllDossiers().subscribe({
      next: (data) => {
        this.dossiers = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement des dossiers', err);
        this.loading = false;
      }
    });
  }

  voirDetails(id: number) {
    console.log('Voir détails pour dossier: ', id);
    // Navigation future vers la page de détails
  }
}
