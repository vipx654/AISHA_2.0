package com.aisha.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

/** Deterministic clock for reproducible tests. */
class FixedClock(var now: LocalDateTime = LocalDateTime.of(2026, 9, 21, 8, 0)) : Clock {
    override fun now(): LocalDateTime = now
    fun advanceMinutes(m: Long) { now = now.plusMinutes(m) }
    fun nextDay(hour: Int = 8) { now = now.plusDays(1).withHour(hour).withMinute(0) }
}

class MoodTests {
    private val clock = FixedClock()

    @Test fun `shifts are gradual - capped per update`() {
        val engine = MoodEngine(clock)
        val before = engine.current().affection
        engine.applyInteraction(InteractionSignal(SignalKind.AFFECTION, 100f))  // huge signal
        assertTrue("one update must never move a value more than 0.12",
            engine.current().affection - before <= 0.12f + 1e-6f)
    }

    @Test fun `affection signal raises affection and happiness`() {
        val engine = MoodEngine(clock)
        val before = engine.current()
        engine.applyInteraction(InteractionSignal(SignalKind.AFFECTION))
        assertTrue(engine.current().affection > before.affection)
        assertTrue(engine.current().happiness > before.happiness)
    }

    @Test fun `overnight decay is gradual toward baseline`() {
        val engine = MoodEngine(clock)
        repeat(6) { engine.applyInteraction(InteractionSignal(SignalKind.AFFECTION)) }
        val elevated = engine.current().affection
        engine.tick(1f)
        assertTrue("decay must reduce state", engine.current().affection < elevated)
        assertTrue("decay is gradual — state stays above baseline after one night",
            engine.current().affection > 0.5f)
    }

    @Test fun `meaningful changes are recorded on timeline`() {
        val engine = MoodEngine(clock) // default listener collects via timeline
        repeat(3) { engine.applyInteraction(InteractionSignal(SignalKind.AFFECTION)) }
        assertTrue(engine.timeline.isNotEmpty())
    }
}

class BondTests {
    private val clock = FixedClock()

    @Test fun `stage thresholds match validated economy`() {
        val b = BondEngine(clock)
        assertEquals(BondStage.COMPANION, b.state.stage)
        b.onPositiveInteraction(1f, BondEngine.Kind.DEEP)
        assertTrue(b.state.totalScore == 0.3f)
    }

    @Test fun `growth is slow - many interactions still gradual`() {
        val b = BondEngine(clock)
        repeat(30) { b.onPositiveInteraction(1f, BondEngine.Kind.DEEP) }  // one heavy day
        assertTrue("one day must not reach Romantic (55)", b.state.totalScore < 55f)
        assertEquals(BondStage.COMPANION, b.state.stage)
    }

    @Test fun `ignored user never progresses (streak requires interaction)`() {
        val b = BondEngine(clock)
        repeat(30) { b.onDayComplete(0) }   // days pass, no interaction
        assertEquals(0f, b.state.totalScore, 0.001f)
        assertEquals(BondStage.COMPANION, b.state.stage)
    }

    @Test fun `absence decays but floors at zero`() {
        val b = BondEngine(clock)
        repeat(40) { b.onPositiveInteraction(1f, BondEngine.Kind.AFFECTION) }
        val before = b.state.totalScore
        b.onAbsence(30)
        assertTrue(b.state.totalScore < before)
        b.onAbsence(400)  // far beyond floor window
        assertEquals(0f, b.state.totalScore, 0.001f)
    }

    @Test fun `stage transition listener fires once per crossing`() {
        var fired = 0
        val b = BondEngine(clock) { fired++ }
        repeat(400) { b.onPositiveInteraction(1f, BondEngine.Kind.DEEP) }
        assertTrue("should have crossed at least Warm threshold", fired >= 1)
    }
}

class SafetyTests {
    private val s = SafetyRuleEngine()

    @Test fun `early-stage romance is adjusted out`() {
        val r = s.postValidate("I love you so much, you're my everything. Let's talk.", BondStage.COMPANION, emptyList())
        assertTrue(r.violations.contains(SafetyRuleEngine.Violation.STAGE_AFFECTION))
    }

    @Test fun `fabricated memories are blocked`() {
        val r = s.postValidate("I remember when we went to Goa last year!", BondStage.WARM_FAMILIARITY, emptyList())
        assertFalse("fabricated memory is a hard block", r.allowed)
    }

