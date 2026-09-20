package com.aisha.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

/**
 * LOCKED spec §6/§17 — one row per finalized day. The blob column is
 * gzip → AES-GCM (Keystore) of the whole DayLogData payload. sha16 is the
 * integrity tag validated before any restore (spec §21: never blind-restore).
 * Cloud sync state lives here: pending rows are the upload queue (§16).
 */
@Entity(tableName = "day_logs")
data class DayLogEntity(
    @PrimaryKey val dayId: String,       // "2026-09-21"
    val blob: ByteArray,                 // compressed + encrypted payload
    val sha16: String,                   // integrity tag (of compressed bytes)
    val rawBytes: Int,
    val storedBytes: Int,
    val status: String,                  // FINALIZED / SYNC_QUEUED / SYNCED
    val updatedAtMs: Long,
)

@Dao
interface DayLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: DayLogEntity)

    @Query("SELECT * FROM day_logs WHERE dayId = :dayId")
    suspend fun byDay(dayId: String): DayLogEntity?

    @Query("SELECT dayId FROM day_logs ORDER BY dayId ASC")
    suspend fun allDayIds(): List<String>

    @Query("SELECT * FROM day_logs WHERE status != 'SYNCED' ORDER BY dayId ASC")
    suspend fun pendingSync(): List<DayLogEntity>

    @Query("UPDATE day_logs SET status = :status WHERE dayId = :dayId")
    suspend fun setStatus(dayId: String, status: String)

    @Query("DELETE FROM day_logs WHERE dayId = :dayId")
    suspend fun delete(dayId: String)
}

/** LOCKED §18 — protected trash: encrypted blobs, not user-browsable, authorized recovery. */
@Entity(tableName = "trash")
data class TrashEntity(
    @PrimaryKey val dayId: String,
    val blob: ByteArray,
    val sha16: String,
    val reason: String,
    val deletedAtMs: Long,
    val rawBytes: Int,
)

@Dao
interface TrashDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TrashEntity)

    @Query("SELECT * FROM trash ORDER BY dayId ASC")
    suspend fun all(): List<TrashEntity>

    @Query("SELECT * FROM trash WHERE dayId = :dayId")
    suspend fun byDay(dayId: String): TrashEntity?

    @Query("DELETE FROM trash WHERE dayId = :dayId")
    suspend fun delete(dayId: String)
}

@Database(entities = [DayLogEntity::class, TrashEntity::class], version = 2, exportSchema = true)
abstract class AishaDatabase : RoomDatabase() {
    abstract fun dayLogDao(): DayLogDao
    abstract fun trashDao(): TrashDao

    companion object {
        const val NAME = "aisha.db"

        /** §19/§21 — app updates must preserve user data: explicit migration, no data loss. */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `trash` (" +
                        "`dayId` TEXT NOT NULL PRIMARY KEY, " +
                        "`blob` BLOB NOT NULL, `sha16` TEXT NOT NULL, `reason` TEXT NOT NULL, " +
                        "`deletedAtMs` INTEGER NOT NULL, `rawBytes` INTEGER NOT NULL)"
                )
            }
        }
    }
}
