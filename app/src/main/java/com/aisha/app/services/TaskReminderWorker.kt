package com.aisha.app.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aisha.app.AishaApplication
import java.time.Duration
import java.time.LocalDateTime

/**
 * LOCKED Master §9 Reminder Engine — reads schedule → checks time/settings →
 * triggers OR QUEUES (quiet hours) → records status to Day Log.
 * Unique work per task id; survives restarts via WorkManager persistence (§21).
 */
class TaskReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK) ?: return Result.failure()
        val container = (applicationContext as AishaApplication).container
        val task = container.taskEngine.let { it.pending().find { t -> t.id == taskId } }
            ?: return Result.success()   // completed/deleted meanwhile — nothing to fire

        val now = LocalDateTime.now()
        val quiet = now.hour >= QUIET_START || now.hour < QUIET_END
        if (quiet) {
            // §9: queue until quiet hours end — never wake the user at night
            scheduleAfter(applicationContext, task.id, minutesUntilQuietEnd(now))
            return Result.success()
        }
        val delivered = AishaNotifier.post(applicationContext, tag = "task_$taskId", id = taskId.hashCode(),
            title = "AISHA reminder", body = task.title)
        container.core.dayLogs.ensureToday()
        container.core.dayLogs.recordEvent(
            if (delivered) "REMINDER_SENT" else "REMINDER_QUEUED",
            "reminder: ${task.title.take(80)}")
        return Result.success()
    }

    private fun minutesUntilQuietEnd(now: LocalDateTime): Long {
        val end = if (now.hour >= QUIET_START) now.plusDays(1).withHour(QUIET_END).withMinute(0)
        else now.withHour(QUIET_END).withMinute(0)
        return Duration.between(now, end).toMinutes().coerceAtLeast(1)
    }

    companion object {
        const val KEY_TASK = "task_id"
        const val QUIET_START = 22
        const val QUIET_END = 7

        fun scheduleAfter(context: Context, taskId: String, delayMinutes: Long) {
            val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                .setInitialDelay(java.time.Duration.ofMinutes(delayMinutes))
                .setInputData(androidx.work.Data.Builder().putString(KEY_TASK, taskId).build())
                .addTag("aisha-reminders")
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("reminder_$taskId", ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context, taskId: String) {
            WorkManager.getInstance(context).cancelUniqueWork("reminder_$taskId")
        }
    }
}

/** Binds core [com.aisha.core.ReminderScheduler] to WorkManager persistence. */
class WorkManagerReminderScheduler(private val context: Context) : com.aisha.core.ReminderScheduler {
    override fun schedule(task: com.aisha.core.AishaTask) {
        val due = task.dueAt ?: return
        val delay = Duration.between(LocalDateTime.now(), due).toMinutes()
        TaskReminderWorker.scheduleAfter(context, task.id, delay.coerceAtLeast(0))
    }
    override fun cancel(taskId: String) = TaskReminderWorker.cancel(context, taskId)
}
