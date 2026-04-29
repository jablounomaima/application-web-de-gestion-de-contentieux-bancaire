// src/app/features/auth/compte-desactive/compte-desactive.component.ts

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

@Component({
  selector: 'app-compte-desactive',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="display:flex; flex-direction:column; align-items:center;
                justify-content:center; height:100vh; text-align:center;
                font-family:Arial,sans-serif; background:#fafafa;">

      <div style="background:#fff; border-radius:16px; padding:48px;
                  box-shadow:0 4px 24px rgba(0,0,0,0.08); max-width:420px;">

        <div style="font-size:64px; margin-bottom:16px;">🔒</div>

        <h1 style="color:#c62828; margin:0 0 12px; font-size:24px;">
          Compte désactivé
        </h1>

        <p style="color:#555; line-height:1.6; margin:0 0 32px;">
          Votre compte a été temporairement désactivé par un administrateur.<br>
          Veuillez contacter votre responsable pour plus d'informations.
        </p>

        <button (click)="seDeconnecter()"
                style="background:#c62828; color:#fff; border:none;
                       padding:12px 32px; border-radius:8px; font-size:15px;
                       cursor:pointer;">
          Se déconnecter
        </button>

      </div>
    </div>
  `
})
export class CompteDesactiveComponent {

  constructor(private keycloak: KeycloakService) {}

  seDeconnecter(): void {
    this.keycloak.logout(window.location.origin + '/login');
  }
}