import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-mission-creer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './missions-liste.component.html',
  styleUrls: ['./missions-liste.component.scss']
})
export class MissionCreerComponent implements OnInit {

  private apiUrl = `${environment.apiUrl}/api/agent/dossiers`;

  dossierId!:   number;
  dossier:      any = null;
  prestataires: any[] = [];
  prestations:  any[] = [];
  prestatairesFiltres: any[] = [];

  loading    = true;
  submitting = false;
  touched    = false;

  successMsg = '';
  errorMsg   = '';
  formError  = '';

  today = new Date().toISOString().split('T')[0];

  // ── Formulaire ─────────────────────────────────────────────────
  form = {
    prestationId:  null as number | null,
    prestataireId: null as number | null,
    description:   '',
    dateFinPrevue: '',
  };

  typeOptions = [
    { value: 'AVOCAT',   label: 'Avocat',   emoji: '⚖️' },
    { value: 'EXPERT',   label: 'Expert',   emoji: '🔬' },
    { value: 'HUISSIER', label: 'Huissier', emoji: '📜' },
  ];

  constructor(
    private route:  ActivatedRoute,
    private router: Router,
    private http:   HttpClient
  ) {}

  ngOnInit(): void {
    this.dossierId = Number(this.route.snapshot.paramMap.get('dossierId'));
    this.charger();
  }

  // ══════════════════════════════════════════════════════════════
  // CHARGEMENT
  // ══════════════════════════════════════════════════════════════

  charger(): void {
    this.loading = true;
    this.http.get<any>(`${this.apiUrl}/${this.dossierId}/missions`).subscribe({
      next: (res) => {
        this.dossier      = res.dossier;
        this.prestataires = res.prestataires || [];
        this.prestations  = res.prestations  || [];
        this.prestatairesFiltres = [...this.prestataires];
        this.loading = false;
      },
      error: (err) => {
        this.errorMsg = err?.error?.error || 'Impossible de charger les données.';
        this.loading  = false;
      }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // SOUMISSION
  // ══════════════════════════════════════════════════════════════

  soumettre(): void {
    this.touched   = true;
    this.formError = '';

    if (!this.valider()) return;

    this.submitting = true;

    const payload = {
      prestationId:  this.form.prestationId,
      prestataireId: this.form.prestataireId,
      description:   this.form.description.trim(),
      dateFinPrevue: this.form.dateFinPrevue || null,
    };

    this.http.post<any>(`${this.apiUrl}/${this.dossierId}/missions/creer`, payload)
      .subscribe({
        next: (res) => {
          this.submitting = false;
          this.successMsg = `✅ Mission ${res.mission?.numeroMission || ''} créée avec succès !`;
          setTimeout(() => this.router.navigate(['/agent/dossiers', this.dossierId, 'missions']), 2000);
        },
        error: (err) => {
          this.submitting = false;
          this.errorMsg   = err?.error?.error || 'Une erreur est survenue.';
        }
      });
  }

  // ══════════════════════════════════════════════════════════════
  // VALIDATION
  // ══════════════════════════════════════════════════════════════

  private valider(): boolean {
    if (!this.form.prestationId)  { this.formError = 'Veuillez sélectionner une prestation.'; return false; }
    if (!this.form.prestataireId) { this.formError = 'Veuillez sélectionner un prestataire.'; return false; }
    if (!this.form.description.trim()) { this.formError = 'La description est obligatoire.'; return false; }
    return true;
  }

  // ══════════════════════════════════════════════════════════════
  // HELPERS
  // ══════════════════════════════════════════════════════════════

  selectionnerPrestation(id: number): void {
    this.form.prestationId  = id;
    this.form.prestataireId = null;
    // Filtrer les prestataires selon le type de prestation
    const presta = this.prestations.find(p => p.id === id);
    if (presta) {
      const typeMap: Record<string, string> = {
        'PROCEDURE_JUDICIAIRE': 'AVOCAT',
        'EXPERTISE':            'EXPERT',
        'SIGNIFICATION':        'HUISSIER',
      };
      const typeAttendu = typeMap[presta.type];
      this.prestatairesFiltres = typeAttendu
        ? this.prestataires.filter(p => p.type === typeAttendu && p.actif)
        : this.prestataires.filter(p => p.actif);
    } else {
      this.prestatairesFiltres = this.prestataires.filter(p => p.actif);
    }
  }

  selectionnerPrestataire(id: number): void {
    this.form.prestataireId = id;
  }

  retour(): void {
    this.router.navigate(['/agent/dossiers', this.dossierId, 'missions']);
  }

  getLabelPrestation(p: any): string {
    const map: Record<string, string> = {
      'PROCEDURE_JUDICIAIRE': '⚖️ Procédure judiciaire',
      'EXPERTISE':            '🔬 Expertise',
      'SIGNIFICATION':        '📜 Signification',
      'RECOUVREMENT':         '💰 Recouvrement',
    };
    return map[p.type] || p.type;
  }

  getLabelType(type: string): string {
    return this.typeOptions.find(t => t.value === type)?.label || type;
  }

  getEmojiType(type: string): string {
    return this.typeOptions.find(t => t.value === type)?.emoji || '👤';
  }

  nomPrestataire(p: any): string {
    return `${p.prenom || ''} ${p.nom || ''}`.trim() || p.username;
  }

  progression(): number {
    const checks = [
      !!this.form.prestationId,
      !!this.form.prestataireId,
      !!this.form.description.trim(),
    ];
    return Math.round(checks.filter(Boolean).length / checks.length * 100);
  }

  getPrestataireSelectionne(): any {
    return this.prestataires.find(p => p.id === this.form.prestataireId);
  }

  getPrestationSelectionnee(): any {
    return this.prestations.find(p => p.id === this.form.prestationId);
  }
}