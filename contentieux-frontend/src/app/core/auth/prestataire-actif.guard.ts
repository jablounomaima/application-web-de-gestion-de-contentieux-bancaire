// src/app/core/auth/prestataire-actif.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

export const PrestataireActifGuard: CanActivateFn = async () => {
  const http   = inject(HttpClient);
  const router = inject(Router);

  try {
    // ✅ Même pattern que AgentActifGuard
    const profil = await firstValueFrom(
      http.get<any>(`${environment.apiUrl}/api/prestataire/mon-profil`)
    );

    if (!profil.actif) {
      router.navigate(['/compte-desactive']);
      return false;
    }
    return true;

  } catch {
    router.navigate(['/compte-desactive']);
    return false;
  }
};