package com.aisha.app.core

import com.aisha.app.model.ChatMessage
import com.aisha.app.model.ProcessedInput

/**
 * LOCKED §4 + §5 — Conversation Engine.
 * Owns active context, topic flow, continuity and response structure.
 * Long-context handling: recent turns stay active; older context is summarised;
 * important facts arrive via the Memory Engine, never by raw history stuffing.
 */
interface ConversationEngine {
    fun activeContext(): List<ChatMessage>

    fun onUserInput(processed: ProcessedInput)

    fun onAishaReply(message: ChatMessage)

    /** Rolling summary of older context for the Prompt Builder. */
    fun olderContextSummary(): String?

    fun requestMemory(topic: String)   // selective memory request → Recall Engine
}
