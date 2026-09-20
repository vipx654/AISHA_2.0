"""LOCKED spec §6 — Memory: Working Memory, Day Log Manager, Recall Engine.
Lifecycle: create at day start → collect → finalize (summary → integrity →
compression → encryption → local store → sync queue) → next day's log.
Recall retrieves ONLY relevant records; NEVER invents missing memories."""
from __future__ import annotations

from collections import deque
from dataclasses import dataclass, field
from datetime import datetime
from typing import List, Optional

from . import storage
from .models import BondState, ChatMessage, MemoryHit, MoodState


class WorkingMemory:
    """Active-session scratch. Truth lives in Day Logs, never here."""
    def __init__(self, maxlen: int = 20):
        self._buf = deque(maxlen=maxlen)
        self.seq = 0

    def push(self, m: ChatMessage):
        self.seq += 1
        m.seq = self.seq
        self._buf.append(m)

    def recent(self, n: int = 8) -> List[ChatMessage]:
        return list(self._buf)[-n:]

    def clear(self): self._buf.clear()


@dataclass
class DayLog:
    day_id: str
    conversations: List[dict] = field(default_factory=list)
    events: List[dict] = field(default_factory=list)
    mood_timeline: List[dict] = field(default_factory=list)
    bond_timeline: List[dict] = field(default_factory=list)
    tasks: List[dict] = field(default_factory=list)
    summary: Optional[str] = None
    status: str = "OPEN"          # OPEN → FINALIZED → SYNC_QUEUED → SYNCED


class DayLogManager:
    def __init__(self, data_dir: str | None = None):
        self.data_dir = data_dir
        self.open_log: Optional[DayLog] = None

    def ensure_today(self, day_id: str) -> DayLog:
        if self.open_log is None or self.open_log.day_id != day_id:
            self.open_log = DayLog(day_id)
        return self.open_log

    def record_conversation(self, msg: ChatMessage):
        self.open_log.conversations.append(
            {"role": msg.role.value, "text": msg.text, "at": msg.timestamp.isoformat(timespec="seconds"),
             "lang": msg.language})

    def record_mood_point(self, at: datetime, mood: MoodState):
        self.open_log.mood_timeline.append({"at": at.isoformat(timespec="seconds"), **mood.to_dict()})

    def record_bond_point(self, at: datetime, bond: BondState):
        self.open_log.bond_timeline.append(
            {"at": at.isoformat(timespec="seconds"), "score": round(bond.total_score, 2),
             "stage": bond.stage.label, "trust": round(bond.trust, 2)})

    def record_event(self, at: datetime, etype: str, description: str):
        self.open_log.events.append({"at": at.isoformat(timespec="seconds"), "type": etype,
                                     "desc": description})

    def record_task(self, at: datetime, title: str, done: bool = False):
        self.open_log.tasks.append({"at": at.isoformat(timespec="seconds"), "title": title, "done": done})

    def finalize_day(self) -> dict:
        """Midnight path: summary → integrity → compression → local store → sync queue."""
        log = self.open_log
        if log is None or log.status != "OPEN":
            return {}
        happy = sum(m["happiness"] for m in log.mood_timeline) / max(1, len(log.mood_timeline))
        stages = [b["stage"] for b in log.bond_timeline]
        log.summary = (f"{len(log.conversations)} messages, {len(log.events)} events; "
                       f"avg happiness {happy:.2f}; bond {stages[0] if stages else 'n/a'} → "
                       f"{stages[-1] if stages else 'n/a'}; tasks {sum(1 for t in log.tasks if t['done'])}"
                       f"/{len(log.tasks)} done.")
        payload = {"day_id": log.day_id, "summary": log.summary, "status": "FINALIZED",
                   "conversations": log.conversations, "events": log.events,
                   "mood_timeline": log.mood_timeline, "bond_timeline": log.bond_timeline,
                   "tasks": log.tasks,
                   "note": "prototype-grade protection; production uses AES + Keystore (§14)"}
        raw_n, stored_n, sha = storage.pack_and_save_daylog(payload, self.data_dir)
        storage.enqueue_sync(log.day_id, self.data_dir)
        log.status = "FINALIZED"
        self._last_stats = {"day_id": log.day_id, "raw": raw_n, "stored": stored_n, "sha": sha}
        return dict(self._last_stats)

    last_stats = {}


class RecallEngine:
    """Search finalized day logs. Returns only what exists — never fabricates."""
    def __init__(self, data_dir: str | None = None):
        self.data_dir = data_dir

    def recall(self, query: str, limit: int = 5) -> List[MemoryHit]:
        terms = [t.lower() for t in query.split() if len(t) > 2]
        hits: List[MemoryHit] = []
        for day_id in storage.list_daylog_ids(self.data_dir):
            try:
                data = storage.load_daylog(day_id, self.data_dir)
            except ValueError as e:
                hits.append(MemoryHit(day_id, f"[INTEGRITY FAILURE — excluded: {e}]", 0.0))
                continue
            for c in data.get("conversations", []):
                score = sum(1 for t in terms if t in c["text"].lower())
                if score:
                    hits.append(MemoryHit(day_id, f"{c['role']}: {c['text'][:110]}", float(score)))
            for e in data.get("events", []):
                score = sum(1 for t in terms if t in (e["desc"] + e["type"]).lower())
                if score:
                    hits.append(MemoryHit(day_id, f"[{e['type']}] {e['desc'][:110]}", float(score) + 0.5))
        hits.sort(key=lambda h: h.relevance, reverse=True)
        return hits[:limit]
