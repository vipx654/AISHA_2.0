package com.aisha.core

/**
 * LOCKED spec §4+§8 — Mood Engine. 5 structured values, gradual updates only.
 * Feeds Decision Engine + Avatar. A software model — never claims human emotion.
 * Deltas validated by prototype 6-month sims (2026-09-20/21).
 */
class MoodEngine(
    private val clock: Clock,
    private val onMeaningfulChange: (MoodChange) -> Unit = {},
) {
    var state: MoodState = MoodState(); private set
    val timeline = mutableListOf<MoodChange>()

    fun current(): MoodState = state

    fun applyInteraction(signal: InteractionSignal) {
        val d = DELTAS[signal.kind] ?: return
        val before = state
        state = state.shifted(d.h * signal.intensity, d.c * signal.intensity, d.e * signal.intensity,
            d.a * signal.intensity, d.u * signal.intensity)
        record(before, signal.kind.name.lowercase())
    }

    /** Gradual overnight decay toward baseline; [fractionOfDay] 0..1. */
    fun tick(fractionOfDay: Float = 1f) {
        val before = state
        val t = DECAY_PER_DAY * fractionOfDay
        fun towards(v: Float, base: Float = 0.5f) = clamp(v + (base - v) * t)
        state = MoodState(towards(state.happiness), towards(state.calmness), towards(state.energy),
            towards(state.affection), towards(state.curiosity))
        record(before, "decay")
    }

    private fun record(before: MoodState, cause: String) {
        val moved = maxOf(
            kotlin.math.abs(before.happiness - state.happiness),
            kotlin.math.abs(before.calmness - state.calmness),
            kotlin.math.abs(before.energy - state.energy),
            kotlin.math.abs(before.affection - state.affection),
            kotlin.math.abs(before.curiosity - state.curiosity))
        if (moved >= MEANINGFUL_DELTA) {
            val change = MoodChange(clock.now(), cause, before, state)
            timeline += change
            onMeaningfulChange(change)
        }
    }

    companion object {
        private const val DECAY_PER_DAY = 0.25f
        private const val MEANINGFUL_DELTA = 0.06f
        private data class D(val h: Float = 0f, val c: Float = 0f, val e: Float = 0f, val a: Float = 0f, val u: Float = 0f)
        private val DELTAS = mapOf(
            SignalKind.POSITIVE_TALK to D(h = 0.045f, c = 0.02f, a = 0.03f, u = 0.01f),
            SignalKind.QUESTION to D(u = 0.06f, e = 0.02f),
            SignalKind.DEEP_CONVERSATION to D(h = -0.02f, c = -0.03f, a = 0.05f, u = 0.04f),
            SignalKind.TASK_DONE to D(h = 0.04f, e = 0.02f),
            SignalKind.COLDNESS to D(h = -0.04f, a = -0.03f, e = -0.02f),
            SignalKind.CONFLICT to D(h = -0.08f, c = -0.06f, a = -0.05f),
            SignalKind.LONG_ABSENCE to D(e = 0.05f, u = 0.04f, h = -0.02f),
            SignalKind.AFFECTION to D(h = 0.06f, a = 0.07f),
        )
    }
}
