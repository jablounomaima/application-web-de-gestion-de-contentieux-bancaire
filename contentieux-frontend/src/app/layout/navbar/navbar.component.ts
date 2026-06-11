import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { KeycloakService } from 'keycloak-angular';
import { NotificationService, NotificationDTO } from '../../core/services/notification.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],  // ← Plus besoin de NotificationBellComponent
  template: `
    <nav class="navbar">
      <div class="nav-brand">
        <a routerLink="/">🏛️ Contentieux</a>
      </div>
      <div class="nav-links">
        <a routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
        <a routerLink="/dossiers" routerLinkActive="active">Dossiers</a>
      </div>
      <div class="nav-actions">
        
        <!-- 🔔 CLOCHE DE NOTIFICATION INLINE -->
        <div class="notif-wrapper">
          <button class="bell-btn" (click)="togglePanel()" [class.has-notifs]="count > 0">
            <span class="bell-icon">🔔</span>
            <span class="badge" *ngIf="count > 0">
              {{ count > 99 ? '99+' : count }}
            </span>
          </button>

          <div class="notif-panel" *ngIf="panelOuvert">
            <div class="panel-header">
              <h3>Notifications</h3>
              <button class="btn-tout-lire" *ngIf="count > 0" (click)="toutMarquerLu()">
                Tout marquer lu
              </button>
            </div>

            <div class="panel-empty" *ngIf="notifications.length === 0">
              <span>🎉</span>
              <p>Aucune notification</p>
            </div>

            <div class="notif-list" *ngIf="notifications.length > 0">
              <div
                *ngFor="let n of notifications"
                class="notif-item"
                [class.non-lue]="!n.lue"
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
        <!-- FIN CLOCHE -->

        <div class="user-menu" *ngIf="userName">
          <span class="user-name">{{ userName }}</span>
          <button class="btn-logout" (click)="logout()">🚪</button>
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .navbar { display:flex; align-items:center; justify-content:space-between; padding:0 24px; height:64px; background:#1e293b; color:white; position:sticky; top:0; z-index:1000; box-shadow:0 2px 8px rgba(0,0,0,0.15); }
    .nav-brand a { font-size:1.25rem; font-weight:800; color:white; text-decoration:none; }
    .nav-links { display:flex; gap:24px; }
    .nav-links a { color:#94a3b8; text-decoration:none; font-size:0.9rem; font-weight:600; padding:6px 12px; border-radius:8px; transition:all 0.2s; }
    .nav-links a:hover, .nav-links a.active { color:white; background:#334155; }
    .nav-actions { display:flex; align-items:center; gap:16px; }
    .user-name { font-size:0.85rem; font-weight:600; color:#cbd5e1; }
    .btn-logout { background:none; border:none; color:#94a3b8; cursor:pointer; font-size:1.1rem; padding:6px; border-radius:8px; transition:all 0.2s; }
    .btn-logout:hover { color:#ef4444; background:rgba(239,68,68,0.1); }
    
    /* ── Cloche ── */
    .notif-wrapper { position:relative; display:inline-block; }
    .bell-btn { position:relative; background:none; border:none; cursor:pointer; padding:8px; border-radius:10px; transition:background 0.2s; font-size:1.3rem; color:#cbd5e1; }
    .bell-btn:hover { background:#334155; }
    .bell-btn.has-notifs .bell-icon { animation:ring 1.5s ease-in-out; }
    @keyframes ring { 0%,100%{transform:rotate(0);} 10%,30%,50%{transform:rotate(-15deg);} 20%,40%{transform:rotate(15deg);} }
    .badge { position:absolute; top:2px; right:2px; background:#dc2626; color:white; font-size:0.62rem; font-weight:800; min-width:18px; height:18px; border-radius:9px; display:flex; align-items:center; justify-content:center; padding:0 4px; border:2px solid #1e293b; }
    
    /* ── Panneau ── */
    .notif-panel { position:absolute; top:calc(100% + 8px); right:0; width:360px; background:#0f172a; border-radius:16px; box-shadow:0 12px 40px rgba(0,0,0,0.4); border:1px solid #334155; z-index:9999; overflow:hidden; animation:slideDown 0.2s ease; }
    @keyframes slideDown { from{opacity:0;transform:translateY(-8px);} to{opacity:1;transform:translateY(0);} }
    .panel-header { display:flex; justify-content:space-between; align-items:center; padding:16px 20px 12px; border-bottom:1px solid #334155; }
    .panel-header h3 { margin:0; font-size:0.95rem; font-weight:800; color:#e2e8f0; }
    .btn-tout-lire { font-size:0.75rem; color:#60a5fa; font-weight:700; background:none; border:none; cursor:pointer; padding:4px 8px; border-radius:6px; transition:background 0.2s; font-family:inherit; }
    .btn-tout-lire:hover { background:rgba(96,165,250,0.1); }
    .panel-empty { text-align:center; padding:40px 20px; color:#64748b; font-size:0.88rem; }
    .panel-empty span { font-size:2rem; display:block; margin-bottom:8px; }
    .notif-list { max-height:420px; overflow-y:auto; }
    .notif-item { display:flex; align-items:flex-start; gap:12px; padding:14px 20px; border-bottom:1px solid #1e293b; cursor:pointer; transition:background 0.15s; position:relative; }
    .notif-item:last-child { border-bottom:none; }
    .notif-item:hover { background:#1e293b; }
    .notif-item.non-lue { background:rgba(96,165,250,0.05); }
    .notif-icon { font-size:1.2rem; flex-shrink:0; margin-top:1px; }
    .notif-body { flex:1; min-width:0; }
    .notif-titre { font-size:0.82rem; font-weight:700; color:#e2e8f0; margin-bottom:3px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
    .notif-message { font-size:0.78rem; color:#94a3b8; line-height:1.4; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; }
    .notif-date { font-size:0.7rem; color:#64748b; margin-top:4px; font-weight:600; }
    .notif-dot { width:8px; height:8px; background:#60a5fa; border-radius:50%; flex-shrink:0; margin-top:6px; }
  `]
})
export class NavbarComponent implements OnInit, OnDestroy {
  userName: string = '';
  notifications: NotificationDTO[] = [];
  count = 0;
  panelOuvert = false;
  private subs: Subscription[] = [];

