package com.aisha.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aisha.app.AishaApplication
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * LOCKED §16/§20 — Day Finalization Service. Runs at midnight: finalize the open
 * Day Log (summary → integrity → compress → encrypt → store → queue) and start the
 * next day container. Self-reschedules; boot receiver re-arms after restart (§21).
 */
class DayFinalizationWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val container = (applicationContext as AishaApplication).container
            val stats = container.core.finalizeDay()
            ServiceScheduler.scheduleNextMidnight(applicationContext)   // keep the chain alive
            Result.success(if (stats != null) workDataOf("finalizedDay" to stats.dayId) else workDataOf())
        } catch (e: Exception) {
            Result.retry()   // §21: transient failure → retry, state preserved
        }
    }

    private fun workDataOf(vararg pairs: Pair<String, String>) =
        androidx.work.Data.Builder().putAll(mapOf(*pairs)).build()
}

object ServiceScheduler {
    private const val FINALIZER = "aisha_day_finalizer"

    fun scheduleNextMidnight(context: Context) {
        val now = LocalDateTime.now()
        val next = if (now.toLocalTime().isAfter(LocalTime.MIDNIGHT)) now.plusDays(1).withHour(0).withMinute(1)
        else now.withHour(0).withMinute(1)
        val delay = Duration.between(now, next).coerceAtLeast(Duration.ofMinutes(1))
        val request = OneTimeWorkRequestBuilder<DayFinalizationWorker>()
            .setInitialDelay(delay)
            .addTag("aisha-services")
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(FINALIZER, ExistingWorkPolicy.REPLACE, request)
    }
}

/** §21 — phone restart must recover services: re-arm the midnight chain. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ServiceScheduler.scheduleNextMidnight(context)
        }
    }
}
