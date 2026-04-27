import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, Observable } from 'rxjs';
import { 
  DossiersAvocatService, 
  DossierAvocatDTO, 
  AffaireJudiciaire,
  Audience,
  DocumentAffaire
} from './dossiers-avocat.service';

type StatutFiltre = 'TOUS' | 'EN_COURS' | 'JUGEMENT_RENDU' | 'PV_SOUMIS' | 'FACTURE_SOUMISE';
type TriColonne = 'dateLancement' | 'numeroAffaire' | 'clientNom' | 'montantReclame' | 'dateProchainAudience';

@Component({
  selector: 'app-dossiers-avocat',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './dossiers-avocat.component.html',
  styleUrls: ['./dossiers-avocat.component.scss']
})
export class DossiersAvocatComponent implements OnInit {

  dossiers: DossierAvocatDTO[] = [];
  dossiersFiltres: DossierAvocatDTO[] = [];
  dossierSelectionne: DossierAvocatDTO | null = null;
  affaireDetail: AffaireJudiciaire | null = null;

  chargement = false;
  erreur: string | null = null;
  modalOuvert = false;
  modalType: 'DETAIL' | 'PV' | 'FACTURE' | 'AUDIENCES' | 'DOCUMENTS' = 'DETAIL';

  recherche = '';
  statutFiltre: StatutFiltre = 'TOUS';
  private recherche$ = new Subject<string>();

  triColonne: TriColonne = 'dateLancement';
  triAscendant = false;

  pvTexte = '';
  factureRef = '';
  montantFacture: number | null = null;

  stats = {
    total: 0,
    enCours: 0,
    jugementRendu: 0,
    pvSoumis: 0,
    factureSoumise: 0,
    audienceProchaine: 0
  };

  readonly statutsFiltre: { value: StatutFiltre; label: string; icon: string; color: string }[] = [
    { value: 'TOUS', label: 'Tous', icon: 'fa-list', color: '#6c757d' },
    { value: 'EN_COURS', label: 'En cours', icon: 'fa-gavel', color: '#0d6efd' },
    { value: 'JUGEMENT_RENDU', label: 'Jugement rendu', icon: 'fa-balance-scale', color: '#198754' },
    { value: 'PV_SOUMIS', label: 'PV soumis', icon: 'fa-file-alt', color: '#ffc107' },
    { value: 'FACTURE_SOUMISE', label: 'Facture soumise', icon: 'fa-file-invoice', color: '#dc3545' }
  ];

  constructor(private dossiersService: DossiersAvocatService) {}

  ngOnInit(): void {
    this.chargerDossiers();
    
    this.recherche$.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(() => this.appliquerFiltres());
  }

  chargerDossiers(): void {
    this.chargement = true;
    this.erreur = null;

    this.dossiersService.getDossiersAssignes().subscribe({
      next: (dossiers: DossierAvocatDTO[]): void => {
        this.dossiers = dossiers;
        this.appliquerFiltres();
        this.calculerStats();
        this.chargement = false;
      },
      error: (err: any): void => {
        this.erreur = 'Erreur lors du chargement des dossiers : ' + (err?.message || 'Erreur inconnue');
        this.chargement = false;
      }
    });
  }

  onRechercheChange(value: string): void {
    this.recherche = value;
    this.recherche$.next(value);
  }

  filtrerParStatut(statut: StatutFiltre): void {
    this.statutFiltre = statut;
    this.appliquerFiltres();
  }

  appliquerFiltres(): void {
    let result = [...this.dossiers];

    if (this.statutFiltre !== 'TOUS') {
      if (this.statutFiltre === 'PV_SOUMIS') {
        result = result.filter((d: DossierAvocatDTO) => d.pvSoumis && !d.factureSoumise);
      } else if (this.statutFiltre === 'FACTURE_SOUMISE') {
        result = result.filter((d: DossierAvocatDTO) => d.factureSoumise);
      } else {
        result = result.filter((d: DossierAvocatDTO) => d.statutAffaire === this.statutFiltre);
      }
    }

    if (this.recherche.trim()) {
      const terme = this.recherche.toLowerCase().trim();
      result = result.filter((d: DossierAvocatDTO) => 
        d.numeroAffaire.toLowerCase().includes(terme) ||
        d.numeroDossier.toLowerCase().includes(terme) ||
        d.clientNom.toLowerCase().includes(terme) ||
        d.clientPrenom.toLowerCase().includes(terme) ||
        d.typeDossier.toLowerCase().includes(terme) ||
        (d.tribunal && d.tribunal.toLowerCase().includes(terme))
      );
    }

    result.sort((a: DossierAvocatDTO, b: DossierAvocatDTO) => {
      let comparaison = 0;
      switch (this.triColonne) {
        case 'numeroAffaire':
          comparaison = a.numeroAffaire.localeCompare(b.numeroAffaire);
          break;
        case 'clientNom':
          comparaison = `${a.clientNom} ${a.clientPrenom}`.localeCompare(`${b.clientNom} ${b.clientPrenom}`);
          break;
        case 'montantReclame':
          comparaison = a.montantReclame - b.montantReclame;
          break;
        case 'dateProchainAudience':
          const dateA = a.dateProchainAudience ? new Date(a.dateProchainAudience).getTime() : 0;
          const dateB = b.dateProchainAudience ? new Date(b.dateProchainAudience).getTime() : 0;
          comparaison = dateA - dateB;
          break;
        case 'dateLancement':
        default:
          const dateLA = new Date(a.dateLancement).getTime();
          const dateLB = new Date(b.dateLancement).getTime();
          comparaison = dateLA - dateLB;
          break;
      }
      return this.triAscendant ? comparaison : -comparaison;
    });

    this.dossiersFiltres = result;
  }

