import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { DossierService } from '../../../core/services/dossier.service';
import { Subject, takeUntil } from 'rxjs';
import { ClientService, Client } from '../../../core/services/client.service';

@Component({
  selector: 'app-agent-dossier-modifier',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './agent-dossier-modifier.component.html',
  styleUrls: ['./agent-dossier-modifier.component.scss']
})
export class AgentDossierModifierComponent implements OnInit, OnDestroy {

  private route          = inject(ActivatedRoute);
  private router         = inject(Router);
  private dossierService = inject(DossierService);
  private cdr            = inject(ChangeDetectorRef);
  private clientService = inject(ClientService);  // ⭐ AJOUTER


  dossierId!: number;
  dossierOriginal: any = null;
  loading = true;
  saving  = false;
  error   = '';
  success = '';

  // 🔥 NOUVEAU : Gestion de l'édition client
  isEditingClient = false;
  originalClientData: any = null;
  clientSaving = false;

  form = {
    libelle:     '',
    description: '',
    notes:       '',
    client: {
        id: null as number | null,
      typeClient:    'PARTICULIER',
      nom:           '',
      prenom:        '',
      raisonSociale: '',
      cin:           '',
      rne:           '',
      email:         '',
      telephone:     '',
      adresse:       ''
    },
    agence: { nom: '', ville: '' },
    montantTotal: 0,
    risques: [] as any[]
  };

  showRisqueModal       = false;
  showGarantieModal     = false;
  showTempGarantieModal = false;

  currentRisqueIndex:         number | null = null;
  currentGarantieRisqueIndex: number | null = null;
  currentGarantieIndex:       number | null = null;
  currentTempGarantieIndex:   number | null = null;
  isEditingRisque = false;

  risqueForm = {
    id:             null as number | null,
    type:           'CREDIT_IMMOBILIER',
    montantInitial: 0,
    montantImpaye:  0,
    dateEcheance:   '',
    description:    '',
    selectionne:    false,
    garanties:      [] as any[]
  };

  garantieForm = {
    id:            null as number | null,
    typeGarantie:  '',
    valeurEstimee: 0,
    description:   ''
  };

  tempGarantieForm = {
    id:            null as number | null,
    typeGarantie:  '',
    valeurEstimee: 0,
    description:   ''
  };

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

  clientTypes = [
    { value: 'PARTICULIER', label: 'Particulier' },
    { value: 'ENTREPRISE',  label: 'Entreprise' }
  ];

  private destroy$ = new Subject<void>();

  // ════════════════════════════════════════════════════
  //  LIFECYCLE
  // ════════════════════════════════════════════════════

