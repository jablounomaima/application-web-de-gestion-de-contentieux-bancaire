import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-creer-prestataire',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './creer-prestataire.component.html',
  styleUrls: ['./creer-prestataire.component.scss']
})
export class CreerPrestataireComponent {

  // ── API ────────────────────────────────────────────────────────
  private apiUrl = `${environment.apiUrl}/api/agent/prestataires`;

  // ── États ──────────────────────────────────────────────────────
  submitting  = false;
  touched     = false;
  showPassword = false;

  // ── Messages ───────────────────────────────────────────────────
  successMsg = '';
  errorMsg   = '';
  formError  = '';

  // ── Types de prestataire (alignés avec l'enum Java) ────────────
  typeOptions = [
    {
      value: 'AVOCAT',
      label: 'Avocat',
      emoji: '⚖️',
      desc:  'Représentation juridique et procédures judiciaires'
    },
    {
      value: 'EXPERT',
      label: 'Expert judiciaire',
      emoji: '🔬',
      desc:  'Évaluation et expertise technique'
    },
    {
      value: 'HUISSIER',
      label: 'Huissier',
      emoji: '📜',
      desc:  'Signification et exécution des actes'
    },
  ];

  // ── Formulaire ─────────────────────────────────────────────────
  form = {
    username:        '',
    motDePasse:      '',
    prenom:          '',
    nom:             '',
    email:           '',
    telephone:       '',
    adresse:         '',
    specialite:      '',
    numeroCartePro:  '',
    typePrestataire: '' as string,
  };

  constructor(
    private router: Router,
    private http: HttpClient
  ) {}

  // ══════════════════════════════════════════════════════════════
  // ACTIONS
  // ══════════════════════════════════════════════════════════════

  selectionnerType(type: string): void {
    this.form.typePrestataire = type;
  }

  soumettre(): void {
    this.touched   = true;
    this.formError = '';

    if (!this.valider()) return;

    this.submitting = true;
    this.errorMsg   = '';
    this.successMsg = '';

    const payload: any = {
      username:        this.form.username.trim(),
      prenom:          this.form.prenom.trim(),
      nom:             this.form.nom.trim(),
      email:           this.form.email.trim(),
      telephone:       this.form.telephone.trim()      || null,
      adresse:         this.form.adresse.trim()        || null,
      specialite:      this.form.specialite.trim()     || null,
      numeroCartePro:  this.form.numeroCartePro.trim() || null,
      typePrestataire: this.form.typePrestataire,
      // ✅ motDePasse toujours inclus — null si vide
      // Le backend génère automatiquement si null
      motDePasse: this.form.motDePasse.trim() || null,
    };

    this.http.post<any>(this.apiUrl, payload).subscribe({
      next: (res) => {
        this.submitting = false;
        // ✅ Message mis à jour — mentionne l'envoi d'email
        const prenom = res.prestataire?.prenom || '';
        const nom    = res.prestataire?.nom    || '';
        this.successMsg = `Prestataire "${prenom} ${nom}" créé avec succès ! `
                        + `Les identifiants ont été envoyés à ${this.form.email}.`;
        setTimeout(() => this.router.navigate(['/agent/prestataires']), 3000);
      },
      error: (err) => {
        this.submitting = false;
        this.errorMsg = err?.error?.error || err?.error?.message || 'Une erreur est survenue.';
      }
    });
  }

  reinitialiser(): void {
    this.form = {
      username:        '',
      motDePasse:      '',
      prenom:          '',
      nom:             '',
      email:           '',
      telephone:       '',
      adresse:         '',
      specialite:      '',
      numeroCartePro:  '',
      typePrestataire: '',
    };
    this.touched    = false;
    this.formError  = '';
    this.errorMsg   = '';
    this.successMsg = '';
  }

  retour(): void {
    this.router.navigate(['/agent/prestataires']);
  }

  // ══════════════════════════════════════════════════════════════
  // VALIDATION
  // ══════════════════════════════════════════════════════════════

  private valider(): boolean {
    if (!this.form.typePrestataire) {
      this.formError = 'Veuillez sélectionner un type de prestataire.';
      return false;
    }
    if (!this.form.prenom.trim()) {
      this.formError = 'Le prénom est obligatoire.';
      return false;
    }
    if (!this.form.nom.trim()) {
      this.formError = 'Le nom est obligatoire.';
      return false;
    }
    if (!this.form.email.trim()) {
      this.formError = 'L\'email est obligatoire.';
      return false;
    }
    if (!this.isEmailValide(this.form.email)) {
      this.formError = 'L\'email n\'est pas valide.';
      return false;
    }
    if (!this.form.username.trim()) {
      this.formError = 'Le nom d\'utilisateur est obligatoire.';
      return false;
    }
    return true;
  }

  private isEmailValide(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }

  // ══════════════════════════════════════════════════════════════
  // HELPERS
  // ══════════════════════════════════════════════════════════════

  progression(): number {
    const checks = [
      !!this.form.typePrestataire,
      !!this.form.prenom.trim(),
      !!this.form.nom.trim(),
      !!this.form.email.trim(),
      !!this.form.username.trim(),
    ];
    return Math.round((checks.filter(Boolean).length / checks.length) * 100);
  }

  initiales(): string {
    const p = this.form.prenom?.[0]?.toUpperCase() || '';
    const n = this.form.nom?.[0]?.toUpperCase()    || '';
    return p + n || '??';
  }

  getLabelType(value: string): string {
    return this.typeOptions.find(t => t.value === value)?.label || value;
  }

  specialitePlaceholder(): string {
    const map: Record<string, string> = {
      'AVOCAT':   'Ex: Droit bancaire, Droit commercial…',
      'EXPERT':   'Ex: Évaluation immobilière, Expertise comptable…',
      'HUISSIER': 'Ex: Saisies mobilières, Significations…',
    };
    return map[this.form.typePrestataire] || 'Ex: Spécialité principale…';
  }
}