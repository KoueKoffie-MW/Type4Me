package com.transcriptor.hid.ai

/**
 * Interface contract for AI transcription rewriting engines.
 */
interface TextRewriter {
    /**
     * Identifies the rewriter engine type or model.
     */
    val engineName: String

    /**
     * Checks if the rewriter is configured and available for use (e.g. API key set or model loaded).
     */
    suspend fun isAvailable(): Boolean = true

    /**
     * Rewrites raw transcribed [text] using the specified [preset].
     *
     * @param text Raw spoken transcription text.
     * @param preset The prompt preset containing system instructions and formatting rules.
     * @return [Result] containing the rewritten text on success, or an exception on failure.
     */
    suspend fun rewrite(text: String, preset: PromptPreset): Result<String>
}
