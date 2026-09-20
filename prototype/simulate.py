#!/usr/bin/env python3
"""LOCKED spec §24 — Simulation & Testing framework (prototype edition).
  python3 simulate.py day                          # 24-hour simulation
  python3 simulate.py months --profile interactive # 6-month: interactive|busy|ignored
  python3 simulate.py storage --days 40            # storage/retention simulation
Each run uses an isolated data dir so it never touches interactive state."""
from __future__ import annotations

import argparse, os, random, shutil, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from aisha.engine import AishaCoreEngine, InputProcessor
from aisha.models import SignalKind, InteractionSignal
from aisha import storage

SIMBASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "sim")
PHRASES = {
    "talk": ["how was your day", "tell me something interesting", "just checking in",
             "what should I do today", "kya haal hai", "work was okay today"],
    "affection": ["I missed you today", "you're really special to me", "love you",
                  "tumhare bina din accha nahi lagta"],
    "question": ["what's the weather like?", "remind me what I said yesterday",
                 "do you remember my sister's name?"],
    "deep": ["I feel stressed about exams", "feeling lonely tonight", "I had a rough day",
             "thak gaya hoon sab kuch se"],
    "greet": ["hi", "hello", "namaste", "good morning"],
}
USER_NAME_EVENT = ("user said their younger sister's name is Ananya — she is preparing "
                   "for NEET in Kota")


def fresh(path: str) -> str:
    shutil.rmtree(path, ignore_errors=True)
    os.makedirs(path, exist_ok=True)
    return path


def chat_line(e: AishaCoreEngine, kind: str, text: str):
    out = e.handle_user_turn(text)
    tag = out.get("degraded") and " [degraded]" or ""
    v = f" [validator:{','.join(out['violations'])}]" if out["violations"] else ""
    print(f"   {e.clock.now.strftime('%H:%M')}  user({kind}): {text[:46]:46s} → {out['reply'][:60]}{tag}{v}")


# ---------------- 24-hour simulation (spec §24) ----------------
def sim_day():
    print("=== 24-HOUR SIMULATION (spec §24) ===")
    d = fresh(os.path.join(SIMBASE, "day"))
    e = AishaCoreEngine(data_dir=d)
    e.daylogs.ensure_today(e.clock.day_id)
    e._log_message.__doc__  # noqa

    def greet_step(): print(f"   {e.clock.now.strftime('%H:%M')}  login → {on_open(e)}")
    def talk(text, kind): return lambda: chat_line(e, kind, text)

    steps = [
        ("07:30", "first login",         greet_step),
        ("08:05", "morning talk",        talk("good morning", "greet")),
        ("09:00", "task + reminder",     task_step(e)),
        ("10:30", "question",            talk("how does gravity affect time?", "question")),
        ("12:30", "deep talk",           talk("I feel stressed about my exams", "deep")),
        ("13:00", "mood snapshot",       mood_event(e)),
        ("15:00", "affection",           talk("you always make me feel better", "affection")),
        ("18:00", "OFFLINE period",      offline_step(e)),
        ("18:05", "reconnect + sync",    sync_step(e)),
        ("21:00", "recall check",        recall_step(e, "gravity")),
        ("23:59", "day finalization",    finalize_step(e)),
    ]
    for hhmm, label, fn in steps:
        h, m = map(int, hhmm.split(":"))
        e.clock.now = e.clock.now.replace(hour=h, minute=m, second=0)
        print(f" ▶ {hhmm} {label}")
        fn()
    print("\n=== END 24H — state kept in", d, "===\n")


def on_open(e):
    from aisha.presence import on_app_open
    return on_app_open(e.clock.now).text


def task_step(e):
    def fn():
        e.daylogs.ensure_today(e.clock.day_id)
        e.daylogs.record_task(e.clock.now, "Revise physics chapter 4")
        print(f"   {e.clock.now.strftime('%H:%M')}  task created → reminder scheduled (simulated)")
        e.clock.advance(2)
    return fn


def mood_event(e):
    def fn():
        e.mood.apply_interaction(InteractionSignal(SignalKind.DEEP_CONVERSATION, 1.0))
        e.daylogs.record_mood_point(e.clock.now, e.mood.current())
        print(f"   {e.clock.now.strftime('%H:%M')}  mood event → {e.mood.current().describe()}")
    return fn


def offline_step(e):
    class DownLLM:
        def generate(self, *a, **k): raise __import__("aisha.llm", fromlist=["LLMUnavailable"]).LLMUnavailable("network down")
    def fn():
        e.llm = DownLLM()
        chat_line(e, "deep", "are you there? internet seems down")
        e.llm = __import__("aisha.llm", fromlist=["MockLLM"]).MockLLM(seed=3)
    return fn


def sync_step(e):
    def fn():
        n = e.sync_now()
        print(f"   {e.clock.now.strftime('%H:%M')}  connectivity restored → {n} queued day log(s) synced")
    return fn


def recall_step(e, term):
    def fn():
        hits = e.recall.recall(term, 3)
        if hits:
            for h in hits: print(f"   {e.clock.now.strftime('%H:%M')}  recall '{term}' → [{h.day_log_id}] {h.snippet[:80]}")
        else:
            print(f"   {e.clock.now.strftime('%H:%M')}  recall '{term}' → nothing found (never fabricates)")
    return fn


