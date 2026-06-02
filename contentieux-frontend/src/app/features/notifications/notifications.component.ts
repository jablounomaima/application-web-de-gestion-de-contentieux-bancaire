import {
  Component, OnInit, OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { NotificationService, NotificationDTO } from '../../core/services/notification.service';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="notif-wrapper">

      <!-- Overlay pour fermer le panneau (derrière le panneau) -->
      <div *ngIf="ouvert"
           class="notif-overlay"
           (click)="ouvert = false">
      </div>

      <!-- Cloche + badge -->
      <button class="notif-bell" (click)="togglePanel()">
        🔔
        <span class="badge" *ngIf="nonLues > 0">{{ nonLues > 9 ? '9+' : nonLues }}</span>
      </button>

      <!-- Toast temps réel -->
      <div class="notif-toast" *ngIf="toastVisible"
           [class]="'notif-toast notif-toast--' + toastType"
           (click)="$event.stopPropagation(); naviguerVersToast()">
        <span class="toast-icon">{{ toastIcon }}</span>
        <div class="toast-body">
          <div class="toast-title">{{ toastTitre }}</div>
          <div class="toast-msg">{{ toastMessage }}</div>
        </div>
        <button class="toast-close" (click)="$event.stopPropagation(); fermerToast()">×</button>
      </div>

      <!-- Panneau déroulant (au-dessus de l'overlay) -->
      <div class="notif-panel" *ngIf="ouvert">

        <div class="notif-header">
          <span>Notifications <span *ngIf="nonLues > 0">({{ nonLues }} non lues)</span></span>
          <button class="btn-tout-lire" *ngIf="nonLues > 0" (click)="toutMarquerLu()">
            Tout marquer lu
          </button>
        </div>

        <div class="notif-empty" *ngIf="notifications.length === 0">
          <span>🔕</span><br>Aucune notification
        </div>

        <ul class="notif-list">
          <li *ngFor="let n of notifications"
              [class.non-lue]="!n.lue"
              (click)="marquerEtNaviguer(n)">
            <div class="notif-item-header">
              <strong>{{ n.titre }}</strong>
              <span class="notif-dot" *ngIf="!n.lue"></span>
            </div>
            <p>{{ n.message }}</p>
            <small>{{ n.dateCreation | date:'dd/MM/yyyy HH:mm' }}</small>
          </li>
        </ul>

        <div class="notif-footer" *ngIf="notifications.length > 0">
          <button class="btn-tout-lire" (click)="toutMarquerLu()">Tout marquer lu</button>
        </div>

      </div>
    </div>
  `,
  styles: [`
    .notif-wrapper  { position: relative; display: inline-block; }

    /* Overlay transparent qui couvre toute la page derrière le panneau */
    .notif-overlay {
      position: fixed;
      top: 0; left: 0;
      width: 100vw; height: 100vh;
      z-index: 999;
      background: transparent;
    }

    .notif-bell {
      background: none; border: none; font-size: 1.4rem;
      cursor: pointer; position: relative; padding: 4px;
      border-radius: 8px; transition: background .15s;
    }
    .notif-bell:hover { background: rgba(255,255,255,.15); }

    .badge {
      position: absolute; top: -4px; right: -6px;
      background: #e53e3e; color: #fff; border-radius: 999px;
      font-size: 0.62rem; font-weight: 700;
      padding: 2px 5px; min-width: 18px; text-align: center;
      line-height: 1.2;
    }

    .notif-toast {
      position: fixed; top: 72px; right: 20px;
      width: 340px; z-index: 10000;
      background: #fff; border-radius: 10px;
      box-shadow: 0 8px 24px rgba(0,0,0,.14);
      border-left: 4px solid #2563eb;
      display: flex; align-items: flex-start; gap: 10px;
      padding: 14px; cursor: pointer;
      animation: toastIn .3s ease;
    }
    .notif-toast--success { border-left-color: #059669; }
    .notif-toast--error   { border-left-color: #dc2626; }
    .notif-toast--warning { border-left-color: #f59e0b; }

    .toast-icon  { font-size: 20px; flex-shrink: 0; }
    .toast-body  { flex: 1; overflow: hidden; }
    .toast-title { font-weight: 700; font-size: 13px; color: #0f172a; margin-bottom: 3px; }
    .toast-msg   { font-size: 12px; color: #475569; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .toast-close {
      background: none; border: none; cursor: pointer;
      color: #94a3b8; font-size: 18px; padding: 0;
      line-height: 1; flex-shrink: 0;
    }
    @keyframes toastIn {
      from { opacity: 0; transform: translateX(30px); }
      to   { opacity: 1; transform: translateX(0); }
    }

    .notif-panel {
      position: absolute; top: calc(100% + 8px); right: 0;
      width: 340px; max-height: 440px; overflow-y: auto;
      background: #fff; border: 1px solid #e2e8f0;
      border-radius: 10px; box-shadow: 0 8px 24px rgba(0,0,0,.12);
      z-index: 1000; /* au-dessus de l'overlay (999) */
    }

    .notif-header {
      display: flex; justify-content: space-between; align-items: center;
      padding: 12px 14px; border-bottom: 1px solid #e2e8f0;
      font-size: 0.85rem; font-weight: 700; color: #1e293b;
      position: sticky; top: 0; background: #fff; z-index: 1;
    }

    .btn-tout-lire {
      background: none; border: none; color: #3182ce;
      font-size: 0.78rem; cursor: pointer; font-weight: 600;
    }
    .btn-tout-lire:hover { text-decoration: underline; }

    .notif-empty {
      padding: 28px 20px; text-align: center;
      color: #a0aec0; font-size: 0.85rem; line-height: 2;
    }

    .notif-list { list-style: none; margin: 0; padding: 0; }

    .notif-list li {
      padding: 11px 14px; border-bottom: 1px solid #f1f5f9;
      cursor: pointer; transition: background .15s;
    }
    .notif-list li:last-child { border-bottom: none; }
    .notif-list li:hover    { background: #f8fafc; }
    .notif-list li.non-lue  { background: #eff6ff; border-left: 3px solid #3b82f6; }

    .notif-item-header {
      display: flex; justify-content: space-between; align-items: center;
      margin-bottom: 3px;
    }
    .notif-item-header strong { font-size: 0.84rem; color: #1e293b; }
    .notif-dot {
      width: 8px; height: 8px; border-radius: 50%;
      background: #3b82f6; flex-shrink: 0;
    }

    .notif-list p     { font-size: 0.79rem; color: #64748b; margin: 0 0 4px; line-height: 1.4; }
    .notif-list small { font-size: 0.72rem; color: #94a3b8; }

    .notif-footer {
      padding: 10px 14px; border-top: 1px solid #e2e8f0;
      text-align: right; background: #f8fafc;
      position: sticky; bottom: 0;
    }
  `]
})
export class NotificationsComponent implements OnInit, OnDestroy {

  notifications: NotificationDTO[] = [];
  ouvert   = false;
  nonLues  = 0;

  toastVisible = false;
  toastTitre   = '';
  toastMessage = '';
  toastType    = 'info';
  toastIcon    = '🔔';
  toastUrlAction: string | undefined;
  private toastTimer: any;

  private subs: Subscription[] = [];
  private prevCount = 0;

  constructor(
    private notifService: NotificationService,
    private router:       Router
  ) {}

  ngOnInit(): void {
    this.subs.push(
      this.notifService.notifications$.subscribe(list => {
        console.log('📋 notifications reçues:', list.length, list);
        this.nonLues = list.filter(n => !n.lue).length;
        if (list.length > 0 && list.length > this.prevCount) {
          const derniere = list[0];
          if (!derniere.lue) this._afficherToast(derniere);
        }
        this.prevCount     = list.length;
        this.notifications = list;
      })
    );
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    clearTimeout(this.toastTimer);
  }

  togglePanel(): void {
    this.ouvert = !this.ouvert;
    if (this.ouvert) {
      this.notifService.chargerNotifications();
    }
  }

  marquerEtNaviguer(n: NotificationDTO): void {
    this.ouvert = false;

    if (!n.lue) {
      this.notifService.marquerCommeLue(n.id).subscribe(() => {
        this.notifService.marquerLueLocalement(n.id);
      });
    }

    if (n.urlAction) {
      const [path, queryString] = n.urlAction.split('?');
      const queryParams: Record<string, string> = {};
      if (queryString) {
        queryString.split('&').forEach(pair => {
          const [key, val] = pair.split('=');
          if (key) queryParams[key] = val ?? '';
        });
      }

      const dossierId = queryParams['dossierId'] ? Number(queryParams['dossierId']) : null;
      if (dossierId) {
        this.notifService.signalerDossierCible(dossierId);
      }

      this.router.navigate([path], {
        queryParams: Object.keys(queryParams).length ? queryParams : undefined
      });
    }
  }

  toutMarquerLu(): void {
    this.notifService.marquerToutesLues().subscribe(() => {
      this.notifService.marquerToutesLuesLocalement();
    });
  }

  private _afficherToast(n: NotificationDTO): void {
    clearTimeout(this.toastTimer);
    this.toastTitre     = n.titre;
    this.toastMessage   = n.message;
    this.toastUrlAction = n.urlAction;
    this.toastType      = this._resolveToastType(n.type);
    this.toastIcon      = this._resolveToastIcon(n.type);
    this.toastVisible   = true;
    this.toastTimer = setTimeout(() => this.fermerToast(), 7000);
  }

  fermerToast(): void {
    this.toastVisible = false;
    clearTimeout(this.toastTimer);
  }

  naviguerVersToast(): void {
    this.fermerToast();
    if (this.toastUrlAction) {
      const [path, queryString] = this.toastUrlAction.split('?');
      const queryParams: Record<string, string> = {};
      if (queryString) {
        queryString.split('&').forEach(pair => {
          const [key, val] = pair.split('=');
          if (key) queryParams[key] = val ?? '';
        });
      }

      const dossierId = queryParams['dossierId'] ? Number(queryParams['dossierId']) : null;
      if (dossierId) {
        this.notifService.signalerDossierCible(dossierId);
      }

      this.router.navigate([path], {
        queryParams: Object.keys(queryParams).length ? queryParams : undefined
      });
    }
  }

  private _resolveToastType(type: string): string {
    if (!type) return 'info';
    if (type.includes('OK') || type.includes('CLOTUR') || type.includes('TERMINEE')) return 'success';
    if (type.includes('REJET') || type.includes('REJETEE'))                           return 'error';
    if (type.includes('SOUMIS') || type.includes('RESOUMISSION'))                    return 'warning';
    return 'info';
  }

  private _resolveToastIcon(type: string): string {
    if (!type) return '🔔';
    if (type.includes('CLOTUR') || type.includes('TERMINEE')) return '🏁';
    if (type.includes('REJET'))    return '❌';
    if (type.includes('OK'))       return '✅';
    if (type.includes('SOUMIS'))   return '📋';
    if (type.includes('MISSION'))  return '📌';
    if (type.includes('AUDIENCE')) return '🗓️';
    return '🔔';
  }
}