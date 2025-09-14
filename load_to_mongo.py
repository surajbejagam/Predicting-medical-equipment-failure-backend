import os
import json
import zipfile
import pandas as pd

# ==========================
# CONFIG (update paths here)
# ==========================
EVENTS_ZIP = "events-1681209680.csv.zip"
DEVICES_ZIP = "devices-1681209661.csv.zip"
MANUF_ZIP  = "manufacturers-1681209657.csv.zip"

OUT_DIR = "cleaned_output"
os.makedirs(OUT_DIR, exist_ok=True)

# --------------------------
# Helper to load CSV from ZIP
# --------------------------
def load_csv_from_zip(zip_path):
    with zipfile.ZipFile(zip_path, "r") as z:
        members = [n for n in z.namelist() if n.lower().endswith(".csv")]
        if not members:
            raise ValueError(f"No CSV inside {zip_path}")
        with z.open(members[0]) as f:
            return pd.read_csv(f, low_memory=False)

# --------------------------
# Cleaning: Events
# --------------------------
def clean_events(df):
    keep = [c for c in [
        "id", "device_id", "action", "action_classification",
        "reason", "determined_cause", "status",
        "country", "date", "date_posted", "date_updated"
    ] if c in df.columns]
    out = df[keep].copy()
    if "reason" in out: out["reason"] = out["reason"].fillna("Unknown")
    if "determined_cause" in out: out["determined_cause"] = out["determined_cause"].fillna("Unknown")
    if "action_classification" in out:
        out["action_classification"] = (
            out["action_classification"].astype(str).str.strip().str.upper()
            .replace({"CLASS 1": "CLASS I","CLASS 2": "CLASS II","CLASS 3": "CLASS III",
                      "I":"CLASS I","II":"CLASS II","III":"CLASS III"})
        )
    # dates -> ISO string
    for col in ["date", "date_posted", "date_updated"]:
        if col in out:
            out[col] = pd.to_datetime(out[col], errors="coerce").dt.strftime("%Y-%m-%d")
            out[col] = out[col].where(out[col].notna(), None)
    return out.drop_duplicates(subset=["id"])

# --------------------------
# Cleaning: Devices
# --------------------------
def clean_devices(df):
    keep = [c for c in [
        "id", "name", "description", "classification", "risk_class",
        "implanted", "code", "quantity_in_commerce", "manufacturer_id",
        "country", "created_at", "updated_at"
    ] if c in df.columns]
    out = df[keep].copy()
    if "risk_class" in out: out["risk_class"] = out["risk_class"].fillna("Unknown")
    if "implanted" in out: out["implanted"] = out["implanted"].fillna("No")
    if "quantity_in_commerce" in out: out["quantity_in_commerce"] = out["quantity_in_commerce"].fillna(0)
    if "country" in out: out["country"] = out["country"].fillna("Unknown")
    for col in ["created_at", "updated_at"]:
        if col in out:
            out[col] = pd.to_datetime(out[col], errors="coerce").dt.strftime("%Y-%m-%d")
            out[col] = out[col].where(out[col].notna(), None)
    return out.drop_duplicates(subset=["id"])

# --------------------------
# Cleaning: Manufacturers
# --------------------------
def clean_manuf(df):
    keep = [c for c in ["id", "name", "parent_company"] if c in df.columns]
    out = df[keep].copy()
    if "parent_company" in out: out["parent_company"] = out["parent_company"].fillna("Independent")
    if "name" in out: out["name"] = out["name"].astype(str).str.strip().str.lower()
    return out.drop_duplicates(subset=["id"])

# --------------------------
# Save CSV + NDJSON
# --------------------------
def save_outputs(df, name):
    csv_path = os.path.join(OUT_DIR, f"clean_{name}.csv")
    json_path = os.path.join(OUT_DIR, f"clean_{name}.json")
    df.to_csv(csv_path, index=False)
    with open(json_path, "w", encoding="utf-8") as f:
        for rec in df.to_dict(orient="records"):
            clean = {k: (None if pd.isna(v) else v) for k, v in rec.items()}
            f.write(json.dumps(clean, ensure_ascii=False) + "\n")
    return csv_path, json_path

# --------------------------
# Main
# --------------------------
def main():
    events_raw = load_csv_from_zip(EVENTS_ZIP)
    devices_raw = load_csv_from_zip(DEVICES_ZIP)
    manuf_raw  = load_csv_from_zip(MANUF_ZIP)

    events = clean_events(events_raw)
    devices = clean_devices(devices_raw)
    manuf   = clean_manuf(manuf_raw)

    e_csv, e_json = save_outputs(events, "events")
    d_csv, d_json = save_outputs(devices, "devices")
    m_csv, m_json = save_outputs(manuf, "manufacturers")

    print("Cleaned files saved:")
    for p in [e_csv, d_csv, m_csv, e_json, d_json, m_json]:
        print(" -", p)

if __name__ == "__main__":
    main()
