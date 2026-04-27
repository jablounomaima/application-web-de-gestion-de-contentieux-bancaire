import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../../environments/environment';

interface Prestataire {
  id: number;
  nom: string;
  prenom?: string;
  email?: string;
  telephone?: string;
  specialite?: string;
  adresse?: string;
}

@Component({
  selector: 'app-designer-avocat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './designer-avocat.component.html',
  styleUrls: ['./designer-avocat.component.scss']
})
export class DesignerAvocatComponent implements OnInit {

  dossierId!: number;
  prestationId!: number;

  avocats: Prestataire[] = [];
  avocatSelectionne: Prestataire | null = null;

  description: string = '';
  dateFinPrevue: string = '';

  chargement = true;
  envoi = false;
  erreur: string | null = null;
  succes: string | null = null;

  private apiUrl = `${environment.apiUrl}/api/agent/dossiers`;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.dossierId   = Number(this.route.snapshot.paramMap.get('dossierId'));
    this.prestationId = Number(this.route.snapshot.paramMap.get('prestationId'));
    this.chargerAvocats();
  }

  chargerAvocats(): void {
    const url = `${this.apiUrl}/${this.dossierId}/prestation/${this.prestationId}/designer-avocat`;
    this.http.get<any>(url).subscribe({
      next: (res) => {
        this.avocats = res.avocats ?? [];
        this.chargement = false;
      },
      error: (err) => {
        this.erreur = err?.error?.error ?? 'Erreur lors du chargement des avocats.';
        this.chargement = false;
      }
    });
  }

  selectionnerAvocat(avocat: Prestataire): void {
    this.avocatSelectionne = avocat;
  }

  formulaireValide(): boolean {
    return !!this.avocatSelectionne && this.description.trim().length > 0;
  }

  soumettre(): void {
    if (!this.formulaireValide()) return;

    this.envoi = true;
    this.erreur = null;
    this.succes = null;

    const payload: any = {
      prestataireId: this.avocatSelectionne!.id,
      description:   this.description.trim()
    };
    if (this.dateFinPrevue) {
      payload.dateFinPrevue = this.dateFinPrevue;
    }

    const url = `${this.apiUrl}/${this.dossierId}/prestation/${this.prestationId}/designer-avocat`;
    this.http.post<any>(url, payload).subscribe({
      next: (res) => {
        this.succes = res?.message ?? 'Avocat désigné avec succès.';
        this.envoi = false;
        setTimeout(() => this.router.navigate(['/agent/dossiers', this.dossierId]), 2000);
      },
      error: (err) => {
        this.erreur = err?.error?.error ?? 'Une erreur est survenue.';
        this.envoi = false;
      }
    });
  }

  annuler(): void {
    this.router.navigate(['/agent/dossiers', this.dossierId]);
  }

  nomComplet(avocat: Prestataire): string {
    return [avocat.prenom, avocat.nom].filter(Boolean).join(' ') || `Avocat #${avocat.id}`;
  }

  initiales(avocat: Prestataire): string {
    const parts = [avocat.prenom, avocat.nom].filter(Boolean);
    return parts.map(p => p![0].toUpperCase()).join('') || 'AV';
  }
}