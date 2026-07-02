# Validation des Factures d'Avocat par le Validateur Financier

## Contexte

Actuellement, quand un avocat soumet une facture via `POST /api/avocat/affaires/{affaireId}/facture`, elle est stockée directement sur `AffaireJudiciaire` avec `factureStatut = EN_ATTENTE`. L'agent bancaire peut la valider/rejeter via `POST /api/agent/dossiers/{dossierId}/affaire/{affaireId}/facture/valider` — **sans passer par le validateur financier**.

Le besoin est que ce flux soit redirigé vers le **validateur financier** (rôle `VALIDATEUR_FINANCIER`) qui valide ou rejette avec une description, et que le résultat soit affiché aux deux parties.

## Architecture Actuelle

- `AffaireJudiciaire.factureStatut` : enum `EN_ATTENTE | PAYEE | REJETEE`
- `AffaireJudiciaire.factureRef` / `montantFacture` : champs simples
- Aucun champ pour le **commentaire de rejet** ni l'**identité du validateur**

---

## Proposed Changes

### Backend

---

#### [MODIFY] [AffaireJudiciaire.java](file:///C:/Users/Oumaima/Downloads/contentieux_security/contentieux_security/src/main/java/com/example/contentieux_security/entity/AffaireJudiciaire.java)

Ajouter 3 nouveaux champs à l'entité :
- `factureCommentaireValidation` (TEXT) : description du validateur lors de la validation/rejet
- `factureValidePar` (String) : username du validateur qui a traité
- `factureStatut` : ajouter la valeur `EN_ATTENTE_VALIDATION` (en attente du validateur financier)

> [!IMPORTANT]
> Il faut modifier l'enum `StatutFacture` pour avoir : `EN_ATTENTE_VALIDATION | PAYEE | REJETEE`.
> La valeur `EN_ATTENTE` actuelle devient `EN_ATTENTE_VALIDATION` pour plus de clarté.

---

#### [MODIFY] [AffaireJudiciaireService.java](file:///C:/Users/Oumaima/Downloads/contentieux_security/contentieux_security/src/main/java/com/example/contentieux_security/service/AffaireJudiciaireService.java)

- `soumettreAvocatFacture()` → changer le statut initial à `EN_ATTENTE_VALIDATION`
- `validerFacture(Long affaireId, boolean accepte)` → modifier pour accepter un commentaire + notifier
- Ajouter `validerFactureAvecCommentaire(Long affaireId, boolean accepte, String commentaire, String validePar)`

---

#### [MODIFY] [ValidateurFinancierController.java](file:///C:/Users/Oumaima/Downloads/contentieux_security/contentieux_security/src/main/java/com/example/contentieux_security/controller/ValidateurFinancierController.java)

Ajouter un nouveau endpoint pour lister et valider les factures d'avocats (AffaireJudiciaire) :
- `GET /api/validateur/financier/affaires/factures` → liste toutes les factures d'avocat `EN_ATTENTE_VALIDATION`
- `POST /api/validateur/financier/affaires/{affaireId}/valider-facture` → valide/rejette avec commentaire

Ce endpoint notifie ensuite :
1. L'**avocat** de la décision (validée/rejetée + commentaire)
2. L'**agent bancaire** du dossier de la décision

---

#### [MODIFY] [AgentAffaireController.java](file:///C:/Users/Oumaima/Downloads/contentieux_security/contentieux_security/src/main/java/com/example/contentieux_security/controller/AgentAffaireController.java)

- Le endpoint `validerFacture` de l'agent devient **obsolète** pour ce flux.
- Conserver mais retourner une erreur indiquant que la validation passe par le validateur financier.
- Mettre à jour `voirAffaire` pour exposer `factureCommentaireValidation` et `factureValidePar`.

---

### Frontend

---

#### [MODIFY] voir-affaire component (Agent) — `voir-affaire.component.html` / `.ts`

Dans la vue agent de l'affaire judiciaire :
- Afficher le statut de la facture avec un badge coloré :
  - `EN_ATTENTE_VALIDATION` → 🟡 Badge orange "En attente du validateur financier"
  - `PAYEE` → 🟢 Badge vert "Validée"
  - `REJETEE` → 🔴 Badge rouge "Rejetée"
- Afficher le commentaire de validation/rejet
- Afficher le nom du validateur et la date

---

#### [MODIFY] avocat-dossier-detail component — `.html` / `.ts`

Dans la section "Interventions Détaillées" de la facture :
- Même affichage avec badge coloré selon le statut
- Afficher le commentaire du validateur financier si rejetée

---

#### [NEW] Validateur Financier — section "Factures Avocat"

Ajouter dans l'interface du validateur financier une nouvelle section/onglet "Factures d'avocat" :
- Lister les factures `EN_ATTENTE_VALIDATION` des affaires judiciaires
- Boutons "Valider ✅" / "Rejeter ❌" avec champ description obligatoire pour le rejet

## Open Questions

> [!IMPORTANT]
> **L'interface du Validateur Financier** : existe-t-il déjà un composant Angular pour le validateur financier ? Si oui, faut-il ajouter un onglet dans l'interface existante ou créer une nouvelle page ?

> [!NOTE]
> Le champ `EN_ATTENTE` de l'enum `StatutFacture` va être renommé `EN_ATTENTE_VALIDATION`. Il faudra faire une migration SQL si la base de données contient déjà des enregistrements avec la valeur `EN_ATTENTE`.

## Verification Plan

### Automated Tests
- Compiler le backend : `mvnw compile`

### Manual Verification
1. Soumettre une facture en tant qu'avocat → vérifier statut `EN_ATTENTE_VALIDATION`
2. Se connecter en tant que validateur financier → voir la facture listée
3. Valider/Rejeter avec commentaire → vérifier les notifications reçues par avocat et agent
4. Vérifier l'affichage du statut et du commentaire chez l'avocat et l'agent
