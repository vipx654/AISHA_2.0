package com.aisha.app.model

/**
 * Shared, locked data vocabulary for every AISHA module.
 * Source of truth: AISHA Locked Module & Feature Specification v1.0 (§2–§8).
 * Do not rename fields casually — engines, prompts, DB schemas and tests all reference these.
 */

/* ---------------------------- Mood (spec §8) ---------------------------- */

/** Short-term emotional state. 5 structured values, all normalised to 0f..1f. */
data class MoodState(
    val happiness: Float = 0.5f,
    val calmness: Float = 0.5f,
    val energy: Float = 0.5f,
    val affection: Float = 0.5f,
    val curiosity: Float = 0.5f,
) {
    /** Guarded gradual shift — spec requires gradual updates, never instant jumps. */
    fun shifted(deltaHappiness: Float = 0f, deltaCalmness: Float = 0f, deltaEnergy: Float = 0f,
                deltaAffection: Float = 0f, deltaCuriosity: Float = 0f, maxStep: Float = 0.12f): MoodState {
        fun clamp(v: Float, d: Float) = (v + d.coerceIn(-maxStep, maxStep)).coerceIn(0f, 1f)
        return copy(
            happiness = clamp(happiness, deltaHappiness),
            calmness = clamp(calmness, deltaCalmness),
            energy = clamp(energy, deltaEnergy),
            affection = clamp(affection, deltaAffection),
            curiosity = clamp(curiosity, deltaCuriosity),
        )
    }
}

/* ------------------------ Love Bond (spec §7) ------------------------ */

/** Long-term relationship stages. Progression is slow and stage-gated. */
enum class BondStage(val order: Int) {
    COMPANION(0),            // helpful calm companion
    WARM_FAMILIARITY(1),     // warmer familiarity
    ROMANTIC(2),             // romantic interaction (gated)
    DEEP_BOND(3),            // deep bond (gated)
}

/** Long-term bond state. Mood ≠ Bond: mood is hours, bond is weeks/months. */
data class BondState(
    val stage: BondStage = BondStage.COMPANION,
    val progressInStage: Float = 0f,   // 0f..1f within current stage
    val trust: Float = 0.2f,
    val consistency: Float = 0.2f,
) {
    val totalScore: Float get() = (stage.order * 25f) + progressInStage * 25f // 0..100 across lifetime
}

/* ------------------------ Decisions (spec §4) ------------------------ */

/** What the Decision Engine wants from the language model for this reply. */
enum class ResponseGoal { ANSWER, ASK, SUPPORT, INFORM, LISTEN, PLAYFUL, STAY_QUIET }

data class Decision(
    val shouldSpeak: Boolean,
    val goal: ResponseGoal,
    val toneHint: String,          // e.g. "warm", "soothing", "practical"
    val lengthHint: String,        // e.g. "one line", "short", "detailed"
    val useMemory: Boolean,
    val reason: String,            // human-safe explanation, may surface in activity log
)

/* ------------------------ Conversation (spec §5) ------------------------ */

enum class Role { USER, AISHA, SYSTEM }

data class ChatMessage(
    val id: Long,
    val role: Role,
    val text: String,
    val timestampMs: Long,
    val language: String = "en",   // "en" | "hi" — spec: Hindi/English
)

data class ProcessedInput(
    val raw: String,
    val language: String,
    val intentHint: String,        // question / greeting / task / emotional / smalltalk / command
    val mentionsPast: Boolean,     // "do you remember…" → triggers Recall Engine
    val emotionalValence: Float,   // -1f..1f quick heuristic for Mood/Bond engines
)

/* ---------------------------- Memory (spec §6) ---------------------------- */

data class MemoryHit(
    val dayLogId: String,          // e.g. "2026-09-20"
    val snippet: String,
    val relevance: Float,
)

data class DayLogSummary(
    val dayLogId: String,
    val headline: String,
    val moodTrend: String,
    val bondTrend: String,
)

/* ---------------------------- Avatar (spec §11) ---------------------------- */

enum class Expression { NEUTRAL, HAPPY, THOUGHTFUL, CONCERNED }

/** Everything the Avatar Controller is allowed to know. Avatar never invents state. */
data class EngineSnapshot(
    val mood: MoodState,
    val bond: BondState,
    val expression: Expression,
    val isSpeaking: Boolean,
    val isThinking: Boolean,
)
