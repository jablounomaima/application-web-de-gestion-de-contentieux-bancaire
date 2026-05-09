import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-prestataires-liste',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './prestataires-liste.component.html',
  styleUrls: ['./prestataires-liste.component.scss']
})
export class PrestatairesListeComponent implements OnInit {

  private apiUrl = `${environment.apiUrl}/api/agent/prestataires`;

  // ── Données ────────────────────────────────────────────────────
  prestataires: any[] = [];
  filtres:      any[] = [];

  // ── États ──────────────────────────────────────────────────────
  loading      = true;
  togglingId:  number | null = null;
  deletingId:  number | null = null;
  prestataireASupprimer: any = null;

  // ── Filtres ────────────────────────────────────────────────────
  search      = '';
  filtreType: string | null = null;
  filtreActif: boolean | null = null;

  // ── Messages ───────────────────────────────────────────────────
  successMsg = '';
  errorMsg   = '';

  // ── Types ──────────────────────────────────────────────────────
  typeOptions = [
    { value: 'AVOCAT',   label: 'Avocat',   emoji: '⚖️' },
    { value: 'EXPERT',   label: 'Expert',   emoji: '🔬' },
    { value: 'HUISSIER', label: 'Huissier', emoji: '📜' },
  ];

  constructor(private router: Router, private http: HttpClient) {}

  ngOnInit(): void {
    // 🔍 LOG TEMPORAIRE — à supprimer après diagnostic
    const kcInstance = (window as any).keycloak;
    if (kcInstance?.token) {
      const payload = JSON.parse(atob(kcInstance.token.split('.')[1]));
      console.log('🔍 Roles:', payload.realm_access?.roles);
      console.log('🔍 User:', payload.preferred_username);
      console.log('🔍 Expire:', new Date(payload.exp * 1000));
    }
  
    this.charger();
  }
  // ══════════════════════════════════════════════════════════════
  // CHARGEMENT
  // ══════════════════════════════════════════════════════════════

  charger(): void {
    this.loading = true;
    this.http.get<any[]>(this.apiUrl).subscribe({
      next: (data) => {
        this.prestataires = data || [];
        this.appliquerFiltres();
        this.loading = false;
      },
      error: (err) => {
        this.errorMsg = err?.error?.error || 'Impossible de charger les prestataires.';
        this.loading  = false;
      }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // FILTRES
  // ══════════════════════════════════════════════════════════════

  appliquerFiltres(): void {
    let result = [...this.prestataires];

    if (this.search.trim()) {
      const q = this.search.toLowerCase();
      result = result.filter(p =>
        p.nom?.toLowerCase().includes(q)       ||
        p.prenom?.toLowerCase().includes(q)    ||
        p.email?.toLowerCase().includes(q)     ||
        p.username?.toLowerCase().includes(q)  ||
        p.specialite?.toLowerCase().includes(q)
      );
    }

    if (this.filtreType) {
      result = result.filter(p => p.type === this.filtreType);
    }

    if (this.filtreActif !== null) {
      result = result.filter(p => p.actif === this.filtreActif);
    }

    this.filtres = result;
  }

  filtrerParType(type: string | null): void {
    this.filtreType = type;
    this.appliquerFiltres();
  }

  filtrerParActif(actif: boolean | null): void {
    this.filtreActif = actif;
    this.appliquerFiltres();
  }

  reinitialiserFiltres(): void {
    this.search      = '';
    this.filtreType  = null;
    this.filtreActif = null;
    this.appliquerFiltres();
  }

  countParType(type: string): number {
    return this.prestataires.filter(p => p.type === type).length;
  }

  // ══════════════════════════════════════════════════════════════
  // TOGGLE ACTIF
  // ══════════════════════════════════════════════════════════════

  toggleActif(p: any): void {
    this.togglingId = p.id;
    this.http.patch<any>(`${this.apiUrl}/${p.id}/toggle`, {}).subscribe({
      next: (res) => {
        // Mise à jour locale instantanée
        p.actif = res.actif;
        this.appliquerFiltres();
        this.togglingId = null;
        this.afficherSucces(`Prestataire ${res.actif ? 'activé' : 'désactivé'} avec succès.`);
      },
      error: (err) => {
        this.togglingId = null;
        this.errorMsg   = err?.error?.error || 'Erreur lors du changement de statut.';
        setTimeout(() => this.errorMsg = '', 4000);
      }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // SUPPRESSION
  // ══════════════════════════════════════════════════════════════

  confirmerSuppression(p: any): void {
    this.prestataireASupprimer = p;
  }

  annulerSuppression(): void {
    this.prestataireASupprimer = null;
  }

  supprimerPrestataire(): void {
    if (!this.prestataireASupprimer) return;
    this.deletingId = this.prestataireASupprimer.id;

    this.http.delete<any>(`${this.apiUrl}/${this.prestataireASupprimer.id}`).subscribe({
      next: () => {
        // Suppression locale instantanée
        this.prestataires = this.prestataires.filter(p => p.id !== this.prestataireASupprimer.id);
        this.appliquerFiltres();
        this.deletingId            = null;
        this.prestataireASupprimer = null;
        this.afficherSucces('Prestataire supprimé avec succès.');
      },
      error: (err) => {
        this.deletingId = null;
        this.errorMsg   = err?.error?.error || 'Impossible de supprimer ce prestataire.';
        setTimeout(() => this.errorMsg = '', 4000);
      }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // NAVIGATION
  // ══════════════════════════════════════════════════════════════

  naviguerVersCreation(): void {
    this.router.navigate(['/agent/prestataires/nouveau']);
  }
//clique en button modifier fil html 
  naviguerVersModification(id: number) {
    this.router.navigate(['/agent/prestataires/modifier', id]);
  }

  // ══════════════════════════════════════════════════════════════
  // HELPERS
  // ══════════════════════════════════════════════════════════════

  private afficherSucces(msg: string): void {
    this.successMsg = msg;
    setTimeout(() => this.successMsg = '', 4000);
  }

  trackById(_: number, item: any): number { return item.id; }

  initiales(p: any): string {
    const pr = p.prenom?.[0]?.toUpperCase() || '';
    const nm = p.nom?.[0]?.toUpperCase()    || '';
    return pr + nm || '??';
  }

  getLabelType(type: string): string {
    return this.typeOptions.find(t => t.value === type)?.label || type;
  }

  getEmojiType(type: string): string {
    return this.typeOptions.find(t => t.value === type)?.emoji || '👤';
  }



 
}