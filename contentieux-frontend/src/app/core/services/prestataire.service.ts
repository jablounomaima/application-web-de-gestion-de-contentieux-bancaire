import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PrestataireService {
  private api = `${environment.apiUrl}/prestataire`;
  private agentApi = `${environment.apiUrl}/agent`;

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<any> {
    return this.http.get(`${this.api}/dashboard`);
  }

  getMissions(): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/missions`);
  }

  getFormPV(missionId: number): Observable<any> {
    return this.http.get(`${this.api}/missions/${missionId}/pv`);
  }

  getFormFacture(missionId: number): Observable<any> {
    return this.http.get(`${this.api}/missions/${missionId}/facture`);
  }

  soumettreResultat(missionId: number, formData: FormData): Observable<any> {
    return this.http.post(`${this.api}/missions/${missionId}/soumettre`, formData);
  }

  soumettreFacture(missionId: number, data: any): Observable<any> {
    return this.http.post(`${this.api}/missions/${missionId}/facture`, data);
  }

  creerPrestataire(request: any): Observable<{ message: string; prestataire: any }> {
    return this.http.post<{ message: string; prestataire: any }>(
      `${this.agentApi}/prestataires`,
      request
    );
  }
}