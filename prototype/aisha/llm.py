"""LOCKED spec §5 — Language Model. REPLACEABLE component; wording only.
GeminiLLM (real, needs GEMINI_API_KEY) | MockLLM (offline deterministic persona)."""
from __future__ import annotations

import json
import os
import random
import urllib.request

from .prompt_builder import AishaPrompt


class LLMUnavailable(Exception):
    pass


class MockLLM:
    """Offline persona used in the prototype and in 'AI unavailable' degraded mode.
    Keeps replies consistent with mood/bond/decision — proves the pipeline works
    without any network."""

    EN = {
        "answer": ["Here's my take — {topic}. Does that match what you were thinking?",
                   "Good question. Short answer: it depends on {topic}, but I'd start simple."],
        "support": ["I'm here. That sounds heavy — want to tell me more about it?",
                    "That's a lot to carry. We can take it slow, one thing at a time."],
        "greet": ["Hey! Good to hear you 🙂", "Hi! I was hoping you'd drop by."],
        "inform": ["Noted — I've got it.", "Okay, I'm keeping track of that."],
        "listen": ["Mm-hm, tell me more.", "I'm listening — go on."],
        "playful": ["Hehe, you're in a mood today 😄", "Oh? Now that's interesting."],
        "ask": ["What made you think of that?", "How did that leave you feeling?"],
    }
    HI = {
        "answer": ["मेरी सोच यह है — {topic}। आप क्या सोच रहे हैं?",
                   "अच्छा सवाल। सीधा जवाब: {topic} पर निर्भर करता है, आसान शुरुआत करते हैं।"],
        "support": ["मैं यहीं हूँ। यह भारी लग रहा है — थोड़ा और बताएं?",
                    "बहुत कुछ सह रहे हैं आप। धीरे-धीरे, एक-एक कदम रखते हैं।"],
        "greet": ["हैलो! आपकी आवाज़ सुनकर अच्छा लगा 🙂", "नमस्ते! आज कैसे हैं आप?"],
        "inform": ["ठीक है, मैं नोट कर रही हूँ।", "समझ गई, इसे मैं संभाल रही हूँ।"],
        "listen": ["हम्म, और सुनाइए।", "मैं सुन रही हूँ।"],
        "playful": ["अरे, आज तो मूड है 😄", "अच्छा जी? यह तो दिलचस्प है।"],
        "ask": ["यह सोचा कैसे?", "इसके बाद कैसा लगा?"],
    }

    def __init__(self, seed: int = 7): self._rng = random.Random(seed)

    def generate(self, prompt: AishaPrompt, state) -> str:
        lang = state["language"]
        goal = state["decision"].goal
        pool = (self.HI if lang == "hi" else self.EN)
        key = {"ANSWER": "answer", "SUPPORT": "support", "ASK": "ask", "INFORM": "inform",
               "LISTEN": "listen", "PLAYFUL": "playful"}.get(goal.name, "listen")
        template = self._rng.choice(pool[key])
        topic = _key_words(state["input"].raw)
        stage = state["bond"].stage
        # Romantic stage adds warm signature; earlier stages never do (safety gate).
        if stage.value >= 2 and self._rng.random() < 0.35 and lang == "en":
            template += " 💜"
        return template.format(topic=topic or "that")


class GeminiLLM:
    """Real Gemini path (locked spec: replaceable component). System+constraints go as
    system_instruction; memories/context/constraints are part of it — model owns wording only."""
    URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    def __init__(self, api_key: str):
        self.key = api_key

    def generate(self, prompt: AishaPrompt, state=None) -> str:
        body = {
            "system_instruction": {"parts": [{"text": prompt.system_block + "\n\n" +
                                              prompt.memory_block + "\n\n" + prompt.constraints_block}]},
            "contents": [{"parts": [{"text": prompt.context_block + "\n\n" + prompt.user_turn}]}],
            "generationConfig": {"temperature": 0.8, "maxOutputTokens": 220},
        }
        req = urllib.request.Request(
            f"{self.URL}?key={self.key}",
            data=json.dumps(body).encode("utf-8"),
            headers={"Content-Type": "application/json"},
        )
        try:
            with urllib.request.urlopen(req, timeout=20) as resp:
                data = json.loads(resp.read().decode("utf-8"))
            return data["candidates"][0]["content"]["parts"][0]["text"].strip()
        except Exception as e:  # network, quota, safety-block → degraded path (spec §21)
            raise LLMUnavailable(f"gemini: {e}")


def make_llm() -> object:
    key = os.environ.get("GEMINI_API_KEY")
    return GeminiLLM(key) if key else MockLLM()


def _key_words(text: str, n: int = 6) -> str:
    stop = set("the a an is are was were i you my your me we it to of in on for and or do does did "
               "what why how when who where this that kya hai kaise mujhe mera meri tum tumhari".split())
    words = [w.strip(".,!?—\"'") for w in text.split()]
    keep = [w for w in words if w.lower() not in stop and w][:n]
    return " ".join(keep)
