package com.aisha.app.core

import com.aisha.app.model.EngineSnapshot
import com.aisha.app.model.Role

/**
 * LOCKED §2 + §5 — Core Engine: the coordinator of coordinators.
 * Owns the locked pipeline order:
 *   User input → Input Processing → Identity/Mood/Bond → Memory → Task context
 *   → Decision → Safety/Rules → Prompt Builder → Language Model
 *   → Response Validator → Reply → Day Log.
 */
interface CoreEngine {
    /** Full locked pipeline for one user turn. Returns AISHA's reply (already validated). */
    suspend fun handleUserTurn(rawInput: String, viaVoice: Boolean): TurnResult

    /** Snapshot for the Avatar Controller (state flow: Core → Avatar → UI/wallpaper). */
    fun snapshot(): EngineSnapshot

    /** Degrade gracefully when the AI service is unavailable (spec §21). */
    fun degradedReply(reason: DegradedReason): TurnResult
}

data class TurnResult(
    val role: Role,
    val text: String,
    val wasDegraded: Boolean = false,
    val safeActivityNote: String? = null,  // user-visible safe summary, never raw reasoning
)

enum class DegradedReason { AI_SERVICE_UNAVAILABLE, NETWORK_OFFLINE, VALIDATOR_BLOCKED }
