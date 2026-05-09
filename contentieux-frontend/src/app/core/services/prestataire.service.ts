import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PrestataireService {
  private api      = `${environment.apiUrl}/api/prestataire`;
  private agentApi = `${environment.apiUrl}/api/agent`;

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<any> {
    return this.http.get(`${this.api}/dashboard`);
  }

  getMissions(recherche?: string): Observable<any> {
    const params = recherche ? `?recherche=${encodeURIComponent(recherche)}` : '';
    return this.http.get<any>(`${this.api}/missions${params}`);
  }

 

  soumettreResultat(missionId: number, formData: FormData): Observable<any> {
    return this.http.post(`${this.api}/missions/${missionId}/resultat`, formData);
  }

  modifierResultat(missionId: number, formData: FormData): Observable<any> {
    return this.http.put(`${this.api}/missions/${missionId}/resultat`, formData);
  }

  // ✅ Méthode ajoutée — utilisée par prestataire-dashboard
  soumettreFacture(missionId: number, data: {
    factureRef: string;
    montant: number;
    pvTexte?: string;
  }): Observable<any> {
    return this.http.post(`${this.api}/missions/${missionId}/facture`, data);
  }

  

  telechargerFichierParId(fichierId: number): Observable<Blob> {
    return this.http.get(
      `${this.api}/missions/fichier/id/${fichierId}`,
      { responseType: 'blob' }
    );
  }

  creerPrestataire(request: any): Observable<{ message: string; prestataire: any }> {
    return this.http.post<{ message: string; prestataire: any }>(
      `${this.agentApi}/prestataires`,
      request
    );
  }

  getDossierMission(missionId: number): Observable<any> {
    return this.http.get<any>(`${this.api}/missions/${missionId}/dossier`);
  }

  soumettresPV(missionId: number, pvTexte: string): Observable<any> {
    return this.http.post<any>(
      `${this.api}/missions/${missionId}/pv`,
      { pvTexte }  // ✅ JSON, pas FormData
    );
  }


  uploaderDocuments(missionId: number, formData: FormData): Observable<any> {
    // ⚠️ NE PAS ajouter Content-Type manuellement — Angular le gère seul
    return this.http.post(
      `${this.api}/missions/${missionId}/documents`,
      formData
      // pas de headers ici
    );
  }
  
  getDocuments(missionId: number): Observable<any> {
    return this.http.get(`${this.api}/missions/${missionId}/documents`);
  }
  
  telechargerFichier(nomServeur: string): Observable<Blob> {
    return this.http.get(
      `${this.api}/documents/${nomServeur}`,
      { responseType: 'blob' }
    );
  }

  getResultatMission(missionId: number): Observable<any> {
    return this.http.get(`${this.api}/missions/${missionId}/resultat`);
  }
  
  getMissionDetail(missionId: number): Observable<any> {
    return this.http.get(`${this.api}/missions/${missionId}`);
  }


}