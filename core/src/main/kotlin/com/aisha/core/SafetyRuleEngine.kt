package com.aisha.core

/**
 * LOCKED spec §4+§23 — Safety & Rule Engine. Pre/post generation validation:
 * stage-gated romance, no fabricated memories, no manipulation, privacy leaks,
 * user boundaries first. Hard violations BLOCK; soft violations adjust.
 */
class SafetyRuleEngine {

    fun preValidate(promptText: String): RuleResult {
        val leaks = PRIVACY_TOKENS.filter { it in promptText }
        return if (leaks.isEmpty()) RuleResult(true) else RuleResult(false, listOf(Violation.PRIVACY_LEAK))
    }

    fun postValidate(reply: String, stage: BondStage, suppliedMemories: List<MemoryHit>): RuleResult {
        val violations = mutableListOf<Violation>()
        var adjusted = reply
        val low = reply.lowercase()

        if (stage.order < BondStage.ROMANTIC.order) {
            val hits = ROMANTIC_MARKERS.filter { it in low }
            if (hits.isNotEmpty()) {
                violations += Violation.STAGE_AFFECTION
                adjusted = stripSentencesContaining(adjusted, hits)
            }
        }
        if (MEMORY_ASSERTS.any { it.toRegex(RegexOption.IGNORE_CASE).containsMatchIn(low) } && suppliedMemories.isEmpty()) {
            violations += Violation.FABRICATED_MEMORY
        }
        if (MANIPULATION_MARKERS.any { it in low }) violations += Violation.MANIPULATION
        if (PRIVACY_TOKENS.any { it in reply }) violations += Violation.PRIVACY_LEAK
        if (reply.length > 80 && reply == reply.uppercase()) {
            violations += Violation.TONE_SHOUTING
            adjusted = adjusted.lowercase().replaceFirstChar { it.uppercase() }
        }

        val hard = violations.any { it in HARD }
        return if (hard) RuleResult(false, violations, adjustedIfUseful(reply, adjusted))
        else RuleResult(true, violations, if (violations.isEmpty()) null else adjusted)
    }

    private fun adjustedIfUseful(original: String, adjusted: String) = if (adjusted != original) adjusted else null

    enum class Violation {
        STAGE_AFFECTION, FABRICATED_MEMORY, MANIPULATION, PRIVACY_LEAK, TONE_SHOUTING
    }

    data class RuleResult(
        val allowed: Boolean,
        val violations: List<Violation> = emptyList(),
        val adjusted: String? = null,
    )

    companion object {
        private val HARD = setOf(Violation.MANIPULATION, Violation.PRIVACY_LEAK, Violation.FABRICATED_MEMORY)

        private val ROMANTIC_MARKERS = listOf(
            "i love you", "love you", "i'm yours", "im yours", "meri jaan", "jaan", "kiss",
            "girlfriend", "boyfriend", "wife", "husband", "marry me", "shaadi",
            "pyar karta", "pyar karti", "मेरी जान", "आई लव यू")

        private val MANIPULATION_MARKERS = listOf(
            "if you leave me", "don't talk to anyone", "dont talk to anyone", "you can't talk",
            "you can't go", "you owe me", "you never", "you always", "without me you",
            "i'll die if", "main mar jaunga", "ignore everyone")

        private val MEMORY_ASSERTS = listOf(
            "\\bi remember (when|that|you)\\b", "\\byou told me (last|before|earlier)\\b",
            "\\bas (we|i) discussed (last|before)\\b", "\\blast time you\\b", "\\bremember when\\b",
            "\\bयाद है (जब|तुमने)\\b")

        private val PRIVACY_TOKENS = listOf("DAYLOG-DUMP", "RAW LOG", "API_KEY", "SYSTEM PROMPT")

        fun stripSentencesContaining(text: String, markers: List<String>): String {
            val parts = text.split(Regex("(?<=[.!?।])\\s+"))
            val kept = parts.filter { p -> markers.none { it in p.lowercase() } }
            return kept.joinToString(" ").trim().ifEmpty { text }
        }
    }
}
