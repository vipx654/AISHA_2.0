package com.aisha.app.memory

import android.content.Context
import com.aisha.core.DayLogData
import com.aisha.core.DayLogStore
import com.aisha.core.EncryptionService
import com.aisha.core.ExportBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPOutputStream

/**
 * LOCKED §17 — Export: readable summaries + ENCRYPTED archives, clearly labelled.
 * Files land in the app's external exports dir (user-accessible via file manager).
 */
class AndroidExportManager(
    private val context: Context,
    private val store: DayLogStore,
    private val crypto: EncryptionService,
) {
    fun exportReadable(fromDay: String, toDay: String): File {
        val days = loadRange(fromDay, toDay)
        val out = File(exportsDir(), "AISHA_EXPORT_READABLE_${fromDay}_to_${toDay}.txt")
        out.writeText(ExportBuilder.buildReadable(days))
        return out
    }

    /** Layout: "AISHA_ENC_V1\n" header || gzip(json) encrypted with Keystore key. */
    fun exportEncryptedArchive(fromDay: String, toDay: String): File {
        val days = loadRange(fromDay, toDay)
        val payload = ExportBuilder.buildArchiveJson(days).toByteArray(Charsets.UTF_8)
        val compressed = ByteArrayOutputStream().also { o ->
            GZIPOutputStream(o).use { it.write(payload) }
        }.toByteArray()
        val blob = crypto.encrypt(compressed)
        val out = File(exportsDir(), "AISHA_EXPORT_ENCRYPTED_${fromDay}_to_${toDay}.aishaenc")
        out.writeBytes("AISHA_ENC_V1\n".toByteArray(Charsets.US_ASCII) + blob)
        return out
    }

    private fun loadRange(fromDay: String, toDay: String): List<DayLogData> =
        store.listIds().filter { it >= fromDay && it <= toDay }
            .mapNotNull { runCatching { store.load(it) }.getOrNull() }   // §21: corrupt log → skip, never crash

    private fun exportsDir(): File =
        File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
}
