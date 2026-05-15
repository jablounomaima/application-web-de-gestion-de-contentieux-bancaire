import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

import {
  AdminService,
  Agent, Agence, Validateur, AgenceForm,
  AgentCreationRequest, ValidateurCreationRequest
} from '../../../core/services/admin.service';

type TabType = 'agents' | 'agences' | 'validateurs';
type ModalType = 'agent' | 'agence' | 'validateur' | null;
type ModalMode = 'create' | 'edit';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard.component.html',  // ← fichier externe
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {

  activeTab: TabType = 'agents';
  agents: Agent[] = [];
  agences: Agence[] = [];
  validateursJuridiques: Validateur[] = [];
  validateursFinanciers: Validateur[] = [];
  loading = true;
  submitting = false;

  modalVisible: ModalType = null;
  modalMode: ModalMode = 'create';
  editingId: number | null = null;

  // Forms
  agentForm: AgentCreationRequest = this.emptyAgent();
  agenceForm: AgenceForm = this.emptyAgence();
  validateurForm: ValidateurCreationRequest = this.emptyValidateur();

  // Toast notification
  toast = { visible: false, message: '', isError: false };

  // Confirm dialog
  confirmDialog = {
    visible: false,
    title: '',
    message: '',
    onConfirm: () => { }
  };

  constructor(private adminService: AdminService,
    private route: ActivatedRoute ,private keycloakService: KeycloakService) { }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      if (params['tab']) {
        this.activeTab = params['tab'] as TabType;
      }
      this.chargerDonnees();
    });
 }

  // ─── Computed ──────────────────────────────────────────────────────────────

  get agentsActifs(): number {
    return this.agents.filter(a => a.actif).length;
  }

  get validateurs(): Validateur[] {
    return [...this.validateursJuridiques, ...this.validateursFinanciers];
  }

  // ─── Data Loading ──────────────────────────────────────────────────────────

  setTab(tab: TabType) {
    this.activeTab = tab;
    this.chargerDonnees();
  }

  chargerDonnees() {
    this.loading = true;
    if (this.activeTab === 'agents' || this.activeTab === 'agences') {
      this.adminService.getAgents().subscribe({
        next: (data) => { this.agents = data.agents; this.agences = data.agences; this.loading = false; },
        error: () => { this.showToast('Erreur de chargement', true); this.loading = false; }
      });
    } else {
      this.adminService.getValidateurs().subscribe({
        next: (data) => {
          this.validateursJuridiques = data.validateursJuridiques;
          this.validateursFinanciers = data.validateursFinanciers;
          this.agences = data.agences;
          this.loading = false;
        },
        error: () => { this.showToast('Erreur de chargement', true); this.loading = false; }
      });
    }
  }

  getAgenceName(agenceId: number): string {
    return this.agences.find(a => a.id === agenceId)?.nom || '—';
  }

  // ─── Modal Management ──────────────────────────────────────────────────────

  openCreateModal(type: ModalType) {
    this.modalMode = 'create';
    this.editingId = null;
    if (type === 'agent') this.agentForm = this.emptyAgent();
    if (type === 'agence') this.agenceForm = this.emptyAgence();
    if (type === 'validateur') this.validateurForm = this.emptyValidateur();
    this.modalVisible = type;
  }

  openEditModal(type: 'agent' | 'agence' | 'validateur', item: any) {
    this.modalMode = 'edit';
    this.editingId = item.id;

    if (type === 'agent') {
      this.agentForm = {
        nom: item.nom,
        prenom: item.prenom,
        username: item.username || '',
        
        email: item.email,
        matricule: item.matricule,
        telephone: item.telephone,
        dateEmbauche: item.dateEmbauche || '',
        role: 'AGENT',
        agenceId: item.agenceId
      };

    } else if (type === 'agence') {
      this.agenceForm = {
        code: item.code,
        nom: item.nom,
        adresse: item.adresse || '',
        ville: item.ville || '',
        telephone: item.telephone || '',
        email: item.email || '',
        directeur: item.directeur || ''
      };

    } else {
      this.validateurForm = {
        nom: item.nom,
        prenom: item.prenom,
        username: item.username || '',  // ← ajouter
                          // ← vide en édition
        email: item.email,
        matricule: item.matricule,
        telephone: item.telephone || '',
        type: item.typeValidateur,
        agenceId: item.agenceId
      };
    }

    this.modalVisible = type;
  }

  closeModal() {
    this.modalVisible = null;
    this.submitting = false;
    this.editingId = null;
  }

  // ─── Agent CRUD ────────────────────────────────────────────────────────────

  submitAgent() {
    // ✅ Validation — password supprimé
    if (!this.agentForm.nom?.trim()      ||
        !this.agentForm.prenom?.trim()   ||
        !this.agentForm.email?.trim()    ||
        !this.agentForm.matricule?.trim()||
        !this.agentForm.username?.trim() ||
        !this.agentForm.agenceId) {
      this.showToast('Veuillez remplir tous les champs obligatoires', true);
      return;
    }
  
    // ✅ Vérification email basique
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.agentForm.email)) {
      this.showToast('Email invalide', true);
      return;
    }
  
    // ✅ Plus de vérification password — généré automatiquement
    this.agentForm.role = 'AGENT';
    this.submitting = true;
  
    const op$ = this.modalMode === 'create'
      ? this.adminService.createAgent(this.agentForm)
      : this.adminService.updateAgent(this.editingId!, this.agentForm);
  
    op$.subscribe({
      next: (res) => {
        // ✅ Message mentionne l'envoi d'email
        const msg = this.modalMode === 'create'
          ? `Agent créé ! Les identifiants ont été envoyés à ${this.agentForm.email}`
          : (res.message || 'Agent mis à jour');
        this.showToast(msg);
        this.closeModal();
        this.chargerDonnees();
      },
      error: (err) => {
        this.showToast(err.error?.error || 'Erreur', true);
        this.submitting = false;
      }
    });
  }
  
  toggleAgent(id: number) {
    // 🔍 LOG TEMPORAIRE — à supprimer après
    this.keycloakService.getToken().then(token => {
      const payload = JSON.parse(atob(token.split('.')[1]));
      console.log('🔍 Roles:', payload.realm_access?.roles);
    });
  
    this.adminService.toggleAgentStatus(id).subscribe({
      next: (res) => { this.showToast(res.message || 'Statut modifié'); this.chargerDonnees(); },
      error: () => this.showToast('Erreur', true)
    });
  }

  supprimerAgent(id: number): void {
    this.confirmDialog = {
      visible: true,
      title: 'Supprimer l\'agent',
      message: 'Cette action est irréversible. L\'agent ne pourra plus se connecter.',
      onConfirm: () => {
        this.confirmDialog.visible = false;
  
        this.adminService.deleteAgent(id).subscribe({
          next: () => {
            // ✅ Retirer immédiatement de la liste locale
            this.agents = this.agents.filter(a => a.id !== id);
            this.showToast('Agent supprimé avec succès');
          },
          error: (err) => {
            this.showToast(err.error?.error || 'Erreur lors de la suppression', true);
          }
        });
      }
    };
  }

  // ─── Agence CRUD ───────────────────────────────────────────────────────────

  submitAgence() {
    this.submitting = true;
    const op$ = this.modalMode === 'create'
      ? this.adminService.createAgence(this.agenceForm)
      : this.adminService.updateAgence(this.editingId!, this.agenceForm);

    op$.subscribe({
      next: (res) => { this.showToast(res.message || 'Succès'); this.closeModal(); this.chargerDonnees(); },
      error: (err) => { this.showToast(err.error?.error || 'Erreur', true); this.submitting = false; }
    });
  }

  supprimerAgence(id: number) {
    this.confirmDialog = {
      visible: true,
      title: 'Supprimer l\'agence',
      message: 'Toutes les données liées seront affectées. Confirmer ?',
      onConfirm: () => {
        this.confirmDialog.visible = false;
        this.adminService.deleteAgence(id).subscribe({
          next: (res) => { this.showToast(res.message || 'Agence supprimée'); this.chargerDonnees(); },
          error: () => this.showToast('Erreur lors de la suppression', true)
        });
      }
    };
  }

  // ─── Validateur CRUD ───────────────────────────────────────────────────────
