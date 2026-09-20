# Architecture Map — spec § → code path
**Single source of truth for AISHA's mind: the pure-Kotlin `:core` module**
(zero Android dependencies, JVM-unit-tested). `:app` = Android shell (Room,
Keystore, Gemini, Compose). `prototype/` = Python test bench (never ships).

| Spec § | System | Where it lives now | Status |
|---|---|---|---|
| §4/§5 | Models, Mood, Bond, Decision, Safety, Prompt, Pipeline | `core/src/main/kotlin/com/aisha/core/` | ✅ implemented + 22 unit tests green |
| §6 | Day Log Store (encrypt+compress+integrity) | `app/data/RoomDayLogStore.kt` + `AishaDatabase.kt` (Room) | ✅ production impl |
| §14 | AES-256/GCM via Android Keystore | `app/security/KeystoreCrypto.kt` (+ contract `core/SecurityContracts.kt`) | ✅ production impl |
| §5 | Real LLM (replaceable) | `app/ai/GeminiLanguageModel.kt` (key via `local.properties`) | ✅ + Mock fallback |
| §5 | Orchestrator | `core/CoreEngine.kt` → wired in `di/AppContainer.kt` → `ui/ChatViewModel.kt` → `ui/screens/ChatScreen.kt` | ✅ end-to-end |
| §3 | 7 screens | `app/ui/screens/` (Chat live; others phase 2+) | 🟡 scaffolded |
| §10 | Tasks/Reminders/Notifications | `app/tasks/TaskModules.kt` contracts | ⏳ phase 3 |
| §11/§12 | Avatar/Live Wallpaper | `app/avatar/` contracts | ⏳ phase 4 |
| §13 | Voice | — | ⏳ phase 4 |
| §16/§17 | Cloud/sync/40-day retention | Room `status` = upload queue seed; cloud client | ⏳ phase 2 |
| §18 | Export/Trash/Recovery | `app/memory/MemoryModules.kt` contracts | ⏳ phase 2 |
| §19 | Audit/Admin | `app/security/SecurityModules.kt`, `app/admin/` | ⏳ phase 5 |
| §20 | Background services | `app/services/ServiceContracts.kt` | ⏳ phase 2 (midnight finalizer first) |
| §21 | Offline-first | core degraded path + Mock fallback | 🟡 engine done; device paths phase 2 |
| §24 | Simulations | `prototype/simulate.py` (bench) + `core/src/test/` (production tests) | ✅ |

## Phases
1. ✅ **Core on Android** — engines, Room, Keystore crypto, Gemini, chat end-to-end, CI
2. **Memory complete** — midnight finalizer (WorkManager), Day Log viewer UI, export/trash/recovery, sync worker
3. **Presence & tasks** — notifications, reminders, planner, ambient policy
4. **Avatar & voice** — stylized avatar → 3D, TTS/STT, lip-sync, live wallpaper
5. **Security hardening** — audit, admin boundaries, key rotation, on-device sim suite

## CI
`.github/workflows/android-ci.yml` — on every push: `:core:test` + `:app:testDebugUnitTest` + `assembleDebug`, APK artifact upload.
