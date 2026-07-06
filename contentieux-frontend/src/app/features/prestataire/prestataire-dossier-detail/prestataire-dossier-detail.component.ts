import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PrestataireService } from '../../../core/services/prestataire.service';

@Component({
  selector: 'app-prestataire-dossier-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './prestataire-dossier-detail.component.html',
  styleUrls: ['./prestataire-dossier-detail.component.scss']
})
export class PrestataireDossierDetailComponent implements OnInit {

  missionId!: number;
  mission: any = null;
  dossier: any = null;

  chargement = true;
  erreur: string | null = null;

  // Modal
  modalOuverte  = false;
  submitting    = false;
  successMsg    = '';
  modalError    = '';

  // Formulaire
  pvTexte    = '';
  factureRef = '';
  montant: number | null = null;
  commentaire = '';
  fichiers: File[] = [];

  // Getters statut
  get casRejetAgent():     boolean { return this.mission?.statut === 'REJETEE'; }
  get casRejetFinancier(): boolean { return this.mission?.statut === 'FACTURE_REJETEE'; }
  get casNormal():         boolean { return ['ASSIGNEE', 'EN_COURS'].includes(this.mission?.statut); }
  get peutSoumettre():     boolean {
    return ['ASSIGNEE', 'EN_COURS', 'REJETEE', 'FACTURE_REJETEE'].includes(this.mission?.statut);
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private prestataireService: PrestataireService
  ) {}

  ngOnInit(): void {
    this.missionId = Number(this.route.snapshot.paramMap.get('id'));
    this.charger();
  }

  charger(): void {
    this.prestataireService.getDossierMission(this.missionId).subscribe({
      next: (res: any) => {
        this.mission    = res.mission ?? null;
        this.dossier    = res.dossier ?? null;
        this.chargement = false;
      },
      error: (err: any) => {
        this.erreur     = err?.error?.error ?? 'Erreur chargement dossier';
        this.chargement = false;
      }
    });
  }

  retour(): void {
    this.router.navigate(['/prestataire/missions']);
  }

  // ── Modal ────────────────────────────────────────────────────
  ouvrirModal(): void {
    this.pvTexte     = '';
    this.factureRef  = '';
    this.montant     = null;
    this.commentaire = '';
    this.fichiers    = [];
    this.successMsg  = '';
    this.modalError  = '';
    this.modalOuverte = true;
  }

  fermerModal(): void {
    if (!this.submitting) this.modalOuverte = false;
  }

  onFichiersChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) this.fichiers = Array.from(input.files);
  }

  supprimerFichier(index: number): void {
    this.fichiers.splice(index, 1);
  }

  soumettrResultat(): void {
    this.modalError = '';

    if (this.casRejetAgent) {
      if (!this.pvTexte.trim())               { this.modalError = 'Le PV est obligatoire.'; return; }
      if (!this.factureRef.trim())            { this.modalError = 'La référence facture est obligatoire.'; return; }
      if (!this.montant || this.montant <= 0) { this.modalError = 'Le montant est obligatoire.'; return; }

      this.submitting = true;
      this.prestataireService.resoumettreApresRejetAgent(this.missionId, {
        pvTexte: this.pvTexte, factureRef: this.factureRef,
        montant: this.montant, fichiers: this.fichiers
      }).subscribe({
        next:  (res) => this._onSuccess(res?.message || 'Soumission envoyée avec succès !'),
        error: (err) => this._onError(err)
      });

    } else if (this.casRejetFinancier) {
      if (!this.factureRef.trim())            { this.modalError = 'La référence facture est obligatoire.'; return; }
      if (!this.montant || this.montant <= 0) { this.modalError = 'Le montant est obligatoire.'; return; }

      this.submitting = true;
      this.prestataireService.resoumettreFactureApresRejetFinancier(this.missionId, {
        factureRef: this.factureRef, montant: this.montant, fichiers: this.fichiers
      }).subscribe({
        next:  (res) => this._onSuccess(res?.message || 'Facture resoumise avec succès !'),
        error: (err) => this._onError(err)
      });

    } else if (this.casNormal) {
      if (!this.commentaire.trim() && this.fichiers.length === 0) {
        this.modalError = 'Ajoutez un commentaire ou au moins un fichier.';
        return;
      }
      this.submitting = true;
      const fd = new FormData();
      if (this.commentaire.trim()) fd.append('commentaire', this.commentaire.trim());
      this.fichiers.forEach(f => fd.append('fichiers', f));

      this.prestataireService.soumettreResultat(this.missionId, fd).subscribe({
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
  nomClient(): string {
    const c = this.dossier?.client;
    if (!c) return '—';
    return [c.nom, c.prenom].filter(Boolean).join(' ') || '—';
  }

  montantTotal(): number {
    return this.dossier?.montant
      ?? this.dossier?.risques?.reduce((s: number, r: any) => s + (r.montantImpaye ?? 0), 0)
      ?? 0;
  }

  formatTaille(bytes: number): string {
    if (bytes < 1024)        return bytes + ' o';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' Ko';
    return (bytes / (1024 * 1024)).toFixed(1) + ' Mo';
  }

  statutClass(s: string): string {
    const m: Record<string, string> = {
      OUVERT: 'pill--blue', EN_TRAITEMENT: 'pill--orange',
      VALIDE: 'pill--green', REJETE: 'pill--red',
      CLOS: 'pill--grey', EN_PROCEDURE: 'pill--purple'
    };
    return m[s] ?? 'pill--grey';
  }

  statutLabel(s: string): string {
    const m: Record<string, string> = {
      OUVERT: 'Ouvert', EN_TRAITEMENT: 'En traitement',
      VALIDE: 'Validé', REJETE: 'Rejeté',
      CLOS: 'Clos', EN_PROCEDURE: 'En procédure'
    };
    return m[s] ?? s;
  }

  missionStatutClass(s: string): string {
    const m: Record<string, string> = {
      EN_COURS: 'pill--blue', ASSIGNEE: 'pill--orange',
      PV_SOUMIS: 'pill--purple', FACTURE_SOUMISE: 'pill--green',
      FACTURE_VALIDEE: 'pill--green', FACTURE_PAYEE: 'pill--green',
      VALIDEE_AGENT: 'pill--green',
      REALISEE: 'pill--green',
      FACTURE_REJETEE: 'pill--orange', REJETEE: 'pill--red',
      TERMINEE: 'pill--green', ANNULEE: 'pill--grey'
    };
    return m[s] ?? 'pill--grey';
  }

  missionStatutLabel(s: string): string {
    const m: Record<string, string> = {
      ASSIGNEE: 'Assignée', EN_COURS: 'En cours',
      PV_SOUMIS: 'PV soumis', FACTURE_SOUMISE: 'Facture soumise',
      FACTURE_VALIDEE: 'Facture validée', FACTURE_PAYEE: 'Facture payée',
      FACTURE_REJETEE: 'Facture rejetée',
      REALISEE: 'Réalisée', VALIDEE_AGENT: 'Validée',
      TERMINEE: 'Terminée', REJETEE: 'Rejetée', ANNULEE: 'Annulée'
    };
    return m[s] ?? s;
  }

  formatRisqueType(t: string): string {
    const m: Record<string, string> = {
      CREDIT_IMMOBILIER:    '🏠 Crédit Immobilier',
      CREDIT_CONSOMMATION:  '🛍️ Crédit Consommation',
      CREDIT_AUTO:          '🚗 Crédit Auto',
      CREDIT_PROFESSIONNEL: '💼 Crédit Professionnel',
      LEASING:              '📋 Leasing',
      DECOUVERT:            '🏦 Découvert'
    };
    return m[t] ?? t;
  }

  risques(): any[] { return this.dossier?.risques ?? []; }
}