import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { NotificationService, NotificationDTO } from '../../../core/services/notification.service';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="notif-wrapper">

      <!-- ── Cloche ── -->
      <button class="bell-btn" (click)="togglePanel()" [class.has-notifs]="count > 0">
        <span class="bell-icon">🔔</span>
        <span class="badge" *ngIf="count > 0">
          {{ count > 99 ? '99+' : count }}
        </span>
      </button>

      <!-- ── Panneau ── -->
      <div class="notif-panel" *ngIf="panelOuvert">

        <div class="panel-header">
          <h3>Notifications</h3>
          <button class="btn-tout-lire"
                  *ngIf="count > 0"
                  (click)="toutMarquerLu()">
            Tout marquer lu
          </button>
        </div>

        <!-- Vide -->
        <div class="panel-empty" *ngIf="notifications.length === 0">
          <span>🎉</span>
          <p>Aucune notification</p>
        </div>

        <!-- Liste -->
        <div class="notif-list" *ngIf="notifications.length > 0">
          <div
            *ngFor="let n of notifications"
            class="notif-item"
            [class.non-lue]="!n.lue"
            [ngClass]="typeClass(n.type)"
            (click)="clicNotif(n)">

            <div class="notif-icon">{{ typeIcon(n.type) }}</div>
            <div class="notif-body">
              <div class="notif-titre">{{ n.titre }}</div>
              <div class="notif-message">{{ n.message }}</div>
              <div class="notif-date">{{ formatDate(n.dateCreation) }}</div>
            </div>
            <div class="notif-dot" *ngIf="!n.lue"></div>
          </div>
        </div>

      </div>
    </div>
  `,
  styles: [`
    * { box-sizing: border-box; }

    .notif-wrapper { position: relative; display: inline-block; }

    /* ── Cloche ── */
    .bell-btn {
      position: relative;
      background: none;
      border: none;
      cursor: pointer;
      padding: 8px;
      border-radius: 10px;
      transition: background 0.2s;
      font-size: 1.3rem;
    }
    .bell-btn:hover { background: rgba(255,255,255,0.15); }
    .bell-btn.has-notifs .bell-icon { animation: ring 1.5s ease-in-out; }
    @keyframes ring {
      0%, 100% { transform: rotate(0); }
      10%, 30%, 50% { transform: rotate(-15deg); }
      20%, 40%       { transform: rotate(15deg); }
    }

    .badge {
      position: absolute;
      top: 2px; right: 2px;
      background: #dc2626;
      color: white;
      font-size: 0.62rem;
      font-weight: 800;
      min-width: 18px;
      height: 18px;
      border-radius: 9px;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0 4px;
      border: 2px solid #001f3f;
    }

    /* ── Panneau ── */
    .notif-panel {
      position: absolute;
      top: calc(100% + 8px);
      right: 0;
      width: 360px;
      background: white;
      border-radius: 16px;
      box-shadow: 0 12px 40px rgba(0,0,0,0.15);
      border: 1px solid #e2e8f0;
      z-index: 9999;
      overflow: hidden;
      animation: slideDown 0.2s ease;
    }
    @keyframes slideDown {
      from { opacity: 0; transform: translateY(-8px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 20px 12px;
      border-bottom: 1px solid #f1f5f9;
    }
    .panel-header h3 { margin: 0; font-size: 0.95rem; font-weight: 800; color: #1e293b; }
    .btn-tout-lire {
      font-size: 0.75rem;
      color: #4338ca;
      font-weight: 700;
      background: none;
      border: none;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 6px;
      transition: background 0.2s;
      font-family: inherit;
    }
    .btn-tout-lire:hover { background: #e0e7ff; }

    /* ── Vide ── */
    .panel-empty {
      text-align: center;
      padding: 40px 20px;
      color: #94a3b8;
      font-size: 0.88rem;
    }
    .panel-empty span { font-size: 2rem; display: block; margin-bottom: 8px; }

    /* ── Liste ── */
    .notif-list { max-height: 420px; overflow-y: auto; }

    .notif-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 14px 20px;
      border-bottom: 1px solid #f8fafc;
      cursor: pointer;
      transition: background 0.15s;
      position: relative;
    }
    .notif-item:last-child { border-bottom: none; }
    .notif-item:hover { background: #f8fafc; }
    .notif-item.non-lue { background: #fafbff; }

    /* Couleur par type */
    .notif-item.type-validation { border-left: 3px solid #3b82f6; }
    .notif-item.type-rejet      { border-left: 3px solid #dc2626; }
    .notif-item.type-mission    { border-left: 3px solid #8b5cf6; }
    .notif-item.type-resultat   { border-left: 3px solid #10b981; }
    .notif-item.type-audience   { border-left: 3px solid #f59e0b; }

    .notif-icon { font-size: 1.2rem; flex-shrink: 0; margin-top: 1px; }

    .notif-body { flex: 1; min-width: 0; }
    .notif-titre {
      font-size: 0.82rem;
      font-weight: 700;
      color: #1e293b;
      margin-bottom: 3px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .notif-message {
      font-size: 0.78rem;
      color: #64748b;
      line-height: 1.4;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .notif-date {
      font-size: 0.7rem;
      color: #94a3b8;
      margin-top: 4px;
      font-weight: 600;
    }

    .notif-dot {
      width: 8px;
      height: 8px;
      background: #4338ca;
      border-radius: 50%;
      flex-shrink: 0;
      margin-top: 6px;
    }
  `]
})
export class NotificationBellComponent implements OnInit, OnDestroy {

  notifications: NotificationDTO[] = [];
  count = 0;
  panelOuvert = false;

  private subs: Subscription[] = [];

  constructor(
    private notifService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // S'abonner à l'état global (le chargement initial est fait par AppComponent)
    this.subs.push(
      this.notifService.notifications$.subscribe(notifs => {
        this.notifications = notifs;
      }),
      this.notifService.count$.subscribe(c => {
        this.count = c;
      })
    );
  }

  togglePanel(): void {
    this.panelOuvert = !this.panelOuvert;
  }

  // Fermer le panneau si clic en dehors
  @HostListener('document:click', ['$event'])
  onClickOutside(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('app-notification-bell')) {
      this.panelOuvert = false;
    }
  }

  clicNotif(n: NotificationDTO): void {
    // Marquer comme lue
    if (!n.lue) {
      this.notifService.marquerCommeLue(n.id).subscribe();
      this.notifService.marquerLueLocalement(n.id);
    }
    // Naviguer vers l'URL d'action
    if (n.urlAction) {
      this.router.navigateByUrl(n.urlAction);
    }
    this.panelOuvert = false;
  }

  toutMarquerLu(): void {
    this.notifService.marquerToutesLues().subscribe();
    this.notifService.marquerToutesLuesLocalement();
  }

  // ── Helpers affichage ─────────────────────────────────────────

  typeIcon(type: string): string {
    const map: Record<string, string> = {
      'VALIDATION_FINANCIERE':    '💰',
      'VALIDATION_JURIDIQUE':     '⚖️',
      'VALIDATION_FINANCIERE_OK': '✅',
      'REJET_FINANCIER':          '❌',
      'VALIDATION_JURIDIQUE_OK':  '✅',
      'REJET_JURIDIQUE':          '❌',
      'NOUVELLE_MISSION':         '📋',
      'MISSION_MODIFIEE':         '✏️',
      'PV_SOUMIS':                '📄',
      'FACTURE_SOUMISE':          '🧾',
      'RESULTATS_SOUMIS':         '📬',
      'NOUVELLE_AUDIENCE':        '🗓️',
      'JUGEMENT_RENDU':           '🏛️',
    };
    return map[type] ?? '🔔';
  }

  typeClass(type: string): string {
    if (['VALIDATION_FINANCIERE', 'VALIDATION_JURIDIQUE',
         'VALIDATION_FINANCIERE_OK', 'VALIDATION_JURIDIQUE_OK'].includes(type))
      return 'type-validation';
    if (['REJET_FINANCIER', 'REJET_JURIDIQUE'].includes(type))
      return 'type-rejet';
    if (['NOUVELLE_MISSION', 'MISSION_MODIFIEE'].includes(type))
      return 'type-mission';
    if (['PV_SOUMIS', 'FACTURE_SOUMISE', 'RESULTATS_SOUMIS'].includes(type))
      return 'type-resultat';
    if (['NOUVELLE_AUDIENCE', 'JUGEMENT_RENDU'].includes(type))
      return 'type-audience';
    return '';
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now  = new Date();
    const diff = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diff < 60)   return 'À l\'instant';
    if (diff < 3600) return `Il y a ${Math.floor(diff / 60)} min`;
    if (diff < 86400) return `Il y a ${Math.floor(diff / 3600)} h`;
    return date.toLocaleDateString('fr-TN', {
      day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
    });
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }
}