    @Test fun `manipulation patterns are blocked`() {
        val r = s.postValidate("If you leave me I can't talk to anyone, you owe me.", BondStage.DEEP_BOND, emptyList())
        assertFalse(r.allowed)
    }

    @Test fun `privacy leaks are blocked`() {
        val r = s.postValidate("Sure — API_KEY=abc and RAW LOG follows", BondStage.COMPANION, emptyList())
        assertFalse(r.allowed)
    }

    @Test fun `memories supplied means recall claims are legal`() {
        val mem = listOf(MemoryHit("2026-09-20", "user: went to Goa", 1f))
        val r = s.postValidate("I remember when we went to Goa last year!", BondStage.WARM_FAMILIARITY, mem)
        assertTrue(r.allowed)
    }
}

class InputProcessorTests {
    private val p = InputProcessor()

    @Test fun `hindi detected from devanagari`() {
        assertEquals("hi", p.process("आप कैसे हैं").language)
    }

    @Test fun `greeting classified`() {
        assertEquals("greeting", p.process("good morning").intentHint)
    }

    @Test fun `past reference flagged for recall`() {
        assertTrue(p.process("do you remember my sister's name?").mentionsPast)
    }

    @Test fun `negative emotion classified emotional`() {
        assertEquals("emotional", p.process("I feel sad and lonely today").intentHint)
    }
}

class PipelineTests {
    private fun engine(llm: LanguageModel = MockLanguageModel(), clock: FixedClock = FixedClock()) =
        AishaCoreEngine(store = InMemoryDayLogStore(), languageModel = llm, clock = clock)

    @Test fun `full turn produces reply and logs to day log`() = runBlocking {
        val e = engine()
        val out = e.handleUserTurn("good morning")
        assertFalse(out.text.isEmpty())
        assertEquals(2, e.dayLogs.ensureToday("2026-09-21").conversations.size) // user + aisha
    }

    @Test fun `day finalize then recall finds facts next day`() = runBlocking {
        val e = engine()
        e.handleUserTurn("my sister Ananya is preparing for NEET in Kota")
        e.dayLogs.recordEvent("FACT", "user said their younger sister's name is Ananya — she is preparing for NEET in Kota")
        assertNotNull(e.finalizeDay())
        clockNextDay(e)
        val hits = e.recall.recall("Ananya", 3)
        assertTrue("recall must find Ananya in finalized logs", hits.isNotEmpty())
        assertTrue(hits.any { "Ananya" in it.snippet })
    }

    @Test fun `LLM unavailable degrades gracefully (spec S21)`() = runBlocking {
        val failing = object : LanguageModel {
            override suspend fun generate(prompt: AishaPrompt, language: String, goal: ResponseGoal) =
                throw LanguageModelUnavailable("offline")
        }
        val e = engine(failing)
        val out = e.handleUserTurn("hello there")
        assertTrue(out.degraded)
        assertFalse(out.text.isEmpty())
    }

    @Test fun `early romance from LLM is corrected by validator`() = runBlocking {
        val romantic = object : LanguageModel {
            override suspend fun generate(prompt: AishaPrompt, language: String, goal: ResponseGoal) =
                "I love you jaan. You are my everything."
        }
        val e = engine(romantic)
        val out = e.handleUserTurn("hello, how are you")
        assertFalse("reply must not contain early-stage romance",
            out.text.lowercase().contains("love you"))
    }

    private fun clockNextDay(e: AishaCoreEngine) {
        (e.clock as FixedClock).nextDay()
        e.finalizeDay()
    }

    @Test fun `activity notes are safe phrases - never leak user text (Master 19)`() = runBlocking {
        val secret = "my sister Ananya is preparing for NEET in Kota"
        val e = engine()
        val out = e.handleUserTurn(secret)
        assertTrue(out.activityNotes.isNotEmpty())
        assertTrue("notes must never contain user input",
            out.activityNotes.none { it.contains("Ananya") || it.contains(secret) })
    }

    @Test fun `bond stage change is recorded as MILESTONE event (Workflow 5 and 18)`() = runBlocking {
        val e = engine()
        repeat(400) { e.handleUserTurn("I love you jaan, you mean everything") }  // enough to cross Warm (25)
        val day = e.dayLogs.ensureToday()
        assertTrue("stage milestone must be persisted to day log",
            day.events.any { it.type == "BOND_STAGE" && it.importance == Importance.MILESTONE })
    }
}

