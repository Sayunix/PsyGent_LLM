import torch
from pathlib import Path
from datasets import load_dataset, Features, Value
from transformers import (
    AutoTokenizer,
    AutoModelForCausalLM,
    BitsAndBytesConfig,
    TrainingArguments,
    Trainer
)
from peft import LoraConfig, get_peft_model, prepare_model_for_kbit_training

# ─── 0) Daten-Pfade ──────────────────────────────────────────────────────────────
BASE = Path("C:/Users/Sosno/Desktop/FH/BA2/Datasets")
data_files = {
    "train":      str(BASE / "train.jsonl"),
    "validation": str(BASE / "validation.jsonl")
}

# zwinge prompt/completion als Strings (emojis etc. kein Problem)
features = Features({
    "prompt":     Value("string"),
    "completion": Value("string")
})

# ─── 1) Dataset laden & split testen ──────────────────────────────────────────────
dataset = load_dataset(
    "json",
    data_files=data_files,
    features=features
)

# ─── 2) Tokenizer ──────────────────────────────────────────────────────────────────
model_name = "meta-llama/Llama-2-7b-chat-hf"
tokenizer  = AutoTokenizer.from_pretrained(model_name, trust_remote_code=True)
if tokenizer.pad_token is None:
    tokenizer.pad_token = tokenizer.eos_token

def collate_fn(batch):
    # 1) Nimm die labels raus und speichere sie separat
    labels = [example.pop("labels") for example in batch]

    # 2) pad input_ids & attention_mask (und ggf. token_type_ids)
    batch_encoding = tokenizer.pad(
        batch,
        padding=True,
        return_tensors="pt"
    )

    # 3) Labels auf max Länge bringen und in Tensor verwandeln
    max_label_len = max(len(l) for l in labels)
    padded_labels = [
        l + [-100] * (max_label_len - len(l))  # -100 wird im Loss ignoriert
        for l in labels
    ]
    batch_encoding["labels"] = torch.tensor(padded_labels, dtype=torch.long)

    return batch_encoding

# ─── 3) Basis-Modell in 4-Bit mit Offload laden ────────────────────────────────────
bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_compute_dtype=torch.float16,
    bnb_4bit_use_double_quant=True,
    bnb_4bit_quant_type="nf4",
    llm_int8_enable_fp32_cpu_offload=True
)

base_model = AutoModelForCausalLM.from_pretrained(
    model_name,
    quantization_config=bnb_config,
    device_map="auto",
    trust_remote_code=True
)
base_model.config.pad_token_id = base_model.config.eos_token_id

# ─── 4) Für k-bit-Training vorbereiten & LoRA hinzufügen ───────────────────────────
model = prepare_model_for_kbit_training(base_model)

peft_config = LoraConfig(
    task_type="CAUSAL_LM",
    inference_mode=False,
    r=16,
    lora_alpha=32,
    target_modules=["q_proj", "v_proj"],
    lora_dropout=0.05,
    bias="none"
)
model = get_peft_model(model, peft_config)

# ─── 5) Tokenisierungs-Funktion ───────────────────────────────────────────────────
def tokenize_fn(example):
    prompt     = example["prompt"]
    completion = example["completion"] + tokenizer.eos_token

    # Tokenisiere Prompt + Completion
    inputs = tokenizer(
        prompt + completion,
        truncation=True,
        max_length=512
    )

    # Labels nur auf Completion-Teil legen
    labels = [-100] * len(inputs["input_ids"])
    comp_ids = tokenizer(
        completion,
        truncation=True,
        max_length=256
    )["input_ids"]
    labels[-len(comp_ids):] = comp_ids
    inputs["labels"] = labels
    return inputs

tokenized = dataset.map(tokenize_fn, batched=False)

# ─── 6) Trainings-Argumente ───────────────────────────────────────────────────────
training_args = TrainingArguments(
    output_dir="psygent-lora-finetuned",
    per_device_train_batch_size=4,
    gradient_accumulation_steps=8,
    num_train_epochs=3,
    learning_rate=3e-4,
    fp16=True,
    logging_steps=50,
    evaluation_strategy="steps",
    eval_steps=500,
    save_steps=500,
    save_total_limit=2
)

# ─── 7) Trainer & Training ───────────────────────────────────────────────────────
trainer = Trainer(
    model=model,
    args=training_args,
    train_dataset=tokenized["train"],
    eval_dataset=tokenized["validation"],
    tokenizer=tokenizer,
    data_collator=collate_fn
)

trainer.train()

# ─── 8) Adapter speichern ─────────────────────────────────────────────────────────
model.save_pretrained("psygent-lora-finetuned")
print("✅ Adapter unter psygent-lora-finetuned/ gespeichert")
