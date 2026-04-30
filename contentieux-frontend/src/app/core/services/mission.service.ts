import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Mission {
  id?: number;
  numeroMission: string;
  typeMission: string;
  dateAssignation: string;
  statut: string;
  pvMission?: string;
  factureRef?: string;
  montantFacture?: number;
}

@Injectable({
  providedIn: 'root'
})
export class MissionService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // Pour l'agent
  getMissionsAgent(dossierId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/agent/dossiers/${dossierId}/missions`);
  }

  creerMission(dossierId: number, missionData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/agent/dossiers/${dossierId}/missions/creer`, missionData);

  }

  // ✅ Ajouter — liste des prestataires de l'agent
getPrestataires(): Observable<any> {
  return this.http.get(`${this.apiUrl}/agent/prestataires`);
}

// ✅ Ajouter — liste des prestations du dossier
getPrestationsDossier(dossierId: number): Observable<any> {
  return this.http.get(`${this.apiUrl}/agent/dossiers/${dossierId}/missions`);
}

  // Pour le prestataire (expert, huissier, etc.)
  getDashboardPrestataire(): Observable<any> {
    return this.http.get(`${this.apiUrl}/prestataire/dashboard`);
  }

  soumettrePV(missionId: number, pvTexte: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/prestataire/missions/${missionId}/pv`, { pvTexte });
  }

  soumettreFacture(missionId: number, factureRef: string, montant: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/prestataire/missions/${missionId}/facture`, { factureRef, montant });
  }
}
