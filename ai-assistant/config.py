import os
from dotenv import load_dotenv

load_dotenv()

# Fournisseur IA actif : "gemini", "deepseek", ou "groq"
AI_PROVIDER = os.getenv("AI_PROVIDER", "groq")

# --- Gemini ---
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-1.5-flash")
GEMINI_URL = (
    f"https://generativelanguage.googleapis.com/v1beta/models/"
    f"{GEMINI_MODEL}:generateContent"
)

# --- DeepSeek ---
DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "")
DEEPSEEK_MODEL = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")
DEEPSEEK_URL = "https://api.deepseek.com/chat/completions"

# --- Groq (gratuit) ---
GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")
GROQ_MODEL = os.getenv("GROQ_MODEL", "llama-3.3-70b-versatile")
GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"

# Clé partagée Spring Boot <-> Flask
INTERNAL_API_KEY = os.getenv("INTERNAL_API_KEY", "change-me-dev-key")

PORT = int(os.getenv("PORT", "5001"))