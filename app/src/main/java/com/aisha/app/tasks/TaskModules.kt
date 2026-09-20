package com.aisha.app.tasks

import com.aisha.app.model.ChatMessage

data class TaskItem(
    val id: String,
    val title: String,
    val dueAtMs: Long?,
    val completed: Boolean = false,
    val dayLogId: String?,
)

/**
 * LOCKED §10 — Task Manager. Create / edit / complete; associate with Day Log;
 * maintain task state locally (encrypted storage §14).
 */
interface TaskManager {
    fun create(title: String, dueAtMs: Long?): TaskItem
    fun edit(task: TaskItem)
    fun complete(taskId: String)
    fun pending(): List<TaskItem>
}

/** LOCKED §10 — schedule, trigger notification, respect settings, context-aware wording. */
interface ReminderEngine {
    fun schedule(task: TaskItem)
    fun cancel(taskId: String)
}

/** LOCKED §10 — daily organisation; user-controlled plans linked to daily context. */
interface DailyPlanner {
    fun planFor(dayLogId: String): List<TaskItem>
}

/** LOCKED §10 — scheduling/delivery, avoid excessive interruption, offline scheduling. */
interface NotificationManager {
    fun deliver(notification: AishaNotification)
    fun setPolicy(quietHours: com.aisha.app.core.AmbientPolicy)
}

data class AishaNotification(val title: String, val body: String, val atMs: Long, val tag: String)
