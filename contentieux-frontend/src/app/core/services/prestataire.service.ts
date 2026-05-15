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

  soumettreFacture(missionId: number, data: {
    factureRef: string;
    montant: number;
    pvTexte?: string;
  }): Observable<any> {
    return this.http.post(`${this.api}/missions/${missionId}/facture`, data);
  }

  // ✅ CAS 1 — Rejet agent : PV + facture + fichiers → /resoumettre-pv
  resoumettreApresRejetAgent(missionId: number, data: {
    pvTexte: string;
    factureRef: string;
    montant: number;
    fichiers?: File[];
  }): Observable<any> {
    const fd = new FormData();
    fd.append('pvTexte',    data.pvTexte);
    fd.append('factureRef', data.factureRef);
    fd.append('montant',    String(data.montant));
    (data.fichiers || []).forEach(f => fd.append('fichiers', f));
    return this.http.post(`${this.api}/missions/${missionId}/resoumettre-pv`, fd);
  }

  // ✅ CAS 2 — Rejet financier : facture seule + fichiers → /resoumettre-facture
  resoumettreFactureApresRejetFinancier(missionId: number, data: {
    factureRef: string;
    montant: number;
    fichiers?: File[];
  }): Observable<any> {
    const fd = new FormData();
    fd.append('factureRef', data.factureRef);
    fd.append('montant',    String(data.montant));
    (data.fichiers || []).forEach(f => fd.append('fichiers', f));
    return this.http.post(`${this.api}/missions/${missionId}/resoumettre-facture`, fd);
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
      { pvTexte }
    );
  }

  uploaderDocuments(missionId: number, formData: FormData): Observable<any> {
    return this.http.post(`${this.api}/missions/${missionId}/documents`, formData);
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

  getMesFactures(): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/factures`);
  }
}