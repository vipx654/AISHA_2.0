package com.aisha.core

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * LOCKED Master §5 Presence Engine — greetings, quiet presence, return-after-
 * absence, permitted proactive actions. Rules: user settings win; quiet hours
 * silence proactive behaviour; NO fabricated absence events (Workflow §18) —
 * absence length derives only from real last-active bookkeeping.
 */
class PresenceEngine(
    private val policy: AmbientPolicy = AmbientPolicy(),
    private val maxProactiveSent: () -> Int = { 0 },
) {

    fun onAppOpen(now: LocalDateTime, lastActiveDay: String?): PresenceAction {
        val absentDays = lastActiveDay?.let {
            ChronoUnit.DAYS.between(java.time.LocalDate.parse(it), now.toLocalDate())
        } ?: 0
        if (absentDays <= 1) return PresenceAction.Greeting(greetingForHour(now), toneForHour(now))
        if (absentDays <= 6) return PresenceAction.Greeting(
            "It's been $absentDays days — good to see you again.", "gentle")
        // Long absence: warm, zero-guilt, never invents what happened meanwhile (§18)
        return PresenceAction.Greeting(
            "$absentDays days… I kept everything safe. No pressure — where would you like to start?",
            "warm, zero-guilt")
    }

    fun greetingForHour(now: LocalDateTime): String = when (now.hour) {
        in 5..11 -> "Good morning ☀️"
        in 12..16 -> "Hey, good afternoon"
        in 17..21 -> "Good evening 🌙"
        else -> "Still up? 🌙"
    }

    private fun toneForHour(now: LocalDateTime) = if (now.hour in 5..11) "bright" else "soft"

    /**
     * Proactive gate (Master §9): quiet hours + daily cap + battery saver.
     * Returns null when silence is required — silence is the default.
     */
    fun proactiveCheck(now: LocalDateTime, batterySaver: Boolean): PresenceAction? {
        if (batterySaver && policy.respectBatterySaver) return null
        if (now.hour >= policy.quietStartHour || now.hour < policy.quietEndHour) return null
        if (maxProactiveSent() >= policy.maxProactivePerDay) return null
        return PresenceAction.QuietPresence("thinking of you today 💜")
    }
}

sealed class PresenceAction {
    data class Greeting(val text: String, val tone: String) : PresenceAction()
    data class QuietPresence(val text: String) : PresenceAction()
    data object None : PresenceAction()
}

/** AmbientPolicy lives in DecisionEngine.kt (locked §9 defaults). */
