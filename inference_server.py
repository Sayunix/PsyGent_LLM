from transformers import AutoTokenizer, AutoModelForCausalLM
from pathlib import Path
import torch
from flask import Flask, request, jsonify

app = Flask(__name__)

SYSTEM_MESSAGE = (
    "Du bist PsyGent, ein deutscher Chatbot für psychische Ersthilfe. "
    "Du bist sehr einfühlsam."
    "Vermeide Fachjargon und steife Formulierungen, setze stattdessen einfache Sätze und kurze Absätze. "
    "Antworte immer ausschließlich in einwandfreiem Deutsch. Rede nicht Englisch. "
    "Vermeide Emojis, Sternchen und englische Ausdrücke. "
    "Biete eine Kurztherapie an zur Vorbereitung für "
    "menschliche Therapie."
)

GREETING = "Hallo! Ich bin PsyGent, hier um dir zu helfen. Erzähl mir, was dir gerade auf der Seele liegt."

# Pfad zu deinem gemergten Modell (inkl. tokenizer files!)
MERGED_DIR = Path(r"C:\Users\Sosno\Desktop\FH\BA2\Psygent_Pythonskripts\psygent-merged-4bit")

# Tokenizer & Modell laden
tokenizer = AutoTokenizer.from_pretrained(MERGED_DIR, trust_remote_code=True)
model     = AutoModelForCausalLM.from_pretrained(
    MERGED_DIR,
    device_map="auto",
    trust_remote_code=True
)
model.config.pad_token_id = model.config.eos_token_id
model.eval()

def build_prompt(history: list[dict], user_input: str) -> str:
    # nimm nur die letzten 6 Turns
    turns = history[-6:]
    prompt = f"### System:\n{SYSTEM_MESSAGE}\n\n"
    for turn in turns:
        prompt += f"### User:\n{turn['user']}\n### Assistant:\n{turn['assistant']}\n\n"
    prompt += f"### User:\n{user_input}\n### Assistant:\n"
    return prompt

@app.route("/generate", methods=["POST"])
def generate():
    data     = request.get_json(force=True) or {}
    history  = data.get("history", [])       # [{ "user": "...", "assistant": "..." }, ...]
    user_in  = data.get("prompt", "").strip()
    if not user_in:
        return jsonify(error="Prompt fehlt"), 400

    # Stateless Greeting:
    if not history:
        return jsonify(response=GREETING)

    # Prompt bauen und generieren
    prompt = build_prompt(history, user_in)
    
    with torch.no_grad():
        inputs = tokenizer(prompt, return_tensors="pt", truncation=True, max_length=1024).to(model.device)
        out_ids = model.generate(
            **inputs,
            max_new_tokens=256,
            do_sample=True,
            temperature=0.7,
            top_p=0.9,
            repetition_penalty=1.1,
            no_repeat_ngram_size=3,
            eos_token_id=tokenizer.eos_token_id,
            pad_token_id=tokenizer.pad_token_id,
            early_stopping=False
        )

    # ausgeben
    full  = tokenizer.decode(out_ids[0], skip_special_tokens=True)
    reply = full.split("### Assistant:")[-1].split("### User")[0].strip()
    return jsonify(response=reply)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)

