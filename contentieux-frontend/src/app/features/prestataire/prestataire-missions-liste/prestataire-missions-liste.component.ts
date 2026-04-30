import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { PrestataireService } from '../../../core/services/prestataire.service';
@Component({
  selector: 'app-prestataire-missions-liste',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './prestataire-missions-liste.component.html',
  styleUrls: ['./prestataire-missions-liste.component.scss']
})
export class PrestataireMissionsListeComponent implements OnInit {

  private apiUrl = `${environment.apiUrl}/api/prestataire/missions`;

  missions: any[] = [];
  missionsFiltrees: any[] = [];
  loading = true;
  errorMsg = '';

  // Filtres
  filtreStatut = 'TOUS';
  filtreRecherche = '';

  // Stats
  stats = { total: 0, assignee: 0, enCours: 0, terminee: 0, rejetee: 0 };

  // Modal résultat
  missionSelectionnee: any = null;
  modalOuverte = false;
  submitting = false;
  successMsg = '';
  modalError = '';

  // Formulaire soumission résultat
  commentaire = '';
  fichiers: File[] = [];

  readonly STATUTS = [
    { value: 'TOUS',             label: 'Toutes',       color: 'gray'   },
    { value: 'ASSIGNEE',         label: 'Assignées',    color: 'amber'  },
    { value: 'EN_COURS',         label: 'En cours',     color: 'blue'   },
    { value: 'PV_SOUMIS',        label: 'PV soumis',    color: 'purple' },
    { value: 'FACTURE_SOUMISE',  label: 'Facture',      color: 'teal'   },
    { value: 'TERMINEE',         label: 'Terminées',    color: 'green'  },
    { value: 'REJETEE',          label: 'Rejetées',     color: 'red'    },
  ];

  constructor(
    private prestataireService: PrestataireService,
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.errorMsg = '';
    // ✅ Utiliser le service au lieu de this.http.get(...)
    this.prestataireService.getMissions().subscribe({
      next: (res) => {
        this.missions = res.missions || [];
        this.calculerStats();
        this.appliquerFiltres();
        this.loading = false;
      },
      error: (err) => {
        this.errorMsg = err?.error?.error || 'Impossible de charger les missions.';
        this.loading = false;
      }
    });
  }
  calculerStats(): void {
    this.stats = {
      total:    this.missions.length,
      assignee: this.missions.filter(m => m.statut === 'ASSIGNEE').length,
      enCours:  this.missions.filter(m => m.statut === 'EN_COURS').length,
      terminee: this.missions.filter(m => m.statut === 'TERMINEE').length,
      rejetee:  this.missions.filter(m => m.statut === 'REJETEE').length,
    };
  }

  appliquerFiltres(): void {
    let liste = [...this.missions];

    if (this.filtreStatut !== 'TOUS') {
      liste = liste.filter(m => m.statut === this.filtreStatut);
    }

    if (this.filtreRecherche.trim()) {
      const kw = this.filtreRecherche.toLowerCase();
      liste = liste.filter(m =>
        m.numeroMission?.toLowerCase().includes(kw) ||
        m.prestation?.dossier?.numeroDossier?.toLowerCase().includes(kw) ||
        m.prestation?.dossier?.client?.nom?.toLowerCase().includes(kw) ||
        m.description?.toLowerCase().includes(kw)
      );
    }

    this.missionsFiltrees = liste;
  }

  // ── Modal soumission résultat ──────────────────────────────
  ouvrirModal(mission: any): void {
    this.missionSelectionnee = mission;
    this.modalOuverte  = true;
    this.commentaire   = '';
    this.fichiers      = [];
    this.successMsg    = '';
    this.modalError    = '';
  }

  fermerModal(): void {
    if (!this.submitting) {
      this.modalOuverte = false;
      this.missionSelectionnee = null;
    }
  }

  onFichiersChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.fichiers = Array.from(input.files);
    }
  }

  supprimerFichier(index: number): void {
    this.fichiers.splice(index, 1);
  }

  soumettrResultat(): void {
    if (!this.commentaire.trim() && this.fichiers.length === 0) {
      this.modalError = 'Ajoutez un commentaire ou au moins un fichier.';
      return;
    }
  
    this.submitting = true;
    this.modalError = '';
  
    const fd = new FormData();
    if (this.commentaire.trim()) fd.append('commentaire', this.commentaire.trim());
    this.fichiers.forEach(f => fd.append('fichiers', f));
  
    // ✅ Utiliser le service
    this.prestataireService.soumettreResultat(this.missionSelectionnee.id, fd)
      .subscribe({
        next: (res) => {
          this.submitting = false;
          this.successMsg = res?.message || 'Résultat soumis avec succès !';
          setTimeout(() => {
            this.fermerModal();
            this.charger();
          }, 1800);
        },
        error: (err) => {
          this.submitting = false;
          this.modalError = err?.error?.error || 'Erreur lors de la soumission.';
        }
      });
  }

  // ── Helpers ──────────────────────────────────────────────────
  voirDetail(id: number): void {
    this.router.navigate(['/prestataire/missions', id]);
  }

  peutSoumettre(m: any): boolean {
    return ['ASSIGNEE', 'EN_COURS', 'REJETEE'].includes(m.statut);
  }

  getStatutConfig(statut: string): { label: string; bg: string; color: string } {
    const map: Record<string, { label: string; bg: string; color: string }> = {
      'ASSIGNEE':        { label: 'Assignée',       bg: '#FAEEDA', color: '#633806' },
      'EN_COURS':        { label: 'En cours',        bg: '#E6F1FB', color: '#0C447C' },
      'PV_SOUMIS':       { label: 'PV soumis',       bg: '#EEEDFE', color: '#3C3489' },
      'FACTURE_SOUMISE': { label: 'Facture soumise', bg: '#E1F5EE', color: '#085041' },
      'TERMINEE':        { label: 'Terminée',        bg: '#EAF3DE', color: '#27500A' },
      'REJETEE':         { label: 'Rejetée',         bg: '#FCEBEB', color: '#791F1F' },
      'VALIDEE_AGENT':   { label: 'Validée',         bg: '#EAF3DE', color: '#27500A' },
    };
    return map[statut] || { label: statut, bg: '#F1EFE8', color: '#444441' };
  }

  getTypeLabel(type: string): string {
    const map: Record<string, string> = {
      'PROCEDURE_JUDICIAIRE': 'Procédure judiciaire',
      'EXPERTISE':            'Expertise',
      'SIGNIFICATION':        'Signification',
      'RECOUVREMENT':         'Recouvrement',
    };
    return map[type] || type;
  }

  formatDate(d: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('fr-FR');
  }

  isEnRetard(m: any): boolean {
    if (!m.dateFinPrevue || m.statut === 'TERMINEE') return false;
    return new Date(m.dateFinPrevue) < new Date();
  }

  formatTaille(bytes: number): string {
    if (bytes < 1024) return bytes + ' o';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' Ko';
    return (bytes / (1024 * 1024)).toFixed(1) + ' Mo';
  }
}