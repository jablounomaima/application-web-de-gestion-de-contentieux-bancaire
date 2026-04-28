import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-modifier-prestataire',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './modifier-prestataire.component.html',
  styleUrls: ['./modifier-prestataire.component.scss']
})
export class ModifierPrestataireComponent implements OnInit {

  private apiUrl = `${environment.apiUrl}/api/agent/prestataires`;

  // ✅ Nouveaux inputs/outputs pour le mode popup
  @Input() mode: 'route' | 'popup' = 'route'; // Mode d'affichage
  @Input() prestataireIdInput?: number; // Pour le mode popup
  @Output() fermer = new EventEmitter<void>();
  @Output() prestataireModifie = new EventEmitter<any>();

  prestataireId!: number;

  // ── États ──────────────────────────────────────────────────────
  loading      = true;
  submitting   = false;
  touched      = false;
  showPassword = false;

  // ── Messages ───────────────────────────────────────────────────
  successMsg = '';
  errorMsg   = '';
  formError  = '';

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
    typePrestataire: '',
  };

  // ── Types ──────────────────────────────────────────────────────
  typeOptions = [
    { value: 'AVOCAT',   label: 'Avocat',   emoji: '⚖️' },
    { value: 'EXPERT',   label: 'Expert',   emoji: '🔬' },
    { value: 'HUISSIER', label: 'Huissier', emoji: '📜' },
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    // ✅ Mode popup ou route
    if (this.mode === 'popup' && this.prestataireIdInput) {
      this.prestataireId = this.prestataireIdInput;
    } else {
      this.prestataireId = Number(this.route.snapshot.paramMap.get('id'));
    }
    
    this.charger();
  }

  // ══════════════════════════════════════════════════════════════
  // CHARGEMENT
  // ══════════════════════════════════════════════════════════════

  charger(): void {
    this.loading = true;
    this.http.get<any>(`${this.apiUrl}/${this.prestataireId}`).subscribe({
      next: (data) => {
        this.form = {
          username:        data.username        || '',
          motDePasse:      '',
          prenom:          data.prenom          || '',
          nom:             data.nom             || '',
          email:           data.email           || '',
          telephone:       data.telephone       || '',
          adresse:         data.adresse         || '',
          specialite:      data.specialite      || '',
          numeroCartePro:  data.numeroCartePro  || '',
          typePrestataire: data.type            || '',
        };
        this.loading = false;
      },
      error: (err) => {
        this.errorMsg = err?.error?.error || 'Impossible de charger le prestataire.';
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
    this.errorMsg   = '';

    const payload: any = {
      username:        this.form.username,
      prenom:          this.form.prenom.trim(),
      nom:             this.form.nom.trim(),
      email:           this.form.email.trim(),
      telephone:       this.form.telephone.trim()    || null,
      adresse:         this.form.adresse.trim()      || null,
      specialite:      this.form.specialite.trim()   || null,
      numeroCartePro:  this.form.numeroCartePro.trim() || null,
      typePrestataire: this.form.typePrestataire,
    };

    if (this.form.motDePasse.trim()) {
      payload.motDePasse = this.form.motDePasse.trim();
    }

    this.http.put<any>(`${this.apiUrl}/${this.prestataireId}`, payload).subscribe({
      next: (response) => {
        this.submitting = false;
        this.successMsg = '✅ Prestataire mis à jour avec succès !';
        
        // ✅ Comportement différent selon le mode
        if (this.mode === 'popup') {
          this.prestataireModifie.emit(response);
          setTimeout(() => this.fermerPopup(), 1500);
        } else {
          setTimeout(() => this.router.navigate(['/agent/prestataires']), 2000);
        }
      },
      error: (err) => {
        this.submitting = false;
        this.errorMsg   = err?.error?.error || err?.error?.message || 'Une erreur est survenue.';
      }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // VALIDATION
  // ══════════════════════════════════════════════════════════════

  private valider(): boolean {
    if (!this.form.prenom.trim()) { this.formError = 'Le prénom est obligatoire.'; return false; }
    if (!this.form.nom.trim())    { this.formError = 'Le nom est obligatoire.';    return false; }
    if (!this.form.email.trim())  { this.formError = 'L\'email est obligatoire.';  return false; }
    if (!this.isEmailValide(this.form.email)) {
      this.formError = 'L\'email n\'est pas valide.';
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

  // ✅ Nouvelle méthode pour popup
  fermerPopup(): void {
    if (this.mode === 'popup') {
      this.fermer.emit();
    } else {
      this.retour();
    }
  }

  retour(): void {
    this.router.navigate(['/agent/prestataires']);
  }

  initiales(): string {
    const p = this.form.prenom?.[0]?.toUpperCase() || '';
    const n = this.form.nom?.[0]?.toUpperCase()    || '';
    return p + n || '??';
  }

  getLabelType(value: string): string {
    return this.typeOptions.find(t => t.value === value)?.label || value;
  }

  getEmojiType(value: string): string {
    return this.typeOptions.find(t => t.value === value)?.emoji || '👤';
  }

  specialitePlaceholder(): string {
    const map: Record<string, string> = {
      'AVOCAT':   'Ex: Droit bancaire…',
      'EXPERT':   'Ex: Évaluation immobilière…',
      'HUISSIER': 'Ex: Saisies mobilières…',
    };
    return map[this.form.typePrestataire] || 'Spécialité principale…';
  }
}