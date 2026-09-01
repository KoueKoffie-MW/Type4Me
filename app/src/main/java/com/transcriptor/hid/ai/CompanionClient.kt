package com.transcriptor.hid.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Data model representing the active workstation desktop context captured by the Type4Me companion.
 *
 * @property windowTitle The title bar text of the foreground application window.
 * @property selectedText Text currently highlighted / selected on the workstation (if any).
 * @property processName The name of the foreground process (e.g. "idea64.exe", "code", "bash").
 * @property timestamp Unix epoch millisecond timestamp when the context snapshot was captured.
 */
data class DesktopContext(
    val windowTitle: String,
    val selectedText: String = "",
    val processName: String = "",
    val timestamp: Long = 0L
) {
    /**
     * True if all context fields are empty or blank.
     */
    val isEmpty: Boolean
        get() = windowTitle.isBlank() && selectedText.isBlank() && processName.isBlank()

    companion object {
        val EMPTY = DesktopContext(windowTitle = "", selectedText = "", processName = "", timestamp = 0L)

        private fun extractString(json: String, vararg keys: String): String {
            for (key in keys) {
                val pattern = Regex(""""$key"\s*:\s*"((?:[^"\\]|\\.)*)"""")
                val match = pattern.find(json)
                if (match != null) {
                    return match.groupValues[1]
                        .replace("\\\"", "\"")
                        .replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\t", "\t")
                        .replace("\\\\", "\\")
                }
            }
            return ""
        }

        private fun extractLong(json: String, key: String): Long {
            val pattern = Regex(""""$key"\s*:\s*(\d+)""")
            return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        }

        /**
         * Parses a [DesktopContext] from a JSON string, supporting both snake_case and camelCase keys.
         */
        fun fromJson(jsonStr: String): DesktopContext {
            val title = extractString(jsonStr, "window_title", "windowTitle")
            val selection = extractString(jsonStr, "selected_text", "selectedText")
            val process = extractString(jsonStr, "process_name", "processName")
            val ts = extractLong(jsonStr, "timestamp")

            return DesktopContext(
                windowTitle = title,
                selectedText = selection,
                processName = process,
                timestamp = ts
            )
        }
    }
}

/**
 * Data model for the companion server health status.
 */
data class CompanionHealth(
    val status: String,
    val version: String
) {
    val isOk: Boolean
        get() = status.equals("ok", ignoreCase = true)

    companion object {
        private fun extractString(json: String, key: String): String {
            val pattern = Regex(""""$key"\s*:\s*"((?:[^"\\]|\\.)*)"""")
            val match = pattern.find(json)
            return match?.groupValues?.get(1) ?: ""
        }

        fun fromJson(jsonStr: String): CompanionHealth {
            val status = extractString(jsonStr, "status").ifBlank { "unknown" }
            val version = extractString(jsonStr, "version").ifBlank { "unknown" }
            return CompanionHealth(
                status = status,
                version = version
            )
        }
    }
}

/**
 * Pluggable connection factory to enable clean unit testing and custom network configuration.
 */
fun interface HttpConnectionFactory {
    fun openConnection(url: URL): HttpURLConnection
}

/**
 * Default connection factory opening standard [HttpURLConnection] instances.
 */
object DefaultHttpConnectionFactory : HttpConnectionFactory {
    override fun openConnection(url: URL): HttpURLConnection {
        return url.openConnection() as HttpURLConnection
    }
}

/**
 * Android client for querying the Type4Me Desktop Context Companion (`tools/companion/type4me_companion.py` / `.ps1`).
 *
 * Provides fast, low-latency ingestion of workstation foreground window title, process name, and selection
 * to enrich Gemini / LiteRT prompt engineering on mobile.
 *
 * **Dual-Tier Air-Gap Guarantee:**
 * If the companion is disabled, offline, or unreachable, [fetchContextOrNull] and [enrichSystemInstruction]
 * immediately and seamlessly fall back to pure voice transcription with 100% air-gap security and zero UI interruption.
 *
 * @param timeoutMs Timeout in milliseconds for connect and read operations (default: 1000ms).
 * @param connectionFactory Connection factory for opening HTTP connections.
 */
class CompanionClient(
    private val timeoutMs: Int = DEFAULT_TIMEOUT_MS,
    private val connectionFactory: HttpConnectionFactory = DefaultHttpConnectionFactory
) {

    /**
     * Queries the companion `GET /context` endpoint.
     *
     * @param hostIp Host IP address or hostname of the workstation (e.g. "192.168.1.50" or "localhost").
     * @param port TCP port companion is listening on (default: 8765).
     * @param token Optional bearer token if companion was started with `--token`.
     * @return [Result] containing [DesktopContext] on success, or an exception on error/timeout.
     */
    suspend fun fetchActiveContext(
        hostIp: String,
        port: Int = DEFAULT_PORT,
        token: String? = null
    ): Result<DesktopContext> = withContext(Dispatchers.IO) {
        if (hostIp.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Host IP address cannot be blank."))
        }

        val endpoint = "http://$hostIp:$port/context"
        runCatching {
            val url = URL(endpoint)
            val connection = connectionFactory.openConnection(url).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                doInput = true
                setRequestProperty("Accept", "application/json")
                if (!token.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            try {
                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: throw IllegalStateException("HTTP $responseCode from Companion at $endpoint")
                }

                val body = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }

                if (responseCode !in 200..299) {
                    throw IllegalStateException("Companion returned HTTP $responseCode: $body")
                }

                DesktopContext.fromJson(body)
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * Queries the companion `GET /health` endpoint to verify connectivity and version compatibility.
     */
    suspend fun checkHealth(
        hostIp: String,
        port: Int = DEFAULT_PORT,
        token: String? = null
    ): Result<CompanionHealth> = withContext(Dispatchers.IO) {
        if (hostIp.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Host IP address cannot be blank."))
        }

        val endpoint = "http://$hostIp:$port/health"
        runCatching {
            val url = URL(endpoint)
            val connection = connectionFactory.openConnection(url).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                doInput = true
                setRequestProperty("Accept", "application/json")
                if (!token.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            try {
                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: throw IllegalStateException("HTTP $responseCode from Companion health check")
                }

                val body = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }

                if (responseCode !in 200..299) {
                    throw IllegalStateException("Companion health check returned HTTP $responseCode: $body")
                }

                CompanionHealth.fromJson(body)
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * Quick boolean liveness check for the desktop companion.
     */
    suspend fun isReachable(hostIp: String, port: Int = DEFAULT_PORT): Boolean {
        if (hostIp.isBlank()) return false
        return checkHealth(hostIp, port).map { it.isOk }.getOrDefault(false)
    }

    /**
     * Safe, non-throwing companion context query with automatic fallback to null on error/timeout.
     * Guaranteed never to throw an exception or block the voice dictation pipeline.
     */
    suspend fun fetchContextOrNull(
        hostIp: String?,
        port: Int = DEFAULT_PORT,
        token: String? = null
    ): DesktopContext? {
        if (hostIp.isNullOrBlank()) return null
        return fetchActiveContext(hostIp, port, token).getOrNull()
    }

    companion object {
        const val DEFAULT_PORT = 8765
        const val DEFAULT_TIMEOUT_MS = 1000

        /**
         * Injects active workstation context into a base system prompt instruction for Gemini or LiteRT.
         *
         * If [context] is null or empty, returns [baseSystemPrompt] completely unmodified.
         */
        fun formatSystemPromptWithContext(baseSystemPrompt: String, context: DesktopContext?): String {
            if (context == null || context.isEmpty) {
                return baseSystemPrompt
            }

            val contextBuilder = StringBuilder()
            contextBuilder.append("\n\n[ACTIVE DESKTOP CONTEXT]\n")
            if (context.processName.isNotBlank()) {
                contextBuilder.append("- Active Application: ").append(context.processName).append("\n")
            }
            if (context.windowTitle.isNotBlank()) {
                contextBuilder.append("- Window Title: ").append(context.windowTitle).append("\n")
            }
            if (context.selectedText.isNotBlank()) {
                contextBuilder.append("- Selected Host Text: ").append(context.selectedText).append("\n")
            }
            contextBuilder.append("Use this desktop context to resolve ambiguous terminology, file paths, variables, and IDE context if relevant.")

            return baseSystemPrompt + contextBuilder.toString()
        }

        /**
         * Enriches system instruction with desktop context (alias for [formatSystemPromptWithContext]).
         */
        fun enrichSystemInstruction(systemInstruction: String, context: DesktopContext?): String {
            return formatSystemPromptWithContext(systemInstruction, context)
        }

        /**
         * Formats the user prompt using [preset] and optional context.
         */
        fun formatUserPromptWithContext(
            rawText: String,
            preset: PromptPreset,
            context: DesktopContext? = null
        ): String {
            return preset.formatUserPrompt(rawText)
        }
    }
}
