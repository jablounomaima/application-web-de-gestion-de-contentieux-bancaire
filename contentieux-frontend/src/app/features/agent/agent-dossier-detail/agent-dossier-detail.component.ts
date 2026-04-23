import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DossierService } from '../../../core/services/dossier.service';

@Component({
  selector: 'app-agent-dossier-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <!-- ══════════════════ LOADING ══════════════════ -->
    <div class="loading-screen" *ngIf="loading">
      <div class="spinner"></div>
      <p>Chargement du dossier…</p>
    </div>

    <!-- ══════════════════ ERROR ══════════════════ -->
    <div class="error-screen" *ngIf="!loading && !dossier">
      <div class="error-icon">⚠️</div>
      <h2>Dossier introuvable</h2>
      <a routerLink="/agent/dossiers" class="btn-back">← Retour aux dossiers</a>
    </div>

    <!-- ══════════════════ MAIN ══════════════════ -->
    <div class="page" *ngIf="!loading && dossier">

      <!-- Breadcrumb -->
      <nav class="breadcrumb">
        <a routerLink="/agent/dossiers">Mes Dossiers</a>
        <span>/</span>
        <span>{{ dossier.numeroDossier }}</span>
      </nav>

      <!-- ── Header ── -->
      <header class="dossier-header">
        <div class="header-left">
          <div class="numero-badge">{{ dossier.numeroDossier }}</div>
          <h1>{{ dossier.libelle || 'Dossier sans titre' }}</h1>
          <p class="client-line">
            <span class="client-tag">CLIENT</span>
            {{ getClientName() }}
          </p>
        </div>
        <div class="header-right">
          <span class="statut-chip" [ngClass]="getStatutClass(dossier.statut)">
            {{ formatStatut(dossier.statut) }}
          </span>
          <button class="btn-icon" (click)="telechargerPdf()" title="Télécharger PDF">
            <span>📄</span> PDF
          </button>
          <button class="btn-primary"
                  *ngIf="canSoumettre()"
                  (click)="ouvrirModalValidateurs()">
            <span>🚀</span> Soumettre
          </button>
        </div>
      </header>

      <!-- ── Métriques ── -->
      <div class="metrics-row">
        <div class="metric">
          <label>Type Client</label>
          <strong>{{ dossier.client?.typeClient || '—' }}</strong>
        </div>
        <div class="metric">
          <label>Date Création</label>
          <strong>{{ dossier.dateCreation | date:'dd/MM/yyyy' }}</strong>
        </div>
        <div class="metric">
          <label>Agence</label>
          <strong>{{ dossier.agence?.nom || '—' }}</strong>
        </div>
        <div class="metric">
          <label>Agent</label>
          <strong>{{ dossier.creePar || '—' }}</strong>
        </div>
        <div class="metric" *ngIf="dossier.validateurFinancierChoisi">
          <label>Valid. Financier</label>
          <strong>{{ dossier.validateurFinancierChoisi }}</strong>
        </div>
        <div class="metric" *ngIf="dossier.validateurJuridiqueChoisi">
          <label>Valid. Juridique</label>
          <strong>{{ dossier.validateurJuridiqueChoisi }}</strong>
        </div>
      </div>

      <!-- ── Alertes validation ── -->
      <div class="alert alert-success" *ngIf="dossier.validationFinanciere === true && dossier.validationJuridique === true">
        ✅ Dossier entièrement validé (financier + juridique)
      </div>
      <div class="alert alert-warning" *ngIf="dossier.statut === 'EN_TRAITEMENT'">
        ⏳ En attente de validation — financier :
        <strong>{{ dossier.validationFinanciere == null ? 'En attente' : (dossier.validationFinanciere ? '✅' : '❌') }}</strong>
        &nbsp;| juridique :
        <strong>{{ dossier.validationJuridique == null ? 'En attente' : (dossier.validationJuridique ? '✅' : '❌') }}</strong>
      </div>
      <div class="alert alert-error" *ngIf="dossier.statut === 'REJETE'">
        ❌ Dossier rejeté
        <span *ngIf="dossier.commentaireFinancier"> — Financier : {{ dossier.commentaireFinancier }}</span>
        <span *ngIf="dossier.commentaireJuridique"> — Juridique : {{ dossier.commentaireJuridique }}</span>
      </div>

      <!-- ── Layout principal ── -->
      <div class="content-grid">

        <!-- COL GAUCHE : Risques & Client -->
        <div class="col-main">

          <!-- Bloc Client -->
          <div class="card">
            <div class="card-header">
              <h2>👤 Informations Client</h2>
            </div>
            <div class="card-body client-grid" *ngIf="dossier.client">
              <div class="info-item">
                <label>Nom complet</label>
                <span>{{ dossier.client.nom }} {{ dossier.client.prenom }}</span>
              </div>
              <div class="info-item" *ngIf="dossier.client.cin">
                <label>CIN</label>
                <span>{{ dossier.client.cin }}</span>
              </div>
              <div class="info-item" *ngIf="dossier.client.rne">
                <label>RNE</label>
                <span>{{ dossier.client.rne }}</span>
              </div>
              <div class="info-item" *ngIf="dossier.client.raisonSociale">
                <label>Raison Sociale</label>
                <span>{{ dossier.client.raisonSociale }}</span>
              </div>
              <div class="info-item" *ngIf="dossier.client.email">
                <label>Email</label>
                <span>{{ dossier.client.email }}</span>
              </div>
              <div class="info-item" *ngIf="dossier.client.telephone">
                <label>Téléphone</label>
                <span>{{ dossier.client.telephone }}</span>
              </div>
              <div class="info-item" *ngIf="dossier.client.adresse">
                <label>Adresse</label>
                <span>{{ dossier.client.adresse }}</span>
              </div>
            </div>
          </div>

          <!-- Bloc Risques (Crédits) -->
          <div class="card">
            <div class="card-header">
              <h2>💳 Crédits / Risques</h2>
              <button class="btn-add"
                      *ngIf="canModifier()"
                      (click)="showModalRisque = true">
                + Ajouter
              </button>
            </div>
            <div class="card-body">
              <div class="empty-state" *ngIf="!dossier.risques || dossier.risques.length === 0">
                <p>Aucun crédit enregistré.</p>
              </div>

              <div class="risque-card" *ngFor="let r of dossier.risques">
                <div class="risque-header">
                  <div class="risque-title">
                    <input type="checkbox"
                           [checked]="r.selectionne"
                           [disabled]="!canModifier()"
                           (change)="toggleRisque(r)"
                           class="risque-check">
                    <strong>{{ r.type }}</strong>
                    <span class="risque-selected" *ngIf="r.selectionne">✓ Sélectionné</span>
                  </div>
                  <div class="risque-montants">
                    <span class="montant-initial">{{ r.montantInitial | number:'1.0-0' }} TND</span>
                    <span class="montant-impaye danger">Impayé : {{ r.montantImpaye | number:'1.0-0' }} TND</span>
                  </div>
                </div>
                <div class="risque-meta">
                  <span *ngIf="r.dateEcheance">📅 Échéance : {{ r.dateEcheance | date:'dd/MM/yyyy' }}</span>
                  <span *ngIf="r.description">📝 {{ r.description }}</span>
                </div>

                <!-- Garanties -->
                <div class="garanties-list" *ngIf="r.garanties && r.garanties.length > 0">
                  <div class="garantie-pill" *ngFor="let g of r.garanties">
                    <span class="g-type">{{ g.typeGarantie }}</span>
                    <span class="g-valeur" *ngIf="g.valeurEstimee">{{ g.valeurEstimee | number:'1.0-0' }} TND</span>
                    <span class="g-desc" *ngIf="g.description">— {{ g.description }}</span>
                    <button class="btn-tiny danger"
                            *ngIf="canModifier()"
                            (click)="supprimerGarantie(g.id)">✕</button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Notes & Description -->
          <div class="card" *ngIf="dossier.description || dossier.notes">
            <div class="card-header"><h2>📝 Notes & Description</h2></div>
            <div class="card-body">
              <div class="info-item" *ngIf="dossier.description">
                <label>Description</label>
                <p>{{ dossier.description }}</p>
              </div>
              <div class="info-item" *ngIf="dossier.notes">
                <label>Notes internes</label>
                <p>{{ dossier.notes }}</p>
              </div>
            </div>
          </div>

        </div>

        <!-- COL DROITE : Historique & Actions -->
        <div class="col-side">

          <!-- Actions rapides -->
          <div class="card actions-card" *ngIf="canModifier()">
            <div class="card-header"><h3>⚡ Actions</h3></div>
            <div class="card-body">
              <button class="btn-action" (click)="ouvrirModalValidateurs()"
                      *ngIf="canSoumettre()">
                🚀 Soumettre à validation
              </button>
              <button class="btn-action secondary" (click)="ouvrirModificationDossier()"
                      *ngIf="canModifier()">
                ✏️ Modifier le dossier
              </button>
              <button class="btn-action danger" (click)="supprimerDossier()"
                      *ngIf="dossier.statut === 'OUVERT'">
                🗑️ Supprimer
              </button>
            </div>
          </div>

          <!-- Historique -->
          <div class="card">
            <div class="card-header"><h3>🕒 Historique</h3></div>
            <div class="card-body">
              <div class="empty-state" *ngIf="!historique || historique.length === 0">
                <p>Aucun historique.</p>
              </div>
              <div class="histo-item" *ngFor="let h of historique">
                <div class="histo-dot"></div>
                <div class="histo-body">
                  <strong>{{ h.action }}</strong>
                  <p *ngIf="h.details">{{ h.details }}</p>
                  <small>{{ h.dateAction | date:'dd/MM/yyyy HH:mm' }} — {{ h.utilisateur }}</small>
                </div>
              </div>
            </div>
          </div>

          <!-- Mission Avocat -->
          <div class="card" *ngIf="missionAvocat">
            <div class="card-header"><h3>⚖️ Mission Avocat</h3></div>
            <div class="card-body">
              <div class="info-item">
                <label>Avocat</label>
                <span>{{ missionAvocat.avocat?.nom }} {{ missionAvocat.avocat?.prenom }}</span>
              </div>
              <div class="info-item">
                <label>Statut</label>
                <span>{{ missionAvocat.statut }}</span>
              </div>
              <div class="info-item" *ngIf="missionAvocat.dateDebut">
                <label>Début</label>
                <span>{{ missionAvocat.dateDebut | date:'dd/MM/yyyy' }}</span>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>

    <!-- ══════════════════ MODAL : Choisir Validateurs ══════════════════ -->
    <div class="modal-overlay" *ngIf="showModalValidateurs" (click)="showModalValidateurs = false">
      <div class="modal-box" (click)="$event.stopPropagation()">
        <div class="modal-head">
          <h2>🔐 Choisir les Validateurs</h2>
          <button class="modal-close" (click)="showModalValidateurs = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>Validateur Financier</label>
            <select [(ngModel)]="formValidateurs.validateurFinancier" name="vf">
              <option value="">-- Choisir --</option>
              <option *ngFor="let v of validateurs_financiers" [value]="v.username">
                {{ v.nom }} {{ v.prenom }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>Validateur Juridique</label>
            <select [(ngModel)]="formValidateurs.validateurJuridique" name="vj">
              <option value="">-- Choisir --</option>
              <option *ngFor="let v of validateurs_juridiques" [value]="v.username">
                {{ v.nom }} {{ v.prenom }}
              </option>
            </select>
          </div>
          <div class="error-msg" *ngIf="erreurValidateurs">{{ erreurValidateurs }}</div>
        </div>
        <div class="modal-foot">
          <button class="btn-cancel" (click)="showModalValidateurs = false">Annuler</button>
          <button class="btn-submit" (click)="choisirEtSoumettre()">Enregistrer & Soumettre</button>
        </div>
      </div>
    </div>

    <!-- ══════════════════ MODAL : Ajouter Risque ══════════════════ -->
    <div class="modal-overlay" *ngIf="showModalRisque" (click)="showModalRisque = false">
      <div class="modal-box" (click)="$event.stopPropagation()">
        <div class="modal-head">
          <h2>+ Ajouter un Crédit</h2>
          <button class="modal-close" (click)="showModalRisque = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>Type de crédit</label>
            <select [(ngModel)]="formRisque.type" name="type">
              <option value="CREDIT_IMMOBILIER">Crédit Immobilier</option>
              <option value="CREDIT_CONSOMMATION">Crédit Consommation</option>
              <option value="CREDIT_AUTO">Crédit Auto</option>
              <option value="CREDIT_PROFESSIONNEL">Crédit Professionnel</option>
              <option value="LEASING">Leasing</option>
            </select>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>Montant Initial (TND)</label>
              <input type="number" [(ngModel)]="formRisque.montantInitial" name="mi" min="0">
            </div>
            <div class="form-group">
              <label>Montant Impayé (TND)</label>
              <input type="number" [(ngModel)]="formRisque.montantImpaye" name="mip" min="0">
            </div>
          </div>
          <div class="form-group">
            <label>Date Échéance</label>
            <input type="date" [(ngModel)]="formRisque.dateEcheance" name="de">
          </div>
          <div class="form-group">
            <label>Description (optionnel)</label>
            <textarea [(ngModel)]="formRisque.description" name="desc" rows="2"></textarea>
          </div>
          <div class="error-msg" *ngIf="erreurRisque">{{ erreurRisque }}</div>
        </div>
        <div class="modal-foot">
          <button class="btn-cancel" (click)="showModalRisque = false">Annuler</button>
          <button class="btn-submit" (click)="ajouterRisque()">Ajouter</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=DM+Mono:wght@400;500&display=swap');

    * { box-sizing: border-box; margin: 0; padding: 0; }

    .page {
      padding: 32px 40px;
      background: #f5f4f0;
      min-height: 100vh;
      font-family: 'DM Sans', sans-serif;
      color: #1a1a1a;
    }

    /* ── Loading / Error ── */
    .loading-screen, .error-screen {
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      min-height: 60vh; gap: 16px; font-family: 'DM Sans', sans-serif;
    }
    .spinner {
      width: 40px; height: 40px; border: 3px solid #e2e2e2;
      border-top-color: #1a1a1a; border-radius: 50%; animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .error-icon { font-size: 3rem; }
    .btn-back {
      background: #1a1a1a; color: white; padding: 10px 20px;
      border-radius: 8px; text-decoration: none; font-weight: 600;
    }

    /* ── Breadcrumb ── */
    .breadcrumb {
      display: flex; align-items: center; gap: 8px;
      font-size: 0.85rem; color: #888; margin-bottom: 24px;
    }
    .breadcrumb a { color: #555; text-decoration: none; font-weight: 500; }
    .breadcrumb a:hover { color: #1a1a1a; }
    .breadcrumb span { color: #ccc; }
    .breadcrumb span:last-child { color: #1a1a1a; font-weight: 600; }

    /* ── Header ── */
    .dossier-header {
      display: flex; justify-content: space-between; align-items: flex-start;
      margin-bottom: 28px; gap: 20px;
    }
    .numero-badge {
      font-family: 'DM Mono', monospace; font-size: 0.75rem; font-weight: 500;
      color: #888; letter-spacing: 0.5px; margin-bottom: 6px;
    }
    .dossier-header h1 {
      font-size: 1.8rem; font-weight: 700; color: #1a1a1a; line-height: 1.2; margin-bottom: 6px;
    }
    .client-line {
      display: flex; align-items: center; gap: 8px; color: #555; font-size: 0.95rem;
    }
    .client-tag {
      background: #e8e8e4; color: #666; font-size: 0.65rem; font-weight: 700;
      padding: 2px 7px; border-radius: 4px; letter-spacing: 0.5px;
    }
    .header-right { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }

    /* ── Statut Chips ── */
    .statut-chip {
      padding: 6px 14px; border-radius: 20px; font-size: 0.78rem;
      font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;
    }
    .statut-ouvert      { background: #e8f5e9; color: #2e7d32; }
    .statut-en-traitement { background: #fff8e1; color: #f57f17; }
    .statut-valide      { background: #e3f2fd; color: #1565c0; }
    .statut-rejete      { background: #fce4ec; color: #c62828; }
    .statut-clos        { background: #f3e5f5; color: #6a1b9a; }

    /* ── Buttons ── */
    .btn-icon {
      display: flex; align-items: center; gap: 6px;
      background: white; border: 1.5px solid #ddd; padding: 8px 16px;
      border-radius: 8px; font-weight: 600; font-size: 0.9rem; cursor: pointer;
      transition: all 0.15s; color: #1a1a1a; font-family: inherit;
    }
    .btn-icon:hover { background: #f5f4f0; border-color: #bbb; }
    .btn-primary {
      display: flex; align-items: center; gap: 6px;
      background: #1a1a1a; color: white; border: none;
      padding: 10px 20px; border-radius: 8px; font-weight: 700;
      font-size: 0.9rem; cursor: pointer; transition: all 0.15s; font-family: inherit;
    }
    .btn-primary:hover { background: #333; transform: translateY(-1px); }
    .btn-add {
      background: #f0f0ec; border: none; padding: 6px 14px;
      border-radius: 6px; font-weight: 600; font-size: 0.82rem;
      cursor: pointer; color: #1a1a1a; font-family: inherit; transition: all 0.15s;
    }
    .btn-add:hover { background: #e4e4e0; }

    /* ── Metrics ── */
    .metrics-row {
      display: flex; gap: 0; margin-bottom: 28px;
      background: white; border-radius: 12px;
      border: 1px solid #e8e8e4; overflow: hidden;
    }
    .metric {
      flex: 1; padding: 16px 20px; border-right: 1px solid #e8e8e4;
    }
    .metric:last-child { border-right: none; }
    .metric label {
      display: block; font-size: 0.7rem; text-transform: uppercase;
      letter-spacing: 0.5px; color: #999; margin-bottom: 4px; font-weight: 600;
    }
    .metric strong { font-size: 0.95rem; color: #1a1a1a; font-weight: 600; }

    /* ── Alerts ── */
    .alert {
      padding: 12px 20px; border-radius: 8px; margin-bottom: 20px;
      font-size: 0.9rem; font-weight: 500;
    }
    .alert-success { background: #e8f5e9; color: #2e7d32; border-left: 4px solid #4caf50; }
    .alert-warning  { background: #fff8e1; color: #e65100; border-left: 4px solid #ff9800; }
    .alert-error    { background: #fce4ec; color: #c62828; border-left: 4px solid #f44336; }

    /* ── Content Grid ── */
    .content-grid {
      display: grid; grid-template-columns: 1fr 320px; gap: 24px;
    }
    .col-main { display: flex; flex-direction: column; gap: 20px; }
    .col-side { display: flex; flex-direction: column; gap: 20px; }

    /* ── Card ── */
    .card {
      background: white; border-radius: 12px;
      border: 1px solid #e8e8e4; overflow: hidden;
    }
    .card-header {
      display: flex; justify-content: space-between; align-items: center;
      padding: 16px 20px; border-bottom: 1px solid #f0f0ec;
    }
    .card-header h2 { font-size: 1rem; font-weight: 700; color: #1a1a1a; }
    .card-header h3 { font-size: 0.95rem; font-weight: 700; color: #1a1a1a; }
    .card-body { padding: 20px; }

    /* ── Client Grid ── */
    .client-grid {
      display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px;
    }
    .info-item label {
      display: block; font-size: 0.72rem; text-transform: uppercase;
      letter-spacing: 0.5px; color: #999; margin-bottom: 4px; font-weight: 600;
    }
    .info-item span, .info-item p {
      font-size: 0.92rem; color: #1a1a1a; font-weight: 500;
    }

    /* ── Risques ── */
    .risque-card {
      border: 1px solid #e8e8e4; border-radius: 10px;
      padding: 16px; margin-bottom: 12px; transition: border-color 0.15s;
    }
    .risque-card:hover { border-color: #ccc; }
    .risque-header {
      display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px;
    }
    .risque-title {
      display: flex; align-items: center; gap: 10px;
    }
    .risque-check { width: 16px; height: 16px; cursor: pointer; }
    .risque-title strong { font-size: 0.95rem; color: #1a1a1a; }
    .risque-selected {
      background: #e8f5e9; color: #2e7d32; font-size: 0.72rem;
      font-weight: 700; padding: 2px 8px; border-radius: 10px;
    }
    .risque-montants { display: flex; flex-direction: column; align-items: flex-end; gap: 2px; }
    .montant-initial { font-size: 0.85rem; color: #555; font-weight: 600; }
    .montant-impaye { font-size: 0.85rem; font-weight: 700; }
    .danger { color: #c62828; }
    .risque-meta {
      display: flex; gap: 16px; font-size: 0.8rem; color: #888; margin-bottom: 10px;
    }

    /* ── Garanties ── */
    .garanties-list { display: flex; flex-wrap: wrap; gap: 8px; }
    .garantie-pill {
      display: flex; align-items: center; gap: 6px;
      background: #f5f4f0; border-radius: 6px; padding: 6px 10px;
      font-size: 0.8rem;
    }
    .g-type { font-weight: 700; color: #1a1a1a; }
    .g-valeur { color: #2e7d32; font-weight: 600; }
    .g-desc { color: #888; }
    .btn-tiny {
      border: none; background: none; cursor: pointer;
      font-size: 0.8rem; padding: 0 4px; line-height: 1;
      border-radius: 4px; font-family: inherit;
    }
    .btn-tiny.danger { color: #c62828; }
    .btn-tiny.danger:hover { background: #fce4ec; }

    /* ── Actions Card ── */
    .actions-card .card-body {
      display: flex; flex-direction: column; gap: 8px;
    }
    .btn-action {
      width: 100%; padding: 10px 16px; border-radius: 8px;
      font-weight: 600; font-size: 0.88rem; cursor: pointer;
      border: none; text-align: left; font-family: inherit; transition: all 0.15s;
      background: #1a1a1a; color: white;
    }
    .btn-action:hover { background: #333; }
    .btn-action.secondary { background: #f0f0ec; color: #1a1a1a; border: 1px solid #e0e0dc; }
    .btn-action.secondary:hover { background: #e4e4e0; }
    .btn-action.danger { background: #fce4ec; color: #c62828; }
    .btn-action.danger:hover { background: #f8bbd0; }

    /* ── Historique ── */
    .histo-item {
      display: flex; gap: 12px; padding: 10px 0;
      border-bottom: 1px solid #f0f0ec;
    }
    .histo-item:last-child { border-bottom: none; }
    .histo-dot {
      width: 8px; height: 8px; border-radius: 50%;
      background: #1a1a1a; flex-shrink: 0; margin-top: 6px;
    }
    .histo-body strong { font-size: 0.88rem; color: #1a1a1a; font-weight: 700; display: block; }
    .histo-body p { font-size: 0.82rem; color: #555; margin: 2px 0; }
    .histo-body small { font-size: 0.75rem; color: #999; font-family: 'DM Mono', monospace; }

    /* ── Empty State ── */
    .empty-state { text-align: center; padding: 30px 20px; color: #bbb; font-size: 0.9rem; }

    /* ── Modals ── */
    .modal-overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,0.5);
      backdrop-filter: blur(4px); z-index: 1000;
      display: flex; align-items: center; justify-content: center;
    }
    .modal-box {
      background: white; border-radius: 16px; width: 520px; max-width: 95vw;
      box-shadow: 0 20px 40px rgba(0,0,0,0.15);
      animation: fadeUp 0.2s ease;
    }
    @keyframes fadeUp { from { transform: translateY(10px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
    .modal-head {
      display: flex; justify-content: space-between; align-items: center;
      padding: 20px 24px; border-bottom: 1px solid #e8e8e4;
    }
    .modal-head h2 { font-size: 1.1rem; font-weight: 700; color: #1a1a1a; }
    .modal-close {
      background: #f0f0ec; border: none; width: 30px; height: 30px;
      border-radius: 50%; cursor: pointer; font-size: 0.9rem; color: #555;
    }
    .modal-body { padding: 24px; }
    .modal-foot {
      display: flex; justify-content: flex-end; gap: 10px;
      padding: 16px 24px; border-top: 1px solid #e8e8e4;
    }

    /* ── Form ── */
    .form-group { margin-bottom: 16px; }
    .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .form-group label {
      display: block; font-size: 0.78rem; font-weight: 700;
      color: #555; margin-bottom: 6px; text-transform: uppercase; letter-spacing: 0.4px;
    }
    .form-group input, .form-group select, .form-group textarea {
      width: 100%; padding: 10px 14px; border: 1.5px solid #e0e0dc;
      border-radius: 8px; font-size: 0.92rem; font-family: inherit;
      outline: none; transition: border-color 0.15s; color: #1a1a1a;
      background: #fafaf8;
    }
    .form-group input:focus, .form-group select:focus, .form-group textarea:focus {
      border-color: #1a1a1a; background: white;
    }
    .error-msg {
      background: #fce4ec; color: #c62828; padding: 10px 14px;
      border-radius: 8px; font-size: 0.85rem; margin-top: 8px; font-weight: 500;
    }
    .btn-cancel {
      background: #f0f0ec; color: #555; border: none;
      padding: 10px 20px; border-radius: 8px; font-weight: 600;
      font-size: 0.9rem; cursor: pointer; font-family: inherit;
    }
    .btn-submit {
      background: #1a1a1a; color: white; border: none;
      padding: 10px 20px; border-radius: 8px; font-weight: 700;
      font-size: 0.9rem; cursor: pointer; font-family: inherit;
    }
    .btn-submit:hover { background: #333; }
  `]
})
export class AgentDossierDetailComponent implements OnInit {

  dossier: any = null;
  historique: any[] = [];
  validateurs_financiers: any[] = [];
  validateurs_juridiques: any[] = [];
  missionAvocat: any = null;
  loading = true;

  // Modals
  showModalValidateurs = false;
  showModalRisque = false;

  // Erreurs
  erreurValidateurs = '';
  erreurRisque = '';

  // Formulaires
  formValidateurs = { validateurFinancier: '', validateurJuridique: '' };
  formRisque = {
    type: 'CREDIT_IMMOBILIER',
    montantInitial: 0,
    montantImpaye: 0,
    dateEcheance: '',
    description: ''
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private dossierService: DossierService
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.chargerDossier(id);
  }

  chargerDossier(id: number) {
    this.loading = true;
    this.dossierService.getDossierDetails(id).subscribe({
      next: (data: any) => {
        this.dossier        = data.dossier || data;
        this.historique     = data.historique || [];
        this.validateurs_financiers = data.validateurs_financiers || [];
        this.validateurs_juridiques = data.validateurs_juridiques || [];
        this.missionAvocat  = data.missionAvocat || null;
        // Pré-remplir validateurs si déjà choisis
        if (this.dossier.validateurFinancierChoisi)
          this.formValidateurs.validateurFinancier = this.dossier.validateurFinancierChoisi;
        if (this.dossier.validateurJuridiqueChoisi)
          this.formValidateurs.validateurJuridique = this.dossier.validateurJuridiqueChoisi;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur chargement dossier', err);
        this.loading = false;
      }
    });
  }

  // ── Helpers affichage ──

  getClientName(): string {
    if (!this.dossier?.client) return 'N/A';
    const c = this.dossier.client;
    if (c.typeClient === 'ENTREPRISE') return c.raisonSociale || 'N/A';
    return `${c.nom || ''} ${c.prenom || ''}`.trim() || 'N/A';
  }

  getStatutClass(statut: string): string {
    const map: any = {
      'OUVERT':        'statut-ouvert',
      'EN_TRAITEMENT': 'statut-en-traitement',
      'VALIDE':        'statut-valide',
      'REJETE':        'statut-rejete',
      'CLOS':          'statut-clos'
    };
    return map[statut] || '';
  }

  formatStatut(statut: string): string {
    const map: any = {
      'OUVERT':        'Ouvert',
      'EN_TRAITEMENT': 'En traitement',
      'VALIDE':        'Validé',
      'REJETE':        'Rejeté',
      'CLOS':          'Clos'
    };
    return map[statut] || statut;
  }

  canModifier(): boolean {
    return this.dossier?.statut === 'OUVERT' || this.dossier?.statut === 'REJETE';
  }

  canSoumettre(): boolean {
    return this.canModifier() &&
      this.dossier?.risques?.some((r: any) => r.selectionne);
  }

  // ── Actions ──

  ouvrirModalValidateurs() {
    this.erreurValidateurs = '';
    this.showModalValidateurs = true;
  }

  ouvrirModificationDossier() {
    this.router.navigate(['/agent/dossiers', this.dossier.id, 'modifier']);
  }

  choisirEtSoumettre() {
    this.erreurValidateurs = '';
    const { validateurFinancier, validateurJuridique } = this.formValidateurs;
    if (!validateurFinancier || !validateurJuridique) {
      this.erreurValidateurs = 'Veuillez choisir les deux validateurs.';
      return;
    }

    this.dossierService.choisirValidateurs(this.dossier.id, this.formValidateurs).subscribe({
      next: () => {
        this.dossierService.soumettreAValidation(this.dossier.id).subscribe({
          next: () => {
            this.showModalValidateurs = false;
            this.chargerDossier(this.dossier.id);
          },
          error: (err: any) => {
            this.erreurValidateurs = err.error?.error || 'Erreur lors de la soumission.';
          }
        });
      },
      error: (err: any) => {
        this.erreurValidateurs = err.error?.error || 'Erreur lors du choix des validateurs.';
      }
    });
  }

  toggleRisque(risque: any) {
    this.dossierService.selectionnerRisque(this.dossier.id, risque.id, !risque.selectionne).subscribe({
      next: () => this.chargerDossier(this.dossier.id),
      error: (err: any) => alert(err.error?.error || 'Erreur sélection risque')
    });
  }

  ajouterRisque() {
    this.erreurRisque = '';
    this.dossierService.ajouterRisque(this.dossier.id, this.formRisque).subscribe({
      next: () => {
        this.showModalRisque = false;
        this.formRisque = { type: 'CREDIT_IMMOBILIER', montantInitial: 0, montantImpaye: 0, dateEcheance: '', description: '' };
        this.chargerDossier(this.dossier.id);
      },
      error: (err: any) => {
        this.erreurRisque = err.error?.error || 'Erreur lors de l\'ajout.';
      }
    });
  }

  supprimerGarantie(garantieId: number) {
    if (!confirm('Supprimer cette garantie ?')) return;
    this.dossierService.supprimerGarantie(garantieId).subscribe({
      next: () => this.chargerDossier(this.dossier.id),
      error: (err: any) => alert(err.error?.error || 'Erreur suppression garantie')
    });
  }

  supprimerDossier() {
    if (!confirm('Supprimer définitivement ce dossier ?')) return;
    this.dossierService.supprimerDossier(this.dossier.id).subscribe({
      next: () => this.router.navigate(['/agent/dossiers']),
      error: (err: any) => alert(err.error?.error || 'Erreur suppression')
    });
  }

  telechargerPdf() {
    if (this.dossierService.telechargerPdf) {
      this.dossierService.telechargerPdf(this.dossier.id).subscribe((blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `dossier-${this.dossier.numeroDossier}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      });
    }
  }
}