"""LOCKED spec §4+§9 — Presence Engine. Greetings, return-after-absence,
quiet ambient policy. Non-intrusive; user settings win."""
from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime

HI_EN = {
    "morning": ("Good morning ☀️", "सुप्रभात ☀️"),
    "afternoon": ("Hey, good afternoon", "नमस्ते, नम दिन"),
    "evening": ("Good evening 🌙", "शुभ संध्या 🌙"),
    "night": ("Still up? 🌙", "अभी जाग रहे हो? 🌙"),
}


@dataclass
class Greeting:
    text: str
    tone: str


def part_of_day(dt: datetime) -> str:
    h = dt.hour
    if 5 <= h < 12: return "morning"
    if 12 <= h < 17: return "afternoon"
    if 17 <= h < 22: return "evening"
    return "night"


def on_app_open(dt: datetime, language: str = "en") -> Greeting:
    idx = 1 if language == "hi" else 0
    return Greeting(HI_EN[part_of_day(dt)][idx], "warm")


def on_return_after_absence(days: int, language: str = "en") -> Greeting:
    if days <= 1:
        return Greeting("Welcome back 🙂" if language == "en" else "वापस आए, अच्छा लगा 🙂", "soft")
    if days <= 6:
        msg = f"It's been {days} days — good to see you again." if language == "en" \
            else f"{days} दिन हो गए — आपको देखकर अच्छा लगा।"
        return Greeting(msg, "gentle")
    msg = f"{days} days… I kept everything safe. No pressure — where would you like to start?" if language == "en" \
        else f"{days} दिन… सब कुछ संभाल कर रखा है। कोई दबाव नहीं — कहाँ से शुरू करें?"
    return Greeting(msg, "warm, zero-guilt")


@dataclass
class AmbientPolicy:
    max_proactive_per_day: int = 2
    quiet_start: int = 22
    quiet_end: int = 7
    respect_battery_saver: bool = True
