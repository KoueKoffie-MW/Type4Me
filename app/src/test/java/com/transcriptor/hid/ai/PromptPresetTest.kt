package com.transcriptor.hid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptPresetTest {

    @Test
    fun testBuiltInPresetsCollection() {
        val builtIns = PromptPreset.BUILTIN_PRESETS
        assertEquals(4, builtIns.size)

        val cleanPolish = builtIns.find { it.title == "Clean & Polish" }
        assertNotNull(cleanPolish)
        assertTrue(cleanPolish!!.isBuiltIn)
        assertEquals(1L, cleanPolish.id)
        assertEquals(0.2f, cleanPolish.temperature, 0.001f)

        val agenticPrompt = builtIns.find { it.title == "Agentic Prompt" }
        assertNotNull(agenticPrompt)
        assertTrue(agenticPrompt!!.isBuiltIn)
        assertEquals(2L, agenticPrompt.id)
        assertEquals(0.2f, agenticPrompt.temperature, 0.001f)

        val businessGerman = builtIns.find { it.title == "Business German" }
        assertNotNull(businessGerman)
        assertTrue(businessGerman!!.isBuiltIn)
        assertEquals(3L, businessGerman.id)
        assertEquals(0.2f, businessGerman.temperature, 0.001f)

        val technicalCode = builtIns.find { it.title == "Technical Code Comments" }
        assertNotNull(technicalCode)
        assertTrue(technicalCode!!.isBuiltIn)
        assertEquals(4L, technicalCode.id)
        assertEquals(0.2f, technicalCode.temperature, 0.001f)

        assertEquals(PromptPreset.CLEAN_AND_POLISH, PromptPreset.DEFAULT_PRESET)
    }

    @Test
    fun testCleanAndPolishPresetPromptInstructions() {
        val preset = PromptPreset.CLEAN_AND_POLISH
        assertTrue(preset.systemPrompt.contains("expert speech transcription editor"))
        assertTrue(preset.systemPrompt.contains("disfluencies"))
        assertTrue(preset.systemPrompt.contains("filler words"))
        assertTrue(preset.systemPrompt.contains("ONLY the polished text"))
    }

    @Test
    fun testBusinessGermanPresetPromptInstructions() {
        val preset = PromptPreset.BUSINESS_GERMAN
        assertTrue(preset.systemPrompt.contains("geschäftliche Korrespondenz"))
        assertTrue(preset.systemPrompt.contains("Höflichkeitsform 'Sie'"))
        assertTrue(preset.systemPrompt.contains("AUSSCHLIESSLICH den überarbeiteten Text"))
    }

    @Test
    fun testTechnicalCodeCommentsPresetPromptInstructions() {
        val preset = PromptPreset.TECHNICAL_CODE_COMMENTS
        assertTrue(preset.systemPrompt.contains("senior software architect"))
        assertTrue(preset.systemPrompt.contains("code comments"))
        assertTrue(preset.systemPrompt.contains("docstrings"))
        assertTrue(preset.systemPrompt.contains("imperative mood"))
    }

    @Test
    fun testDefaultFormatUserPrompt() {
        val preset = PromptPreset.CLEAN_AND_POLISH
        val input = "um basically we have a null pointer exception here"
        val formatted = preset.formatUserPrompt(input)
        assertEquals(input, formatted)
    }

    @Test
    fun testCustomTemplateFormatUserPrompt() {
        val customPreset = PromptPreset(
            id = 10L,
            title = "Bug Summary",
            description = "Summarizes bug reports",
            systemPrompt = "You are a QA lead.",
            userPromptTemplate = "Please extract the root cause from:\n{INPUT_TEXT}\nProvide actionable bullets."
        )

        val input = "App crashes on startup due to missing database table."
        val formatted = customPreset.formatUserPrompt(input)
        val expected = "Please extract the root cause from:\nApp crashes on startup due to missing database table.\nProvide actionable bullets."
        assertEquals(expected, formatted)
    }

    @Test
    fun testCustomTemplateWithoutPlaceholderAppendsInput() {
        val customPreset = PromptPreset(
            id = 11L,
            title = "Translate to Afrikaans",
            description = "Translates input",
            systemPrompt = "You are a translator.",
            userPromptTemplate = "Translate the following to Afrikaans:"
        )

        val input = "Good morning everyone."
        val formatted = customPreset.formatUserPrompt(input)
        assertEquals("Translate the following to Afrikaans:\n\nGood morning everyone.", formatted)
    }

    @Test
    fun testCustomPresetCreation() {
        val customPreset = PromptPreset(
            id = 0L,
            title = "Meeting Minutes",
            description = "Formats voice dictation into markdown meeting notes",
            systemPrompt = "Convert conversation into structured Markdown notes.",
            isBuiltIn = false,
            temperature = 0.1f
        )

        assertFalse(customPreset.isBuiltIn)
        assertEquals("Meeting Minutes", customPreset.title)
        assertEquals(0.1f, customPreset.temperature, 0.001f)
    }
}