  trierPar(colonne: TriColonne): void {
    if (this.triColonne === colonne) {
      this.triAscendant = !this.triAscendant;
    } else {
      this.triColonne = colonne;
      this.triAscendant = true;
    }
    this.appliquerFiltres();
  }

  calculerStats(): void {
    const aujourdhui = new Date();
    aujourdhui.setHours(0, 0, 0, 0);

    this.stats = {
      total: this.dossiers.length,
      enCours: this.dossiers.filter((d: DossierAvocatDTO) => d.statutAffaire === 'EN_COURS').length,
      jugementRendu: this.dossiers.filter((d: DossierAvocatDTO) => d.statutAffaire === 'JUGEMENT_RENDU').length,
      pvSoumis: this.dossiers.filter((d: DossierAvocatDTO) => d.pvSoumis && !d.factureSoumise).length,
      factureSoumise: this.dossiers.filter((d: DossierAvocatDTO) => d.factureSoumise).length,
      audienceProchaine: this.dossiers.filter((d: DossierAvocatDTO) => {
        if (!d.dateProchainAudience) return false;
        const dateAudience = new Date(d.dateProchainAudience);
        dateAudience.setHours(0, 0, 0, 0);
        return dateAudience >= aujourdhui;
      }).length
    };
  }

  ouvrirModal(dossier: DossierAvocatDTO, type: typeof this.modalType): void {
    this.dossierSelectionne = dossier;
    this.modalType = type;
    this.modalOuvert = true;
    this.pvTexte = '';
    this.factureRef = '';
    this.montantFacture = null;

    if (type === 'DETAIL') {
      this.chargerDetailAffaire(dossier.affaireId);
    }
  }

  fermerModal(): void {
    this.modalOuvert = false;
    this.dossierSelectionne = null;
    this.affaireDetail = null;
  }

  chargerDetailAffaire(affaireId: number): void {
    this.chargement = true;
    this.dossiersService.getAffaireDetail(affaireId).subscribe({
      next: (affaire: AffaireJudiciaire): void => {
        this.affaireDetail = affaire;
        this.chargement = false;
      },
      error: (err: any): void => {
        this.erreur = 'Erreur chargement détail : ' + (err?.message || 'Erreur inconnue');
        this.chargement = false;
      }
    });
  }

  soumettrePV(): void {
    if (!this.dossierSelectionne || !this.pvTexte.trim()) return;

    this.chargement = true;
    this.dossiersService.soumettrePV(this.dossierSelectionne.affaireId, this.pvTexte.trim()).subscribe({
      next: (): void => {
        this.fermerModal();
        this.chargerDossiers();
      },
      error: (err: any): void => {
        this.erreur = 'Erreur soumission PV : ' + (err?.message || 'Erreur inconnue');
        this.chargement = false;
      }
    });
  }

  soumettreFacture(): void {
    if (!this.dossierSelectionne || !this.factureRef.trim() || !this.montantFacture) return;

    this.chargement = true;
    this.dossiersService.soumettreFacture(
      this.dossierSelectionne.affaireId,
      this.factureRef.trim(),
      this.montantFacture
    ).subscribe({
      next: (): void => {
        this.fermerModal();
        this.chargerDossiers();
      },
      error: (err: any): void => {
        this.erreur = 'Erreur soumission facture : ' + (err?.message || 'Erreur inconnue');
        this.chargement = false;
      }
    });
  }

  getBadgeClass(statut: string): string {
    const classes: Record<string, string> = {
      'EN_COURS': 'badge-en-cours',
      'JUGEMENT_RENDU': 'badge-jugement',
      'CLOTUREE': 'badge-cloturee',
      'PV_SOUMIS': 'badge-pv',
      'FACTURE_SOUMISE': 'badge-facture'
    };
    return classes[statut] || 'badge-default';
  }

  getStatutLabel(statut: string): string {
    const labels: Record<string, string> = {
      'EN_COURS': 'En cours',
      'JUGEMENT_RENDU': 'Jugement rendu',
      'CLOTUREE': 'Clôturée',
      'PV_SOUMIS': 'PV soumis',
      'FACTURE_SOUMISE': 'Facture soumise'
    };
    return labels[statut] || statut;
  }

  formatMontant(montant: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'TND',
      minimumFractionDigits: 2
    }).format(montant);
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '—';
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  }

  getInitials(nom: string, prenom: string): string {
    return `${nom?.charAt(0) || ''}${prenom?.charAt(0) || ''}`.toUpperCase();
  }

  getAvatarColor(nom: string): string {
    const colors = ['#e74c3c', '#3498db', '#2ecc71', '#f39c12', '#9b59b6', '#1abc9c', '#e67e22'];
    let hash = 0;
    for (let i = 0; i < nom.length; i++) {
      hash = nom.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }

  estUrgent(dossier: DossierAvocatDTO): boolean {
    if (!dossier.dateEcheanceMission) return false;
    const echeance = new Date(dossier.dateEcheanceMission);
    const aujourdhui = new Date();
    const diffJours = Math.ceil((echeance.getTime() - aujourdhui.getTime()) / (1000 * 60 * 60 * 24));
    return diffJours <= 7 && diffJours >= 0;
  }

  estEnRetard(dossier: DossierAvocatDTO): boolean {
    if (!dossier.dateEcheanceMission) return false;
    const echeance = new Date(dossier.dateEcheanceMission);
    const aujourdhui = new Date();
    return echeance < aujourdhui;
  }
}