import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

// ─── Interfaces ────────────────────────────────────────────────────────────────

/**
 * Représente un dossier contentieux dans la liste.
 * Correspond à l'entité DossierContentieux côté Spring Boot.
 */
export interface Dossier {
  id: number;
  numeroDossier: string;
  statut: string;          // EN_ATTENTE | EN_COURS | VALIDE | REJETE | CLOS
  typeDossier: string;     // CREDIT_IMMOBILIER | CREDIT_CONSOMMATION | ...
  dateCreation: string;
  montantCreance?: number;
  client?: {
    nom?: string;
    prenom?: string;
    raisonSociale?: string;
    typeClient: string;    // PHYSIQUE | MORALE
  };
}

/**
 * Payload envoyé au backend pour créer un nouveau dossier.
 * Correspond à DossierCreationRequest.java côté Spring Boot.
 */
export interface DossierCreation {
  clientId?: number;        // Si le client existe déjà en base
  nomClient?: string;       // Sinon, saisie manuelle
  prenomClient?: string;
  typeClient: 'PARTICULIER' | 'ENTREPRISE';
  cin?: string;             // Pour personne physique
  raisonSociale?: string;   // Pour personne morale
  matriculeFiscal?: string;
  typeDossier: string;
  montantCreance: number;
  description?: string;
}

// ─── Service ───────────────────────────────────────────────────────────────────

/**
 * Service Angular responsable de toutes les opérations HTTP
 * liées aux dossiers contentieux.
 *
 * Base URL : /api/agent/dossiers  (définie dans DossierController.java)
 * Toutes les requêtes incluent automatiquement le Bearer Token Keycloak
 * via l'intercepteur authTokenInterceptor.
 */
@Injectable({
  providedIn: 'root'
})
export class DossierService {

  /**
   * URL de base correspondant à @RequestMapping("/api/agent/dossiers")
   * dans DossierController.java
   */
  private apiUrl = `${environment.apiUrl}/api/agent/dossiers`;

  constructor(private http: HttpClient) {}

  // ─── Lecture ─────────────────────────────────────────────────────────────────

  /**
   * Récupère tous les dossiers de l'agent connecté.
   * GET /api/agent/dossiers
   *
   * Le backend retourne { dossiers: [...], totalDossiers: N }
   * On extrait uniquement le tableau via map().
   */
  getAllDossiers(): Observable<Dossier[]> {
    return this.http.get<any>(this.apiUrl).pipe(
      map(res => {
        console.log('Réponse brute API →', res);        // ← AJOUTEZ
        console.log('Dossiers extraits →', res.dossiers); // ← AJOUTEZ
        return res.dossiers ?? res;
      })
    );
  }

  /**
   * Récupère le détail complet d'un dossier par son ID.
   * GET /api/agent/dossiers/{id}
   *
   * Retourne : { dossier, validateurs_financiers, validateurs_juridiques,
   *              historique, missionAvocat, affaireExiste, prestationJudiciaire }
   */
  getDossierDetails(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  // ─── Création / Modification ──────────────────────────────────────────────────

  /**
   * Crée un nouveau dossier contentieux.
   * POST /api/agent/dossiers
   *
   * ATTENTION : l'ancien endpoint /creer n'existe pas côté backend.
   * Le backend utilise POST sur la racine @PostMapping dans DossierController.
   */
  creerDossier(dossier: any): Observable<any> {
    return this.http.post(this.apiUrl, dossier);
  }

  /**
   * Modifie un dossier existant.
   * PUT /api/agent/dossiers/{id}
   */
  modifierDossier(id: number, dossier: DossierCreation): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, dossier);
  }

  /**
   * Supprime un dossier.
   * DELETE /api/agent/dossiers/{id}
   */
  supprimerDossier(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  // ─── Workflow ────────────────────────────────────────────────────────────────

  /**
   * Soumet le dossier à la validation (changement de statut).
   * POST /api/agent/dossiers/{id}/soumettre
   */
  soumettreDossier(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/soumettre`, {});
  }

  /**
   * Assigne les validateurs financier et juridique au dossier.
   * POST /api/agent/dossiers/{id}/choisir-validateurs
   */
  choisirValidateurs(id: number, data: { validateurFinancier: string; validateurJuridique: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/choisir-validateurs`, data);
  }

  // ─── Prestations ─────────────────────────────────────────────────────────────

  /**
   * Lance une nouvelle prestation sur un dossier.
   * POST /api/agent/dossiers/{dossierId}/prestations
   */
  lancerPrestation(dossierId: number, data: { type: string; description: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/${dossierId}/prestations`, data);
  }

  /**
   * Récupère les données du formulaire de lancement de prestation.
   * GET /api/agent/dossiers/{dossierId}/prestations/lancer
   */
  getFormLancer(dossierId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${dossierId}/prestations/lancer`);
  }

  /**
   * Récupère le détail d'une prestation (avec liste des prestataires disponibles).
   * GET /api/agent/dossiers/{dossierId}/prestations/{prestationId}
   */
  getDetailPrestation(dossierId: number, prestationId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${dossierId}/prestations/${prestationId}`);
  }

  /**
   * Désigne un prestataire pour une mission liée à une prestation.
   * POST /api/agent/dossiers/{dossierId}/prestations/{prestationId}/designer
   */
  designerPrestataire(dossierId: number, prestationId: number, data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/${dossierId}/prestations/${prestationId}/designer`, data);
  }

  // ─── PDF ─────────────────────────────────────────────────────────────────────

  /**
   * Télécharge le PDF du dossier.
   * GET /api/agent/dossiers/{dossierId}/pdf
   * Retourne un Blob pour le téléchargement côté navigateur.
   */
  telechargerPdf(dossierId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${dossierId}/pdf`, { responseType: 'blob' });
  }
  // ─── Risques ─────────────────────────────────────────────

selectionnerRisque(dossierId: number, risqueId: number, selectionne: boolean): Observable<any> {
  return this.http.post(
    `${this.apiUrl}/${dossierId}/risques/${risqueId}/selection`,
    { selectionne }
  );
}

ajouterRisque(dossierId: number, data: any): Observable<any> {
  return this.http.post(`${this.apiUrl}/${dossierId}/risques`, data);
}

// ─── Garanties ───────────────────────────────────────────

supprimerGarantie(garantieId: number): Observable<any> {
  return this.http.delete(`${environment.apiUrl}/api/garanties/${garantieId}`);
}

// ─── Alias pour compatibilité avec ton component ─────────

// ton component appelle soumettreAValidation → on map vers soumettreDossier
soumettreAValidation(id: number): Observable<any> {
  return this.soumettreDossier(id);
}
}