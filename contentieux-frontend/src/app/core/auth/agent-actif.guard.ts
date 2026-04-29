// src/app/core/auth/agent-actif.guard.ts

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

export const AgentActifGuard: CanActivateFn = async () => {
  const http   = inject(HttpClient);
  const router = inject(Router);

  try {
    // ✅ Appel API pour vérifier si le compte est actif en DB
    const profil = await firstValueFrom(
      http.get<any>(`${environment.apiUrl}/api/agent/mon-profil`)
    );

    if (!profil.actif) {
      // ✅ Compte désactivé → rediriger vers page dédiée
      router.navigate(['/compte-desactive']);
      return false;
    }

    return true;

  } catch (err) {
    // Si erreur 403 → compte désactivé
    router.navigate(['/compte-desactive']);
    return false;
  }
};