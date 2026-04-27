// ════════════════════════════════════════════════════════════════════
// client.service.ts — VERSION CORRIGÉE
//
// Correction : apiUrl pointe maintenant vers /api/agent/clients
// ce qui correspond au @PutMapping("/clients/{id}") de AgentController
// sous @RequestMapping("/api/agent")
// ════════════════════════════════════════════════════════════════════

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Client {
  id?: number;
  typeClient: string;
  nom: string;
  prenom: string;
  raisonSociale: string;
  cin: string;
  rne: string;
  email: string;
  telephone: string;
  adresse: string;
}

@Injectable({ providedIn: 'root' })
export class ClientService {

  // ✅ CORRECTION : /api/agent/clients  (pas /api/clients)
  // AgentController est sous @RequestMapping("/api/agent")
  // et SecurityConfig autorise /api/agent/** pour ROLE_AGENT
  private apiUrl = `${environment.apiUrl}/api/agent/clients`;

  constructor(private http: HttpClient) {}

  getClients(): Observable<Client[]> {
    return this.http.get<Client[]>(this.apiUrl);
  }

  getClientById(id: number): Observable<Client> {
    return this.http.get<Client>(`${this.apiUrl}/${id}`);
  }

  creerClient(data: Omit<Client, 'id'>): Observable<any> {
    return this.http.post(`${this.apiUrl}/creer`, data);
  }

  // ✅ Appelle PUT /api/agent/clients/{id}
  // → AgentController.updateClient() → ClientService.modifierClient()
  modifierClient(id: number, data: Omit<Client, 'id'>): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, data);
  }

  supprimerClient(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  getDossiersClient(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}/dossiers`);
  }
}