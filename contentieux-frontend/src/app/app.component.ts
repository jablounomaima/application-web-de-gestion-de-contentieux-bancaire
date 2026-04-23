import { Component, OnInit } from '@angular/core';
import { RouterOutlet, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { KeycloakService } from 'keycloak-angular';
import { NotificationService, NotificationDTO, NotificationsResponse } from './core/services/notification.service';
import { SidebarComponent } from './features/sidebar/sidebar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule, RouterModule, SidebarComponent],
  template: `
    <ng-container *ngIf="isLoggedIn; else publicLayout">

      <!-- ══ HEADER ══ -->
      <header class="app-header">
        <div class="logo">
          <span class="logo-icon">🏦</span>
          <span>Contentieux Bancaire</span>
        </div>
        <div class="user-actions">

          <!-- Notifications -->
          <div class="notifications" (click)="toggleNotifications()">
            <i class="icon">🔔</i>
            <span class="badge" *ngIf="nonLues > 0">{{ nonLues }}</span>
            <div class="notif-dropdown" *ngIf="showNotifications" (click)="$event.stopPropagation()">
              <h4>Notifications</h4>
              <div *ngIf="notifications.length === 0" class="no-notif">Aucune notification</div>
              <ul>
                <li *ngFor="let notif of notifications"
                    [class.unread]="!notif.lue"
                    (click)="marquerLue(notif)">
                  <strong>{{ notif.titre }}</strong>
                  <p>{{ notif.message }}</p>
                  <small>{{ notif.dateCreation | date:'short' }}</small>
                </li>
              </ul>
            </div>
          </div>

          <!-- Profil -->
          <div class="user-profile">
            <div class="avatar">{{ username.charAt(0).toUpperCase() }}</div>
            <span class="username">{{ username }}</span>
          </div>

          <!-- Déconnexion -->
          <button class="logout-btn" (click)="logout()">
            <span>Déconnexion</span>
          </button>

        </div>
      </header>

      <!-- ══ LAYOUT ══ -->
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
    .app-header {
      background-color: #001f3f;
      color: white;
      padding: 12px 30px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-family: 'Inter', sans-serif;
      box-shadow: 0 4px 6px rgba(0,0,0,0.1);
      position: relative;
      z-index: 15;
    }
    .logo { display: flex; align-items: center; gap: 10px; font-size: 1.4rem; font-weight: 700; letter-spacing: 0.5px; }
    .user-actions { display: flex; align-items: center; gap: 25px; position: relative; }
    .user-profile { display: flex; align-items: center; gap: 10px; }
    .avatar { width: 35px; height: 35px; background: linear-gradient(135deg, #007bff, #001f3f); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 1.1rem; box-shadow: 0 2px 4px rgba(0,0,0,0.2); }
    .username { font-weight: 500; font-size: 0.95rem; }
    .logout-btn { background-color: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); color: white; padding: 8px 16px; border-radius: 6px; cursor: pointer; transition: all 0.2s; font-weight: 500; display: flex; align-items: center; gap: 5px; }
    .logout-btn:hover { background-color: white; color: #001f3f; }
    .notifications { cursor: pointer; position: relative; font-size: 1.3rem; display: flex; align-items: center; }
    .badge { position: absolute; top: -5px; right: -8px; background: #ff4757; color: white; font-size: 0.7rem; padding: 2px 6px; border-radius: 50%; font-weight: bold; border: 2px solid #001f3f; }
    .notif-dropdown { position: absolute; top: 45px; right: -50px; background: white; color: #333; width: 320px; border-radius: 10px; box-shadow: 0 10px 25px rgba(0,0,0,0.2); border: 1px solid #eee; z-index: 1000; overflow: hidden; cursor: default; animation: dropDown 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
    @keyframes dropDown { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }
    .notif-dropdown h4 { margin: 0; padding: 15px 20px; background: #f8f9fa; border-bottom: 1px solid #eee; font-size: 1rem; color: #001f3f; }
    .no-notif { padding: 20px; text-align: center; color: #777; font-style: italic; }
    .notif-dropdown ul { list-style: none; margin: 0; padding: 0; max-height: 350px; overflow-y: auto; }
    .notif-dropdown li { padding: 15px 20px; border-bottom: 1px solid #f1f1f1; cursor: pointer; transition: background 0.2s; }
    .notif-dropdown li:hover { background: #f8f9fa; }
    .notif-dropdown li.unread { background: #f0f7ff; border-left: 4px solid #007bff; }
    .notif-dropdown li strong { display: block; font-size: 0.95rem; margin-bottom: 5px; color: #001f3f; }
    .notif-dropdown li p { margin: 0 0 5px 0; font-size: 0.85rem; color: #555; line-height: 1.4; }
    .notif-dropdown li small { font-size: 0.75rem; color: #888; }
    .app-layout { display: flex; min-height: calc(100vh - 65px); font-family: 'Inter', sans-serif; }
    .app-main { flex: 1; padding: 30px; background-color: #f4f7f6; overflow-y: auto; height: calc(100vh - 65px); box-sizing: border-box; }
    .public-main { min-height: 100vh; }
  `]
})
export class AppComponent implements OnInit {
  title = 'contentieux-frontend';
  isLoggedIn = false;
  username = '';
  roles: string[] = [];

  notifications: NotificationDTO[] = [];
  nonLues = 0;
  showNotifications = false;

  constructor(
    private keycloak: KeycloakService,
    private notificationService: NotificationService
  ) {}

  async ngOnInit() {
    try {
      this.isLoggedIn = await this.keycloak.isLoggedIn();
  
      if (!this.isLoggedIn) return;
  
      this.roles = this.keycloak.getUserRoles();
  
      // 1. Récupération sécurisée du username
      try {
        const userProfile = await this.keycloak.loadUserProfile();
        this.username = userProfile.username || 'Utilisateur';
      } catch (profileErr) {
        const tokenParsed: any = this.keycloak.getKeycloakInstance().tokenParsed;
        this.username = tokenParsed?.preferred_username || 'Utilisateur';
      }
  
      // 2. Vérification que le token existe avant d'appeler l'API
      const token = await this.keycloak.getToken();
      if (token) {
        this.chargerNotifications();
      }
  
    } catch (err) {
      console.error('[AppComponent] Erreur globale:', err);
    }
  }

  chargerNotifications() {
    this.notificationService.getNotifications().subscribe({
      next: (data) => {
        const list = Array.isArray(data)
          ? data
          : (data as NotificationsResponse)?.notifications ?? [];
        this.notifications = list;
        this.nonLues = list.filter(n => !n.lue).length;
      },
      error: (err) => console.error('Erreur chargement notifications', err)
    });
  }

  toggleNotifications() {
    this.showNotifications = !this.showNotifications;
  }

  marquerLue(notif: NotificationDTO) {
    if (!notif.lue) {
      this.notificationService.marquerCommeLue(notif.id).subscribe({
        next: () => {
          notif.lue = true;
          this.nonLues = Math.max(0, this.nonLues - 1);
        }
      });
    }
  }

  logout() {
    this.keycloak.logout(window.location.origin);
  }
}