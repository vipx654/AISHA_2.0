package com.aisha.app.data

import com.aisha.core.BondPoint
import com.aisha.core.ChatMessage
import com.aisha.core.DayEvent
import com.aisha.core.DayLogData
import com.aisha.core.DayLogStatus
import com.aisha.core.DayLogStore
import com.aisha.core.EncryptionService
import com.aisha.core.MoodPoint
import com.aisha.core.MoodState
import com.aisha.core.Role
import com.aisha.core.StoredDayStats
import com.aisha.core.TaskEntry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * LOCKED spec §6/§14/§17 — production DayLogStore:
 * serialize → gzip (spec §17: compress BEFORE encryption) → AES-GCM (Keystore)
 → Room (encrypted blob + integrity tag) → sync status (§16 upload queue).
 */
class RoomDayLogStore(
    private val dao: DayLogDao,
    private val crypto: EncryptionService,
    private val clock: () -> Long = System::currentTimeMillis,
) : DayLogStore {

    override fun save(day: DayLogData): StoredDayStats {
        val raw = toJson(day).toString().toByteArray(Charsets.UTF_8)
        val compressed = gzip(raw)
        val sha16 = sha256(compressed).take(16)
        val blob = crypto.encrypt(compressed)
        val entity = DayLogEntity(
            dayId = day.dayId, blob = blob, sha16 = sha16,
            rawBytes = raw.size, storedBytes = blob.size,
            status = DayLogStatus.FINALIZED.name, updatedAtMs = clock())
        runBlocking { dao.upsert(entity) }
        return StoredDayStats(day.dayId, raw.size, blob.size, sha16)
    }

    override fun load(dayId: String): DayLogData? {
        val entity = runBlocking { dao.byDay(dayId) } ?: return null
        val compressed = try { crypto.decrypt(entity.blob) } catch (e: Exception) {
            throw IllegalStateException("integrity failure — refusing blind restore (spec §21)", e)
        }
        check(sha256(compressed).take(16) == entity.sha16) { "integrity tag mismatch for $dayId" }
        return fromJson(JSONObject(String(gunzip(compressed), Charsets.UTF_8)))
    }

    override fun listIds(): List<String> = runBlocking { dao.allDayIds() }

    private fun sha256(b: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    private fun gzip(b: ByteArray): ByteArray = ByteArrayOutputStream().also { out ->
        GZIPOutputStream(out).use { it.write(b) }
    }.toByteArray()

    private fun gunzip(b: ByteArray): ByteArray = GZIPInputStream(ByteArrayInputStream(b)).readBytes()

    // ---- JSON (org.json, Android built-in) — manual but explicit and versioned ----
    private fun toJson(d: DayLogData) = JSONObject().apply {
        put("dayId", d.dayId); put("summary", d.summary ?: ""); put("status", d.status.name)
        put("conversations", JSONArray().apply {
            d.conversations.forEach {
                put(JSONObject().put("role", it.role.name).put("text", it.text)
                    .put("at", it.at.toString()).put("lang", it.language))
            }
        })
        put("events", JSONArray().apply {
            d.events.forEach { put(JSONObject().put("type", it.type).put("desc", it.description).put("at", it.at.toString())) }
        })
        put("mood", JSONArray().apply {
            d.moodTimeline.forEach { p -> put(JSONObject().put("at", p.at.toString()).put("m", JSONObject(
                mapOf("h" to p.mood.happiness, "c" to p.mood.calmness, "e" to p.mood.energy,
                    "a" to p.mood.affection, "u" to p.mood.curiosity)))) }
        })
        put("bond", JSONArray().apply {
            d.bondTimeline.forEach { put(JSONObject().put("at", it.at.toString())
                .put("s", it.score).put("st", it.stage).put("t", it.trust)) }
        })
        put("tasks", JSONArray().apply {
            d.tasks.forEach { put(JSONObject().put("title", it.title).put("done", it.done).put("at", it.at.toString())) }
        })
    }

    private fun fromJson(o: JSONObject): DayLogData {
        val day = DayLogData(o.getString("dayId"), summary = o.optString("summary").ifEmpty { null },
            status = runCatching { DayLogStatus.valueOf(o.getString("status")) }.getOrDefault(DayLogStatus.FINALIZED))
        val convs = o.optJSONArray("conversations") ?: JSONArray()
        for (i in 0 until convs.length()) {
            val c = convs.getJSONObject(i)
            day.conversations += ChatMessage(Role.valueOf(c.getString("role")), c.getString("text"),
                LocalDateTime.parse(c.getString("at")), c.optString("lang", "en"))
        }
        val evts = o.optJSONArray("events") ?: JSONArray()
        for (i in 0 until evts.length()) {
            val e = evts.getJSONObject(i)
            day.events += DayEvent(LocalDateTime.parse(e.getString("at")), e.getString("type"), e.getString("desc"))
        }
        val moods = o.optJSONArray("mood") ?: JSONArray()
        for (i in 0 until moods.length()) {
            val m = moods.getJSONObject(i).getJSONObject("m")
            day.moodTimeline += MoodPoint(LocalDateTime.parse(moods.getJSONObject(i).getString("at")),
                MoodState(m.getDouble("h").toFloat(), m.getDouble("c").toFloat(), m.getDouble("e").toFloat(),
                    m.getDouble("a").toFloat(), m.getDouble("u").toFloat()))
        }
        val bonds = o.optJSONArray("bond") ?: JSONArray()
        for (i in 0 until bonds.length()) {
            val b = bonds.getJSONObject(i)
            day.bondTimeline += BondPoint(LocalDateTime.parse(b.getString("at")), b.getDouble("s").toFloat(),
                b.getString("st"), b.getDouble("t").toFloat())
        }
        val tasks = o.optJSONArray("tasks") ?: JSONArray()
        for (i in 0 until tasks.length()) {
            val t = tasks.getJSONObject(i)
            day.tasks += TaskEntry(LocalDateTime.parse(t.getString("at")), t.getString("title"), t.getBoolean("done"))
        }
        return day
    }
}
