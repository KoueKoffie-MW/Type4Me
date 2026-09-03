package com.transcriptor.hid.stress

import com.transcriptor.hid.engine.DefaultDeltaDiffEngine
import com.transcriptor.hid.engine.DefaultKeystrokeDispatcher
import com.transcriptor.hid.engine.InterpolationContext
import com.transcriptor.hid.engine.MacroExecutionState
import com.transcriptor.hid.engine.MacroRunner
import com.transcriptor.hid.engine.UsQwertyKeymap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Collections

class MacroRunnerStressTest {

    private val sentReports = Collections.synchronizedList(mutableListOf<ByteArray>())
    private lateinit var dispatcher: DefaultKeystrokeDispatcher
    private lateinit var runner: MacroRunner

    @Before
    fun setUp() {
        sentReports.clear()
        dispatcher = DefaultKeystrokeDispatcher(
            translator = UsQwertyKeymap(),
            deltaDiffEngine = DefaultDeltaDiffEngine(),
            reportSender = { report ->
                sentReports.add(report.copyOf())
                true
            }
        )
        runner = MacroRunner(
            keystrokeDispatcher = dispatcher,
            reportSender = { report ->
                sentReports.add(report.copyOf())
                true
            }
        )
    }

    @Test
    fun testRapidConcurrentPromptExtraction() = runBlocking {
        val complexMacroJson = """
        [
            {"type":"type_string","text":"git commit -m \"{{prompt:Commit Message|Initial commit}}\""},
            {"type":"prompt_variable","variableName":"BranchName","promptLabel":"Target Branch","defaultValue":"main"},
            {"type":"key_combo","modifiers":0,"usageId":40,"holdMs":10,"repeatCount":1},
            {"type":"type_string","text":"echo {{timestamp}} >> build.log && git push origin {{prompt:Remote|origin}}"},
            {"type":"prompt_variable","variableName":"Token","promptLabel":"Auth Token","defaultValue":""}
        ]
        """.trimIndent()

        // 50 concurrent extractions
        val jobs = (1..50).map {
            async(Dispatchers.Default) {
                runner.extractPrompts(complexMacroJson)
            }
        }

        val results = jobs.awaitAll()
        results.forEach { prompts ->
            assertEquals(4, prompts.size)
            val labels = prompts.map { it.label }.toSet()
            assertTrue(labels.contains("Commit Message"))
            assertTrue(labels.contains("Target Branch"))
            assertTrue(labels.contains("Remote"))
            assertTrue(labels.contains("Auth Token"))
        }
    }

    @Test
    fun testMassiveMacroStepExecution() = runBlocking {
        // Build a massive macro with 500 steps
        val steps = (1..500).map { i ->
            if (i % 2 == 0) {
                """{"type":"type_string","text":"x"}"""
            } else {
                """{"type":"delay","durationMs":0}"""
            }
        }.joinToString(",", prefix = "[", postfix = "]")

        withTimeout(15000L) {
            runner.execute(steps, InterpolationContext())
        }

        assertEquals(MacroExecutionState.Success, runner.executionState.value)
        assertEquals(250, dispatcher.currentHostText.value.length)
    }

    @Test
    fun testConcurrentMacroExecutionContention() = runBlocking {
        // Two concurrent macros competing for execution
        val macro1 = """
        [
            {"type":"type_string","text":"macro1-start "},
            {"type":"delay","durationMs":10},
            {"type":"type_string","text":"macro1-end "}
        ]
        """.trimIndent()

        val macro2 = """
        [
            {"type":"type_string","text":"macro2-start "},
            {"type":"delay","durationMs":10},
            {"type":"type_string","text":"macro2-end "}
        ]
        """.trimIndent()

        val job1 = async(Dispatchers.Default) { runner.execute(macro1) }
        val job2 = async(Dispatchers.Default) { runner.execute(macro2) }

        job1.await()
        job2.await()

        val hostText = dispatcher.currentHostText.value
        assertTrue("Expected macro1 text", hostText.contains("macro1-start"))
        assertTrue("Expected macro2 text", hostText.contains("macro2-start"))
        assertTrue("Final state should be Success or Idle", runner.executionState.value is MacroExecutionState.Success)
    }

    @Test
    fun testMacroCancellationEmitsEmergencyRelease() = runBlocking {
        val longMacro = """
        [
            {"type":"type_string","text":"starting long typing burst that will be cancelled"},
            {"type":"delay","durationMs":2000}
        ]
        """.trimIndent()

        val job = launch(Dispatchers.Default) {
            runner.execute(longMacro)
        }

        delay(30L)
        job.cancelAndJoin()

        // Verify that emergency release report (8 zeros) was sent
        assertTrue("Reports should be sent", sentReports.isNotEmpty())
        val lastReport = sentReports.last()
        assertEquals(0.toByte(), lastReport[0])
        assertEquals(0.toByte(), lastReport[2])
    }

    @Test
    fun testAdversarialMalformedMacroInputs() = runBlocking {
        // 1. Completely invalid JSON
        runner.execute("not json at all {[[")
        assertTrue(runner.executionState.value is MacroExecutionState.Error)

        // 2. Empty string
        runner.execute("")
        assertTrue(runner.executionState.value is MacroExecutionState.Error)

        // 3. Array of empty objects
        runner.execute("[{}, {}]")
        assertTrue(runner.executionState.value is MacroExecutionState.Error)

        // 4. Missing required fields
        runner.execute("""[{"type":"KeyCombination"}]""")
        assertTrue(runner.executionState.value is MacroExecutionState.Error)

        // Extract prompts on garbage input should return emptyList, not throw
        val garbagePrompts = runner.extractPrompts("invalid json %%%")
        assertTrue(garbagePrompts.isEmpty())
    }
}
