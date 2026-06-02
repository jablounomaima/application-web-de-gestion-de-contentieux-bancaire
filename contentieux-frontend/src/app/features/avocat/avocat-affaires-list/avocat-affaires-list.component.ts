import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AvocatService } from '../../../core/services/avocat.service';
import { ActivatedRoute } from '@angular/router';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-avocat-affaires-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './avocat-affaires-list.component.html',
  styleUrls: ['./avocat-affaires-list.component.scss']
})
export class AvocatAffairesListComponent implements OnInit {

  // ── Data ────────────────────────────────────────────────────────
  stats: any        = null;
  affaires: any[]   = [];
  filtrees: any[]   = [];
  loading           = true;
  search            = '';
  filtreStatut      = '';

  // ── Stats secondaires ────────────────────────────────────────────
  statuts = [
    { key: '',                label: 'Toutes',          icon: '📁' },
    { key: 'EN_COURS',        label: 'En cours',        icon: '⚖️' },
    { key: 'JUGEMENT_RENDU',  label: 'Jugement rendu',  icon: '📜' },
    { key: 'EXECUTION_FORCEE',label: 'Exécution forcée',icon: '⚡' },
    { key: 'TRANSACTION',     label: 'Transaction',     icon: '🤝' },
    { key: 'CLOSE',           label: 'Clôturée',        icon: '✅' }
  ];

  constructor(
    private avocatService: AvocatService,
    private router: Router,
    private route: ActivatedRoute,
    private notifService: NotificationService
  ) {}

  _cibleDossierId: number | null = null;

  private _surlignerAffaire(dossierId: number): void {
    const affaire = this.affaires.find(
      (a: any) => a.dossierId === dossierId
    );
    if (!affaire) {
      // Attendre le chargement
      setTimeout(() => this._surlignerAffaire(dossierId), 300);
      return;
    }
    setTimeout(() => {
      const el = document.getElementById('affaire-' + affaire.id);
      if (!el) return;
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.classList.add('affaire-highlight');
      setTimeout(() => el.classList.remove('affaire-highlight'), 4000);
    }, 300);
  }
ngOnInit(): void {
  this.chargerStats();
  this.chargerAffaires();

  // Écouter queryParams (premier chargement depuis notification)
  this.route.queryParams.subscribe(params => {
    const id = params['dossierId'] ? Number(params['dossierId']) : null;
    if (id) {
      this._cibleDossierId = id;
    }
  });

  // Écouter le service (même URL)
  this.notifService.dossierCible$.subscribe(id => {
    if (id !== null) {
      this._cibleDossierId = null;
      setTimeout(() => {
        this._cibleDossierId = id;
        this.notifService.signalerDossierCible(null);
        this._surlignerAffaire(id);
      }, 50);
    }
  });
}

  // ── Chargement ───────────────────────────────────────────────────

  chargerStats(): void {
    this.avocatService.getDashboard().subscribe({
      next: (s) => this.stats = s,
      error: () => this.stats = null  // fallback sur calcul local
    });
  }

