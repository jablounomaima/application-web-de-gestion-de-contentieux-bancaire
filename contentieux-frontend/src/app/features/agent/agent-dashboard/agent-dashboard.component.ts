import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { DossierService } from '../../../core/services/dossier.service';
import { AgentDossiersListeComponent } from '../agent-dossiers-liste/agent-dossiers-liste.component';

// ─── Interfaces ───────────────────────────────────────────────────────────────

interface GarantieForm {
  typeGarantie: string;
  description: string;
  valeurEstimee: number | null;
  documentRef: string;
}

interface RisqueForm {
  type: string;
  montantInitial: number | null;
  montantImpaye: number | null;
  dateEcheance: string;
  description: string;
  garanties: GarantieForm[];
}

interface DossierForm {
  typeClient: 'PARTICULIER' | 'ENTREPRISE';
  clientNom: string;
  clientPrenom: string;
  clientCin: string;
  clientRne: string;
  clientEmail: string;
  clientTelephone: string;
  clientAdresse: string;
  clientRaisonSociale: string;
  libelle: string;
  description: string;
  notes: string;
  risques: RisqueForm[];
}

// ─── Composant ────────────────────────────────────────────────────────────────

@Component({
  selector: 'app-agent-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AgentDossiersListeComponent],
  templateUrl: './agent-dashboard.component.html',
  styleUrls: ['./agent-dashboard.component.scss']
})
export class AgentDashboardComponent implements OnInit {

  dossiers: any[] = [];
  loading = true;

  stats = { total: 0, enCours: 0, valides: 0, rejetes: 0, montantTotal: 0 };

  showModal    = false;
  submitting   = false;
  erreurCreation = '';
  step         = 1;

  showGarantieModal  = false;
  risqueIndexEnCours = -1;

  form!: DossierForm;
  newRisque!: RisqueForm;
  newGarantie!: GarantieForm;

  // Labels affichés dans la barre de progression
  readonly stepsList = ['Client', 'Dossier', 'Risques', 'Confirmation'];

  // ─── Dictionnaires de labels ──────────────────────────────────────────────

  private readonly risqueLabels: Record<string, string> = {
    CREDIT_IMMOBILIER:  'Crédit Immobilier',
    CREDIT_CONSOMMATION: 'Crédit Consommation',
    DECOUVERT:          'Découvert Bancaire',
    LEASING:            'Leasing'
  };

  private readonly garantieLabels: Record<string, string> = {
    HYPOTHEQUE:   'Hypothèque',
    CAUTION:      'Caution',
    NANTISSEMENT: 'Nantissement',
    GAGE:         'Gage',
    AUTRE:        'Autre'
  };

  private readonly garantieIcons: Record<string, string> = {
    HYPOTHEQUE:   'ti-home',
    CAUTION:      'ti-shield',
    NANTISSEMENT: 'ti-certificate',
    GAGE:         'ti-lock',
    AUTRE:        'ti-file'
  };

  // ─── Constructeur ─────────────────────────────────────────────────────────

  constructor(
    private dossierService: DossierService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.form        = this.emptyForm();
    this.newRisque   = this.emptyRisque();
    this.newGarantie = this.emptyGarantie();
    this.chargerDossiers();

    this.route.queryParams.subscribe(params => {
      if (params['action'] === 'nouveau-dossier') {
        this.ouvrirNouveauDossier();
        this.router.navigate([], { queryParams: {}, replaceUrl: true });
      }
    });
  }

  // ─── Chargement ───────────────────────────────────────────────────────────

  chargerDossiers(): void {
    this.loading = true;
    this.dossierService.getAllDossiers().subscribe({
      next: (data: any[]) => {
        this.dossiers = Array.isArray(data) ? data : [];
        this.calculerStats(this.dossiers);
        this.loading = false;
      },
      error: () => {
        this.dossiers = [];
        this.loading  = false;
      }
    });
  }

