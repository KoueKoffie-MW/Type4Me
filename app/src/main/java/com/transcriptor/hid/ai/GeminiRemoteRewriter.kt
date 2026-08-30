package com.transcriptor.hid.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Functional interface for generating text content from Gemini.
 * Allows seamless unit testing and decoupling from the network layer.
 */
fun interface GeminiContentGenerator {
    suspend fun generate(
        apiKey: String,
        model: String,
        prompt: String,
        systemInstruction: String,
        temperature: Float
    ): String
}

/**
 * Robust Android implementation of [GeminiContentGenerator] using standard HTTPS REST calls.
 * Avoids Apache HTTP client classpath collisions on Android runtime while supporting all Gemini models.
 */
class DefaultGenAiSdkGenerator : GeminiContentGenerator {
    override suspend fun generate(
        apiKey: String,
        model: String,
        prompt: String,
        systemInstruction: String,
        temperature: Float
    ): String = withContext(Dispatchers.IO) {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val url = URL(endpoint)
        val connection = (url.openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30000
            readTimeout = 60000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }

        val requestBody = JSONObject().apply {
            if (systemInstruction.isNotBlank()) {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
            }
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", temperature.toDouble())
                put("topP", 0.95)
            })
        }

        try {
            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: throw IllegalStateException("HTTP $responseCode from Gemini API (no error stream)")
            }

            val responseBody = BufferedReader(InputStreamReader(responseStream, "UTF-8")).use { reader ->
                reader.readText()
            }

            if (responseCode !in 200..299) {
                val errorMsg = try {
                    val errorJson = JSONObject(responseBody)
                    if (errorJson.has("error")) {
                        errorJson.getJSONObject("error").optString("message", responseBody)
                    } else {
                        responseBody
                    }
                } catch (_: Exception) {
                    responseBody
                }
                throw IllegalStateException(errorMsg)
            }

            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
                ?: throw IllegalStateException("Gemini returned an empty candidates list.")

            if (candidates.length() == 0) {
                throw IllegalStateException("Gemini returned 0 candidates.")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
                ?: throw IllegalStateException("Gemini response missing content object.")

            val parts = content.optJSONArray("parts")
                ?: throw IllegalStateException("Gemini response missing text parts.")

            val resultBuilder = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("text")) {
                    resultBuilder.append(part.getString("text"))
                }
            }

            val output = resultBuilder.toString().trim()
            if (output.isEmpty()) {
                throw IllegalStateException("Gemini returned an empty text response.")
            }
            output
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * Remote AI text rewriter powered by the Google GenAI SDK.
 *
 * Supports fast, deterministic transcript cleanup with customizable models:
 * - [MODEL_GEMINI_3_7_FLASH]: Default balanced model for high-fidelity rewriting and formatting.
 * - [MODEL_GEMINI_3_5_FLASH_LITE]: Ultra-low-latency model for rapid speech cleanup.
 * - [MODEL_GEMINI_2_5_FLASH]: Multimodal balanced model.
 *
 * @param apiKeyProvider Suspend lambda supplying the Gemini API key (e.g. from SettingsRepository).
 * @param modelProvider Suspend lambda supplying the target Gemini model identifier (default: "gemini-3.7-flash").
 * @param generator Strategy for calling the GenAI API (default: [DefaultGenAiSdkGenerator]).
 */
class GeminiRemoteRewriter(
    private val apiKeyProvider: suspend () -> String?,
    private val modelProvider: suspend () -> String = { MODEL_GEMINI_3_5_FLASH_LITE },
    private val generator: GeminiContentGenerator = DefaultGenAiSdkGenerator()
) : TextRewriter {

    constructor(
        apiKeyProvider: suspend () -> String?,
        model: String,
        generator: GeminiContentGenerator = DefaultGenAiSdkGenerator()
    ) : this(
        apiKeyProvider = apiKeyProvider,
        modelProvider = { model },
        generator = generator
    )

    override val engineName: String = "Gemini Remote"

    override suspend fun isAvailable(): Boolean {
        val key = apiKeyProvider()
        return !key.isNullOrBlank()
    }

    override suspend fun rewrite(text: String, preset: PromptPreset): Result<String> {
        if (text.isBlank()) {
            return Result.success("")
        }

        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Gemini API key is not configured in settings."))
        }

        val activeModel = modelProvider()

        return runCatching {
            val userPrompt = preset.formatUserPrompt(text)
            generator.generate(
                apiKey = apiKey,
                model = activeModel,
                prompt = userPrompt,
                systemInstruction = preset.systemPrompt,
                temperature = preset.temperature
            )
        }
    }

    companion object {
        const val MODEL_GEMINI_3_5_FLASH_LITE = "gemini-3.5-flash-lite"
        const val MODEL_GEMINI_3_FLASH_PREVIEW = "gemini-3-flash-preview"
        const val MODEL_GEMINI_3_1_FLASH_LITE = "gemini-3.1-flash-lite"
        const val MODEL_GEMINI_3_7_FLASH = "gemini-3.7-flash"

        val SUPPORTED_MODELS = listOf(
            MODEL_GEMINI_3_5_FLASH_LITE,
            MODEL_GEMINI_3_FLASH_PREVIEW,
            MODEL_GEMINI_3_1_FLASH_LITE,
            MODEL_GEMINI_3_7_FLASH
        )
    }
}
