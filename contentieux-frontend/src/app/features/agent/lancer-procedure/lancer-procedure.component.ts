import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DossierService } from '../../../core/services/dossier.service';

@Component({
  selector: 'app-lancer-procedure',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './lancer-procedure.component.html',
  styleUrls: ['./lancer-procedure.component.scss']
})
export class LancerProcedureComponent implements OnInit {

  // ── Données ────────────────────────────────────────────────────────
  dossier: any = null;
  loading = true;

  // ── Flags ─────────────────────────────────────────────────────────
  submitting = false;
  showModalConfirm = false;

  // ── Messages ───────────────────────────────────────────────────────
  successMessage = '';
  errorMessage = '';
  formError = '';
  submitError = '';

  // ── Date du jour (pour le min du date picker) ──────────────────────
  today: string = new Date().toISOString().split('T')[0];

  // ── Types de procédure ─────────────────────────────────────────────
  procedureTypes = [
    { value: 'INJONCTION_PAYER',     label: 'Injonction de payer',     emoji: '💰' },
    { value: 'SAISIE_IMMOBILIERE',   label: 'Saisie immobilière',      emoji: '🏠' },
    { value: 'SAISIE_MOBILIERE',     label: 'Saisie mobilière',        emoji: '📦' },
    { value: 'LIQUIDATION',          label: 'Liquidation judiciaire',  emoji: '⚖️' },
    { value: 'REFERE',               label: 'Référé',                  emoji: '⚡' },
    { value: 'AUTRE',                label: 'Autre procédure',         emoji: '📋' },
  ];

