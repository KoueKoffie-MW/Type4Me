package com.transcriptor.hid.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GeminiRemoteRewriterTest {

    @Test
    fun testIsAvailableReturnsFalseWhenApiKeyMissing() = runBlocking {
        val rewriter = GeminiRemoteRewriter(
            apiKeyProvider = { null }
        )
        assertFalse(rewriter.isAvailable())

        val blankRewriter = GeminiRemoteRewriter(
            apiKeyProvider = { "   " }
        )
        assertFalse(blankRewriter.isAvailable())
    }

    @Test
    fun testIsAvailableReturnsTrueWhenApiKeyPresent() = runBlocking {
        val rewriter = GeminiRemoteRewriter(
            apiKeyProvider = { "AIzaSyFakeKeyForTesting12345" }
        )
        assertTrue(rewriter.isAvailable())
    }

    @Test
    fun testRewriteReturnsEmptyStringForBlankInput() = runBlocking {
        var generatorCalled = false
        val rewriter = GeminiRemoteRewriter(
            apiKeyProvider = { "AIzaSyFakeKeyForTesting12345" },
            generator = { _, _, _, _, _ ->
                generatorCalled = true
                "Should not be called"
            }
        )

        val result = rewriter.rewrite("   ", PromptPreset.CLEAN_AND_POLISH)
        assertTrue(result.isSuccess)
        assertEquals("", result.getOrNull())
        assertFalse(generatorCalled)
    }

    @Test
    fun testRewriteFailsWhenApiKeyMissing() = runBlocking {
        val rewriter = GeminiRemoteRewriter(
            apiKeyProvider = { null }
        )

        val result = rewriter.rewrite("Hello world", PromptPreset.CLEAN_AND_POLISH)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("API key"))
    }

    @Test
    fun testSuccessfulRewriteWithDefaultModel() = runBlocking {
        var capturedApiKey = ""
        var capturedModel = ""
        var capturedPrompt = ""
        var capturedSystemInstruction = ""
        var capturedTemperature = 0f

        val expectedOutput = "Sehr geehrte Damen und Herren, anbei finden Sie den Bericht."

        val rewriter = GeminiRemoteRewriter(
            apiKeyProvider = { "valid-api-key-xyz" },
            model = GeminiRemoteRewriter.MODEL_GEMINI_3_7_FLASH,
            generator = { apiKey, model, prompt, systemInstruction, temperature ->
                capturedApiKey = apiKey
                capturedModel = model
                capturedPrompt = prompt
                capturedSystemInstruction = systemInstruction
                capturedTemperature = temperature
                expectedOutput
            }
        )

        val input = "hallo ich schicke ihnen mal den bericht"
        val result = rewriter.rewrite(input, PromptPreset.BUSINESS_GERMAN)

        assertTrue(result.isSuccess)
        assertEquals(expectedOutput, result.getOrNull())
        assertEquals("valid-api-key-xyz", capturedApiKey)
        assertEquals("gemini-3.7-flash", capturedModel)
        assertEquals(input, capturedPrompt)
        assertEquals(PromptPreset.BUSINESS_GERMAN.systemPrompt, capturedSystemInstruction)
        assertEquals(0.2f, capturedTemperature, 0.001f)
    }

    @Test
    fun testSuccessfulRewriteWithFlashLiteModel() = runBlocking {
        var capturedModel = ""
        val rewriter = GeminiRemoteRewriter(
            apiKeyProvider = { "valid-api-key" },
            model = GeminiRemoteRewriter.MODEL_GEMINI_3_5_FLASH_LITE,
            generator = { _, model, _, _, _ ->
                capturedModel = model
                "Polished text"
            }
        )

        val result = rewriter.rewrite("test text", PromptPreset.CLEAN_AND_POLISH)
        assertTrue(result.isSuccess)
        assertEquals("gemini-3.5-flash-lite", capturedModel)
    }

    @Test
    fun testDynamicModelProviderPicksUpUpdatedModelOnEachRewrite() = runBlocking {
        var currentModelInSettings = GeminiRemoteRewriter.MODEL_GEMINI_3_7_FLASH
        var capturedModel = ""

        val rewriter = GeminiRemoteRewriter(
            apiKeyProvider = { "valid-api-key" },
            modelProvider = { currentModelInSettings },
            generator = { _, model, _, _, _ ->
                capturedModel = model
                "Output"
            }
        )

        // First invocation with default model
        rewriter.rewrite("first test", PromptPreset.CLEAN_AND_POLISH)
        assertEquals(GeminiRemoteRewriter.MODEL_GEMINI_3_7_FLASH, capturedModel)

        // User changes model dynamically in settings
        currentModelInSettings = GeminiRemoteRewriter.MODEL_GEMINI_3_FLASH_PREVIEW
        rewriter.rewrite("second test", PromptPreset.CLEAN_AND_POLISH)
        assertEquals(GeminiRemoteRewriter.MODEL_GEMINI_3_FLASH_PREVIEW, capturedModel)

        // User changes model again to flash-lite
        currentModelInSettings = GeminiRemoteRewriter.MODEL_GEMINI_3_5_FLASH_LITE
        rewriter.rewrite("third test", PromptPreset.CLEAN_AND_POLISH)
        assertEquals(GeminiRemoteRewriter.MODEL_GEMINI_3_5_FLASH_LITE, capturedModel)
    }

    @Test
    fun testRewritePropagatesGeneratorException() = runBlocking {
        val rewriter = GeminiRemoteRewriter(
            apiKeyProvider = { "valid-api-key" },
            generator = { _, _, _, _, _ ->
                throw RuntimeException("Quota exceeded 429")
            }
        )

        val result = rewriter.rewrite("Sample text", PromptPreset.CLEAN_AND_POLISH)
        assertTrue(result.isFailure)
        assertEquals("Quota exceeded 429", result.exceptionOrNull()?.message)
    }

    @Test
    fun testLiteRtOnDeviceRewriterPromptFormattingAndExecution() = runBlocking {
        var capturedPrompt = ""
        val rewriter = LiteRtOnDeviceRewriter(
            modelFile = null,
            inferenceEngine = { prompt ->
                capturedPrompt = prompt
                "LiteRT cleaned text"
            }
        )

        val rawInput = "um we need to optimize this query"
        val result = rewriter.rewrite(rawInput, PromptPreset.TECHNICAL_CODE_COMMENTS)

        assertTrue(result.isSuccess)
        assertEquals("LiteRT cleaned text", result.getOrNull())
        assertTrue(capturedPrompt.contains("<start_of_turn>system\n${PromptPreset.TECHNICAL_CODE_COMMENTS.systemPrompt}<end_of_turn>"))
        assertTrue(capturedPrompt.contains("<start_of_turn>user\n$rawInput<end_of_turn>"))
        assertTrue(capturedPrompt.endsWith("<start_of_turn>model\n"))
    }

    @Test
    fun testLiteRtOnDeviceRewriterFailsWhenModelMissingAndNoEngine() = runBlocking {
        val nonExistentFile = File("non_existent_model_weights.bin")
        val rewriter = LiteRtOnDeviceRewriter(
            modelFile = nonExistentFile,
            inferenceEngine = null
        )

        assertFalse(rewriter.isAvailable())
        val result = rewriter.rewrite("Sample input", PromptPreset.CLEAN_AND_POLISH)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("LiteRT on-device model weights are not installed"))
    }
}
