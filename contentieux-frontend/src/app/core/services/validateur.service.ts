import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ValidateurService {

  private readonly apiUrl = `${environment.apiUrl}/api/validateur`;

  constructor(private http: HttpClient) {}

  // ═══════════════════════════════
  //  FINANCIER
  // ═══════════════════════════════

  getDashboardFinancier(): Observable<any> {
    return this.http.get(`${this.apiUrl}/dashboard-financier`);
  }

  getDossiersFinancier(recherche: string = ''): Observable<any> {
    let params = new HttpParams();
    if (recherche.trim()) {
      params = params.set('recherche', recherche.trim());
    }
    return this.http.get(`${this.apiUrl}/financier/dossiers`, { params });
  }

  getDossierDetailFinancier(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/financier/dossiers/${id}`);
  }

  validerFinancier(id: number, commentaire: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/financier/dossiers/${id}/valider`, { commentaire });
  }

  rejeterFinancier(id: number, commentaire: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/financier/dossiers/${id}/rejeter`, { commentaire });
  }

  // ═══════════════════════════════
  //  JURIDIQUE
  // ═══════════════════════════════

  getDashboardJuridique(): Observable<any> {
    return this.http.get(`${this.apiUrl}/dashboard-juridique`);
  }

  getDossiersJuridique(recherche: string = ''): Observable<any> {
    let params = new HttpParams();
    if (recherche.trim()) {
      params = params.set('recherche', recherche.trim());
    }
    return this.http.get(`${this.apiUrl}/juridique/dossiers-juridique`, { params });
  }

  getDossierDetailJuridique(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/juridique/dossiers/${id}`);
  }

  validerJuridique(id: number, commentaire: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/juridique/dossiers/${id}/valider`, { commentaire });
  }

  rejeterJuridique(id: number, commentaire: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/juridique/dossiers/${id}/rejeter`, { commentaire });
  }
}