submitValidateur() {

 
    // ✅ Validation — password supprimé
    if (!this.validateurForm.nom?.trim()      ||
        !this.validateurForm.prenom?.trim()   ||
        !this.validateurForm.username?.trim() ||
        !this.validateurForm.email?.trim()    ||
        !this.validateurForm.matricule?.trim()||
        !this.validateurForm.agenceId         ||
        !this.validateurForm.type) {
      this.showToast('Veuillez remplir tous les champs obligatoires', true);
      return;
    }
  
    // ✅ Vérification email basique
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.validateurForm.email)) {
      this.showToast('Email invalide', true);
      return;
    }
  
    // ✅ Plus de vérification password — généré automatiquement
    this.submitting = true;
  
    const op$ = this.modalMode === 'create'
      ? this.adminService.createValidateur(this.validateurForm)
      : this.adminService.updateValidateur(this.editingId!, this.validateurForm);
  
    op$.subscribe({
      next: (res) => {
        // ✅ Message mentionne l'envoi d'email
        const msg = this.modalMode === 'create'
          ? `Validateur créé ! Les identifiants ont été envoyés à ${this.validateurForm.email}`
          : (res.message || 'Validateur mis à jour');
        this.showToast(msg);
        this.closeModal();
        this.chargerDonnees();
      },
      error: (err) => {
        this.showToast(err.error?.error || 'Erreur', true);
        this.submitting = false;
      }
    });
  }
  toggleValidateur(id: number) {
    this.adminService.toggleValidateurStatus(id).subscribe({
      next: (res) => {
        // ✅ Mise à jour locale instantanée sans recharger toute la liste
        const msg = res.actif ? 'Validateur activé' : 'Validateur désactivé';
        this.showToast(msg);
  
        // ✅ Mettre à jour localement dans les deux listes
        const updateStatut = (liste: any[]) =>
          liste.map(v => v.id === id ? { ...v, actif: res.actif } : v);
  
        this.validateursJuridiques = updateStatut(this.validateursJuridiques);
        this.validateursFinanciers = updateStatut(this.validateursFinanciers);
      },
      error: () => this.showToast('Erreur toggle validateur', true)
    });
  }

  supprimerValidateur(id: number) {
    this.confirmDialog = {
      visible: true,
      title: 'Supprimer le validateur',
      message: 'Cette action est irréversible. Confirmer la suppression ?',
      onConfirm: () => {
        this.confirmDialog.visible = false;
        this.adminService.deleteValidateur(id).subscribe({
          next: (res) => { this.showToast(res.message || 'Validateur supprimé'); this.chargerDonnees(); },
          error: () => this.showToast('Erreur lors de la suppression', true)
        });
      }
    };
  }

  // ─── Helpers ───────────────────────────────────────────────────────────────

  private showToast(message: string, isError = false) {
    this.toast = { visible: true, message, isError };
    setTimeout(() => this.toast.visible = false, 3500);
  }

  private emptyAgent(): AgentCreationRequest {
    return {
      nom: '',
      prenom: '',
      username: '',
     
      email: '',
      matricule: '',
      telephone: '',
      dateEmbauche: '',
      role: 'AGENT',
      agenceId: 0
    };
  }

  // Remplacer emptyAgence()
  private emptyAgence(): AgenceForm {
    return { code: '', nom: '', adresse: '', ville: '', telephone: '', email: '', directeur: '' };
  }

  private emptyValidateur(): ValidateurCreationRequest {
    return {
      nom: '', prenom: '', username: '',
      email: '', matricule: '', telephone: '',
      type: 'VALIDATEUR_JURIDIQUE', agenceId: 0
    };
  }







  // Dans admin-dashboard.component.ts
reinitialiserMotDePasse(username: string): void {
  this.confirmDialog = {
    visible: true,
    title: 'Réinitialiser le mot de passe',
    message: `Générer un nouveau mot de passe pour "${username}" 
              et l'envoyer par email ?`,
    onConfirm: () => {
      this.confirmDialog.visible = false;
      this.adminService.reinitialiserMotDePasse(username).subscribe({
        next: () => this.showToast(
          `Nouveau mot de passe envoyé à ${username} ✅`
        ),
        error: () => this.showToast('Erreur réinitialisation', true)
      });
    }
  };
}
}