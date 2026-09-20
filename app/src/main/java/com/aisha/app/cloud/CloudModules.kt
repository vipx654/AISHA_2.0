package com.aisha.app.cloud

/**
 * LOCKED §16 — Cloud is BACKUP / RECOVERY / SYNC only. Local-first; optional
 * per user. Flow: Local DB → Encryption Layer → Sync Manager → Upload Queue
 * → Cloud Storage. LOCKED: 40-day cloud retention; cloud expiry never deletes
 * active local data. Conflicts resolved by explicit version/timestamp/merge
 * policy — never silent overwrite.
 */
interface SyncManager {
    fun enqueueUpload(payloadId: String)
    fun enqueueDownload(backupId: String)
    fun pendingCount(): Int
    fun onConnectivityRestored()
}

/** LOCKED §16 — pending encrypted objects, retry on failure, keep local source until confirmed. */
interface UploadQueue {
    fun add(payloadId: String)
    fun processNext(): Boolean
}

/** LOCKED §16 — restore: validate → decrypt locally → rebuild indexes → resume. */
interface DownloadQueue {
    fun add(backupId: String)
    fun processNext(): Boolean
}

/** LOCKED §16/§17 — encrypted backups, retention policy, corrupt backup validation. */
interface BackupManager {
    fun createBackup(): BackupHandle
    fun validateBackup(handle: BackupHandle): Boolean    // never blind-restore
    fun pruneExpiredRetentionPolicy()                    // 40 days, LOCKED
}

data class BackupHandle(val id: String, val createdAtMs: Long, val encryptedBytes: Long)
