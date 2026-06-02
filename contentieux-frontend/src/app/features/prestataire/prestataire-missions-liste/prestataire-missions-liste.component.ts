import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { PrestataireService } from '../../../core/services/prestataire.service';
import { NotificationService, NotificationDTO } from '../../../core/services/notification.service';

@Component({
  selector: 'app-prestataire-missions-liste',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './prestataire-missions-liste.component.html',
  styleUrls: ['./prestataire-missions-liste.component.scss']
})
export class PrestataireMissionsListeComponent implements OnInit, OnDestroy {

  missions: any[] = [];
  missionsFiltrees: any[] = [];
  loading = true;
  errorMsg = '';

  // Filtres
  filtreStatut    = 'TOUS';
  filtreRecherche = '';

  // Stats
  stats = { total: 0, assignee: 0, enCours: 0, terminee: 0, rejetee: 0 };

  // Modal
  missionSelectionnee: any = null;
  modalOuverte = false;
  submitting   = false;
  successMsg   = '';
  modalError   = '';

  // Formulaire commun
  commentaire = '';
  fichiers: File[] = [];

  // ✅ Champs resoumission
  pvTexte    = '';
  factureRef = '';
  montant: number | null = null;

  readonly STATUTS = [
    { value: 'TOUS',            label: 'Toutes',          color: 'gray'   },
    { value: 'ASSIGNEE',        label: 'Assignées',       color: 'amber'  },
    { value: 'EN_COURS',        label: 'En cours',        color: 'blue'   },
    { value: 'PV_SOUMIS',       label: 'PV soumis',       color: 'purple' },
    { value: 'FACTURE_SOUMISE', label: 'Facture soumise', color: 'teal'   },
    { value: 'FACTURE_REJETEE', label: 'Facture rejetée', color: 'orange' },
    { value: 'VALIDEE_AGENT',   label: 'Validée',         color: 'green'  },
    { value: 'REALISEE',        label: 'Réalisée',        color: 'green'  },
    { value: 'TERMINEE',        label: 'Terminées',       color: 'green'  },
    { value: 'REJETEE',         label: 'Rejetées',        color: 'red'    },
  ];

  // ✅ Getters pour le template
  get casRejetAgent():     boolean { return this.missionSelectionnee?.statut === 'REJETEE'; }
  get casRejetFinancier(): boolean { return this.missionSelectionnee?.statut === 'FACTURE_REJETEE'; }
  get casNormal():         boolean { return ['ASSIGNEE', 'EN_COURS'].includes(this.missionSelectionnee?.statut); }

  private notifSub: Subscription | null = null;
  private _derniereNotifId: number | null = null;

  constructor(
    private prestataireService: PrestataireService,
    private router:             Router,
    private notifService:       NotificationService
  ) {}

  ngOnInit(): void {
    this.charger();
    this._ecouterNotifications();
    
    setTimeout(() => {
      console.log('📋 Structure mission[0]:', JSON.stringify(this.missions[0], null, 2));
    }, 2000);
  }

  ngOnDestroy(): void {
    this.notifSub?.unsubscribe();
  }

  // ── Écoute WebSocket — réagir aux rejets/validations en temps réel ──
  private _ecouterNotifications(): void {
    this.notifSub = this.notifService.notifications$.subscribe(
      (notifs: NotificationDTO[]) => {
        if (!notifs.length) return;
        const derniere = notifs[0];
        if (this._derniereNotifId === derniere.id) return;
        this._derniereNotifId = derniere.id;

        // Recharger si la notif concerne une mission (rejet, cloture, facture)
        const typesConcernes = [
          'MISSION_REJETEE', 'MISSION_CLOTUREE',
          'REJET_FINANCIER', 'VALIDATION_FINANCIERE_OK',
          'NOUVELLE_MISSION', 'MISSION_MODIFIEE'
        ];
        if (typesConcernes.includes(derniere.type)) {
          // Recharger après un court délai pour laisser le backend persister
          setTimeout(() => this.charger(), 800);
        }
      }
    );
  }

  charger(): void {
    this.loading  = true;
    this.errorMsg = '';
    this.prestataireService.getMissions().subscribe({
      next: (res) => {
        this.missions = res.missions || [];
        console.log('📋 Structure mission[0]:', JSON.stringify(this.missions[0], null, 2));
        this.calculerStats();
        this.appliquerFiltres();
        this.loading = false;
      },
      error: (err) => {
        this.errorMsg = err?.error?.error || 'Impossible de charger les missions.';
        this.loading  = false;
      }
    });
  }

  calculerStats(): void {
    this.stats = {
      total:    this.missions.length,
      assignee: this.missions.filter(m => m.statut === 'ASSIGNEE').length,
      enCours:  this.missions.filter(m => m.statut === 'EN_COURS').length,
      terminee: this.missions.filter(m =>
        ['TERMINEE', 'REALISEE', 'VALIDEE_AGENT'].includes(m.statut)
      ).length,
      rejetee:  this.missions.filter(m =>
        ['REJETEE', 'FACTURE_REJETEE'].includes(m.statut)
      ).length,
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

  // ── Modal ────────────────────────────────────────────────────
  ouvrirModal(mission: any): void {
    this.missionSelectionnee = mission;
    this.modalOuverte = true;
    // Reset tous les champs
    this.pvTexte     = '';
    this.factureRef  = '';
    this.montant     = null;
    this.commentaire = '';
    this.fichiers    = [];
    this.successMsg  = '';
    this.modalError  = '';
  }

  fermerModal(): void {
    if (!this.submitting) {
      this.modalOuverte        = false;
      this.missionSelectionnee = null;
    }
  }

  onFichiersChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) this.fichiers = Array.from(input.files);
  }

