package com.aisha.core

import java.time.LocalDateTime
import java.util.UUID

/**
 * LOCKED Master §9 Task Manager — authoritative owner of task state (Master §21).
 * Create/edit/complete → validate → expose status to planner/reminder/day log.
 * Pure logic; persistence via [TaskStore], reminders via [ReminderScheduler].
 */
data class AishaTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: LocalDateTime,
    val dueAt: LocalDateTime? = null,
    val completed: Boolean = false,
    val completedAt: LocalDateTime? = null,
    val dayLogId: String? = null,
)

class TaskValidationException(message: String) : IllegalArgumentException(message)

object TaskValidator {
    /** Workflow §9: validation before save. Blank titles and nonsensical dues rejected. */
    fun validate(title: String, dueAt: LocalDateTime?, now: LocalDateTime) {
        if (title.isBlank()) throw TaskValidationException("task title cannot be empty")
        if (title.length > 200) throw TaskValidationException("task title too long")
        if (dueAt != null && dueAt.isBefore(now.minusMinutes(1))) {
            throw TaskValidationException("reminder time is in the past")
        }
    }
}

interface TaskStore {
    fun save(task: AishaTask)
    fun byId(id: String): AishaTask?
    fun all(): List<AishaTask>
    fun delete(id: String)
}

/** Reminder scheduling contract; Android binds WorkManager impl (phase 3). */
interface ReminderScheduler {
    fun schedule(task: AishaTask)
    fun cancel(taskId: String)
}

/** No-op scheduler used until the Android WorkManager impl binds (honest, §25). */
object NoopReminderScheduler : ReminderScheduler {
    override fun schedule(task: AishaTask) {}
    override fun cancel(taskId: String) {}
}

class TaskEngine(
    private val store: TaskStore,
    private val clock: Clock,
    private val scheduler: ReminderScheduler = NoopReminderScheduler,
    private val onEvent: (String, AishaTask) -> Unit = { _, _ -> },     // → Day Log recorder
) {

    fun create(title: String, dueAt: LocalDateTime? = null): AishaTask {
        val now = clock.now()
        TaskValidator.validate(title, dueAt, now)
        val task = AishaTask(title = title.trim(), createdAt = now, dueAt = dueAt,
            dayLogId = now.toLocalDate().toString())
        store.save(task)
        if (dueAt != null) scheduler.schedule(task)
        onEvent("TASK_CREATED", task)
        return task
    }

    fun edit(taskId: String, newTitle: String? = null, newDueAt: LocalDateTime? = null): AishaTask {
        val task = store.byId(taskId) ?: throw NoSuchElementException("task not found: $taskId")
        check(!task.completed) { "completed tasks are immutable (audit integrity)" }
        val now = clock.now()
        TaskValidator.validate(newTitle ?: task.title, newDueAt ?: task.dueAt, now)
        val edited = task.copy(title = (newTitle ?: task.title).trim(), dueAt = newDueAt ?: task.dueAt)
        store.save(edited)
        if (newDueAt != null) scheduler.schedule(edited)
        onEvent("TASK_EDITED", edited)
        return edited
    }

    fun complete(taskId: String): AishaTask {
        val task = store.byId(taskId) ?: throw NoSuchElementException("task not found: $taskId")
        if (task.completed) return task                      // idempotent (§22 error handling)
        val done = task.copy(completed = true, completedAt = clock.now())
        store.save(done)
        scheduler.cancel(taskId)
        onEvent("TASK_COMPLETED", done)
        return done
    }

    fun delete(taskId: String) {
        store.byId(taskId) ?: return
        scheduler.cancel(taskId)
        store.delete(taskId)
        onEvent("TASK_DELETED", AishaTask(title = "(deleted)", createdAt = clock.now(), id = taskId))
    }

    fun pending(): List<AishaTask> = store.all().filter { !it.completed }.sortedBy { it.dueAt ?: LocalDateTime.MAX }
    fun all(): List<AishaTask> = store.all().sortedBy { it.createdAt }
    fun forDay(dayId: String): List<AishaTask> = store.all().filter { it.dayLogId == dayId }
    fun dueBetween(from: LocalDateTime, to: LocalDateTime): List<AishaTask> =
        store.all().filter { !it.completed && it.dueAt != null && !it.dueAt!!.isBefore(from) && !it.dueAt!!.isAfter(to) }
}
