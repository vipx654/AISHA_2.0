package com.aisha.app.cloud

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aisha.app.AishaApplication
import com.aisha.app.data.DayLogEntity

/**
 * LOCKED §16 — Sync Service. Processes the upload queue built from Room status.
 * HONEST MODE (Master §25): the real cloud backend is tracked PENDING in
 * docs/MASTER_BUILD_CHECKLIST.md. Until an uploader is configured, this worker
 * validates integrity of pending encrypted objects and leaves them PENDING —
 * it NEVER marks them synced without a confirmed upload, and local data is
 * untouched either way (§16: local source kept until confirmation).
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    interface CloudUploader {
        /** @return true only after server confirms the encrypted object. */
        suspend fun upload(entity: DayLogEntity): Boolean
    }

    override suspend fun doWork(): Result {
        val container = (applicationContext as AishaApplication).container
        val dao = container.db.dayLogDao()
        val uploader: CloudUploader? = null // PENDING: real backend (phase 2+ decision)
        for (entity in dao.pendingSync()) {
            val intact = try {
                container.dayLogStore.load(entity.dayId) != null   // validates integrity tag
            } catch (e: Exception) { false }                       // §21: corrupt → do not upload
            if (intact && uploader != null) {
                if (uploader.upload(entity)) dao.setStatus(entity.dayId, "SYNCED")
            }
            // uploader == null → stays pending by design; retry on next schedule (§21)
        }
        return Result.success()
    }
}