  ngOnInit(): void {
    this.dossierId = Number(this.route.snapshot.paramMap.get('id'));
    if (this.dossierId && !isNaN(this.dossierId)) {
      this.loadFullDossier();
    } else {
      this.error   = 'ID du dossier invalide';
      this.loading = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ════════════════════════════════════════════════════
  //  CHARGEMENT
  // ════════════════════════════════════════════════════

  private loadFullDossier(): void {
    this.loading = true;
    this.error = '';
    
    console.log('=== CHARGEMENT DOSSIER MODIFICATION ===');
    console.log('Dossier ID:', this.dossierId);
    
    this.dossierService.getDossierDetails(this.dossierId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data: any) => {
          console.log('📦 Réponse complète:', data);
          
          // Extraire le dossier
          const dossier = data.dossier || data;
          this.dossierOriginal = dossier;
          
          // AFFICHER TOUTES LES INFORMATIONS CLIENT
          console.log('🔍 INFORMATIONS CLIENT:');
          console.log('  - dossier.client:', dossier.client);
          console.log('  - dossier.clientId:', dossier.clientId);
          console.log('  - dossier.client?.id:', dossier.client?.id);
          console.log('  - Toutes les clés:', Object.keys(dossier));
          
          // Récupérer l'ID client
          const clientId = dossier.client?.id || dossier.clientId;
          console.log('  - Client ID récupéré:', clientId);
          
          // Client data mapping
          let clientData: any = {};
          
          if (dossier.client && typeof dossier.client === 'object') {
            clientData = {
              id: dossier.client.id || null,
              nom: dossier.client.nom || '',
              prenom: dossier.client.prenom || '',
              raisonSociale: dossier.client.raisonSociale || '',
              typeClient: dossier.client.typeClient || 'PARTICULIER',
              cin: dossier.client.cin || '',
              rne: dossier.client.rne || '',
              email: dossier.client.email || '',
              telephone: dossier.client.telephone || '',
              adresse: dossier.client.adresse || ''
            };
            console.log('✅ Client trouvé dans dossier.client');
          } 
          else if (dossier.clientNom || dossier.clientPrenom || dossier.clientRaisonSociale) {
            clientData = {
              id: dossier.clientId || null,
              nom: dossier.clientNom || '',
              prenom: dossier.clientPrenom || '',
              raisonSociale: dossier.clientRaisonSociale || '',
              typeClient: dossier.clientTypeClient || dossier.typeClient || 'PARTICULIER',
              cin: dossier.clientCin || '',
              rne: dossier.clientRne || '',
              email: dossier.clientEmail || '',
              telephone: dossier.clientTelephone || '',
              adresse: dossier.clientAdresse || ''
            };
            console.log('✅ Client trouvé dans les champs plats');
          } 
          else if (dossier.dossierClient) {
            clientData = {
              id: dossier.dossierClient.id || null,
              nom: dossier.dossierClient.nom || '',
              prenom: dossier.dossierClient.prenom || '',
              raisonSociale: dossier.dossierClient.raisonSociale || '',
              typeClient: dossier.dossierClient.typeClient || 'PARTICULIER',
              cin: dossier.dossierClient.cin || '',
              rne: dossier.dossierClient.rne || '',
              email: dossier.dossierClient.email || '',
              telephone: dossier.dossierClient.telephone || '',
              adresse: dossier.dossierClient.adresse || ''
            };
            console.log('✅ Client trouvé dans dossier.dossierClient');
          }
          
          console.log('📋 Client data final:', clientData);
          
          // AJOUTER L'ID CLIENT DANS LE FORMULAIRE
          this.form = {
            libelle: dossier.libelle || '',
            description: dossier.description || '',
            notes: dossier.notes || '',
            client: {
              id: clientData.id || null,
              typeClient: clientData.typeClient || 'PARTICULIER',
              nom: clientData.nom || '',
              prenom: clientData.prenom || '',
              raisonSociale: clientData.raisonSociale || '',
              cin: clientData.cin || '',
              rne: clientData.rne || '',
              email: clientData.email || '',
              telephone: clientData.telephone || '',
              adresse: clientData.adresse || ''
            },
            agence: {
              nom: dossier.agence?.nom || dossier.agenceNom || '',
              ville: dossier.agence?.ville || dossier.agenceVille || ''
            },
            montantTotal: this.calculateTotalImpaye(dossier.risques),
            risques: dossier.risques || []
          };
          
          console.log('📋 Client ID dans le formulaire:', this.form.client.id);
          console.log('📋 Client complet dans le formulaire:', this.form.client);
          
          this.loading = false;
        },
        error: (err: any) => {
          console.error('❌ Erreur chargement dossier:', err);
          this.error = err.error?.error || err.message || 'Impossible de charger le dossier';
          this.loading = false;
        }
      });
  }

  private calculateTotalImpaye(risques: any[]): number {
    return (risques || []).reduce((sum: number, r: any) => sum + (r.montantImpaye || 0), 0);
  }

  // ════════════════════════════════════════════════════
  //  🔥 NOUVEAU : GESTION CLIENT
  // ════════════════════════════════════════════════════

  toggleClientEditing(): void {
    if (this.isEditingClient) {
      // Annuler les modifications
      this.form.client = JSON.parse(JSON.stringify(this.originalClientData));
    }
    this.isEditingClient = !this.isEditingClient;
  }

  hasClientChanges(): boolean {
    return JSON.stringify(this.form.client) !== JSON.stringify(this.originalClientData);
  }

