import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { RouterOutlet, RouterModule, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { KeycloakService } from 'keycloak-angular';
import { Subscription } from 'rxjs';
import {
  NotificationService,
  NotificationDTO
} from './core/services/notification.service';
import { SidebarComponent } from './features/sidebar/sidebar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule, RouterModule, SidebarComponent],
  template: `
    <ng-container *ngIf="isLoggedIn; else publicLayout">
      <header class="app-header">
        <div class="brand">
          <div class="brand-mark">CB</div>
          <div>
            <div class="brand-title">Contentieux Bancaire</div>
            <div class="brand-subtitle">Plateforme de gestion intelligente</div>
          </div>
        </div>
        <div class="user-actions">

          <!-- ══ NOTIFICATIONS ══ -->
          <div class="notif-wrapper" (click)="toggleNotifications()">
            <button class="bell-btn" [class.has-notifs]="nonLues > 0">
              🔔
              <span class="badge" *ngIf="nonLues > 0">
                {{ nonLues > 99 ? '99+' : nonLues }}
              </span>
            </button>

            <div class="notif-dropdown" *ngIf="showNotifications"
                 (click)="$event.stopPropagation()">
              <div class="notif-header">
                <span>Notifications
                  <span class="count-badge" *ngIf="nonLues > 0">{{ nonLues }} non lues</span>
                </span>
                <button class="btn-tout-lire" *ngIf="nonLues > 0" (click)="toutMarquerLu()">
                  Tout marquer lu
                </button>
              </div>
              <div class="no-notif" *ngIf="notifications.length === 0">
                🎉 Aucune notification
              </div>
              <ul>
                <li *ngFor="let notif of notificationsTries"
                    [class.unread]="!notif.lue"
                    (click)="clicNotification(notif)">
                  <div class="notif-row">
                    <span class="notif-icone">{{ typeIcone(notif.type) }}</span>
                    <div class="notif-body">
                      <strong>{{ notif.titre }}</strong>
                      <p>{{ notif.message }}</p>
                      <small>{{ formatDate(notif.dateCreation) }}</small>
                    </div>
                    <span class="dot" *ngIf="!notif.lue"></span>
                  </div>
                </li>
              </ul>
            </div>
          </div>

          <!-- ══ TOAST TEMPS RÉEL ══ -->
          <div class="toast-container" (click)="$event.stopPropagation()">
            <div class="notif-toast"
                 *ngIf="toastVisible"
                 [class]="'notif-toast notif-toast--' + toastType"
                 (click)="naviguerVersToast()">
              <span class="toast-icon">{{ toastIcon }}</span>
              <div class="toast-body">
                <div class="toast-title">{{ toastTitre }}</div>
                <div class="toast-msg">{{ toastMessage }}</div>
              </div>
              <button class="toast-close"
                      (click)="$event.stopPropagation(); fermerToast()">×</button>
            </div>
          </div>

          <!-- ══ PROFIL ══ -->
          <div class="user-profile">
            <div class="avatar">{{ username.charAt(0).toUpperCase() }}</div>
            <div class="profile-meta">
              <div class="username">{{ username }}</div>
              <div class="role-chip">Compte sécurisé</div>
            </div>
          </div>

          <button class="logout-btn" (click)="logout()">
            <span>Déconnexion</span>
          </button>
        </div>
      </header>

      <div class="app-layout">
        <app-sidebar [username]="username" [roles]="roles"></app-sidebar>
        <main class="app-main">
          <router-outlet></router-outlet>
        </main>
      </div>
    </ng-container>

    <ng-template #publicLayout>
      <main class="public-main">
        <router-outlet></router-outlet>
      </main>
    </ng-template>
  `,
  styles: [`
    /* ── HEADER ── */
    .app-header {
      position: sticky;
      top: 0;
      z-index: 20;
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 28px;
background: linear-gradient(135deg, #071a2f 0%, #0b2b4a 55%, #1d4ed8 100%);
      color: white;
      box-shadow: 0 12px 36px rgba(7, 26, 47, 0.18);
      border-bottom: 1px solid rgba(255, 255, 255, 0.16);
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .brand-mark {
      width: 46px;
      height: 46px;
      border-radius: 14px;
      display: grid;
      place-items: center;
      font-weight: 800;
      color: white;
      background: linear-gradient(135deg, #60a5fa, #2563eb);
      box-shadow: 0 10px 24px rgba(37, 99, 235, 0.28);
    }

    .brand-title {
      font-size: 1rem;
      font-weight: 800;
      letter-spacing: 0.02em;
    }

    .brand-subtitle {
      font-size: 0.78rem;
      color: rgba(255,255,255,0.76);
      margin-top: 2px;
    }

    .user-actions {
      display: flex;
      align-items: center;
      gap: 18px;
      position: relative;
    }

    .user-profile {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 6px 10px;
      border-radius: 999px;
      background: rgba(255,255,255,0.12);
      border: 1px solid rgba(255,255,255,0.16);
    }

    .avatar {
      width: 38px;
      height: 38px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 800;
      font-size: 1rem;
      background: linear-gradient(135deg, #ffffff 0%, #60a5fa 100%);
      color: #0b2b4a;
    }

    .profile-meta {
      display: flex;
      flex-direction: column;
    }

    .username {
      font-weight: 700;
      font-size: 0.95rem;
      line-height: 1.1;
    }

    .role-chip {
      font-size: 0.72rem;
      color: rgba(255,255,255,0.76);
    }

    .logout-btn {
      background: rgba(255,255,255,0.14);
      border: 1px solid rgba(255,255,255,0.16);
      color: white;
      padding: 9px 14px;
      border-radius: 999px;
      cursor: pointer;
      transition: all 0.2s;
      font-weight: 600;
    }

    .logout-btn:hover {
      background: white;
      color: #0b2b4a;
      transform: translateY(-1px);
    }

    /* ── NOTIFICATIONS ── */
    .notif-wrapper { position:relative; display:inline-block; cursor:pointer; }
    .bell-btn { background:none; border:none; font-size:1.35rem; cursor:pointer; position:relative; padding:6px; border-radius:10px; color:white; transition:background .2s; }
    .bell-btn:hover { background:rgba(255,255,255,0.14); }
    .bell-btn.has-notifs { animation:ring 2s ease-in-out; }
    @keyframes ring { 0%,100%{transform:rotate(0);} 10%,30%,50%{transform:rotate(-15deg);} 20%,40%{transform:rotate(15deg);} }
    .badge { position:absolute; top:0; right:0; background:#ff4757; color:white; font-size:0.62rem; padding:2px 5px; border-radius:999px; font-weight:bold; border:2px solid #071a2f; min-width:18px; text-align:center; }

    /* ── DROPDOWN NOTIFICATIONS - Style sidebar ── */
    .notif-dropdown {
      position:absolute;
      top:calc(100% + 10px);
      right:-20px;
      background:white;
      color:#333;
      width:380px;
      max-width:90vw;
      border-radius:16px;
      box-shadow:0 16px 50px rgba(7,26,47,0.25);
      border:1px solid rgba(255,255,255,0.14);
      z-index:1000;
      overflow:hidden;
      animation:slideDown .2s ease;
    }
    @keyframes slideDown { from{opacity:0;transform:translateY(-8px);} to{opacity:1;transform:translateY(0);} }

    .notif-header {
      display:flex;
      justify-content:space-between;
      align-items:center;
      padding:14px 18px;
background:linear-gradient(135deg, #071a2f 0%, #0b2b4a 55%, #1d4ed8 100%);
      border-bottom:1px solid rgba(255,255,255,0.14);
      font-size:0.9rem;
      font-weight:600;
      color:white;
    }
    .notif-header .count-badge {
      background:#ff4757;
      color:white;
      padding:2px 8px;
      border-radius:999px;
      font-size:0.75rem;
      margin-left:8px;
    }
    .btn-tout-lire {
      background:rgba(255,255,255,0.14);
      border:1px solid rgba(255,255,255,0.16);
      color:white;
      font-size:0.78rem;
      cursor:pointer;
      font-weight:600;
      padding:4px 12px;
      border-radius:8px;
      transition:all .2s;
    }
    .btn-tout-lire:hover {
      background:rgba(255,255,255,0.25);
      transform:translateY(-1px);
    }

    .no-notif { padding:30px; text-align:center; color:#94a3b8; font-style:italic; font-size:0.9rem; }
    .notif-dropdown ul { list-style:none; margin:0; padding:0; max-height:400px; overflow-y:auto; }
    .notif-dropdown li { border-bottom:1px solid #f5f5f5; cursor:pointer; transition:background .15s; }
    .notif-dropdown li:hover { background:#f8fafc; }
    .notif-dropdown li.unread {
      background:linear-gradient(135deg, #eff6ff, #dbeafe);
      border-left:4px solid #2563eb;
    }
    .notif-row {
      display:flex;
      align-items:flex-start;
      gap:12px;
      padding:14px 16px;
    }
    .notif-icone { font-size:1.3rem; flex-shrink:0; margin-top:2px; }
    .notif-body { flex:1; min-width:0; }
    .notif-body strong { display:block; font-size:0.87rem; color:#0f172a; margin-bottom:4px; }
    .notif-body p { margin:0 0 4px; font-size:0.8rem; color:#475569; line-height:1.4; white-space:pre-wrap; word-break:break-word; }
    .notif-body small { font-size:0.73rem; color:#94a3b8; }
    .dot { width:8px; height:8px; background:#2563eb; border-radius:50%; flex-shrink:0; margin-top:6px; }

    /* ── TOAST - Style sidebar ── */
    .toast-container {
      position:fixed;
      top:78px;
      right:20px;
      z-index:9999;
      pointer-events:none;
    }
    .notif-toast {
      pointer-events:all;
      background:white;
      border-radius:14px;
      box-shadow:0 12px 32px rgba(7,26,47,0.2);
      border-left:4px solid #2563eb;
      display:flex;
      align-items:flex-start;
      gap:12px;
      padding:14px 16px;
      width:360px;
      max-width:90vw;
      cursor:pointer;
      animation:toastIn .3s ease;
      transition:opacity .3s, transform .3s;
    }
    .notif-toast--success { border-left-color:#059669; }
    .notif-toast--error   { border-left-color:#dc2626; }
    .notif-toast--warning { border-left-color:#f59e0b; }
    .notif-toast--info    { border-left-color:#2563eb; }
    @keyframes toastIn { from{opacity:0;transform:translateX(30px);} to{opacity:1;transform:translateX(0);} }
    .toast-icon { font-size:20px; flex-shrink:0; }
    .toast-body { flex:1; overflow:hidden; }
    .toast-title { font-weight:700; font-size:13px; color:#0f172a; margin-bottom:3px; }
    .toast-msg { font-size:12px; color:#475569; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
    .toast-close {
      background:none;
      border:none;
      cursor:pointer;
      color:#94a3b8;
      font-size:20px;
      padding:0;
      line-height:1;
      flex-shrink:0;
      align-self:flex-start;
      transition:color .2s;
    }
    .toast-close:hover { color:#475569; }

    /* ── LAYOUT ── */
    .app-layout {
      display:flex;
      min-height:calc(100vh - 74px);
      font-family:'Inter',sans-serif;
    }
    .app-main {
      flex:1;
      padding:30px;
      background:linear-gradient(135deg, #f8fbff 0%, #eef4ff 100%);
      overflow-y:auto;
      height:calc(100vh - 74px);
      box-sizing:border-box;
    }
    .public-main { min-height:100vh; }

    /* ── RESPONSIVE ── */
    @media (max-width: 768px) {
      .app-header { padding:12px 16px; flex-wrap:wrap; gap:8px; }
      .brand-title { font-size:0.85rem; }
      .brand-subtitle { display:none; }
      .user-profile .profile-meta { display:none; }
      .logout-btn span { display:none; }
      .logout-btn { padding:8px 12px; font-size:1.1rem; }
      .logout-btn::before { content:'🚪'; }
      .notif-dropdown { right:-10px; width:calc(100vw - 30px); }
      .app-main { padding:16px; }
    }
  `]
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'contentieux-frontend';
  isLoggedIn = false;
  username   = '';
  roles: string[] = [];

  notifications: NotificationDTO[] = [];
  nonLues           = 0;
  showNotifications = false;

  // ── Toast ────────────────────────────────────────────────────
  toastVisible    = false;
  toastTitre      = '';
  toastMessage    = '';
  toastType       = 'info';
  toastIcon       = '🔔';
  toastUrlAction: string | undefined;
  private toastTimer: any;

  // ── Anti double-navigation ───────────────────────────────────
  private navigationEnCours = false;

  private sub!: Subscription;
  private prevCount = 0;

  constructor(
    private keycloak:             KeycloakService,
    private notificationService:  NotificationService,
    private router:               Router
  ) {}

  // ── Tri : non lues d'abord, puis par date desc ───────────────
  get notificationsTries(): NotificationDTO[] {
    return [...this.notifications].sort((a, b) => {
      if (!a.lue && b.lue)  return -1;
      if (a.lue  && !b.lue) return  1;
      return new Date(b.dateCreation).getTime() - new Date(a.dateCreation).getTime();
    });
  }

  // ════════════════════════════════════════════════════════════
  // Init
  // ════════════════════════════════════════════════════════════
  async ngOnInit() {
    try {
      this.isLoggedIn = await this.keycloak.isLoggedIn();
      if (!this.isLoggedIn) return;

      this.roles = this.keycloak.getUserRoles();

      try {
        const userProfile = await this.keycloak.loadUserProfile();
        this.username = userProfile.username || 'Utilisateur';
      } catch {
        const tokenParsed: any = this.keycloak.getKeycloakInstance().tokenParsed;
        this.username = tokenParsed?.preferred_username || 'Utilisateur';
      }

      this.sub = this.notificationService.notifications$.subscribe(
        (list: NotificationDTO[]) => {
          this.nonLues = list.filter(n => !n.lue).length;

          if (list.length > 0 && list.length > this.prevCount) {
            const derniere = list[0];
            if (!derniere.lue) {
              this._afficherToast(derniere);
            }
          }
          this.prevCount     = list.length;
          this.notifications = list;
        }
      );

      await this._connecterWebSocket();
      this.notificationService.chargerNotifications();

    } catch (err) {
      console.error('[AppComponent] Erreur globale:', err);
    }
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    clearTimeout(this.toastTimer);
    this.notificationService.disconnectWebSocket();
    this.notificationService.stopperPolling();
  }

  // ════════════════════════════════════════════════════════════
  // WebSocket
  // ════════════════════════════════════════════════════════════
  private async _connecterWebSocket(): Promise<void> {
    try {
      await this.keycloak.updateToken(30);
      const token = await this.keycloak.getToken();
      if (!token) {
        console.error('❌ [WS] Token introuvable');
        return;
      }

      const payload   = JSON.parse(atob(token.split('.')[1]));
      const expiresAt = new Date(payload.exp * 1000);
      console.log(`>>> [WS] Token valide jusqu'à ${expiresAt.toISOString()}`);

      this.notificationService.connectWebSocket(
        this.username,
        token,
        () => this._getFreshToken()
      );
    } catch (err) {
      console.error('❌ [WS] Erreur rafraîchissement token:', err);
    }
  }

  private async _getFreshToken(): Promise<string> {
    await this.keycloak.updateToken(30);
    return this.keycloak.getToken();
  }

  // ════════════════════════════════════════════════════════════
  // Toast temps réel
  // ════════════════════════════════════════════════════════════
  private _afficherToast(n: NotificationDTO): void {
    clearTimeout(this.toastTimer);
    this.toastTitre     = n.titre;
    this.toastMessage   = n.message;
    this.toastUrlAction = this.resolveUrl(n) ?? n.urlAction ?? undefined;
    this.toastType      = this._resolveToastType(n.type);
    this.toastIcon      = this.typeIcone(n.type);
    this.toastVisible   = true;
    this.toastTimer     = setTimeout(() => this.fermerToast(), 7000);
  }

  fermerToast(): void {
    this.toastVisible = false;
    clearTimeout(this.toastTimer);
  }

  naviguerVersToast(): void {
    this.fermerToast();
    if (!this.toastUrlAction) return;

    const resolvedUrl    = this.toastUrlAction;
    const urlSansParams  = resolvedUrl.split('?')[0];
    const currentUrlBase = this.router.url.split('?')[0];

    if (currentUrlBase === urlSansParams) {
      this.router.navigateByUrl('/', { skipLocationChange: true }).then(() => {
        this.router.navigateByUrl(resolvedUrl);
      });
    } else {
      this.router.navigateByUrl(resolvedUrl);
    }
  }

  private _resolveToastType(type: string): string {
    if (!type) return 'info';
    if (type.includes('_OK') || type.includes('CLOTUR'))  return 'success';
    if (type.includes('REJET'))                           return 'error';
    if (type.includes('SOUMIS') || type.includes('RESOUMISSION') || type.includes('NOUVELLE')) return 'warning';
    return 'info';
  }

  // ════════════════════════════════════════════════════════════
  // Panel notifications
  // ════════════════════════════════════════════════════════════
  @HostListener('document:click', ['$event'])
  onClickOutside(event: MouseEvent): void {
    if (!(event.target as HTMLElement).closest('.notif-wrapper')) {
      this.showNotifications = false;
    }
  }

  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
    if (this.showNotifications) {
      this.notificationService.chargerNotifications();
    }
  }

  clicNotification(notif: NotificationDTO): void {
    if (this.navigationEnCours) return;
    this.navigationEnCours = true;
    setTimeout(() => this.navigationEnCours = false, 1000);

    console.log('=== CLIC NOTIF ===');
    console.log('type:', notif.type);
    console.log('dossierId:', notif.dossierId);
    console.log('urlAction:', notif.urlAction);
    console.log('roles:', this.roles);
    console.log('url résolue:', this.resolveUrl(notif) ?? notif.urlAction);
    console.log('currentUrl:', this.router.url);
    console.log('==================');

    if (!notif.lue) {
      this.notificationService.marquerCommeLue(notif.id).subscribe();
      this.notificationService.marquerLueLocalement(notif.id);
    }

    this.showNotifications = false;

    const url = this.resolveUrl(notif) ?? notif.urlAction;
    if (!url) return;

    const dossierId = notif.dossierId ?? (notif as any).dossier?.id ?? null;
    const qMatch    = notif.urlAction?.match(/[?&]missionId=(\d+)/);
    const missionId = qMatch ? Number(qMatch[1]) : null;

    const resolvedMissionMatch = url.match(/\/missions\/(\d+)/);
    const resolvedMissionId = resolvedMissionMatch ? Number(resolvedMissionMatch[1]) : null;

    const effectiveMissionId = missionId ?? resolvedMissionId;

    if (url === '/validateur/financier/factures' && dossierId) {
      this.notificationService.signalerFactureCible(dossierId);
      if (effectiveMissionId) this.notificationService.signalerMissionCible(effectiveMissionId);
    } else if (dossierId) {
      this.notificationService.signalerDossierCible(dossierId);
      if (effectiveMissionId) this.notificationService.signalerMissionCible(effectiveMissionId);
    } else if (effectiveMissionId) {
      this.notificationService.signalerMissionCible(effectiveMissionId);
    }

    const urlSansParams  = url.split('?')[0];
    const currentUrlBase = this.router.url.split('?')[0];

    if (currentUrlBase === urlSansParams) {
      const currentFull = this.router.url;
      if (currentFull === url) {
        this.router.navigateByUrl(url, { onSameUrlNavigation: 'reload' } as any);
      } else {
        this.router.navigateByUrl(url, { replaceUrl: true });
      }
      return;
    }

    this.router.navigateByUrl(url);
  }

  toutMarquerLu(): void {
    this.notificationService.marquerToutesLues().subscribe();
    this.notificationService.marquerToutesLuesLocalement();
  }

  // ════════════════════════════════════════════════════════════
  // Résolution d'URL par type de notification et rôle
  // ════════════════════════════════════════════════════════════
  private resolveUrl(notif: NotificationDTO): string | null {
    const roles   = this.roles.map(r => r.replace(/^ROLE_/, '').toUpperCase());
    const hasRole = (r: string) => roles.includes(r);
    const dossierId = notif.dossierId ?? (notif as any).dossier?.id;

    const safeUrl = (url: string | undefined | null, fallback: string): string => {
      if (!url) return fallback;
      if (url.includes('/agent/') && !hasRole('AGENT')) return fallback;
      if (
        url.includes('/prestataire/') &&
        !hasRole('PRESTATAIRE') &&
        !hasRole('EXPERT') &&
        !hasRole('HUISSIER')
      ) return fallback;
      return url;
    };

    const extractMotif = (): string => {
      const m = notif.message?.match(/Motif\s*:\s*(.+?)(?:\s*Merci|$)/s);
      return m ? encodeURIComponent(m[1].trim()) : '';
    };

    const isRejectionVersM = (): string | null => {
      const m = notif.urlAction?.match(/\/missions\/(\d+)/);
      return m ? m[1] : null;
    };

    switch (notif.type) {

      // ── Validation dossier ──────────────────────────────────
      case 'VALIDATION_FINANCIERE':
        return dossierId ? `/validateur/financier/dashboard?dossierId=${dossierId}` : null;

      case 'VALIDATION_JURIDIQUE':
        return dossierId ? `/validateur/juridique/dashboard?dossierId=${dossierId}` : null;

      // ── Rejet financier ─────────────────────────────────────
      case 'REJET_FINANCIER':
        if (hasRole('PRESTATAIRE') || hasRole('EXPERT') || hasRole('HUISSIER')) {
          const mId = isRejectionVersM();
          if (mId) {
            const motif = extractMotif();
            return `/prestataire/missions/${mId}?action=edit-facture&motif=${motif}`;
          }
          return safeUrl(notif.urlAction, `/prestataire/missions`);
        }
        if (hasRole('AVOCAT')) return `/avocat/affaires`;
        if (notif.urlAction?.includes('/resultats-prestataires')) return notif.urlAction;
        return dossierId ? `/agent/dossiers/${dossierId}` : `/agent/liste`;

      case 'VALIDATION_JURIDIQUE_OK':
      case 'REJET_JURIDIQUE':
        if (hasRole('PRESTATAIRE') || hasRole('EXPERT') || hasRole('HUISSIER'))
          return safeUrl(notif.urlAction, `/prestataire/missions`);
        if (hasRole('AVOCAT')) return `/avocat/affaires`;
        if (notif.urlAction?.includes('/resultats-prestataires')) return notif.urlAction;
        return dossierId ? `/agent/dossiers/${dossierId}` : `/agent/liste`;

      // ── Décision financière OK → selon rôle ─────────────────
      case 'VALIDATION_FINANCIERE_OK':
        if (hasRole('PRESTATAIRE') || hasRole('EXPERT') || hasRole('HUISSIER'))
          return safeUrl(notif.urlAction, `/prestataire/missions`);
        if (hasRole('AVOCAT'))
          return safeUrl(notif.urlAction, `/avocat/affaires`);
        if (notif.urlAction?.includes('/resultats-prestataires')) return notif.urlAction;
        return dossierId ? `/agent/dossiers/${dossierId}` : `/agent/liste`;

      // ── Mission prestataire ─────────────────────────────────
      case 'NOUVELLE_MISSION':
      case 'MISSION_MODIFIEE':
        if (hasRole('AVOCAT')) return `/avocat/dashboard`;
        return safeUrl(notif.urlAction, `/prestataire/missions`);

      // ── Soumissions facture / PV ────────────────────────────
      case 'PV_SOUMIS':
      case 'FACTURE_SOUMISE':
      case 'RESULTAT_SOUMIS':
      case 'RESULTAT_MODIFIE':
        if (hasRole('VALIDATEUR_FINANCIER')) return `/validateur/financier/factures`;
        if (hasRole('AGENT'))
          return notif.urlAction
            ?? (dossierId ? `/agent/dossiers/${dossierId}/resultats-prestataires` : `/agent/liste`);
        return dossierId ? `/agent/dossiers/${dossierId}` : `/agent/liste`;

      // ── Mission clôturée / rejetée ───────────────────────────
      case 'MISSION_CLOTUREE':
      case 'MISSION_REJETEE':
        return safeUrl(notif.urlAction, `/prestataire/missions`);

      // ── Resoumission ────────────────────────────────────────
      case 'RESOUMISSION':
        if (hasRole('AGENT')) {
          return dossierId ? `/agent/dossiers/${dossierId}` : `/agent/liste`;
        }
        return safeUrl(notif.urlAction, `/prestataire/missions`);

      // ── Audience / Jugement ─────────────────────────────────
      case 'NOUVELLE_AUDIENCE':
      case 'JUGEMENT_RENDU':
        if (hasRole('AVOCAT')) return dossierId ? `/avocat/affaires/${dossierId}` : `/avocat/affaires`;
        return dossierId ? `/agent/dossiers/${dossierId}` : `/agent/liste`;

      // ── Rejet facture ───────────────────────────────────────
      case 'FACTURE_REJETEE':
        if (hasRole('PRESTATAIRE') || hasRole('EXPERT') || hasRole('HUISSIER')) {
          const mId = isRejectionVersM();
          if (mId) {
            const motif = extractMotif();
            return `/prestataire/missions/${mId}?action=edit-facture&motif=${motif}`;
          }
          return `/prestataire/missions`;
        }
        return dossierId ? `/agent/dossiers/${dossierId}` : `/agent/liste`;

      // ✅ AJOUT : Mot de passe oublié → admin dashboard avec tab et username
      case 'MOT_DE_PASSE_OUBLIE':
        return notif.urlAction ?? '/admin/dashboard';

      default:
        if (notif.urlAction) {
          if (!hasRole('AGENT') && notif.urlAction.includes('/agent/')) {
            if (hasRole('AVOCAT'))               return `/avocat/affaires`;
            if (hasRole('VALIDATEUR_FINANCIER')) return `/validateur/financier/factures`;
            if (hasRole('PRESTATAIRE') || hasRole('EXPERT') || hasRole('HUISSIER'))
              return `/prestataire/missions`;
            return null;
          }
          if (hasRole('AGENT') && notif.urlAction.startsWith('/admin/dossiers/')) {
            return notif.urlAction.replace('/admin/dossiers/', '/agent/dossiers/');
          }
          return notif.urlAction;
        }
        return dossierId ? `/agent/dossiers/${dossierId}` : null;
    }
  }

  // ════════════════════════════════════════════════════════════
  // Helpers affichage
  // ════════════════════════════════════════════════════════════
  typeIcone(type: string): string {
    const map: Record<string, string> = {
      'VALIDATION_FINANCIERE':    '💰',
      'VALIDATION_JURIDIQUE':     '⚖️',
      'VALIDATION_FINANCIERE_OK': '✅',
      'REJET_FINANCIER':          '❌',
      'VALIDATION_JURIDIQUE_OK':  '✅',
      'REJET_JURIDIQUE':          '❌',
      'NOUVELLE_MISSION':         '📋',
      'MISSION_MODIFIEE':         '✏️',
      'MISSION_CLOTUREE':         '🏁',
      'MISSION_REJETEE':          '❌',
      'RESOUMISSION':             '🔄',
      'PV_SOUMIS':                '📄',
      'FACTURE_SOUMISE':          '🧾',
      'RESULTAT_SOUMIS':          '📬',
      'RESULTAT_MODIFIE':         '📝',
      'NOUVELLE_AUDIENCE':        '🗓️',
      'JUGEMENT_RENDU':           '🏛️',
      'FACTURE_REJETEE':          '❌',
      'MOT_DE_PASSE_OUBLIE':      '🔑',
    };
    return map[type] ?? '🔔';
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now  = new Date();
    const diff = Math.floor((now.getTime() - date.getTime()) / 1000);
    if (diff < 60)    return 'À l\'instant';
    if (diff < 3600)  return `Il y a ${Math.floor(diff / 60)} min`;
    if (diff < 86400) return `Il y a ${Math.floor(diff / 3600)} h`;
    return date.toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'short',
      hour: '2-digit', minute: '2-digit'
    });
  }

  logout(): void {
    this.notificationService.disconnectWebSocket();
    this.notificationService.stopperPolling();
    this.keycloak.logout(window.location.origin);
  }
}