package com.aisha.app.core

import com.aisha.app.model.BondStage
import com.aisha.app.model.MoodState

/** Immutable personality context handed to the Prompt Builder. Spec §4 Identity Engine. */
data class IdentityContext(
    val name: String,
    val personality: String,       // stable description, never drifts arbitrarily
    val communicationStyle: String,
    val coreValues: List<String>,
    val languagePolicy: String,    // Hindi/English behaviour
)

/**
 * LOCKED §4 — Identity Engine.
 * Stable personality, communication style, core values, consistent identity.
 * The language model must never be allowed to redefine AISHA's identity.
 */
interface IdentityEngine {
    fun personalityContext(): IdentityContext

    /** Stage/tone adjustments are expressed as constraints, never personality changes. */
    fun styleConstraints(stage: BondStage, mood: MoodState): List<String>
}
