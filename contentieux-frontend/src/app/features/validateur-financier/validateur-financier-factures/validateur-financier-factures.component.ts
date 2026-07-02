import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { NotificationService, NotificationDTO } from '../../../core/services/notification.service';

@Component({
  selector: 'app-validateur-financier-factures',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  template: `
    <div class="page-container">

    <!-- ══ Onglets ══ -->
    <div class="tabs-bar">
      <button class="tab-btn" [class.tab-active]="ongletActif === 'prestataires'" (click)="ongletActif='prestataires'">
        🧑‍💼 Factures Prestataires
      </button>
      <button class="tab-btn" [class.tab-active]="ongletActif === 'avocats'" (click)="ongletActif='avocats'; chargerFacturesAvocat()">
        ⚖️ Factures d'Avocat
        <span class="tab-badge" *ngIf="facturesAvocat.length > 0">{{ facturesAvocat.length }}</span>
      </button>
    </div>

    <!-- ══ ONGLET PRESTATAIRES ══ -->
    <ng-container *ngIf="ongletActif === 'prestataires'">

      <!-- ══ Header ══ -->
      <div class="page-header">
        <div class="title-group">
          <span class="role-badge">VALIDATEUR FINANCIER</span>
          <h1>Factures des Prestataires</h1>
          <p class="subtitle">
            {{ totalDossiers }} dossier(s) —
            {{ totalFactures }} facture(s) soumise(s)
          </p>
        </div>
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input type="text"
                 [(ngModel)]="recherche"
                 placeholder="Rechercher un dossier ou prestataire..."
                 class="search-input">
        </div>
      </div>

      <!-- ══ Bannière nouvelle facture ══ -->
      <div class="nouvelle-facture-banner" *ngIf="nouvelleBanniereVisible"
           (click)="rechargerEtFermerBanniere()">
        💰 Nouvelle facture reçue —
        <strong>cliquez pour actualiser</strong>
        <button class="banner-close" (click)="$event.stopPropagation(); fermerBanniere()">✕</button>
      </div>

      <!-- ══ Diagrammes statiques ══ -->
      <section class="charts-panel" *ngIf="!loading && dossiers.length > 0">
        <div class="chart-card chart-card-full">
          <div class="chart-header">
            <h3>Répartition des factures</h3>
            <span>Vue synthétique</span>
          </div>
          <div class="bars">
            <div class="bar-item" *ngFor="let bar of chartBars">
              <div class="bar-track">
                <div class="bar-fill" [style.height.%]="bar.percent" [style.background]="bar.color"></div>
              </div>
              <div class="bar-meta">
                <strong>{{ bar.value }}</strong>
                <span>{{ bar.label }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ══ Loader ══ -->
      <div class="loader-state" *ngIf="loading">
        <div class="spinner"></div>
        <p>Chargement des factures...</p>
      </div>

      <!-- ══ Vide ══ -->
      <div class="empty-state"
           *ngIf="!loading && dossiersFiltres().length === 0">
        <div class="empty-icon">🧾</div>
        <p class="empty-text">Aucune facture soumise pour le moment.</p>
      </div>

      <!-- ══ Liste dossiers ══ -->
      <div *ngFor="let dossier of dossiersFiltres()"
           class="dossier-block"
           [id]="'dossier-' + dossier.dossierId">

        <!-- En-tête dossier -->
        <div class="dossier-header"
             (click)="toggleDossier(dossier.dossierId)">
          <div class="dossier-left">
            <span class="dossier-icon">📁</span>
            <div>
              <div class="dossier-num">{{ dossier.numeroDossier }}</div>
              <div class="dossier-lib">{{ dossier.libelle }}</div>
              <div class="dossier-client" *ngIf="dossier.clientNom">
                👤 {{ dossier.clientNom }}
              </div>
            </div>
          </div>
          <div class="dossier-right">
            <div class="dossier-statuts">
              <span class="ds-badge ds-attente"
                    *ngIf="countDossierStatut(dossier, 'attente') > 0">
                ⏳ {{ countDossierStatut(dossier, 'attente') }}
              </span>
              <span class="ds-badge ds-valide"
                    *ngIf="countDossierStatut(dossier, 'valide') > 0">
                ✅ {{ countDossierStatut(dossier, 'valide') }}
              </span>
              <span class="ds-badge ds-rejete"
                    *ngIf="countDossierStatut(dossier, 'rejete') > 0">
                ❌ {{ countDossierStatut(dossier, 'rejete') }}
              </span>
            </div>
            <div class="total-badge">
              <div class="total-ht">HT : {{ dossier.totalHT | number:'1.3-3' }} TND</div>
              <div class="total-ttc">TTC : {{ dossier.totalTTC | number:'1.3-3' }} TND</div>
            </div>
            <span class="nb-factures">{{ dossier.factures.length }} facture(s)</span>
            <span class="toggle-icon">{{ isDossierOpen(dossier.dossierId) ? '▲' : '▼' }}</span>
          </div>
        </div>

        <!-- Tableau factures -->
        <div class="factures-table-wrap" *ngIf="isDossierOpen(dossier.dossierId)">
          <table class="factures-table">
            <thead>
              <tr>
                <th>Prestataire</th>
                <th>Type</th>
                <th>Référence</th>
                <th>Montant HT</th>
                <th>TVA 19%</th>
                <th>Total TTC</th>
                <th>Statut</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
            <tr *ngFor="let f of dossier.factures"
    [id]="'facture-' + f.missionId"
    [ngClass]="{
      'row-valide':   isValidee(f),
      'row-rejete':   isRejete(f),
      'row-attente':  peutEtreTraitee(f),
      'row-nouvelle': f._nouvelle
    }">

                <td>
                  <div class="prestataire-cell">
                    <div class="prestataire-avatar" [ngClass]="avatarClass(f.prestataireType)">
                      {{ initiales(f.prestataireNom) }}
                    </div>
                    <div>
                      <div class="prestataire-nom">{{ f.prestataireNom }}</div>
                      <div class="prestataire-email">{{ f.prestataireEmail }}</div>
                    </div>
                  </div>
                </td>
                <td>
                  <span class="type-badge" [ngClass]="typeBadgeClass(f.prestataireType)">
                    {{ typeLabel(f.prestataireType) }}
                  </span>
                </td>
                <td class="mono">{{ f.factureRef }}</td>
                <td>{{ f.montantHT | number:'1.3-3' }} TND</td>
                <td class="tva-col">{{ f.montantHT * 0.19 | number:'1.3-3' }} TND</td>
                <td class="ttc-val">{{ f.montantTTC | number:'1.3-3' }} TND</td>
                <td>
                  <span class="statut-pill" [ngClass]="statutClass(f)">{{ statutLabel(f) }}</span>
                  <div *ngIf="isRejete(f) && f.commentaire" class="rejet-motif">
                    💬 {{ f.commentaire }}
                  </div>
                  <div *ngIf="f.dateFacture && isValidee(f)" class="date-validation">
                    {{ f.dateFacture | date:'dd/MM/yyyy HH:mm' }}
                  </div>
                </td>
                <td>
                  <div class="action-btns" *ngIf="peutEtreTraitee(f)">
                    <button class="btn-valider" (click)="validerFacture(f, true, dossier)" [disabled]="f.saving">
                      <span *ngIf="!f.saving">✓ Valider</span>
                      <span *ngIf="f.saving">...</span>
                    </button>
                    <button class="btn-rejeter" (click)="ouvrirModal(f, dossier)" [disabled]="f.saving">
                      ✕ Rejeter
                    </button>
                  </div>
                  <div *ngIf="isValidee(f)" class="traite-label valide-label">✅ Validée</div>
                  <div *ngIf="isRejete(f)"  class="traite-label rejete-label">❌ Rejetée</div>
                </td>
              </tr>
            </tbody>
            <tfoot>
              <tr class="total-row">
                <td colspan="3"><strong>TOTAL DOSSIER</strong></td>
                <td><strong>{{ dossier.totalHT | number:'1.3-3' }} TND</strong></td>
                <td><strong>{{ dossier.totalHT * 0.19 | number:'1.3-3' }} TND</strong></td>
                <td class="ttc-val"><strong>{{ dossier.totalTTC | number:'1.3-3' }} TND</strong></td>
                <td colspan="2"></td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </ng-container>

    <!-- ══ Modal rejet ══ -->
    <div class="modal-overlay" *ngIf="modalVisible" (click)="fermerModal()">
      <div class="modal-box" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <span class="modal-icon">❌</span>
          <h3>Motif de rejet</h3>
        </div>
        <p>
          Indiquez la raison du rejet de la facture
          <strong class="mono">{{ factureEnCours?.factureRef }}</strong>
          de <strong>{{ factureEnCours?.prestataireNom }}</strong>
        </p>
        <textarea [(ngModel)]="commentaireRejet" rows="4" class="modal-textarea"
                  placeholder="Ex: Montant incorrect, référence manquante..."></textarea>
        <div class="modal-actions">
          <button class="btn-annuler" (click)="fermerModal()">Annuler</button>
          <button class="btn-confirmer-rejet"
                  (click)="validerFacture(factureEnCours, false, dossierEnCours)"
                  [disabled]="!commentaireRejet.trim()">
            Confirmer le rejet
          </button>
        </div>
      </div>
    </div>

    <!-- ══ Toast ══ -->
    <div class="toast"
         [ngClass]="{ 'show': toastVisible, 'success': toastType === 'success', 'error': toastType === 'error', 'info': toastType === 'info' }">
      {{ toastMessage }}
    </div>

    <!-- ══ ONGLET FACTURES AVOCAT ══ -->
    <ng-container *ngIf="ongletActif === 'avocats'">

      <!-- Header -->
      <div class="page-header">
        <div class="title-group">
          <span class="role-badge">⚖️ FACTURES D'AVOCAT</span>
          <h1>Factures en attente de validation</h1>
          <p class="subtitle">{{ facturesAvocat.length }} facture(s) soumises par les avocats</p>
        </div>
        <button class="btn-refresh" (click)="chargerFacturesAvocat()">
          🔄 Actualiser
        </button>
      </div>

      <!-- Loader avocat -->
      <div class="loader-state" *ngIf="loadingAvocat">
        <div class="spinner"></div>
        <p>Chargement des factures d'avocat...</p>
      </div>

      <!-- Vide -->
      <div class="empty-state" *ngIf="!loadingAvocat && facturesAvocat.length === 0">
        <div class="empty-icon">⚖️</div>
        <p class="empty-text">Aucune facture d'avocat en attente de validation.</p>
      </div>

      <!-- Tableau -->
      <div class="factures-table-wrap" *ngIf="!loadingAvocat && facturesAvocat.length > 0">
        <table class="factures-table">
          <thead>
            <tr>
              <th>Avocat</th>
              <th>Dossier</th>
              <th>Client</th>
              <th>Réf. Facture</th>
              <th>Montant HT</th>
              <th>Total TTC</th>
              <th>Statut</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let aff of facturesAvocat"
                [id]="'facture-avocat-' + aff.affaireId"
                [ngClass]="{
                  'row-valide':   isValideeAvocat(aff),
                  'row-rejete':   isRejeteeAvocat(aff),
                  'row-attente':  peutEtreTraiteeAvocat(aff)
                }">
              <td>
                <div class="prestataire-cell">
                  <div class="prestataire-avatar avatar-avocat">{{ initiales(aff.avocatNom) }}</div>
                  <div>
                    <div class="prestataire-nom">{{ aff.avocatNom }}</div>
                    <div class="prestataire-email">{{ aff.avocatEmail }}</div>
                  </div>
                </div>
              </td>
              <td class="mono">{{ aff.numeroDossier }}</td>
              <td>{{ aff.clientNom || '—' }}</td>
              <td class="mono">{{ aff.factureRef }}</td>
              <td>{{ aff.montantHT | number:'1.3-3' }} TND</td>
              <td class="ttc-val">{{ aff.montantTTC | number:'1.3-3' }} TND</td>
              <td>
                <span class="statut-pill" [ngClass]="statutClassAvocat(aff)">{{ statutLabelAvocat(aff) }}</span>
                <div *ngIf="isRejeteeAvocat(aff) && aff.factureCommentaireValidation" class="rejet-motif">
                  💬 {{ aff.factureCommentaireValidation }}
                </div>
              </td>
              <td>
                <div class="action-btns" *ngIf="peutEtreTraiteeAvocat(aff)">
                  <button class="btn-valider" (click)="validerFactureAvocat(aff, true)" [disabled]="aff.saving">
                    <span *ngIf="!aff.saving">✓ Valider</span>
                    <span *ngIf="aff.saving">...</span>
                  </button>
                  <button class="btn-rejeter" (click)="ouvrirModalAvocat(aff)" [disabled]="aff.saving">✕ Rejeter</button>
                </div>
                <div *ngIf="isValideeAvocat(aff)" class="traite-label valide-label">✅ Validée</div>
                <div *ngIf="isRejeteeAvocat(aff)" class="traite-label rejete-label">❌ Rejetée</div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

    </ng-container><!-- /onglet avocats -->

    <!-- ══ Modal rejet avocat ══ -->
    <div class="modal-overlay" *ngIf="modalAvocatVisible" (click)="fermerModalAvocat()">
      <div class="modal-box" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <span class="modal-icon">❌</span>
          <h3>Motif de rejet</h3>
        </div>
        <p>
          Indiquez la raison du rejet de la facture
          <strong class="mono">{{ affaireEnCours?.factureRef }}</strong>
          de <strong>{{ affaireEnCours?.avocatNom }}</strong>
        </p>
        <textarea [(ngModel)]="commentaireRejetAvocat" rows="4" class="modal-textarea"
                  placeholder="Ex: Montant incorrect, référence manquante..."></textarea>
        <div class="modal-actions">
          <button class="btn-annuler" (click)="fermerModalAvocat()">Annuler</button>
          <button class="btn-confirmer-rejet"
                  (click)="validerFactureAvocat(affaireEnCours, false)"
                  [disabled]="!commentaireRejetAvocat.trim()">
            Confirmer le rejet
          </button>
        </div>
      </div>
    </div>

  `,

  styles: [`
    * { box-sizing: border-box; }

    .page-container {
      padding: 30px;
      background: #f0f2f5;
      min-height: 100vh;
      font-family: 'Inter', sans-serif;
    }

    /* ── Bannière nouvelle facture ── */
    .nouvelle-facture-banner {
      display: flex; align-items: center; gap: 10px;
      background: linear-gradient(135deg, #fef3c7, #fde68a);
      border: 1.5px solid #f59e0b; border-radius: 12px;
      padding: 14px 20px; margin-bottom: 20px;
      font-weight: 700; color: #92400e; cursor: pointer;
      transition: background 0.2s;
      box-shadow: 0 2px 8px rgba(245,158,11,0.2);
    }
    .nouvelle-facture-banner:hover { background: linear-gradient(135deg, #fde68a, #fbbf24); }
    .banner-close {
      margin-left: auto; background: none; border: none;
      font-size: 1rem; cursor: pointer; color: #92400e;
      padding: 2px 6px; border-radius: 4px;
    }
    .banner-close:hover { background: rgba(0,0,0,0.1); }

    /* ── Ligne nouvelle facture ── */
    .row-nouvelle { animation: highlight-new 2s ease-out forwards; }
    @keyframes highlight-new {
      0%   { background: #fef9c3 !important; }
      100% { background: white; }
    }

    /* ── Surlignage dossier / ligne depuis notification ── */
    .dossier-highlight {
      border: 2px solid #f59e0b !important;
      box-shadow: 0 0 0 4px rgba(245,158,11,0.2) !important;
      transition: all 0.3s ease;
    }

    /* ── Header ── */
    .page-header {
      display: flex; align-items: flex-start; justify-content: space-between;
      gap: 20px; margin-bottom: 24px; flex-wrap: wrap;
    }
    .role-badge {
      background: #fef3c7; color: #92400e; padding: 4px 10px; border-radius: 6px;
      font-weight: 800; font-size: 0.7rem; letter-spacing: 1px;
      display: inline-block; margin-bottom: 6px;
    }
    h1 { margin: 0; font-size: 1.8rem; color: #1e293b; font-weight: 800; }
    .subtitle { color: #64748b; margin-top: 4px; font-size: 0.9rem; }
    .search-box { position: relative; }
    .search-icon {
      position: absolute; left: 12px; top: 50%;
      transform: translateY(-50%); font-size: 0.85rem; pointer-events: none;
    }
    .search-input {
      padding: 10px 16px 10px 36px; border: 1.5px solid #e2e8f0;
      border-radius: 10px; font-size: 0.9rem; outline: none;
      width: 320px; background: white; font-family: inherit; transition: all 0.2s;
    }
    .search-input:focus { border-color: #f59e0b; box-shadow: 0 0 0 3px rgba(245,158,11,0.1); }

    /* ── Charts ── */
    .charts-panel { display: grid; grid-template-columns: 1fr; gap: 16px; margin-bottom: 24px; }
    .chart-card { background: white; border-radius: 16px; padding: 22px; border: 1px solid #e2e8f0; box-shadow: 0 2px 10px rgba(0,0,0,0.04); }
    .chart-card-full { padding: 24px 32px; }
    .chart-header { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 18px; }
    .chart-header h3 { margin: 0; font-size: 1rem; color: #1e293b; font-weight: 700; }
    .chart-header span { font-size: 0.75rem; color: #94a3b8; }

    .chart-card-full .bars { max-width: 480px; margin: 0 auto; }
    .bars { display: flex; align-items: flex-end; justify-content: center; gap: 36px; height: 150px; }
    .bar-item { flex: 1; max-width: 90px; display: flex; flex-direction: column; align-items: center; height: 100%; }
    .bar-track { flex: 1; width: 36px; background: #f1f5f9; border-radius: 8px; display: flex; align-items: flex-end; overflow: hidden; }
    .bar-fill { width: 100%; border-radius: 8px 8px 0 0; min-height: 4px; }
    .bar-meta { margin-top: 8px; display: flex; flex-direction: column; align-items: center; }
    .bar-meta strong { font-size: 0.9rem; color: #1e293b; font-weight: 800; }
    .bar-meta span { font-size: 0.68rem; color: #94a3b8; text-align: center; }

    /* ── Loader / Empty ── */
    .loader-state { display: flex; flex-direction: column; align-items: center; padding: 80px 0; color: #94a3b8; }
    .spinner {
      width: 40px; height: 40px; border: 3px solid #e2e8f0;
      border-top-color: #f59e0b; border-radius: 50%;
      animation: spin 0.8s linear infinite; margin-bottom: 15px;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .empty-state { text-align: center; padding: 60px; color: #94a3b8; }
    .empty-icon  { font-size: 3rem; margin-bottom: 12px; }
    .empty-text  { font-size: 1rem; font-weight: 600; }

    /* ── Dossier block ── */
    .dossier-block {
      background: white; border-radius: 16px;
      box-shadow: 0 2px 12px rgba(0,0,0,0.05);
      border: 1px solid #e2e8f0; margin-bottom: 20px; overflow: hidden;
    }
    .dossier-header {
      display: flex; justify-content: space-between; align-items: center;
      padding: 18px 24px; cursor: pointer; transition: background 0.2s;
      border-left: 4px solid #f59e0b;
    }
    .dossier-header:hover { background: #fffbeb; }
    .dossier-left { display: flex; align-items: center; gap: 14px; }
    .dossier-icon { font-size: 1.6rem; }
    .dossier-num  { font-weight: 800; color: #1e293b; font-size: 0.95rem; font-family: monospace; }
    .dossier-lib  { color: #64748b; font-size: 0.85rem; margin-top: 2px; }
    .dossier-client { color: #94a3b8; font-size: 0.78rem; margin-top: 2px; }
    .dossier-right { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
    .dossier-statuts { display: flex; gap: 6px; }
    .ds-badge { padding: 3px 8px; border-radius: 10px; font-size: 0.7rem; font-weight: 800; }
    .ds-attente { background: #fef9c3; color: #a16207; }
    .ds-valide  { background: #dcfce7; color: #15803d; }
    .ds-rejete  { background: #fee2e2; color: #dc2626; }
    .total-badge { text-align: right; }
    .total-ht  { font-size: 0.78rem; color: #64748b; }
    .total-ttc { font-size: 0.9rem; font-weight: 800; color: #d97706; }
    .nb-factures {
      background: #fef3c7; color: #92400e;
      padding: 3px 10px; border-radius: 12px; font-size: 0.75rem; font-weight: 700;
    }
    .toggle-icon { font-size: 0.8rem; color: #94a3b8; }

    /* ── Table ── */
    .factures-table-wrap { overflow-x: auto; }
    .factures-table { width: 100%; border-collapse: collapse; font-size: 0.84rem; }
    .factures-table th {
      padding: 11px 14px; background: #f8fafc; text-align: left;
      font-size: 0.72rem; font-weight: 800; color: #64748b;
      text-transform: uppercase; letter-spacing: 0.4px;
      border-bottom: 2px solid #e2e8f0; white-space: nowrap;
    }
    .factures-table td {
      padding: 13px 14px; border-bottom: 1px solid #f1f5f9;
      color: #374151; vertical-align: middle;
    }
    .factures-table tbody tr:last-child td { border-bottom: none; }
    .factures-table tbody tr:hover { background: #fafafa; }
    .row-valide  { background: #f0fdf4 !important; }
    .row-rejete  { background: #fff5f5 !important; }
    .row-attente { background: white; }
    .factures-table tfoot td {
      background: #fffbeb; padding: 12px 14px; border-top: 2px solid #fde68a;
    }

    /* ── Prestataire ── */
    .prestataire-cell { display: flex; align-items: center; gap: 10px; }
    .prestataire-avatar {
      width: 36px; height: 36px; border-radius: 50%; color: white;
      display: flex; align-items: center; justify-content: center;
      font-weight: 800; font-size: 0.78rem; flex-shrink: 0;
    }
    .avatar-avocat   { background: linear-gradient(135deg, #4338ca, #6366f1); }
    .avatar-expert   { background: linear-gradient(135deg, #059669, #10b981); }
    .avatar-huissier { background: linear-gradient(135deg, #d97706, #f59e0b); }
    .avatar-autre    { background: linear-gradient(135deg, #64748b, #94a3b8); }
    .prestataire-nom   { font-weight: 700; color: #1e293b; font-size: 0.85rem; }
    .prestataire-email { font-size: 0.75rem; color: #94a3b8; }

    /* ── Type badges ── */
    .type-badge { display: inline-block; padding: 3px 10px; border-radius: 20px; font-size: 0.7rem; font-weight: 800; white-space: nowrap; }
    .type-avocat   { background: #e0e7ff; color: #3730a3; }
    .type-expert   { background: #dcfce7; color: #15803d; }
    .type-huissier { background: #fef3c7; color: #92400e; }
    .type-autre    { background: #f1f5f9; color: #64748b; }

    /* ── Statut pills ── */
    .statut-pill { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 0.72rem; font-weight: 800; white-space: nowrap; }
    .pill-attente { background: #fef9c3; color: #a16207; }
    .pill-valide  { background: #dcfce7; color: #15803d; }
    .pill-rejete  { background: #fee2e2; color: #991b1b; }
    .rejet-motif { font-size: 0.73rem; color: #991b1b; background: #fef2f2; border-radius: 5px; padding: 3px 8px; margin-top: 5px; font-style: italic; }
    .date-validation { font-size: 0.7rem; color: #94a3b8; margin-top: 4px; }
    .tva-col { color: #64748b; }
    .mono    { font-family: monospace; font-size: 0.82rem; }
    .ttc-val { font-weight: 800; color: #d97706; }
    .total-row td { font-size: 0.85rem; }

    /* ── Actions ── */
    .action-btns { display: flex; gap: 6px; }
    .btn-valider {
      background: #dcfce7; color: #15803d; border: none; padding: 6px 12px;
      border-radius: 7px; font-weight: 700; font-size: 0.78rem; cursor: pointer;
      transition: all 0.2s; font-family: inherit;
    }
    .btn-valider:hover:not(:disabled) { background: #bbf7d0; }
    .btn-rejeter {
      background: #fee2e2; color: #dc2626; border: none; padding: 6px 12px;
      border-radius: 7px; font-weight: 700; font-size: 0.78rem; cursor: pointer;
      transition: all 0.2s; font-family: inherit;
    }
    .btn-rejeter:hover:not(:disabled) { background: #fecaca; }
    .btn-valider:disabled, .btn-rejeter:disabled { opacity: 0.5; cursor: not-allowed; }
    .traite-label { font-size: 0.78rem; font-weight: 700; }
    .valide-label { color: #15803d; }
    .rejete-label { color: #dc2626; }

    /* ── Modal ── */
    .modal-overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,0.4);
      display: flex; align-items: center; justify-content: center; z-index: 1000;
    }
    .modal-box { background: white; border-radius: 16px; padding: 32px; width: 480px; box-shadow: 0 20px 60px rgba(0,0,0,0.2); }
    .modal-header { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
    .modal-icon { font-size: 1.5rem; }
    .modal-box h3 { margin: 0; font-size: 1.2rem; color: #1e293b; }
    .modal-box p  { color: #64748b; font-size: 0.9rem; margin-bottom: 16px; }
    .modal-textarea {
      width: 100%; padding: 12px; border: 1.5px solid #e2e8f0;
      border-radius: 8px; font-family: inherit; font-size: 0.9rem;
      resize: vertical; outline: none; transition: border 0.2s;
    }
    .modal-textarea:focus { border-color: #dc2626; }
    .modal-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 16px; }
    .btn-annuler {
      background: #f1f5f9; color: #64748b; border: none; padding: 10px 20px;
      border-radius: 8px; font-weight: 700; cursor: pointer; font-family: inherit;
    }
    .btn-confirmer-rejet {
      background: #dc2626; color: white; border: none; padding: 10px 20px;
      border-radius: 8px; font-weight: 700; cursor: pointer; font-family: inherit; transition: background 0.2s;
    }
    .btn-confirmer-rejet:hover:not(:disabled) { background: #b91c1c; }
    .btn-confirmer-rejet:disabled { opacity: 0.5; cursor: not-allowed; }

    /* ── Toast ── */
    .toast {
      position: fixed; bottom: 30px; right: 30px; padding: 14px 24px;
      border-radius: 12px; font-weight: 700; font-size: 0.9rem;
      opacity: 0; transform: translateY(20px); transition: all 0.3s;
      z-index: 9999; pointer-events: none; min-width: 260px; text-align: center;
    }
    .toast.show    { opacity: 1; transform: translateY(0); }
    .toast.success { background: #059669; color: white; }
    .toast.error   { background: #dc2626; color: white; }
    .toast.info    { background: #d97706; color: white; }
  `]
})
export class ValidateurFinancierFacturesComponent implements OnInit, OnDestroy {

