import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AvocatService } from '../../../core/services/avocat.service';

@Component({
  selector: 'app-avocat-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <div class="title-group">
          <span class="role-badge">AVOCAT</span>
          <h1>Espace de Gestion Judiciaire</h1>
          <p class="subtitle">Pilotez vos affaires, planifiez vos audiences et soumettez vos honoraires</p>
        </div>
      </div>
      
      <!-- Stats Dashboard -->
      <div class="stats-row">
        <div class="stat-card indigo">
          <div class="stat-icon">📁</div>
          <div class="stat-info">
            <h4>Total Affaires</h4>
            <span class="stat-value">{{ stats?.totalAffaires || 0 }}</span>
          </div>
        </div>
        <div class="stat-card blue">
          <div class="stat-icon">⚖️</div>
          <div class="stat-info">
            <h4>Audiences à venir</h4>
            <span class="stat-value">{{ stats?.audiencesAVenir || 0 }}</span>
          </div>
        </div>
        <div class="stat-card green">
          <div class="stat-icon">💎</div>
          <div class="stat-info">
            <h4>Honoraires</h4>
            <span class="stat-value">{{ stats?.totalHonoraires | number:'1.2-2' }} TND</span>
          </div>
        </div>
        <div class="stat-card orange">
          <div class="stat-icon">🔔</div>
          <div class="stat-info">
            <h4>En cours</h4>
            <span class="stat-value">{{ stats?.affairesEnCours || 0 }}</span>
          </div>
        </div>
      </div>

      <div class="main-content">
        <div class="content-card">
          <div class="card-header">
            <h2>Mes Affaires Judiciaires</h2>
            <div class="search-box">
              <input type="text" [(ngModel)]="search" placeholder="🔍 Rechercher une affaire..." (ngModelChange)="filtrer()">
            </div>
          </div>
          
          <div class="table-wrapper">
            <table class="premium-table">
              <thead>
                <tr>
                  <th>N° Affaire / Dossier</th>
                  <th>Client & Adversaire</th>
                  <th>Tribunal / Chambre</th>
                  <th>N° Rôle</th>
                  <th>Statut Affaire</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngIf="loading"><td colspan="6" class="loading-state">Récupération des données...</td></tr>
                <tr *ngIf="!loading && affairesFiltrees.length === 0"><td colspan="6" class="empty-state">Aucune affaire trouvée.</td></tr>
                <tr *ngFor="let aff of affairesFiltrees" class="data-row">
                  <td>
                    <div class="aff-ref">
                      <span class="aff-id">#{{ aff.id }}</span>
                      <strong>{{ aff.dossier?.numeroDossier || 'N/A' }}</strong>
                    </div>
                  </td>
                  <td>
                    <div class="people-info">
                      <strong>{{ aff.dossier?.client?.nom || aff.dossier?.client?.raisonSociale || 'Client Inconnu' }}</strong>
                      <span class="vs">vs</span>
                      <strong>{{ aff.adversaire || 'N/A' }}</strong>
                    </div>
                  </td>
                  <td>
                    <div class="court-info">
                      <span>{{ aff.tribunal || 'Non défini' }}</span>
                      <small>{{ aff.chambre || '-' }}</small>
                    </div>
                  </td>
                  <td><code>{{ aff.numeroRole || '-' }}</code></td>
                  <td>
                    <span class="status-pill" [class]="aff.statut?.toLowerCase()">{{ aff.statut }}</span>
                  </td>
                  <td>
                    <button class="btn-manage" (click)="ouvrirGestion(aff)">Gérer l\\'Affaire</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- MODAL GESTION AFFAIRE -->
    <div class="modal-overlay" *ngIf="selectedAffaire" (click)="fermerModal()">
      <div class="modal-card wide" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <div class="modal-title">
            <h3>Affaire #{{ selectedAffaire.id }} - {{ selectedAffaire.dossier?.numeroDossier }}</h3>
            <span class="status-pill" [class]="selectedAffaire.statut?.toLowerCase()">{{ selectedAffaire.statut }}</span>
          </div>
          <button class="close-btn" (click)="fermerModal()">✕</button>
        </div>

        <div class="modal-tabs">
          <button [class.active]="modalTab === 'audiences'" (click)="modalTab = 'audiences'">🗓️ Audiences</button>
          <button [class.active]="modalTab === 'jugement'" (click)="modalTab = 'jugement'">⚖️ Jugement</button>
          <button [class.active]="modalTab === 'honoraires'" (click)="modalTab = 'honoraires'">💰 Honoraires</button>
          <button [class.active]="modalTab === 'tribunal'" (click)="modalTab = 'tribunal'">🏛️ Tribunal</button>
        </div>

        <div class="modal-body scrollable">
          
          <!-- TAB AUDIENCES -->
          <div *ngIf="modalTab === 'audiences'">
            <div class="section-header">
              <h4>Chronologie des Audiences</h4>
              <button class="btn-small-add" (click)="showAddAudience = !showAddAudience">+ Ajouter</button>
            </div>

            <!-- Formulaire ajout audience -->
            <div class="mini-form" *ngIf="showAddAudience">
              <div class="form-grid">
                <div class="field"><label>Date</label><input type="date" [(ngModel)]="newAudience.dateAudience"></div>
                <div class="field"><label>Heure</label><input type="time" [(ngModel)]="newAudience.heure"></div>
                <div class="field"><label>Salle</label><input type="text" [(ngModel)]="newAudience.salle"></div>
                <div class="field"><label>Statut</label>
                  <select [(ngModel)]="newAudience.statut">
                    <option value="A_VENIR">À Venir</option>
                    <option value="EN_COURS">En Cours</option>
                    <option value="REALISEE">Réalisée</option>
                    <option value="REPORTEE">Reportée</option>
                  </select>
                </div>
              </div>
              <div class="field"><label>Motif / Objet</label><textarea [(ngModel)]="newAudience.motif"></textarea></div>
              <div class="form-actions">
                <button class="btn-primary" (click)="ajouterAudience()">Enregistrer l\\'audience</button>
              </div>
            </div>

            <div class="timeline">
              <div *ngFor="let aud of selectedAffaire.audiences" class="timeline-item">
                <div class="time-meta">
                  <span class="date">{{ aud.dateAudience | date:'dd MMM yyyy' }}</span>
                  <span class="hour">{{ aud.heure }}</span>
                </div>
                <div class="time-content">
                  <div class="content-top">
                    <strong>{{ aud.motif }}</strong>
                    <span class="aud-status" [class]="aud.statut?.toLowerCase()">{{ aud.statut }}</span>
                  </div>
                  <p *ngIf="aud.resultat"><strong>Résultat:</strong> {{ aud.resultat }}</p>
                  <div class="actions">
                    <button (click)="supprimerAudience(aud.id)" class="text-danger">Supprimer</button>
                  </div>
                </div>
              </div>
              <div *ngIf="!selectedAffaire.audiences?.length" class="empty-mini">Aucune audience planifiée.</div>
            </div>
          </div>

          <!-- TAB JUGEMENT -->
          <div *ngIf="modalTab === 'jugement'">
            <div class="section-header"><h4>Saisie du Jugement</h4></div>
            <div class="standard-form">
              <div class="form-grid">
                <div class="field"><label>Type de Jugement</label>
                  <select [(ngModel)]="jugement.typeJugement">
                    <option value="FINAL">Final</option>
                    <option value="INTERLOCUTOIRE">Interlocutoire</option>
                    <option value="PREPARATOIRE">Préparatoire</option>
                  </select>
                </div>
                <div class="field"><label>Date du Jugement</label><input type="date" [(ngModel)]="jugement.dateJugement"></div>
                <div class="field"><label>Montant Jugé</label><input type="text" [(ngModel)]="jugement.montantJuge"></div>
              </div>
              <div class="field"><label>Description / Dispositif</label><textarea rows="5" [(ngModel)]="jugement.descriptionJugement"></textarea></div>
              <button class="btn-primary full" (click)="enregistrerJugement()">Enregistrer le Jugement Officiel</button>
            </div>
          </div>

          <!-- TAB HONORAIRES -->
          <div *ngIf="modalTab === 'honoraires'">
            <div class="section-header"><h4>Soumission des Prestations</h4></div>
            <div class="info-banner" *ngIf="selectedAffaire.mission">
              Statut Mission: <strong>{{ selectedAffaire.mission.statut }}</strong>
            </div>
            
            <div class="honoraires-form">
              <h5>1. Rapport de Mission (PV)</h5>
              <textarea placeholder="Détaillez vos diligences..." rows="4" [(ngModel)]="pvTexte"></textarea>
              <button class="btn-secondary" (click)="soumettrePV()" [disabled]="!selectedAffaire.mission">Soumettre le PV</button>

              <hr>

              <h5>2. Facture d\\'Honoraires</h5>
              <div class="form-grid">
                <div class="field"><label>Référence Facture</label><input type="text" [(ngModel)]="facture.ref"></div>
                <div class="field"><label>Montant (TND)</label><input type="number" [(ngModel)]="facture.montant"></div>
              </div>
              <button class="btn-primary" (click)="soumettreFacture()" [disabled]="!selectedAffaire.mission">Soumettre la Facture</button>
            </div>
          </div>

          <!-- TAB TRIBUNAL -->
          <div *ngIf="modalTab === 'tribunal'">
            <div class="section-header"><h4>Informations Tribunal</h4></div>
            <div class="standard-form">
              <div class="field"><label>Tribunal</label><input type="text" [(ngModel)]="tribunal.nom"></div>
              <div class="form-grid">
                <div class="field"><label>Chambre</label><input type="text" [(ngModel)]="tribunal.chambre"></div>
                <div class="field"><label>Numéro de Rôle</label><input type="text" [(ngModel)]="tribunal.role"></div>
              </div>
              <button class="btn-primary" (click)="modifierTribunal()">Mettre à jour</button>
            </div>
          </div>

        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-container { padding: 30px; background: #f0f2f5; min-height: 100vh; font-family: 'Inter', sans-serif; }
    .page-header { margin-bottom: 35px; }
    .role-badge { background: #e0e7ff; color: #4338ca; padding: 4px 10px; border-radius: 6px; font-weight: 800; font-size: 0.7rem; letter-spacing: 1px; display: inline-block; margin-bottom: 8px; }
    h1 { margin: 0; font-size: 2.2rem; color: #1e293b; font-weight: 800; }
    .subtitle { color: #64748b; margin-top: 6px; font-size: 1.1rem; }

    .stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 35px; }
    .stat-card { background: white; border-radius: 20px; padding: 25px; display: flex; align-items: center; gap: 20px; box-shadow: 0 4px 12px rgba(0,0,0,0.03); border: 1px solid #e2e8f0; }
    .stat-icon { width: 60px; height: 60px; border-radius: 18px; display: flex; align-items: center; justify-content: center; font-size: 2rem; }
    .indigo .stat-icon { background: #e0e7ff; color: #4338ca; }
    .blue .stat-icon { background: #e0f2fe; color: #0284c7; }
    .green .stat-icon { background: #dcfce7; color: #16a34a; }
    .orange .stat-icon { background: #ffedd5; color: #ea580c; }
    .stat-info h4 { margin: 0; color: #64748b; font-size: 0.85rem; font-weight: 600; text-transform: uppercase; }
    .stat-value { font-size: 1.8rem; font-weight: 800; color: #1e293b; }

    .content-card { background: white; border-radius: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); border: 1px solid #e2e8f0; overflow: hidden; }
    .card-header { padding: 25px 30px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; }
    .card-header h2 { margin: 0; font-size: 1.3rem; color: #1e293b; font-weight: 700; }
    .search-box input { padding: 10px 20px; border: 1px solid #e2e8f0; border-radius: 10px; width: 300px; outline: none; }
    .search-box input:focus { border-color: #4338ca; box-shadow: 0 0 0 3px rgba(67, 56, 202, 0.1); }

    .premium-table { width: 100%; border-collapse: collapse; }
    .premium-table th { text-align: left; padding: 18px 30px; background: #f8fafc; color: #64748b; font-weight: 700; font-size: 0.8rem; text-transform: uppercase; border-bottom: 1px solid #f1f5f9; }
    .premium-table td { padding: 18px 30px; border-bottom: 1px solid #f1f5f9; color: #475569; }
    .data-row:hover { background: #f8fafc; }
    .aff-ref { display: flex; flex-direction: column; }
    .aff-id { font-size: 0.75rem; color: #94a3b8; font-weight: 600; }
    .people-info { display: flex; flex-direction: column; }
    .vs { font-size: 0.7rem; color: #94a3b8; font-weight: 800; font-style: italic; margin: 2px 0; }
    .court-info { display: flex; flex-direction: column; }
    .court-info small { color: #94a3b8; font-size: 0.75rem; }
    code { background: #f1f5f9; padding: 4px 8px; border-radius: 6px; font-family: 'JetBrains Mono', monospace; font-size: 0.85rem; }

    .status-pill { padding: 6px 12px; border-radius: 8px; font-size: 0.75rem; font-weight: 800; text-transform: uppercase; background: #f1f5f9; color: #64748b; }
    .status-pill.en_cours { background: #e0f2fe; color: #0369a1; }
    .status-pill.jugement_rendu { background: #dcfce7; color: #15803d; }
    .status-pill.terminee { background: #f1f5f9; color: #1e293b; }

    .btn-manage { background: #1e293b; color: white; border: none; padding: 10px 18px; border-radius: 10px; font-weight: 700; font-size: 0.85rem; cursor: pointer; transition: all 0.2s; }
    .btn-manage:hover { background: #334155; transform: scale(1.05); }

    /* Modal Styling */
    .modal-overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.7); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 1000; }
    .modal-card.wide { width: 900px; max-width: 95vw; background: white; border-radius: 24px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); display: flex; flex-direction: column; max-height: 90vh; }
    .modal-header { padding: 25px 35px; border-bottom: 1px solid #f1f5f9; display: flex; justify-content: space-between; align-items: center; }
    .modal-title h3 { margin: 0; font-size: 1.4rem; color: #1e293b; }
    .close-btn { background: none; border: none; font-size: 1.5rem; color: #94a3b8; cursor: pointer; }

    .modal-tabs { display: flex; gap: 10px; padding: 15px 35px; background: #f8fafc; border-bottom: 1px solid #f1f5f9; }
    .modal-tabs button { padding: 10px 20px; border: none; background: transparent; border-radius: 10px; font-weight: 700; color: #64748b; cursor: pointer; transition: all 0.2s; }
    .modal-tabs button.active { background: white; color: #4338ca; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); }

    .modal-body.scrollable { padding: 35px; overflow-y: auto; flex: 1; }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; }
    .section-header h4 { margin: 0; font-size: 1.1rem; color: #1e293b; }
    .btn-small-add { background: #4338ca; color: white; border: none; padding: 6px 14px; border-radius: 8px; font-weight: 700; font-size: 0.8rem; cursor: pointer; }

    .mini-form { background: #f8fafc; padding: 20px; border-radius: 16px; margin-bottom: 25px; border: 1px solid #e2e8f0; }
    .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px; }
    .field { display: flex; flex-direction: column; gap: 6px; margin-bottom: 15px; }
    .field label { font-size: 0.8rem; font-weight: 700; color: #64748b; }
    .field input, .field select, .field textarea { padding: 10px 14px; border: 1px solid #e2e8f0; border-radius: 8px; outline: none; font-family: inherit; }
    .field input:focus { border-color: #4338ca; }
    .btn-primary { background: #4338ca; color: white; border: none; padding: 12px 24px; border-radius: 10px; font-weight: 700; cursor: pointer; }
    .btn-primary.full { width: 100%; margin-top: 10px; }

    .timeline { position: relative; padding-left: 30px; border-left: 2px solid #e2e8f0; }
    .timeline-item { margin-bottom: 30px; position: relative; }
    .timeline-item::before { content: ''; position: absolute; left: -37px; top: 0; width: 12px; height: 12px; border-radius: 50%; background: #4338ca; border: 4px solid white; box-shadow: 0 0 0 2px #4338ca; }
    .time-meta { display: flex; flex-direction: column; margin-bottom: 8px; }
    .time-meta .date { font-weight: 800; color: #1e293b; }
    .time-meta .hour { font-size: 0.8rem; color: #64748b; }
    .time-content { background: #f8fafc; padding: 15px 20px; border-radius: 12px; }
    .content-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .aud-status { font-size: 0.65rem; font-weight: 800; padding: 3px 8px; border-radius: 6px; text-transform: uppercase; }
    .aud-status.a_venir { background: #e0f2fe; color: #0369a1; }
    .aud-status.realisee { background: #dcfce7; color: #15803d; }

    .info-banner { background: #eff6ff; color: #1e40af; padding: 12px 20px; border-radius: 10px; margin-bottom: 20px; font-size: 0.9rem; }
    .honoraires-form h5 { margin: 20px 0 10px; color: #1e293b; }
    .btn-secondary { background: #f1f5f9; color: #1e293b; border: 1px solid #e2e8f0; padding: 10px 20px; border-radius: 10px; font-weight: 700; cursor: pointer; }

    .text-danger { color: #dc2626; background: none; border: none; cursor: pointer; font-size: 0.8rem; font-weight: 600; padding: 0; }
  `]
})
export class AvocatDashboardComponent implements OnInit {
  stats: any = null;
  affaires: any[] = [];
  affairesFiltrees: any[] = [];
  loading = true;
  search = '';

  selectedAffaire: any = null;
  modalTab: 'audiences' | 'jugement' | 'honoraires' | 'tribunal' = 'audiences';
  showAddAudience = false;

  newAudience = { dateAudience: '', heure: '', salle: '', motif: '', statut: 'A_VENIR' };
  jugement = { typeJugement: 'FINAL', dateJugement: '', montantJuge: '', descriptionJugement: '' };
  tribunal = { nom: '', chambre: '', role: '' };
  pvTexte = '';
  facture = { ref: '', montant: 0 };

  constructor(private avocatService: AvocatService) {}

  ngOnInit() {
    this.chargerStats();
    this.chargerAffaires();
  }

  chargerStats() {
    this.avocatService.getDashboard().subscribe(s => this.stats = s);
  }

  chargerAffaires() {
    this.loading = true;
    this.avocatService.getAffaires().subscribe({
      next: (data) => {
        this.affaires = data.affaires || [];
        this.affairesFiltrees = [...this.affaires];
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  filtrer() {
    const q = this.search.toLowerCase();
    this.affairesFiltrees = this.affaires.filter(a => 
      a.dossier?.numeroDossier?.toLowerCase().includes(q) ||
      a.adversaire?.toLowerCase().includes(q)
    );
  }

  ouvrirGestion(aff: any) {
    this.selectedAffaire = aff;
    this.modalTab = 'audiences';
    this.showAddAudience = false;
    
    // Reset forms
    this.newAudience = { dateAudience: '', heure: '', salle: '', motif: '', statut: 'A_VENIR' };
    this.jugement = { 
      typeJugement: aff.typeJugement || 'FINAL', 
      dateJugement: aff.dateJugement || '', 
      montantJuge: aff.montantJuge || '', 
      descriptionJugement: aff.descriptionJugement || '' 
    };
    this.tribunal = { nom: aff.tribunal || '', chambre: aff.chambre || '', role: aff.numeroRole || '' };
    this.pvTexte = aff.mission?.pvTexte || '';
    this.facture = { ref: aff.mission?.factureRef || '', montant: aff.mission?.montantFacture || 0 };
  }

  fermerModal() {
    this.selectedAffaire = null;
    this.chargerStats();
    this.chargerAffaires();
  }

  ajouterAudience() {
    this.avocatService.ajouterAudience(this.selectedAffaire.id, this.newAudience).subscribe({
      next: () => {
        alert('Audience ajoutée !');
        this.avocatService.getAffaireDetail(this.selectedAffaire.id).subscribe(aff => this.selectedAffaire = aff);
        this.showAddAudience = false;
      },
      error: (e) => alert(e.error?.error || 'Erreur')
    });
  }

  supprimerAudience(id: number) {
    if (confirm('Supprimer cette audience ?')) {
      this.avocatService.supprimerAudience(this.selectedAffaire.id, id).subscribe(() => {
        this.avocatService.getAffaireDetail(this.selectedAffaire.id).subscribe(aff => this.selectedAffaire = aff);
      });
    }
  }

  enregistrerJugement() {
    this.avocatService.enregistrerJugement(this.selectedAffaire.id, this.jugement).subscribe({
      next: () => alert('Jugement enregistré !'),
      error: (e) => alert(e.error?.error || 'Erreur')
    });
  }

  modifierTribunal() {
    const body = { tribunal: this.tribunal.nom, chambre: this.tribunal.chambre, numeroRole: this.tribunal.role };
    this.avocatService.modifierTribunal(this.selectedAffaire.id, body).subscribe({
      next: () => alert('Tribunal mis à jour !'),
      error: (e) => alert(e.error?.error || 'Erreur')
    });
  }

  soumettrePV() {
    this.avocatService.soumettrePV(this.selectedAffaire.id, this.selectedAffaire.mission.id, this.pvTexte).subscribe({
      next: () => alert('PV soumis avec succès !'),
      error: (e) => alert(e.error?.error || 'Erreur')
    });
  }

  soumettreFacture() {
    const body = { factureRef: this.facture.ref, montantFacture: this.facture.montant };
    this.avocatService.soumettreFacture(this.selectedAffaire.id, this.selectedAffaire.mission.id, body).subscribe({
      next: () => alert('Facture soumise !'),
      error: (e) => alert(e.error?.error || 'Erreur')
    });
  }
}
