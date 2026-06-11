import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, Subject, interval, Subscription } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Client, IMessage } from '@stomp/stompjs';
import { Router } from '@angular/router';
import { inject } from '@angular/core';
declare var SockJS: any;

export interface NotificationDTO {
  id: number;
  titre: string;
  message: string;
  type: string;
  dateCreation: string;
  lue: boolean;
  urlAction?: string;
  dossierId?: number;
  dossier?: { id: number };
}

export interface NotificationsResponse {
  notifications: NotificationDTO[];
  count?: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {
  private router = inject(Router);

  private readonly apiUrl = `${environment.apiUrl}/api/notifications`;

  private notificationsSubject = new BehaviorSubject<NotificationDTO[]>([]);
  notifications$ = this.notificationsSubject.asObservable();

  private countSubject = new BehaviorSubject<number>(0);
  count$ = this.countSubject.asObservable();

  // ← Dossier cible à ouvrir après navigation depuis notification
  private dossierCibleSubject = new BehaviorSubject<number | null>(null);
  dossierCible$ = this.dossierCibleSubject.asObservable();

  // ← Signal de nouvelle notification temps réel
  private nouvelleNotifSubject = new Subject<NotificationDTO>();
  nouvelleNotif$ = this.nouvelleNotifSubject.asObservable();

  private stompClient: Client | null = null;
  private pollingSubscription: Subscription | null = null;

  constructor(private http: HttpClient) {}

  // ================= DOSSIER CIBLE =================

  signalerDossierCible(id: number | null): void {
    this.dossierCibleSubject.next(id);
  }

  // ================= HTTP =================

  chargerNotifications(): void {
    this.http.get<NotificationsResponse>(this.apiUrl).subscribe({
      next: (res) => {
        const notifs = (res?.notifications ?? []).map(n => this.normaliserNotification(n));
        this.notificationsSubject.next(notifs);
        this.countSubject.next(notifs.filter(n => !n.lue).length);
      },
      error: (e) => {
        console.error('❌ Erreur chargement notifications:', e);
        this.notificationsSubject.next([]);
        this.countSubject.next(0);
      }
    });
  }

  rafraichirCount(): void {
    this.http.get<{ count: number }>(`${this.apiUrl}/count`).subscribe({
      next: (res) => this.countSubject.next(res?.count ?? 0),
      error: (e) => console.warn('Erreur count:', e)
    });
  }

  getNotifications(): Observable<NotificationsResponse> {
    return this.http.get<NotificationsResponse>(this.apiUrl);
  }

  marquerCommeLue(id: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/lue`, {});
  }

  marquerToutesLues(): Observable<any> {
    return this.http.put(`${this.apiUrl}/toutes-lues`, {});
  }

  // ================= WEBSOCKET =================

  connectWebSocket(
    username: string,
    token: string,
    getToken?: () => Promise<string>
  ): void {
    if (this.stompClient?.active) {
      console.log('⚠️ [WS] Déjà connecté, skip');
      return;
    }

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      console.log('>>> [WS DEBUG] Token claims:', {
        preferred_username: payload.preferred_username,
        username_passé:     username
      });
    } catch (e) {
      console.warn('⚠️ [WS] Impossible de décoder le token');
    }

    let currentToken = token;

    this.stompClient = new Client({
      webSocketFactory: () =>
        new SockJS(`${environment.apiUrl}/ws?token=${currentToken}`),

      connectHeaders: {
        Authorization: `Bearer ${currentToken}`
      },

      reconnectDelay: 5000,

      beforeConnect: async () => {
        if (getToken) {
          try {
            currentToken = await getToken();
            if (this.stompClient) {
              this.stompClient.connectHeaders = {
                Authorization: `Bearer ${currentToken}`
              };
              this.stompClient.webSocketFactory = () =>
                new SockJS(`${environment.apiUrl}/ws?token=${currentToken}`);
              console.log('🔄 [WS] Token rafraîchi avant reconnexion');
            }
          } catch (e) {
            console.error('❌ [WS] Impossible de rafraîchir le token:', e);
          }
        }
      },

      debug: (str) => console.debug('[STOMP]', str),

      onConnect: (frame) => {
        console.log('✅ [WS] Connecté — frame headers:', frame.headers);
        this.stompClient!.subscribe(`/user/queue/notifications`, (msg: IMessage) => {
          console.log('📩 [WS] Message reçu:', msg.body);
          try {
            const notif: NotificationDTO = JSON.parse(msg.body);
            this.ajouterNotification(notif);
          } catch (e) {
            console.error('❌ Erreur parsing notif:', e);
          }
        });
      },

      onStompError: (frame) => {
        console.error('⚠️ [WS] STOMP error:', frame.headers['message'], frame.body);
        this.demarrerPolling();
      },

      onWebSocketError: (event) => {
        console.error('❌ [WS] WebSocket error:', event);
        this.demarrerPolling();
      },

      onDisconnect: () => console.warn('🔌 [WS] Déconnecté')
    });

    this.stompClient.activate();
  }

  disconnectWebSocket(): void {
    this.stopperPolling();
    this.stompClient?.deactivate();
    this.stompClient = null;
  }

  // ================= POLLING =================

  demarrerPolling(intervalMs = 30000): void {
    if (this.pollingSubscription) return;
    this.pollingSubscription = interval(intervalMs).subscribe(() => {
      this.rafraichirCount();
    });
  }

  stopperPolling(): void {
    this.pollingSubscription?.unsubscribe();
    this.pollingSubscription = null;
  }

  // ================= HELPERS =================

  private normaliserNotification(n: any): NotificationDTO {
    const urlAction = n.urlAction ?? n.url_action ?? n.url ?? undefined;
    const dossierId = n.dossierId
      ?? n.dossier?.id
      ?? (urlAction ? this.extractIdFromUrl(urlAction) : undefined);
    return { ...n, dossierId, urlAction };
  }

  private extractIdFromUrl(url: string): number | undefined {
    if (!url) return undefined;
    const match = url.match(/\/dossiers\/(\d+)/);
    return match ? Number(match[1]) : undefined;
  }

  private ajouterNotification(notif: any): void {
    const normalized = this.normaliserNotification(notif);
    const current = this.notificationsSubject.getValue();
    this.notificationsSubject.next([normalized, ...current]);
    this.countSubject.next(this.countSubject.getValue() + 1);
    this.nouvelleNotifSubject.next(normalized); // ← émet le signal temps réel
  }

  marquerLueLocalement(id: number): void {
    const updated = this.notificationsSubject.getValue()
      .map(n => n.id === id ? { ...n, lue: true } : n);
    this.notificationsSubject.next(updated);
    this.countSubject.next(updated.filter(n => !n.lue).length);
  }

  marquerToutesLuesLocalement(): void {
    const updated = this.notificationsSubject.getValue().map(n => ({ ...n, lue: true }));
    this.notificationsSubject.next(updated);
    this.countSubject.next(0);
  }

  ngOnDestroy(): void {
    this.disconnectWebSocket();
  }


  private missionCibleSubject = new BehaviorSubject<string | number | null>(null);
  missionCible$ = this.missionCibleSubject.asObservable();
  
  signalerMissionCible(missionIdOrNum: string | number | null): void {
    this.missionCibleSubject.next(missionIdOrNum);
  }

  naviguerVersNotification(notification: any): void {
    this.marquerCommeLue(notification.id).subscribe();
    const url: string | null = notification.urlAction ?? notification.url ?? null;
    if (!url) return;
    const [path, queryString] = url.split('?');
    const queryParams: Record<string, string> = {};
    if (queryString) {
      queryString.split('&').forEach(pair => {
        const [key, val] = pair.split('=');
        if (key && val) queryParams[key] = decodeURIComponent(val);
      });
    }
    this.router.navigate([path], {
      queryParams: Object.keys(queryParams).length ? queryParams : undefined
    });
  }


  // ← Signal spécifique pour naviguer vers une facture (validateur financier)
private factureCibleSubject = new BehaviorSubject<number | null>(null);
factureCible$ = this.factureCibleSubject.asObservable();

signalerFactureCible(id: number | null): void {
  this.factureCibleSubject.next(id);
}
}