  dossiers:     any[] = [];
  totalDossiers = 0;
  totalFactures = 0;
  loading       = true;
  recherche     = '';

  // Onglets
  ongletActif: 'prestataires' | 'avocats' = 'prestataires';

  private _missionId_cible: number | null = null;

  // Modal (prestataires)
  modalVisible      = false;
  factureEnCours:  any = null;
  dossierEnCours:  any = null;
  commentaireRejet = '';

  // Factures avocat
  facturesAvocat: any[] = [];
  loadingAvocat  = false;

  // ✅ Affaire d'avocat ciblée depuis une notification (?affaireId=...)
  private _affaireId_cible: number | null = null;

  // Modal (avocat)
  modalAvocatVisible     = false;
  affaireEnCours:  any   = null;
  commentaireRejetAvocat = '';

  // Toast
  toastVisible = false;
  toastMessage = '';
  toastType: 'success' | 'error' | 'info' = 'success';

  // Accordéon
  dossiersOuverts = new Set<number>();

  // Bannière nouvelle facture
  nouvelleBanniereVisible = false;
  private banniereTimeout: any = null;

  private _derniereNotifId: number | null = null;
  private _dossierId_cible: number | null = null;
  private destroy$ = new Subject<void>();

  private api = `${environment.apiUrl}/api/validateur/financier`;