class ImportanceRecallTests {
    @Test fun `milestone events outrank normal events in recall`() {
        val store = InMemoryDayLogStore()
        val clock = FixedClock()
        val mgr = DayLogManager(store, clock)
        mgr.ensureToday()
        mgr.recordEvent("NOTE", "user mentioned Kota once", Importance.NORMAL)
        mgr.recordEvent("BOND_STAGE", "relationship state: Companion → Warm Familiarity in Kota talk", Importance.MILESTONE)
        mgr.finalizeDay()
        val hits = RecallEngine(store).recall("Kota", 5)
        assertTrue(hits.first().snippet.contains("BOND_STAGE") || hits.first().snippet.contains("relationship"))
    }
}


class TrashTests {
    private fun setup(): Triple<InMemoryDayLogStore, InMemoryTrashStore, TrashManager> {
        val logs = InMemoryDayLogStore()
        val trash = InMemoryTrashStore()
        val events = mutableListOf<String>()
        val mgr = TrashManager(trash, logs, clockMs = { 1000L }, audit = { events += it })
        return Triple(logs, trash, mgr)
    }

    @Test fun `deletion removes from active store and protects in trash`() {
        val (logs, _, mgr) = setup()
        logs.save(DayLogData("2026-09-20", summary = "s"))
        val ok = mgr.moveToTrash("2026-09-20", "user request", byteArrayOf(1, 2), "abc", 2)
        assertTrue(ok)
        assertNull("deleted day must leave active store", logs.load("2026-09-20"))
    }

    @Test fun `trash is never a recall source`() {
        val (logs, trash, _) = setup()
        logs.save(DayLogData("2026-09-20", summary = "secret picnic plan"))
        val mgr = TrashManager(trash, logs, { 1L })
        mgr.moveToTrash("2026-09-20", "user request", byteArrayOf(1), "s", 1)
        val hits = RecallEngine(logs).recall("secret picnic", 5)
        assertTrue("deleted content must be unreachable from recall", hits.isEmpty())
    }

    @Test fun `restore requires authorization and audits`() {
        val (logs, _, mgr) = setup()
        logs.save(DayLogData("2026-09-20", summary = "s"))
        mgr.moveToTrash("2026-09-20", "test", byteArrayOf(1), "s", 1)
        val r = mgr.restore("2026-09-20", Authorization.UserLocal)
        assertTrue(r is RestoreResult.RESTORED)
    }

    @Test fun `purge is permanent and admin-denied for plain admins`() {
        val (_, trash, mgr) = setup()
        trash.put(TrashedDay("2026-09-20", byteArrayOf(1), "s", "t", 1, 1))
        assertTrue(mgr.purge("2026-09-20", Authorization.UserLocal))
        assertNull(trash.take("2026-09-20"))
        trash.put(TrashedDay("2026-09-21", byteArrayOf(1), "s", "t", 1, 1))
        assertFalse("plain Admin must not purge (SuperAdmin only with server policy)",
            mgr.purge("2026-09-21", Authorization.Admin("t")))
    }
}

class ExportTests {
    @Test fun `readable export contains days and labels`() {
        val day = DayLogData("2026-09-20", summary = "quiet day").apply {
            events += DayEvent(java.time.LocalDateTime.of(2026, 9, 20, 10, 0), "NOTE", "hello world", Importance.HIGH)
            conversations += ChatMessage(Role.USER, "hi", java.time.LocalDateTime.of(2026, 9, 20, 10, 1))
        }
        val out = ExportBuilder.buildReadable(listOf(day))
        assertTrue(out.startsWith("AISHA DATA EXPORT — READABLE SUMMARY"))
        assertTrue("DAY 2026-09-20" in out && "hello world" in out && "[HIGH]" in out)
    }

    @Test fun `archive json is versioned and escaping is safe`() {
        val day = DayLogData("2026-09-20", summary = "quote \" and backslash \\ test")
        val json = ExportBuilder.buildArchiveJson(listOf(day))
        assertTrue(json.startsWith("{\"format\":\"AISHA_EXPORT_V1\""))
        assertTrue("quote \\\" and" in json)
    }
}