  async saveClientOnly(): Promise<void> {
    if (!this.validateClientForm()) {
      return;
    }

    this.clientSaving = true;
    this.error = '';

    try {
        const clientPayload: Omit<Client, 'id'> = {
            typeClient:    this.form.client.typeClient,
            nom:           this.form.client.nom?.trim() || '',
            prenom:        this.form.client.prenom?.trim() || '',
            raisonSociale: this.form.client.raisonSociale?.trim() || '',
            cin:           this.form.client.cin?.trim() || '',
            rne:           this.form.client.rne?.trim() || '',
            email:         this.form.client.email?.trim() || '',
            telephone:     this.form.client.telephone?.trim() || '',
            adresse:       this.form.client.adresse?.trim() || ''
          };

      // Appel API pour mettre à jour le client
      await this.clientService.modifierClient(this.dossierOriginal.client?.id, clientPayload).toPromise();

      // Mettre à jour les données originales
      this.originalClientData = JSON.parse(JSON.stringify(this.form.client));
      this.isEditingClient = false;
      this.success = 'Client modifié avec succès !';
      
      setTimeout(() => { this.success = ''; }, 3000);
    } catch (err: any) {
      this.error = err.error?.message || err.error?.error || err.message || 'Erreur lors de la modification du client';
    } finally {
      this.clientSaving = false;
    }
  }

  validateClientForm(): boolean {
    if (this.form.client.typeClient === 'PARTICULIER') {
      if (!this.form.client.nom?.trim()) {
        this.error = 'Le nom est obligatoire pour un particulier';
        return false;
      }
      if (!this.form.client.prenom?.trim()) {
        this.error = 'Le prénom est obligatoire pour un particulier';
        return false;
      }
      if (!this.form.client.cin?.trim()) {
        this.error = 'Le CIN est obligatoire pour un particulier';
        return false;
      }
    } else if (this.form.client.typeClient === 'ENTREPRISE') {
      if (!this.form.client.raisonSociale?.trim()) {
        this.error = 'La raison sociale est obligatoire pour une entreprise';
        return false;
      }
      if (!this.form.client.rne?.trim()) {
        this.error = 'Le RNE est obligatoire pour une entreprise';
        return false;
      }
    }
    return true;
  }

  // ════════════════════════════════════════════════════
  //  HELPERS
  // ════════════════════════════════════════════════════

  canModify(): boolean {
    return ['OUVERT', 'REJETE'].includes(this.dossierOriginal?.statut);
  }

  getStatusLabel(): string {
    const map: Record<string, string> = {
      'OUVERT':        'Ouvert',
      'EN_TRAITEMENT': 'En traitement',
      'VALIDE':        'Validé',
      'REJETE':        'Rejeté',
      'CLOS':          'Clos'
    };
    return map[this.dossierOriginal?.statut] || this.dossierOriginal?.statut || 'Inconnu';
  }

  updateTotalAmount(): void {
    this.form.montantTotal = this.form.risques.reduce(
      (sum: number, r: any) => sum + (r.montantImpaye || 0), 0
    );
  }

  trackByIndex(index: number): number {
    return index;
  }

  getRisqueTypeLabel(type: string): string {
    return this.risqueTypes.find(t => t.value === type)?.label || type;
  }

  getGarantieTypeLabel(type: string): string {
    return this.garantieTypes.find(t => t.value === type)?.label || type;
  }

  // ════════════════════════════════════════════════════
  //  GARANTIES DANS LA MODAL RISQUE (temporaires)
  // ════════════════════════════════════════════════════

  openAddGarantieFromRisqueModal(): void {
    this.currentTempGarantieIndex = null;
    this.tempGarantieForm = { id: null, typeGarantie: '', valeurEstimee: 0, description: '' };
    this.showTempGarantieModal = true;
  }

  editGarantieFromRisqueModal(index: number): void {
    const garantie = this.risqueForm.garanties[index];
    this.currentTempGarantieIndex = index;
    this.tempGarantieForm = {
      id:            garantie.id,
      typeGarantie:  garantie.typeGarantie,
      valeurEstimee: garantie.valeurEstimee,
      description:   garantie.description || ''
    };
    this.showTempGarantieModal = true;
  }