  chargerAffaires(): void {
    this.loading = true;
  
    // ✅ Appeler les deux endpoints en parallèle
    this.avocatService.getDashboard().subscribe({
      next: (s) => this.stats = s,
      error: () => this.stats = null
    });
  
    this.avocatService.getAffaires().subscribe({
      next: (data: any) => {
        this.affaires = (data.affaires || []).map((a: any) => ({
          ...a,
          missionStatut: data.missionStatuts?.[a.id] ?? null,
          missionId:     data.missionIds?.[a.id]     ?? null,
          dossierId:      data.dossierIds?.[a.id]     ?? null, // ← AJOUTER

          // ✅ champs PV / Facture directs
          pvTexte:        a.pvTexte        ?? null,
          pvStatut:       a.pvStatut       ?? null,
          pvFichiers:     a.pvFichiers     ?? [],
          factureRef:     a.factureRef     ?? null,
          montantFacture: a.montantFacture ?? null,
          factureStatut:  a.factureStatut  ?? null,
  
          // ✅ nombre d'audiences depuis la liste
          nombreAudiences: Array.isArray(a.audiences) ? a.audiences.length : 0,
        }));
        this.appliquerFiltres();
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  // ── Filtres ──────────────────────────────────────────────────────

  appliquerFiltres(): void {
    let result = [...this.affaires];

    if (this.filtreStatut) {
      result = result.filter(a => a.statut === this.filtreStatut);
    }

    const q = this.search.toLowerCase().trim();
    if (q) {
      result = result.filter(a =>
        a.numeroAffaire?.toLowerCase().includes(q) ||
        a.tribunal?.toLowerCase().includes(q)      ||
        a.statut?.toLowerCase().includes(q)
      );
    }

    this.filtrees = result;
  }

  setFiltreStatut(key: string): void {
    this.filtreStatut = key;
    this.appliquerFiltres();
  }

  // ── Navigation ───────────────────────────────────────────────────

  gererAudiences(aff: any): void {
    this.router.navigate(['/avocat/affaires', aff.id, 'audiences']);
  }

  gererJugement(aff: any): void {
    this.router.navigate(['/avocat/affaires', aff.id, 'jugement']);
  }

  gererTribunal(aff: any): void {
    this.router.navigate(['/avocat/affaires', aff.id, 'tribunal']);
  }

  gererHonoraires(aff: any): void {
    this.router.navigate(['/avocat/affaires', aff.id, 'honoraires']);
  }

  // ── Helpers affichage ────────────────────────────────────────────

  statutLabel(s: string): string {
    const m: Record<string, string> = {
      EN_COURS:         'En cours',
      JUGEMENT_RENDU:   'Jugement rendu',
      EXECUTION_FORCEE: 'Exécution forcée',
      TRANSACTION:      'Transaction',
      CLOSE:            'Clôturée'
    };
    return m[s] ?? s;
  }

  statutClass(s: string): string {
    const m: Record<string, string> = {
      EN_COURS:         'pill--blue',
      JUGEMENT_RENDU:   'pill--green',
      EXECUTION_FORCEE: 'pill--orange',
      TRANSACTION:      'pill--purple',
      CLOSE:            'pill--grey'
    };
    return m[s] ?? 'pill--grey';
  }

  missionStatutClass(s: string): string {
    const m: Record<string, string> = {
      EN_COURS:        'pill--blue',
      PV_SOUMIS:       'pill--orange',
      FACTURE_SOUMISE: 'pill--purple',
      TERMINEE:        'pill--green',
      ANNULEE:         'pill--grey'
    };
    return m[s] ?? 'pill--grey';
  }

  compterParStatut(key: string): number {
    if (!key) return this.affaires.length;
    return this.affaires.filter(a => a.statut === key).length;
  }

  voirDossier(aff: any): void {
    this.router.navigate(['/avocat/affaires', aff.id, 'dossier']);
  }


  // ── Calculé depuis la liste locale (fallback si backend ne renvoie pas) ──

get totalHonorairesCalcule(): number {
  return this.affaires
    .filter(a => a.montantFacture)
    .reduce((sum, a) => sum + (a.montantFacture || 0), 0);
}



// ════════ Getters calculés depuis affaires[] ════════

get totalAffaires(): number {
  return this.affaires.length;
}

get affairesEnCours(): number {
  return this.affaires.filter(a => a.statut === 'EN_COURS').length;
}

get jugementRendu(): number {
  return this.affaires.filter(a => a.statut === 'JUGEMENT_RENDU').length;
}

get totalAudiences(): number {
  // Somme des audiences de toutes les affaires
  return this.affaires.reduce((sum, a) => {
    const nb = a.nombreAudiences ?? a.audiences?.length ?? 0;
    return sum + nb;
  }, 0);
}



get totalHonoraires(): number {
  return this.affaires
    .filter(a => a.montantFacture && a.factureStatut !== 'REJETEE')
    .reduce((sum, a) => sum + (Number(a.montantFacture) || 0), 0);
}

get pvEnAttente(): number {
  return this.affaires.filter(a => a.pvStatut === 'EN_ATTENTE').length;
}

get facturesEnAttente(): number {
  return this.affaires.filter(a => a.factureStatut === 'EN_ATTENTE').length;
}
// ✅ Audiences à venir — vient du backend (getAudiencesAVenir)
get audiencesAVenir(): number {
  return this.stats?.audiencesAVenir ?? 0;
}



}