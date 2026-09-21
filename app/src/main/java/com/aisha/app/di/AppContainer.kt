package com.aisha.app.di

import android.content.Context
import androidx.room.Room
import com.aisha.app.ai.GeminiLanguageModel
import com.aisha.app.data.AishaDatabase
import com.aisha.app.data.RoomDayLogStore
import com.aisha.app.data.RoomTrashStore
import com.aisha.app.memory.AndroidExportManager
import com.aisha.app.security.KeystoreCrypto
import com.aisha.core.AishaCoreEngine
import com.aisha.core.Clock
import com.aisha.core.LanguageModel
import com.aisha.core.MockLanguageModel
import com.aisha.core.TrashManager
import java.time.LocalDateTime

/**
 * Manual dependency graph (spec §1: clear module responsibilities and interfaces).
 * Production app composition root — swaps Gemini for Mock when no key is set,
 * which is also the offline/degraded fallback (spec §21).
 */
class AppContainer(context: Context, geminiApiKey: String?) {

    val clock: Clock = Clock { LocalDateTime.now() }

    val crypto = KeystoreCrypto()

    val db = Room.databaseBuilder(context, AishaDatabase::class.java, AishaDatabase.NAME)
        .addMigrations(AishaDatabase.MIGRATION_1_2)              // §19: explicit, data-preserving
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()

    val dayLogStore = RoomDayLogStore(db.dayLogDao(), crypto)

    /** §18 — protected trash lifecycle with audit trail. */
    val trashManager = TrashManagerLauncher(context, dayLogStore, crypto, db.trashDao())

    /** §17 — readable + encrypted exports. */
    val exporter = AndroidExportManager(context, dayLogStore, crypto)

    /** Master §9 — Task Manager (authoritative task state owner) + WorkManager reminders. */
    val taskEngine = com.aisha.core.TaskEngine(
        store = com.aisha.app.data.RoomTaskStore(db.taskDao()),
        clock = clock,
        scheduler = com.aisha.app.services.WorkManagerReminderScheduler(context),
        onEvent = { type, task ->
            android.util.Log.i("AISHA_TASK", "$type ${task.id} ${task.title.take(40)}")
        })

    val languageModel: LanguageModel =
        geminiApiKey?.takeIf { it.isNotBlank() }?.let { GeminiLanguageModel(it) } ?: MockLanguageModel()

    val core = AishaCoreEngine(
        store = dayLogStore,
        languageModel = languageModel,
        clock = clock,
    )
}


/**
 * §18 — app-side bridge for the pure [TrashManager]: encrypts the payload before
 * it enters the protected trash store, and validates integrity on restore.
 */
class TrashManagerLauncher(
    private val context: Context,
    private val dayLogStore: com.aisha.core.DayLogStore,
    private val crypto: com.aisha.core.EncryptionService,
    trashDao: com.aisha.app.data.TrashDao,
) {
    private val trashStore = RoomTrashStore(trashDao)
    private val core = TrashManager(trashStore, dayLogStore,
        audit = { android.util.Log.i("AISHA_AUDIT", it) })

    suspend fun moveToTrashEncrypted(dayId: String, reason: String): Boolean {
        val entity = db().dayLogDao().byDay(dayId) ?: return false
        return core.moveToTrash(dayId, reason, entity.blob, entity.sha16, entity.rawBytes)
    }

    suspend fun restore(dayId: String): Boolean {
        val result = core.restore(dayId, com.aisha.core.Authorization.UserLocal)
        val item = (result as? com.aisha.core.RestoreResult.RESTORED)?.item ?: return false
        return try {
            val json = String(crypto.decrypt(item.blob), Charsets.UTF_8)  // decrypt validates GCM tag
            check(json.contains("\"dayId\":\"$dayId\"")) { "integrity: payload mismatch" }
            db().dayLogDao().upsert(
                com.aisha.app.data.DayLogEntity(dayId, item.blob, item.sha16,
                    item.rawBytes, item.rawBytes, "FINALIZED", item.deletedAtMs))
            true
        } catch (e: Exception) {
            android.util.Log.e("AISHA_AUDIT", "RECOVERY rejected $dayId: ${e.message}")
            false
        }
    }

    private fun db(): AishaDatabase =
        (context as? android.app.Application)?.let { (it as com.aisha.app.AishaApplication).container.db }
            ?: throw IllegalStateException("application context required")
}