  removeGarantieFromRisqueModal(index: number): void {
    if (confirm('Supprimer cette garantie ?')) {
      this.risqueForm.garanties.splice(index, 1);
    }
  }

  saveTempGarantie(): void {
    if (!this.tempGarantieForm.typeGarantie) {
      this.error = 'Le type de garantie est obligatoire';
      return;
    }
    if (!this.tempGarantieForm.valeurEstimee || this.tempGarantieForm.valeurEstimee <= 0) {
      this.error = 'La valeur estimée doit être supérieure à 0';
      return;
    }

    const newGarantie = {
      typeGarantie:  this.tempGarantieForm.typeGarantie,
      valeurEstimee: this.tempGarantieForm.valeurEstimee,
      description:   this.tempGarantieForm.description || ''
    };

    if (this.currentTempGarantieIndex !== null) {
      this.risqueForm.garanties[this.currentTempGarantieIndex] = {
        ...this.risqueForm.garanties[this.currentTempGarantieIndex],
        ...newGarantie
      };
    } else {
      if (!this.risqueForm.garanties) { this.risqueForm.garanties = []; }
      this.risqueForm.garanties.push(newGarantie);
    }

    this.showTempGarantieModal = false;
    this.error = '';
  }

  // ════════════════════════════════════════════════════
  //  RISQUES
  // ════════════════════════════════════════════════════

  openAddRisqueModal(): void {
    this.isEditingRisque    = false;
    this.currentRisqueIndex = null;
    this.risqueForm = {
      id: null, type: 'CREDIT_IMMOBILIER',
      montantInitial: 0, montantImpaye: 0,
      dateEcheance: '', description: '',
      selectionne: false, garanties: []
    };
    this.showRisqueModal = true;
  }

  editRisque(index: number): void {
    const r = this.form.risques[index];
    this.isEditingRisque    = true;
    this.currentRisqueIndex = index;
    this.risqueForm = {
      id:             r.id,
      type:           r.type,
      montantInitial: r.montantInitial,
      montantImpaye:  r.montantImpaye,
      dateEcheance:   r.dateEcheance || '',
      description:    r.description  || '',
      selectionne:    r.selectionne  || false,
      garanties:      [...(r.garanties || [])]
    };
    this.showRisqueModal = true;
  }

  // ── Synchronisation garanties (méthode de classe, pas imbriquée) ──
  private async syncGarantiesSimple(
    risqueId: number,
    oldGaranties: any[],
    newGaranties: any[]
  ): Promise<void> {
    // Supprimer les garanties retirées
    for (const oldG of oldGaranties) {
      if (oldG?.id && !newGaranties.some((g: any) => g.id === oldG.id)) {
        await this.dossierService.supprimerGarantie(oldG.id).toPromise();
      }
    }
    // Ajouter les nouvelles garanties (sans ID = pas encore en base)
    for (const g of newGaranties) {
      if (!g.id) {
        await this.dossierService.ajouterGarantie(risqueId, {
          typeGarantie:  g.typeGarantie,
          valeurEstimee: g.valeurEstimee,
          description:   g.description || null
        }).toPromise();
      }
    }
  }

