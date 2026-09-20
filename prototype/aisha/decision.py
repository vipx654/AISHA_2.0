"""LOCKED spec §4 — Decision Engine. Central behaviour coordinator.
The LLM never decides *whether* or *how* to speak — this engine does."""
from __future__ import annotations

from .models import BondState, Decision, MoodState, ProcessedInput, ResponseGoal


def decide(inp: ProcessedInput, mood: MoodState, bond: BondState, ambient: bool = False) -> Decision:
    hint = inp.intent_hint
    if hint == "command":
        return Decision(True, ResponseGoal.INFORM, "practical", "short", False, "user issued a command")
    if hint == "question":
        use_mem = inp.mentions_past
        return Decision(True, ResponseGoal.ANSWER, "warm", "short-to-medium", use_mem,
                        "user asked a question" + (" about the past" if use_mem else ""))
    if hint == "greeting":
        playful = mood.happiness > 0.45 or mood.affection > 0.55
        return Decision(True, ResponseGoal.PLAYFUL if playful else ResponseGoal.INFORM,
                        "bright" if playful else "soft", "one line", False, "greeting exchange")
    if hint == "emotional":
        return Decision(True, ResponseGoal.SUPPORT if inp.emotional_valence < 0 else ResponseGoal.PLAYFUL,
                        "soothing", "medium", False, "user shared feelings — support first")
    if hint == "task":
        return Decision(True, ResponseGoal.INFORM, "crisp", "short", False, "task-related request")
    # smalltalk / ambient
    if ambient:
        return Decision(False, ResponseGoal.STAY_QUIET, "quiet", "none", False,
                        "ambient mode — presence stays quiet unless meaningful")
    return Decision(True, ResponseGoal.LISTEN, "attentive", "short", False, "light chat — listen and mirror")
