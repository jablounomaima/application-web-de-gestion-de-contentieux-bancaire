import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface DossierContentieux {
  id: number;
  numeroDossier: string;
  typeDossier: string;
  statut: string;
  dateCreation: string;
  montantReclame: number;
  client?: {
    id: number;
    nom: string;
    prenom: string;
    email: string;
    telephone: string;
  };
  creancier?: {
    id: number;
    nom: string;
    prenom: string;
  };
}

export interface Mission {
  id: number;
  numeroMission: string;
  statut: string;
  dateAssignation: string;
  dateEcheance: string;
  prestation?: {
    id: number;
    typePrestation: string;
  };
}

export interface Audience {
  id: number;
  dateAudience: string;
  heure: string;
  salle: string;
  motif: string;
  statut: string;
  resultat: string | null;
  prochaineAudience: string | null;
}

export interface DocumentAffaire {
  id: number;
  nomFichierOriginal: string;
  typeDocument: string;
  description: string;
  dateUpload: string;
  uploadeePar: string;
  typeMime?: string;
}

export interface AffaireJudiciaire {
  id: number;
  numeroAffaire: string;
  statut: string;
  dateLancement: string;
  tribunal: string | null;
  chambre: string | null;
  numeroRole: string | null;
  dateProchainAudience: string | null;
  typeJugement: string | null;
  dateJugement: string | null;
  montantJuge: string | null;
  delaiPaiementJuge: string | null;
  descriptionJugement: string | null;
  dossier: DossierContentieux;
  mission: Mission;
  audiences: Audience[];
  documents: DocumentAffaire[];
}

export interface DossierAvocatDTO {
  affaireId: number;
  numeroAffaire: string;
  numeroDossier: string;
  typeDossier: string;
  statutAffaire: string;
  statutMission: string;
  clientNom: string;
  clientPrenom: string;
  clientTelephone: string;
  montantReclame: number;
  tribunal: string | null;
  chambre: string | null;
  numeroRole: string | null;
  dateProchainAudience: string | null;
  nombreAudiences: number;
  nombreDocuments: number;
  dateLancement: string;
  dateEcheanceMission: string | null;
  pvSoumis: boolean;
  factureSoumise: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class DossiersAvocatService {
  private readonly API_URL = '/api/affaires';

  constructor(private http: HttpClient) {}

  getAffairesParAvocat(): Observable<AffaireJudiciaire[]> {
    return this.http.get<AffaireJudiciaire[]>(`${this.API_URL}/mes-affaires`);
  }

  getDossiersAssignes(): Observable<DossierAvocatDTO[]> {
    return this.getAffairesParAvocat().pipe(
      map((affaires: AffaireJudiciaire[]) => affaires.map((affaire: AffaireJudiciaire) => this.mapperVersDTO(affaire)))
    );
  }

  getAffaireDetail(affaireId: number): Observable<AffaireJudiciaire> {
    return this.http.get<AffaireJudiciaire>(`${this.API_URL}/${affaireId}`);
  }

  soumettrePV(affaireId: number, pvTexte: string): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/${affaireId}/pv`, { pvTexte });
  }

  soumettreFacture(affaireId: number, factureRef: string, montant: number): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/${affaireId}/facture`, { factureRef, montant });
  }

  private mapperVersDTO(affaire: AffaireJudiciaire): DossierAvocatDTO {
    const dossier = affaire.dossier;
    const mission = affaire.mission;
    const client = dossier?.client;

    return {
      affaireId: affaire.id,
      numeroAffaire: affaire.numeroAffaire,
      numeroDossier: dossier?.numeroDossier || 'N/A',
      typeDossier: dossier?.typeDossier || 'N/A',
      statutAffaire: affaire.statut,
      statutMission: mission?.statut || 'N/A',
      clientNom: client?.nom || 'N/A',
      clientPrenom: client?.prenom || '',
      clientTelephone: client?.telephone || 'N/A',
      montantReclame: dossier?.montantReclame || 0,
      tribunal: affaire.tribunal,
      chambre: affaire.chambre,
      numeroRole: affaire.numeroRole,
      dateProchainAudience: affaire.dateProchainAudience,
      nombreAudiences: affaire.audiences?.length || 0,
      nombreDocuments: affaire.documents?.length || 0,
      dateLancement: affaire.dateLancement,
      dateEcheanceMission: mission?.dateEcheance || null,
      pvSoumis: mission?.statut === 'PV_SOUMIS' || mission?.statut === 'FACTURE_SOUMISE',
      factureSoumise: mission?.statut === 'FACTURE_SOUMISE'
    };
  }
}