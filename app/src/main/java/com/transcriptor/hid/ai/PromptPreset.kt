package com.transcriptor.hid.ai

/**
 * Data model representing an AI prompt preset for transcription rewriting.
 *
 * @property id Unique identifier for the preset (0 for unpersisted custom presets).
 * @property title User-facing title for the preset (e.g., "Clean & Polish").
 * @property description Brief summary of what the preset does.
 * @property systemPrompt The system instruction sent to the LLM.
 * @property isBuiltIn True if this is an immutable system preset, false if user-created.
 * @property temperature Sampling temperature (low temperature = deterministic text cleanup).
 * @property userPromptTemplate Template for formatting user input (default "{INPUT_TEXT}").
 */
data class PromptPreset(
    val id: Long = 0L,
    val title: String,
    val description: String,
    val systemPrompt: String,
    val isBuiltIn: Boolean = false,
    val temperature: Float = 0.2f,
    val userPromptTemplate: String = "{INPUT_TEXT}"
) {
    /**
     * Formats the user input text using [userPromptTemplate].
     */
    fun formatUserPrompt(input: String): String {
        return if (userPromptTemplate.contains("{INPUT_TEXT}")) {
            userPromptTemplate.replace("{INPUT_TEXT}", input)
        } else {
            "$userPromptTemplate\n\n$input"
        }
    }

    companion object {
        const val TEMPLATE_PLACEHOLDER = "{INPUT_TEXT}"

        val CLEAN_AND_POLISH = PromptPreset(
            id = 1L,
            title = "Clean & Polish",
            description = "Fixes grammar, removes filler words, and cleans up raw speech.",
            systemPrompt = "You are an expert speech transcription editor. Your task is to clean up raw spoken transcripts. " +
                "Fix grammar, punctuation, capitalization, and spelling. Remove speech disfluencies, filler words (um, uh, like, you know, ehm, halt, quasi, etc.), " +
                "stuttering, and accidental word repetitions. Preserve the original language (German, English, Afrikaans, etc.), meaning, and tone. " +
                "Output ONLY the polished text without any conversational filler, explanation, or preambles.",
            isBuiltIn = true,
            temperature = 0.2f
        )

        val AGENTIC_PROMPT_ENGINEER = PromptPreset(
            id = 2L,
            title = "Agentic Prompt",
            description = "Structures raw dictated thoughts into high-precision, action-oriented agent prompts.",
            systemPrompt = "You are an expert Prompt Engineer for autonomous AI agents (like Antigravity, Claude, or Hermes). " +
                "Your task is to analyze my raw, dictated text and restructure it into a precise, action-oriented prompt that the agent can execute effectively.\n\n" +
                "Rewrite this into an optimized English prompt using the following strict structure:\n" +
                "1. CONTEXT: A brief summary of the situation or background.\n" +
                "2. OBJECTIVE: The specific, measurable goal the agent must achieve.\n" +
                "3. CONSTRAINTS & RULES: Any technical limitations (e.g., specific libraries, coding standards, tone).\n" +
                "4. REQUIRED OUTPUT FORMAT: How the final answer should be structured (e.g., code blocks, JSON, bullet points).\n\n" +
                "Output ONLY the optimized prompt. Do not include any conversational filler, introductions, or conclusions.",
            userPromptTemplate = "Here is the dictated text:\n<text>\n{INPUT_TEXT}\n</text>",
            isBuiltIn = true,
            temperature = 0.2f
        )

        val BUSINESS_GERMAN = PromptPreset(
            id = 3L,
            title = "Business German",
            description = "Transforms spoken German into formal, professional business correspondence (Sie-Form).",
            systemPrompt = "Du bist ein professioneller Assistent für geschäftliche Korrespondenz auf Deutsch. " +
                "Wandle den diktierten Text in elegantes, präzises und fehlerfreies Geschäftsdeutsch (Höflichkeitsform 'Sie') um. " +
                "Korrigiere Grammatik, Zeichensetzung und Rechtschreibung. Formuliere Sätze klar und professionell, ohne den sachlichen Inhalt zu verändern. " +
                "Gib AUSSCHLIESSLICH den überarbeiteten Text ohne Einleitung oder Begleitkommentare aus.",
            isBuiltIn = true,
            temperature = 0.2f
        )

        val TECHNICAL_CODE_COMMENTS = PromptPreset(
            id = 4L,
            title = "Technical Code Comments",
            description = "Converts spoken explanations into standard technical docstrings and code comments.",
            systemPrompt = "You are a senior software architect. Convert the spoken developer explanation into clean, concise, " +
                "standard technical documentation, code comments, or docstrings (supporting Javadoc, KDoc, Python docstrings, or MATLAB). " +
                "Use clear technical terminology, standard comment formatting (e.g. //, /** */, #, %%), and imperative mood. " +
                "Output ONLY the comment text without any surrounding explanations.",
            isBuiltIn = true,
            temperature = 0.2f
        )

        val BUILTIN_PRESETS: List<PromptPreset> = listOf(
            CLEAN_AND_POLISH,
            AGENTIC_PROMPT_ENGINEER,
            BUSINESS_GERMAN,
            TECHNICAL_CODE_COMMENTS
        )

        val DEFAULT_PRESET: PromptPreset = CLEAN_AND_POLISH
    }
}
