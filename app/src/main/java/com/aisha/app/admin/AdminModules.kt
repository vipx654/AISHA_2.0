package com.aisha.app.admin

/**
 * LOCKED §19 — Admin Dashboard: system monitoring, authorized recovery, audit
 * review, maintenance, operational status. Privileged authorization MUST be
 * enforced server-side; the client is not trusted to grant admin access.
 */
interface AdminDashboardApi {
    fun systemStatus(): Map<String, String>
    fun authorizeAdmin(token: String): Boolean     // server-verified
}

/** LOCKED §19 — Super Admin: security policy, key workflows, disaster recovery. */
interface SuperAdminApi {
    fun configureSecurityPolicy(authorization: com.aisha.app.security.Authorization)
    fun runDisasterRecovery(authorization: com.aisha.app.security.Authorization)
}