  async saveRisque(): Promise<void> {
    this.error   = '';
    this.success = '';
    this.saving  = true;
  
    // ── VALIDATION ─────────────────────────────────────────────────
    if (!this.risqueForm.montantInitial || this.risqueForm.montantInitial <= 0) {
      this.error  = 'Le montant initial doit être supérieur à 0';
      this.saving = false;
      return;
    }
  
    if (this.risqueForm.montantImpaye < 0) {
      this.error  = 'Le montant impayé ne peut pas être négatif';
      this.saving = false;
      return;
    }
  
    if (!this.risqueForm.garanties || this.risqueForm.garanties.length === 0) {
      this.error  = '⚠️ Un crédit doit avoir au moins une garantie.';
      this.saving = false;
      return;
    }
  
    try {
      let risqueId!: number;
  
      // ── CAS 1 : MODIFICATION ────────────────────────────────────
      if (this.isEditingRisque && this.currentRisqueIndex !== null) {
  
        const existing = this.form.risques[this.currentRisqueIndex];
        if (!existing?.id) { throw new Error('Risque introuvable'); }
        risqueId = existing.id as number;
  
        const updatePayload = {
          type:           this.risqueForm.type,
          montantInitial: Number(this.risqueForm.montantInitial),
          montantImpaye:  Number(this.risqueForm.montantImpaye),
          dateEcheance:   this.risqueForm.dateEcheance
                            ? this.risqueForm.dateEcheance
                            : null,
          description:    this.risqueForm.description?.trim() || null
        };
  
        console.log('📤 [MODIFICATION] Payload risque:', updatePayload);
  
        await this.dossierService
          .modifierRisque(this.dossierId, risqueId, updatePayload)
          .toPromise();
  
        await this.syncGarantiesSimple(
          risqueId,
          existing.garanties || [],
          this.risqueForm.garanties
        );
  
        this.form.risques[this.currentRisqueIndex] = {
          ...this.risqueForm,
          id: risqueId
        };
      }
  
      // ── CAS 2 : AJOUT ───────────────────────────────────────────
      else {
  
        const addPayload = {
          type:           this.risqueForm.type,
          montantInitial: Number(this.risqueForm.montantInitial),
          montantImpaye:  Number(this.risqueForm.montantImpaye),
          dateEcheance:   this.risqueForm.dateEcheance
                            ? this.risqueForm.dateEcheance
                            : null,
          description:    this.risqueForm.description?.trim() || null
        };
  
        console.log('📤 Payload JSON:', JSON.stringify(addPayload));
  
        const response: any = await this.dossierService
          .ajouterRisque(this.dossierId, addPayload)
          .toPromise();
  
        console.log('📦 Réponse backend:', response);
  
        const extractedId =
          response?.id         ||
          response?.risqueId   ||
          (typeof response === 'number' ? response : null);
  
        if (!extractedId) {
          throw new Error('Impossible de récupérer l\'ID du risque créé');
        }
        risqueId = extractedId as number;
  
        // Créer les garanties une par une
        const garantiesFinales: any[] = [];
  
        for (const g of this.risqueForm.garanties) {
          const garantiePayload = {
            typeGarantie:  g.typeGarantie,
            valeurEstimee: Number(g.valeurEstimee),
            description:   g.description?.trim() || null
          };
  
          console.log('📤 [GARANTIE] Payload:', garantiePayload);
  
          const result: any = await this.dossierService
            .ajouterGarantie(risqueId, garantiePayload)
            .toPromise();
  
          console.log('📦 Réponse garantie:', result);
  
          garantiesFinales.push({
            id:            result?.id || result?.garantieId,
            typeGarantie:  g.typeGarantie,
            valeurEstimee: Number(g.valeurEstimee),
            description:   g.description || ''
          });
        }
  
        this.form.risques.push({
          ...this.risqueForm,
          id:        risqueId,
          garanties: garantiesFinales
        });
      }
  
      this.updateTotalAmount();
      this.showRisqueModal = false;
      this.success = 'Crédit sauvegardé avec succès';
      setTimeout(() => (this.success = ''), 3000);
  
    } catch (err: any) {
      console.error('❌ Status:', err.status);
      console.error('❌ Message:', err.message);
      console.error('❌ Error body:', JSON.stringify(err.error));
      console.error('❌ Full error:', err);
      this.error = err.error?.message || err.error?.error || err.message || 'Erreur lors de la sauvegarde';
    } finally {
      this.saving = false;
    }
  }

  toggleRisqueSelection(index: number): void {
    this.form.risques[index].selectionne = !this.form.risques[index].selectionne;
    this.form.risques = [...this.form.risques];
  }

