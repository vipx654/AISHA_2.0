# Master Build Checklist — Status Tracker
> Governed by MASTER BUILD SPECIFICATION §24/§25: nothing is silently dropped.
> Every locked module stays on this list forever — ✅ implemented, 🟡 partial, ⏳ PENDING.
> "Done" requires the Master Implementation Contract (§22): input → validation →
> processing → state change → persistence → security → error handling → UI/API
> integration → background behavior → tests → recovery.

| # | Master item | Status | Lives in | Notes |
|---|---|---|---|---|
| 1 | Application UI & navigation | 🟡 | `app/ui/` | 7 screens scaffolded; Chat live end-to-end; Home/Mood/Bond/Logs render placeholders |
| 2 | Core Intelligence engines | ✅ | `core/` (Identity, Mood, Bond, Decision, Presence, Conversation via WorkingMemory, Safety) | 22 unit tests green |
| 3 | AI integration + response validation | ✅ | `core/AiPipeline.kt`, `app/ai/GeminiLanguageModel.kt` | pipeline locked; Gemini + Mock; validator blocks/adjusts |
| 4 | Memory, Day Logs, recall | 🟡 | `core/Memory.kt`, `app/data/`, `ui/DayLogViewModel` | engine+store+recall+viewer UI done (decrypt→integrity→render); recall search UI ⏳ |
| 5 | Mood & Love Bond state | ✅ | `core/MoodEngine.kt`, `core/BondEngine.kt` | validated economy; stage-gating |
| 6 | Presence & proactive behavior | 🟡 | `core/PresenceEngine.kt` (4 tests) + Chat greeting wiring | time-of-day + return-after-absence greetings live on device; zero-guilt tiers; proactive notification delivery ⏳ (gate logic done) |
| 7 | Tasks, reminders, notifications | ✅ | `core/TaskEngine.kt` (5 tests) + Room tasks v3 + `TaskReminderWorker` + `AishaNotifier` + Tasks UI | create/edit/complete validated; reminders persist across restarts; quiet hours queue instead of firing; channel + Android 13 runtime permission |
| 8 | 3D avatar & rendering pipeline | ⏳ PENDING | `app/avatar/` contracts only | phase 4 (stylized 2D → 3D) |
| 9 | Voice & lip sync | ⏳ PENDING | — | phase 4 |
| 10 | Authentication & authorization | 🟡 | `core/SecurityContracts.kt` roles | local-only mode real; account mode/Google Sign-In ⏳ |
| 11 | Encryption & key management | 🟡 | `app/security/KeystoreCrypto.kt` | AES-256/GCM real; audit+rotation workflows ⏳ |
| 12 | Cloud backup/sync & queues | ⏳ PENDING | Room `status` queue seed; cloud client ⏳ | phase 2; 40-day retention LOCKED |
| 13 | Storage, retention, export, recovery | 🟡 | Room store + `memory/AndroidExportManager.kt` | readable+encrypted exports done; share-sheet UI ⏳; cloud retention ⏳ |
| 14 | Trash/deletion lifecycle | ✅ | `core/Trash.kt` (4 tests) + `app/data/RoomTrashStore.kt` + viewer delete flow | delete→protected trash→authorized restore→audit; purge SuperAdmin-gated |
| 15 | Admin & audit | ⏳ PENDING | `app/security/SecurityModules.kt`, `app/admin/` contracts | phase 5; server-side enforcement |
| 16 | Background services | 🟡 | `app/services/Workers.kt` | Day Finalization worker (midnight, self-rescheduling, boot-rearmed) ✅; sync worker queues honestly (cloud backend ⏳ PENDING §25); notification/battery/update/crash services ⏳ |
| 17 | Offline/failure handling | 🟡 | `core/CoreEngine.kt` degraded paths | engine done; device paths (restart/recovery) ⏳ |
| 18 | Simulation & automated tests | 🟡 | `core/src/test/` (22 green), `prototype/simulate.py` | CI runs suite; on-device sims ⏳ |
| 19 | Migration/update safety | 🟡 | Room v2 + `MIGRATION_1_2` | explicit data-preserving migration shipped; future versions follow |
| 20 | Post-launch extension interfaces | 🟡 | `docs/ARCHITECTURE_MAP.md` phase plan | extension points reserved, none remove foundation |

**Master rule check (§25):** no locked module has been removed or replaced by a
fake placeholder. Everything not built is tracked ⏳ PENDING above.
