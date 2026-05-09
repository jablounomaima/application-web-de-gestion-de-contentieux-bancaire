import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AvocatService {
  private api = `${environment.apiUrl}/api/avocat/affaires`;
  constructor(private http: HttpClient) {}



  getDashboard(): Observable<any> {
    return this.http.get(`${this.api}/dashboard`);
  }

  getAffaires(): Observable<any> {
    return this.http.get(this.api);
  }

  getAffaireDetail(id: number): Observable<any> {
    return this.http.get(`${this.api}/${id}`);
  }

  getAudiences(affaireId: number): Observable<any> {
    return this.http.get(`${this.api}/${affaireId}/audiences`);
  }

  ajouterAudience(affaireId: number, body: any): Observable<any> {
    return this.http.post(`${this.api}/${affaireId}/audiences`, body);
  }

  modifierAudience(affaireId: number, audienceId: number, body: any): Observable<any> {
    return this.http.put(`${this.api}/${affaireId}/audiences/${audienceId}`, body);
  }

  supprimerAudience(affaireId: number, audienceId: number): Observable<any> {
    return this.http.delete(`${this.api}/${affaireId}/audiences/${audienceId}`);
  }

  enregistrerJugement(affaireId: number, body: any): Observable<any> {
    return this.http.post(`${this.api}/${affaireId}/jugement`, body);
  }

  soumettrePV(affaireId: number, pvTexte: string): Observable<any> {
    return this.http.post(`${this.api}/${affaireId}/pv`, { pvTexte });
  }

  soumettreFacture(affaireId: number, body: { factureRef: string; montantFacture: number }): Observable<any> {
    return this.http.post(`${this.api}/${affaireId}/facture`, body);
  }
  modifierTribunal(affaireId: number, body: any): Observable<any> {
    return this.http.post(`${this.api}/${affaireId}/tribunal`, body);
  }

  soumettrePVAvecFichiers(affaireId: number, formData: FormData): Observable<any> {
    console.log('URL appelée:', `${this.api}/${affaireId}/pv/fichiers`); // ✅
    return this.http.post(
      `${this.api}/${affaireId}/pv/fichiers`,
      formData
    );
  }


  getDossierAffaire(affaireId: number): Observable<any> {
    return this.http.get(`${this.api}/${affaireId}/dossier`);
  }

  supprimerFichierPV(affaireId: number, index: number): Observable<any> {
    return this.http.delete(`${this.api}/${affaireId}/pv/fichiers/${index}`);
  }


  supprimerJugement(affaireId: number): Observable<any> {
    return this.http.delete(`${this.api}/${affaireId}/jugement`);
  }
   
}
