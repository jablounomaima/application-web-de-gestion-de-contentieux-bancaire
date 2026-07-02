import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { NavbarComponent } from '../../../layout/navbar/navbar.component';
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
  templateUrl: './admin-dashboard.component.html',
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

  // ── Highlight depuis notification ─────────────────────────
  selectedUsername: string | null = null;
  highlightedAgentId: number | null = null;
  private fromNotification = false;
  // Confirm dialog
  confirmDialog = {
    visible: false,
    title: '',
    message: '',
    onConfirm: () => {}
  };

  constructor(
    private adminService: AdminService,
    private route: ActivatedRoute,
    private router: Router,
    private keycloakService: KeycloakService
  ) {}

  // ════════════════════════════════════════════════════════════
  // Init
  // ════════════════════════════════════════════════════════════
 

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
  
      // ✅ Ignorer l'émission vide causée par le nettoyage de l'URL
      if (this.fromNotification) return;
  
      if (params['tab']) {
        this.activeTab = params['tab'] as TabType;
      }
  
      this.selectedUsername = params['username'] ?? null;
  
      this.chargerDonnees();
  
      // ✅ Nettoyer l'URL seulement si on a des params
      if (params['username'] || params['tab']) {
        this.fromNotification = true;  // ← bloquer le prochain emit
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: {},
          replaceUrl: true
        }).then(() => {
          // ✅ Débloquer après que la navigation soit terminée
          setTimeout(() => this.fromNotification = false, 500);
        });
      }
    });
  }

  // ─── Computed ──────────────────────────────────────────────

  get agentsActifs(): number {
    return this.agents.filter(a => a.actif).length;
  }

  get totalManagedAccounts(): number {
    return this.agents.length + this.agences.length + this.validateursJuridiques.length + this.validateursFinanciers.length;
  }

  get chartSeries(): Array<{ label: string; value: number }> {
    return [
      { label: 'Agents', value: this.agents.length },
      { label: 'Agences', value: this.agences.length },
      { label: 'Valid. J', value: this.validateursJuridiques.length },
      { label: 'Valid. F', value: this.validateursFinanciers.length }
    ];
  }

  get chartMaxValue(): number {
    const values = this.chartSeries.map(item => item.value);
    return Math.max(...values, 1);
  }

  get chartBars(): Array<{ label: string; value: number; percent: number }> {
    return this.chartSeries.map(item => ({
      label: item.label,
      value: item.value,
      percent: Math.round((item.value / this.chartMaxValue) * 100)
    }));
  }

  get activeAgentRatio(): number {
    if (this.agents.length === 0) return 0;
    return Math.round((this.agentsActifs / this.agents.length) * 100);
  }

  get ringCircumference(): number {
    return 2 * Math.PI * 54;
  }

  get ringOffset(): number {
    const progress = this.activeAgentRatio / 100;
    return this.ringCircumference * (1 - progress);
  }

  get validateurs(): Validateur[] {
    return [...this.validateursJuridiques, ...this.validateursFinanciers];
  }

  // ─── Navigation onglets ────────────────────────────────────

  setTab(tab: TabType) {
    this.activeTab          = tab;
    this.highlightedAgentId = null;  // ✅ reset highlight navigation manuelle
    this.selectedUsername   = null;  // ✅ reset username cible
    this.chargerDonnees();
  }

  // ─── Data Loading ──────────────────────────────────────────

  chargerDonnees() {
    this.loading = true;

    if (this.activeTab === 'agents' || this.activeTab === 'agences') {
      this.adminService.getAgents().subscribe({
        next: (data) => {
          this.agents  = data.agents;
          this.agences = data.agences;
          this.loading = false;

          // ✅ Surligner l'agent concerné (uniquement si venu d'une notification)
          if (this.selectedUsername) {
            const agent = this.agents.find(
              a => a.username === this.selectedUsername
            );
            if (agent) {
              this.highlightedAgentId = agent.id;
              setTimeout(() => {
                const el = document.getElementById('agent-row-' + agent.id);
                el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
              }, 300);
              // ✅ Effacer le highlight après 4 secondes
              setTimeout(() => {
                this.highlightedAgentId = null;
                this.selectedUsername   = null;
              }, 4000);
            }
          }
        },
        error: () => {
          this.showToast('Erreur de chargement', true);
          this.loading = false;
        }
      });

    } else {
      // ── tab = validateurs ──────────────────────────────────
      this.adminService.getValidateurs().subscribe({
        next: (data) => {
          this.validateursJuridiques = data.validateursJuridiques;
          this.validateursFinanciers = data.validateursFinanciers;
          this.agences               = data.agences;
          this.loading               = false;

          // ✅ Surligner le validateur concerné (uniquement si venu d'une notification)
          if (this.selectedUsername) {
            const tous = [
              ...this.validateursJuridiques,
              ...this.validateursFinanciers
            ];
            const validateur = tous.find(
              v => v.username === this.selectedUsername
            );
            if (validateur) {
              this.highlightedAgentId = validateur.id;
              setTimeout(() => {
                const el = document.getElementById(
                  'validateur-row-' + validateur.id
                );
                el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
              }, 300);
              // ✅ Effacer le highlight après 4 secondes
              setTimeout(() => {
                this.highlightedAgentId = null;
                this.selectedUsername   = null;
              }, 4000);
            }
          }
        },
        error: () => {
          this.showToast('Erreur de chargement', true);
          this.loading = false;
        }
      });
    }
  }

  getAgenceName(agenceId: number): string {
    return this.agences.find(a => a.id === agenceId)?.nom || '—';
  }

  // ─── Modal Management ──────────────────────────────────────

  openCreateModal(type: ModalType) {
    this.modalMode = 'create';
    this.editingId = null;
    if (type === 'agent')      this.agentForm      = this.emptyAgent();
    if (type === 'agence')     this.agenceForm     = this.emptyAgence();
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
        username: item.username || '',
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
    this.submitting   = false;
    this.editingId    = null;
  }

  // ─── Agent CRUD ────────────────────────────────────────────

  submitAgent() {
    if (!this.agentForm.nom?.trim()       ||
        !this.agentForm.prenom?.trim()    ||
        !this.agentForm.email?.trim()     ||
        !this.agentForm.matricule?.trim() ||
        !this.agentForm.username?.trim()  ||
        !this.agentForm.agenceId) {
      this.showToast('Veuillez remplir tous les champs obligatoires', true);
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.agentForm.email)) {
      this.showToast('Email invalide', true);
      return;
    }
    this.agentForm.role = 'AGENT';
    this.submitting = true;

    const op$ = this.modalMode === 'create'
      ? this.adminService.createAgent(this.agentForm)
      : this.adminService.updateAgent(this.editingId!, this.agentForm);

    op$.subscribe({
      next: (res) => {
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
    this.keycloakService.getToken().then(token => {
      const payload = JSON.parse(atob(token.split('.')[1]));
      console.log('🔍 Roles:', payload.realm_access?.roles);
    });
    this.adminService.toggleAgentStatus(id).subscribe({
      next: (res) => {
        this.showToast(res.message || 'Statut modifié');
        this.chargerDonnees();
      },
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

  // ─── Agence CRUD ───────────────────────────────────────────

  submitAgence() {
    this.submitting = true;
    const op$ = this.modalMode === 'create'
      ? this.adminService.createAgence(this.agenceForm)
      : this.adminService.updateAgence(this.editingId!, this.agenceForm);

    op$.subscribe({
      next: (res) => {
        this.showToast(res.message || 'Succès');
        this.closeModal();
        this.chargerDonnees();
      },
      error: (err) => {
        this.showToast(err.error?.error || 'Erreur', true);
        this.submitting = false;
      }
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
          next: (res) => {
            this.showToast(res.message || 'Agence supprimée');
            this.chargerDonnees();
          },
          error: () => this.showToast('Erreur lors de la suppression', true)
        });
      }
    };
  }

  // ─── Validateur CRUD ───────────────────────────────────────

  submitValidateur() {
    if (!this.validateurForm.nom?.trim()       ||
        !this.validateurForm.prenom?.trim()    ||
        !this.validateurForm.username?.trim()  ||
        !this.validateurForm.email?.trim()     ||
        !this.validateurForm.matricule?.trim() ||
        !this.validateurForm.agenceId          ||
        !this.validateurForm.type) {
      this.showToast('Veuillez remplir tous les champs obligatoires', true);
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.validateurForm.email)) {
      this.showToast('Email invalide', true);
      return;
    }
    this.submitting = true;

    const op$ = this.modalMode === 'create'
      ? this.adminService.createValidateur(this.validateurForm)
      : this.adminService.updateValidateur(this.editingId!, this.validateurForm);

    op$.subscribe({
      next: (res) => {
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
        const msg = res.actif ? 'Validateur activé' : 'Validateur désactivé';
        this.showToast(msg);
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
          next: (res) => {
            this.showToast(res.message || 'Validateur supprimé');
            this.chargerDonnees();
          },
          error: () => this.showToast('Erreur lors de la suppression', true)
        });
      }
    };
  }

  // ─── Mot de passe ──────────────────────────────────────────

  reinitialiserMotDePasse(username: string | undefined): void {
    if (!username) {
      this.showToast('Username introuvable', true);
      return;
    }
    this.confirmDialog = {
      visible: true,
      title: 'Réinitialiser le mot de passe',
      message: `Générer un nouveau mot de passe pour "${username}" et l'envoyer par email ?`,
      onConfirm: () => {
        this.confirmDialog.visible = false;
        this.adminService.reinitialiserMotDePasse(username).subscribe({
          next: () => this.showToast(`Nouveau mot de passe envoyé à ${username} ✅`),
          error: () => this.showToast('Erreur réinitialisation', true)
        });
      }
    };
  }

  // ─── Helpers ───────────────────────────────────────────────

  private showToast(message: string, isError = false) {
    this.toast = { visible: true, message, isError };
    setTimeout(() => this.toast.visible = false, 3500);
  }

  private emptyAgent(): AgentCreationRequest {
    return {
      nom: '', prenom: '', username: '',
      email: '', matricule: '', telephone: '',
      dateEmbauche: '', role: 'AGENT', agenceId: 0
    };
  }

  private emptyAgence(): AgenceForm {
    return {
      code: '', nom: '', adresse: '',
      ville: '', telephone: '', email: '', directeur: ''
    };
  }

  private emptyValidateur(): ValidateurCreationRequest {
    return {
      nom: '', prenom: '', username: '',
      email: '', matricule: '', telephone: '',
      type: 'VALIDATEUR_JURIDIQUE', agenceId: 0
    };
  }
}