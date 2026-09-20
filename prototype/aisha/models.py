"""Shared locked vocabulary — mirrors app/src/main/java/com/aisha/app/model/Models.kt
Spec §2–§8. Keep both sides in sync."""
from __future__ import annotations

from dataclasses import dataclass, field, asdict
from datetime import datetime, timedelta
from enum import Enum
from typing import List, Optional


def clamp(v: float, lo: float = 0.0, hi: float = 1.0) -> float:
    return max(lo, min(hi, v))


# ---------------- Mood (spec §8) ----------------
@dataclass
class MoodState:
    happiness: float = 0.5
    calmness: float = 0.5
    energy: float = 0.5
    affection: float = 0.5
    curiosity: float = 0.5

    def shifted(self, dh: float = 0.0, dc: float = 0.0, de: float = 0.0,
                da: float = 0.0, du: float = 0.0, max_step: float = 0.12) -> "MoodState":
        """Gradual updates only — spec forbids instant jumps."""
        step = lambda v, d: clamp(v + max(-max_step, min(max_step, d)))
        return MoodState(step(self.happiness, dh), step(self.calmness, dc),
                         step(self.energy, de), step(self.affection, da),
                         step(self.curiosity, du))

    def to_dict(self): return asdict(self)

    @staticmethod
    def from_dict(d): return MoodState(**d)

    def describe(self) -> str:
        def lvl(v): return "high" if v >= 0.66 else "low" if v <= 0.33 else "mid"
        return (f"happiness={lvl(self.happiness)} calmness={lvl(self.calmness)} "
                f"energy={lvl(self.energy)} affection={lvl(self.affection)} curiosity={lvl(self.curiosity)}")


# ---------------- Love Bond (spec §7) ----------------
class BondStage(Enum):
    COMPANION = 0
    WARM_FAMILIARITY = 1
    ROMANTIC = 2
    DEEP_BOND = 3

    @property
    def label(self):
        return {0: "Companion", 1: "Warm Familiarity", 2: "Romantic", 3: "Deep Bond"}[self.value]


STAGE_BOUNDS = (25.0, 55.0, 85.0)   # total score thresholds → stage transitions


@dataclass
class BondState:
    total_score: float = 0.0           # 0..100 lifetime
    trust: float = 0.2
    consistency: float = 0.2

    @property
    def stage(self) -> BondStage:
        s = self.total_score
        if s < STAGE_BOUNDS[0]: return BondStage.COMPANION
        if s < STAGE_BOUNDS[1]: return BondStage.WARM_FAMILIARITY
        if s < STAGE_BOUNDS[2]: return BondStage.ROMANTIC
        return BondStage.DEEP_BOND

    def to_dict(self): return asdict(self)

    @staticmethod
    def from_dict(d): return BondState(**d)


# ---------------- Decisions & conversation (spec §4/§5) ----------------
class ResponseGoal(Enum):
    ANSWER = "answer"; ASK = "ask"; SUPPORT = "support"; INFORM = "inform"
    LISTEN = "listen"; PLAYFUL = "playful"; STAY_QUIET = "stay_quiet"


@dataclass
class Decision:
    should_speak: bool
    goal: ResponseGoal
    tone_hint: str
    length_hint: str
    use_memory: bool
    reason: str


class Role(Enum):
    USER = "user"; AISHA = "aisha"; SYSTEM = "system"


@dataclass
class ChatMessage:
    role: Role
    text: str
    timestamp: datetime
    language: str = "en"
    seq: int = 0


@dataclass
class ProcessedInput:
    raw: str
    language: str
    intent_hint: str            # question/greeting/task/emotional/smalltalk/command
    mentions_past: bool
    emotional_valence: float    # -1..1


class SignalKind(Enum):
    POSITIVE_TALK = "positive_talk"; QUESTION = "question"; DEEP_CONVERSATION = "deep"
    TASK_DONE = "task_done"; COLDNESS = "coldness"; CONFLICT = "conflict"
    LONG_ABSENCE = "absence"; AFFECTION = "affection"


@dataclass
class InteractionSignal:
    kind: SignalKind
    intensity: float = 1.0


@dataclass
class MemoryHit:
    day_log_id: str
    snippet: str
    relevance: float


# ---------------- Simulated clock ----------------
@dataclass
class SimClock:
    now: datetime = field(default_factory=lambda: datetime(2026, 9, 20, 8, 0))

    def advance(self, minutes: int): self.now += timedelta(minutes=minutes)

    def next_day(self, hour: int = 8):
        self.now = (self.now + timedelta(days=1)).replace(hour=hour, minute=0, second=0)

    @property
    def day_id(self) -> str: return self.now.strftime("%Y-%m-%d")
