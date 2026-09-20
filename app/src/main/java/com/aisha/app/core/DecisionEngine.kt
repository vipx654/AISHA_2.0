package com.aisha.app.core

import com.aisha.app.model.BondState
import com.aisha.app.model.Decision
import com.aisha.app.model.MoodState
import com.aisha.app.model.ProcessedInput

/**
 * LOCKED §4 — Decision Engine. Central behaviour coordinator.
 * Decides WHETHER to speak, tone/length, whether memory is used, and the goal
 * (ask / support / inform / listen / playful). Produces a [Decision] consumed
 * by the Prompt Builder. The LLM never makes these decisions itself.
 */
interface DecisionEngine {
    fun decide(
        input: ProcessedInput,
        mood: MoodState,
        bond: BondState,
        presence: PresenceContext,
    ): Decision
}

data class PresenceContext(
    val timeOfDayMs: Long,
    val userReturnedAfterMs: Long?,   // return-after-absence (spec §9)
    val userBusyHint: Boolean,        // respect attention/notification settings
    val isAmbient: Boolean,           // wallpaper/ambient presence vs active chat
)