  constructor(
    private keycloakService: KeycloakService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  async ngOnInit() {
    try {
      const userDetails = await this.keycloakService.loadUserProfile();
      this.userName = userDetails.firstName || userDetails.username || 'Utilisateur';
      const token = await this.keycloakService.getToken();
      this.notificationService.connectWebSocket(userDetails.username || '', token);
      this.notificationService.chargerNotifications();
    } catch (e) {
      console.warn('Navbar init:', e);
    }

    this.subs.push(
      this.notificationService.notifications$.subscribe((notifs: NotificationDTO[]) => {
        this.notifications = notifs;
      }),
      this.notificationService.count$.subscribe((c: number) => {
        this.count = c;
      })
    );
  }

  togglePanel(): void {
    this.panelOuvert = !this.panelOuvert;
  }

  

  @HostListener('document:click', ['$event'])
  onClickOutside(event: MouseEvent): void {
    if (!(event.target as HTMLElement).closest('.notif-wrapper')) {
      this.panelOuvert = false;
    }
  }
  

  clicNotif(n: NotificationDTO): void {
    if (!n.lue) {
      this.notificationService.marquerCommeLue(n.id).subscribe();
      this.notificationService.marquerLueLocalement(n.id);
    }
  
    this.panelOuvert = false;
  
    // ✅ Signaler le dossierId AVANT la navigation
    if (n.dossierId) {
      this.notificationService.signalerDossierCible(n.dossierId);
    }
  
    if (n.urlAction) {
      this.router.navigateByUrl(n.urlAction);
    }
  }

  toutMarquerLu(): void {
    this.notificationService.marquerToutesLues().subscribe();
    this.notificationService.marquerToutesLuesLocalement();
  }

  typeIcon(type: string): string {
    const map: Record<string, string> = {
      'VALIDATION_FINANCIERE': '💰', 'VALIDATION_JURIDIQUE': '⚖️',
      'VALIDATION_FINANCIERE_OK': '✅', 'REJET_FINANCIER': '❌',
      'VALIDATION_JURIDIQUE_OK': '✅', 'REJET_JURIDIQUE': '❌',
      'NOUVELLE_MISSION': '📋', 'MISSION_MODIFIEE': '✏️',
      'PV_SOUMIS': '📄', 'FACTURE_SOUMISE': '🧾',
      'RESULTATS_SOUMIS': '📬', 'NOUVELLE_AUDIENCE': '🗓️',
      'JUGEMENT_RENDU': '🏛️'
    };
    return map[type] ?? '🔔';
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diff = Math.floor((now.getTime() - date.getTime()) / 1000);
    if (diff < 60) return 'À l\'instant';
    if (diff < 3600) return `Il y a ${Math.floor(diff / 60)} min`;
    if (diff < 86400) return `Il y a ${Math.floor(diff / 3600)} h`;
    return date.toLocaleDateString('fr-TN', {
      day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
    });
  }

  logout(): void {
    this.keycloakService.logout(window.location.origin);
  }

  ngOnDestroy(): void {
    this.notificationService.disconnectWebSocket();
    this.subs.forEach(s => s.unsubscribe());
  }
}