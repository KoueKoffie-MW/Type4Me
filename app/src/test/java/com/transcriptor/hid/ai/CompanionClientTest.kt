package com.transcriptor.hid.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class CompanionClientTest {

    // --------------------------------------------------------------------------
    // 1. Data Model & JSON Parsing Tests
    // --------------------------------------------------------------------------

    @Test
    fun testParseValidContextResponseSnakeCase() {
        val json = """
            {
                "window_title": "MainActivity.kt - Type4Me - Android Studio",
                "process_name": "studio64.exe",
                "selected_text": "fun rewrite(text: String)",
                "timestamp": 1725235200000
            }
        """.trimIndent()

        val context = DesktopContext.fromJson(json)
        assertEquals("MainActivity.kt - Type4Me - Android Studio", context.windowTitle)
        assertEquals("studio64.exe", context.processName)
        assertEquals("fun rewrite(text: String)", context.selectedText)
        assertEquals(1725235200000L, context.timestamp)
        assertFalse(context.isEmpty)
    }

    @Test
    fun testParseValidContextResponseCamelCase() {
        val json = """
            {
                "windowTitle": "Visual Studio Code - server.py",
                "processName": "Code",
                "selectedText": "import http.server",
                "timestamp": 1725235210000
            }
        """.trimIndent()

        val context = DesktopContext.fromJson(json)
        assertEquals("Visual Studio Code - server.py", context.windowTitle)
        assertEquals("Code", context.processName)
        assertEquals("import http.server", context.selectedText)
        assertEquals(1725235210000L, context.timestamp)
        assertFalse(context.isEmpty)
    }

    @Test
    fun testParsePartialContextWithMissingFields() {
        val json = """
            {
                "window_title": "Terminal - bash"
            }
        """.trimIndent()

        val context = DesktopContext.fromJson(json)
        assertEquals("Terminal - bash", context.windowTitle)
        assertEquals("", context.processName)
        assertEquals("", context.selectedText)
        assertEquals(0L, context.timestamp)
        assertFalse(context.isEmpty)
    }

    @Test
    fun testEmptyContextModel() {
        val empty = DesktopContext.EMPTY
        assertTrue(empty.isEmpty)
        assertEquals("", empty.windowTitle)
        assertEquals("", empty.processName)
        assertEquals("", empty.selectedText)
        assertEquals(0L, empty.timestamp)

        val blankJson = "{}"
        val parsedBlank = DesktopContext.fromJson(blankJson)
        assertTrue(parsedBlank.isEmpty)

        val invalidJson = "{ not valid json }"
        val parsedInvalid = DesktopContext.fromJson(invalidJson)
        assertTrue(parsedInvalid.isEmpty)
    }

    @Test
    fun testParseHealthResponse() {
        val json = """{"status": "ok", "version": "2.0.0"}"""
        val health = CompanionHealth.fromJson(json)
        assertEquals("ok", health.status)
        assertEquals("2.0.0", health.version)
        assertTrue(health.isOk)

        val errJson = """{"status": "error", "version": "2.0.0"}"""
        val errHealth = CompanionHealth.fromJson(errJson)
        assertFalse(errHealth.isOk)
    }

    // --------------------------------------------------------------------------
    // 2. HTTP Network Client Mock Tests
    // --------------------------------------------------------------------------

    private class MockHttpConnection(
        url: URL,
        private val statusCode: Int,
        private val responseBody: String,
        private val throwOnConnect: Boolean = false
    ) : HttpURLConnection(url) {
        val requestHeaders = mutableMapOf<String, String>()

        override fun getResponseCode(): Int {
            if (throwOnConnect) throw IOException("Connection refused or timed out")
            return statusCode
        }

        override fun getInputStream(): InputStream {
            if (throwOnConnect) throw IOException("Network unreachable")
            if (statusCode !in 200..299) throw IOException("HTTP error $statusCode")
            return ByteArrayInputStream(responseBody.toByteArray(Charsets.UTF_8))
        }

        override fun getErrorStream(): InputStream? {
            if (statusCode in 200..299) return null
            return ByteArrayInputStream(responseBody.toByteArray(Charsets.UTF_8))
        }

        override fun setRequestProperty(key: String, value: String) {
            requestHeaders[key] = value
        }

        override fun connect() {
            if (throwOnConnect) throw IOException("Connect failed")
        }

        override fun disconnect() {}
        override fun usingProxy(): Boolean = false
    }

    @Test
    fun testFetchActiveContextSuccess() = runBlocking {
        val jsonBody = """
            {
                "window_title": "Project.md - Editor",
                "process_name": "notepad.exe",
                "selected_text": "Milestone 5",
                "timestamp": 1725235300000
            }
        """.trimIndent()

        var capturedUrl: URL? = null
        var lastConnection: MockHttpConnection? = null

        val client = CompanionClient(
            timeoutMs = 1000,
            connectionFactory = { url ->
                capturedUrl = url
                MockHttpConnection(url, 200, jsonBody).also { lastConnection = it }
            }
        )

        val result = client.fetchActiveContext("192.168.1.100", 8765, token = "secret123")
        assertTrue(result.isSuccess)

        val context = result.getOrNull()
        assertNotNull(context)
        assertEquals("Project.md - Editor", context!!.windowTitle)
        assertEquals("notepad.exe", context.processName)
        assertEquals("Milestone 5", context.selectedText)
        assertEquals("http://192.168.1.100:8765/context", capturedUrl?.toString())
        assertEquals("Bearer secret123", lastConnection?.requestHeaders?.get("Authorization"))
    }

    @Test
    fun testFetchActiveContextWithoutTokenOmitsAuthHeader() = runBlocking {
        val jsonBody = """{"window_title": "Browser", "process_name": "chrome.exe", "selected_text": "", "timestamp": 12345}"""
        var lastConnection: MockHttpConnection? = null

        val client = CompanionClient(
            timeoutMs = 500,
            connectionFactory = { url ->
                MockHttpConnection(url, 200, jsonBody).also { lastConnection = it }
            }
        )

        val result = client.fetchActiveContext("127.0.0.1")
        assertTrue(result.isSuccess)
        assertNull(lastConnection?.requestHeaders?.get("Authorization"))
    }

    @Test
    fun testFetchActiveContextUnauthorizedReturnsFailure() = runBlocking {
        val errBody = """{"error": "Unauthorized", "message": "Invalid token"}"""
        val client = CompanionClient(
            connectionFactory = { url -> MockHttpConnection(url, 401, errBody) }
        )

        val result = client.fetchActiveContext("192.168.1.50", 8765, token = "bad_token")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("401") == true)
    }

    @Test
    fun testFetchActiveContextNetworkTimeoutReturnsFailure() = runBlocking {
        val client = CompanionClient(
            connectionFactory = { url -> MockHttpConnection(url, 500, "", throwOnConnect = true) }
        )

        val result = client.fetchActiveContext("192.168.1.99", 8765)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun testCheckHealthSuccess() = runBlocking {
        val jsonBody = """{"status": "ok", "version": "2.0.0"}"""
        val client = CompanionClient(
            connectionFactory = { url -> MockHttpConnection(url, 200, jsonBody) }
        )

        val result = client.checkHealth("192.168.1.100")
        assertTrue(result.isSuccess)
        val health = result.getOrNull()
        assertNotNull(health)
        assertEquals("ok", health!!.status)
        assertEquals("2.0.0", health.version)
        assertTrue(health.isOk)
    }

    @Test
    fun testIsReachableReturnsTrueWhenOkAndFalseWhenFailing() = runBlocking {
        val goodClient = CompanionClient(
            connectionFactory = { url -> MockHttpConnection(url, 200, """{"status": "ok", "version": "2.0.0"}""") }
        )
        assertTrue(goodClient.isReachable("192.168.1.100"))

        val badClient = CompanionClient(
            connectionFactory = { url -> MockHttpConnection(url, 500, "", throwOnConnect = true) }
        )
        assertFalse(badClient.isReachable("192.168.1.100"))
        assertFalse(badClient.isReachable(""))
    }

    // --------------------------------------------------------------------------
    // 3. Dual-Tier Air-Gap Fallback Guarantee Tests
    // --------------------------------------------------------------------------

    @Test
    fun testFetchContextOrNullReturnsNullWhenHostBlank() = runBlocking {
        val client = CompanionClient()
        assertNull(client.fetchContextOrNull(null))
        assertNull(client.fetchContextOrNull(""))
        assertNull(client.fetchContextOrNull("   "))
    }

    @Test
    fun testFetchContextOrNullReturnsNullGracefullyOnNetworkError() = runBlocking {
        val client = CompanionClient(
            connectionFactory = { url -> MockHttpConnection(url, 500, "", throwOnConnect = true) }
        )
        val context = client.fetchContextOrNull("10.0.0.1")
        assertNull(context)
    }

    // --------------------------------------------------------------------------
    // 4. Prompt Formatting & Context Injection Tests
    // --------------------------------------------------------------------------

    @Test
    fun testFormatSystemPromptWithContextInjectsStructuredBlock() {
        val basePrompt = "You are an expert coding assistant."
        val context = DesktopContext(
            windowTitle = "type4me_companion.py - Visual Studio Code",
            processName = "Code.exe",
            selectedText = "def extract_windows():",
            timestamp = 1725235400000
        )

        val formatted = CompanionClient.formatSystemPromptWithContext(basePrompt, context)
        assertTrue(formatted.startsWith(basePrompt))
        assertTrue(formatted.contains("[ACTIVE DESKTOP CONTEXT]"))
        assertTrue(formatted.contains("- Active Application: Code.exe"))
        assertTrue(formatted.contains("- Window Title: type4me_companion.py - Visual Studio Code"))
        assertTrue(formatted.contains("- Selected Host Text: def extract_windows():"))
        assertTrue(formatted.contains("Use this desktop context to resolve ambiguous terminology"))
    }

    @Test
    fun testFormatSystemPromptWithNullOrEmptyContextLeavesPromptUnmodified() {
        val basePrompt = "You are an expert speech transcription editor."

        val formattedWithNull = CompanionClient.formatSystemPromptWithContext(basePrompt, null)
        assertEquals(basePrompt, formattedWithNull)

        val formattedWithEmpty = CompanionClient.formatSystemPromptWithContext(basePrompt, DesktopContext.EMPTY)
        assertEquals(basePrompt, formattedWithEmpty)

        val formattedWithBlankFields = CompanionClient.formatSystemPromptWithContext(
            basePrompt,
            DesktopContext(windowTitle = "   ", processName = "", selectedText = "  ")
        )
        assertEquals(basePrompt, formattedWithBlankFields)
    }

    @Test
    fun testEnrichSystemInstructionMatchesFormatting() {
        val basePrompt = PromptPreset.CLEAN_AND_POLISH.systemPrompt
        val context = DesktopContext(
            windowTitle = "Terminal",
            processName = "bash",
            selectedText = "git status"
        )

        val enriched = CompanionClient.enrichSystemInstruction(basePrompt, context)
        assertTrue(enriched.contains(basePrompt))
        assertTrue(enriched.contains("- Active Application: bash"))
        assertTrue(enriched.contains("- Selected Host Text: git status"))
    }

    @Test
    fun testFormatUserPromptWithContextUsesPresetTemplate() {
        val rawInput = "create a function to parse desktop context"
        val preset = PromptPreset.AGENTIC_PROMPT_ENGINEER

        val result = CompanionClient.formatUserPromptWithContext(
            rawText = rawInput,
            preset = preset,
            context = DesktopContext(windowTitle = "Android Studio")
        )

        assertEquals(preset.formatUserPrompt(rawInput), result)
        assertTrue(result.contains("create a function to parse desktop context"))
    }
}
