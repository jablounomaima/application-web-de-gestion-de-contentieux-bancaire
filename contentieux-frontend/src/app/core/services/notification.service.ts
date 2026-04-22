import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface NotificationDTO {
  id: number;
  titre: string;
  message: string;
  dateCreation: string;
  lue: boolean;
  typeNotification: string;
}

export interface NotificationsResponse {
  notifications: NotificationDTO[];
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private apiUrl = `${environment.apiUrl}/api/notifications`;

  constructor(private http: HttpClient) {}

  getNotifications(): Observable<NotificationDTO[] | NotificationsResponse> {
    return this.http.get<NotificationDTO[] | NotificationsResponse>(this.apiUrl);
  }

  marquerCommeLue(id: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/lue`, {});
  }
}
