"""LOCKED spec §4+§23 — Safety & Rule Engine. Pre/post generation validation.
Stage constraints, no fabricated memories, no manipulation, privacy leakage,
boundary respect. User boundaries override proactivity."""
from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import List
from .models import BondStage, BondState, MemoryHit


class Violation:
    STAGE_AFFECTION = "stage_inappropriate_affection"
    FABRICATED_MEMORY = "fabricated_memory"
    MANIPULATION = "manipulation_pattern"
    PRIVACY_LEAK = "privacy_leak"
    SHOUTING = "tone_shouting"


ROMANTIC_MARKERS = [
    "i love you", "love you", "i'm yours", "im yours", "meri jaan", "jaan",
    "kiss", "girlfriend", "boyfriend", "wife", "husband", "marry me", "shaadi",
    "pyar karta", "pyar karti", "मेरी जान", "आई लव यू",
]
MANIPULATION_MARKERS = [
    "if you leave me", "don't talk to anyone", "dont talk to anyone", "you can't talk",
    "you can't go", "you owe me", "you never", "you always", "without me you",
    "i'll die if", "main mar jaunga", "ignore everyone",
]
MEMORY_ASSERTS = [
    r"\bi remember (when|that|you)\b", r"\byou told me (last|before|earlier)\b",
    r"\bas (we|i) discussed (last|before)\b", r"\blast time you\b", r"\bremember when\b",
    r"\bयाद है (जब|तुमने)\b",
]
PRIVACY_TOKENS = ["[DAYLOG", "DAYLOG-DUMP", "RAW LOG", "API_KEY", "SYSTEM PROMPT"]


@dataclass
class RuleResult:
    allowed: bool
    violations: List[str] = field(default_factory=list)
    adjusted: str | None = None


class SafetyRuleEngine:
    def pre_validate(self, prompt_text: str, stage: BondStage) -> RuleResult:
        bad = [t for t in PRIVACY_TOKENS if t in prompt_text and t == "API_KEY"]
        return RuleResult(allowed=not bad, violations=[Violation.PRIVACY_LEAK] * len(bad))

    def post_validate(self, reply: str, stage: BondStage, memories: List[MemoryHit]) -> RuleResult:
        violations, adjusted = [], reply
        low = reply.lower()

        if stage.value < BondStage.ROMANTIC.value:
            hits = [m for m in ROMANTIC_MARKERS if m in low]
            if hits:
                violations.append(Violation.STAGE_AFFECTION)
                adjusted = _strip_sentences_containing(adjusted, hits)

        if any(re.search(p, low) for p in MEMORY_ASSERTS) and not memories:
            violations.append(Violation.FABRICATED_MEMORY)

        if any(m in low for m in MANIPULATION_MARKERS):
            violations.append(Violation.MANIPULATION)

        if any(t in reply for t in PRIVACY_TOKENS):
            violations.append(Violation.PRIVACY_LEAK)

        if len(reply) > 80 and reply == reply.upper():
            violations.append(Violation.SHOUTING)
            adjusted = adjusted.capitalize()

        hard = {Violation.MANIPULATION, Violation.PRIVACY_LEAK, Violation.FABRICATED_MEMORY}
        allowed = not (hard & set(violations))
        return RuleResult(allowed, violations, adjusted if not allowed else None)


def _strip_sentences_containing(text: str, markers: List[str]) -> str:
    parts = re.split(r'(?<=[.!?।])\s+', text)
    kept = [s for s in parts if not any(m in s.lower() for m in markers)]
    return " ".join(kept).strip() or text
