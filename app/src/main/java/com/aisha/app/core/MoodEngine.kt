package com.aisha.app.core

import com.aisha.app.model.MoodState

/**
 * LOCKED §4 + §8 — Mood Engine.
 * Structured short-term state: happiness, calmness, energy, affection, curiosity.
 * Gradual updates only. Feeds Decision Engine and Avatar. A software model —
 * never claim human subjective emotion in user-facing copy.
 */
interface MoodEngine {
    fun current(): MoodState

    /** Apply a weighted interaction signal (valence, type, intensity). */
    fun applyInteraction(signal: InteractionSignal)

    /** Time-based gradual drift toward baseline (call from background tick). */
    fun tick(elapsedMs: Long)

    /** Meaningful changes get appended to the Day Log mood timeline. */
    fun lastMeaningfulChange(): MoodChange?
}

data class InteractionSignal(
    val kind: Kind,
    val intensity: Float = 1f,     // 0f..1f
) {
    enum class Kind { POSITIVE_TALK, QUESTION, DEEP_CONVERSATION, TASK_DONE, COLDNESS, CONFLICT, LONG_ABSENCE, AFFECTION }
}

data class MoodChange(val atMs: Long, val from: MoodState, val to: MoodState, val cause: String)