  async deleteRisque(index: number): Promise<void> {
    const risque = this.form.risques[index];
    if (!risque) { return; }

    if (!confirm('Supprimer ce crédit ? Cette action est irréversible.')) { return; }

    this.saving = true;
    try {
      if (risque.id && risque.id > 0) {
        await this.dossierService.supprimerRisque(this.dossierId, risque.id).toPromise();
      }
      this.form.risques.splice(index, 1);
      this.updateTotalAmount();
      this.success = 'Crédit supprimé avec succès';
      setTimeout(() => { this.success = ''; }, 3000);
    } catch (err: any) {
      this.error = err.error?.error || 'Erreur lors de la suppression';
    } finally {
      this.saving = false;
    }
  }

  // ════════════════════════════════════════════════════
  //  GARANTIES (hors modal risque)
  // ════════════════════════════════════════════════════

  openAddGarantieModal(risqueIndex: number): void {
    this.currentGarantieRisqueIndex = risqueIndex;
    this.currentGarantieIndex       = null;
    this.garantieForm = { id: null, typeGarantie: '', valeurEstimee: 0, description: '' };
    this.showGarantieModal = true;
  }

  editGarantie(risqueIndex: number, garantieIndex: number): void {
    const garantie = this.form.risques[risqueIndex].garanties[garantieIndex];
    this.currentGarantieRisqueIndex = risqueIndex;
    this.currentGarantieIndex       = garantieIndex;
    this.garantieForm = {
      id:            garantie.id,
      typeGarantie:  garantie.typeGarantie,
      valeurEstimee: garantie.valeurEstimee,
      description:   garantie.description || ''
    };
    this.showGarantieModal = true;
  }

  async saveGarantie(): Promise<void> {
    if (!this.garantieForm.typeGarantie) {
      this.error = 'Le type de garantie est obligatoire';
      return;
    }
    if (!this.garantieForm.valeurEstimee || this.garantieForm.valeurEstimee <= 0) {
      this.error = 'La valeur estimée doit être supérieure à 0';
      return;
    }

    const risque = this.form.risques[this.currentGarantieRisqueIndex!];
    if (!risque.id || risque.id < 0) {
      this.error = 'Veuillez d\'abord sauvegarder le crédit';
      return;
    }

    const garantieData = {
      typeGarantie:  this.garantieForm.typeGarantie,
      valeurEstimee: this.garantieForm.valeurEstimee,
      description:   this.garantieForm.description || null
    };

    this.saving = true;
    try {
      if (this.currentGarantieIndex !== null && this.garantieForm.id && this.garantieForm.id > 0) {
        // MODIFICATION
        await this.dossierService.modifierGarantie(this.garantieForm.id, garantieData).toPromise();
        risque.garanties[this.currentGarantieIndex] = {
          ...risque.garanties[this.currentGarantieIndex],
          ...garantieData
        };
      } else {
        // AJOUT
        const result: any = await this.dossierService.ajouterGarantie(risque.id, garantieData).toPromise();
        if (!risque.garanties) { risque.garanties = []; }
        risque.garanties.push({
          id:            result?.id || result?.garantieId,
          typeGarantie:  this.garantieForm.typeGarantie,
          valeurEstimee: this.garantieForm.valeurEstimee,
          description:   this.garantieForm.description || ''
        });
      }

      this.form.risques = [...this.form.risques];
      this.showGarantieModal = false;
      this.resetGarantieForm();
      this.error   = '';
      this.success = 'Garantie sauvegardée avec succès !';
      setTimeout(() => { this.success = ''; }, 3000);

    } catch (err: any) {
      this.error = err.error?.error || err.message || 'Erreur lors de la sauvegarde';
    } finally {
      this.saving = false;
    }
  }

  resetGarantieForm(): void {
    this.currentGarantieRisqueIndex = null;
    this.currentGarantieIndex       = null;
    this.garantieForm = { id: null, typeGarantie: '', valeurEstimee: 0, description: '' };
  }

