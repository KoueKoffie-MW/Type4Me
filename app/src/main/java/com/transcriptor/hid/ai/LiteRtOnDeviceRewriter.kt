package com.transcriptor.hid.ai

import java.io.File

/**
 * Functional interface for executing on-device LiteRT / MediaPipe GenAI inference.
 */
fun interface LiteRtInferenceEngine {
    suspend fun generate(formattedPrompt: String): String
}

/**
 * On-device AI text rewriter contract using Google LiteRT (MediaPipe GenAI / Gemma).
 *
 * Provides offline, zero-latency, private transcription rewriting on device NPUs/CPUs.
 *
 * @property modelFile File pointing to the quantised model weights (.bin or .task).
 * @property inferenceEngine Engine implementing the LiteRT-LM C++/JNI inference loop.
 */
class LiteRtOnDeviceRewriter(
    val modelFile: File? = null,
    private val inferenceEngine: LiteRtInferenceEngine? = null
) : TextRewriter {

    override val engineName: String = "LiteRT On-Device (Gemma)"

    override suspend fun isAvailable(): Boolean {
        return modelFile != null && modelFile.exists() && modelFile.length() > 0
    }

    /**
     * Formats the system instructions and user transcript into standard Gemma turn format.
     */
    fun formatPrompt(text: String, preset: PromptPreset): String {
        val userContent = preset.formatUserPrompt(text)
        return buildString {
            append("<start_of_turn>system\n")
            append(preset.systemPrompt)
            append("<end_of_turn>\n")
            append("<start_of_turn>user\n")
            append(userContent)
            append("<end_of_turn>\n")
            append("<start_of_turn>model\n")
        }
    }

    override suspend fun rewrite(text: String, preset: PromptPreset): Result<String> {
        if (text.isBlank()) {
            return Result.success("")
        }

        if (!isAvailable() && inferenceEngine == null) {
            return Result.failure(
                IllegalStateException(
                    "LiteRT on-device model weights are not installed. Expected model at: ${modelFile?.absolutePath ?: "unspecified"}"
                )
            )
        }

        return runCatching {
            val formattedPrompt = formatPrompt(text, preset)
            inferenceEngine?.generate(formattedPrompt)?.trim()
                ?: throw IllegalStateException("LiteRT native inference runtime is not initialized.")
        }
    }

    companion object {
        const val DEFAULT_MODEL_NAME = "gemma-2b-it-cpu-int4.bin"
    }
}
