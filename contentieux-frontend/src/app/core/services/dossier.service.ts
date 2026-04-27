import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DossierService {

  private apiUrl = `${environment.apiUrl}/api/agent/dossiers`;
  private clientApiUrl = `${environment.apiUrl}/api/agent/clients`;

  private lancerapiUrl = 'http://localhost:8080/api'; 
  constructor(private http: HttpClient) {}

  // ─── Lecture ─────────────────────────────────────────────────────────────────
  getAllDossiers(): Observable<any> {
    return this.http.get<any>(this.apiUrl).pipe(
      map(res => res.dossiers ?? res)
    );
  }

  getDossierDetails(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  // ─── Dossier ─────────────────────────────────────────────────────────────────
  creerDossier(dossier: any): Observable<any> {
    return this.http.post(this.apiUrl, dossier);
  }

  modifierDossier(id: number, dossier: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, dossier);
  }

  supprimerDossier(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  // ─── Workflow ────────────────────────────────────────────────────────────────
  soumettreDossier(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/soumettre`, {});
  }

  choisirValidateurs(id: number, data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/choisir-validateurs`, data);
  }

  soumettreAValidation(id: number): Observable<any> {
    return this.soumettreDossier(id);
  }

  // ─── Risques ─────────────────────────────────────────────────────────────────
  ajouterRisque(dossierId: number, data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/${dossierId}/risques`, data);
  }

  modifierRisque(dossierId: number, risqueId: number, data: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/${dossierId}/risques/${risqueId}`, data);
  }

  supprimerRisque(dossierId: number, risqueId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${dossierId}/risques/${risqueId}`);
  }

  selectionnerRisque(dossierId: number, risqueId: number, selectionne: boolean): Observable<any> {
    return this.http.patch(`${this.apiUrl}/${dossierId}/risques/${risqueId}/selectionner`, { selectionne });
  }

  // ─── Garanties ───────────────────────────────────────────────────────────────
  ajouterGarantie(risqueId: number, data: any): Observable<any> {
    const url = `${environment.apiUrl}/api/agent/dossiers/risques/${risqueId}/garanties`;
    console.log('➕ POST Garantie:', url, data);
    return this.http.post(url, data);
  }

  modifierGarantie(garantieId: number, data: any): Observable<any> {
    const url = `${environment.apiUrl}/api/agent/dossiers/garanties/${garantieId}`;
    console.log('🔧 PUT Garantie:', url, data);
    return this.http.put(url, data);
  }

  supprimerGarantie(garantieId: number): Observable<any> {
    const url = `${environment.apiUrl}/api/agent/dossiers/garanties/${garantieId}`;
    console.log('🗑️ DELETE Garantie:', url);
    return this.http.delete(url);
  }

  // ─── PDF ─────────────────────────────────────────────────────────────────────
  telechargerPdf(dossierId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${dossierId}/pdf`, { responseType: 'blob' });
  }


//------------------------------------------------
ressoumettreDossier(id: number): Observable<any> {
  return this.http.post(`${this.apiUrl}/${id}/ressoumettre`, {});
}



/**
 * Lance une procédure judiciaire pour un dossier validé.
 * Correspond à POST /api/agent/dossiers/{dossierId}/prestations/lancer
 */
// Correspond à :
//   POST /api/agent/dossiers/{dossierId}/prestations/lancer
// ═══════════════════════════════════════════════════════════════════
 

// Après
lancerProcedureJudiciaire(dossierId: number, payload: any): Observable<any> {
  return this.http.post(`${this.apiUrl}/${dossierId}/prestations`, payload);
}


}