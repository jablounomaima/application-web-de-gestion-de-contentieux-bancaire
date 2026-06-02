import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { NotificationService, NotificationDTO } from '../../../core/services/notification.service';

@Component({
  selector: 'app-resultats-prestataires',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page">

      <!-- Header -->
      <div class="page-header">
        <button class="btn-back" (click)="retour()">← Retour</button>
        <div>
          <h1>📊 Résultats Prestataires</h1>
          <p class="sub" *ngIf="numeroDossier">
            Dossier : <strong>{{ numeroDossier }}</strong> — {{ libelle }}
          </p>
        </div>
      </div>

      <!-- Loader -->
      <div class="loading" *ngIf="loading">
        <div class="spinner"></div>
        <p>Chargement...</p>
      </div>

      <div class="error-box" *ngIf="erreur">{{ erreur }}</div>

      <div *ngIf="!loading && !erreur">

        <!-- Vide -->
        <div class="empty" *ngIf="resultats.length === 0">
          Aucune mission assignée pour ce dossier.
        </div>

        <!-- Cards missions -->
        <div class="mission-card"
             *ngFor="let r of resultats"
             [id]="'mission-' + r.missionId">

          <!-- Header -->
          <div class="mission-header" [ngClass]="getStatutClass(r.statut)">
            <div class="mission-title">
              <span class="numero">{{ r.numeroMission }}</span>
              <span class="type-badge">{{ r.typePrestation }}</span>
            </div>
            <span class="statut-label">{{ formatStatut(r.statut) }}</span>
          </div>

          <div class="mission-body">

            <!-- Prestataire -->
            <div class="section" *ngIf="r.prestataire">
              <h3>👤 Prestataire</h3>
              <div class="info-row">
                <span>{{ r.prestataire.nom }} {{ r.prestataire.prenom }}</span>
                <span class="meta">
                  {{ r.prestataire.type }} · {{ r.prestataire.specialite }}
                </span>
              </div>
            </div>

            <!-- PV -->
            <div class="section">
              <h3>
                📄 Procès-Verbal
                <span class="badge-ok"  *ngIf="r.pvTexte">✅ Soumis</span>
                <span class="badge-non" *ngIf="!r.pvTexte">⏳ Non soumis</span>
              </h3>
              <div class="text-box" *ngIf="r.pvTexte">{{ r.pvTexte }}</div>
              <div class="meta" *ngIf="r.dateValidationPv">
                📅 {{ r.dateValidationPv | date:'dd/MM/yyyy HH:mm' }}
              </div>
            </div>

            <!-- Commentaire prestataire -->
            <div class="section" *ngIf="r.commentaire">
              <h3>💬 Commentaire prestataire</h3>
              <div class="text-box comment">{{ r.commentaire }}</div>
              <div class="meta">
                Par {{ r.soumisePar }} ·
                {{ r.dateSoumission | date:'dd/MM/yyyy HH:mm' }}
              </div>
            </div>

            <!-- Facture -->
            <div class="section">
              <h3>
                💳 Facture
                <span class="badge-ok"  *ngIf="r.factureRef">✅ Soumise</span>
                <span class="badge-non" *ngIf="!r.factureRef">⏳ Non soumise</span>
              </h3>

              <div class="facture-row" *ngIf="r.factureRef">
                <div class="facture-item">
                  <span class="flabel">Référence</span>
                  <span class="fval ref">{{ r.factureRef }}</span>
                </div>
                <div class="facture-item">
                  <span class="flabel">Montant HT</span>
                  <span class="fval montant">
                    {{ r.montantFacture | number:'1.3-3' }} TND
                  </span>
                </div>
                <div class="facture-item">
                  <span class="flabel">TVA 19%</span>
                  <span class="fval">
                    {{ r.montantFacture * 0.19 | number:'1.3-3' }} TND
                  </span>
                </div>
                <div class="facture-item">
                  <span class="flabel">Total TTC</span>
                  <span class="fval montant">
                    {{ r.montantFacture * 1.19 | number:'1.3-3' }} TND
                  </span>
                </div>
              </div>

              <!-- ✅ Statut validation financière -->
              <div class="validation-financiere" *ngIf="r.factureRef">

                <!-- En attente -->
                <div class="vf-attente"
                     *ngIf="r.factureValide === null || r.factureValide === undefined">
                  <span class="vf-icon">⏳</span>
                  <div>
                    <div class="vf-titre">En attente de validation financière</div>
                    <div class="vf-sub">
                      La facture doit être validée par le validateur financier
                      avant que vous puissiez valider la mission.
                    </div>
                  </div>
                </div>

                <!-- Validée par le financier -->
                <div class="vf-validee" *ngIf="r.factureValideeParFinancier">
                  <span class="vf-icon">✅</span>
                  <div>
                    <div class="vf-titre">
                      Facture validée par le validateur financier
                    </div>
                    <div class="vf-sub" *ngIf="r.valideParValidateur">
                      Par : <strong>{{ r.valideParValidateur }}</strong>
                      <span *ngIf="r.dateValidationFacture">
                        · {{ r.dateValidationFacture | date:'dd/MM/yyyy HH:mm' }}
                      </span>
                    </div>
                    <div class="vf-action-hint">
                      ↓ Vous pouvez maintenant valider la mission ci-dessous.
                    </div>
                  </div>
                </div>

                <!-- Rejetée par le financier -->
                <div class="vf-rejetee" *ngIf="r.factureRejeteeParFinancier">
                  <span class="vf-icon">❌</span>
                  <div>
                    <div class="vf-titre">
                      Facture rejetée par le validateur financier
                    </div>
                    <div class="vf-sub" *ngIf="r.commentaireValidateur">
                      Motif : <em>{{ r.commentaireValidateur }}</em>
                    </div>
                    <div class="vf-action-hint">
                      Le prestataire doit soumettre une nouvelle facture.
                    </div>
                  </div>
                </div>

              </div>
            </div>

            <!-- Fichiers -->
            <div class="section" *ngIf="r.fichiers?.length > 0">
              <h3>📎 Documents ({{ r.fichiers.length }})</h3>
              <div class="fichier-item" *ngFor="let f of r.fichiers">
                <div class="fichier-left">
                  <span class="fichier-icon">{{ icone(f.typeMime) }}</span>
                  <div>
                    <div class="fichier-nom">{{ f.nomFichierOriginal }}</div>
                    <div class="meta">
                      {{ formatTaille(f.tailleFichier) }} ·
                      {{ f.dateUpload | date:'dd/MM/yyyy' }}
                    </div>
                  </div>
                </div>
                <button class="btn-dl"
                        (click)="telecharger(f.id, f.nomFichierOriginal)"
                        [disabled]="dlEnCours[f.id]">
                  {{ dlEnCours[f.id] ? '⏳' : '⬇️' }} Télécharger
                </button>
              </div>
            </div>

            <!-- ✅ Actions agent — seulement si facture validée financièrement -->
            <div class="actions-section" *ngIf="r.peutEtreValideeParAgent">
              <div class="actions-title">
                ✅ Facture validée — vous pouvez valider la mission
              </div>
              <div class="actions">
                <button class="btn-valider"
                        (click)="valider(r.missionId)"
                        [disabled]="actionEnCours[r.missionId]">
                  {{ actionEnCours[r.missionId] ? '...' : '✅ Valider la mission' }}
                </button>
                <button class="btn-rejeter"
                        (click)="ouvrirRejet(r)"
                        [disabled]="actionEnCours[r.missionId]">
                  ❌ Rejeter la mission
                </button>
              </div>
            </div>

            <!-- Commentaire agent -->
            <div class="agent-comment" *ngIf="r.commentaireAgent">
              💬 Commentaire agent : {{ r.commentaireAgent }}
            </div>

          </div>
        </div>
      </div>

    </div>

    <!-- ══ Modal rejet ══ -->
    <div class="modal-overlay" *ngIf="modalRejetVisible"
         (click)="fermerRejet()">
      <div class="modal-box" (click)="$event.stopPropagation()">
        <h3>❌ Rejeter la mission</h3>
        <p>Mission : <strong>{{ missionEnCours?.numeroMission }}</strong></p>
        <label class="modal-label">Motif du rejet *</label>
        <textarea [(ngModel)]="motifRejet"
                  rows="4"
                  class="modal-textarea"
                  placeholder="Expliquez la raison du rejet...">
        </textarea>
        <div class="modal-actions">
          <button class="btn-annuler" (click)="fermerRejet()">Annuler</button>
          <button class="btn-confirmer-rejet"
                  (click)="confirmerRejet()"
                  [disabled]="!motifRejet.trim()">
            Confirmer le rejet
          </button>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <div class="toast"
         [ngClass]="{
           show:    toastVisible,
           success: toastType === 'success',
           error:   toastType === 'error'
         }">
      {{ toastMessage }}
    </div>
  `,
  styles: [`
    * { box-sizing: border-box; }
    .page { font-family: Inter, sans-serif; padding: 30px; max-width: 1000px; margin: 0 auto; }

    /* ── Header ── */
    .page-header { display: flex; align-items: flex-start; gap: 20px; margin-bottom: 30px; }
    .btn-back { padding: 10px 18px; background: #f0f0f0; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; color: #555; transition: background 0.2s; }
    .btn-back:hover { background: #e0e0e0; }
    h1 { margin: 0; font-size: 1.6rem; color: #1a237e; }
    .sub { margin: 4px 0 0; color: #777; font-size: .9rem; }

    /* ── Loader / Error / Empty ── */
    .loading { display: flex; flex-direction: column; align-items: center; padding: 60px; gap: 16px; color: #999; }
    .spinner { width: 36px; height: 36px; border: 4px solid #eee; border-top-color: #1a237e; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .error-box { background: #fdecea; color: #c62828; padding: 14px; border-radius: 8px; border-left: 4px solid #c62828; }
    .empty { text-align: center; color: #bbb; padding: 60px; font-style: italic; }

    /* ── Mission card ── */
    .mission-card { background: white; border-radius: 14px; box-shadow: 0 4px 20px rgba(0,0,0,.07); margin-bottom: 24px; overflow: hidden; border: 2px solid transparent; }
    .mission-header { padding: 16px 24px; display: flex; justify-content: space-between; align-items: center; }
    .mission-header.s-assignee      { background: linear-gradient(135deg,#f57f17,#f9a825); }
    .mission-header.s-en-cours      { background: linear-gradient(135deg,#1565c0,#1976d2); }
    .mission-header.s-pv-soumis     { background: linear-gradient(135deg,#00695c,#00897b); }
    .mission-header.s-facture       { background: linear-gradient(135deg,#2e7d32,#388e3c); }
    .mission-header.s-facture-val   { background: linear-gradient(135deg,#1b5e20,#2e7d32); }
    .mission-header.s-facture-rej   { background: linear-gradient(135deg,#b71c1c,#c62828); }
    .mission-header.s-terminee      { background: linear-gradient(135deg,#424242,#616161); }
    .mission-header.s-rejetee       { background: linear-gradient(135deg,#b71c1c,#c62828); }
    .mission-title { display: flex; align-items: center; gap: 12px; }
    .numero { color: white; font-weight: 700; font-size: 1rem; }
    .type-badge { background: rgba(255,255,255,.25); color: white; padding: 3px 10px; border-radius: 20px; font-size: .8rem; }
    .statut-label { color: rgba(255,255,255,.9); font-size: .85rem; font-weight: 600; }

    .mission-body { padding: 24px; display: flex; flex-direction: column; gap: 20px; }

    /* ── Sections ── */
    .section h3 { margin: 0 0 10px; font-size: .95rem; color: #333; display: flex; align-items: center; gap: 8px; }
    .info-row { display: flex; flex-direction: column; gap: 2px; }
    .info-row span:first-child { font-weight: 600; color: #333; }
    .meta { font-size: .8rem; color: #999; margin-top: 4px; }
    .text-box { background: #f8f9fa; border-left: 4px solid #1a237e; border-radius: 8px; padding: 14px; white-space: pre-wrap; font-size: .9rem; color: #333; line-height: 1.6; }
    .text-box.comment { border-left-color: #f57c00; }
    .badge-ok  { background: #e8f5e9; color: #2e7d32; padding: 2px 10px; border-radius: 20px; font-size: .78rem; }
    .badge-non { background: #fff3e0; color: #e65100; padding: 2px 10px; border-radius: 20px; font-size: .78rem; }

    /* ── Facture ── */
    .facture-row { display: flex; gap: 30px; margin-bottom: 12px; flex-wrap: wrap; }
    .facture-item { display: flex; flex-direction: column; gap: 4px; }
    .flabel { font-size: .78rem; color: #999; text-transform: uppercase; }
    .fval { font-size: 1rem; font-weight: 700; color: #333; }
    .fval.ref { color: #1a237e; }
    .fval.montant { color: #2e7d32; font-size: 1.1rem; }

    /* ── Validation financière ── */
    .validation-financiere { margin-top: 12px; }
    .vf-attente,
    .vf-validee,
    .vf-rejetee {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 14px 16px;
      border-radius: 10px;
      margin-top: 8px;
    }
    .vf-attente { background: #fffbeb; border: 1px solid #fde68a; }
    .vf-validee { background: #f0fdf4; border: 1px solid #86efac; }
    .vf-rejetee { background: #fef2f2; border: 1px solid #fecaca; }
    .vf-icon { font-size: 1.4rem; flex-shrink: 0; margin-top: 2px; }
    .vf-titre { font-weight: 700; font-size: .9rem; color: #1e293b; }
    .vf-sub { font-size: .82rem; color: #64748b; margin-top: 4px; }
    .vf-action-hint { font-size: .8rem; font-weight: 600; margin-top: 6px; color: #15803d; }
    .vf-rejetee .vf-action-hint { color: #dc2626; }

    /* ── Fichiers ── */
    .fichier-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
    .fichier-item:last-child { border-bottom: none; }
    .fichier-left { display: flex; align-items: center; gap: 12px; }
    .fichier-icon { font-size: 1.6rem; }
    .fichier-nom  { font-weight: 600; color: #333; font-size: .9rem; }
    .btn-dl { padding: 7px 14px; background: #e3f2fd; color: #0d47a1; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: .82rem; }
    .btn-dl:hover    { background: #bbdefb; }
    .btn-dl:disabled { opacity: .6; cursor: not-allowed; }

    /* ── Actions agent ── */
    .actions-section {
      background: #f0fdf4;
      border: 2px solid #86efac;
      border-radius: 12px;
      padding: 16px 20px;
    }
    .actions-title {
      font-weight: 700;
      color: #15803d;
      font-size: .9rem;
      margin-bottom: 12px;
    }
    .actions { display: flex; gap: 12px; }
    .btn-valider { padding: 11px 24px; background: #15803d; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: 700; font-size: .9rem; transition: all 0.2s; }
    .btn-valider:hover:not(:disabled) { background: #166534; transform: translateY(-1px); }
    .btn-rejeter { padding: 11px 24px; background: #dc2626; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: 700; font-size: .9rem; transition: all 0.2s; }
    .btn-rejeter:hover:not(:disabled) { background: #b91c1c; transform: translateY(-1px); }
    .btn-valider:disabled,
    .btn-rejeter:disabled { opacity: .6; cursor: not-allowed; transform: none; }

    /* ── Commentaire agent ── */
    .agent-comment { background: #fff8e1; border-left: 4px solid #f9a825; padding: 10px 14px; border-radius: 6px; font-size: .88rem; color: #555; }

    /* ── Modal ── */
    .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
    .modal-box { background: white; border-radius: 16px; padding: 32px; width: 480px; box-shadow: 0 20px 60px rgba(0,0,0,0.2); }
    .modal-box h3 { margin: 0 0 8px; font-size: 1.2rem; color: #1e293b; }
    .modal-box p  { color: #64748b; font-size: .9rem; margin-bottom: 16px; }
    .modal-label  { display: block; font-size: .8rem; font-weight: 700; color: #475569; text-transform: uppercase; letter-spacing: .5px; margin-bottom: 8px; }
    .modal-textarea { width: 100%; padding: 12px; border: 1.5px solid #e2e8f0; border-radius: 8px; font-family: inherit; font-size: .9rem; resize: vertical; outline: none; }
    .modal-textarea:focus { border-color: #dc2626; }
    .modal-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 16px; }
    .btn-annuler { background: #f1f5f9; color: #64748b; border: none; padding: 10px 20px; border-radius: 8px; font-weight: 700; cursor: pointer; }
    .btn-confirmer-rejet { background: #dc2626; color: white; border: none; padding: 10px 20px; border-radius: 8px; font-weight: 700; cursor: pointer; }
    .btn-confirmer-rejet:disabled { opacity: .5; cursor: not-allowed; }

    /* ── Toast ── */
    .toast { position: fixed; bottom: 30px; right: 30px; padding: 14px 24px; border-radius: 12px; font-weight: 700; font-size: .9rem; opacity: 0; transform: translateY(20px); transition: all 0.3s; z-index: 9999; pointer-events: none; min-width: 260px; text-align: center; }
    .toast.show    { opacity: 1; transform: translateY(0); }
    .toast.success { background: #059669; color: white; }
    .toast.error   { background: #dc2626; color: white; }

    /* ── Mission highlight (scroll cible) ── */
    @keyframes highlight-pulse {
      0%   { box-shadow: 0 0 0 4px #f59e0b66; border: 2px solid #f59e0b; }
      60%  { box-shadow: 0 0 0 3px #f59e0b33; border: 2px solid #fbbf24; }
      100% { box-shadow: 0 4px 20px rgba(0,0,0,.07); border: 2px solid transparent; }
    }
    .mission-highlight {
      animation: highlight-pulse 4s ease-out forwards;
    }
  `]
})
export class ResultatsPrestatairesComponent implements OnInit, OnDestroy {

  dossierId!:    number;
  numeroDossier = '';
  libelle       = '';
  resultats:    any[] = [];
  loading       = true;
  erreur        = '';

  // ── (1) Propriété missionCibleId ──
  missionCibleId: number | null = null;

  dlEnCours:     Record<number, boolean> = {};
  actionEnCours: Record<number, boolean> = {};

  // Modal rejet
  modalRejetVisible = false;
  missionEnCours:  any = null;
  motifRejet       = '';

  // Toast
  toastVisible = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

  private api = environment.apiUrl;

  private notifSub: Subscription | null = null;
  private _derniereNotifId: number | null = null;

  constructor(
    private route:        ActivatedRoute,
    private router:       Router,
    private http:         HttpClient,
    private notifService: NotificationService
  ) {}

  // ── (2) ngOnInit : lire missionId depuis l'URL avant charger() ──
  ngOnInit(): void {
    this.dossierId = Number(this.route.snapshot.paramMap.get('dossierId'));
    const missionIdParam = this.route.snapshot.queryParamMap.get('missionId');
    if (missionIdParam) {
      this.missionCibleId = Number(missionIdParam);
    }
    this.charger();
    this._ecouterNotifications();
  }

  ngOnDestroy(): void {
    this.notifSub?.unsubscribe();
  }

  // ── Écoute WebSocket — mise à jour en temps réel après décision financière ──
  private _ecouterNotifications(): void {
    this.notifSub = this.notifService.notifications$.subscribe(
      (notifs: NotificationDTO[]) => {
        if (!notifs.length) return;
        const derniere = notifs[0];
        if (this._derniereNotifId === derniere.id) return;
        this._derniereNotifId = derniere.id;

        const typesConcernes = [
          'VALIDATION_FINANCIERE_OK', 'REJET_FINANCIER',
          'MISSION_CLOTUREE', 'MISSION_REJETEE'
        ];
        if (typesConcernes.includes(derniere.type)) {
          this.showToast('🔄 Mise à jour disponible — rechargement automatique…', 'success');
          setTimeout(() => this.charger(), 800);
        }
      }
    );
  }

  // ════════════════════════════════════════
  // Chargement
  // ════════════════════════════════════════

  charger(): void {
    this.loading = true;
    this.http.get<any>(
      `${this.api}/api/agent/dossiers/${this.dossierId}/resultats-prestataires`
    ).subscribe({
      // ── (3) Scroll vers la mission ciblée après chargement ──
      next: (data) => {
        this.numeroDossier = data.numeroDossier;
        this.libelle       = data.libelle;
        this.resultats     = data.resultats;
        this.loading       = false;
        if (this.missionCibleId) {
          setTimeout(() => this._scrollVersMission(this.missionCibleId!), 400);
        }
      },
      error: (err) => {
        this.erreur  = err?.error?.error || 'Erreur chargement';
        this.loading = false;
      }
    });
  }

  // ── (4) Méthode privée de scroll + highlight ──
  private _scrollVersMission(missionId: number): void {
    const el = document.getElementById('mission-' + missionId);
    if (!el) return;
    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    el.classList.add('mission-highlight');
    setTimeout(() => el.classList.remove('mission-highlight'), 4000);
  }

  // ════════════════════════════════════════
  // Valider mission
  // ════════════════════════════════════════

  valider(missionId: number): void {
    this.actionEnCours[missionId] = true;
    this.http.post(
      `${this.api}/api/agent/missions/${missionId}/valider`,
      { commentaire: '' }
    ).subscribe({
      next: () => {
        this.actionEnCours[missionId] = false;
        this.showToast('✅ Mission validée avec succès !', 'success');
        setTimeout(() => this.charger(), 500);
      },
      error: (e) => {
        this.actionEnCours[missionId] = false;
        this.showToast(e?.error?.error || 'Erreur validation.', 'error');
      }
    });
  }

  // ════════════════════════════════════════
  // Rejeter mission
  // ════════════════════════════════════════

  ouvrirRejet(mission: any): void {
    this.missionEnCours   = mission;
    this.motifRejet       = '';
    this.modalRejetVisible = true;
  }

  fermerRejet(): void {
    this.modalRejetVisible = false;
    this.missionEnCours    = null;
  }

  confirmerRejet(): void {
    if (!this.motifRejet.trim() || !this.missionEnCours) return;
    const missionId = this.missionEnCours.missionId;
    this.actionEnCours[missionId] = true;
    this.modalRejetVisible = false;

    this.http.post(
      `${this.api}/api/agent/missions/${missionId}/rejeter`,
      { commentaire: this.motifRejet }
    ).subscribe({
      next: () => {
        this.actionEnCours[missionId] = false;
        this.showToast('❌ Mission rejetée.', 'success');
        setTimeout(() => this.charger(), 500);
      },
      error: (e) => {
        this.actionEnCours[missionId] = false;
        this.showToast(e?.error?.error || 'Erreur rejet.', 'error');
      }
    });
  }

  // ════════════════════════════════════════
  // Téléchargement
  // ════════════════════════════════════════

  telecharger(fichierId: number, nomOriginal: string): void {
    this.dlEnCours[fichierId] = true;
    this.http.get(
      `${this.api}/api/prestataire/missions/fichier/id/${fichierId}`,
      { responseType: 'blob' }
    ).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href     = url;
        a.download = nomOriginal;
        a.click();
        URL.revokeObjectURL(url);
        this.dlEnCours[fichierId] = false;
      },
      error: () => {
        this.dlEnCours[fichierId] = false;
        this.showToast('Erreur téléchargement.', 'error');
      }
    });
  }

  // ════════════════════════════════════════
  // Helpers
  // ════════════════════════════════════════

  retour(): void {
    this.router.navigate(['/agent/dossiers', this.dossierId]);
  }

  getStatutClass(statut: string): string {
    const map: Record<string, string> = {
      ASSIGNEE:         's-assignee',
      EN_COURS:         's-en-cours',
      PV_SOUMIS:        's-pv-soumis',
      FACTURE_SOUMISE:  's-facture',
      FACTURE_VALIDEE:  's-facture-val',
      FACTURE_REJETEE:  's-facture-rej',
      TERMINEE:         's-terminee',
      REJETEE:          's-rejetee',
      VALIDEE_AGENT:    's-terminee'
    };
    return map[statut] ?? '';
  }

  formatStatut(s: string): string {
    const map: Record<string, string> = {
      ASSIGNEE:         'Assignée',
      EN_COURS:         'En cours',
      PV_SOUMIS:        'PV soumis',
      FACTURE_SOUMISE:  'Facture soumise',
      FACTURE_VALIDEE:  'Facture validée ✅',
      FACTURE_REJETEE:  'Facture rejetée ❌',
      TERMINEE:         'Terminée',
      REJETEE:          'Rejetée',
      VALIDEE_AGENT:    'Validée par l\'agent'
    };
    return map[s] ?? s;
  }

  icone(mime: string): string {
    if (!mime)                          return '📄';
    if (mime === 'application/pdf')     return '📕';
    if (mime.startsWith('image/'))      return '🖼️';
    if (mime.includes('word'))          return '📝';
    if (mime.includes('sheet') || mime.includes('excel')) return '📊';
    return '📄';
  }

  formatTaille(t: number): string {
    if (!t)             return '0 B';
    if (t < 1024)       return t + ' B';
    if (t < 1024*1024)  return (t / 1024).toFixed(1) + ' KB';
    return (t / (1024*1024)).toFixed(1) + ' MB';
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    this.toastMessage = message;
    this.toastType    = type;
    this.toastVisible = true;
    setTimeout(() => this.toastVisible = false, 3500);
  }
}