  async deleteGarantie(risqueIndex: number, garantieIndex: number): Promise<void> {
    const risque   = this.form.risques[risqueIndex];
    const garantie = risque.garanties[garantieIndex];

    if (!confirm('Supprimer cette garantie ?')) { return; }

    if (garantie.id && garantie.id > 0) {
      try {
        await this.dossierService.supprimerGarantie(garantie.id).toPromise();
      } catch (err) {
        this.error = 'Erreur lors de la suppression';
        return;
      }
    }

    risque.garanties.splice(garantieIndex, 1);
    risque.garanties    = [...risque.garanties];
    this.form.risques   = [...this.form.risques];
    this.success = 'Garantie supprimée';
    setTimeout(() => (this.success = ''), 3000);
  }

  // ════════════════════════════════════════════════════
  //  SAUVEGARDE DOSSIER (MODIFIÉE pour inclure client)
  // ════════════════════════════════════════════════════

  validateForm(): boolean {
    if (!this.form.libelle?.trim()) {
      this.error = 'Le libellé du dossier est obligatoire';
      return false;
    }
    return true;
  }
  async save(): Promise<void> {
  this.error   = '';
  this.success = '';
  this.saving  = true;

  if (!this.validateForm() || !this.canModify()) {
    this.saving = false;
    return;
  }

  try {
    // ── 1. Récupérer l'ID client ──────────────────
    const clientId = this.form.client?.id || this.dossierOriginal?.client?.id;
    
    console.log('🔍 Client ID:', clientId);

    // ── 2. Mettre à jour le CLIENT avec ClientService ──────────────────
    if (clientId && clientId > 0) {
      const clientData: Client = {
        nom: this.form.client.nom?.trim() || '',
        prenom: this.form.client.prenom?.trim() || '',
        typeClient: this.form.client.typeClient,
        cin: this.form.client.cin?.trim() || '',
        raisonSociale: this.form.client.raisonSociale?.trim() || '',
        rne: this.form.client.rne?.trim() || '',
        email: this.form.client.email?.trim() || '',
        telephone: this.form.client.telephone?.trim() || '',
        adresse: this.form.client.adresse?.trim() || ''
      };

      console.log('📤 Mise à jour client:', clientData);
      await this.clientService.modifierClient(clientId, clientData).toPromise();
      console.log('✅ Client mis à jour');
    }

    // ── 3. Modifier les infos de base du dossier ──────────────────
    const baseData = {
      libelle: this.form.libelle.trim(),
      description: this.form.description?.trim() || '',
      notes: this.form.notes?.trim() || ''
    };

    await this.dossierService.modifierDossier(this.dossierId, baseData as any).toPromise();

    // ── 4. Gestion des risques ───────────────────────────────
    const refreshedData: any = await this.dossierService
      .getDossierDetails(this.dossierId).toPromise();
    const existingRisques: any[] = (refreshedData.dossier || refreshedData).risques || [];

    for (const existing of existingRisques) {
      if (!this.form.risques.some((r: any) => r.id === existing.id)) {
        await this.dossierService
          .supprimerRisque(this.dossierId, existing.id).toPromise();
      }
    }

    for (const risque of this.form.risques) {
      if (!risque.id || risque.id < 0) {
        const newR: any = await this.dossierService.ajouterRisque(this.dossierId, {
          type: risque.type,
          montantInitial: Number(risque.montantInitial),
          montantImpaye: Number(risque.montantImpaye),
          dateEcheance: risque.dateEcheance || null,
          description: risque.description || null
        }).toPromise();
        risque.id = newR?.id || newR?.risqueId;
      }

      const orig = existingRisques.find((r: any) => r.id === risque.id);
      if (orig && orig.selectionne !== risque.selectionne) {
        await this.dossierService.selectionnerRisque(
          this.dossierId, risque.id, risque.selectionne
        ).toPromise();
      }
    }

    this.success = 'Dossier modifié avec succès !';
    this.saving = false;
    setTimeout(() => { this.router.navigate(['/agent/dossiers', this.dossierId]); }, 1500);

  } catch (err: any) {
    console.error('❌ Erreur:', err);
    this.error = err.error?.message || err.error?.error || err.message || 'Erreur lors de la modification';
    this.saving = false;
  }
}
  cancel(): void {
    if (confirm('Annuler les modifications ?')) {
      this.router.navigate(['/agent/dossiers', this.dossierId]);
    }
  }


  
}