  // ── Formulaire ─────────────────────────────────────────────────────
  form = {
    typeProcedure:       '',
    datePremierAudience: '',
    motif:               '',
    observations:        '',
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private dossierService: DossierService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.chargerDossier(id);
    } else {
      this.loading = false;
    }
  }

  // ══════════════════════════════════════════════════════════════════
  // CHARGEMENT DOSSIER
  // ══════════════════════════════════════════════════════════════════

  chargerDossier(id: number): void {
    this.loading = true;
    this.dossierService.getDossierDetails(id).subscribe({
      next: (data: any) => {
        let d = data.dossier || data;

        // Reconstruction client (même logique que le composant parent)
        d.client = {
          typeClient: d.clientType || d.clientTypeClient || 'PARTICULIER',
          nom: d.clientNom || d.client?.nom || null,
          prenom: d.clientPrenom || d.client?.prenom || null,
          cin: d.clientCin || d.client?.cin || null,
          email: d.clientEmail || d.client?.email || null,
          telephone: d.clientTelephone || d.client?.telephone || null,
          adresse: d.clientAdresse || d.client?.adresse || null,
          raisonSociale: d.clientRaisonSociale || d.client?.raisonSociale || null,
          rne: d.clientRne || d.client?.rne || null,
        };

        // Reconstruction agence
        d.agence = {
          nom: d.agenceNom || d.agence?.nom || null,
          ville: d.agenceVille || d.agence?.ville || null,
        };

        // Init garanties sur les risques
        if (d.risques && Array.isArray(d.risques)) {
          d.risques = d.risques.map((r: any) => ({
            ...r,
            garanties: r.garanties || [],
            _inclus: true  // tous sélectionnés par défaut
          }));
        } else {
          d.risques = [];
        }

        this.dossier = d;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('❌ Erreur chargement dossier:', err);
        this.loading = false;
      }
    });
  }

  // ══════════════════════════════════════════════════════════════════
  // HELPERS AFFICHAGE
  // ══════════════════════════════════════════════════════════════════

  getClientName(): string {
    if (!this.dossier?.client) return '—';
    const c = this.dossier.client;
    if (c.typeClient === 'ENTREPRISE') return c.raisonSociale || '—';
    return `${c.nom || ''} ${c.prenom || ''}`.trim() || '—';
  }

  getStatutClass(statut: string): string {
    const map: Record<string, string> = {
      'OUVERT': 'statut-ouvert',
      'EN_TRAITEMENT': 'statut-en-traitement',
      'VALIDE': 'statut-valide',
      'REJETE': 'statut-rejete',
      'EN_PROCEDURE': 'statut-en-procedure',
      'CLOS': 'statut-clos'
    };
    return map[statut] || '';
  }

  formatStatut(statut: string): string {
    const map: Record<string, string> = {
      'OUVERT': 'Ouvert',
      'EN_TRAITEMENT': 'En traitement',
      'VALIDE': 'Validé',
      'REJETE': 'Rejeté',
      'EN_PROCEDURE': 'En procédure',
      'CLOS': 'Clos'
    };
    return map[statut] || statut;
  }

  formatRisqueType(type: string): string {
    const map: Record<string, string> = {
      'CREDIT_IMMOBILIER': 'Crédit Immobilier',
      'CREDIT_CONSOMMATION': 'Crédit Consommation',
      'CREDIT_AUTO': 'Crédit Auto',
      'CREDIT_PROFESSIONNEL': 'Crédit Professionnel',
      'LEASING': 'Leasing',
      'DECOUVERT': 'Découvert Bancaire',
    };
    return map[type] || type;
  }

  getLabelType(value: string): string {
    return this.procedureTypes.find(t => t.value === value)?.label || value;
  }

  getMontantTotal(): number {
    if (!this.dossier?.risques) return 0;
    return this.dossier.risques.reduce(
      (sum: number, r: any) => sum + (r.montantImpaye || 0), 0
    );
  }

  getMontantSelectionne(): number {
    if (!this.dossier?.risques) return 0;
    return this.dossier.risques
      .filter((r: any) => r._inclus)
      .reduce((sum: number, r: any) => sum + (r.montantImpaye || 0), 0);
  }

  toggleRisqueInclus(risque: any): void {
    risque._inclus = !risque._inclus;
  }

  // ── Checklist score ───────────────────────────────────────────────
  getChecklistScore(): number {
    const checks = [
      this.dossier?.validationFinanciere === true,
      this.dossier?.validationJuridique === true,
      this.dossier?.statut === 'VALIDE',
      this.getMontantTotal() > 0,
      !!this.form.typeProcedure,
      
      !!this.form.datePremierAudience,
      !!this.form.motif.trim(),
    ];
    const done = checks.filter(Boolean).length;
    return Math.round((done / checks.length) * 100);
  }

  // ══════════════════════════════════════════════════════════════════
  // VALIDATION FORMULAIRE
  // ══════════════════════════════════════════════════════════════════

  validerFormulaire(): boolean {
    this.formError = '';

    if (!this.form.typeProcedure) {
      this.formError = 'Veuillez sélectionner un type de procédure.';
      return false;
    }

   

   


    if (!this.form.motif?.trim()) {
      this.formError = 'Le motif de la procédure est obligatoire.';
      return false;
    }

    const risquesInclus = this.dossier?.risques?.filter((r: any) => r._inclus) || [];
    if (risquesInclus.length === 0) {
      this.formError = 'Veuillez sélectionner au moins un crédit concerné.';
      return false;
    }

    return true;
  }

  // ══════════════════════════════════════════════════════════════════
  // ACTIONS
  // ══════════════════════════════════════════════════════════════════

  lancerProcedure(): void {
    if (!this.validerFormulaire()) return;
    this.submitError = '';
    this.showModalConfirm = true;
  }

  confirmerLancement(): void {
    this.submitError = '';
    this.submitting = true;
  
    // Le backend attend uniquement: type + description
    const payload = {
      type:        'PROCEDURE_JUDICIAIRE',
      description: this.form.motif.trim(),
    };
  
    this.dossierService.lancerProcedureJudiciaire(this.dossier.id, payload).subscribe({
      next: (response: any) => {
        this.submitting = false;
        this.showModalConfirm = false;
        this.successMessage = '✅ Procédure judiciaire lancée avec succès.';
        setTimeout(() => {
          this.router.navigate(['/agent/dossiers', this.dossier.id]);
        }, 2000);
      },
      error: (err: any) => {
        this.submitError = err.error?.error || err.error?.message || 'Erreur lors du lancement.';
        this.submitting = false;
      }
    });
  }

  reinitialiserFormulaire(): void {
    this.form = {
        typeProcedure:       '',
        datePremierAudience: '',
        motif:               '',
        observations:        '',
      };


    this.formError = '';
    this.errorMessage = '';
    this.successMessage = '';
    // Réinitialiser les sélections de risques
    if (this.dossier?.risques) {
      this.dossier.risques.forEach((r: any) => r._inclus = true);
    }
  }

  
}