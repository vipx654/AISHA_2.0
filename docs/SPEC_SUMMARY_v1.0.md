# AISHA v1.0 — Spec Summary & Engineering Interpretation
> Condensed working notes from `AISHA_Locked_Module_Feature_Specification_v1.0.pdf` (16 pages).
> The PDF remains the master document; this file is our daily-reference digest.

---

## 1. What AISHA IS
- **Mobile-first (Android) AI companion** — calm, emotionally natural, romantic-companion experience via a *controlled* relationship/bond model.
- **The LLM is only a component** — it generates wording; it does NOT own memory truth, authorization, or mood/bond state.
- **Offline-first, optional cloud** — local encrypted working storage; cloud is only backup/sync/recovery.
- **User owns data** — memory control, export, deletion, backup are user-governed.
- **3D avatar = state visualizer** — reflects internal engine state; never invents emotion on its own.

## 2. The 10 Major Systems (Master Inventory)
| # | System | Locked modules |
|---|--------|----------------|
| 1 | User Interface | Home, Chat, Voice, Day Logs, Mood, Relationship, Settings |
| 2 | Core Intelligence | Identity, Mood, Bond, Decision, Presence, Conversation, Safety/Rules |
| 3 | AI Integration | Input Processing, Prompt Builder, Language Model, Response Validator |
| 4 | Memory | Working Memory, Day Logs, Recall, Export, Trash, Recovery |
| 5 | Avatar | Controller, Expressions, Animation, Lip Sync, Clothing, Live Wallpaper |
| 6 | Task/Productivity | Tasks, Reminders, Daily Planner, Notifications |
| 7 | Security | Encryption, Key Management, Audit, Secure Storage |
| 8 | Cloud | Sync, Upload Queue, Download Queue, Backup, Storage Mgmt |
| 9 | Administration | Admin Dashboard, Super Admin, Recovery, Monitoring |
| 10 | System Services | Midnight/Day-Finalization, Sync, Notification, Battery, Update, Crash Recovery |

## 3. Core Intelligence (the "brain" we orchestrate)
- **Identity Engine** — stable personality, communication style, values. No arbitrary personality drift.
- **Mood Engine** — 5 structured values: happiness, calmness, energy, affection, curiosity. Short-term, gradual updates. Feeds Decision Engine + Avatar. It's a software model, not a claim of human emotion.
- **Love Bond Engine** — long-term relationship state. Slow growth from trust/consistency/shared experience. Stage-aware behavior. Stages: Companion → Warmer familiarity → Romantic interaction → Deep bond. NO manipulation, blackmail, coercion, possessiveness, or deliberate isolation. User boundaries override proactivity.
- **Decision Engine** — central coordinator: whether to speak, tone/length, memory use, ask/support/inform → produces a *response intent*.
- **Presence Engine** — greetings, return-after-absence, quiet when inactive, non-intrusive.
- **Conversation Engine** — active context, topic flow, continuity, selective memory requests.
- **Safety & Rule Engine** — privacy boundaries, relationship-stage constraints, **no fabricated memories**, pre/post generation validation.

## 4. The AI Pipeline (locked order)
```
User input → Input Processing → Identity/Mood/Bond state → Memory (selective)
→ Task context (if relevant) → Decision Engine → Safety/Rules
→ Prompt Builder → Language Model → Response Validator → Reply → Day Log
```
- **Prompt Builder** assembles: identity + mood + bond stage + relevant memories + active context + response intent + safety constraints.
- **Response Validator** checks: tone, rule compliance, context consistency, privacy leakage → can regenerate/adjust.
- **Long context**: recent = active, older = summarized, important facts come from Memory Engine (not raw history stuffing).
- LLM is **replaceable/upgradable** — swap without touching the rest.

## 5. Memory & Day Logs
- **Day Log lifecycle:** create container at day start → collect conversations/events/mood/bond/task records → finalize (summary → integrity check → compression → **encryption** → local storage → sync queue) → next day's log.
- **Day Log contents:** summary, conversations, optional voice records, mood timeline, bond timeline, tasks, important events, security metadata.
- **Recall:** find relevant date/topic → retrieve ONLY relevant records → never invent missing memories.
- Modules: Working Memory, Day Log Manager, Recall Engine, Export Manager, Trash Manager, Recovery Manager.

