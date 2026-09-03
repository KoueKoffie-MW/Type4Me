package com.transcriptor.hid.engine

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MacroRunnerTest {

    private val sentReports = mutableListOf<ByteArray>()
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
    fun testExtractPromptsFromMacroSteps() {
        val stepsJson = """
            [
                {"type":"type_string","text":"git commit -m \"{{prompt:Commit Message|initial}}\""},
                {"type":"prompt_variable","variableName":"branch_name","promptLabel":"Target Branch","defaultValue":"main"},
                {"type":"delay","durationMs":100}
            ]
        """.trimIndent()

        val prompts = runner.extractPrompts(stepsJson)
        assertEquals(2, prompts.size)
        assertEquals("Commit Message", prompts[0].label)
        assertEquals("initial", prompts[0].defaultValue)
        assertEquals("Target Branch", prompts[1].label)
        assertEquals("main", prompts[1].defaultValue)
    }

    @Test
    fun testExecuteMacroWithPromptAnswers() = runBlocking {
        val stepsJson = """
            [
                {"type":"prompt_variable","variableName":"user_name","promptLabel":"User Name","defaultValue":"anonymous"}
            ]
        """.trimIndent()

        val context = InterpolationContext(
            promptAnswers = mapOf("user_name" to "Alice")
        )

        runner.execute(stepsJson, context)
        assertEquals("Alice", dispatcher.currentHostText.value)
    }

    @Test
    fun testExecuteMacroPromptVarFallbackToLabel() = runBlocking {
        val stepsJson = """
            [
                {"type":"prompt_variable","variableName":"first_name","promptLabel":"First Name","defaultValue":"Bob"}
            ]
        """.trimIndent()

        // Answer provided under promptLabel "First Name"
        val context = InterpolationContext(
            promptAnswers = mapOf("First Name" to "Charlie")
        )

        runner.execute(stepsJson, context)
        assertEquals("Charlie", dispatcher.currentHostText.value)
    }

    @Test
    fun testExecuteMacroMalformedJsonReportsError() = runBlocking {
        runner.execute("{ invalid json }")
        val state = runner.executionState.value
        assertTrue(state is MacroExecutionState.Error)
    }
}
