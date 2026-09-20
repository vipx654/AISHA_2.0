package com.aisha.app.memory

import com.aisha.core.Authorization
import java.io.File

/**
 * Contracts for systems built in phase 2 (spec §6/§17/§18). Working memory,
 * Day Logs and Recall live in :core + :app data layer.
 */
interface ExportManager {
    fun exportHumanReadable(dateRange: ClosedRange<String>): File
    fun exportEncryptedArchive(dateRange: ClosedRange<String>): File
}

/** LOCKED §18 — hidden, encrypted, not user-browsable, authorized recovery only. */
interface TrashManager {
    fun moveToTrash(itemId: String, reason: String)
    fun restore(itemId: String, authorization: Authorization): Boolean
}

/** LOCKED §18 — authorize → validate → restore → rebuild indexes → audit event. */
interface RecoveryManager {
    fun requestRecovery(dateRange: ClosedRange<String>, authorization: Authorization): RecoveryReport
}

data class RecoveryReport(val restoredItems: Int, val auditEventId: String, val success: Boolean)
