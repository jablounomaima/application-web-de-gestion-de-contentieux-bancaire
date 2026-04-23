import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DossierService } from '../../../core/services/dossier.service';

@Component({
  selector: 'app-agent-dossier-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './agent-dossier-detail.component.html',
  styleUrls: ['./agent-dossier-detail.component.scss']
})
export class AgentDossierDetailComponent implements OnInit {

  dossier: any = null;
  historique: any[] = [];
  validateurs_financiers: any[] = [];
  validateurs_juridiques: any[] = [];
  missionAvocat: any = null;
  loading = true;

  // ── Modals ────────────────────────────────────────────────────────
  showModalValidateurs = false;
  showModalRisque      = false;

  // ── Erreurs ───────────────────────────────────────────────────────
  erreurValidateurs = '';
  erreurRisque      = '';

  // ── Formulaires ───────────────────────────────────────────────────
  formValidateurs = { validateurFinancier: '', validateurJuridique: '' };
  formRisque = {
    type:           'CREDIT_IMMOBILIER',
    montantInitial: 0,
    montantImpaye:  0,
    dateEcheance:   '',
    description:    ''
  };

  constructor(
    private route:         ActivatedRoute,
    private router:        Router,
    private dossierService: DossierService
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) this.chargerDossier(id);
  }

  // ════════════════════════════════════════════════════
  //  CHARGEMENT
  // ════════════════════════════════════════════════════

  chargerDossier(id: number) {
    this.loading = true;
    this.dossierService.getDossierDetails(id).subscribe({
      next: (data: any) => {
        const d = data.dossier || data;

        // ── Reconstruire client depuis les champs plats du DTO ──────
        d.client = {
          typeClient:    d.clientType || d.clientTypeClient || 'PARTICULIER',
          nom:           d.clientNom           || null,
          prenom:        d.clientPrenom         || null,
          cin:           d.clientCin            || null,
          email:         d.clientEmail          || null,
          telephone:     d.clientTelephone      || null,
          adresse:       d.clientAdresse        || null,
          raisonSociale: d.clientRaisonSociale  || null,
          rne:           d.clientRne            || null,
        };

        // ── Reconstruire agence depuis les champs plats ─────────────
        d.agence = {
          nom:   d.agenceNom   || null,
          ville: d.agenceVille || null,
        };

        this.dossier                = d;
        this.historique             = data.historique             || [];
        this.validateurs_financiers = data.validateurs_financiers || [];
        this.validateurs_juridiques = data.validateurs_juridiques || [];
        this.missionAvocat          = data.missionAvocat          || null;

        // Pré-remplir validateurs si déjà choisis
        if (d.validateurFinancierChoisi)
          this.formValidateurs.validateurFinancier = d.validateurFinancierChoisi;
        if (d.validateurJuridiqueChoisi)
          this.formValidateurs.validateurJuridique = d.validateurJuridiqueChoisi;

        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur chargement dossier', err);
        this.loading = false;
      }
    });
  }

  // ════════════════════════════════════════════════════
  //  HELPERS AFFICHAGE
  // ════════════════════════════════════════════════════

  getClientName(): string {
    if (!this.dossier?.client) return '—';
    const c = this.dossier.client;
    if (c.typeClient === 'ENTREPRISE') return c.raisonSociale || '—';
    return `${c.nom || ''} ${c.prenom || ''}`.trim() || '—';
  }

  getStatutClass(statut: string): string {
    const map: Record<string, string> = {
      'OUVERT':         'statut-ouvert',
      'EN_TRAITEMENT':  'statut-en-traitement',
      'VALIDE':         'statut-valide',
      'REJETE':         'statut-rejete',
      'CLOS':           'statut-clos'
    };
    return map[statut] || '';
  }

  formatStatut(statut: string): string {
    const map: Record<string, string> = {
      'OUVERT':         'Ouvert',
      'EN_TRAITEMENT':  'En traitement',
      'VALIDE':         'Validé',
      'REJETE':         'Rejeté',
      'CLOS':           'Clos'
    };
    return map[statut] || statut;
  }

  formatRisqueType(type: string): string {
    const map: Record<string, string> = {
      'CREDIT_IMMOBILIER':    'Crédit Immobilier',
      'CREDIT_CONSOMMATION':  'Crédit Consommation',
      'CREDIT_AUTO':          'Crédit Auto',
      'CREDIT_PROFESSIONNEL': 'Crédit Professionnel',
      'LEASING':              'Leasing',
      'DECOUVERT':            'Découvert Bancaire',
    };
    return map[type] || type;
  }

  getMontantTotal(): number {
    if (!this.dossier?.risques) return 0;
    return this.dossier.risques.reduce(
      (sum: number, r: any) => sum + (r.montantImpaye || 0), 0
    );
  }

  getValidIcon(val: boolean | null): string {
    if (val === true)  return 'ok';
    if (val === false) return 'ko';
    return 'pending';
  }

  getValidEmoji(val: boolean | null): string {
    if (val === true)  return '✅';
    if (val === false) return '❌';
    return '⏳';
  }

  canModifier(): boolean {
    return this.dossier?.statut === 'OUVERT' || this.dossier?.statut === 'REJETE';
  }

  canSoumettre(): boolean {
    return this.canModifier() &&
      this.dossier?.risques?.some((r: any) => r.selectionne);
  }

  // ════════════════════════════════════════════════════
  //  ACTIONS
  // ════════════════════════════════════════════════════

  ouvrirModalValidateurs() {
    this.erreurValidateurs = '';
    this.showModalValidateurs = true;
  }

  ouvrirModificationDossier() {
    this.router.navigate(['/agent/dossiers', this.dossier.id, 'modifier']);
  }

  choisirEtSoumettre() {
    this.erreurValidateurs = '';
    const { validateurFinancier, validateurJuridique } = this.formValidateurs;
    if (!validateurFinancier || !validateurJuridique) {
      this.erreurValidateurs = 'Veuillez choisir les deux validateurs.';
      return;
    }

    this.dossierService.choisirValidateurs(this.dossier.id, this.formValidateurs).subscribe({
      next: () => {
        this.dossierService.soumettreAValidation(this.dossier.id).subscribe({
          next: () => {
            this.showModalValidateurs = false;
            this.chargerDossier(this.dossier.id);
          },
          error: (err: any) => {
            this.erreurValidateurs = err.error?.error || 'Erreur lors de la soumission.';
          }
        });
      },
      error: (err: any) => {
        this.erreurValidateurs = err.error?.error || 'Erreur lors du choix des validateurs.';
      }
    });
  }

  toggleRisque(risque: any) {
    this.dossierService.selectionnerRisque(
      this.dossier.id, risque.id, !risque.selectionne
    ).subscribe({
      next: () => this.chargerDossier(this.dossier.id),
      error: (err: any) => alert(err.error?.error || 'Erreur sélection risque')
    });
  }

  ajouterRisque() {
    this.erreurRisque = '';
    if (!this.formRisque.montantInitial || !this.formRisque.montantImpaye) {
      this.erreurRisque = 'Les montants sont obligatoires.';
      return;
    }
    this.dossierService.ajouterRisque(this.dossier.id, this.formRisque).subscribe({
      next: () => {
        this.showModalRisque = false;
        this.formRisque = {
          type: 'CREDIT_IMMOBILIER', montantInitial: 0,
          montantImpaye: 0, dateEcheance: '', description: ''
        };
        this.chargerDossier(this.dossier.id);
      },
      error: (err: any) => {
        this.erreurRisque = err.error?.error || "Erreur lors de l'ajout.";
      }
    });
  }

  supprimerGarantie(garantieId: number) {
    if (!confirm('Supprimer cette garantie ?')) return;
    this.dossierService.supprimerGarantie(garantieId).subscribe({
      next: () => this.chargerDossier(this.dossier.id),
      error: (err: any) => alert(err.error?.error || 'Erreur suppression garantie')
    });
  }

  supprimerDossier() {
    if (!confirm('Supprimer définitivement ce dossier ?')) return;
    this.dossierService.supprimerDossier(this.dossier.id).subscribe({
      next: () => this.router.navigate(['/agent/dossiers']),
      error: (err: any) => alert(err.error?.error || 'Erreur suppression')
    });
  }

  telechargerPdf() {
    this.dossierService.telechargerPdf(this.dossier.id).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href     = url;
        a.download = `dossier-${this.dossier.numeroDossier}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => alert('Erreur lors du téléchargement du PDF.')
    });
  }
}