# Master Build + Workflow Digest — what the new documents change
> Sources: `docs/specs/AISHA_MASTER_BUILD_SPECIFICATION.pdf` (MASTER BUILD),
> `docs/specs/AISHA_Feature_Workflow_and_Operation_Specification_v1.0.pdf`,
> original `AISHA_Locked_Module_Feature_Specification_v1.0.pdf`.

## New/strongened rules and how we comply
| Rule | Source | Implementation in repo |
|---|---|---|
| State ownership: ONE authoritative owner per state; readers never overwrite | Master §21 | Engines own their state in `:core` (mood/bond/identity/decision); Room owns day logs; AvatarController will own render state; SyncManager will own sync status. Enforced by architecture + reviewed in PR checklist |
| Implementation contract (definition of done) | Master §22 | Headed column set of MASTER_BUILD_CHECKLIST; PRs must tick all 11 contract stages |
| Nothing dropped / staged = PENDING, tracked | Master §25 | MASTER_BUILD_CHECKLIST.md — permanent tracker, ⏳ items stay visible |
| Safe activity log, never chain-of-thought | Master §19, Workflow §20 | `TurnResult.activityNotes` — fixed safe phrases emitted by pipeline; unit-tested to never contain user text |
| Event importance classification | Workflow §5 | `Importance` enum (LOW/NORMAL/HIGH/MILESTONE) on DayEvent; recall boosts by importance; stage changes recorded as MILESTONE |
| No fabricated absence events | Workflow §18 | Presence greetings derive from real state (last-active bookkeeping), never invented events |
| Memory accuracy check in validator | Workflow §4 step 7 | SafetyRuleEngine MEMORY_ASSERTS vs supplied memories (implemented + tested) |
| 9-step response pipeline incl. Context Assembly + Logging | Workflow §4 | Locked pipeline in `core/CoreEngine.kt` — same order, logging step present |
| 40-day cloud retention unchanged | Master §13 | Stays LOCKED for phase 2 cloud work |

## Build-order impact
Phase plan unchanged (ARCHITECTURE_MAP.md) but every phase now exits only via the
§22 contract. Phase 2 (next): midnight Day Finalization service, Day Log viewer,
export/trash, sync worker — each with tests + error handling per contract.
