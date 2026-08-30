package com.transcriptor.hid.data

import com.transcriptor.hid.engine.KeyLayout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        settingsRepository = InMemorySettingsRepository()
    }

    @Test
    fun testDefaultSettings() = runBlocking {
        assertEquals(KeyLayout.GERMAN_QWERTZ, settingsRepository.keyLayout.first())
        assertEquals(8L, settingsRepository.typingDelayMs.first())
        assertFalse(settingsRepository.liveDiffModeEnabled.first())
        assertNull(settingsRepository.apiKey.first())
        assertEquals("gemini-3.5-flash-lite", settingsRepository.selectedModel.first())
        assertEquals(1L, settingsRepository.activePresetId.first())
    }

    @Test
    fun testSetKeyLayout() = runBlocking {
        settingsRepository.setKeyLayout(KeyLayout.US_QWERTY)
        assertEquals(KeyLayout.US_QWERTY, settingsRepository.keyLayout.first())

        settingsRepository.setKeyLayout(KeyLayout.GERMAN_QWERTZ)
        assertEquals(KeyLayout.GERMAN_QWERTZ, settingsRepository.keyLayout.first())
    }

    @Test
    fun testSetTypingDelayMs() = runBlocking {
        settingsRepository.setTypingDelayMs(5L)
        assertEquals(5L, settingsRepository.typingDelayMs.first())

        settingsRepository.setTypingDelayMs(10L)
        assertEquals(10L, settingsRepository.typingDelayMs.first())
    }

    @Test
    fun testSetLiveDiffModeEnabled() = runBlocking {
        settingsRepository.setLiveDiffModeEnabled(true)
        assertTrue(settingsRepository.liveDiffModeEnabled.first())

        settingsRepository.setLiveDiffModeEnabled(false)
        assertFalse(settingsRepository.liveDiffModeEnabled.first())
    }

    @Test
    fun testSetApiKey() = runBlocking {
        val testKey = "AIzaSyD-sample-api-key-999"
        settingsRepository.setApiKey(testKey)
        assertEquals(testKey, settingsRepository.apiKey.first())
    }

    @Test
    fun testSetSelectedModel() = runBlocking {
        settingsRepository.setSelectedModel("gemini-3.5-flash-lite")
        assertEquals("gemini-3.5-flash-lite", settingsRepository.selectedModel.first())

        settingsRepository.setSelectedModel("gemini-2.5-flash")
        assertEquals("gemini-2.5-flash", settingsRepository.selectedModel.first())
    }

    @Test
    fun testSetActivePresetId() = runBlocking {
        settingsRepository.setActivePresetId(3L)
        assertEquals(3L, settingsRepository.activePresetId.first())
    }
}
