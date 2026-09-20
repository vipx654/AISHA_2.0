package com.aisha.core

import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min

/** Shared locked vocabulary. Spec §2–§8. Mirrored 1:1 by the validated prototype. */

fun clamp(v: Float, lo: Float = 0f, hi: Float = 1f): Float = max(lo, min(hi, v))

/* ---------------- Mood (spec §8) ---------------- */
data class MoodState(
    val happiness: Float = 0.5f,
    val calmness: Float = 0.5f,
    val energy: Float = 0.5f,
    val affection: Float = 0.5f,
    val curiosity: Float = 0.5f,
) {
    /** Gradual updates only (spec §8) — per-call change capped at [maxStep]. */
    fun shifted(dh: Float = 0f, dc: Float = 0f, de: Float = 0f, da: Float = 0f, du: Float = 0f,
                maxStep: Float = 0.12f): MoodState {
        fun step(v: Float, d: Float) = clamp(v + d.coerceIn(-maxStep, maxStep))
        return MoodState(step(happiness, dh), step(calmness, dc), step(energy, de),
            step(affection, da), step(curiosity, du))
    }

    fun describe(): String {
        fun lvl(v: Float) = if (v >= 0.66f) "high" else if (v <= 0.33f) "low" else "mid"
        return "happiness=${lvl(happiness)} calmness=${lvl(calmness)} energy=${lvl(energy)} " +
            "affection=${lvl(affection)} curiosity=${lvl(curiosity)}"
    }
}

/* ---------------- Love Bond (spec §7) ---------------- */
enum class BondStage(val order: Int, val label: String) {
    COMPANION(0, "Companion"),
    WARM_FAMILIARITY(1, "Warm Familiarity"),
    ROMANTIC(2, "Romantic"),
    DEEP_BOND(3, "Deep Bond"),
}

/** Lifetime score 0..100; stage thresholds LOCKED by validated tuning (2026-09 sims). */
const val STAGE_WARM = 25f; const val STAGE_ROMANTIC = 55f; const val STAGE_DEEP = 85f

data class BondState(
    val totalScore: Float = 0f,
    val trust: Float = 0.2f,
    val consistency: Float = 0.2f,
) {
    val stage: BondStage
        get() = when {
            totalScore < STAGE_WARM -> BondStage.COMPANION
            totalScore < STAGE_ROMANTIC -> BondStage.WARM_FAMILIARITY
            totalScore < STAGE_DEEP -> BondStage.ROMANTIC
            else -> BondStage.DEEP_BOND
        }
}

/* ---------------- Decisions (spec §4) ---------------- */
enum class ResponseGoal { ANSWER, ASK, SUPPORT, INFORM, LISTEN, PLAYFUL, STAY_QUIET }

data class Decision(
    val shouldSpeak: Boolean,
    val goal: ResponseGoal,
    val toneHint: String,
    val lengthHint: String,
    val useMemory: Boolean,
    val reason: String,
)

/* ---------------- Conversation (spec §5) ---------------- */
enum class Role { USER, AISHA, SYSTEM }

data class ChatMessage(
    val role: Role,
    val text: String,
    val at: LocalDateTime,
    val language: String = "en",
)

data class ProcessedInput(
    val raw: String,
    val language: String,
    val intentHint: String,       // greeting/question/task/emotional/smalltalk/command
    val mentionsPast: Boolean,
    val emotionalValence: Float,  // -1..1
)

/* ---------------- Mood signals ---------------- */
enum class SignalKind { POSITIVE_TALK, QUESTION, DEEP_CONVERSATION, TASK_DONE, COLDNESS, CONFLICT, LONG_ABSENCE, AFFECTION }

data class InteractionSignal(val kind: SignalKind, val intensity: Float = 1f)

data class MemoryHit(val dayLogId: String, val snippet: String, val relevance: Float)

data class MoodChange(val at: LocalDateTime?, val cause: String, val before: MoodState, val after: MoodState)

data class StageTransition(val at: LocalDateTime?, val old: BondStage, val new: BondStage)

/** Injectable time source — deterministic in tests. */
fun interface Clock { fun now(): LocalDateTime }