  calculerStats(dossiers: any[]): void {
    this.stats.total   = dossiers.length;
    this.stats.enCours = dossiers.filter(d =>
      ['EN_COURS', 'EN_TRAITEMENT', 'OUVERT'].includes(d.statut)
    ).length;
    this.stats.valides       = dossiers.filter(d => d.statut === 'VALIDE').length;
    this.stats.rejetes       = dossiers.filter(d => d.statut === 'REJETE').length;
    this.stats.montantTotal  = dossiers.reduce(
      (sum, d) => sum + (d.montantTotalEngagement || 0), 0
    );
  }

  voirDetails(id: number): void {
    this.router.navigate(['/agent/dossier', id]);
  }

  // ─── Modal principal ──────────────────────────────────────────────────────

  ouvrirNouveauDossier(): void {
    this.step            = 1;
    this.erreurCreation  = '';
    this.form            = this.emptyForm();
    this.newRisque       = this.emptyRisque();
    this.newGarantie     = this.emptyGarantie();
    this.showModal       = true;
  }

  passerEtapeSuivante(): void {
    this.erreurCreation = '';

    // ── Validation étape 1 : Client ──
    if (this.step === 1) {
      if (this.form.typeClient === 'PARTICULIER') {
        if (!this.form.clientNom?.trim() || !this.form.clientPrenom?.trim()) {
          this.erreurCreation = 'Le nom et le prénom sont obligatoires.';
          return;
        }
        if (!this.form.clientCin?.trim()) {
          this.erreurCreation = 'Le CIN est obligatoire pour un particulier.';
          return;
        }
      } else {
        if (!this.form.clientRaisonSociale?.trim()) {
          this.erreurCreation = 'La raison sociale est obligatoire pour une entreprise.';
          return;
        }
      }
    }

    // ── Validation étape 2 : Dossier ──
    if (this.step === 2) {
      if (!this.form.libelle?.trim()) {
        this.erreurCreation = 'Le libellé du dossier est obligatoire.';
        return;
      }
    }

    // ── Validation étape 3 : Risques & Garanties ──
    if (this.step === 3) {
      if (this.form.risques.length === 0) {
        this.erreurCreation = 'Veuillez ajouter au moins un risque.';
        return;
      }
      if (this.aDesRisquesSansGarantie()) {
        this.erreurCreation = '⚠️ Chaque risque doit avoir au moins une garantie.';
        return;
      }
    }

    this.step++;
  }

  creerDossier(): void {
    this.erreurCreation = '';
    this.submitting     = true;

    const payload = {
      typeClient:           this.form.typeClient,
      clientNom:            this.form.clientNom            || null,
      clientPrenom:         this.form.clientPrenom         || null,
      clientCin:            this.form.clientCin            || null,
      clientRne:            this.form.clientRne            || null,
      clientEmail:          this.form.clientEmail          || null,
      clientTelephone:      this.form.clientTelephone      || null,
      clientAdresse:        this.form.clientAdresse        || null,
      clientRaisonSociale:  this.form.clientRaisonSociale  || null,
      libelle:              this.form.libelle,
      description:          this.form.description          || null,
      notes:                this.form.notes                || null,

      risques: this.form.risques.map(r => ({
        type:           r.type,
        montantInitial: r.montantInitial,
        montantImpaye:  r.montantImpaye,
        dateEcheance:   r.dateEcheance  || null,
        description:    r.description   || null,

        // ✅ Garanties imbriquées dans chaque risque
        garanties: r.garanties.map(g => ({
          typeGarantie:  g.typeGarantie,
          description:   g.description  || null,
          valeurEstimee: g.valeurEstimee,
          documentRef:   g.documentRef  || null
        }))
      }))
    };

    this.dossierService.creerDossier(payload).subscribe({
      next: (res: any) => {
        this.showModal  = false;
        this.submitting = false;
        this.chargerDossiers();

        const id = res?.dossierId ?? res?.id;
        if (id) this.router.navigate(['/agent/dossier', id]);
      },
      error: (err) => {
        this.erreurCreation =
          err.error?.message ||
          err.error?.error   ||
          'Erreur lors de la création du dossier.';
        this.submitting = false;
      }
    });
  }

