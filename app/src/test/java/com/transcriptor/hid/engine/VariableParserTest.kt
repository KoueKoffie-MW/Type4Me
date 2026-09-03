package com.transcriptor.hid.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VariableParserTest {

    @Test
    fun testTimestampVariable() {
        val template = "event_time: {{timestamp}}"
        val context = InterpolationContext()
        val (result, backtrack) = VariableParser.evaluate(template, context)

        assertThat(result).startsWith("event_time: ")
        val timestampStr = result.removePrefix("event_time: ")
        val timestamp = timestampStr.toLongOrNull()
        assertThat(timestamp).isNotNull()
        assertThat(timestamp).isGreaterThan(1700000000000L)
        assertThat(backtrack).isEqualTo(0)
    }

    @Test
    fun testIsoDateVariable() {
        val template = "log_date={{iso_date}}"
        val context = InterpolationContext()
        val (result, backtrack) = VariableParser.evaluate(template, context)

        assertThat(result).startsWith("log_date=")
        assertThat(result).contains("T")
        assertThat(result).endsWith("Z")
        assertThat(backtrack).isEqualTo(0)
    }

    @Test
    fun testFormattedDateVariable() {
        val template = "release-{{date:yyyy-MM-dd}}"
        val context = InterpolationContext()
        val (result, _) = VariableParser.evaluate(template, context)

        val expected = "release-" + SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun testUuidAndShortUuidVariables() {
        val template = "id: {{uuid}}, short: {{short_uuid}}"
        val context = InterpolationContext()
        val (result, _) = VariableParser.evaluate(template, context)

        val parts = result.split(", short: ")
        assertThat(parts).hasSize(2)
        val fullUuid = parts[0].removePrefix("id: ")
        val shortUuid = parts[1]

        assertThat(fullUuid).hasLength(36)
        assertThat(fullUuid).contains("-")
        assertThat(shortUuid).hasLength(8)
    }

    @Test
    fun testClipboardVariableInterpolation() {
        val template = "kubectl apply -f -\n{{clipboard}}\n"
        val context = InterpolationContext(clipboardText = "apiVersion: v1\nkind: Pod")
        val (result, _) = VariableParser.evaluate(template, context)

        assertThat(result).isEqualTo("kubectl apply -f -\napiVersion: v1\nkind: Pod\n")
    }

    @Test
    fun testClipboardVariableEmptyFallback() {
        val template = "echo '{{clipboard}}'"
        val context = InterpolationContext(clipboardText = null)
        val (result, _) = VariableParser.evaluate(template, context)

        assertThat(result).isEqualTo("echo ''")
    }

    @Test
    fun testPromptVariablesWithPipeAndColonDefaults() {
        val template = "git checkout -b {{prompt:Branch Name|feature/login}} from {{prompt:Base:main}}"
        val prompts = VariableParser.extractPrompts(template)

        assertThat(prompts).hasSize(2)
        assertThat(prompts[0].label).isEqualTo("Branch Name")
        assertThat(prompts[0].defaultValue).isEqualTo("feature/login")
        assertThat(prompts[1].label).isEqualTo("Base")
        assertThat(prompts[1].defaultValue).isEqualTo("main")

        // Test default evaluation without answers
        val (defaultResult, _) = VariableParser.evaluate(template, InterpolationContext())
        assertThat(defaultResult).isEqualTo("git checkout -b feature/login from main")

        // Test evaluation with answers
        val answers = mapOf("Branch Name" to "fix/issue-42", "Base" to "develop")
        val (customResult, _) = VariableParser.evaluate(template, InterpolationContext(promptAnswers = answers))
        assertThat(customResult).isEqualTo("git checkout -b fix/issue-42 from develop")
    }

    @Test
    fun testSimplePromptInput() {
        val template = "grep -rn '{{prompt_input}}' ."
        val prompts = VariableParser.extractPrompts(template)

        assertThat(prompts).hasSize(1)
        assertThat(prompts[0].label).isEqualTo("Input")

        val (result, _) = VariableParser.evaluate(template, InterpolationContext(promptAnswers = mapOf("Input" to "TODO")))
        assertThat(result).isEqualTo("grep -rn 'TODO' .")
    }

    @Test
    fun testHostOsVariable() {
        val template = "platform: {{host_os}}"
        val (winResult, _) = VariableParser.evaluate(template, InterpolationContext(hostOs = "WINDOWS"))
        assertThat(winResult).isEqualTo("platform: WINDOWS")

        val (macResult, _) = VariableParser.evaluate(template, InterpolationContext(hostOs = "MACOS"))
        assertThat(macResult).isEqualTo("platform: MACOS")

        val (linuxResult, _) = VariableParser.evaluate(template, InterpolationContext(hostOs = "LINUX"))
        assertThat(linuxResult).isEqualTo("platform: LINUX")
    }

    @Test
    fun testUnrecognizedVariablesPreservedAsLiterals() {
        val template = "docker ps --format '{{.ID}} {{.Names}}'"
        val prompts = VariableParser.extractPrompts(template)

        // Unrecognized tags like {{.ID}} should not be treated as user prompts
        assertThat(prompts).isEmpty()

        val (result, _) = VariableParser.evaluate(template, InterpolationContext())
        assertThat(result).isEqualTo("docker ps --format '{{.ID}} {{.Names}}'")
    }

    @Test
    fun testBackslashDelimiterEscaping() {
        val template = """\{\{ ansible_host \}\} and \{\{ jinja_var \}\}"""
        val (result, _) = VariableParser.evaluate(template, InterpolationContext())

        assertThat(result).isEqualTo("{{ ansible_host }} and {{ jinja_var }}")
        val prompts = VariableParser.extractPrompts(template)
        assertThat(prompts).isEmpty()
    }

    @Test
    fun testCursorBacktrackingSingleLine() {
        val template = "git commit -m \"{{cursor}}\""
        val (result, backtrack) = VariableParser.evaluate(template, InterpolationContext())

        assertThat(result).isEqualTo("git commit -m \"\"")
        assertThat(backtrack).isEqualTo(1) // 1 quote character after cursor
    }

    @Test
    fun testCursorBacktrackingWithEmojisAndCodePoints() {
        val template = "echo \"🚀 {{cursor}} completed! 🎉\""
        val (result, backtrack) = VariableParser.evaluate(template, InterpolationContext())

        assertThat(result).isEqualTo("echo \"🚀  completed! 🎉\"")
        // " completed! 🎉" -> 1 space + 10 chars + 1 space + 1 emoji (🎉 = 1 code point) + 1 quote = 14 code points
        val expectedBacktrack = " completed! 🎉\"".codePointCount(0, " completed! 🎉\"".length)
        assertThat(backtrack).isEqualTo(expectedBacktrack)
    }

    @Test
    fun testMultipleCursorTagsUsesFirstOnly() {
        val template = "start {{cursor}} middle {{cursor}} end"
        val (result, backtrack) = VariableParser.evaluate(template, InterpolationContext())

        assertThat(result).isEqualTo("start  middle  end")
        val expectedBacktrack = " middle  end".codePointCount(0, " middle  end".length)
        assertThat(backtrack).isEqualTo(expectedBacktrack)
    }

    @Test
    fun testWindowsPathBackslashNotTreatedAsEscape() {
        val template = "C:\\Windows\\System32\\{{prompt:executable|cmd.exe}}"
        val prompts = VariableParser.extractPrompts(template)
        assertThat(prompts).hasSize(1)
        assertThat(prompts[0].label).isEqualTo("executable")
        assertThat(prompts[0].defaultValue).isEqualTo("cmd.exe")

        val (defaultResult, _) = VariableParser.evaluate(template, InterpolationContext())
        assertThat(defaultResult).isEqualTo("C:\\Windows\\System32\\cmd.exe")

        val answers = mapOf("executable" to "powershell.exe")
        val (customResult, _) = VariableParser.evaluate(template, InterpolationContext(promptAnswers = answers))
        assertThat(customResult).isEqualTo("C:\\Windows\\System32\\powershell.exe")
    }

    @Test
    fun testPromptWithoutColonAndBlankLabels() {
        val template = "run {{prompt}} and {{prompt:}} and {{prompt_input}}"
        val prompts = VariableParser.extractPrompts(template)
        assertThat(prompts).hasSize(1)
        assertThat(prompts[0].label).isEqualTo("Input")

        val (result, _) = VariableParser.evaluate(template, InterpolationContext(promptAnswers = mapOf("Input" to "test")))
        assertThat(result).isEqualTo("run test and test and test")
    }
}
