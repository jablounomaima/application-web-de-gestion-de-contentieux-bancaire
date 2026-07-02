#!/usr/bin/env python3
"""
test_assistant.py — Tests complets de l'AI Assistant Flask
Lancer APRÈS avoir démarré le serveur Flask : python app.py

Usage : python test_assistant.py
"""

import json
import requests

BASE_URL = "http://localhost:5001"
HEADERS = {
    "Content-Type": "application/json",
    "X-Internal-Api-Key": "change-me-dev-key",
}

PASS = "\033[92m✅\033[0m"
FAIL = "\033[91m❌\033[0m"


def titre(text):
    print(f"\n{'='*60}")
    print(f"  {text}")
    print("="*60)


def test(description, status, body, expected_status=200, expected_key=None):
    ok_status = status == expected_status
    ok_key = (expected_key is None) or (expected_key in body)
    icon = PASS if (ok_status and ok_key) else FAIL
    print(f"{icon}  {description}")
    if not ok_status:
        print(f"     Status attendu : {expected_status} | reçu : {status}")
    if not ok_key:
        print(f"     Clé attendue '{expected_key}' absente dans : {body}")


# ────────────────────────────────────────────
# 1. HEALTH CHECK
# ────────────────────────────────────────────
titre("1. HEALTH CHECK")
r = requests.get(f"{BASE_URL}/api/assistant/health")
test("Service disponible (200)", r.status_code, r.json(), 200, "status")
test("Fournisseur IA configuré", r.status_code, r.json(), 200, "provider")
print(f"   → Provider actif : {r.json().get('provider')}")


# ────────────────────────────────────────────
# 2. SÉCURITÉ
# ────────────────────────────────────────────
titre("2. SÉCURITÉ")
r = requests.post(f"{BASE_URL}/api/assistant/chat", json={"message": "test"})
test("Sans clé API → 401", r.status_code, r.json(), 401, "error")

r = requests.post(
    f"{BASE_URL}/api/assistant/chat",
    json={"message": "test"},
    headers={**HEADERS, "X-Internal-Api-Key": "mauvaise-cle"},
)
test("Mauvaise clé API → 401", r.status_code, r.json(), 401, "error")


# ────────────────────────────────────────────
# 3. VALIDATION DES ENTRÉES
# ────────────────────────────────────────────
titre("3. VALIDATION DES ENTRÉES")
r = requests.post(f"{BASE_URL}/api/assistant/chat", json={"message": ""}, headers=HEADERS)
test("Message vide → 400", r.status_code, r.json(), 400, "error")

r = requests.post(
    f"{BASE_URL}/api/assistant/analyze-dossier",
    json={"documentText": ""},
    headers=HEADERS,
)
test("Document vide → 400", r.status_code, r.json(), 400, "error")


# ────────────────────────────────────────────
# 4. CHAT PAR RÔLE (appels Gemini réels)
# ────────────────────────────────────────────
titre("4. CHAT PAR RÔLE (appels Gemini réels)")

scenarios = [
    {
        "role": "AGENT",
        "message": "Quelles sont les prochaines étapes pour ce dossier ?",
        "context": {
            "numeroDossier": "DOS-2026-0001",
            "statut": "EN_ATTENTE_VALIDATION",
            "montant": "12 500 TND",
        },
    },
    {
        "role": "VALIDATEUR_FINANCIER",
        "message": "Y a-t-il des incohérences financières à vérifier ?",
        "context": {
            "numeroDossier": "DOS-2026-0002",
            "statut": "VALIDATION_FINANCIERE",
            "montant": "45 000 TND",
            "factures": "3 factures jointes",
        },
    },
    {
        "role": "AVOCAT",
        "message": "Quels arguments plaider pour ce dossier de recouvrement ?",
        "context": {
            "numeroDossier": "DOS-2026-0003",
            "statut": "EN_COURS_JUDICIAIRE",
            "dateAudience": "2026-07-15",
            "montant": "88 000 TND",
        },
    },
]

for sc in scenarios:
    payload = {
        "message": sc["message"],
        "role": sc["role"],
        "context": sc["context"],
    }
    r = requests.post(f"{BASE_URL}/api/assistant/chat", json=payload, headers=HEADERS)
    test(f"Chat rôle {sc['role']}", r.status_code, r.json(), 200, "response")
    if r.status_code == 200:
        snippet = r.json()["response"][:150].replace("\n", " ")
        print(f"   → {snippet}…")


# ────────────────────────────────────────────
# 5. HISTORIQUE DE CONVERSATION
# ────────────────────────────────────────────
titre("5. CONVERSATION MULTI-TOURS")
payload = {
    "message": "Et si le client conteste la créance ?",
    "role": "AVOCAT",
    "context": {"numeroDossier": "DOS-2026-0003"},
    "history": [
        {"role": "user", "content": "Quels arguments plaider ?"},
        {"role": "assistant", "content": "Voici les arguments recommandés : ..."},
    ],
}
r = requests.post(f"{BASE_URL}/api/assistant/chat", json=payload, headers=HEADERS)
test("Conversation avec historique", r.status_code, r.json(), 200, "response")
if r.status_code == 200:
    snippet = r.json()["response"][:150].replace("\n", " ")
    print(f"   → {snippet}…")


# ────────────────────────────────────────────
# 6. ANALYSE DE DOCUMENT
# ────────────────────────────────────────────
titre("6. ANALYSE DE DOCUMENT")
document = """
MISE EN DEMEURE
Date : 10 mai 2026
Objet : Recouvrement de créance — Dossier N° DOS-2026-0010

Monsieur Ahmed Ben Ali est mis en demeure de régler la somme de 34 750 TND
correspondant à un prêt personnel contracté le 15 mars 2024, arrivé à
échéance le 15 mars 2026 sans remboursement.
Sans régularisation dans un délai de 15 jours, une procédure judiciaire
sera engagée devant le Tribunal de Première Instance de Tunis.
"""
payload = {"documentText": document, "role": "VALIDATEUR_JURIDIQUE"}
r = requests.post(
    f"{BASE_URL}/api/assistant/analyze-dossier", json=payload, headers=HEADERS
)
test("Analyse de mise en demeure", r.status_code, r.json(), 200, "analysis")
if r.status_code == 200:
    snippet = r.json()["analysis"][:250].replace("\n", " ")
    print(f"   → {snippet}…")


# ────────────────────────────────────────────
# RÉSUMÉ
# ────────────────────────────────────────────
print(f"\n{'='*60}")
print("  TESTS TERMINÉS")
print("="*60)
print("Si des tests Gemini échouent avec 502 : vérifiez GEMINI_API_KEY dans .env")
