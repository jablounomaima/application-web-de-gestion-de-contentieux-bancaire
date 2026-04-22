import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Dossier {
  id: number;
  numeroDossier: string;
  statut: string;
  typeDossier: string;
  dateCreation: string;
  montantCreance?: number;
  client?: {
    nom?: string;
    prenom?: string;
    raisonSociale?: string;
    typeClient: string;
  };
}

export interface DossierCreation {
  clientId?: number;
  nomClient: string;
  prenomClient?: string;
  typeClient: 'PHYSIQUE' | 'MORALE';
  cin?: string;
  raisonSociale?: string;
  matriculeFiscal?: string;
  typeDossier: string;
  montantCreance: number;
  description?: string;
}

@Injectable({
  providedIn: 'root'
})
export class DossierService {
  private apiUrl = `${environment.apiUrl}/agent`;

  constructor(private http: HttpClient) {}

  getAllDossiers(): Observable<Dossier[]> {
    return this.http.get<Dossier[]>(`${this.apiUrl}/dossiers`);
  }

  getDossierDetails(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/dossiers/${id}`);
  }

  creerDossier(dossier: DossierCreation): Observable<any> {
    return this.http.post(`${this.apiUrl}/dossiers/creer`, dossier);
  }

  lancerPrestation(dossierId: number, data: { type: string; description: string }): Observable<any> {
    return this.http.post(`${environment.apiUrl}/agent/dossiers/${dossierId}/prestations`, data);
  }

  getFormLancer(dossierId: number): Observable<any> {
    return this.http.get(`${environment.apiUrl}/agent/dossiers/${dossierId}/prestations/lancer`);
  }

  getDetailPrestation(dossierId: number, prestationId: number): Observable<any> {
    return this.http.get(`${environment.apiUrl}/agent/dossiers/${dossierId}/prestations/${prestationId}`);
  }

  designerPrestataire(dossierId: number, prestationId: number, data: any): Observable<any> {
    return this.http.post(`${environment.apiUrl}/agent/dossiers/${dossierId}/prestations/${prestationId}/designer`, data);
  }

  telechargerPdf(dossierId: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/agent/dossiers/${dossierId}/pdf`, { responseType: 'blob' });
  }
}
