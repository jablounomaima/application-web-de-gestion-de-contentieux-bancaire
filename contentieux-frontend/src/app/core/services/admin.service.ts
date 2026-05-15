import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// ─── Interfaces ────────────────────────────────────────────────────────────────

export interface Agence {
  id: number;
  code: string;
  nom: string;
  adresse: string;
  ville: string;
  telephone: string;
  email: string;
  directeur: string;
  nombreAgents: number;
}
export type AgenceForm = Omit<Agence, 'id' | 'nombreAgents'>;

export interface Agent {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  username: string;  // ✅ ajouter

  matricule: string;
  telephone: string;
  agenceId: number;
  actif: boolean;
}

export interface Validateur {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  matricule: string;
  telephone: string;
  typeValidateur: 'VALIDATEUR_JURIDIQUE' | 'VALIDATEUR_FINANCIER';
  agenceId: number;
  actif: boolean;
  username?: string;
}

export interface AgentCreationRequest {
  nom: string;
  prenom: string;
  username?: string;
  password?: string;
  email: string;
  matricule: string;
  telephone: string;
  dateEmbauche?: string;
  role?: string;
  agenceId: number;
}

export interface ValidateurCreationRequest {
  nom: string;
  prenom: string;
  username: string;   // ← manquant
  password?: string;  // ← manquant (optionnel pour l'édition)
  email: string;
  matricule: string;
  telephone: string;
  type: string;
  agenceId: number;
}

export interface AgentsResponse {
  agents: Agent[];
  agences: Agence[];
}

export interface ValidateursResponse {
  validateursFinanciers: Validateur[];
  validateursJuridiques: Validateur[];
  agences: Agence[];
}

export interface ApiMessage {
  message?: string;
  error?: string;
}

// ─── Service ───────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class AdminService {

  private api = `${environment.apiUrl}/api/admin`;

  constructor(private http: HttpClient) { }

  // ── Dashboard ──────────────────────────────────────────────────────────────

  getDashboard(): Observable<{ username: string }> {
    return this.http.get<{ username: string }>(`${this.api}/dashboard`);
  }

  // ── Agences ────────────────────────────────────────────────────────────────

  getAgences(): Observable<Agence[]> {
    return this.http.get<Agence[]>(`${this.api}/agences`);
  }

  createAgence(dto: AgenceForm): Observable<ApiMessage> {          // ✅ corrigé
    return this.http.post<ApiMessage>(`${this.api}/agences`, dto);
  }

  updateAgence(id: number, dto: AgenceForm): Observable<ApiMessage> {  // ✅ corrigé
    return this.http.put<ApiMessage>(`${this.api}/agences/${id}`, dto);
  }

  deleteAgence(id: number): Observable<ApiMessage> {
    return this.http.delete<ApiMessage>(`${this.api}/agences/${id}`);
  }

  // ── Agents ─────────────────────────────────────────────────────────────────

  getAgents(): Observable<AgentsResponse> {
    return this.http.get<AgentsResponse>(`${this.api}/agents`);
  }

  getAgentById(id: number): Observable<Agent> {
    return this.http.get<Agent>(`${this.api}/agents/${id}`);
  }

  createAgent(request: AgentCreationRequest): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(`${this.api}/agents`, request);
  }

  updateAgent(id: number, request: AgentCreationRequest): Observable<ApiMessage> {
    return this.http.put<ApiMessage>(`${this.api}/agents/${id}`, request);
  }

  deleteAgent(id: number): Observable<ApiMessage> {
    return this.http.delete<ApiMessage>(`${this.api}/agents/${id}`);
  }

  toggleAgentStatus(id: number): Observable<ApiMessage> {
    return this.http.patch<ApiMessage>(`${this.api}/agents/${id}/toggle`, {});
  }

  // ── Validateurs ────────────────────────────────────────────────────────────

  getValidateurs(): Observable<ValidateursResponse> {
    return this.http.get<ValidateursResponse>(`${this.api}/validateurs`);
  }

  getValidateurById(id: number): Observable<Validateur> {
    return this.http.get<Validateur>(`${this.api}/validateurs/${id}`);
  }

  createValidateur(request: ValidateurCreationRequest): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(`${this.api}/validateurs`, request);
  }

  updateValidateur(id: number, request: ValidateurCreationRequest): Observable<ApiMessage> {
    return this.http.put<ApiMessage>(`${this.api}/validateurs/${id}`, request);
  }

  toggleValidateurStatus(id: number): Observable<ApiMessage & { actif: boolean }> {
    return this.http.patch<ApiMessage & { actif: boolean }>(
      `${this.api}/validateurs/${id}/toggle`, {}
    );
  }
  deleteValidateur(id: number): Observable<ApiMessage> {
    return this.http.delete<ApiMessage>(`${this.api}/validateurs/${id}`);
  }



  // Dans admin.service.ts
reinitialiserMotDePasse(username: string): Observable<any> {
  return this.http.post<any>(
    `${this.api}/reinitialiser-mdp/${username}`, {}
  );
}
}