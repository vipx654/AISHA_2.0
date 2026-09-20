"""LOCKED spec §5 — Response Validator. Tone, rule compliance, context
consistency, privacy leakage. May force regeneration/adjustment."""
from __future__ import annotations

from .safety import SafetyRuleEngine, RuleResult
from .models import BondState, MemoryHit


class ResponseValidator:
    def __init__(self):
        self.safety = SafetyRuleEngine()

    def validate(self, reply: str, bond: BondState, memories: list) -> tuple[bool, str, list]:
        result: RuleResult = self.safety.post_validate(reply, bond.stage, memories)
        reply_out = result.adjusted if (not result.allowed and result.adjusted) else reply
        return result.allowed, reply_out, result.violations
