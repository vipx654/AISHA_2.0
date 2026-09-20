package com.aisha.app.ai

import com.aisha.app.model.BondState
import com.aisha.app.model.Decision
import com.aisha.app.model.MemoryHit
import com.aisha.app.model.MoodState
import com.aisha.app.model.ProcessedInput

/**
 * LOCKED §5 — Input Processing. Language/intent/context detection; produces
 * structured signals for the Core Engine. Never talks to the LLM directly.
 */
interface InputProcessor {
    fun process(raw: String): ProcessedInput
}

/**
 * LOCKED §5 — Prompt Builder. Assembles the structured prompt:
 * identity + mood + relationship stage + relevant memories + active context
 * + response intent + safety constraints. Deterministic and inspectable.
 */
interface PromptBuilder {
    fun build(
        identity: com.aisha.app.core.IdentityContext,
        mood: MoodState,
        bond: BondState,
        decision: Decision,
        memories: List<MemoryHit>,
        activeContext: List<com.aisha.app.model.ChatMessage>,
        olderSummary: String?,
        input: ProcessedInput,
    ): AishaPrompt
}

data class AishaPrompt(
    val systemBlock: String,
    val memoryBlock: String,
    val contextBlock: String,
    val constraintsBlock: String,
    val userTurn: String,
) {
    fun full(): String = systemBlock + "\n" + memoryBlock + "\n" + contextBlock + "\n" + constraintsBlock + "\n" + userTurn
}

/**
 * LOCKED §5 — Language Model. REPLACEABLE component: natural wording only.
 * Does NOT own authorization, encryption or memory truth. Swap freely.
 */
interface LanguageModelClient {
    suspend fun generate(prompt: AishaPrompt): String
}

/**
 * LOCKED §5 — Response Validator. Checks tone, rule compliance, context
 * consistency, privacy leakage. May trigger regeneration/adjustment.
 */
interface ResponseValidator {
    fun validate(prompt: AishaPrompt, reply: String, bond: BondState, memories: List<MemoryHit>): com.aisha.app.core.RuleResult
}
