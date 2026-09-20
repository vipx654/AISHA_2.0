package com.aisha.app.data

import com.aisha.core.TrashStore
import com.aisha.core.TrashedDay
import kotlinx.coroutines.runBlocking

/** §18 — Room-backed protected trash (blobs stay encrypted at rest, §14). */
class RoomTrashStore(private val dao: TrashDao) : TrashStore {
    override fun put(item: TrashedDay) = runBlocking {
        dao.upsert(TrashEntity(item.dayId, item.blob, item.sha16, item.reason, item.deletedAtMs, item.rawBytes))
    }
    override fun list(): List<TrashedDay> = runBlocking {
        dao.all().map { TrashedDay(it.dayId, it.blob, it.sha16, it.reason, it.deletedAtMs, it.rawBytes) }
    }
    override fun take(dayId: String): TrashedDay? = runBlocking {
        dao.byDay(dayId)?.let { dao.delete(it.dayId); TrashedDay(it.dayId, it.blob, it.sha16, it.reason, it.deletedAtMs, it.rawBytes) }
    }
    override fun purge(dayId: String) = runBlocking { dao.delete(dayId) }
}
