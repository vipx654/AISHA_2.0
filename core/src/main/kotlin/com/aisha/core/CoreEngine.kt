package com.aisha.core

import java.time.LocalDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * LOCKED spec §2+§5 — Core Engine orchestrator. Pipeline order is LOCKED:
 * input → processing → mood/bond → memory → decision → safety → prompt →
 * language model → validator → reply → day log. Replaceable LLM; graceful
 * degradation (spec §21). Android layer wraps this behind a ViewModel.
 */
class AishaCoreEngine(
    private val store: DayLogStore,
    private var languageModel: LanguageModel,
    val clock: Clock,
) {
    val identity = Identity.CONTEXT
    val mood = MoodEngine(clock) { /* meaningful changes recorded by pipeline */ }
    val bond = BondEngine(clock) { t -> stageEvents += t }
    val working = WorkingMemory()
    val dayLogs = DayLogManager(store, clock)
    val recall = RecallEngine(store)
    private val safety = SafetyRuleEngine()
    private val processor = InputProcessor()

    val stageEvents = ArrayDeque<StageTransition>()
    private val mutex = Mutex()
    private var interactionsToday = 0
    private var lastActiveDay: String? = null
    private var streak = 0

    /** Last structured prompt — exposed for the debug/activity surfaces only. */
    var lastPrompt: AishaPrompt? = null; private set
    var lastDecision: Decision? = null; private set

    suspend fun handleUserTurn(rawInput: String, ambient: Boolean = false): TurnResult = mutex.withLock {
        val log = dayLogs.ensureToday()
        val inp = processor.process(rawInput)

        // 1) mood/bond update — gradual, validated deltas
        val sig = signalFor(inp)
        mood.applyInteraction(sig)
        val quality = if (inp.intentHint == "emotional" || inp.intentHint == "question") 1f else 0.7f
        val kind = when (sig.kind) {
            SignalKind.AFFECTION -> BondEngine.Kind.AFFECTION
            SignalKind.DEEP_CONVERSATION -> BondEngine.Kind.DEEP
            else -> BondEngine.Kind.POSITIVE
        }
        bond.onPositiveInteraction(quality, kind)
        interactionsToday++

        // 2) decision (spec §4 — engine decides, not the LLM)
        val decision = DecisionEngine.decide(inp, mood.current(), bond.state, ambient)
        lastDecision = decision

        // 3) selective memory (spec §6 — only when relevant)
        val memories = if (inp.mentionsPast) recall.recall(rawInput, 3) else emptyList()

        // 4) prompt assembly (locked blocks)
        val prompt = PromptBuilder.build(identity, mood.current(), bond.state, decision,
            memories, working.recent(8), null, inp)
        lastPrompt = prompt
        if (!safety.preValidate(prompt.full()).allowed) {
            return degraded(DegradedReason.VALIDATOR_BLOCKED, inp.language)
        }

        // 5) language model (replaceable; failure → §21 degraded path)
        val reply: String = try {
            languageModel.generate(prompt, inp.language, decision.goal)
        } catch (e: LanguageModelUnavailable) {
            return degraded(DegradedReason.AI_SERVICE_UNAVAILABLE, inp.language)
        }

        // 6) response validation; one regeneration then safe fallback
        val first = safety.postValidate(reply, bond.state.stage, memories)
        val (finalText, violations) = if (first.allowed) {
            (first.adjusted ?: reply) to first.violations.map { it.name }
        } else {
            val retry: String? = try { languageModel.generate(prompt, inp.language, decision.goal) } catch (e: LanguageModelUnavailable) { null }
            val second = retry?.let { safety.postValidate(it, bond.state.stage, memories) }
            when {
                second != null && second.allowed -> (second.adjusted ?: retry!!) to second.violations.map { it.name }
                second != null && second.adjusted != null -> second.adjusted!! to second.violations.map { it.name }
                else -> fallbackReply(inp.language) to (first.violations.map { it.name } + listOf("fallback_used"))
            }
        }

        // 7) reply → day log (spec §5 last step) + safe activity notes (Master §19)
        recordTurn(log.dayId, inp, finalText)
        dayLogs.recordMoodPoint(mood.current())
        dayLogs.recordBondPoint(bond.state)
        val transition = stageEvents.removeFirstOrNull()
        if (transition != null) {
            dayLogs.recordEvent("BOND_STAGE", "relationship state: ${transition.old.label} → ${transition.new.label}",
                Importance.MILESTONE)
        }
        val notes = buildList {
            if (memories.isNotEmpty()) add("checked recent memory")
            if (decision.goal == ResponseGoal.SUPPORT) add("adjusting response tone")
            if (violations.isNotEmpty()) add("applied response safety rules")
            if (transition != null) add("updated relationship state")
            add("saving today's event")
        }
        return TurnResult(finalText, violations, degraded = false,
            language = inp.language, memoriesUsed = memories.size,
            stageTransition = transition, activityNotes = notes)
    }

    /** Midnight/day-boundary path (spec §6/§20 Day Finalization Service). */
    fun finalizeDay(): StoredDayStats? {
        val stats = dayLogs.finalizeDay()
        closeDayBookkeeping()
        mood.tick(1f)
        return stats
    }

    private fun closeDayBookkeeping() {
        val today = clock.now().toLocalDate()
        if (interactionsToday > 0) {
            streak = if (lastActiveDay == today.minusDays(1).toString()) streak + 1 else 1
            bond.onDayComplete(streak)
            lastActiveDay = today.toString()
        } else {
            lastActiveDay?.let { last ->
                val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(last), today).toInt()
                bond.onAbsence(days)
            }
            streak = 0
        }
        interactionsToday = 0
    }

    private fun recordTurn(dayId: String, inp: ProcessedInput, reply: String) {
        val user = ChatMessage(Role.USER, inp.raw, clock.now(), inp.language)
        val aisha = ChatMessage(Role.AISHA, reply, clock.now(), inp.language)
        working.push(user); working.push(aisha)
        dayLogs.ensureToday(dayId)
        dayLogs.recordConversation(user)
        dayLogs.recordConversation(aisha)
    }

    private fun fallbackReply(language: String) =
        if (language == "hi") "मैं इसे इस तरह नहीं कहूँगी — बताइए क्या चाहिए।"
        else "I'd rather not say it that way — let me know what you need."

    /** Spec §21 — preserve state, degrade politely, inform user. */
    private fun degraded(reason: DegradedReason, language: String): TurnResult {
        val text = when (reason) {
            DegradedReason.AI_SERVICE_UNAVAILABLE ->
                if (language == "hi") "भाषा सेवा अभी उपलब्ध नहीं है, पर मैं यहीं हूँ — आपकी बातें सुरक्षित सेव हो रही हैं।"
                else "I can't reach my language service right now, but I'm still here — everything you say is saved safely."
            DegradedReason.VALIDATOR_BLOCKED ->
                if (language == "hi") "दूसरे शब्दों में कहें — सुरक्षा परत ने रोक लगाई।"
                else "Let's rephrase that — my safety layer blocked that path."
            DegradedReason.NETWORK_OFFLINE ->
                if (language == "hi") "ऑफ़लाइन हैं — सब कुछ स्थानीय रूप से सेव हो रहा है।"
                else "We're offline — everything is being saved locally."
        }
        val note = when (reason) {
            DegradedReason.AI_SERVICE_UNAVAILABLE -> "language service offline — using safe local mode"
            DegradedReason.VALIDATOR_BLOCKED -> "applied response safety rules"
            DegradedReason.NETWORK_OFFLINE -> "offline — data stays encrypted on device"
        }
        return TurnResult(text, emptyList(), degraded = true, language = language,
            memoriesUsed = 0, stageTransition = null, activityNotes = listOf(note))
    }
}

/**
 * Master §19 — Safe Activity Log. Notes are fixed operational phrases only
 * ("checking recent memory"), never raw prompts, reasoning or model output.
 */
data class TurnResult(
    val text: String,
    val violations: List<String>,
    val degraded: Boolean,
    val language: String,
    val memoriesUsed: Int,
    val stageTransition: StageTransition?,
    val activityNotes: List<String> = emptyList(),
)

enum class DegradedReason { AI_SERVICE_UNAVAILABLE, NETWORK_OFFLINE, VALIDATOR_BLOCKED }