  supprimerFichier(index: number): void {
    this.fichiers.splice(index, 1);
  }

  // ── Soumission selon le cas ──────────────────────────────────
  soumettrResultat(): void {
    this.modalError = '';

    // CAS 1 — rejet agent : PV + facture + fichiers
    if (this.casRejetAgent) {
      if (!this.pvTexte.trim())              { this.modalError = 'Le PV est obligatoire.'; return; }
      if (!this.factureRef.trim())           { this.modalError = 'La référence facture est obligatoire.'; return; }
      if (!this.montant || this.montant <= 0){ this.modalError = 'Le montant est obligatoire.'; return; }

      this.submitting = true;
      this.prestataireService.resoumettreApresRejetAgent(this.missionSelectionnee.id, {
        pvTexte:    this.pvTexte,
        factureRef: this.factureRef,
        montant:    this.montant,
        fichiers:   this.fichiers
      }).subscribe({
        next:  (res) => this._onSuccess(res?.message || 'Soumission envoyée avec succès !'),
        error: (err) => this._onError(err)
      });

    // CAS 2 — rejet financier : facture seule + fichiers
    } else if (this.casRejetFinancier) {
      if (!this.factureRef.trim())           { this.modalError = 'La référence facture est obligatoire.'; return; }
      if (!this.montant || this.montant <= 0){ this.modalError = 'Le montant est obligatoire.'; return; }

      this.submitting = true;
      this.prestataireService.resoumettreFactureApresRejetFinancier(this.missionSelectionnee.id, {
        factureRef: this.factureRef,
        montant:    this.montant,
        fichiers:   this.fichiers
      }).subscribe({
        next:  (res) => this._onSuccess(res?.message || 'Facture resoumise avec succès !'),
        error: (err) => this._onError(err)
      });

    // CAS NORMAL — première soumission (ASSIGNEE / EN_COURS)
    } else if (this.casNormal) {
      if (!this.commentaire.trim() && this.fichiers.length === 0) {
        this.modalError = 'Ajoutez un commentaire ou au moins un fichier.';
        return;
      }

      this.submitting = true;
      const fd = new FormData();
      if (this.commentaire.trim()) fd.append('commentaire', this.commentaire.trim());
      this.fichiers.forEach(f => fd.append('fichiers', f));

      this.prestataireService.soumettreResultat(this.missionSelectionnee.id, fd).subscribe({
        next:  (res) => this._onSuccess(res?.message || 'Résultat soumis avec succès !'),
        error: (err) => this._onError(err)
      });
    }
  }

  private _onSuccess(message: string): void {
    this.submitting = false;
    this.successMsg = message;
    setTimeout(() => { this.fermerModal(); this.charger(); }, 1800);
  }

  private _onError(err: any): void {
    this.submitting = false;
    this.modalError = err?.error?.error || 'Erreur lors de la soumission.';
  }

  // ── Helpers ──────────────────────────────────────────────────
  voirDetail(id: number): void {
    this.router.navigate(['/prestataire/missions', id]);
  }

  peutSoumettre(m: any): boolean {
    return ['ASSIGNEE', 'EN_COURS', 'REJETEE', 'FACTURE_REJETEE'].includes(m.statut);
  }

  getStatutConfig(statut: string): { label: string; bg: string; color: string } {
    const map: Record<string, { label: string; bg: string; color: string }> = {
      'ASSIGNEE':        { label: 'Assignée',        bg: '#FAEEDA', color: '#633806' },
      'EN_COURS':        { label: 'En cours',         bg: '#E6F1FB', color: '#0C447C' },
      'PV_SOUMIS':       { label: 'PV soumis',        bg: '#EEEDFE', color: '#3C3489' },
      'FACTURE_SOUMISE': { label: 'Facture soumise',  bg: '#E1F5EE', color: '#085041' },
      'FACTURE_REJETEE': { label: 'Facture rejetée',  bg: '#FEF0E7', color: '#7A3B0A' },
      'VALIDEE_AGENT':   { label: 'Validée',          bg: '#EAF3DE', color: '#27500A' },
      'REALISEE':        { label: 'Réalisée',         bg: '#EAF3DE', color: '#27500A' },
      'TERMINEE':        { label: 'Terminée',         bg: '#EAF3DE', color: '#27500A' },
      'REJETEE':         { label: 'Rejetée',          bg: '#FCEBEB', color: '#791F1F' },
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
    if (bytes < 1024)        return bytes + ' o';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' Ko';
    return (bytes / (1024 * 1024)).toFixed(1) + ' Mo';
  }
}