## 6. Avatar & Presence
- Flow: **Core state → Avatar Controller → expression/animation/lip-sync → UI / Live Wallpaper.**
- Expressions: neutral, happy, thoughtful, concerned (expandable). Animations: breathing, blinking, posture, looking around, walking/sitting, idle variation. Micro-expressions + natural eye contact.
- Lip sync tied to TTS playback. Clothing = cosmetic only, independent of mood.
- **Live Wallpaper:** optional, lightweight, battery-aware, NO mic/camera just because wallpaper is on, no conversation triggered by unlock alone.

## 7. Voice
Voice input → recognition → conversation pipeline → TTS → lip sync. Optional voice logging; retained audio encrypted; no persistent retention when logging disabled.

## 8. Security & Privacy
- Protection flow: **raw data → compress → encrypt → store/upload.**
- Encrypted always: conversations, day logs, audio/video, backups, exports.
- Keys: platform secure storage (Android Keystore-class), separate user-data key material, rotation-capable.
- Roles: **User** (own data/settings), **Admin** (authorized maintenance/audit), **Super Admin** (security config, keys, disaster recovery). Privileged authorization enforced **server-side** — client is never trusted to grant admin.
- Audit logs record accountability events WITHOUT duplicating private conversation content.

## 9. Cloud, Sync & Retention
- Purpose: backup, recovery, sync, future multi-device. Local-first; cloud optional per user.
- Flow: Local DB → Encryption → Sync Manager → Upload Queue → Cloud. Upload queue retries, keeps local source until confirmed. Download: validate → decrypt locally → rebuild indexes.
- Conflict handling: explicit version/timestamp/merge policy — never silent overwrite.
- **LOCKED: 40-day cloud retention.** Cloud expiry must NOT delete active local data.

## 10. Trash, Deletion, Recovery
- Delete = remove from active UI/recall → move encrypted copy to protected recovery lifecycle → sync state.
- Hidden Trash: encrypted, not user-browsable, not used by normal recall, authorized recovery only.
- Recovery: authorize → validate → restore → rebuild indexes → audit event.

## 11. Background Services
Midnight/Day-Finalization, Sync, Notification, Battery Manager, Update Manager (with DB migration), Crash Recovery, Storage Manager, Connectivity Monitor.

## 12. Offline-First Failure Matrix (key rows)
| Failure | Behavior |
|---|---|
| No internet | Local features continue; cloud ops queue |
| AI service down | Preserve state, degraded/local behavior, inform user |
| Upload interrupted | Keep source, mark pending, retry |
| Phone restart | Recover local DB + queues |
| App update | Migrate DB, preserve user data |
| Phone lost | Cloud recovery only if backup was enabled |
| Corrupt backup | Validate, never blind-restore, use another valid point |

## 13. Testing Requirements (must-build)
- **Six-month simulations:** interactive / busy / ignored user; day-by-day mood-bond-memory; stage transitions; long-term recall.
- **24-hour simulation:** first login → morning greeting → task → text → voice → mood/bond events → avatar → offline period → reconnect → queue sync → day finalization → compression/encryption.
- **Security sims:** failed logins, authz failures, key rotation, interrupted upload, corrupt backup, cloud outage, recovery, audit verification.
- **Storage sims:** text-only, voice-heavy, media-heavy, 40-day retention, offline queue growth, thresholds.

## 14. Locked vs Roadmap
- **LOCKED/Required:** Android-first, all UI, Core Engine, AI pipeline, Memory/Day Logs, Tasks, Avatar+lip-sync, Live Wallpaper, Voice, local encrypted storage, 40-day cloud retention, export/deletion/recovery, authn/authz, admin boundaries, audit, offline-first, background services, simulation framework. (Cloud backup itself = optional per user, architecture required.)
- **ROADMAP (post-launch):** preference learning, style adaptation, memory importance scoring, semantic retrieval, mood smoothing, advanced micro-expressions/outfits, seasonal themes, smarter reminders, multi-device sync, MFA, anomaly detection, key rotation, admin reporting, prompt/model optimization.

## 15. Master Implementation Rule
> LOCKED ≠ implemented. Build in phases, but preserve the interfaces/responsibilities defined here. Every future schema, API, screen, and test is checked against this document before a feature counts as complete.
