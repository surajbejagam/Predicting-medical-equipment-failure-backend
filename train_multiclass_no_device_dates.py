# train_multiclass_no_device_dates.py
import os
import json
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import OneHotEncoder, FunctionTransformer
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.impute import SimpleImputer
from sklearn.utils import shuffle
from joblib import dump

# -----------------------------
# Helpers
# -----------------------------
def load_ndjson(path):
    rows = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            s = line.strip()
            if s:
                rows.append(json.loads(s))
    return pd.DataFrame(rows)

def first_existing(paths):
    for p in paths:
        if os.path.exists(p):
            return p
    raise FileNotFoundError(paths)

def to_dt(s):
    return pd.to_datetime(s, errors="coerce")

def one_hot_flags(series):
    s = series.fillna("UNK").astype(str)
    return (s == "CLASS I").astype(int), (s == "CLASS II").astype(int), (s == "CLASS III").astype(int)

# -----------------------------
# 1) Load data (events/devices/manufacturers)
# -----------------------------
events_path = first_existing(["clean_events_with_slug.json", "clean_events.json"])
devices_path = first_existing(["clean_devices_with_slug.json", "clean_devices.json"])
manufs_path  = first_existing(["clean_manufacturers_with_slug.json", "clean_manufacturers.json"])

events = load_ndjson(events_path)
devices = load_ndjson(devices_path)
manufs  = load_ndjson(manufs_path)

# Keep labeled events and with a device_id
events = events[events["action_classification"].isin(["CLASS I","CLASS II","CLASS III"])]
events = events[events["device_id"].notna()]

# Build a reliable event_time for ordering history:
# prefer 'date', else 'date_posted', else 'date_updated'
for c in ["date","date_posted","date_updated"]:
    if c in events.columns:
        events[c] = to_dt(events[c])

def choose_event_time(row):
    for c in ["date","date_posted","date_updated"]:
        if c in events.columns:
            v = row.get(c)
            if pd.notna(v):
                return v
    return pd.NaT

events["event_time"] = events.apply(choose_event_time, axis=1)
# drop events without any time info (can't create past counts safely)
events = events[pd.notna(events["event_time"])].copy()

# -----------------------------
# 2) Join events → devices → manufacturers (no device date usage)
# -----------------------------
events = events.merge(
    devices.rename(columns={"id":"device_pk"}),
    left_on="device_id", right_on="device_pk",
    how="left", suffixes=("","_device")
)
events = events.merge(
    manufs.rename(columns={"id":"manufacturer_pk"}),
    left_on="manufacturer_id", right_on="manufacturer_pk",
    how="left", suffixes=("","_manuf")
)

# -----------------------------
# 3) Leak-safe history features
# -----------------------------
# Sort by (device_id, event_time) and create cumulative counts shifted by 1
events = events.sort_values(["device_id","event_time"])
eI, eII, eIII = one_hot_flags(events["action_classification"])
events["dev_is_I"], events["dev_is_II"], events["dev_is_III"] = eI, eII, eIII

for col in ["dev_is_I","dev_is_II","dev_is_III"]:
    events[col+"_cum_past"] = events.groupby("device_id")[col].cumsum().shift(1).fillna(0)

events["device_past_recalls_total"] = (
    events["dev_is_I_cum_past"] + events["dev_is_II_cum_past"] + events["dev_is_III_cum_past"]
)

# Manufacturer history
events = events.sort_values(["manufacturer_id","event_time"])
mI, mII, mIII = one_hot_flags(events["action_classification"])
events["mfr_is_I"], events["mfr_is_II"], events["mfr_is_III"] = mI, mII, mIII

for col in ["mfr_is_I","mfr_is_II","mfr_is_III"]:
    events[col+"_cum_past"] = events.groupby("manufacturer_id")[col].cumsum().shift(1).fillna(0)

events["mfr_past_recalls_total"] = (
    events["mfr_is_I_cum_past"] + events["mfr_is_II_cum_past"] + events["mfr_is_III_cum_past"]
)

# -----------------------------
# 4) Feature table (inputs + label)
# -----------------------------
label_col = "action_classification"

cat_cols = [
    "risk_class",
    "classification",
    "country",
    "parent_company",
]
bool_cols = [
    "implanted",
]
num_cols = [
    "quantity_in_commerce",
    "device_past_recalls_total",
    "dev_is_I_cum_past", "dev_is_II_cum_past", "dev_is_III_cum_past",
    "mfr_past_recalls_total",
    "mfr_is_I_cum_past", "mfr_is_II_cum_past", "mfr_is_III_cum_past",
]

needed = [label_col] + cat_cols + bool_cols + num_cols
df = events[needed].copy()

# Types
for c in bool_cols:
    df[c] = df[c].map(lambda x: 1 if str(x).strip().lower() in {"true","1","yes","y"} else 0)
for c in num_cols:
    df[c] = pd.to_numeric(df[c], errors="coerce")

# Drop rows without label
df = df[df[label_col].notna()]
df = shuffle(df, random_state=42).reset_index(drop=True)

X = df[cat_cols + bool_cols + num_cols]
y = df[label_col].astype(str)

# -----------------------------
# 5) Preprocess + Model
# -----------------------------
def _log1p_safe(a):
    return np.log1p(np.maximum(a, 0))

numeric_transform = Pipeline(steps=[
    ("imputer", SimpleImputer(strategy="constant", fill_value=0.0)),
    ("log1p", FunctionTransformer(_log1p_safe, feature_names_out="one-to-one")),
])

categorical_transform = Pipeline(steps=[
    ("imputer", SimpleImputer(strategy="most_frequent")),
    ("onehot", OneHotEncoder(handle_unknown="ignore", min_frequency=0.01)),
])

preprocessor = ColumnTransformer(
    transformers=[
        ("cat", categorical_transform, cat_cols),
        ("num", numeric_transform,    bool_cols + num_cols),
    ],
    remainder="drop"
)

clf = Pipeline(steps=[
    ("prep", preprocessor),
    ("rf", RandomForestClassifier(
        n_estimators=400,
        max_depth=None,
        min_samples_split=4,
        min_samples_leaf=2,
        class_weight="balanced_subsample",
        random_state=42,
        n_jobs=-1
    ))
])

# -----------------------------
# 6) Train / Test / Evaluate
# -----------------------------
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, stratify=y, random_state=42
)

clf.fit(X_train, y_train)
y_pred = clf.predict(X_test)

print("\n=== Classification Report (CLASS I / CLASS II / CLASS III) ===")
print(classification_report(y_test, y_pred, digits=3))

print("\n=== Confusion Matrix ===")
print(confusion_matrix(y_test, y_pred, labels=["CLASS I","CLASS II","CLASS III"]))
from sklearn.metrics import accuracy_score

acc = accuracy_score(y_test, y_pred)
print(f"\n=== Overall Accuracy === {acc:.3f}")


# -----------------------------
# 7) Save model
# -----------------------------
dump(clf, "recall_severity_rf_no_device_dates.joblib")
print("\nSaved model to recall_severity_rf_no_device_dates.joblib")

# Optional: quick sanity preds
example = X_test.head(5)
pred = clf.predict(example)
print("\nExample predictions:")
for i, p in enumerate(pred, 1):
    print(f"{i}. {p}")
