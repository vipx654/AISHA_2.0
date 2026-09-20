"""LOCKED spec §2+§5 — Core Engine. Runs the full locked pipeline:
User input → Input Processing → Identity/Mood/Bond → Memory → Decision
→ Safety/Rules → Prompt Builder → Language Model → Response Validator
→ Reply → Day Log. The LLM is a replaceable mouth; engines own the mind."""
from __future__ import annotations

import os
import random
from dataclasses import dataclass, field
from typing import List, Optional

from . import identity as idmod, storage
from .bond import BondEngine
from .decision import decide
from .llm import LLMUnavailable, MockLLM, make_llm
from .memory import DayLogManager, RecallEngine, WorkingMemory
from .models import (BondState, ChatMessage, Decision, MemoryHit, MoodState,
                     ProcessedInput, ResponseGoal, Role, SignalKind, InteractionSignal, SimClock)
from .mood import MoodEngine
from .prompt_builder import build as build_prompt
from .safety import SafetyRuleEngine
from .validator import ResponseValidator


# ---------------- Input Processing (spec §5) ----------------
POSITIVE_WORDS = set("good great nice awesome happy love thanks thank amazing wonderful cool "
                     "acha accha badhiya mast khush pyar wonderful best".split())
NEGATIVE_WORDS = set("sad lonely tired stress stressed depressed angry hate bad worst akela "
                     "thak thaka dukh dukhi upset anxiety".split())
PAST_MARKERS = ["remember", "recall", "last time", "yaad", "earlier", "yesterday", "before"]
QUESTION_WORDS = ["what", "why", "how", "when", "who", "where", "kya", "kaise", "kaun", "kab"]
TASK_MARKERS = ["remind", "task", "todo", "planner", "wake me", "alarm", "yaad dila"]
GREETINGS = ["hi", "hello", "hey", "namaste", "namaskar", "good morning", "good evening", "good night"]


class InputProcessor:
    def process(self, raw: str) -> ProcessedInput:
        low = raw.lower()
        lang = "hi" if any("\u0900" <= ch <= "\u097F" for ch in raw) else "en"
        valence = 0.0
        valence += 0.6 * sum(w in low for w in POSITIVE_WORDS)
        valence -= 0.6 * sum(w in low for w in NEGATIVE_WORDS)
        valence = max(-1.0, min(1.0, valence))
        mentions_past = any(m in low for m in PAST_MARKERS)
        is_q = raw.strip().endswith("?") or any(w in low for w in QUESTION_WORDS)
        if any(low.strip().startswith(g) for g in GREETINGS) and len(raw.split()) <= 6:
            intent = "greeting"
        elif any(m in low for m in TASK_MARKERS):
            intent = "task"
        elif is_q:
            intent = "question"
        elif abs(valence) > 0.3:
            intent = "emotional"
        else:
            intent = "smalltalk"
        return ProcessedInput(raw, lang, intent, mentions_past, valence)


def _signal_for(inp: ProcessedInput) -> InteractionSignal:
    low = inp.raw.lower()
    if any(w in low for w in ("love you", "miss you", "pyar", "jaan")):
        return InteractionSignal(SignalKind.AFFECTION)
    if inp.intent_hint == "emotional" and inp.emotional_valence < 0:
        return InteractionSignal(SignalKind.DEEP_CONVERSATION, 1.0)
    if inp.intent_hint == "question":
        return InteractionSignal(SignalKind.QUESTION)
    if inp.intent_hint == "task":
        return InteractionSignal(SignalKind.TASK_DONE)
    if inp.emotional_valence < -0.3:
        return InteractionSignal(SignalKind.COLDNESS, abs(inp.emotional_valence))
    return InteractionSignal(SignalKind.POSITIVE_TALK, max(0.4, inp.emotional_valence or 0.6))


