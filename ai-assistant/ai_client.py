# ai_client.py
import requests
import config


class AIClientError(Exception):
    pass


def get_ai_response(messages: list[dict], provider: str | None = None) -> str:
    provider = (provider or config.AI_PROVIDER).lower()
    if provider == "groq":
        return _call_openai_compatible(
            messages,
            api_key=config.GROQ_API_KEY,
            model=config.GROQ_MODEL,
            url=config.GROQ_URL,
            name="Groq",
        )
    elif provider == "deepseek":
        return _call_openai_compatible(
            messages,
            api_key=config.DEEPSEEK_API_KEY,
            model=config.DEEPSEEK_MODEL,
            url=config.DEEPSEEK_URL,
            name="DeepSeek",
        )
    elif provider == "gemini":
        return _call_gemini(messages)
    else:
        raise AIClientError(f"Fournisseur IA inconnu : {provider}")


def _call_openai_compatible(
    messages: list[dict],
    api_key: str,
    model: str,
    url: str,
    name: str,
) -> str:
    """Appel générique format OpenAI (Groq, DeepSeek, OpenRouter...)"""
    if not api_key:
        raise AIClientError(f"{name} API key manquante dans la configuration.")

    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json",
    }
    payload = {
        "model": model,
        "messages": messages,
        "temperature": 0.3,
    }
    resp = requests.post(url, headers=headers, json=payload, timeout=60)
    if resp.status_code != 200:
        raise AIClientError(f"Erreur {name} ({resp.status_code}) : {resp.text}")
    data = resp.json()
    try:
        return data["choices"][0]["message"]["content"]
    except (KeyError, IndexError) as exc:
        raise AIClientError(f"Réponse {name} inattendue : {data}") from exc


def _call_gemini(messages: list[dict]) -> str:
    if not config.GEMINI_API_KEY:
        raise AIClientError("GEMINI_API_KEY manquante dans la configuration.")

    system_text = ""
    conversation = []

    for msg in messages:
        role = msg.get("role")
        content = msg.get("content", "")
        if role == "system":
            system_text = content
        elif role == "assistant":
            conversation.append({"role": "model", "parts": [{"text": content}]})
        else:
            conversation.append({"role": "user", "parts": [{"text": content}]})

    # Injecter le system prompt dans le premier message user
    if system_text and conversation:
        first_user = next((m for m in conversation if m["role"] == "user"), None)
        if first_user:
            original = first_user["parts"][0]["text"]
            first_user["parts"][0]["text"] = f"{system_text}\n\n---\n\n{original}"

    payload = {"contents": conversation}
    url = (
        f"https://generativelanguage.googleapis.com/v1beta/models/"
        f"{config.GEMINI_MODEL}:generateContent?key={config.GEMINI_API_KEY}"
    )
    resp = requests.post(url, json=payload, timeout=60)
    if resp.status_code != 200:
        raise AIClientError(f"Erreur Gemini ({resp.status_code}) : {resp.text}")
    data = resp.json()
    try:
        return data["candidates"][0]["content"]["parts"][0]["text"]
    except (KeyError, IndexError) as exc:
        raise AIClientError(f"Réponse Gemini inattendue : {data}") from exc