  constructor(
    private http: HttpClient,
    private notificationService: NotificationService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // ── Lire les paramètres ?tab=avocats&affaireId=... depuis l'URL ───────────────
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        if (params['tab'] === 'avocats') {
          this.ongletActif = 'avocats';

          const affaireIdParam = params['affaireId'];
          if (affaireIdParam) {
            this._affaireId_cible = Number(affaireIdParam);
            // 🔍 DEBUG — à retirer une fois le problème confirmé/résolu
            console.log('🔍 [ValidateurFinancierFactures] affaireId cible depuis URL :', this._affaireId_cible);
          }

          this.chargerFacturesAvocat();
        }
      });

    this.notificationService.factureCible$
      .pipe(takeUntil(this.destroy$))
      .subscribe((id: number | null) => {
        if (id !== null) {
          this._dossierId_cible = id;
          this.notificationService.signalerFactureCible(null);
          if (!this.loading && this.dossiers.length > 0) {
            setTimeout(() => this._surlignerFacture(id, this._missionId_cible), 100);
          }
        }
      });

    // ✅ Écouter aussi missionId pour surligner la bonne ligne
    this.notificationService.missionCible$
    .pipe(takeUntil(this.destroy$))
    .subscribe((missionIdOrNum: string | number | null) => {
      if (missionIdOrNum === null) return;

      if (typeof missionIdOrNum === 'string') {
        // Numéro textuel (ex: "MISS-2026-00036")
        // Pour ce composant, on ignore le string car il gère les factures par ID
        // Le fallback dans _surlignerFacture prendra la première facture en attente
        this._missionId_cible = null;
      } else {
        // ID numérique direct
        this._missionId_cible = missionIdOrNum;
      }
      this.notificationService.signalerMissionCible(null);
    });

    this.charger();
    this._ecouterNouvellesFactures();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.banniereTimeout) clearTimeout(this.banniereTimeout);
  }

  // ════════════════════════════════════════
  // Factures Avocat
  // ════════════════════════════════════════

  chargerFacturesAvocat(): void {
    this.loadingAvocat = true;
    this.http.get<any>(`${this.api}/affaires/factures`).subscribe({
      next: (res) => {
        this.facturesAvocat = res?.affaires ?? [];
        this.loadingAvocat  = false;

        // 🔍 DEBUG — à retirer une fois le problème confirmé/résolu
        console.log(
          '🔍 [ValidateurFinancierFactures] Factures avocat reçues :',
          this.facturesAvocat.map(a => a.affaireId)
        );

        // ✅ Si une affaire est ciblée (depuis notification), la surligner
        // ⚠️ Fix course de conditions : on ne remet _affaireId_cible à null
        // qu'APRÈS la tentative de surlignage, pour survivre à un éventuel
        // second chargement déclenché en parallèle (ex: événement WebSocket).
        if (this._affaireId_cible !== null) {
          const affaireId = this._affaireId_cible;
          setTimeout(() => {
            this._surlignerFactureAvocat(affaireId);
            this._affaireId_cible = null;
          }, 250);
        }
      },
      error: () => {
        this.showToast('Impossible de charger les factures d\'avocat.', 'error');
        this.loadingAvocat = false;
      }
    });
  }

  // ── Surligne + scrolle jusqu'à la ligne de la facture d'avocat ciblée ──
  private _surlignerFactureAvocat(affaireId: number): void {
    const el = document.getElementById('facture-avocat-' + affaireId);
    if (!el) {
      // 🔍 DEBUG — si ce warning apparaît, l'ID cible ne correspond à aucune
      // ligne du DOM : vérifier le nom du champ renvoyé par le backend
      // (affaireId vs id/idAffaire) et si l'affaire cible est bien présente
      // dans la liste chargée (voir le log ci-dessus).
      console.warn('⚠️ Facture avocat cible introuvable dans le DOM :', affaireId);
      return;
    }
    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    el.classList.add('row-nouvelle');
    setTimeout(() => el.classList.remove('row-nouvelle'), 4000);
  }

  ouvrirModalAvocat(aff: any): void {
    this.affaireEnCours         = aff;
    this.commentaireRejetAvocat = '';
    this.modalAvocatVisible     = true;
  }

  fermerModalAvocat(): void {
    this.modalAvocatVisible = false;
    this.affaireEnCours     = null;
  }

  validerFactureAvocat(aff: any, valide: boolean): void {
    if (!aff) return;
    aff.saving = true;
    this.modalAvocatVisible = false;

    const body = { valide, commentaire: valide ? '' : this.commentaireRejetAvocat };

    this.http.post(
      `${this.api}/affaires/${aff.affaireId}/valider-facture`, body
    ).subscribe({
      next: () => {
        aff.saving = false;
        aff.factureStatut = valide ? 'PAYEE' : 'REJETEE';
        if (!valide) aff.factureCommentaireValidation = this.commentaireRejetAvocat;
        this.showToast(
          valide ? '✓ Facture avocat validée avec succès !' : '✓ Facture avocat rejetée.',
          'success'
        );
        this.fermerModalAvocat();
      },
      error: (e) => {
        aff.saving = false;
        this.showToast(e.error?.error || 'Erreur lors de la validation.', 'error');
      }
    });
  }

  // ════════════════════════════════════════
  // Écoute WebSocket — nouvelles factures
  // ════════════════════════════════════════

  private _ecouterNouvellesFactures(): void {
    this.notificationService.nouvelleNotif$
      .pipe(takeUntil(this.destroy$))
      .subscribe((notif: NotificationDTO) => {
        const typesFacture = [
          'FACTURE_SOUMISE', 'RESOUMISSION',
          'RESULTAT_SOUMIS', 'RESULTAT_MODIFIE',
          'FACTURE_AVOCAT_SOUMISE'
        ];
        if (!typesFacture.includes(notif.type)) return;
        if (this._derniereNotifId === notif.id) return;
        this._derniereNotifId = notif.id;

        // Si onglet avocats actif, recharger les factures avocat
        if (notif.type === 'FACTURE_AVOCAT_SOUMISE' || this.ongletActif === 'avocats') {
          this.chargerFacturesAvocat();
          this.showToast('⚖️ Nouvelle facture d\'avocat reçue — liste mise à jour', 'info');
          return;
        }

        // Recharger automatiquement et surligner le dossier
        if (notif.dossierId) {
          this._dossierId_cible = notif.dossierId;
        }
        this.charger();

        // ✅ Afficher la bannière + auto-masquage après 8s
        this.nouvelleBanniereVisible = true;
        if (this.banniereTimeout) clearTimeout(this.banniereTimeout);
        this.banniereTimeout = setTimeout(() => {
          this.nouvelleBanniereVisible = false;
        }, 8000);

        this.showToast('💰 Nouvelle facture reçue — liste mise à jour', 'info');
      });
  }

  // ════════════════════════════════════════
  // Surlignage dossier
  // ════════════════════════════════════════

  private _surlignerFacture(dossierId: number, missionId: number | null): void {
    const dossier = this.dossiers.find(d => d.dossierId === dossierId);
    if (!dossier) {
      console.warn('Dossier cible introuvable :', dossierId);
      return;
    }

    // Ouvrir le dossier
    this.dossiersOuverts.add(dossierId);

    setTimeout(() => {
      // ── Surligner le bloc dossier ──
      const elDossier = document.getElementById('dossier-' + dossierId);
      if (elDossier) {
        elDossier.scrollIntoView({ behavior: 'smooth', block: 'start' });
        elDossier.classList.add('dossier-highlight');
        setTimeout(() => elDossier.classList.remove('dossier-highlight'), 4000);
      }

      // ── Trouver la facture exacte ──
      let factureCible: any = null;

      if (missionId) {
        // Chercher par missionId (le plus précis)
        factureCible = dossier.factures?.find((f: any) => f.missionId === missionId);
      }

      if (!factureCible) {
        // Fallback : première facture en attente
        factureCible = dossier.factures?.find((f: any) => this.peutEtreTraitee(f));
      }

      if (!factureCible) return;

      // ── Scroll + surlignage de la ligne facture ──
      setTimeout(() => {
        const elFacture = document.getElementById('facture-' + factureCible.missionId);
        if (!elFacture) return;
        elFacture.scrollIntoView({ behavior: 'smooth', block: 'center' });
        elFacture.classList.add('row-nouvelle');
        setTimeout(() => elFacture.classList.remove('row-nouvelle'), 4000);
      }, 300);

    }, 200);
  }
  // ════════════════════════════════════════
  // Recharger depuis la bannière
  // ════════════════════════════════════════

  rechargerEtFermerBanniere(): void {
    this.nouvelleBanniereVisible = false;
    if (this.banniereTimeout) clearTimeout(this.banniereTimeout);
    this.charger();
  }

  fermerBanniere(): void {
    this.nouvelleBanniereVisible = false;
    if (this.banniereTimeout) clearTimeout(this.banniereTimeout);
  }

  // ════════════════════════════════════════
  // Chargement
  // ════════════════════════════════════════

  charger(): void {
    this.loading = true;
    this.http.get<any>(`${this.api}/factures`).subscribe({
      next: (res) => {
        this.dossiers      = res.dossiers ?? [];
        this.totalDossiers = res.totalDossiers ?? 0;
        this.totalFactures = res.totalFactures ?? 0;
        this.dossiers.forEach(d => this.dossiersOuverts.add(d.dossierId));
        this.loading = false;

        if (this._dossierId_cible) {
          const dossierId  = this._dossierId_cible;
          const missionId  = this._missionId_cible;
          this._dossierId_cible = null;
          this._missionId_cible = null;
          setTimeout(() => this._surlignerFacture(dossierId, missionId), 400);
        }
      },
      error: () => {
        this.showToast('Impossible de charger les factures.', 'error');
        this.loading = false;
      }
    });
  }

  // ════════════════════════════════════════
  // Filtrage
  // ════════════════════════════════════════

  dossiersFiltres(): any[] {
    if (!this.recherche.trim()) return this.dossiers;
    const q = this.recherche.toLowerCase();
    return this.dossiers.filter(d =>
      d.numeroDossier?.toLowerCase().includes(q) ||
      d.libelle?.toLowerCase().includes(q)       ||
      d.clientNom?.toLowerCase().includes(q)     ||
      d.factures?.some((f: any) =>
        f.prestataireNom?.toLowerCase().includes(q) ||
        f.factureRef?.toLowerCase().includes(q)
      )
    );
  }

  // ════════════════════════════════════════
  // Stats
  // ════════════════════════════════════════

  countStatut(type: 'attente' | 'valide' | 'rejete'): number {
    return this.dossiers.reduce((total, d) => {
      return total + d.factures.filter((f: any) => {
        if (type === 'valide') return this.isValidee(f);
        if (type === 'rejete') return this.isRejete(f);
        return this.peutEtreTraitee(f);
      }).length;
    }, 0);
  }

  countDossierStatut(dossier: any, type: 'attente' | 'valide' | 'rejete'): number {
    return dossier.factures.filter((f: any) => {
      if (type === 'valide') return this.isValidee(f);
      if (type === 'rejete') return this.isRejete(f);
      return this.peutEtreTraitee(f);
    }).length;
  }

  // ════════════════════════════════════════
  // Diagrammes statiques
  // ════════════════════════════════════════

  /**
   * Répartition des factures : Total, En attente, Validées, Rejetées.
   * Hauteurs calculées par rapport au MAX (pas au total), car "Total"
   * n'est pas la somme des 3 autres catégories du point de vue visuel
   * (il englobe déjà les 3 sous-statuts).
   */
  get chartBars(): { key: string; label: string; color: string; value: number; percent: number }[] {
    const items = [
      { key: 'total',   label: 'Total factures', color: '#d97706', value: this.totalFactures },
      { key: 'attente', label: 'En attente',      color: '#a16207', value: this.countStatut('attente') },
      { key: 'valide',  label: 'Validées',        color: '#15803d', value: this.countStatut('valide') },
      { key: 'rejete',  label: 'Rejetées',        color: '#dc2626', value: this.countStatut('rejete') },
    ];
    const max = Math.max(...items.map(i => i.value), 1);
    return items.map(i => ({
      ...i,
      percent: Math.round((i.value / max) * 100)
    }));
  }

  // ════════════════════════════════════════
  // Accordéon
  // ════════════════════════════════════════

  toggleDossier(id: number): void {
    this.dossiersOuverts.has(id)
      ? this.dossiersOuverts.delete(id)
      : this.dossiersOuverts.add(id);
  }

  isDossierOpen(id: number): boolean {
    return this.dossiersOuverts.has(id);
  }

  // ════════════════════════════════════════
  // Validation / Rejet
  // ════════════════════════════════════════

  ouvrirModal(facture: any, dossier: any): void {
    this.factureEnCours   = facture;
    this.dossierEnCours   = dossier;
    this.commentaireRejet = '';
    this.modalVisible     = true;
  }

  fermerModal(): void {
    this.modalVisible   = false;
    this.factureEnCours = null;
    this.dossierEnCours = null;
  }

  validerFacture(facture: any, valide: boolean, dossier: any): void {
    if (!facture) return;
    facture.saving    = true;
    this.modalVisible = false;

    const body = { valide, commentaire: valide ? '' : this.commentaireRejet };

    this.http.post(
      `${this.api}/missions/${facture.missionId}/valider-facture`, body
    ).subscribe({
      next: () => {
        facture.saving        = false;
        facture.factureValide = valide;
        facture.statutMission = valide ? 'FACTURE_VALIDEE' : 'FACTURE_REJETEE';
        if (!valide) facture.commentaire = this.commentaireRejet;
        this.showToast(
          valide ? '✓ Facture validée avec succès !' : '✓ Facture rejetée.',
          'success'
        );
        this.fermerModal();
      },
      error: (e) => {
        facture.saving = false;
        this.showToast(e.error?.error || 'Erreur lors de la validation.', 'error');
      }
    });
  }

  // ════════════════════════════════════════
  // Helpers état
  // ════════════════════════════════════════

  isValidee(f: any): boolean {
    const statutsValides = ['FACTURE_VALIDEE', 'VALIDEE_AGENT', 'TERMINEE'];
    if (f.statutMission && statutsValides.includes(f.statutMission)) return true;
    return f.factureValide === true;
  }

  isRejete(f: any): boolean {
    const statutsRejetes = ['FACTURE_REJETEE', 'REJETEE'];
    if (f.statutMission && statutsRejetes.includes(f.statutMission)) return true;
    return f.factureValide === false && !!f.commentaire;
  }

  peutEtreTraitee(f: any): boolean {
    if (this.isValidee(f)) return false;
    if (this.isRejete(f))  return false;
    const statutsAttente = ['FACTURE_SOUMISE', null, undefined, ''];
    return statutsAttente.includes(f.statutMission) || f.factureValide == null;
  }

  // ════════════════════════════════════════
  // Helpers affichage
  // ════════════════════════════════════════

  statutClass(f: any): string {
    if (this.isValidee(f)) return 'pill-valide';
    if (this.isRejete(f))  return 'pill-rejete';
    return 'pill-attente';
  }

  statutLabel(f: any): string {
    if (this.isValidee(f)) return '✅ Validée';
    if (this.isRejete(f))  return '❌ Rejetée';
    return '⏳ En attente';
  }

  // ── Statut facture avocat (factureStatut : 'EN_ATTENTE_VALIDATION' | 'PAYEE' | 'REJETEE') ──

  isValideeAvocat(aff: any): boolean {
    return aff.factureStatut === 'PAYEE';
  }

  isRejeteeAvocat(aff: any): boolean {
    return aff.factureStatut === 'REJETEE';
  }

  peutEtreTraiteeAvocat(aff: any): boolean {
    return !this.isValideeAvocat(aff) && !this.isRejeteeAvocat(aff);
  }

  statutClassAvocat(aff: any): string {
    if (this.isValideeAvocat(aff)) return 'pill-valide';
    if (this.isRejeteeAvocat(aff)) return 'pill-rejete';
    return 'pill-attente';
  }

  statutLabelAvocat(aff: any): string {
    if (this.isValideeAvocat(aff)) return '✅ Validée';
    if (this.isRejeteeAvocat(aff)) return '❌ Rejetée';
    return '⏳ En attente';
  }

  avatarClass(type: string): string {
    const map: Record<string, string> = {
      'AVOCAT': 'avatar-avocat', 'EXPERT': 'avatar-expert', 'HUISSIER': 'avatar-huissier'
    };
    return map[type] ?? 'avatar-autre';
  }

  typeBadgeClass(type: string): string {
    const map: Record<string, string> = {
      'AVOCAT': 'type-avocat', 'EXPERT': 'type-expert', 'HUISSIER': 'type-huissier'
    };
    return map[type] ?? 'type-autre';
  }

  typeLabel(type: string): string {
    const map: Record<string, string> = {
      'AVOCAT': '⚖️ Avocat', 'EXPERT': '🔬 Expert', 'HUISSIER': '📋 Huissier'
    };
    return map[type] ?? type ?? '—';
  }

  initiales(nom: string): string {
    if (!nom) return '?';
    return nom.split(' ').map(p => p[0]?.toUpperCase()).slice(0, 2).join('');
  }

  private showToast(message: string, type: 'success' | 'error' | 'info'): void {
    this.toastMessage = message;
    this.toastType    = type;
    this.toastVisible = true;
    setTimeout(() => this.toastVisible = false, 4000);
  }
}