package com.transcriptor.hid.stress

import com.transcriptor.hid.engine.InterpolationContext
import com.transcriptor.hid.engine.TemplateToken
import com.transcriptor.hid.engine.VariableDescriptor
import com.transcriptor.hid.engine.VariableParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VariableParserStressTest {

    @Test
    fun testComplexWindowsPathsWithVariables() {
        val testCases = listOf(
            // Standard backslash path
            "C:\\Users\\Admin\\path\\{{prompt:FileName|default.txt}}" to "C:\\Users\\Admin\\path\\default.txt",
            // Path with colon before backslash
            "C:\\{{prompt:RootFile|boot.ini}}" to "C:\\boot.ini",
            // Deep folder structure
            "D:\\Work\\Repo_2026\\v1.0\\{{timestamp}}" to true,
            // UNC network path
            "\\\\192.168.1.100\\shared_drive\\{{prompt:DocName|doc.pdf}}" to "\\\\192.168.1.100\\shared_drive\\doc.pdf",
            // Forward slash mixed path
            "C:/Users/Admin/AppData/Local/{{prompt:Dir|Temp}}/log.txt" to "C:/Users/Admin/AppData/Local/Temp/log.txt"
        )

        for ((template, expected) in testCases) {
            val prompts = VariableParser.extractPrompts(template)
            assertTrue("Should extract prompt for: $template", prompts.isNotEmpty() || template.contains("timestamp"))

            val (result, _) = VariableParser.evaluate(template, InterpolationContext())
            if (expected is String) {
                assertEquals("Evaluation mismatch for $template", expected, result)
            } else {
                assertTrue("Evaluated string should not be empty for $template", result.isNotEmpty())
            }
        }
    }

    @Test
    fun testWindowsPathEdgeCasesParenthesesAndHyphens() {
        // Test folder names ending in parentheses or hyphens before backslash
        val templateParens = "C:\\Program Files (x86)\\{{prompt:BinName|node.exe}}"
        val promptsParens = VariableParser.extractPrompts(templateParens)
        val (resultParens, _) = VariableParser.evaluate(templateParens, InterpolationContext())

        // Also test folder ending with hyphen
        val templateHyphen = "C:\\opt\\build-\\{{prompt:Target|debug}}"
        val promptsHyphen = VariableParser.extractPrompts(templateHyphen)
        val (resultHyphen, _) = VariableParser.evaluate(templateHyphen, InterpolationContext())

        // Verify prompts are properly extracted and folder path backslashes preserved:
        assertEquals(1, promptsParens.size)
        assertEquals("BinName", promptsParens[0].label)
        assertEquals("node.exe", promptsParens[0].defaultValue)
        assertEquals("C:\\Program Files (x86)\\node.exe", resultParens)

        assertEquals(1, promptsHyphen.size)
        assertEquals("Target", promptsHyphen[0].label)
        assertEquals("debug", promptsHyphen[0].defaultValue)
        assertEquals("C:\\opt\\build-\\debug", resultHyphen)

        // Verify explicit escape is preserved
        val templateEscaped = "\\{{literal}}"
        assertTrue(VariableParser.extractPrompts(templateEscaped).isEmpty())
        val (resultEscaped, _) = VariableParser.evaluate(templateEscaped, InterpolationContext())
        assertEquals("{{literal}}", resultEscaped)
    }

    @Test
    fun testEmptyBracesBoundaryConditions() {
        // Empty braces {{}}
        val t1 = "echo {{}} done"
        val tokens1 = VariableParser.parse(t1)
        val (r1, b1) = VariableParser.evaluate(t1, InterpolationContext())
        // {{}} has no expression inside, must be preserved literally without crash
        assertEquals(0, b1)
        assertTrue("Result should contain {{}}", r1.contains("{{}}"))

        // Whitespace-only braces {{   }}
        val t2 = "prefix {{   }} suffix"
        val tokens2 = VariableParser.parse(t2)
        val (r2, b2) = VariableParser.evaluate(t2, InterpolationContext())
        assertEquals(0, b2)
        assertTrue(r2.contains("prefix") && r2.contains("suffix"))

        // Triple braces {{{var}}}
        val t3 = "{{{prompt:Var|val}}}"
        val (r3, _) = VariableParser.evaluate(t3, InterpolationContext())
        // In current regex implementation, {{{var}}} matches {{ with leading { in expression
        // producing UnrecognizedLiteral and retaining literal tag
        assertEquals("{{{prompt:Var|val}}}", r3)
    }

    @Test
    fun testUnclosedBracesBoundaryConditions() {
        val unclosedCases = listOf(
            "unclosed {{ tag without end",
            "{{ starts right away",
            "nested unclosed {{outer {{inner}}",
            "}}}} only closing braces",
            "{{{{ only open braces",
            "}}{{ inverted braces",
            "prefix {{prompt:unfinished"
        )

        for (template in unclosedCases) {
            // Must not throw any IndexOutOfBoundsException, PatternSyntaxException, or crash
            val tokens = VariableParser.parse(template)
            assertNotNull("Tokens must not be null for $template", tokens)
            val (result, _) = VariableParser.evaluate(template, InterpolationContext())
            assertNotNull("Result must not be null for $template", result)
        }
    }

    @Test
    fun testMassiveTemplateEvaluationStress() {
        // Generate template with 1,000 dynamic variables
        val sb = StringBuilder()
        repeat(1000) { i ->
            sb.append("Item $i: {{uuid}} - {{timestamp}} | ")
        }
        val massiveTemplate = sb.toString()

        val startTime = System.currentTimeMillis()
        val (result, backtrack) = VariableParser.evaluate(massiveTemplate, InterpolationContext())
        val elapsed = System.currentTimeMillis() - startTime

        assertEquals(0, backtrack)
        assertTrue("Result length should be very large", result.length > 50000)
        assertTrue("Evaluation of 1000 variables should complete under 1500ms", elapsed < 1500L)
    }

    @Test
    fun testConcurrentParsingAndEvaluation() = runBlocking {
        val template = "Deploy {{prompt:Service|api}} to {{prompt:Env|prod}} at {{iso_date}} by {{host_os}}"
        val context = InterpolationContext(
            promptAnswers = mapOf("Service" to "auth-svc", "Env" to "staging"),
            hostOs = "LINUX"
        )

        val workers = (1..50).map {
            async(Dispatchers.Default) {
                val prompts = VariableParser.extractPrompts(template)
                val (result, backtrack) = VariableParser.evaluate(template, context)
                Triple(prompts, result, backtrack)
            }
        }

        val results = workers.awaitAll()
        results.forEach { (prompts, result, backtrack) ->
            assertEquals(2, prompts.size)
            assertEquals(0, backtrack)
            assertTrue(result.contains("Deploy auth-svc to staging at"))
            assertTrue(result.contains("by LINUX"))
        }
    }
}
