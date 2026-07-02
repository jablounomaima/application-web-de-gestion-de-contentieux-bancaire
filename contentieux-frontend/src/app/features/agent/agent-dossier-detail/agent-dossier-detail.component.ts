import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router, NavigationEnd } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DossierService } from '../../../core/services/dossier.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ActionValidationComponent } from '../action-validation/action-validation.component';
// CORRECTION 1 : import LancerAffaireComponent supprimé (inutilisé)

@Component({
  selector: 'app-agent-dossier-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ActionValidationComponent],
  templateUrl: './agent-dossier-detail.component.html',
  styleUrls: ['./agent-dossier-detail.component.scss']
})
export class AgentDossierDetailComponent implements OnInit, OnDestroy {

  dossier: any = null;
  historique: any[] = [];
  validateurs_financiers: any[] = [];
  validateurs_juridiques: any[] = [];
  missionAvocat: any = null;
  loading = true;

  resoumission = false;
  erreurResoumission = '';

  affaireExiste = false;
  prestationJudiciaire: any = null;

  formAffaire = { tribunal: '', numeroRole: '', chambre: '' };

  // ── Modals ────────────────────────────────────────────────────────
  showModalValidateurs = false;
  showModalRisque = false;
  showModalModifier = false;
  showModalSupprimer = false;

  // ── Erreurs ───────────────────────────────────────────────────────
  erreurValidateurs = '';
  erreurRisque = '';
  erreurModifier = '';
  erreurSupprimer = '';
  success = '';
  error   = '';

  // ── Flags ─────────────────────────────────────────────────────────
  saving = false;
  suppressing = false;

  // ── Formulaires ───────────────────────────────────────────────────
  formValidateurs = { validateurFinancier: '', validateurJuridique: '' };

  formRisque = {
    type: 'CREDIT_IMMOBILIER',
    montantInitial: 0,
    montantImpaye: 0,
    dateEcheance: '',
    description: ''
  };

  risqueTypes = [
    { value: 'CREDIT_IMMOBILIER',    label: '🏠 Crédit Immobilier' },
    { value: 'CREDIT_CONSOMMATION',  label: '🛍️ Crédit Consommation' },
    { value: 'CREDIT_AUTO',          label: '🚗 Crédit Auto' },
    { value: 'CREDIT_PROFESSIONNEL', label: '💼 Crédit Professionnel' },
    { value: 'LEASING',              label: '📋 Leasing' },
    { value: 'DECOUVERT',            label: '🏦 Découvert Bancaire' }
  ];

  formModifier = { libelle: '', description: '', notes: '' };

