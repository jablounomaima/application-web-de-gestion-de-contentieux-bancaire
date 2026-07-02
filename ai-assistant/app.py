# app.py
"""
Microservice Flask : "AI Assistant Contentieux Bancaire Intelligent"

Endpoints :
- GET  /api/assistant/health
- POST /api/assistant/chat
- POST /api/assistant/analyze-dossier

Sécurité : header "X-Internal-Api-Key" attendu (appelé depuis Spring Boot,
jamais directement depuis Angular en production).
"""

from flask import Flask, request, jsonify, render_template
from flask_cors import CORS

import config
from ai_client import get_ai_response, AIClientError
from prompts import build_system_prompt, build_analysis_prompt

app = Flask(__name__)
# CORS activé pour faciliter les tests directs depuis Angular en dev.
# En production, privilégier l'appel via le backend Spring Boot.
CORS(app)


def _check_internal_key():
    key = request.headers.get("X-Internal-Api-Key")
    if key != config.INTERNAL_API_KEY:
        return False
    return True


@app.route("/")
def index():
    return render_template("index.html")


@app.route("/api/assistant/health", methods=["GET"])
def health():
    return jsonify({
        "status": "ok",
        "provider": config.AI_PROVIDER,
    })


@app.route("/api/assistant/chat", methods=["POST"])
def chat():
    """
    Body JSON attendu :
    {
      "message": "Quelle est la prochaine étape pour ce dossier ?",
      "role": "AGENT" | "VALIDATEUR_FINANCIER" | "VALIDATEUR_JURIDIQUE" | "AVOCAT",
      "context": {                # optionnel, infos du dossier courant
          "numeroDossier": "DOS-2026-0123",
          "statut": "EN_ATTENTE_VALIDATION",
          "montant": "15 000 TND",
          "dateAudience": "2026-07-02"
      },
      "history": [                # optionnel, échanges précédents
          {"role": "user", "content": "..."},
          {"role": "assistant", "content": "..."}
      ],
      "provider": "gemini"        # optionnel, override config.AI_PROVIDER
    }
    """
    if not _check_internal_key():
        return jsonify({"error": "Non autorisé"}), 401

    data = request.get_json(silent=True) or {}
    user_message = data.get("message", "").strip()
    if not user_message:
        return jsonify({"error": "Le champ 'message' est requis."}), 400

    role = data.get("role", "DEFAULT")
    context = data.get("context") or {}
    history = data.get("history") or []
    provider = data.get("provider")

    system_prompt = build_system_prompt(role, context)

    messages = [{"role": "system", "content": system_prompt}]
    # On garde un historique limité pour rester concis (10 derniers échanges)
    messages.extend(history[-10:])
    messages.append({"role": "user", "content": user_message})

    try:
        answer = get_ai_response(messages, provider=provider)
    except AIClientError as exc:
        return jsonify({"error": str(exc)}), 502

    return jsonify({"response": answer})


@app.route("/api/assistant/analyze-dossier", methods=["POST"])
def analyze_dossier():
    """
    Body JSON attendu :
    {
      "documentText": "...texte extrait du document du dossier...",
      "role": "AVOCAT",
      "provider": "deepseek"   # optionnel
    }
    """
    if not _check_internal_key():
        return jsonify({"error": "Non autorisé"}), 401

    data = request.get_json(silent=True) or {}
    document_text = data.get("documentText", "").strip()
    if not document_text:
        return jsonify({"error": "Le champ 'documentText' est requis."}), 400

    role = data.get("role", "DEFAULT")
    provider = data.get("provider")

    prompt = build_analysis_prompt(document_text, role)
    messages = [{"role": "user", "content": prompt}]

    try:
        answer = get_ai_response(messages, provider=provider)
    except AIClientError as exc:
        return jsonify({"error": str(exc)}), 502

    return jsonify({"analysis": answer})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=config.PORT, debug=True)