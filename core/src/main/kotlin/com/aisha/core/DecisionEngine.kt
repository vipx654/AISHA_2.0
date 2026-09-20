package com.aisha.core

/**
 * LOCKED spec §4 — Decision Engine. Central behaviour coordinator: whether to
 * speak, tone/length, memory use, goal. The LLM never makes these decisions.
 * Table validated by prototype runs.
 */
object DecisionEngine {
    fun decide(inp: ProcessedInput, mood: MoodState, bond: BondState, ambient: Boolean = false): Decision {
        return when (inp.intentHint) {
            "command" -> Decision(true, ResponseGoal.INFORM, "practical", "short", false, "user issued a command")
            "question" -> Decision(true, ResponseGoal.ANSWER, "warm", "short-to-medium", inp.mentionsPast,
                "user asked a question" + if (inp.mentionsPast) " about the past" else "")
            "greeting" -> {
                val playful = mood.happiness > 0.45f || mood.affection > 0.55f
                Decision(true, if (playful) ResponseGoal.PLAYFUL else ResponseGoal.INFORM,
                    if (playful) "bright" else "soft", "one line", false, "greeting exchange")
            }
            "emotional" -> Decision(true,
                if (inp.emotionalValence < 0) ResponseGoal.SUPPORT else ResponseGoal.PLAYFUL,
                "soothing", "medium", false, "user shared feelings — support first")
            "task" -> Decision(true, ResponseGoal.INFORM, "crisp", "short", false, "task-related request")
            else ->
                if (ambient) Decision(false, ResponseGoal.STAY_QUIET, "quiet", "none", false,
                    "ambient mode — presence stays quiet unless meaningful")
                else Decision(true, ResponseGoal.LISTEN, "attentive", "short", false, "light chat — listen and mirror")
        }
    }
}

/** LOCKED spec §9 — ambient/proactive behaviour policy. User settings win. */
data class AmbientPolicy(
    val maxProactivePerDay: Int = 2,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
    val respectBatterySaver: Boolean = true,
)
