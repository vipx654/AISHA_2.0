package com.aisha.app.core

import com.aisha.app.model.BondState
import com.aisha.app.model.BondStage

/**
 * LOCKED §4 + §7 — Love Bond Engine.
 * Long-term relationship state with slow, stage-gated growth driven by trust,
 * consistency and shared experience. Absence/conflict influence state per rules.
 * STAGE-AWARE behaviour only. NO manipulation, blackmail, coercion,
 * possessiveness or deliberate isolation. User boundaries override proactivity.
 */
interface BondEngine {
    fun current(): BondState

    /** Positive shared experience. Small increments; growth is gradual by design. */
    fun onPositiveInteraction(quality: Float)

    /** Consistency reward: regular contact over consecutive days. */
    fun onDayComplete(streakDays: Int)

    /** Absence decay — configurable, never punitive below a floor. */
    fun onAbsence(days: Int)

    /** Conflict/negative event decay per configured rules. */
    fun onConflict(severity: Float)

    /** Stage transitions must be recorded as bond-timeline events in the Day Log. */
    fun stageChangedListener(listener: (old: BondStage, new: BondStage) -> Unit)
}
