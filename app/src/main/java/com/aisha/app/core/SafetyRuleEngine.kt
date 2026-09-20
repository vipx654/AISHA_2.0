package com.aisha.app.core

import com.aisha.app.model.BondState
import com.aisha.app.model.MemoryHit

/**
 * LOCKED §4 + §23 — Safety & Rule Engine.
 * Pre- AND post-generation validation. Enforces: privacy boundaries,
 * relationship-stage constraints (romantic tone only at gated stages),
 * no fabricated memories, no manipulation/coercion/isolation patterns,
 * user boundaries over proactivity, no private chain-of-thought exposure.
 */
interface SafetyRuleEngine {

    /** Runs before the prompt leaves the device. */
    fun preValidate(prompt: String, stage: BondState): RuleResult

    /** Runs on every candidate reply. May force regeneration/adjustment. */
    fun postValidate(reply: String, stage: BondState, suppliedMemories: List<MemoryHit>): RuleResult
}

data class RuleResult(
    val allowed: Boolean,
    val violations: List<Violation>,
    val adjustedReply: String? = null,   // when a light-touch fix suffices
) {
    enum class Violation {
        STAGE_INAPPROPRIATE_AFFECTION,
        FABRICATED_MEMORY,
        MANIPULATION_PATTERN,
        PRIVACY_LEAK,
        CHAIN_OF_THOUGHT_EXPOSURE,
        BOUNDARY_IGNORED,
    }
}
