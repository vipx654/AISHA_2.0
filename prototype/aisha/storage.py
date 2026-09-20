"""Local working storage for the prototype (spec §14/§17 direction).
PROTOTYPE-GRADE protection: zlib compression + base64 + SHA-256 integrity tag.
The Android app replaces this with AES via Android Keystore (EncryptionService).
Layout:
  data/state.json          live engine state (mood/bond/streak/last day)
  data/daylogs/*.aisha     finalized day logs (compressed+tagged)
  data/sync_queue.jsonl    simulated cloud upload queue (spec §16)
"""
from __future__ import annotations

import base64, hashlib, json, os, zlib

DATA_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "data")


def _paths(data_dir: str | None):
    base = data_dir or DATA_DIR
    os.makedirs(os.path.join(base, "daylogs"), exist_ok=True)
    return base


def save_state(state: dict, data_dir: str | None = None):
    base = _paths(data_dir)
    with open(os.path.join(base, "state.json"), "w") as f:
        json.dump(state, f, indent=2, default=str)


def load_state(data_dir: str | None = None) -> dict | None:
    base = _paths(data_dir)
    p = os.path.join(base, "state.json")
    if not os.path.exists(p): return None
    with open(p) as f: return json.load(f)


# File format: line1 = "AISHA1|<sha16-of-compressed>", remaining bytes = base64(zlib(json)).
# Integrity-first: a tampered/corrupt log refuses to load (spec §21 — never blind-restore).

def pack_and_save_daylog(payload: dict, data_dir: str | None = None) -> tuple[int, int, str]:
    """compress → integrity tag → store. Returns (raw_bytes, stored_bytes, sha16)."""
    base = _paths(data_dir)
    raw = json.dumps(payload, sort_keys=True, default=str).encode("utf-8")
    compressed = zlib.compress(raw, 9)
    sha = hashlib.sha256(compressed).hexdigest()[:16]
    blob = base64.b64encode(compressed)
    path = os.path.join(base, "daylogs", f"{payload['day_id']}.aisha")
    with open(path, "wb") as f:
        f.write(f"AISHA1|{sha}\n".encode("ascii"))
        f.write(blob)
    return len(raw), os.path.getsize(path), sha


def load_daylog(day_id: str, data_dir: str | None = None) -> dict | None:
    base = _paths(data_dir)
    path = os.path.join(base, "daylogs", f"{day_id}.aisha")
    if not os.path.exists(path):
        return None
    with open(path, "rb") as f:
        content = f.read()
    header, _, blob = content.partition(b"\n")
    sha = header.decode("ascii").split("|")[1]
    compressed = base64.b64decode(blob)
    if hashlib.sha256(compressed).hexdigest()[:16] != sha:
        raise ValueError("integrity check failed — corrupt or tampered day log")
    return json.loads(zlib.decompress(compressed).decode("utf-8"))


def list_daylog_ids(data_dir: str | None = None) -> list:
    base = _paths(data_dir)
    d = os.path.join(base, "daylogs")
    return sorted(fn.replace(".aisha", "") for fn in os.listdir(d) if fn.endswith(".aisha"))


def enqueue_sync(day_id: str, data_dir: str | None = None):
    base = _paths(data_dir)
    with open(os.path.join(base, "sync_queue.jsonl"), "a") as f:
        f.write(json.dumps({"day_id": day_id, "status": "pending"}) + "\n")


def drain_sync_queue(data_dir: str | None = None) -> int:
    """Simulated connectivity return (spec §21): mark all pending as synced."""
    base = _paths(data_dir)
    p = os.path.join(base, "sync_queue.jsonl")
    if not os.path.exists(p): return 0
    n = 0
    with open(p) as f: lines = [json.loads(l) for l in f if l.strip()]
    for rec in lines:
        if rec["status"] == "pending":
            rec["status"] = "synced"; n += 1
    with open(p, "w") as f:
        for rec in lines: f.write(json.dumps(rec) + "\n")
    return n


def pending_sync_count(data_dir: str | None = None) -> int:
    base = _paths(data_dir)
    p = os.path.join(base, "sync_queue.jsonl")
    if not os.path.exists(p): return 0
    with open(p) as f:
        return sum(1 for l in f if l.strip() and json.loads(l)["status"] == "pending")
