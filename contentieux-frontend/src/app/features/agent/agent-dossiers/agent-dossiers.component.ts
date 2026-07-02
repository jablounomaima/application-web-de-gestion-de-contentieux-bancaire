import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { DossierService } from '../../../core/services/dossier.service';

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
  selector: 'app-agent-dossiers',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './agent-dossiers.component.html',
  styleUrls: ['./agent-dossiers.component.scss']
})
export class AgentDossiersComponent implements OnInit {

  dossiers: any[] = [];
  dossiersFiltres: any[] = [];
  loading = true;

  // Filtres
  recherche   = '';
  filtreStatut = '';
  filtreType   = '';

  // ─── Modal création dossier
  showModal       = false;
  submitting      = false;
  erreurCreation  = '';
  step            = 1;

  // ─── Modal garantie
  showGarantieModal   = false;
  risqueIndexEnCours  = -1;

  // ─── Formulaires
  form!: DossierForm;
  newRisque!: RisqueForm;
  newGarantie!: GarantieForm;


  // Ajouter ces deux arrays dans la classe, après les déclarations de formulaires

risqueTypes = [
  { value: 'CREDIT_IMMOBILIER',    label: '🏠 Crédit Immobilier' },
  { value: 'CREDIT_CONSOMMATION',  label: '🛍️ Crédit Consommation' },
  { value: 'CREDIT_AUTO',          label: '🚗 Crédit Auto' },
  { value: 'CREDIT_PROFESSIONNEL', label: '💼 Crédit Professionnel' },
  { value: 'LEASING',              label: '📋 Leasing' },
  { value: 'DECOUVERT',            label: '🏦 Découvert Bancaire' }
];

garantieTypes = [
  { value: 'BIENS_IMMOBILIERS',   label: '🏢 Biens Immobiliers' },
  { value: 'VEHICULES',           label: '🚗 Véhicules' },
  { value: 'EQUIPEMENTS',         label: '🖥️ Équipements' },
  { value: 'CAUTION_PERSONNELLE', label: '👤 Caution Personnelle' },
  { value: 'GARANTIE_BANCAIRE',   label: '🏛️ Garantie Bancaire' },
  { value: 'AUTRE',               label: '📝 Autre' }
];

  constructor(
    private dossierService: DossierService,
    private router: Router
  ) {}

  ngOnInit() {
    this.form        = this.emptyForm();
    this.newRisque   = this.emptyRisque();
    this.newGarantie = this.emptyGarantie();
    this.chargerDossiers();
  }

  // ─── Chargement ───────────────────────────────────────────────────────────

  chargerDossiers() {
    this.loading = true;
    this.dossierService.getAllDossiers().subscribe({
      next: (data: any[]) => {
        this.dossiers = data;
        this.appliquerFiltres();
        this.loading = false;
      },
      error: (err) => { console.error(err); this.loading = false; }
    });
  }

  appliquerFiltres() {
    this.dossiersFiltres = this.dossiers.filter(d => {
      const clientName = this.getClientName(d).toLowerCase();
      const matchRecherche = !this.recherche ||
        d.numeroDossier?.toLowerCase().includes(this.recherche.toLowerCase()) ||
        clientName.includes(this.recherche.toLowerCase());
      const matchStatut = !this.filtreStatut || d.statut === this.filtreStatut;
      const matchType   = !this.filtreType   || d.typeDossier === this.filtreType;
      return matchRecherche && matchStatut && matchType;
    });
  }

  reinitialiserFiltres() {
    this.recherche   = '';
    this.filtreStatut = '';
    this.filtreType   = '';
    this.appliquerFiltres();
  }

  voirDetails(id: number) {
    this.router.navigate(['/agent/dossier', id]);
  }

  // ─── Modal principal ──────────────────────────────────────────────────────

  ouvrirNouveauDossier() {
    this.step           = 1;
    this.erreurCreation = '';
    this.form           = this.emptyForm();
    this.newRisque      = this.emptyRisque();
    this.showModal      = true;
  }

