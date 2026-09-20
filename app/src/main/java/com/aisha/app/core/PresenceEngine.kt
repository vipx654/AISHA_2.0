package com.aisha.app.core

/**
 * LOCKED §4 + §9 — Presence Engine.
 * Ambient presence, context-sensitive greetings, return-after-absence behaviour,
 * silence/attention management. Non-intrusive; user notification settings win.
 */
interface PresenceEngine {
    fun onAppOpen(context: PresenceContext): Greeting?

    fun onReturnAfterAbsence(context: PresenceContext): Greeting?

    /** Quiet when inactive — only ambient avatar presence, no chatty notifications. */
    fun ambientBehaviourPolicy(): AmbientPolicy
}

data class Greeting(val text: String, val tone: String)

data class AmbientPolicy(
    val maxProactiveMessagesPerDay: Int,
    val quietHoursStart: Int,     // 0..23
    val quietHoursEnd: Int,
    val respectBatterySaver: Boolean,
)
