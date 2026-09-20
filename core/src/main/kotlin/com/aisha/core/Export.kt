package com.aisha.core

/**
 * LOCKED §17 — Export: human-readable summaries AND encrypted archives, clearly
 * labelled. This builder is pure; the app layer composes: bytes → gzip →
 * EncryptionService → file, and labels output ENCRYPTED vs READABLE.
 */
object ExportBuilder {

    fun buildReadable(days: List<DayLogData>): String {
        val sb = StringBuilder()
        sb.appendLine("AISHA DATA EXPORT — READABLE SUMMARY")
        sb.appendLine("days: ${days.size} | range: ${days.minOfOrNull { it.dayId } ?: "-"} .. ${days.maxOfOrNull { it.dayId } ?: "-"}")
        sb.appendLine("=".repeat(60))
        for (d in days.sortedBy { it.dayId }) {
            sb.appendLine()
            sb.appendLine("DAY ${d.dayId}  [${d.status}]")
            sb.appendLine("summary: ${d.summary ?: "(none)"}")
            if (d.events.isNotEmpty()) {
                sb.appendLine("events:")
                d.events.forEach { sb.appendLine("  - [${it.importance}] ${it.type}: ${it.description}") }
            }
            if (d.conversations.isNotEmpty()) {
                sb.appendLine("conversation (${d.conversations.size} messages):")
                d.conversations.forEach { sb.appendLine("  ${it.role.name.lowercase()}: ${it.text}") }
            }
        }
        return sb.toString()
    }

    /** Stable, versioned JSON payload — app layer compresses + encrypts this. */
    fun buildArchiveJson(days: List<DayLogData>): String {
        val esc = { s: String -> s.replace("\\", "\\\\").replace("\"", "\\\"") }
        val parts = days.sortedBy { it.dayId }.map { d ->
            val convs = d.conversations.joinToString(",") {
                "{\"role\":\"${it.role}\",\"text\":\"${esc(it.text)}\",\"at\":\"${it.at}\"}"
            }
            val evts = d.events.joinToString(",") {
                "{\"type\":\"${it.type}\",\"desc\":\"${esc(it.description)}\",\"imp\":\"${it.importance}\",\"at\":\"${it.at}\"}"
            }
            "{\"dayId\":\"${d.dayId}\",\"summary\":\"${esc(d.summary ?: "")}\",\"conversations\":[$convs],\"events\":[$evts]}"
        }
        return "{\"format\":\"AISHA_EXPORT_V1\",\"days\":[$parts]}"
    }
}
