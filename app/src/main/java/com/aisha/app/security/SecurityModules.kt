package com.aisha.app.security

/**
 * LOCKED §14 — Key Management. Device key material protected by platform
 * secure storage (Android Keystore); separate user-data key material;
 * rotation-capable; high-risk operations restricted.
 */
interface KeyManager {
    fun dataKeyHandle(): String        // never expose raw key material
    fun rotateKeys(authorization: Authorization)
}

/**
 * LOCKED §14 — Encryption. Data protection flow: raw data → compress where
 * applicable → ENCRYPT → storage/upload. Protects conversations, day logs,
 * audio, media, backups, exports.
 */
interface EncryptionService {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(cipher: ByteArray): ByteArray
}

/** Privileged roles per §15. Enforced server-side for admin/super-admin. Client is NOT trusted. */
sealed class Authorization {
    object UserLocal : Authorization()
    data class Admin(val token: String) : Authorization()
    data class SuperAdmin(val token: String) : Authorization()
}

/**
 * LOCKED §19 — Audit Manager. Admin logins, authorization failures, recovery,
 * security changes, sensitive exports, security events. Records accountability
 * metadata WITHOUT duplicating private conversation content.
 */
interface AuditManager {
    fun record(event: AuditEvent): String
    fun recent(limit: Int): List<AuditEvent>
}

data class AuditEvent(val atMs: Long, val type: String, val actor: String, val detail: String)