  // ── Gestion du cycle de vie ───────────────────────────────────────
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private dossierService: DossierService,
    private notifService: NotificationService
  ) {}

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const id = Number(params.get('id'));
      if (id) this.chargerDossier(id);
    });

    this.route.queryParams.subscribe(params => {
      if (params['refresh']) {
        const currentId = Number(this.route.snapshot.paramMap.get('id'));
        if (currentId) this.chargerDossier(currentId);
      }
    });

    // CORRECTION 2 : takeUntil(this.destroy$) ajouté pour éviter la fuite mémoire
    this.router.events.pipe(
      takeUntil(this.destroy$)
    ).subscribe(event => {
      if (event instanceof NavigationEnd) {
        const urlSansParams = event.urlAfterRedirects.split('?')[0];
        const estDetailExact = /^\/agent\/dossiers\/\d+$/.test(urlSansParams);
        if (!estDetailExact) return;
        const currentId = Number(this.route.snapshot.paramMap.get('id'));
        if (currentId && currentId === this.dossier?.id) {
          this.chargerDossier(currentId);
        }
      }
    });

    // ── Rechargement automatique via WebSocket ────────────────────
    const typesAgent = [
      'VALIDATION_FINANCIERE_OK', 'REJET_FINANCIER',
      'VALIDATION_JURIDIQUE_OK',  'REJET_JURIDIQUE',
      'NOUVELLE_AUDIENCE',        'JUGEMENT_RENDU',
      'PV_SOUMIS',                'FACTURE_SOUMISE'
    ];

    this.notifService.nouvelleNotif$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(notif => {
      if (typesAgent.includes(notif.type)) {
        const currentId = Number(this.route.snapshot.paramMap.get('id'));
        if (currentId) this.chargerDossier(currentId);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  chargerDossier(id: number) {
    this.loading = true;
    console.log('📥 Chargement du dossier ID:', id);

    this.dossierService.getDossierDetails(id).subscribe({
      next: (data: any) => {
        console.log('📦 Données reçues:', data);

        let d = data.dossier || data;

        console.log('🔍 Structure du dossier reçu:', {
          id: d.id,
          numeroDossier: d.numeroDossier,
          risques: d.risques,
          clientType: d.clientType,
          clientNom: d.clientNom
        });

        if (!d.risques) {
          console.warn('⚠️ Aucun risque trouvé, initialisation d\'un tableau vide');
          d.risques = [];
        }

        if (d.risques && Array.isArray(d.risques)) {
          d.risques = d.risques.map((risque: any) => ({
            ...risque,
            garanties: risque.garanties || []
          }));
          console.log('✅ Risques après traitement:', d.risques.length);
        }

        d.client = {
          typeClient:    d.clientType          || d.clientTypeClient  || 'PARTICULIER',
          nom:           d.clientNom           || d.client?.nom       || null,
          prenom:        d.clientPrenom        || d.client?.prenom    || null,
          cin:           d.clientCin           || d.client?.cin       || null,
          email:         d.clientEmail         || d.client?.email     || null,
          telephone:     d.clientTelephone     || d.client?.telephone || null,
          adresse:       d.clientAdresse       || d.client?.adresse   || null,
          raisonSociale: d.clientRaisonSociale || d.client?.raisonSociale || null,
          rne:           d.clientRne           || d.client?.rne       || null,
        };

        d.agence = {
          nom:   d.agenceNom   || d.agence?.nom   || null,
          ville: d.agenceVille || d.agence?.ville || null,
        };

        this.dossier                 = d;
        this.historique              = data.historique              || [];
        this.validateurs_financiers  = data.validateurs_financiers  || [];
        this.validateurs_juridiques  = data.validateurs_juridiques  || [];
        this.missionAvocat           = data.missionAvocat           || null;
        this.affaireExiste           = data.affaireExiste           ?? false;
        this.prestationJudiciaire    = data.prestationJudiciaire    || null;

        if (d.validateurFinancierChoisi) {
          this.formValidateurs.validateurFinancier = d.validateurFinancierChoisi;
        }
        if (d.validateurJuridiqueChoisi) {
          this.formValidateurs.validateurJuridique = d.validateurJuridiqueChoisi;
        }

        console.log('✅ Dossier chargé - Nombre de risques:', this.dossier?.risques?.length || 0);
        console.log('✅ Total impayé:', this.getMontantTotal());

        this.loading = false;
      },
      error: (err: any) => {
        console.error('❌ Erreur chargement dossier:', err);
        this.loading = false;
      }
    });
  }

  // ==================== CRÉDITS (RISQUES) ====================

  // CORRECTION 4 : méthode appelée depuis le template pour réinitialiser le formulaire
  openAddRisqueModal() {
    this.erreurRisque = '';
    this.formRisque = {
      type: 'CREDIT_IMMOBILIER',
      montantInitial: 0,
      montantImpaye: 0,
      dateEcheance: '',
      description: ''
    };
    this.showModalRisque = true;
  }

  ajouterRisque() {
    this.erreurRisque = '';
    if (!this.formRisque.montantInitial || this.formRisque.montantInitial <= 0) {
      this.erreurRisque = 'Le montant initial est obligatoire';
      return;
    }
    if (this.formRisque.montantImpaye < 0) {
      this.erreurRisque = 'Le montant impayé ne peut pas être négatif';
      return;
    }

    const riskData = {
      type:           this.formRisque.type,
      montantInitial: this.formRisque.montantInitial,
      montantImpaye:  this.formRisque.montantImpaye,
      dateEcheance:   this.formRisque.dateEcheance || null,
      description:    this.formRisque.description  || null
    };

    this.dossierService.ajouterRisque(this.dossier.id, riskData).subscribe({
      next: () => {
        this.showModalRisque = false;
        this.chargerDossier(this.dossier.id);
      },
      error: (err: any) => {
        this.erreurRisque = err.error?.error || "Erreur lors de l'ajout.";
      }
    });
  }

  modifierRisque(risque: any) {
    const newMontantImpaye = prompt('Nouveau montant impayé:', risque.montantImpaye);
    if (newMontantImpaye && !isNaN(Number(newMontantImpaye))) {
      const updatedRisque = { ...risque, montantImpaye: Number(newMontantImpaye) };
      this.dossierService.modifierRisque(this.dossier.id, risque.id, updatedRisque).subscribe({
        next: () => {
          this.chargerDossier(this.dossier.id);
          alert('✅ Crédit modifié');
        },
        error: (err: any) => alert('❌ Erreur: ' + (err.error?.error))
      });
    }
  }

  supprimerRisque(risqueId: number) {
    if (confirm('Supprimer ce crédit ? Toutes les garanties associées seront également supprimées.')) {
      this.dossierService.supprimerRisque(this.dossier.id, risqueId).subscribe({
        next: () => {
          this.chargerDossier(this.dossier.id);
          alert('✅ Crédit supprimé');
        },
        error: (err: any) => alert('❌ Erreur: ' + (err.error?.error))
      });
    }
  }

  toggleRisque(risque: any) {
    this.dossierService.selectionnerRisque(this.dossier.id, risque.id, !risque.selectionne).subscribe({
      next: () => this.chargerDossier(this.dossier.id),
      error: (err: any) => alert('Erreur: ' + (err.error?.error))
    });
  }

  // ==================== HELPERS AFFICHAGE ====================

  getClientName(): string {
    if (!this.dossier?.client) return '—';
    const c = this.dossier.client;
    if (c.typeClient === 'ENTREPRISE') return c.raisonSociale || '—';
    return `${c.nom || ''} ${c.prenom || ''}`.trim() || '—';
  }

  getStatutClass(statut: string): string {
    const map: Record<string, string> = {
      'OUVERT':          'statut-ouvert',
      'EN_TRAITEMENT':   'statut-en-traitement',
      'EN_CORRECTION':   'statut-en-correction',
      'VALIDE':          'statut-valide',
      'EN_PROCEDURE':    'statut-en-procedure',
      'EN_EXECUTION':    'statut-en-execution',
      'CLOTURE_PARTIEL': 'statut-cloture-partiel',
      'REJETE':          'statut-rejete',
      'CLOTURE':         'statut-cloture'
    };
    return map[statut] || '';
  }

  formatStatut(statut: string): string {
    const map: Record<string, string> = {
      'OUVERT':          'Ouvert',
      'EN_TRAITEMENT':   'En traitement',
      'EN_CORRECTION':   'En correction',
      'VALIDE':          'Validé',
      'EN_PROCEDURE':    'En procédure',
      'EN_EXECUTION':    'En exécution',
      'CLOTURE_PARTIEL': 'Clôture partielle',
      'REJETE':          'Rejeté',
      'CLOTURE':         'Clôturé'
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
    return this.dossier.risques.reduce((sum: number, r: any) => sum + (r.montantImpaye || 0), 0);
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
    return this.dossier?.statut === 'OUVERT'
        || this.dossier?.statut === 'EN_CORRECTION'
        || this.dossier?.statut === 'REJETE';
  }

  // CORRECTION 6 : canSoumettre() limité au statut OUVERT uniquement
  // EN_CORRECTION utilise son propre flux via ressoumettreDossier()
  canSoumettre(): boolean {
    return this.dossier?.statut === 'OUVERT'
        && this.dossier?.risques?.some((r: any) => r.selectionne);
  }

  // ==================== ACTIONS ====================

  ouvrirModificationDossier() {
    if (this.canModifier() && this.dossier?.id) {
      this.router.navigate(['/agent/dossiers', this.dossier.id, 'modifier']);
    }
  }

  fermerModalModifier() {
    this.showModalModifier = false;
    this.erreurModifier    = '';
    this.saving            = false;
  }

  enregistrerModification() {
    this.erreurModifier = '';
    if (!this.formModifier.libelle?.trim()) {
      this.erreurModifier = 'Le libellé est obligatoire.';
      return;
    }
    this.saving = true;
    this.dossierService.modifierDossier(this.dossier.id, this.formModifier as any).subscribe({
      next: () => {
        this.saving = false;
        this.fermerModalModifier();
        this.chargerDossier(this.dossier.id);
      },
      error: (err: any) => {
        this.erreurModifier = err.error?.error || 'Erreur lors de la modification.';
        this.saving = false;
      }
    });
  }

  supprimerDossier() {
    this.erreurSupprimer  = '';
    this.showModalSupprimer = true;
  }

  fermerModalSupprimer() {
    this.showModalSupprimer = false;
    this.erreurSupprimer    = '';
    this.suppressing        = false;
  }

  confirmerSuppression() {
    this.erreurSupprimer = '';
    this.suppressing     = true;
    this.dossierService.supprimerDossier(this.dossier.id).subscribe({
      next:  () => this.router.navigate(['/agent/dossiers']),
      error: (err: any) => {
        this.erreurSupprimer = err.error?.error || 'Erreur lors de la suppression.';
        this.suppressing     = false;
      }
    });
  }

  ouvrirModalValidateurs() {
    this.erreurValidateurs = '';
    this.showModalValidateurs = true;
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

  supprimerGarantie(garantieId: number) {
    if (!confirm('Supprimer cette garantie ?')) return;
    this.dossierService.supprimerGarantie(garantieId).subscribe({
      next:  () => this.chargerDossier(this.dossier.id),
      error: (err: any) => alert(err.error?.error || 'Erreur suppression garantie')
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

  voirMissions() {
    this.router.navigate(['/agent/dossiers', this.dossier.id, 'missions']);
  }

  lancerProcedure() {
    this.router.navigate(['/agent/dossiers', this.dossier.id, 'prestations', 'lancer']);
  }

  designerAvocat() {
    this.router.navigate([
      '/agent/dossiers', this.dossier.id,
      'prestation', this.prestationJudiciaire.id,
      'designer-avocat'
    ]);
  }

  lancerAffaire(): void {
    this.router.navigate(['/agent/dossiers', this.dossier.id, 'affaire', 'lancer']);
  }

  voirAffaire() {
    this.router.navigate(['/agent/dossiers', this.dossier.id, 'affaire']);
  }

  // CORRECTION 5 : un seul point d'entrée pour la re-soumission
  // Le backend gère les deux cas (financier et/ou juridique)
  ressoumettreDossier() {
    this.erreurResoumission = '';
    this.resoumission = true;
  
    // Construire le payload avec uniquement les validateurs ayant rejeté
    const payload: any = {};
  
    if (this.dossier.validationFinanciere === false) {
      payload.resoumettreFinancier = true;
      payload.validateurFinancier  = this.dossier.validateurFinancierChoisi;
    }
  
    if (this.dossier.validationJuridique === false) {
      payload.resoumettreJuridique = true;
      payload.validateurJuridique  = this.dossier.validateurJuridiqueChoisi;
    }
  
    this.dossierService.ressoumettreDossier(this.dossier.id, payload).subscribe({
      next: () => {
        this.resoumission = false;
        this.chargerDossier(this.dossier.id);
      },
      error: (err: any) => {
        this.erreurResoumission = err.error?.error || 'Erreur lors de la re-soumission.';
        this.resoumission = false;
      }
    });
  }
  refreshDossier() {
    if (this.dossier?.id) {
      console.log('🔄 Rafraîchissement manuel du dossier');
      this.chargerDossier(this.dossier.id);
    }
  }

  refreshRisques(): void {
    if (!this.dossier?.id) return;
    console.log('🔄 Rechargement forcé des risques pour le dossier:', this.dossier.id);
    this.dossierService.getDossierDetails(this.dossier.id).subscribe({
      next: (data: any) => {
        const loadedDossier = data.dossier || data;
        if (loadedDossier.risques) {
          this.dossier.risques = loadedDossier.risques.map((risque: any) => ({
            ...risque,
            garanties: risque.garanties || []
          }));
          this.dossier = { ...this.dossier };
          console.log('✅ Risques rechargés:', this.dossier.risques.length);
        }
      },
      error: (err: any) => {
        console.error('❌ Erreur rechargement risques:', err);
      }
    });
  }

  forceRefreshAfterModification() {
    console.log('🔄 Force refresh after modification');
    const currentId = this.dossier?.id;
    if (currentId) this.chargerDossier(currentId);
  }

  creerMission(): void {
    this.router.navigate(['/agent/dossiers', this.dossier?.id, 'missions', 'creer']);
  }

  voirResultats() {
    this.router.navigate(['/agent/dossiers', this.dossier?.id, 'resultats-prestataires']);
  }
}