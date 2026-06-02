import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { NotificationService, NotificationDTO } from '../../../core/services/notification.service';

@Component({
  selector: 'app-validateur-financier-factures',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  template: `
    <div class="page-container">

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

      <!-- ══ Stats ══ -->
      <div class="stats-bar" *ngIf="!loading && dossiers.length > 0">
        <div class="stat-item">
          <span class="stat-val">{{ totalFactures }}</span>
          <span class="stat-lab">Total factures</span>
        </div>
        <div class="stat-item stat-attente">
          <span class="stat-val">{{ countStatut('attente') }}</span>
          <span class="stat-lab">En attente</span>
        </div>
        <div class="stat-item stat-valide">
          <span class="stat-val">{{ countStatut('valide') }}</span>
          <span class="stat-lab">Validées</span>
        </div>
        <div class="stat-item stat-rejete">
          <span class="stat-val">{{ countStatut('rejete') }}</span>
          <span class="stat-lab">Rejetées</span>
        </div>
      </div>

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
      <div *ngFor="let dossier of dossiersFiltres()" class="dossier-block">

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
                  [ngClass]="{
                    'row-valide':  isValidee(f),
                    'row-rejete':  isRejete(f),
                    'row-attente': peutEtreTraitee(f),
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
                  <div *ngIf="isRejete(f)" class="traite-label rejete-label">❌ Rejetée</div>
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
    </div>

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
      display: flex;
      align-items: center;
      gap: 10px;
      background: linear-gradient(135deg, #fef3c7, #fde68a);
      border: 1.5px solid #f59e0b;
      border-radius: 12px;
      padding: 14px 20px;
      margin-bottom: 20px;
      font-weight: 700;
      color: #92400e;
      cursor: pointer;
      transition: background 0.2s;
      box-shadow: 0 2px 8px rgba(245,158,11,0.2);
    }
    .nouvelle-facture-banner:hover { background: linear-gradient(135deg, #fde68a, #fbbf24); }
    .banner-close {
      margin-left: auto;
      background: none;
      border: none;
      font-size: 1rem;
      cursor: pointer;
      color: #92400e;
      padding: 2px 6px;
      border-radius: 4px;
    }
    .banner-close:hover { background: rgba(0,0,0,0.1); }

    /* ── Ligne nouvelle facture ── */
    .row-nouvelle {
      animation: highlight-new 2s ease-out forwards;
    }
    @keyframes highlight-new {
      0%   { background: #fef9c3 !important; }
      100% { background: white; }
    }

    /* ── Header ── */
    .page-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 20px;
      margin-bottom: 24px;
      flex-wrap: wrap;
    }
    .role-badge {
      background: #fef3c7;
      color: #92400e;
      padding: 4px 10px;
      border-radius: 6px;
      font-weight: 800;
      font-size: 0.7rem;
      letter-spacing: 1px;
      display: inline-block;
      margin-bottom: 6px;
    }
    h1 { margin: 0; font-size: 1.8rem; color: #1e293b; font-weight: 800; }
    .subtitle { color: #64748b; margin-top: 4px; font-size: 0.9rem; }
    .search-box { position: relative; }
    .search-icon {
      position: absolute; left: 12px; top: 50%;
      transform: translateY(-50%); font-size: 0.85rem; pointer-events: none;
    }
    .search-input {
      padding: 10px 16px 10px 36px;
      border: 1.5px solid #e2e8f0;
      border-radius: 10px;
      font-size: 0.9rem;
      outline: none;
      width: 320px;
      background: white;
      font-family: inherit;
      transition: all 0.2s;
    }
    .search-input:focus { border-color: #f59e0b; box-shadow: 0 0 0 3px rgba(245,158,11,0.1); }

    /* ── Stats bar ── */
    .stats-bar {
      display: flex; gap: 16px; margin-bottom: 24px; flex-wrap: wrap;
    }
    .stat-item {
      background: white; border-radius: 12px; padding: 14px 20px;
      display: flex; flex-direction: column; align-items: center; gap: 4px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.04); border: 1px solid #e2e8f0; min-width: 110px;
    }
    .stat-val { font-size: 1.6rem; font-weight: 800; color: #1e293b; }
    .stat-lab { font-size: 0.72rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; }
    .stat-attente .stat-val { color: #a16207; }
    .stat-valide  .stat-val { color: #15803d; }
    .stat-rejete  .stat-val { color: #dc2626; }

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

  // Modal
  modalVisible      = false;
  factureEnCours:  any = null;
  dossierEnCours:  any = null;
  commentaireRejet = '';

  // Toast
  toastVisible = false;
  toastMessage = '';
  toastType: 'success' | 'error' | 'info' = 'success';

  // Accordéon
  dossiersOuverts = new Set<number>();

  // Bannière nouvelle facture
  nouvelleBanniereVisible = false;
  private banniereTimeout: any = null;

  private notifSub: Subscription | null = null;

  private api = `${environment.apiUrl}/api/validateur/financier`;

  constructor(
    private http: HttpClient,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.charger();
    this._ecouterNouvellesFactures();
  }

  ngOnDestroy(): void {
    this.notifSub?.unsubscribe();
    if (this.banniereTimeout) clearTimeout(this.banniereTimeout);
  }

  // ════════════════════════════════════════
  // Écoute WebSocket — nouvelles factures
  // ════════════════════════════════════════

  private _ecouterNouvellesFactures(): void {
    this.notifSub = this.notificationService.notifications$.subscribe(
      (notifs: NotificationDTO[]) => {
        if (!notifs.length) return;

        // La dernière notif reçue (en tête de liste après ajouterNotification)
        const derniere = notifs[0];

        // On réagit uniquement aux notifications de type facture soumise / resoumission
        // ✅ CORRIGÉ : utiliser les types réellement émis par le backend
        const typesFacture = ['FACTURE_SOUMISE', 'RESOUMISSION', 'RESULTAT_SOUMIS', 'RESULTAT_MODIFIE'];
        if (!typesFacture.includes(derniere.type)) return;

        // Éviter de réagir deux fois à la même notif
        if (this._derniereNotifId === derniere.id) return;
        this._derniereNotifId = derniere.id;

        // Afficher la bannière + toast sans recharger automatiquement
        this.nouvelleBanniereVisible = true;
        this.showToast('💰 Nouvelle facture reçue — cliquez sur la bannière pour actualiser', 'info');

        // Fermer la bannière automatiquement après 60s si non cliquée
        if (this.banniereTimeout) clearTimeout(this.banniereTimeout);
        this.banniereTimeout = setTimeout(() => {
          this.nouvelleBanniereVisible = false;
        }, 60000);
      }
    );
  }

  private _derniereNotifId: number | null = null;

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
        if (type === 'valide')  return this.isValidee(f);
        if (type === 'rejete')  return this.isRejete(f);
        return this.peutEtreTraitee(f);
      }).length;
    }, 0);
  }

  countDossierStatut(dossier: any, type: 'attente' | 'valide' | 'rejete'): number {
    return dossier.factures.filter((f: any) => {
      if (type === 'valide')  return this.isValidee(f);
      if (type === 'rejete')  return this.isRejete(f);
      return this.peutEtreTraitee(f);
    }).length;
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