# ---------------- Core Engine ----------------
@dataclass
class AishaCoreEngine:
    data_dir: Optional[str] = None
    llm: object = None
    clock: SimClock = field(default_factory=SimClock)
    debug: bool = False
    rng: random.Random = field(default_factory=lambda: random.Random(42))

    def __post_init__(self):
        self.identity = idmod.IDENTITY
        self.mood = MoodEngine()
        self.bond = BondEngine()
        self.working = WorkingMemory()
        self.daylogs = DayLogManager(self.data_dir)
        self.recall = RecallEngine(self.data_dir)
        self.safety = SafetyRuleEngine()
        self.validator = ResponseValidator()
        self.llm = self.llm or make_llm()
        self.last_decision: Optional[Decision] = None
        self.last_memories: List[MemoryHit] = []
        self.last_prompt = None
        self.last_violations: List[str] = []
        self.stage_events_today: List[str] = []
        self._interactions_today = 0
        self.bond.on_stage_listener(self._on_stage_change)
        self._load()

    # ---- persistence ----
    def _load(self):
        st = storage.load_state(self.data_dir)
        if st:
            self.mood.state = MoodState.from_dict(st["mood"])
            self.bond.state = BondState.from_dict(st["bond"])
            self.clock = SimClock(datetime_from_iso(st["clock"]))
            self.working.seq = st.get("seq", 0)

    def save(self):
        storage.save_state({"mood": self.mood.state.to_dict(), "bond": self.bond.state.to_dict(),
                            "clock": self.clock.now.isoformat(), "seq": self.working.seq}, self.data_dir)

    # ---- pipeline ----
    def handle_user_turn(self, raw: str, ambient: bool = False) -> dict:
        self.daylogs.ensure_today(self.clock.day_id)
        inp = InputProcessor().process(raw)

        # 1) emotional update (gradual)
        sig = _signal_for(inp)
        self.mood.apply_interaction(sig)
        quality = 1.0 if inp.intent_hint in ("emotional", "question") else 0.7
        kind = {SignalKind.AFFECTION: "affection",
                SignalKind.DEEP_CONVERSATION: "deep"}.get(sig.kind, "positive")
        self.bond.on_positive_interaction(quality, kind)
        self._interactions_today += 1

        # 2) decision
        decision = decide(inp, self.mood.current(), self.bond.state, ambient)
        self.last_decision = decision

        # 3) memory (selective — only when relevant)
        memories: List[MemoryHit] = []
        if inp.mentions_past:
            memories = self.recall.recall(raw, limit=3)
        self.last_memories = memories

        # 4) prompt (identity + mood + stage + memories + context + constraints)
        prompt = build_prompt(self.identity, self.mood.current(), self.bond.state, decision,
                              memories, self.working.recent(8), None, inp)
        self.last_prompt = prompt
        if not self.safety.pre_validate(prompt.full(), self.bond.state.stage).allowed:
            return self._degraded("VALIDATOR_BLOCKED")

        # 5) language model
        state = {"language": inp.language, "decision": decision,
                 "bond": self.bond.state, "mood": self.mood.current(), "input": inp}
        try:
            reply = self.llm.generate(prompt, state)
        except LLMUnavailable:
            return self._degraded("AI_SERVICE_UNAVAILABLE")

        # 6) response validation (regenerate once on hard violation, then safe fallback)
        ok, reply, violations = self.validator.validate(reply, self.bond.state, memories)
        if not ok:
            try:
                reply2 = self.llm.generate(prompt, state)
                ok, reply2, v2 = self.validator.validate(reply2, self.bond.state, memories)
                if ok:
                    reply, violations = reply2, v2
                else:
                    reply = ("I'd rather not say it that way — let me know what you need."
                             if inp.language == "en" else "मैं इसे इस तरह नहीं कहूँगी — बताइए क्या चाहिए।")
                    violations = violations + ["fallback_used"]
            except LLMUnavailable:
                return self._degraded("AI_SERVICE_UNAVAILABLE")
        self.last_violations = violations

        # 7) reply → day log
        self.clock.advance(self.rng.randint(1, 4))
        self._log_message(Role.USER, inp.raw, inp.language)
        self._log_message(Role.AISHA, reply, inp.language)
        self.daylogs.record_mood_point(self.clock.now, self.mood.current())
        self.daylogs.record_bond_point(self.clock.now, self.bond.state)
        self.save()
        return {"reply": reply, "violations": violations, "decision": decision,
                "language": inp.language, "memories": memories}

    def _degraded(self, reason: str, language: str = "en") -> dict:
        if reason == "AI_SERVICE_UNAVAILABLE":
            reply = ("I can't reach my language service right now, but I'm still here — "
                     "everything you say is saved safely.")
            if language == "hi":
                reply = "भाषा सेवा अभी उपलब्ध नहीं है, पर मैं यहीं हूँ — आपकी बातें सुरक्षित सेव हो रही हैं।"
        else:
            reply = ("Let's rephrase that — my safety layer blocked that path."
                     if language == "en" else "दूसरे शब्दों में कहें — सुरक्षा परत ने रोक लगाई।")
        return {"reply": reply, "violations": [], "degraded": reason,
                "decision": None, "memories": [], "language": language}

    # ---- day lifecycle (spec §6: midnight service behaviour) ----
    def finalize_day(self) -> dict:
        stats = self.daylogs.finalize_day()
        self._close_day_bond_bookkeeping()
        self.mood.tick(1.0)                      # overnight decay toward baseline
        if stats:
            self.daylogs.record_event(self.clock.now, "DAY_FINALIZED",
                                      f"{stats['day_id']} stored raw={stats['raw']}B stored={stats['stored']}B sha={stats['sha']}")
        return stats

    def advance_day(self, hour: int = 8):
        stats = self.finalize_day()
        self.clock.next_day(hour)
        self.daylogs.ensure_today(self.clock.day_id)
        self.stage_events_today = []
        self.save()
        return stats

    def sync_now(self) -> int:
        return storage.drain_sync_queue(self.data_dir)

    # ---- helpers ----
    def _on_stage_change(self, old, new):
        self.stage_events_today.append(f"BOND_STAGE: {old.label} → {new.label}")
        if self.daylogs.open_log:
            self.daylogs.record_event(self.clock.now, "BOND_STAGE", f"{old.label} → {new.label}")

    def _log_message(self, role: Role, text: str, lang: str):
        msg = ChatMessage(role, text, self.clock.now, lang)
        self.working.push(msg)
        self.daylogs.ensure_today(self.clock.day_id)
        self.daylogs.record_conversation(msg)

    def _close_day_bond_bookkeeping(self):
        """Spec §7: consistency rewards require real interaction; absence decays per rules."""
        from datetime import timedelta
        st = storage.load_state(self.data_dir) or {}
        yesterday = (self.clock.now - timedelta(days=1)).strftime("%Y-%m-%d")
        if self._interactions_today > 0:
            streak = st.get("streak", 0) + 1 if st.get("last_active_day") == yesterday else 1
            self.bond.on_day_complete(streak)
            st["streak"] = streak
            st["last_active_day"] = self.clock.day_id
        else:
            last_active = st.get("last_active_day")
            if last_active:
                days_gone = (self.clock.now.date() - _date(last_active)).days
                self.bond.on_absence(days_gone)
            st["streak"] = 0
        self._interactions_today = 0
        storage.save_state(st, self.data_dir)


def _date(iso: str):
    from datetime import date
    return date.fromisoformat(iso)


def datetime_from_iso(s: str):
    from datetime import datetime
    return datetime.fromisoformat(s)