  passerEtapeSuivante() {
    this.erreurCreation = '';

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

    if (this.step === 2) {
      if (!this.form.libelle?.trim()) {
        this.erreurCreation = 'Le libellé du dossier est obligatoire.';
        return;
      }
    }

    this.step++;
  }

  creerDossier() {
    this.erreurCreation = '';
    this.submitting     = true;

    const payload = {
      typeClient:          this.form.typeClient,
      clientNom:           this.form.clientNom           || null,
      clientPrenom:        this.form.clientPrenom        || null,
      clientCin:           this.form.clientCin           || null,
      clientRne:           this.form.clientRne           || null,
      clientEmail:         this.form.clientEmail         || null,
      clientTelephone:     this.form.clientTelephone     || null,
      clientAdresse:       this.form.clientAdresse       || null,
      clientRaisonSociale: this.form.clientRaisonSociale || null,
      libelle:             this.form.libelle,
      description:         this.form.description         || null,
      notes:               this.form.notes               || null,
      risques: this.form.risques.map(r => ({
        type:           r.type,
        montantInitial: r.montantInitial,
        montantImpaye:  r.montantImpaye,
        dateEcheance:   r.dateEcheance  || null,
        description:    r.description   || null,
        garanties: r.garanties.map(g => ({
          typeGarantie:  g.typeGarantie,
          description:   g.description   || null,
          valeurEstimee: g.valeurEstimee,
          documentRef:   g.documentRef   || null
        }))
      }))
    };

    console.log('[creerDossier] payload →', JSON.stringify(payload, null, 2));

    this.dossierService.creerDossier(payload).subscribe({
      next: (res: any) => {
        this.showModal  = false;
        this.submitting = false;
        this.chargerDossiers();
        const id = res?.dossierId ?? res?.id;
        if (id) this.router.navigate(['/agent/dossier', id]);
      },
      error: (err) => {
        console.error('[creerDossier] erreur →', err);
        this.erreurCreation =
          err.error?.message  ||
          err.error?.error    ||
          (typeof err.error === 'string' ? err.error : null) ||
          'Erreur lors de la création du dossier.';
        this.submitting = false;
      }
    });
  }

  // ─── Risques ──────────────────────────────────────────────────────────────

  ajouterRisque() {
    if (!this.newRisque.type || !this.newRisque.montantInitial) return;
    this.form.risques.push({ ...this.newRisque, garanties: [] });
    this.newRisque = this.emptyRisque();
  }

  supprimerRisque(index: number) {
    this.form.risques.splice(index, 1);
  }

  getTotalImpaye(): number {
    return this.form.risques.reduce((sum, r) => sum + (r.montantImpaye || 0), 0);
  }

  // ─── Garanties ────────────────────────────────────────────────────────────

  ouvrirAjoutGarantie(risqueIndex: number) {
    this.risqueIndexEnCours = risqueIndex;
    this.newGarantie        = this.emptyGarantie();
    this.showGarantieModal  = true;
  }

  confirmerGarantie() {
    if (!this.newGarantie.typeGarantie) return;
    this.form.risques[this.risqueIndexEnCours].garanties.push({ ...this.newGarantie });
    this.showGarantieModal = false;
    this.newGarantie       = this.emptyGarantie();
  }

  supprimerGarantie(risqueIndex: number, garantieIndex: number) {
    this.form.risques[risqueIndex].garanties.splice(garantieIndex, 1);
  }

  // ─── Utilitaires affichage ────────────────────────────────────────────────

  getClientName(dossier: any): string {
    const c = dossier.client;
    if (!c) return '—';
    return c.typeClient === 'MORALE'
      ? (c.raisonSociale || '—')
      : `${c.nom || ''} ${c.prenom || ''}`.trim() || '—';
  }

  getClientInitial(dossier: any): string {
    const name = this.getClientName(dossier);
    return name !== '—' ? name[0].toUpperCase() : '?';
  }

  getStatutClass(statut: string): string {
    const map: Record<string, string> = {
      'EN_ATTENTE': 'statut-en-attente',
      'EN_COURS':   'statut-en-cours',
      'VALIDE':     'statut-valide',
      'REJETE':     'statut-rejete',
      'CLOS':       'statut-clos'
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