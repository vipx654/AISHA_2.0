# AISHA 2.0

Mobile-first AI companion (Android · Kotlin + Compose) built from the
**AISHA Locked Module & Feature Specification v1.0** — see
`uploads/AISHA_Locked_Module_Feature_Specification_v1.0.pdf` (master document)
and `docs/SPEC_SUMMARY_v1.0.md` (working digest).

## Repository layout
```
app/            Android app (Kotlin + Compose) — locked interfaces for all 10 systems
prototype/      Python Core Engine prototype — runnable pipeline + spec §24 simulations
docs/           Spec summary + architecture map
```

## Status (v0.1.0 — skeleton + core prototype)
- ✅ Android project skeleton, locked module interfaces (spec §2–§20)
- ✅ Core Engine prototype in Python: mood (5-axis), bond (4 stages), decision,
  safety validator, prompt builder, day logs (finalize → compress → integrity → queue),
  selective recall, offline degraded mode, sync queue
- ✅ Simulations: 24-hour day, 6-month profiles (interactive/busy/ignored), storage/retention
- ⏳ Android module implementations (phased — see docs/ARCHITECTURE_MAP.md)
- ⏳ Gemini API integration in Android layer (prototype already supports it via `GEMINI_API_KEY`)

## Run the prototype
```
cd prototype
python3 cli.py                       # chat (offline MockLLM)
GEMINI_API_KEY=... python3 cli.py    # real Gemini
python3 simulate.py day              # 24-hour simulation
python3 simulate.py months --profile interactive
python3 simulate.py storage
```

## Open the Android app
Open the repo root in **Android Studio** (Hedgehog+ / JDK 17). `./gradlew` wrapper
included (Gradle 8.7, AGP 8.5.2, Kotlin 1.9.24, Compose BOM 2024.06). minSdk 26.

## Locked build rule (spec §26)
LOCKED ≠ implemented. Build in phases, keep interfaces. Check every schema, API,
screen and test against `docs/SPEC_SUMMARY_v1.0.md` before calling a feature done.
