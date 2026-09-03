package com.transcriptor.hid.ui

import com.transcriptor.hid.ai.PromptPreset
import com.transcriptor.hid.ai.TextRewriter
import com.transcriptor.hid.data.InMemorySettingsRepository
import com.transcriptor.hid.data.PresetRepository
import com.transcriptor.hid.engine.HidKeyStroke
import com.transcriptor.hid.engine.KeyLayout
import com.transcriptor.hid.engine.KeystrokeDispatcher
import com.transcriptor.hid.service.HidConnectionState
import com.transcriptor.hid.service.HidTransport
import com.transcriptor.hid.service.HostLedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var settingsRepository: InMemorySettingsRepository
    private lateinit var presetRepository: FakePresetRepository
    private lateinit var textRewriter: FakeTextRewriter
    private lateinit var keystrokeDispatcher: FakeKeystrokeDispatcher
    private lateinit var hidTransport: FakeHidTransport
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        settingsRepository = InMemorySettingsRepository(
            initialKeyLayout = KeyLayout.GERMAN_QWERTZ,
            initialTypingDelayMs = 8L,
            initialLiveDiffEnabled = false
        )
        presetRepository = FakePresetRepository(
            initialPresets = PromptPreset.BUILTIN_PRESETS
        )
        textRewriter = FakeTextRewriter()
        keystrokeDispatcher = FakeKeystrokeDispatcher()
        hidTransport = FakeHidTransport()

        viewModel = MainViewModel(
            settingsRepository = settingsRepository,
            presetRepository = presetRepository,
            textRewriter = textRewriter,
            keystrokeDispatcher = keystrokeDispatcher,
            hidTransport = hidTransport,
            externalScope = testScope
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState() {
        val state = viewModel.uiState.value
        assertEquals("", state.transcriptionText)
        assertEquals(KeyLayout.GERMAN_QWERTZ, state.activeLayout)
        assertEquals(HidConnectionState.DISCONNECTED, state.connectionState)
        assertNull(state.connectedDeviceName)
        assertFalse(state.liveDiffEnabled)
        assertEquals(8L, state.typingDelayMs)
        assertEquals(4, state.presets.size)
        assertEquals(PromptPreset.CLEAN_AND_POLISH.id, state.selectedPreset?.id)
        assertFalse(state.isAiRewriting)
        assertFalse(state.isTransmitting)
        assertNull(state.errorMessage)
        assertTrue(state.undoHistory.isEmpty())
        assertFalse(state.isPresetDialogOpen)
    }

    @Test
    fun testTextChangedUpdatesCanvasAndHistory() {
        viewModel.onIntent(MainUiIntent.TextChanged("First draft"))
        assertEquals("First draft", viewModel.uiState.value.transcriptionText)
        assertEquals(0, viewModel.uiState.value.undoHistory.size)

        viewModel.onIntent(MainUiIntent.TextChanged("Second draft"))
        assertEquals("Second draft", viewModel.uiState.value.transcriptionText)
        assertEquals(listOf("First draft"), viewModel.uiState.value.undoHistory)
    }

    @Test
    fun testLayoutSelectionUpdatesStateAndPersists() = runTest(testDispatcher) {
        viewModel.onIntent(MainUiIntent.LayoutSelected(KeyLayout.US_QWERTY))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(KeyLayout.US_QWERTY, viewModel.uiState.value.activeLayout)
        assertEquals(KeyLayout.US_QWERTY, settingsRepository.keyLayout.first())
    }

    @Test
    fun testLiveDiffToggleUpdatesStateAndPersists() = runTest(testDispatcher) {
        viewModel.onIntent(MainUiIntent.LiveDiffToggled(true))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.liveDiffEnabled)
        assertTrue(settingsRepository.liveDiffModeEnabled.first())
    }

    @Test
    fun testDelayChangedUpdatesStateAndPersists() = runTest(testDispatcher) {
        viewModel.onIntent(MainUiIntent.DelayChanged(6L))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(6L, viewModel.uiState.value.typingDelayMs)
        assertEquals(6L, settingsRepository.typingDelayMs.first())
    }

    @Test
    fun testPresetSelectionUpdatesStateAndSettings() = runTest(testDispatcher) {
        val businessGerman = PromptPreset.BUSINESS_GERMAN
        viewModel.onIntent(MainUiIntent.PresetSelected(businessGerman))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(businessGerman.id, viewModel.uiState.value.selectedPreset?.id)
        assertEquals(businessGerman.id, settingsRepository.activePresetId.first())
    }

    @Test
    fun testTriggerAiRewriteSuccessTransitionsAndRecordsUndo() = runTest(testDispatcher) {
        val rawSpeech = "uh so basically we need to fix the bug"
        val polished = "We need to fix the bug."
        textRewriter.rewriteTransform = { _, _ -> polished }

        viewModel.onIntent(MainUiIntent.TextChanged(rawSpeech))
        viewModel.onIntent(MainUiIntent.TriggerAiRewrite)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAiRewriting)
        assertEquals(polished, state.transcriptionText)
        assertEquals(listOf(rawSpeech), state.undoHistory)
        assertNull(state.errorMessage)
    }

    @Test
    fun testTriggerAiRewriteFailureSetsErrorMessage() = runTest(testDispatcher) {
        textRewriter.shouldFail = true
        textRewriter.failureMessage = "Quota exceeded 429"

        viewModel.onIntent(MainUiIntent.TextChanged("Some text"))
        viewModel.onIntent(MainUiIntent.TriggerAiRewrite)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAiRewriting)
        assertEquals("Some text", state.transcriptionText)
        assertEquals("Quota exceeded 429", state.errorMessage)
    }

    @Test
    fun testTriggerAiRewriteBlankTextSetsError() {
        viewModel.onIntent(MainUiIntent.TextChanged("   "))
        viewModel.onIntent(MainUiIntent.TriggerAiRewrite)

        assertEquals("No text available to rewrite.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isAiRewriting)
    }

    @Test
    fun testSendBufferedKeystrokesWhenConnected() = runTest(testDispatcher) {
        hidTransport.connectionState.value = HidConnectionState.CONNECTED
        hidTransport.connectedDeviceName.value = "Workstation-PC"
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(MainUiIntent.TextChanged("Hello PC"))
        viewModel.onIntent(MainUiIntent.SendBufferedKeystrokes)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, keystrokeDispatcher.burstDispatches.size)
        assertEquals("Hello PC" to 8L, keystrokeDispatcher.burstDispatches[0])
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isTransmitting)
    }

    @Test
    fun testSendBufferedKeystrokesWhenDisconnectedSetsError() {
        hidTransport.connectionState.value = HidConnectionState.DISCONNECTED

        viewModel.onIntent(MainUiIntent.TextChanged("Hello PC"))
        viewModel.onIntent(MainUiIntent.SendBufferedKeystrokes)

        assertEquals(0, keystrokeDispatcher.burstDispatches.size)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Host PC is not connected"))
    }

    @Test
    fun testClearTextEmptiesCanvasAndPushesUndo() {
        viewModel.onIntent(MainUiIntent.TextChanged("Text before clear"))
        viewModel.onIntent(MainUiIntent.ClearText)

        assertEquals("", viewModel.uiState.value.transcriptionText)
        assertEquals(listOf("Text before clear"), viewModel.uiState.value.undoHistory)
        assertEquals("", keystrokeDispatcher.currentHostText.value)
    }

    @Test
    fun testUndoRestoresPreviousText() {
        viewModel.onIntent(MainUiIntent.TextChanged("Version 1"))
        viewModel.onIntent(MainUiIntent.TextChanged("Version 2"))
        viewModel.onIntent(MainUiIntent.TextChanged("Version 3"))

        viewModel.onIntent(MainUiIntent.UndoText)
        assertEquals("Version 2", viewModel.uiState.value.transcriptionText)

        viewModel.onIntent(MainUiIntent.UndoText)
        assertEquals("Version 1", viewModel.uiState.value.transcriptionText)
    }

    @Test
    fun testSaveCustomPresetPersistsAndSelects() = runTest(testDispatcher) {
        viewModel.onIntent(
            MainUiIntent.SaveCustomPreset(
                title = "Markdown Formatter",
                prompt = "Convert spoken input to markdown tables.",
                description = "Custom markdown converter"
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Markdown Formatter", state.selectedPreset?.title)
        assertFalse(state.selectedPreset!!.isBuiltIn)
        assertFalse(state.isPresetDialogOpen)
        assertEquals(5, state.presets.size)
    }

    @Test
    fun testSaveCustomPresetValidationFailsOnBlankInput() {
        viewModel.onIntent(
            MainUiIntent.SaveCustomPreset(
                title = "  ",
                prompt = ""
            )
        )
        assertEquals("Preset title and prompt cannot be blank.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testDeleteCustomPresetRemovesAndRevertsSelection() = runTest(testDispatcher) {
        viewModel.onIntent(
            MainUiIntent.SaveCustomPreset(
                title = "Temp Preset",
                prompt = "Temp prompt"
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val customPreset = viewModel.uiState.value.selectedPreset!!
        assertEquals("Temp Preset", customPreset.title)
        assertEquals(5, viewModel.uiState.value.presets.size)

        viewModel.onIntent(MainUiIntent.DeleteCustomPreset(customPreset))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(4, state.presets.size)
        assertEquals(PromptPreset.CLEAN_AND_POLISH.id, state.selectedPreset?.id)
    }

    @Test
    fun testCannotDeleteBuiltInPreset() = runTest(testDispatcher) {
        val builtIn = PromptPreset.CLEAN_AND_POLISH
        viewModel.onIntent(MainUiIntent.DeleteCustomPreset(builtIn))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Cannot delete built-in preset"))
        assertEquals(4, viewModel.uiState.value.presets.size)
    }

    @Test
    fun testLiveDiffTransmissionDuringTextTyping() = runTest(testDispatcher) {
        hidTransport.connectionState.value = HidConnectionState.CONNECTED
        viewModel.onIntent(MainUiIntent.LiveDiffToggled(true))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(MainUiIntent.TextChanged("Hello"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, keystrokeDispatcher.liveDiffDispatches.size)
        assertEquals("Hello" to 8L, keystrokeDispatcher.liveDiffDispatches[0])

        viewModel.onIntent(MainUiIntent.TextChanged("Hello World"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, keystrokeDispatcher.liveDiffDispatches.size)
        assertEquals("Hello World" to 8L, keystrokeDispatcher.liveDiffDispatches[1])
    }

    @Test
    fun testOpenAndClosePresetDialog() {
        viewModel.onIntent(MainUiIntent.OpenPresetDialog(PromptPreset.BUSINESS_GERMAN))
        assertTrue(viewModel.uiState.value.isPresetDialogOpen)
        assertEquals(PromptPreset.BUSINESS_GERMAN.id, viewModel.uiState.value.editingPreset?.id)

        viewModel.onIntent(MainUiIntent.ClosePresetDialog)
        assertFalse(viewModel.uiState.value.isPresetDialogOpen)
        assertNull(viewModel.uiState.value.editingPreset)
    }

    @Test
    fun testDismissError() {
        viewModel.onIntent(MainUiIntent.TextChanged("  "))
        viewModel.onIntent(MainUiIntent.TriggerAiRewrite)
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onIntent(MainUiIntent.DismissError)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testTelemetryCalculations() {
        viewModel.onIntent(MainUiIntent.TextChanged("Das ist ein toller Sprachtest."))
        val state = viewModel.uiState.value
        assertEquals(5, state.wordCount)
        assertEquals(30, state.charCount)
        assertTrue(state.canClear)
        assertTrue(state.canRewrite)
    }

    @Test
    fun testInitialSettingsLoadedFromRepository() = runTest(testDispatcher) {
        val customSettings = InMemorySettingsRepository(
            initialApiKey = "AIzaSyInitialPresetKey",
            initialModel = "gemini-3.5-flash-lite"
        )
        val customVm = MainViewModel(
            settingsRepository = customSettings,
            presetRepository = presetRepository,
            textRewriter = textRewriter,
            keystrokeDispatcher = keystrokeDispatcher,
            hidTransport = hidTransport,
            externalScope = testScope
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = customVm.uiState.value
        assertFalse(state.isSettingsOpen)
        assertEquals("AIzaSyInitialPresetKey", state.apiKeyInput)
        assertEquals("gemini-3.5-flash-lite", state.selectedModel)
        assertFalse(state.isApiKeyVisible)
        assertFalse(state.isTestingApiKey)
        assertNull(state.settingsFeedbackMessage)
    }

    @Test
    fun testOpenAndCloseSettingsDialog() {
        viewModel.onIntent(MainUiIntent.OpenSettings)
        assertTrue(viewModel.uiState.value.isSettingsOpen)
        assertNull(viewModel.uiState.value.settingsFeedbackMessage)
        assertNull(viewModel.uiState.value.isApiKeyValid)

        viewModel.onIntent(MainUiIntent.CloseSettings)
        assertFalse(viewModel.uiState.value.isSettingsOpen)
    }

    @Test
    fun testUpdateApiKeyUpdatesStateAndResetsFeedback() {
        viewModel.onIntent(MainUiIntent.UpdateApiKey("AIzaSyNewTestKey123"))
        assertEquals("AIzaSyNewTestKey123", viewModel.uiState.value.apiKeyInput)
        assertNull(viewModel.uiState.value.settingsFeedbackMessage)
        assertNull(viewModel.uiState.value.isApiKeyValid)
    }

    @Test
    fun testToggleApiKeyVisibility() {
        assertFalse(viewModel.uiState.value.isApiKeyVisible)

        viewModel.onIntent(MainUiIntent.ToggleApiKeyVisibility)
        assertTrue(viewModel.uiState.value.isApiKeyVisible)

        viewModel.onIntent(MainUiIntent.ToggleApiKeyVisibility)
        assertFalse(viewModel.uiState.value.isApiKeyVisible)
    }

    @Test
    fun testSelectModelUpdatesState() {
        viewModel.onIntent(MainUiIntent.SelectModel("gemini-2.5-flash"))
        assertEquals("gemini-2.5-flash", viewModel.uiState.value.selectedModel)
    }

    @Test
    fun testSaveSettingsPersistsToRepositoryAndClosesDialog() = runTest(testDispatcher) {
        viewModel.onIntent(MainUiIntent.OpenSettings)
        viewModel.onIntent(MainUiIntent.UpdateApiKey("AIzaSyPersistedKey99"))
        viewModel.onIntent(MainUiIntent.SelectModel("gemini-3.5-flash-lite"))
        viewModel.onIntent(MainUiIntent.UpdateSpeakerAccent("Afrikaans"))
        viewModel.onIntent(MainUiIntent.UpdateSpokenLanguage("English"))
        viewModel.onIntent(MainUiIntent.SaveSettings)

        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSettingsOpen)
        assertEquals("AIzaSyPersistedKey99", settingsRepository.apiKey.first())
        assertEquals("gemini-3.5-flash-lite", settingsRepository.selectedModel.first())
        assertEquals("Afrikaans", settingsRepository.speakerAccent.first())
        assertEquals("English", settingsRepository.spokenLanguage.first())
        assertEquals("Afrikaans", viewModel.uiState.value.speakerAccent)
        assertEquals("English", viewModel.uiState.value.spokenLanguage)
    }

    @Test
    fun testTestApiKeyValidationBlankKey() = runTest(testDispatcher) {
        viewModel.onIntent(MainUiIntent.UpdateApiKey("   "))
        viewModel.onIntent(MainUiIntent.TestApiKey)

        val state = viewModel.uiState.value
        assertFalse(state.isTestingApiKey)
        assertEquals(false, state.isApiKeyValid)
        assertEquals("API key cannot be blank.", state.settingsFeedbackMessage)
    }

    @Test
    fun testTestApiKeyValidationSuccess() = runTest(testDispatcher) {
        textRewriter.shouldFail = false
        viewModel.onIntent(MainUiIntent.UpdateApiKey("AIzaSyValidKey12345"))
        viewModel.onIntent(MainUiIntent.TestApiKey)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isTestingApiKey)
        assertEquals(true, state.isApiKeyValid)
        assertTrue(state.settingsFeedbackMessage!!.contains("valid", ignoreCase = true))
    }

    @Test
    fun testTestApiKeyValidationFailure() = runTest(testDispatcher) {
        textRewriter.shouldFail = true
        textRewriter.failureMessage = "API_KEY_INVALID: 400 Bad Request"

        viewModel.onIntent(MainUiIntent.UpdateApiKey("AIzaSyInvalidKey"))
        viewModel.onIntent(MainUiIntent.TestApiKey)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isTestingApiKey)
        assertEquals(false, state.isApiKeyValid)
        assertTrue(state.settingsFeedbackMessage!!.contains("Validation failed", ignoreCase = true))
        assertTrue(state.settingsFeedbackMessage!!.contains("API_KEY_INVALID", ignoreCase = true))
    }

    @Test
    fun testLiveDiffRapidTypingDispatchesDeltasConcurrentlySafe() = runTest(testDispatcher) {
        hidTransport.connectionState.value = HidConnectionState.CONNECTED
        viewModel.onIntent(MainUiIntent.LiveDiffToggled(true))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(MainUiIntent.TextChanged("Word1"))
        viewModel.onIntent(MainUiIntent.TextChanged("Word1 Word2"))
        viewModel.onIntent(MainUiIntent.TextChanged("Word1 Word2 Word3"))

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(keystrokeDispatcher.liveDiffDispatches.isNotEmpty())
        assertEquals("Word1 Word2 Word3", keystrokeDispatcher.currentHostText.value)
    }

    @Test
    fun testSwitchModeAndMouseIntents() = runTest {
        assertEquals(AppMode.KEYBOARD, viewModel.uiState.value.activeMode)

        // Switch to Touchpad mode
        viewModel.onIntent(MainUiIntent.SwitchMode(AppMode.TOUCHPAD))
        assertEquals(AppMode.TOUCHPAD, viewModel.uiState.value.activeMode)

        // Switch back to Keyboard mode
        viewModel.onIntent(MainUiIntent.SwitchMode(AppMode.KEYBOARD))
        assertEquals(AppMode.KEYBOARD, viewModel.uiState.value.activeMode)

        // Test mouse movement & click events without throwing
        viewModel.onIntent(MainUiIntent.SendMouseMove(dx = 10, dy = -5))
        viewModel.onIntent(MainUiIntent.SendMouseLeftClick)
        viewModel.onIntent(MainUiIntent.SendMouseRightClick)
        viewModel.onIntent(MainUiIntent.SendMouseMiddleClick)
        viewModel.onIntent(MainUiIntent.SendMouseScroll(wheel = 2))
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun testTriggerMacroWithPromptsOpensPromptDialog() = runTest(testDispatcher) {
        hidTransport.connectionState.value = HidConnectionState.CONNECTED
        testDispatcher.scheduler.advanceUntilIdle()

        val macro = com.transcriptor.hid.data.db.MacroEntity(
            id = 1L,
            title = "Test Macro",
            description = "A macro with prompts",
            categoryId = 1L,
            stepsJson = """[{"type":"prompt_variable","variableName":"commit_msg","promptLabel":"Commit Message","defaultValue":"Initial commit"}]"""
        )

        viewModel.onIntent(MainUiIntent.TriggerMacro(macro))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(macro, state.activePromptMacro)
        assertEquals(1, state.activePrompts.size)
        assertEquals("Commit Message", state.activePrompts[0].label)

        // Submit prompt answers
        viewModel.onIntent(MainUiIntent.SubmitPromptAnswers(mapOf("Commit Message" to "feat: add feature")))
        testDispatcher.scheduler.advanceUntilIdle()

        val postSubmitState = viewModel.uiState.value
        assertNull(postSubmitState.activePromptMacro)
        assertTrue(postSubmitState.activePrompts.isEmpty())
        assertEquals("feat: add feature", keystrokeDispatcher.currentHostText.value)
    }
}

// =============================================================================
// Test Doubles / Fakes
// =============================================================================

private class FakePresetRepository(
    initialPresets: List<PromptPreset> = PromptPreset.BUILTIN_PRESETS
) : PresetRepository {
    private val presets = initialPresets.toMutableList()
    private val _presetsFlow = MutableStateFlow<List<PromptPreset>>(presets.toList())

    override fun getAllPresets(): Flow<List<PromptPreset>> = _presetsFlow.asStateFlow()

    override suspend fun getPresetById(id: Long): PromptPreset? = presets.find { it.id == id }

    override suspend fun insertPreset(preset: PromptPreset): Long {
        val nextId = (presets.maxOfOrNull { it.id } ?: 0L) + 1L
        val saved = preset.copy(id = nextId)
        presets.add(saved)
        _presetsFlow.value = presets.toList()
        return nextId
    }

    override suspend fun updatePreset(preset: PromptPreset) {
        val index = presets.indexOfFirst { it.id == preset.id }
        if (index != -1) {
            presets[index] = preset
            _presetsFlow.value = presets.toList()
        }
    }

    override suspend fun deletePreset(preset: PromptPreset) {
        presets.removeAll { it.id == preset.id }
        _presetsFlow.value = presets.toList()
    }

    override suspend fun ensureBuiltInPresetsSeeded() {
        // no-op for fake
    }
}

private class FakeTextRewriter : TextRewriter {
    override val engineName: String = "FakeRewriter"
    var shouldFail: Boolean = false
    var failureMessage: String = "AI Error"
    var rewriteTransform: (String, PromptPreset) -> String = { text, _ -> "Clean: $text" }

    override suspend fun rewrite(text: String, preset: PromptPreset): Result<String> {
        return if (shouldFail) {
            Result.failure(RuntimeException(failureMessage))
        } else {
            Result.success(rewriteTransform(text, preset))
        }
    }
}

private class FakeKeystrokeDispatcher : KeystrokeDispatcher {
    private val _currentHostText = MutableStateFlow("")
    override val currentHostText: StateFlow<String> = _currentHostText.asStateFlow()

    val burstDispatches = mutableListOf<Pair<String, Long>>()
    val liveDiffDispatches = mutableListOf<Pair<String, Long>>()

    override suspend fun dispatchBurst(text: String, delayMs: Long) {
        burstDispatches.add(text to delayMs)
        _currentHostText.value += text
    }

    override suspend fun dispatchLiveDiff(newHypothesis: String, delayMs: Long) {
        liveDiffDispatches.add(newHypothesis to delayMs)
        _currentHostText.value = newHypothesis
    }

    override suspend fun sendRawKeyStrokes(keyStrokes: List<HidKeyStroke>, delayMs: Long) {}

    override suspend fun streamClipboardToHost(clipText: String, bracketedPaste: Boolean, delayMs: Long) {
        burstDispatches.add(clipText to delayMs)
        _currentHostText.value += clipText
    }

    override fun resetState() {
        _currentHostText.value = ""
    }
}

private class FakeHidTransport : HidTransport {
    override val connectionState = MutableStateFlow(HidConnectionState.DISCONNECTED)
    override val connectedDeviceName = MutableStateFlow<String?>(null)
    override val hostLedState = MutableStateFlow(HostLedState.ALL_OFF)

    override suspend fun initialize(): Boolean = true
    override suspend fun sendKeyboardReport(report: ByteArray): Boolean = true
    override suspend fun sendMouseReport(buttons: Int, dx: Int, dy: Int, wheel: Int): Boolean = true
    override suspend fun disconnect() {
        connectionState.value = HidConnectionState.DISCONNECTED
        connectedDeviceName.value = null
    }
    override fun release() {}
}
