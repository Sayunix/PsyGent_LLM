from transformers import AutoModelForCausalLM
from peft import PeftModel
from pathlib import Path
import torch

model_name   = "meta-llama/Llama-2-7b-chat-hf"
adapter_path = Path(r"C:\Users\Sosno\Desktop\FH\BA2\Psygent_Pythonskripts\psygent-lora-finetuned")
out_dir      = Path(r"C:\Users\Sosno\Desktop\FH\BA2\Psygent_Pythonskripts\psygent-merged-fullfp16")

# 1) Basismodell vollständig auf CPU laden
base = AutoModelForCausalLM.from_pretrained(
    model_name,
    torch_dtype=torch.float16,
    device_map={"": "cpu"},
    trust_remote_code=True
)

# 2) LoRA ebenfalls auf CPU laden
lora = PeftModel.from_pretrained(
    base,
    adapter_path,
    torch_dtype=torch.float16,
    device_map={"": "cpu"}
)

# 3) Merge & Unload – jetzt ein FP16-Modell in CPU-Speicher
merged = lora.merge_and_unload()

# 4) Speichern
merged.save_pretrained(out_dir)
print(f"✅ Gemergt in {out_dir.resolve()}")
