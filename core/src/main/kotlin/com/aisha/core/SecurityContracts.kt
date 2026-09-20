package com.aisha.core

/**
 * LOCKED spec §14 — Encryption contract. Data protection flow:
 * raw → compress where applicable → ENCRYPT → storage/upload.
 * Implementations must protect conversations, day logs, audio, backups, exports.
 */
interface EncryptionService {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(blob: ByteArray): ByteArray
}

/** LOCKED spec §19 — privileged roles; enforced server-side for admin tiers. */
sealed class Authorization {
    data object UserLocal : Authorization()
    data class Admin(val token: String) : Authorization()
    data class SuperAdmin(val token: String) : Authorization()
}
