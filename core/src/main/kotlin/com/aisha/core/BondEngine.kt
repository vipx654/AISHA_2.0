package com.aisha.core

import kotlin.math.max
import kotlin.math.min

/**
 * LOCKED spec §4+§7 — Love Bond Engine. Slow, stage-gated growth.
 * Economy validated by 6-month sims: interactive→Deep Bond ≈ month 4-5;
 * busy→Warm; ignored→floor at 0, never punished below it.
 * NO manipulation/possessiveness; user boundaries first (spec §23).
 */
class BondEngine(private val clock: Clock, private val onStageChange: (StageTransition) -> Unit = {}) {
    var state: BondState = BondState(); private set
    val transitions = mutableListOf<StageTransition>()
    private var lastStage: BondStage = BondStage.COMPANION

    fun current(): BondState = state

    fun onPositiveInteraction(quality: Float, kind: Kind = Kind.POSITIVE) {
        val pts = when (kind) {
            Kind.POSITIVE -> 0.10f
            Kind.DEEP -> 0.30f
            Kind.AFFECTION -> 0.22f
        }
        state = state.copy(
            totalScore = clamp(state.totalScore + pts * quality, 0f, 100f),
            trust = clamp(state.trust + 0.01f * quality))
        maybeTransition()
    }

    /** Consistency reward — requires REAL interaction streak (validated rule). */
    fun onDayComplete(streakDays: Int) {
        if (streakDays >= 2) {
            state = state.copy(totalScore = clamp(state.totalScore + min(0.30f, 0.10f * streakDays), 0f, 100f))
        }
        state = state.copy(consistency = clamp(state.consistency + if (streakDays >= 2) 0.02f else -0.01f))
        maybeTransition()
    }

    /** Absence decay — capped, floor-bounded, never punitive below zero. */
    fun onAbsence(days: Int) {
        if (days <= 1) return
        val drop = max(-6f, -0.25f * min(days, 24)).coerceAtMost(0f)
        state = state.copy(totalScore = clamp(state.totalScore + drop, 0f, 100f))
        maybeTransition()
    }

    fun onConflict(severity: Float) {
        state = state.copy(
            totalScore = clamp(state.totalScore - 1.5f * severity, 0f, 100f),
            trust = clamp(state.trust - 0.05f * severity))
        maybeTransition()
    }

    private fun maybeTransition() {
        val new = state.stage
        if (new != lastStage) {
            val t = StageTransition(clock.now(), lastStage, new)
            transitions += t
            lastStage = new
            onStageChange(t)
        }
    }

    enum class Kind { POSITIVE, DEEP, AFFECTION }
}
