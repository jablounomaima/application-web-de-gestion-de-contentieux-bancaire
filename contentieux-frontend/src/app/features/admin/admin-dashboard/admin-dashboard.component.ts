import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

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
    private route: ActivatedRoute ) { }

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
        password: '',
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
        password: '',                   // ← vide en édition
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
    if (!this.agentForm.nom?.trim() ||
      !this.agentForm.prenom?.trim() ||
      !this.agentForm.email?.trim() ||
      !this.agentForm.matricule?.trim() ||
      !this.agentForm.username?.trim() ||
      !this.agentForm.agenceId) {
      this.showToast('Veuillez remplir tous les champs obligatoires de l’agent', true);
      return;
    }

    if (this.modalMode === 'create' && !this.agentForm.password?.trim()) {
      this.showToast('Le mot de passe initial est obligatoire à la création', true);
      return;
    }

    this.agentForm.role = 'AGENT';
    this.submitting = true;
    const op$ = this.modalMode === 'create'
      ? this.adminService.createAgent(this.agentForm)
      : this.adminService.updateAgent(this.editingId!, this.agentForm);

    op$.subscribe({
      next: (res) => { this.showToast(res.message || 'Succès'); this.closeModal(); this.chargerDonnees(); },
      error: (err) => { this.showToast(err.error?.error || 'Erreur', true); this.submitting = false; }
    });
  }

  toggleAgent(id: number) {
    this.adminService.toggleAgentStatus(id).subscribe({
      next: (res) => { this.showToast(res.message || 'Statut modifié'); this.chargerDonnees(); },
      error: () => this.showToast('Erreur', true)
    });
  }

  supprimerAgent(id: number) {
    this.confirmDialog = {
      visible: true,
      title: 'Supprimer l\'agent',
      message: 'Cette action est irréversible. Confirmer la suppression ?',
      onConfirm: () => {
        this.confirmDialog.visible = false;
        this.adminService.deleteAgent(id).subscribe({
          next: (res) => { this.showToast(res.message || 'Agent supprimé'); this.chargerDonnees(); },
          error: () => this.showToast('Erreur lors de la suppression', true)
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
    if (!this.validateurForm.nom?.trim() ||
      !this.validateurForm.prenom?.trim() ||
      !this.validateurForm.username?.trim() ||
      !this.validateurForm.email?.trim() ||
      !this.validateurForm.matricule?.trim() ||
      !this.validateurForm.agenceId) {
      this.showToast('Veuillez remplir tous les champs obligatoires', true);
      return;
    }
    if (this.modalMode === 'create' && !this.validateurForm.password?.trim()) {
      this.showToast('Le mot de passe initial est obligatoire à la création', true);
      return;
    }

    this.submitting = true;  // ← manquait aussi
    const op$ = this.modalMode === 'create'
      ? this.adminService.createValidateur(this.validateurForm)
      : this.adminService.updateValidateur(this.editingId!, this.validateurForm);

    op$.subscribe({
      next: (res) => { this.showToast(res.message || 'Succès'); this.closeModal(); this.chargerDonnees(); },
      error: (err) => { this.showToast(err.error?.error || 'Erreur', true); this.submitting = false; }
    });
  }

  toggleValidateur(id: number) {
    this.adminService.toggleValidateurStatus(id).subscribe({
      next: (res) => { this.showToast(res.message || 'Statut modifié'); this.chargerDonnees(); },
      error: () => this.showToast('Erreur', true)
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
      password: '',
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
      nom: '', prenom: '', username: '', password: '',
      email: '', matricule: '', telephone: '',
      type: 'VALIDATEUR_JURIDIQUE', agenceId: 0
    };
  }
}