package com.aisha.app.security

import com.aisha.core.Authorization

/**
 * LOCKED §14 — Key management beyond the data key (KeystoreCrypto): audit trail,
 * restricted operations. Audit never duplicates private conversation content (§19).
 */
interface KeyManager {
    fun dataKeyHandle(): String          // never expose raw key material
    fun rotateKeys(authorization: Authorization)
}

interface AuditManager {
    fun record(event: AuditEvent): String
    fun recent(limit: Int): List<AuditEvent>
}

data class AuditEvent(val atMs: Long, val type: String, val actor: String, val detail: String)
