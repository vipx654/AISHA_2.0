# Architecture Map — spec § → code path
Every locked module has a single home. Keep this table current.

| Spec § | System | Android (Kotlin) | Prototype (Python) |
|---|---|---|---|
| §3 | UI screens (7) | `app/src/main/java/com/aisha/app/ui/` | console CLI |
| §4 | Identity Engine | `core/IdentityEngine.kt` | `aisha/identity.py` |
| §4+§8 | Mood Engine | `core/MoodEngine.kt` | `aisha/mood.py` |
| §4+§7 | Love Bond Engine | `core/BondEngine.kt` | `aisha/bond.py` |
| §4 | Decision Engine | `core/DecisionEngine.kt` | `aisha/decision.py` |
| §4+§9 | Presence Engine | `core/PresenceEngine.kt` | `aisha/presence.py` |
| §4+§5 | Conversation Engine | `core/ConversationEngine.kt` | `engine.WorkingMemory` |
| §4+§23 | Safety & Rule Engine | `core/SafetyRuleEngine.kt` | `aisha/safety.py` |
| §5 | Input Processing | `ai/AiPipeline.kt` | `engine.InputProcessor` |
| §5 | Prompt Builder | `ai/AiPipeline.kt` | `aisha/prompt_builder.py` |
| §5 | Language Model (replaceable) | `ai/AiPipeline.kt` | `aisha/llm.py` (Mock+Gemini) |
| §5 | Response Validator | `ai/AiPipeline.kt` | `aisha/validator.py` |
| §5 | Core Engine orchestrator | `core/CoreEngine.kt` | `aisha/engine.py` |
| §6 | Working Memory / Day Logs / Recall | `memory/MemoryModules.kt` | `aisha/memory.py` |
| §6/§17 | Storage & integrity | — (Room + Keystore, phase 2) | `aisha/storage.py` |
| §10 | Tasks / Reminders / Planner / Notifications | `tasks/TaskModules.kt` | (stub in prototype) |
| §11+§12 | Avatar / Live Wallpaper | `avatar/AvatarModules.kt` | — |
| §13 | Voice pipeline | (phase 4) | — |
| §14+§15+§19 | Security / Keys / Auth / Audit | `security/SecurityModules.kt`, `admin/AdminModules.kt` | (prototype-grade tags only) |
| §16+§17 | Cloud / Queues / 40-day retention | `cloud/CloudModules.kt` | `storage.enqueue/drain_sync_queue` |
| §18 | Trash / Deletion / Recovery | `memory/MemoryModules.kt` | — |
| §20 | Background services | `services/ServiceContracts.kt` | `engine.advance_day` (midnight path) |
| §21 | Offline-first failure handling | (phased) | `_degraded` path in engine |
| §24 | Simulation framework | (test module, later) | `simulate.py` |

## Implementation phases (proposal — each phase ships against the locked checklist)
1. **Phase 1 — Core on Android:** port prototype engines to Kotlin, Room schema for
   day logs, EncryptedSharedPreferences/Keystore wiring, AppContainer DI, chat screen
   connected to Gemini via PromptBuilder pipeline.
2. **Phase 2 — Memory complete:** Day Log finalization service (WorkManager midnight),
   recall UI, export/trash/recovery flows, sync queues + fake-cloud endpoint.
3. **Phase 3 — Presence & tasks:** notifications, reminders, daily planner,
   presence greetings, ambient policy.
4. **Phase 4 — Avatar & voice:** stylized 2D avatar (upgrade path to 3D), TTS/STT,
   lip-sync, live wallpaper.
5. **Phase 5 — Security hardening + admin + audit + simulation suite on device.**
