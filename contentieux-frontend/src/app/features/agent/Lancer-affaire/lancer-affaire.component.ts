import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-lancer-affaire',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './lancer-affaire.component.html',
  styleUrls: ['./lancer-affaire.component.scss']
})
export class LancerAffaireComponent implements OnInit {

  dossierId!: number;

  // Données chargées depuis le backend
  missionId: number | null = null;
  avocat: any = null;
  dossier: any = null;
  mission: any = null;

  // Formulaire
  

  // États
  chargement = true;
  envoi      = false;
  erreur: string | null = null;
  succes: string | null = null;

  private apiUrl = `${environment.apiUrl}/api/agent/dossiers`;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.dossierId = Number(this.route.snapshot.paramMap.get('dossierId'));
    this.chargerFormulaire();
  }

  /** GET /api/agent/dossiers/{dossierId}/affaire/lancer */
  chargerFormulaire(): void {
    this.http.get<any>(`${this.apiUrl}/${this.dossierId}/affaire/lancer`).subscribe({
      next: (res) => {
        this.missionId = res.missionId;
        this.avocat    = res.avocat;
        this.dossier   = res.dossier;
        this.mission   = res.mission;

        this.chargement = false;
      },
      error: (err) => {
        this.erreur     = err?.error?.error ?? 'Impossible de charger les données.';
        this.chargement = false;
      }
    });
  }

  formulaireValide(): boolean {
    return !!this.missionId;
  }

  /** POST /api/agent/dossiers/{dossierId}/affaire/lancer */
  soumettre(): void {
    if (!this.formulaireValide()) return;
  
    this.envoi  = true;
    this.erreur = null;
  
    const payload = { missionId: this.missionId };
  
    this.http.post<any>(`${this.apiUrl}/${this.dossierId}/affaire/lancer`, payload).subscribe({
      next: (res) => {
        this.succes = res?.message ?? 'Affaire lancée avec succès.';
        this.envoi  = false;
        setTimeout(() => this.router.navigate(['/agent/dossiers', this.dossierId]), 2000);
      },
      error: (err) => {
        this.erreur = err?.error?.error ?? 'Une erreur est survenue.';
        this.envoi  = false;
      }
    });}

  annuler(): void {
    this.router.navigate(['/agent/dossiers', this.dossierId]);
  }

  nomAvocat(): string {
    if (!this.avocat) return '—';
    return [this.avocat.prenom, this.avocat.nom].filter(Boolean).join(' ') || `Avocat #${this.avocat.id}`;
  }

  initialesAvocat(): string {
    if (!this.avocat) return 'AV';
    const parts = [this.avocat.prenom, this.avocat.nom].filter(Boolean);
    return parts.map((p: string) => p[0].toUpperCase()).join('') || 'AV';
  }
}