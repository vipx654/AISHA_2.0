package com.aisha.app.data

import com.aisha.core.AishaTask
import com.aisha.core.TaskStore
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking

/** Room-backed TaskStore (Master §9 persistence leg of the §22 contract). */
class RoomTaskStore(private val dao: TaskDao, private val zone: ZoneId = ZoneId.systemDefault()) : TaskStore {

    override fun save(task: AishaTask) = runBlocking {
        dao.upsert(TaskEntity(task.id, task.title, toMs(task.createdAt), task.dueAt?.let { toMs(it) },
            task.completed, task.completedAt?.let { toMs(it) }, task.dayLogId))
    }

    override fun byId(id: String): AishaTask? = runBlocking { dao.byId(id)?.toDomain() }
    override fun all(): List<AishaTask> = runBlocking { dao.all().map { it.toDomain() } }
    override fun delete(id: String) = runBlocking { dao.delete(id) }

    private fun TaskEntity.toDomain() = AishaTask(
        id = id, title = title, createdAt = fromMs(createdAtMs),
        dueAt = dueAtMs?.let { fromMs(it) }, completed = completed,
        completedAt = completedAtMs?.let { fromMs(it) }, dayLogId = dayLogId)

    private fun toMs(dt: LocalDateTime): Long = dt.atZone(zone).toInstant().toEpochMilli()
    private fun fromMs(ms: Long): LocalDateTime = Instant.ofEpochMilli(ms).atZone(zone).toLocalDateTime()
}
