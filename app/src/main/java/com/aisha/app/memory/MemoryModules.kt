package com.aisha.app.memory

import com.aisha.app.model.ChatMessage
import com.aisha.app.model.DayLogSummary
import com.aisha.app.model.MemoryHit

/**
 * LOCKED §6 — Working Memory. Active-session scratch state only.
 * Truth lives in Day Logs, never here.
 */
interface WorkingMemory {
    fun push(message: ChatMessage)
    fun recent(n: Int): List<ChatMessage>
    fun clear()
}

/** Status of a Day Log in its lifecycle: create → collect → finalize → stored → synced. */
enum class DayLogStatus { OPEN, FINALIZED, SYNC_QUEUED, SYNCED }

/**
 * LOCKED §6 — Day Log Manager. Daily container lifecycle:
 * create at day start → collect conversations/events → finalize
 * (summary → integrity check → compression → ENCRYPTION → local storage → sync queue)
 * → create next day's log. Handled at midnight by the Day Finalization Service (§20).
 */
interface DayLogManager {
    fun today(): DayLogHandle
    fun recordConversation(message: ChatMessage)
    fun recordMoodPoint(atMs: Long, mood: com.aisha.app.model.MoodState)
    fun recordBondPoint(atMs: Long, bond: com.aisha.app.model.BondState)
    fun recordEvent(type: String, description: String)
    fun finalizeDay(): DayLogSummary     // midnight path only
    fun nextDay()
}

data class DayLogHandle(
    val id: String,                      // "2026-09-20"
    val status: DayLogStatus,
)

/**
 * LOCKED §6 — Recall Engine. Identify relevant date/topic/context, retrieve
 * ONLY relevant records, supply to the response pipeline. NEVER invent
 * missing memories. Deleted memories are unavailable here (hidden trash).
 */
interface RecallEngine {
    fun recall(query: String, limit: Int = 5): List<MemoryHit>
    fun recallByDate(dateId: String): List<MemoryHit>
}

/** LOCKED §6/§17 — human-readable + encrypted-archive exports, clearly labelled. */
interface ExportManager {
    fun exportHumanReadable(dateRange: ClosedRange<String>): java.io.File
    fun exportEncryptedArchive(dateRange: ClosedRange<String>): java.io.File
}

/** LOCKED §18 — hidden, encrypted, not user-browsable, authorized recovery only. */
interface TrashManager {
    fun moveToTrash(itemId: String, reason: String)
    fun restore(itemId: String, authorization: com.aisha.app.security.Authorization): Boolean
}

/** LOCKED §18 — authorize → validate → restore → rebuild indexes → audit event. */
interface RecoveryManager {
    fun requestRecovery(dateRange: ClosedRange<String>, authorization: com.aisha.app.security.Authorization): RecoveryReport
}

data class RecoveryReport(val restoredItems: Int, val auditEventId: String, val success: Boolean)
