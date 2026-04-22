import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ValidateurService {
  private api = `${environment.apiUrl}/validateur`;

  constructor(private http: HttpClient) {}

  // Financier
  getDashboardFinancier(): Observable<any> {
    return this.http.get(`${this.api}/dashboard-financier`);
  }

  getDossiersFinancier(recherche?: string): Observable<any> {
    const params = recherche ? `?recherche=${recherche}` : '';
    return this.http.get(`${this.api}/financier/dossiers${params}`);
  }

  getDossierDetailFinancier(dossierId: number): Observable<any> {
    return this.http.get(`${this.api}/financier/dossiers/${dossierId}`);
  }

  validerFinancier(dossierId: number, commentaire: string): Observable<any> {
    return this.http.post(`${this.api}/financier/dossiers/${dossierId}/valider`, { commentaire });
  }

  rejeterFinancier(dossierId: number, motif: string): Observable<any> {
    return this.http.post(`${this.api}/financier/dossiers/${dossierId}/rejeter`, { motif });
  }

  // Juridique
  getDashboardJuridique(): Observable<any> {
    return this.http.get(`${this.api}/dashboard-juridique`);
  }

  getDossiersJuridique(recherche?: string): Observable<any> {
    const params = recherche ? `?recherche=${recherche}` : '';
    return this.http.get(`${this.api}/juridique/dossiers${params}`);
  }

  validerJuridique(dossierId: number, commentaire: string): Observable<any> {
    return this.http.post(`${this.api}/juridique/dossiers/${dossierId}/valider`, { commentaire });
  }

  rejeterJuridique(dossierId: number, motif: string): Observable<any> {
    return this.http.post(`${this.api}/juridique/dossiers/${dossierId}/rejeter`, { motif });
  }
}
