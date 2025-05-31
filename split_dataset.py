import pandas as pd
import json
from pathlib import Path
from sklearn.model_selection import train_test_split

# Pfad & System-Message
EXCEL_DIR = Path(r"C:/Users/Sosno/Desktop/FH/BA2/Datasets")
SYSTEM_MESSAGE = (
    "Du bist PsyGent, ein deutscher Chatbot für psychische Ersthilfe. "
    "Antworte **immer** ausschließlich in einwandfreiem Deutsch. "
    "ohne Tippfehler, unvollständige oder merkwürdige Formulierungen. "
    "Vermeide sämtliche englischen Ausdrücke, Emojis oder *-Aktionen."
    "Verwende informelle Anrede und biete eine Kurztherapie an zur Vorbereitung für menschliche Therapie."
)

#  1) Excel laden 
df_relax   = pd.read_excel(EXCEL_DIR/"psygent_dataset_entspannungstechniken.xlsx", sheet_name=0)
df_contact = pd.read_excel(EXCEL_DIR/"psygent_dataset_kontaktaufnahme.xlsx", sheet_name=0)
df_relax.columns   = df_relax.columns.str.strip().str.lower()
df_contact.columns = df_contact.columns.str.strip().str.lower()

#  2) Kategorien & vereinigen 
df_relax["category"]="entspannung"
df_contact["category"]="kontakt"
df = pd.concat([df_relax, df_contact], ignore_index=True)[["prompt","completion","category"]]

#  3) train/val/test Split 
train_df, temp = train_test_split(df, test_size=0.2, random_state=42, 
                                  stratify=df["category"], shuffle=True)
val_df, test_df= train_test_split(temp, test_size=0.5, random_state=42, 
                                  stratify=temp["category"], shuffle=True)

#  4) JSONL-Export mit Markern 
for split_name, split_df in [("train",train_df),("validation",val_df),("test",test_df)]:
    out = EXCEL_DIR/f"{split_name}.jsonl"
    with open(out, "w", encoding="utf-8") as f:
        for _, row in split_df.iterrows():
            prompt  = (
                f"### System:\n{SYSTEM_MESSAGE}\n"
                f"### User:\n{row['prompt']}\n"
                f"### Assistant:\n"
            )
            completion = row["completion"]
            rec = {"prompt": prompt, "completion": completion}
            f.write(json.dumps(rec, ensure_ascii=False)+"\n")
    print(f"Wrote {out} ({len(split_df)} examples)")
