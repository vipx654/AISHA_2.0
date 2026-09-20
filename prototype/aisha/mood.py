"""LOCKED spec §4+§8 — Mood Engine. 5 structured values, gradual updates,
feeds Decision Engine + Avatar. A software model — never claims human emotion."""
from __future__ import annotations

from dataclasses import dataclass, field
from .models import MoodState, SignalKind, InteractionSignal, clamp

BASELINE = MoodState()
DECAY_PER_DAY = 0.25          # drift back toward baseline
MEANINGFUL_DELTA = 0.06

DELTA_MAP = {
    SignalKind.POSITIVE_TALK:  dict(dh=+0.045, dc=+0.02, da=+0.03, du=+0.01),
    SignalKind.QUESTION:       dict(du=+0.06, de=+0.02),
    SignalKind.DEEP_CONVERSATION: dict(dh=-0.02, dc=-0.03, da=+0.05, du=+0.04),
    SignalKind.TASK_DONE:      dict(dh=+0.04, de=+0.02),
    SignalKind.COLDNESS:       dict(dh=-0.04, da=-0.03, de=-0.02),
    SignalKind.CONFLICT:       dict(dh=-0.08, dc=-0.06, da=-0.05),
    SignalKind.LONG_ABSENCE:   dict(de=+0.05, du=+0.04, dh=-0.02),
    SignalKind.AFFECTION:      dict(dh=+0.06, da=+0.07),
}


@dataclass
class MoodChange:
    at: object
    cause: str
    before: MoodState
    after: MoodState


@dataclass
class MoodEngine:
    state: MoodState = field(default_factory=MoodState)
    timeline: list = field(default_factory=list)
    _last_change: MoodChange | None = None

    def current(self) -> MoodState: return self.state

    def apply_interaction(self, signal: InteractionSignal):
        deltas = DELTA_MAP.get(signal.kind, {})
        if not deltas: return
        before = self.state
        self.state = self.state.shifted(**{k: v * signal.intensity for k, v in deltas.items()})
        self._record(before, signal.kind.value, self.state)

    def tick(self, fraction_of_day: float):
        """Gradual decay toward baseline. fraction_of_day: 0..1."""
        before = self.state
        s = self.state
        t = DECAY_PER_DAY * fraction_of_day
        self.state = MoodState(
            clamp(s.happiness + (BASELINE.happiness - s.happiness) * t),
            clamp(s.calmness + (BASELINE.calmness - s.calmness) * t),
            clamp(s.energy + (BASELINE.energy - s.energy) * t),
            clamp(s.affection + (BASELINE.affection - s.affection) * t),
            clamp(s.curiosity + (BASELINE.curiosity - s.curiosity) * t),
        )
        self._record(before, "decay", self.state)

    def _record(self, before: MoodState, cause: str, after: MoodState):
        moved = max(abs(a - b) for a, b in zip(
            (before.happiness, before.calmness, before.energy, before.affection, before.curiosity),
            (after.happiness, after.calmness, after.energy, after.affection, after.curiosity)))
        if moved >= MEANINGFUL_DELTA:
            change = MoodChange(None, cause, before, after)
            self.timeline.append(change)
            self._last_change = change

    def last_meaningful_change(self): return self._last_change