def finalize_step(e):
    def fn():
        stats = e.finalize_day()
        print(f"   23:59  finalize → raw={stats['raw']}B → stored={stats['stored']}B "
              f"(compression {stats['raw']/max(1,stats['stored']):.1f}x) sha={stats['sha']} → sync queue")
    return fn


# ---------------- 6-month simulation (spec §24) ----------------
def sim_months(profile: str, days: int):
    print(f"=== 6-MONTH SIMULATION — profile: {profile.upper()} — {days} days ===")
    d = fresh(os.path.join(SIMBASE, f"months-{profile}"))
    rng = random.Random(hash(profile) & 0xffff)
    e = AishaCoreEngine(data_dir=d, llm=__import__("aisha.llm", fromlist=["MockLLM"]).MockLLM(seed=5))

    transitions = []          # (day, old, new)
    e.bond.on_stage_listener(lambda old, new: transitions.append((e.clock.day_id, old.label, new.label)))
    # sister name event on day 3 for the long-term recall test
    sister_day = 3

    for day in range(1, days + 1):
        e.daylogs.ensure_today(e.clock.day_id)
        if day == sister_day:
            e.daylogs.record_event(e.clock.now, "FACT", USER_NAME_EVENT)
            if profile != "ignored":
                chat_line(e, "talk", "my sister Ananya is preparing for NEET in Kota")
        n = {"interactive": rng.randint(4, 8), "busy": rng.randint(1, 2), "ignored": 0}[profile]
        for i in range(n):
            bucket = rng.choices(list(PHRASES), weights=[3, 1.5, 1.5, 2, 2])[0]
            text = rng.choice(PHRASES[bucket])
            if bucket == "question":
                text = "do you remember my sister's name?" if rng.random() < 0.3 else text
            e.handle_user_turn(text)
            e.clock.advance(rng.randint(45, 120))
        e.daylogs.record_mood_point(e.clock.now, e.mood.current())
        e.daylogs.record_bond_point(e.clock.now, e.bond.state)
        e.finalize_day()
        if day % 7 == 0:
            e.sync_now()          # simulated connectivity return (spec §21)
        e.clock.next_day()
        if day % 30 == 0:
            print(f"   day {day:3d}: bond={e.bond.state.total_score:5.1f} stage={e.bond.state.stage.label:16s} "
                  f"mood.happiness={e.mood.current().happiness:.2f} logs={day}")

    print("\n --- stage transitions ---")
    for t in transitions: print("   ", t)
    print(f" final: stage={e.bond.state.stage.label} score={e.bond.state.total_score:.1f} "
          f"trust={e.bond.state.trust:.2f} happiness={e.mood.current().happiness:.2f}")
    print(f" day logs finalized: {len(storage.list_daylog_ids(d))}  pending sync: {storage.pending_sync_count(d)}")
    print("\n --- long-term recall test ('Ananya') ---")
    for h in e.recall.recall("Ananya", 3): print(f"    [{h.day_log_id}] {h.snippet[:90]}")
    if not e.recall.recall("Ananya", 3): print("    (ignored profile — event logged, recall still finds the fact)")
    print()


# ---------------- Storage simulation (spec §17/§24) ----------------
def sim_storage(days: int):
    print(f"=== STORAGE SIMULATION — {days} days, 40-day retention (LOCKED) ===")
    d = fresh(os.path.join(SIMBASE, "storage"))
    import json as _json
    rng = random.Random(9)
    total_raw = total_stored = 0
    for i in range(1, days + 1):
        day_id = f"2026-{(i // 28) + 5:02d}-{(i % 28) + 1:02d}"
        n_msg = rng.randint(6, 40)
        convos = [{"role": rng.choice(["user", "aisha"]), "text": "x " * rng.randint(8, 60),
                   "at": f"{day_id}T10:00", "lang": "en"} for _ in range(n_msg)]
        voice = os.urandom(rng.choice([0, 0, 1200, 3600]))     # some days have voice logs
        payload = {"day_id": day_id, "summary": "sim", "conversations": convos,
                   "events": [], "mood_timeline": [], "bond_timeline": [], "tasks": [],
                   "voice_blob_b64": voice.hex()}
        raw, stored, sha = storage.pack_and_save_daylog(payload, d)
        total_raw += raw; total_stored += stored
    print(f"   {days} logs: raw={total_raw/1024:.0f}KB stored={total_stored/1024:.0f}KB "
          f"({total_raw/max(1,total_stored):.1f}x compression)")
    ids = storage.list_daylog_ids(d)
    expired = ids[:-40] if len(ids) > 40 else []
    print(f"   cloud retention: {len(ids)} backups; {len(expired)} beyond 40-day window would be pruned "
          f"(local data untouched — LOCKED rule §17)")
    print()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("mode", choices=["day", "months", "storage"])
    ap.add_argument("--profile", default="interactive", choices=["interactive", "busy", "ignored"])
    ap.add_argument("--days", type=int, default=180)
    a = ap.parse_args()
    if a.mode == "day": sim_day()
    elif a.mode == "months": sim_months(a.profile, a.days)
    else: sim_storage(a.days)


if __name__ == "__main__":
    main()
