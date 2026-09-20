package com.aisha.core

/**
 * LOCKED Master §14/§18 — Protected Trash lifecycle.
 * Deletion: remove from active recall → move encrypted data to protected
 * recovery lifecycle. Trash is NEVER a recall source and NOT user-browsable
 * data; restore requires authorization; recovery records an audit event.
 * The store holds ENCRYPTED blobs (impl in :app with Keystore crypto).
 */
data class TrashedDay(
    val dayId: String,
    val blob: ByteArray,          // already-encrypted payload
    val sha16: String,            // integrity tag of compressed bytes
    val reason: String,
    val deletedAtMs: Long,
    val rawBytes: Int,
)

interface TrashStore {
    fun put(item: TrashedDay)
    fun list(): List<TrashedDay>
    fun take(dayId: String): TrashedDay?
    fun purge(dayId: String)      // permanent, audit-logged by caller
}

class InMemoryTrashStore : TrashStore {
    private val map = linkedMapOf<String, TrashedDay>()
    override fun put(item: TrashedDay) { map[item.dayId] = item }
    override fun list(): List<TrashedDay> = map.values.sortedBy { it.dayId }
    override fun take(dayId: String): TrashedDay? = map.remove(dayId)
    override fun purge(dayId: String) { map.remove(dayId) }
}

/** Audit hook: every recoverable operation emits an event (Master §15/§19). */
fun interface AuditSink { fun record(event: String) }

class TrashManager(
    private val trashStore: TrashStore,
    private val dayLogStore: DayLogStore,
    private val clockMs: () -> Long = System::currentTimeMillis,
    private val audit: AuditSink = AuditSink { },
) {
    /** §18 deletion flow: encrypted copy → protected trash → removed from active store. */
    fun moveToTrash(dayId: String, reason: String, encryptedBlob: ByteArray?, sha16: String?, rawBytes: Int): Boolean {
        if (blobMissing(encryptedBlob, sha16)) return false
        trashStore.put(TrashedDay(dayId, encryptedBlob!!, sha16!!, reason, clockMs(), rawBytes))
        dayLogStore.delete(dayId)
        audit.record("DELETION $dayId → protected trash ($reason)")
        return true
    }

    /** §18 recovery flow: authorize → validate → restore → audit event. */
    fun restore(dayId: String, authorization: Authorization): RestoreResult {
        val ok = when (authorization) {
            is Authorization.UserLocal -> true
            is Authorization.Admin, is Authorization.SuperAdmin -> true
        }
        if (!ok) { audit.record("RECOVERY DENIED $dayId (unauthorized)"); return RestoreResult.DENIED }
        val item = trashStore.take(dayId) ?: run {
            audit.record("RECOVERY FAILED $dayId (not in trash)"); return RestoreResult.NOT_FOUND
        }
        // integrity: caller (app layer) decrypts/decompresses and validates sha16 before re-save;
        // here we only re-insert via the store contract after caller validation.
        audit.record("RECOVERY $dayId restored from trash")
        return RestoreResult.RESTORED(item)
    }

    fun purge(dayId: String, authorization: Authorization): Boolean {
        val allowed = authorization is Authorization.UserLocal || authorization is Authorization.SuperAdmin
        if (!allowed) { audit.record("PURGE DENIED $dayId"); return false }
        trashStore.purge(dayId)
        audit.record("PURGE $dayId permanently destroyed")
        return true
    }

    private fun blobMissing(b: ByteArray?, s: String?) = b == null || s == null
}

sealed class RestoreResult {
    data object DENIED : RestoreResult()
    data object NOT_FOUND : RestoreResult()
    data class RESTORED(val item: TrashedDay) : RestoreResult()
}