  // ─── Risques ──────────────────────────────────────────────────────────────

  ajouterRisque(): void {
    if (!this.newRisque.type || !this.newRisque.montantInitial) return;
    this.form.risques.push({ ...this.newRisque, garanties: [] });
    this.newRisque = this.emptyRisque();
  }

  supprimerRisque(index: number): void {
    this.form.risques.splice(index, 1);
  }

  getTotalImpaye(): number {
    return this.form.risques.reduce((sum, r) => sum + (r.montantImpaye || 0), 0);
  }

  // ─── Garanties ────────────────────────────────────────────────────────────

  ouvrirAjoutGarantie(risqueIndex: number): void {
    this.risqueIndexEnCours = risqueIndex;
    this.newGarantie        = this.emptyGarantie();
    this.showGarantieModal  = true;
  }

  confirmerGarantie(): void {
    if (!this.newGarantie.typeGarantie) return;
    this.form.risques[this.risqueIndexEnCours].garanties.push({ ...this.newGarantie });
    this.showGarantieModal = false;
    this.newGarantie       = this.emptyGarantie();
  }

  supprimerGarantie(risqueIndex: number, garantieIndex: number): void {
    this.form.risques[risqueIndex].garanties.splice(garantieIndex, 1);
  }

  // ─── Utilitaires affichage ────────────────────────────────────────────────

  /** Vérifie si un risque n'a aucune garantie */
  risqueSansGarantie(risque: RisqueForm): boolean {
    return !risque.garanties || risque.garanties.length === 0;
  }

  /** Vérifie s'il existe au moins un risque sans garantie */
  aDesRisquesSansGarantie(): boolean {
    return this.form.risques.some(r => this.risqueSansGarantie(r));
  }

  getRisqueLabel(type: string): string {
    return this.risqueLabels[type] || type;
  }

  getGarantieLabel(type: string): string {
    return this.garantieLabels[type] || type;
  }

  getGarantieIcon(type: string): string {
    return this.garantieIcons[type] || 'ti-lock';
  }

  getClientName(dossier: any): string {
    const type = dossier.clientTypeClient || dossier.clientType || '';
    if (type === 'ENTREPRISE') return dossier.clientRaisonSociale || '—';
    const full = `${dossier.clientNom || ''} ${dossier.clientPrenom || ''}`.trim();
    return full || '—';
  }

  getClientInitial(dossier: any): string {
    const name = this.getClientName(dossier);
    return name !== '—' ? name[0].toUpperCase() : '?';
  }

  getStatutClass(statut: string): string {
    const map: Record<string, string> = {
      EN_ATTENTE: 'statut-en-attente',
      EN_COURS:   'statut-en-cours',
      VALIDE:     'statut-valide',
      REJETE:     'statut-rejete',
      CLOS:       'statut-clos'
    };
    return map[statut] || '';
  }

  // ─── Helpers privés ───────────────────────────────────────────────────────

  private emptyForm(): DossierForm {
    return {
      typeClient: 'PARTICULIER',
      clientNom: '', clientPrenom: '', clientCin: '',
      clientRne: '', clientEmail: '', clientTelephone: '',
      clientAdresse: '', clientRaisonSociale: '',
      libelle: '', description: '', notes: '',
      risques: []
    };
  }

  private emptyRisque(): RisqueForm {
    return {
      type: '', montantInitial: null, montantImpaye: null,
      dateEcheance: '', description: '', garanties: []
    };
  }

  private emptyGarantie(): GarantieForm {
    return {
      typeGarantie: '', description: '',
      valeurEstimee: null, documentRef: ''
    };
  }
}