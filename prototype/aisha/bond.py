"""LOCKED spec §4+§7 — Love Bond Engine. Long-term, slow, stage-gated growth.
Trust/consistency/shared experience raise it; absence/conflict lower it.
NO manipulation, NO possessiveness, user boundaries first."""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Callable, List, Tuple
from .models import BondState, BondStage, STAGE_BOUNDS, clamp

GAIN = dict(positive=0.10, deep=0.30, affection=0.22, streak_bonus=0.30,
            conflict=-1.50, absence_per_day=-0.25, absence_floor=-6.0)


@dataclass
class BondEngine:
    state: BondState = field(default_factory=BondState)
    stage_timeline: List[Tuple[object, str, str]] = field(default_factory=list)  # (day, old, new)
    _listener: Callable | None = None
    last_stage: BondStage = BondStage.COMPANION

    def current(self) -> BondState: return self.state

    def on_positive_interaction(self, quality: float, kind: str = "positive"):
        pts = dict(positive=GAIN["positive"], deep=GAIN["deep"], affection=GAIN["affection"]).get(kind, GAIN["positive"])
        self.state.total_score = clamp(self.state.total_score + pts * quality, 0, 100)
        self.state.trust = clamp(self.state.trust + 0.01 * quality)
        self._maybe_transition()

    def on_day_complete(self, streak_days: int):
        """Consistency reward — steady contact builds bond slowly."""
        if streak_days >= 2:
            bonus = min(GAIN["streak_bonus"], 0.1 * streak_days)
            self.state.total_score = clamp(self.state.total_score + bonus, 0, 100)
        self.state.consistency = clamp(self.state.consistency + (0.02 if streak_days >= 2 else -0.01))
        self._maybe_transition()

    def on_absence(self, days: int):
        if days <= 1: return
        drop = max(GAIN["absence_floor"], GAIN["absence_per_day"] * min(days, 24))
        self.state.total_score = clamp(self.state.total_score + drop, 0, 100)
        self._maybe_transition()

    def on_conflict(self, severity: float):
        self.state.total_score = clamp(self.state.total_score + GAIN["conflict"] * severity, 0, 100)
        self.state.trust = clamp(self.state.trust - 0.05 * severity)
        self._maybe_transition()

    def on_stage_listener(self, listener):
        self._listener = listener

    def _maybe_transition(self):
        new = self.state.stage
        if new != self.last_stage:
            old, self.last_stage = self.last_stage, new
            self.stage_timeline.append((None, old.label, new.label))
            if self._listener: self._listener(old, new)
