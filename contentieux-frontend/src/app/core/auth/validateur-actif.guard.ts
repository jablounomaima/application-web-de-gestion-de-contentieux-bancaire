// src/app/core/auth/validateur-actif.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

export const ValidateurActifGuard: CanActivateFn = async () => {
  const http   = inject(HttpClient);
  const router = inject(Router);

  try {
    const profil = await firstValueFrom(
      http.get<any>(`${environment.apiUrl}/api/validateur/mon-profil`)
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