import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DossierService } from '../../../core/services/dossier.service';

@Component({
  selector: 'app-action-validation',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './action-validation.component.html',
  styleUrls: ['./action-validation.component.scss']
})
export class ActionValidationComponent {

  @Input() dossier: any = null;
  @Output() actionSuccess = new EventEmitter<string>();
  @Output() actionError   = new EventEmitter<string>();

  loading = false;

  // ✅ inject() au lieu de constructor
  private dossierService = inject(DossierService);

  ressoumettreDossier(): void {
    if (!this.dossier?.id) return;
    this.loading = true;
    this.dossierService.ressoumettreDossier(this.dossier.id).subscribe({
      next: () => {
        this.loading = false;
        this.actionSuccess.emit('Dossier ressoumis avec succès ✅');
      },
      error: (err: any) => {
        this.loading = false;
        this.actionError.emit(err.error?.error || 'Erreur lors de la ressoumission');
      }
    });
  }

  telechargerPdf(): void {
    if (!this.dossier?.id) return;
    this.loading = true;
    this.dossierService.telechargerPdf(this.dossier.id).subscribe({
      next: (blob: Blob) => {
        this.loading = false;
        const url = window.URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href     = url;
        a.download = `dossier-${this.dossier.numeroDossier}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err: any) => {
        this.loading = false;
        this.actionError.emit(err.error?.error || 'Erreur téléchargement PDF');
      }
    });
  }
}