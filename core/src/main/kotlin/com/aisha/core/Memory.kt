package com.aisha.core

import java.time.LocalDateTime

/**
 * LOCKED spec §6 — Memory system: Working Memory, Day Log Manager, Recall Engine.
 * Lifecycle: create at day start → collect → finalize (summary → integrity →
 * compression → ENCRYPTION → local store → sync queue) → next day.
 * Encryption/compression live behind [DayLogStore] (Android: AES + Keystore).
 */
class WorkingMemory(private val capacity: Int = 20) {
    private val buf = ArrayDeque<ChatMessage>(capacity)
    fun push(m: ChatMessage) {
        if (buf.size >= capacity) buf.removeFirst()
        buf.addLast(m)
    }
    fun recent(n: Int = 8): List<ChatMessage> = buf.takeLast(n)
    fun clear() = buf.clear()
}

/** Full day record (spec §6 structure). Storage layer encrypts/serializes this. */
data class DayLogData(
    val dayId: String,                       // "2026-09-21"
    var summary: String? = null,
    val conversations: MutableList<ChatMessage> = mutableListOf(),
    val events: MutableList<DayEvent> = mutableListOf(),
    val moodTimeline: MutableList<MoodPoint> = mutableListOf(),
    val bondTimeline: MutableList<BondPoint> = mutableListOf(),
    val tasks: MutableList<TaskEntry> = mutableListOf(),
    var status: DayLogStatus = DayLogStatus.OPEN,
)

data class DayEvent(val at: LocalDateTime, val type: String, val description: String)
data class MoodPoint(val at: LocalDateTime, val mood: MoodState)
data class BondPoint(val at: LocalDateTime, val score: Float, val stage: String, val trust: Float)
data class TaskEntry(val at: LocalDateTime, val title: String, val done: Boolean = false)

enum class DayLogStatus { OPEN, FINALIZED, SYNC_QUEUED, SYNCED }

/** Result of persisting one finalized day (integrity-first, spec §6/§21). */
data class StoredDayStats(val dayId: String, val rawBytes: Int, val storedBytes: Int, val sha256_16: String)

/**
 * Storage boundary (spec §14/§17). Implementations MUST: compress where useful,
 * encrypt (Android: AES/GCM via Keystore), integrity-tag, persist, enqueue sync.
 * Core ships [InMemoryDayLogStore] for tests; app ships Room+Keystore impl.
 */
interface DayLogStore {
    fun save(day: DayLogData): StoredDayStats
    fun load(dayId: String): DayLogData?
    fun listIds(): List<String>
}

/** Test/memory-only store. Production app NEVER uses this for persistent data. */
class InMemoryDayLogStore : DayLogStore {
    private val map = linkedMapOf<String, DayLogData>()
    override fun save(day: DayLogData): StoredDayStats {
        map[day.dayId] = day
        val raw = day.conversations.sumOf { it.text.length } + day.events.sumOf { it.description.length }
        return StoredDayStats(day.dayId, raw, raw, "mem")
    }
    override fun load(dayId: String): DayLogData? = map[dayId]
    override fun listIds(): List<String> = map.keys.sorted()
}

class DayLogManager(private val store: DayLogStore, private val clock: Clock) {
    var openLog: DayLogData? = null; private set

    fun ensureToday(dayId: String = clock.now().toLocalDate().toString()): DayLogData {
        if (openLog == null || openLog!!.dayId != dayId) openLog = DayLogData(dayId)
        return openLog!!
    }

    fun recordConversation(m: ChatMessage) { openLog!!.conversations += m }
    fun recordMoodPoint(mood: MoodState) { openLog!!.moodTimeline += MoodPoint(clock.now(), mood) }
    fun recordBondPoint(bond: BondState) {
        openLog!!.bondTimeline += BondPoint(clock.now(), bond.totalScore, bond.stage.label, bond.trust)
    }
    fun recordEvent(type: String, description: String) { openLog!!.events += DayEvent(clock.now(), type, description) }
    fun recordTask(title: String, done: Boolean = false) { openLog!!.tasks += TaskEntry(clock.now(), title, done) }

    /** Midnight path: summary → store (compress+encrypt+tag+sync-queue inside store). */
    fun finalizeDay(): StoredDayStats? {
        val log = openLog ?: return null
        if (log.status != DayLogStatus.OPEN) return null
        val avgHappy = if (log.moodTimeline.isEmpty()) 0f
            else log.moodTimeline.map { it.mood.happiness }.average().toFloat()
        val stages = log.bondTimeline.map { it.stage }
        val happyStr = "%.2f".format(avgHappy)
        log.summary = "${log.conversations.size} messages, ${log.events.size} events; " +
            "avg happiness $happyStr; bond ${stages.firstOrNull() ?: "n/a"} → ${stages.lastOrNull() ?: "n/a"}; " +
            "tasks ${log.tasks.count { it.done }}/${log.tasks.size} done."
        log.status = DayLogStatus.FINALIZED
        val stats = store.save(log)
        openLog = null
        return stats
    }
}

/**
 * LOCKED spec §6 — Recall Engine: retrieve ONLY relevant records; NEVER invent.
 */
class RecallEngine(private val store: DayLogStore) {
    fun recall(query: String, limit: Int = 5): List<MemoryHit> {
        val terms = query.split(" ").filter { it.length > 2 }.map { it.lowercase() }
        val hits = mutableListOf<MemoryHit>()
        for (dayId in store.listIds()) {
            val day = store.load(dayId) ?: continue
            for (c in day.conversations) {
                val score = terms.count { c.text.lowercase().contains(it) }.toFloat()
                if (score > 0) hits += MemoryHit(dayId, "${c.role.name.lowercase()}: ${c.text.take(110)}", score)
            }
            for (e in day.events) {
                val score = terms.count { (e.description + " " + e.type).lowercase().contains(it) } + 0.5f
                if (score > 0.5f) hits += MemoryHit(dayId, "[${e.type}] ${e.description.take(110)}", score)
            }
        }
        return hits.sortedByDescending { it.relevance }.take(limit)
    }

    fun recallByDate(dateId: String): List<MemoryHit> {
        val day = store.load(dateId) ?: return emptyList()
        return day.conversations.map { MemoryHit(dateId, "${it.role.name.lowercase()}: ${it.text.take(110)}", 1f) }
    }
}
