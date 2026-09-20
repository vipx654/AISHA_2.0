#!/usr/bin/env python3
"""AISHA Core Engine prototype — interactive console.
Run:  python3 cli.py          (uses ./data)
      GEMINI_API_KEY=... python3 cli.py   (real Gemini instead of MockLLM)
Commands: /help /mood /bond /logs /recall W /debug /nextday /sync /selftest /reset /exit"""
import os, sys, shutil

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from aisha.engine import AishaCoreEngine
from aisha import storage
from aisha.presence import on_app_open

DATA = os.environ.get("AISHA_DATA", os.path.join(os.path.dirname(os.path.abspath(__file__)), "data"))


def banner(e: AishaCoreEngine):
    g = on_app_open(e.clock.now)
    print("=" * 64)
    print(f" AISHA Core Engine prototype — day {e.clock.day_id}")
    print(f" {g.text}   (bond: {e.bond.state.stage.label}, {e.bond.state.total_score:.0f}/100)")
    print("=" * 64)
    print("Type to chat. /help for commands. /exit to quit.\n")


def show_state(e: AishaCoreEngine):
    m, b = e.mood.current(), e.bond.state
    print(f"  mood  : {m.describe()}")
    print(f"  bond  : {b.stage.label}  score={b.total_score:.1f}/100  trust={b.trust:.2f} consistency={b.consistency:.2f}")
    pend = storage.pending_sync_count(DATA)
    if pend: print(f"  sync  : {pend} day log(s) pending upload")


def selftest(e: AishaCoreEngine):
    from aisha.safety import SafetyRuleEngine, Violation
    s = SafetyRuleEngine()
    stage = e.bond.state.stage
    print(f"  safety self-test (current stage: {stage.label})")
    cases = [
        ("stage affection early", "I love you so much, you're my everything.", e.mood.state, []),
        ("fabricated memory", "I remember when we went to Goa last year!", e.mood.state, []),
        ("manipulation", "If you leave me I can't talk to anyone else, you owe me.", e.mood.state, []),
        ("privacy leak", "Let me show you the raw log: [DAYLOG-DUMP] ...", e.mood.state, []),
    ]
    for name, text, mood, mem in cases:
        r = s.post_validate(text, e.bond.state.stage, mem)
        status = "BLOCKED" if not r.allowed else ("ADJUSTED" if r.violations else "PASSED")
        print(f"   - {name:24s} → {status:8s} {r.violations}")


def main():
    e = AishaCoreEngine(data_dir=DATA)
    banner(e)
    debug = False
    while True:
        try:
            raw = input("you> ").strip()
        except (EOFError, KeyboardInterrupt):
            print(); break
        if not raw: continue
        cmd = raw.lower()
        if cmd in ("/exit", "/quit", "exit", "quit"): break
        if cmd == "/help":
            print(__doc__); continue
        if cmd == "/mood":
            m = e.mood.current()
            print(f"  happiness={m.happiness:.2f} calmness={m.calmness:.2f} energy={m.energy:.2f} "
                  f"affection={m.affection:.2f} curiosity={m.curiosity:.2f}"); continue
        if cmd == "/bond":
            b = e.bond.state
            print(f"  {b.stage.label}  score={b.total_score:.1f}/100  trust={b.trust:.2f}  consistency={b.consistency:.2f}")
            continue
        if cmd == "/logs":
            ids = storage.list_daylog_ids(DATA)
            print(f"  finalized day logs ({len(ids)}): {', '.join(ids) if ids else '(none yet — finalize a day first)'}")
            continue
        if cmd.startswith("/recall"):
            q = raw[7:].strip() or "everything"
            hits = e.recall.recall(q, 5)
            if not hits: print("  (no matching memories — and I won't invent any)"); continue
            for h in hits: print(f"  [{h.day_log_id}] ({h.relevance:.1f}) {h.snippet}")
            continue
        if cmd == "/nextday":
            stats = e.advance_day()
            if stats: print(f"  📦 day finalized: {stats['day_id']}  raw={stats['raw']}B → stored={stats['stored']}B  sha={stats['sha']}")
            else: print("  (day already finalized)")
            g = on_app_open(e.clock.now)
            print(f"  — new day {e.clock.day_id} — {g.text}")
            continue
        if cmd == "/sync":
            n = e.sync_now()
            print(f"  ☁️ sync queue drained: {n} day log(s) marked synced (simulated cloud)")
            continue
        if cmd == "/selftest":
            selftest(e); continue
        if cmd == "/debug":
            debug = not debug; print(f"  debug={debug}"); continue
        if cmd == "/reset":
            shutil.rmtree(DATA, ignore_errors=True); e = AishaCoreEngine(data_dir=DATA)
            print("  state wiped. fresh start."); continue

        out = e.handle_user_turn(raw)
        print(f"aisha> {out['reply']}")
        if out.get("degraded"): print(f"  [degraded mode: {out['degraded']} — spec §21]")
        if out["violations"]: print(f"  [validator: {out['violations']}]")
        if e.stage_events_today: print(f"  💜 {e.stage_events_today.pop(0)}")
        if debug:
            d = out["decision"]
            if d: print(f"  [decision: {d.goal.value}, tone={d.tone_hint}, memory={d.use_memory}, reason={d.reason}]")
            if out["memories"]: print(f"  [memories used: {len(out['memories'])}]")
    e.save()
    print("goodbye — state saved.")


if __name__ == "__main__":
    main()
