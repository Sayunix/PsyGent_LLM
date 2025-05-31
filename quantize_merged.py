from transformers import AutoModelForCausalLM, BitsAndBytesConfig
from pathlib import Path
import torch

merged_fp16 = Path(r"C:\Users\Sosno\Desktop\FH\BA2\Psygent_Pythonskripts\psygent-merged-fullfp16")
out_4bit = Path(r"C:\Users\Sosno\Desktop\FH\BA2\Psygent_Pythonskripts\psygent-merged-4bit")

# 4-Bit Konfiguration
bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_compute_dtype=torch.float16,
    bnb_4bit_quant_type="nf4",
    bnb_4bit_use_double_quant=True,
)

# 1) FP16-Modell laden
model_4bit = AutoModelForCausalLM.from_pretrained(
    merged_fp16,
    quantization_config=bnb_config,
    device_map="auto",
    trust_remote_code=True
)

# 2) 4-Bit-Modell speichern
model_4bit.save_pretrained(out_4bit)
print(f"✅ 4-Bit Modell gespeichert in {out_4bit.resolve()}")
