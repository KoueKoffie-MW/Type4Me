package com.transcriptor.hid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transcriptor.hid.ai.GeminiRemoteRewriter
import com.transcriptor.hid.ai.PromptPreset
import com.transcriptor.hid.ai.TextRewriter
import com.transcriptor.hid.data.MacroRepository
import com.transcriptor.hid.data.PairedHostRepository
import com.transcriptor.hid.data.PresetRepository
import com.transcriptor.hid.data.SettingsRepository
import com.transcriptor.hid.data.SnippetRepository
import com.transcriptor.hid.data.db.CategoryEntity
import com.transcriptor.hid.data.db.MacroEntity
import com.transcriptor.hid.data.db.PairedHostEntity
import com.transcriptor.hid.data.db.SnippetEntity
import com.transcriptor.hid.engine.DefaultKeystrokeDispatcher
import com.transcriptor.hid.engine.HidConstants
import com.transcriptor.hid.engine.HidKeyStroke
import com.transcriptor.hid.engine.InterpolationContext
import com.transcriptor.hid.engine.KeyLayout
import com.transcriptor.hid.engine.KeymapTranslator
import com.transcriptor.hid.engine.KeystrokeDispatcher
import com.transcriptor.hid.engine.MacroRunner
import com.transcriptor.hid.engine.VariableParser
import com.transcriptor.hid.audio.PttAudioEngine
import com.transcriptor.hid.motion.GyroAirMouseEngine
import com.transcriptor.hid.service.BluetoothConnectionWatchdog
import com.transcriptor.hid.service.BluetoothHidTransport
import com.transcriptor.hid.service.HidConnectionState
import com.transcriptor.hid.service.HidTransport
import com.transcriptor.hid.service.MultiHostConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Single-screen ViewModel coordinating UI state, voice typing interception,
 * HID transmission, variable interpolation, macro execution, and multi-host switching.
 */
open class MainViewModel(
    val settingsRepository: SettingsRepository,
    val presetRepository: PresetRepository,
    val textRewriter: TextRewriter,
    val keystrokeDispatcher: KeystrokeDispatcher,
    val hidTransport: HidTransport,
    val snippetRepository: SnippetRepository? = null,
    val macroRepository: MacroRepository? = null,
    val pairedHostRepository: PairedHostRepository? = null,
    externalScope: CoroutineScope? = null,
    val pttAudioEngine: PttAudioEngine? = null,
    val gyroAirMouseEngine: GyroAirMouseEngine? = null,
    val connectionWatchdog: BluetoothConnectionWatchdog? = null
) : ViewModel() {

    // Use injected scope or default to ViewModel's viewModelScope
    private val scope: CoroutineScope = externalScope ?: viewModelScope

    private var liveDiffJob: Job? = null

    val macroRunner: MacroRunner = MacroRunner(
        keystrokeDispatcher = keystrokeDispatcher,
        reportSender = { report -> hidTransport.sendKeyboardReport(report) }
    )

    private val _uiState = MutableStateFlow(
        MainUiState(
            presets = PromptPreset.BUILTIN_PRESETS,
            selectedPreset = PromptPreset.DEFAULT_PRESET
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        observeSettingsAndData()
    }

    private fun observeSettingsAndData() {
        scope.launch {
            settingsRepository.keyLayout.collect { layout ->
                _uiState.update { it.copy(activeLayout = layout) }
                (keystrokeDispatcher as? DefaultKeystrokeDispatcher)?.translator =
                    KeymapTranslator.create(layout)
            }
        }

        scope.launch {
            settingsRepository.apiKey.collect { key ->
                _uiState.update { current ->
                    if (!current.isSettingsOpen) {
                        current.copy(apiKeyInput = key ?: "")
                    } else current
                }
            }
        }

        scope.launch {
            settingsRepository.selectedModel.collect { model ->
                _uiState.update { current ->
                    if (!current.isSettingsOpen) {
                        current.copy(selectedModel = model)
                    } else current
                }
            }
        }

        scope.launch {
            settingsRepository.typingDelayMs.collect { delayMs ->
                _uiState.update { it.copy(typingDelayMs = delayMs) }
            }
        }

        scope.launch {
            settingsRepository.speakerAccent.collect { accent ->
                _uiState.update { current ->
                    if (!current.isSettingsOpen) {
                        current.copy(speakerAccent = accent, speakerAccentInput = accent)
                    } else {
                        current.copy(speakerAccent = accent)
                    }
                }
            }
        }

        scope.launch {
            settingsRepository.spokenLanguage.collect { language ->
                _uiState.update { current ->
                    if (!current.isSettingsOpen) {
                        current.copy(spokenLanguage = language, spokenLanguageInput = language)
                    } else {
                        current.copy(spokenLanguage = language)
                    }
                }
            }
        }

        scope.launch {
            settingsRepository.liveDiffModeEnabled.collect { enabled ->
                _uiState.update { it.copy(liveDiffEnabled = enabled) }
            }
        }

        scope.launch {
            presetRepository.getAllPresets().collect { presetList ->
                val available = if (presetList.isNotEmpty()) presetList else PromptPreset.BUILTIN_PRESETS
                _uiState.update { current ->
                    val currentSelection = current.selectedPreset
                    val validSelection = if (currentSelection != null && available.any { it.id == currentSelection.id }) {
                        available.first { it.id == currentSelection.id }
                    } else {
                        available.firstOrNull() ?: PromptPreset.DEFAULT_PRESET
                    }
                    current.copy(
                        presets = available,
                        selectedPreset = validSelection
                    )
                }
            }
        }

        scope.launch {
            hidTransport.connectionState.collect { connState ->
                _uiState.update { it.copy(connectionState = connState) }
            }
        }

        scope.launch {
            hidTransport.connectedDeviceName.collect { deviceName ->
                _uiState.update { it.copy(connectedDeviceName = deviceName) }
            }
        }

        // Observe Multi-Host State
        (hidTransport as? BluetoothHidTransport)?.let { bt ->
            scope.launch {
                bt.multiHostState.collect { mhState ->
                    when (mhState) {
                        is MultiHostConnectionState.Connected -> {
                            _uiState.update { it.copy(activeHost = mhState.activeHost) }
                        }
                        is MultiHostConnectionState.Disconnected -> {
                            _uiState.update { it.copy(activeHost = null) }
                        }
                        is MultiHostConnectionState.Error -> {
                            _uiState.update { it.copy(errorMessage = mhState.message) }
                        }
                        else -> Unit
                    }
                }
            }
        }

        // Observe M3 Snippet & Category & Macro Flows
        snippetRepository?.let { repo ->
            scope.launch {
                repo.getAllCategories().collect { cats ->
                    _uiState.update { it.copy(categories = cats) }
                }
            }

            scope.launch {
                repo.getFavoriteSnippets().collect { favs ->
                    _uiState.update { it.copy(favorites = favs) }
                }
            }

            scope.launch {
                repo.getAllSnippets().collect { allSnippets ->
                    _uiState.update { current ->
                        val filtered = filterSnippets(allSnippets, current.selectedCategoryId, current.snippetsSearchQuery)
                        current.copy(snippets = filtered)
                    }
                }
            }
        }

        macroRepository?.let { repo ->
            scope.launch {
                repo.getAllMacros().collect { macroList ->
                    _uiState.update { it.copy(macros = macroList) }
                }
            }
        }

        pairedHostRepository?.let { repo ->
            scope.launch {
                repo.getAllPairedHosts().collect { hostList ->
                    _uiState.update { it.copy(pairedHosts = hostList) }
                }
            }
        }

        pttAudioEngine?.let { engine ->
            scope.launch {
                engine.audioLevel.collect { level ->
                    _uiState.update { it.copy(audioLevel = level) }
                }
            }
            scope.launch {
                engine.isRecording.collect { rec ->
                    _uiState.update { it.copy(isPttRecording = rec) }
                }
            }
        }

        connectionWatchdog?.let { watchdog ->
            scope.launch {
                watchdog.isReconnecting.collect { recon ->
                    _uiState.update { it.copy(isWatchdogReconnecting = recon) }
                }
            }
            scope.launch {
                watchdog.reconnectAttempts.collect { att ->
                    _uiState.update { it.copy(watchdogAttempts = att) }
                }
            }
        }

        gyroAirMouseEngine?.let { mouse ->
            scope.launch {
                mouse.isAiming.collect { aiming ->
                    _uiState.update { it.copy(isAirMouseAiming = aiming) }
                }
            }
            scope.launch {
                mouse.isAvailable.collect { avail ->
                    _uiState.update { it.copy(isAirMouseAvailable = avail) }
                }
            }
        }
    }

    private fun filterSnippets(
        all: List<SnippetEntity>,
        categoryId: Long?,
        query: String
    ): List<SnippetEntity> {
        return all.filter { snippet ->
            val matchesCat = categoryId == null || snippet.categoryId == categoryId
            val matchesQuery = query.isBlank() ||
                    snippet.title.contains(query, ignoreCase = true) ||
                    snippet.content.contains(query, ignoreCase = true) ||
                    snippet.tags.any { it.contains(query, ignoreCase = true) }
            matchesCat && matchesQuery
        }
    }

    private fun refreshSnippetsFilter() {
        snippetRepository?.let { repo ->
            scope.launch {
                repo.getAllSnippets().collect { all ->
                    _uiState.update { current ->
                        val filtered = filterSnippets(all, current.selectedCategoryId, current.snippetsSearchQuery)
                        current.copy(snippets = filtered)
                    }
                }
            }
        }
    }

    /**
     * Primary entry point for dispatching user actions to the ViewModel.
     */
    fun onIntent(intent: MainUiIntent) {
        processIntent(intent)
    }

    /**
     * Process UI intent and apply corresponding reductions or asynchronous effects.
     */
    fun processIntent(intent: MainUiIntent) {
        when (intent) {
            is MainUiIntent.TextChanged -> handleTextChanged(intent.newText)
            is MainUiIntent.LayoutSelected -> handleLayoutSelected(intent.layout)
            is MainUiIntent.LiveDiffToggled -> handleLiveDiffToggled(intent.enabled)
            is MainUiIntent.DelayChanged -> handleDelayChanged(intent.delayMs)
            is MainUiIntent.PresetSelected -> handlePresetSelected(intent.preset)
            is MainUiIntent.TriggerAiRewrite -> handleTriggerAiRewrite()
            is MainUiIntent.SendBufferedKeystrokes -> handleSendBufferedKeystrokes()
            is MainUiIntent.ClearText -> handleClearText()
            is MainUiIntent.UndoText -> handleUndoText()
            is MainUiIntent.SaveCustomPreset -> handleSaveCustomPreset(intent.title, intent.prompt, intent.description)
            is MainUiIntent.DeleteCustomPreset -> handleDeleteCustomPreset(intent.preset)
            is MainUiIntent.OpenPresetDialog -> _uiState.update { it.copy(isPresetDialogOpen = true, editingPreset = intent.preset) }
            is MainUiIntent.ClosePresetDialog -> _uiState.update { it.copy(isPresetDialogOpen = false, editingPreset = null) }
            is MainUiIntent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
            is MainUiIntent.OpenSettings -> handleOpenSettings()
            is MainUiIntent.CloseSettings -> handleCloseSettings()
            is MainUiIntent.UpdateApiKey -> handleUpdateApiKey(intent.apiKey)
            is MainUiIntent.ToggleApiKeyVisibility -> handleToggleApiKeyVisibility()
            is MainUiIntent.SelectModel -> handleSelectModel(intent.model)
            is MainUiIntent.UpdateSpeakerAccent -> handleUpdateSpeakerAccent(intent.accent)
            is MainUiIntent.UpdateSpokenLanguage -> handleUpdateSpokenLanguage(intent.language)
            is MainUiIntent.SaveSettings -> handleSaveSettings()
            is MainUiIntent.TestApiKey -> handleTestApiKey()
            is MainUiIntent.OpenHostConnectDialog -> handleOpenHostConnectDialog()
            is MainUiIntent.CloseHostConnectDialog -> _uiState.update { it.copy(isHostConnectDialogOpen = false) }
            is MainUiIntent.ConnectToHost -> handleConnectToHost(intent.address)
            is MainUiIntent.DisconnectActiveHost -> handleDisconnectActiveHost()
            is MainUiIntent.SwitchMode -> _uiState.update { it.copy(activeMode = intent.mode) }
            is MainUiIntent.SendMouseMove -> handleSendMouseMove(intent.dx, intent.dy)
            is MainUiIntent.SendMouseLeftClick -> handleSendMouseClick(BluetoothHidTransport.MOUSE_BUTTON_LEFT)
            is MainUiIntent.SendMouseRightClick -> handleSendMouseClick(BluetoothHidTransport.MOUSE_BUTTON_RIGHT)
            is MainUiIntent.SendMouseMiddleClick -> handleSendMouseClick(BluetoothHidTransport.MOUSE_BUTTON_MIDDLE)
            is MainUiIntent.SendMouseScroll -> handleSendMouseScroll(intent.wheel)

            // Milestone 3 Intents
            is MainUiIntent.SelectSnippetCategory -> handleSelectSnippetCategory(intent.categoryId)
            is MainUiIntent.UpdateSnippetSearchQuery -> handleUpdateSnippetSearchQuery(intent.query)
            is MainUiIntent.TriggerSnippet -> handleTriggerSnippet(intent.snippet)
            is MainUiIntent.SubmitPromptAnswers -> handleSubmitPromptAnswers(intent.answers)
            is MainUiIntent.DismissPromptDialog -> _uiState.update { it.copy(activePromptSnippet = null, activePromptMacro = null, activePrompts = emptyList()) }
            is MainUiIntent.ToggleSnippetFavorite -> handleToggleSnippetFavorite(intent.snippet)
            is MainUiIntent.DeleteSnippet -> handleDeleteSnippet(intent.snippet)
            is MainUiIntent.SaveSnippet -> handleSaveSnippet(intent.snippet)
            is MainUiIntent.TriggerMacro -> handleTriggerMacro(intent.macro)
            is MainUiIntent.SendRawHotkey -> handleSendRawHotkey(intent.strokes)
            is MainUiIntent.StreamClipboardToHost -> handleStreamClipboardToHost(intent.clipText, intent.bracketedPaste)

            // Milestone 4 Intents
            is MainUiIntent.SwitchHost -> handleSwitchHost(intent.target)

            // Next-Gen Innovation Intents (R2-R5)
            is MainUiIntent.SetScreenLensOpen -> _uiState.update { it.copy(isScreenLensOpen = intent.isOpen) }
            is MainUiIntent.ApplyScreenLensContext -> handleApplyScreenLensContext(intent.extractedText)
            is MainUiIntent.SetPttRecording -> handleSetPttRecording(intent.isRecording)
            is MainUiIntent.UpdateAudioLevel -> _uiState.update { it.copy(audioLevel = intent.level) }
            is MainUiIntent.SetAirMouseAiming -> handleSetAirMouseAiming(intent.isAiming)
            is MainUiIntent.AirMouseClick -> handleAirMouseClick(intent.buttonMask)
            is MainUiIntent.SetAirMouseSensitivity -> handleSetAirMouseSensitivity(intent.sensitivity)
        }
    }

    private fun handleTextChanged(newText: String) {
        val currentText = _uiState.value.transcriptionText
        val currentHistory = _uiState.value.undoHistory.toMutableList()

        if (currentText.isNotEmpty() && (currentHistory.isEmpty() || currentHistory.last() != currentText)) {
            currentHistory.add(currentText)
        }

        _uiState.update {
            it.copy(
                transcriptionText = newText,
                undoHistory = currentHistory,
                errorMessage = null
            )
        }

        // In Live Diff transmission mode, emit real-time deltas directly to connected host PC
        val state = _uiState.value
        if (state.liveDiffEnabled && state.connectionState == HidConnectionState.CONNECTED) {
            liveDiffJob?.cancel()
            liveDiffJob = scope.launch {
                _uiState.update { it.copy(isTransmitting = true) }
                try {
                    keystrokeDispatcher.dispatchLiveDiff(newText, delayMs = state.typingDelayMs)
                } finally {
                    _uiState.update { it.copy(isTransmitting = false) }
                }
            }
        }
    }

    private fun handleLayoutSelected(layout: KeyLayout) {
        scope.launch {
            settingsRepository.setKeyLayout(layout)
            _uiState.update { it.copy(activeLayout = layout) }
            (keystrokeDispatcher as? DefaultKeystrokeDispatcher)?.translator =
                KeymapTranslator.create(layout)
        }
    }

    private fun handleLiveDiffToggled(enabled: Boolean) {
        scope.launch {
            settingsRepository.setLiveDiffModeEnabled(enabled)
            _uiState.update { it.copy(liveDiffEnabled = enabled) }
        }
    }

    private fun handleDelayChanged(delayMs: Long) {
        scope.launch {
            settingsRepository.setTypingDelayMs(delayMs)
            _uiState.update { it.copy(typingDelayMs = delayMs) }
        }
    }

    private fun handlePresetSelected(preset: PromptPreset) {
        _uiState.update { it.copy(selectedPreset = preset) }
        scope.launch {
            settingsRepository.setActivePresetId(preset.id)
        }
    }

    private fun handleTriggerAiRewrite() {
        val currentState = _uiState.value
        val textToRewrite = currentState.transcriptionText
        val preset = currentState.selectedPreset ?: PromptPreset.DEFAULT_PRESET

        if (textToRewrite.isBlank()) {
            _uiState.update { it.copy(errorMessage = "No text available to rewrite.") }
            return
        }

        _uiState.update { it.copy(isAiRewriting = true, errorMessage = null) }

        scope.launch {
            val result = textRewriter.rewrite(textToRewrite, preset)
            result.onSuccess { transformed ->
                val history = _uiState.value.undoHistory.toMutableList()
                history.add(textToRewrite)
                _uiState.update {
                    it.copy(
                        transcriptionText = transformed,
                        isAiRewriting = false,
                        undoHistory = history
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isAiRewriting = false,
                        errorMessage = error.localizedMessage ?: "AI rewrite failed."
                    )
                }
            }
        }
    }

    private fun handleSendBufferedKeystrokes() {
        val currentState = _uiState.value
        if (currentState.connectionState != HidConnectionState.CONNECTED) {
            _uiState.update { it.copy(errorMessage = "Host PC is not connected. Please pair and connect via Bluetooth first.") }
            return
        }

        if (currentState.transcriptionText.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "No text to send.") }
            return
        }

        _uiState.update { it.copy(isTransmitting = true, errorMessage = null) }

        scope.launch {
            try {
                keystrokeDispatcher.dispatchBurst(
                    currentState.transcriptionText,
                    delayMs = currentState.typingDelayMs
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Transmission error: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isTransmitting = false) }
            }
        }
    }

    private fun handleClearText() {
        val currentText = _uiState.value.transcriptionText
        val history = _uiState.value.undoHistory.toMutableList()
        if (currentText.isNotEmpty()) {
            history.add(currentText)
        }
        _uiState.update {
            it.copy(
                transcriptionText = "",
                undoHistory = history,
                errorMessage = null
            )
        }
        keystrokeDispatcher.resetState()
    }

    private fun handleUndoText() {
        val history = _uiState.value.undoHistory.toMutableList()
        if (history.isEmpty()) return

        val previousText = history.removeAt(history.size - 1)
        _uiState.update {
            it.copy(
                transcriptionText = previousText,
                undoHistory = history,
                errorMessage = null
            )
        }
    }

    private fun handleSaveCustomPreset(title: String, prompt: String, description: String) {
        if (title.isBlank() || prompt.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Preset title and prompt cannot be blank.") }
            return
        }

        scope.launch {
            val preset = PromptPreset(
                title = title.trim(),
                description = description.ifBlank { "Custom preset: $title" },
                systemPrompt = prompt.trim(),
                isBuiltIn = false
            )
            val newId = presetRepository.insertPreset(preset)
            val persistedPreset = preset.copy(id = newId)

            _uiState.update {
                it.copy(
                    selectedPreset = persistedPreset,
                    isPresetDialogOpen = false,
                    editingPreset = null
                )
            }
        }
    }

    private fun handleDeleteCustomPreset(preset: PromptPreset) {
        if (preset.isBuiltIn) {
            _uiState.update { it.copy(errorMessage = "Cannot delete built-in preset '${preset.title}'.") }
            return
        }

        scope.launch {
            presetRepository.deletePreset(preset)
            _uiState.update { current ->
                val fallback = current.presets.firstOrNull { it.id != preset.id } ?: PromptPreset.DEFAULT_PRESET
                current.copy(
                    selectedPreset = if (current.selectedPreset?.id == preset.id) fallback else current.selectedPreset,
                    isPresetDialogOpen = false,
                    editingPreset = null
                )
            }
        }
    }

    private fun handleOpenSettings() {
        _uiState.update {
            it.copy(
                isSettingsOpen = true,
                speakerAccentInput = it.speakerAccent,
                spokenLanguageInput = it.spokenLanguage,
                settingsFeedbackMessage = null,
                isApiKeyValid = null
            )
        }
    }

    private fun handleCloseSettings() {
        _uiState.update {
            it.copy(
                isSettingsOpen = false,
                speakerAccentInput = it.speakerAccent,
                spokenLanguageInput = it.spokenLanguage,
                settingsFeedbackMessage = null,
                isApiKeyValid = null
            )
        }
    }

    private fun handleUpdateApiKey(apiKey: String) {
        _uiState.update {
            it.copy(
                apiKeyInput = apiKey,
                settingsFeedbackMessage = null,
                isApiKeyValid = null
            )
        }
    }

    private fun handleToggleApiKeyVisibility() {
        _uiState.update { it.copy(isApiKeyVisible = !it.isApiKeyVisible) }
    }

    private fun handleSelectModel(model: String) {
        _uiState.update { it.copy(selectedModel = model) }
    }

    private fun handleUpdateSpeakerAccent(accent: String) {
        _uiState.update { it.copy(speakerAccentInput = accent) }
    }

    private fun handleUpdateSpokenLanguage(language: String) {
        _uiState.update { it.copy(spokenLanguageInput = language) }
    }

    private fun handleSaveSettings() {
        val key = _uiState.value.apiKeyInput.trim()
        val model = _uiState.value.selectedModel
        val accent = _uiState.value.speakerAccentInput.trim()
        val language = _uiState.value.spokenLanguageInput.trim().ifBlank { "English" }
        scope.launch {
            settingsRepository.setApiKey(key)
            settingsRepository.setSelectedModel(model)
            settingsRepository.setSpeakerAccent(accent)
            settingsRepository.setSpokenLanguage(language)
            _uiState.update {
                it.copy(
                    isSettingsOpen = false,
                    speakerAccent = accent,
                    spokenLanguage = language,
                    settingsFeedbackMessage = null,
                    isApiKeyValid = null
                )
            }
        }
    }

    private fun handleTestApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
        val model = _uiState.value.selectedModel
        if (key.isBlank()) {
            _uiState.update {
                it.copy(
                    isTestingApiKey = false,
                    settingsFeedbackMessage = "API key cannot be blank.",
                    isApiKeyValid = false
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isTestingApiKey = true,
                settingsFeedbackMessage = "Validating Gemini API key...",
                isApiKeyValid = null
            )
        }

        scope.launch {
            val result = if (textRewriter is GeminiRemoteRewriter) {
                (textRewriter as GeminiRemoteRewriter).testApiKey(key, model)
            } else {
                runCatching {
                    val testPreset = PromptPreset(
                        id = 9999L,
                        title = "Test",
                        description = "Test Gemini API key validity",
                        systemPrompt = "Respond with 'OK'",
                        temperature = 0.0f
                    )
                    textRewriter.rewrite("Test connection", testPreset).getOrThrow()
                }
            }
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isTestingApiKey = false,
                        settingsFeedbackMessage = "Gemini API key is valid!",
                        isApiKeyValid = true
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isTestingApiKey = false,
                        settingsFeedbackMessage = "Validation failed: ${error.localizedMessage ?: "Invalid API key or network error"}",
                        isApiKeyValid = false
                    )
                }
            }
        }
    }

    /**
     * Refreshes the list of bonded (paired) Bluetooth host devices.
     */
    fun loadPairedDevices() {
        val btTransport = hidTransport as? BluetoothHidTransport
        val connectedName = _uiState.value.connectedDeviceName
        val activeDev = btTransport?.activeDevice
        val list = btTransport?.getBondedDevices()?.map { dev ->
            val devName = try { dev.name ?: dev.address } catch (_: SecurityException) { dev.address }
            val isConn = (activeDev != null && activeDev.address == dev.address) ||
                    (_uiState.value.connectionState == HidConnectionState.CONNECTED && devName == connectedName)
            PairedDeviceUi(
                name = devName,
                address = dev.address,
                isConnected = isConn
            )
        } ?: emptyList()
        _uiState.update { it.copy(pairedDevices = list) }
    }

    private fun handleOpenHostConnectDialog() {
        loadPairedDevices()
        _uiState.update { it.copy(isHostConnectDialogOpen = true) }
    }

    private fun handleConnectToHost(address: String) {
        scope.launch {
            val btTransport = hidTransport as? BluetoothHidTransport
            val success = btTransport?.connectDeviceByAddress(address) ?: false
            if (!success) {
                _uiState.update { it.copy(errorMessage = "Could not initiate connection to host device.") }
            }
            loadPairedDevices()
        }
    }

    private fun handleDisconnectActiveHost() {
        scope.launch {
            hidTransport.disconnect()
            loadPairedDevices()
        }
    }

    private fun handleSendMouseMove(dx: Int, dy: Int) {
        scope.launch {
            hidTransport.sendMouseReport(buttons = 0, dx = dx, dy = dy, wheel = 0)
        }
    }

    private fun handleSendMouseClick(buttonMask: Int) {
        scope.launch {
            hidTransport.sendMouseReport(buttons = buttonMask, dx = 0, dy = 0, wheel = 0)
            kotlinx.coroutines.delay(15)
            hidTransport.sendMouseReport(buttons = 0, dx = 0, dy = 0, wheel = 0)
        }
    }

    private fun handleSendMouseScroll(wheel: Int) {
        scope.launch {
            hidTransport.sendMouseReport(buttons = 0, dx = 0, dy = 0, wheel = wheel)
        }
    }

    // --- M3 Snippets & Macros Handlers ---

    private fun handleSelectSnippetCategory(categoryId: Long?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        refreshSnippetsFilter()
    }

    private fun handleUpdateSnippetSearchQuery(query: String) {
        _uiState.update { it.copy(snippetsSearchQuery = query) }
        refreshSnippetsFilter()
    }

    private fun handleTriggerSnippet(snippet: SnippetEntity) {
        val prompts = VariableParser.extractPrompts(snippet.content)
        if (prompts.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    activePromptSnippet = snippet,
                    activePrompts = prompts
                )
            }
        } else {
            dispatchSnippetExecution(snippet, emptyMap())
        }
    }

    private fun handleSubmitPromptAnswers(answers: Map<String, String>) {
        val snippet = _uiState.value.activePromptSnippet
        val macro = _uiState.value.activePromptMacro

        _uiState.update {
            it.copy(
                activePromptSnippet = null,
                activePromptMacro = null,
                activePrompts = emptyList()
            )
        }

        if (snippet != null) {
            dispatchSnippetExecution(snippet, answers)
        } else if (macro != null) {
            dispatchMacroExecution(macro, answers)
        }
    }

    private fun dispatchSnippetExecution(snippet: SnippetEntity, promptAnswers: Map<String, String>) {
        if (_uiState.value.connectionState != HidConnectionState.CONNECTED) {
            _uiState.update { it.copy(errorMessage = "Host PC is not connected.") }
            return
        }

        val context = InterpolationContext(
            promptAnswers = promptAnswers,
            hostOs = _uiState.value.activeHost?.hostOs?.name ?: "WINDOWS"
        )

        val (resolvedText, backtrack) = VariableParser.evaluate(snippet.content, context)
        val delayMs = _uiState.value.typingDelayMs

        scope.launch {
            _uiState.update { it.copy(isTransmitting = true) }
            try {
                keystrokeDispatcher.dispatchBurst(resolvedText, delayMs)
                if (backtrack > 0) {
                    val leftArrow = HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_LEFT)
                    keystrokeDispatcher.sendRawKeyStrokes(List(backtrack) { leftArrow }, delayMs)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Snippet dispatch error: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isTransmitting = false) }
            }
        }
    }

    private fun handleToggleSnippetFavorite(snippet: SnippetEntity) {
        scope.launch {
            snippetRepository?.setFavorite(snippet.id, !snippet.isFavorite)
        }
    }

    private fun handleDeleteSnippet(snippet: SnippetEntity) {
        scope.launch {
            snippetRepository?.deleteSnippet(snippet)
        }
    }

    private fun handleSaveSnippet(snippet: SnippetEntity) {
        scope.launch {
            snippetRepository?.insertSnippet(snippet)
        }
    }

    private fun handleTriggerMacro(macro: MacroEntity) {
        if (_uiState.value.connectionState != HidConnectionState.CONNECTED) {
            _uiState.update { it.copy(errorMessage = "Host PC is not connected.") }
            return
        }

        val prompts = macroRunner.extractPrompts(macro.stepsJson)
        if (prompts.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    activePromptMacro = macro,
                    activePrompts = prompts
                )
            }
        } else {
            dispatchMacroExecution(macro, emptyMap())
        }
    }

    private fun dispatchMacroExecution(macro: MacroEntity, promptAnswers: Map<String, String>) {
        if (_uiState.value.connectionState != HidConnectionState.CONNECTED) {
            _uiState.update { it.copy(errorMessage = "Host PC is not connected.") }
            return
        }

        val context = InterpolationContext(
            promptAnswers = promptAnswers,
            hostOs = _uiState.value.activeHost?.hostOs?.name ?: "WINDOWS"
        )

        scope.launch {
            _uiState.update { it.copy(isTransmitting = true) }
            try {
                macroRunner.execute(macro.stepsJson, context)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Macro error: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isTransmitting = false) }
            }
        }
    }

    private fun handleSendRawHotkey(strokes: List<HidKeyStroke>) {
        if (_uiState.value.connectionState != HidConnectionState.CONNECTED) {
            _uiState.update { it.copy(errorMessage = "Host PC is not connected.") }
            return
        }

        scope.launch {
            keystrokeDispatcher.sendRawKeyStrokes(strokes, _uiState.value.typingDelayMs)
        }
    }

    private fun handleStreamClipboardToHost(clipText: String, bracketedPaste: Boolean) {
        if (_uiState.value.connectionState != HidConnectionState.CONNECTED) {
            _uiState.update { it.copy(errorMessage = "Host PC is not connected.") }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isTransmitting = true) }
            try {
                keystrokeDispatcher.streamClipboardToHost(clipText, bracketedPaste, _uiState.value.typingDelayMs)
            } finally {
                _uiState.update { it.copy(isTransmitting = false) }
            }
        }
    }

    // --- M4 Multi-Host Handlers ---

    private fun handleSwitchHost(target: PairedHostEntity) {
        scope.launch {
            val bt = hidTransport as? BluetoothHidTransport
            if (bt == null) {
                _uiState.update { it.copy(errorMessage = "Bluetooth transport is not available.") }
                return@launch
            }
            val success = bt.switchHost(target)
            if (!success) {
                _uiState.update { it.copy(errorMessage = "Failed to switch to host: ${target.hostName}") }
            }
            loadPairedDevices()
        }
    }

    // --- Next-Gen Innovation Handlers (R2-R5) ---

    private fun handleApplyScreenLensContext(extractedText: String) {
        if (extractedText.isBlank()) return
        _uiState.update { current ->
            val formatted = "```\n$extractedText\n```\n"
            val updated = if (current.transcriptionText.isBlank()) {
                formatted
            } else {
                "${current.transcriptionText.trimEnd()}\n\n$formatted"
            }
            current.copy(
                transcriptionText = updated,
                capturedScreenContext = extractedText,
                isScreenLensOpen = false
            )
        }
    }

    private fun handleSetPttRecording(isRecording: Boolean) {
        _uiState.update { it.copy(isPttRecording = isRecording) }
        if (isRecording) {
            pttAudioEngine?.startRecording(
                languageCode = if (_uiState.value.spokenLanguage.equals("Afrikaans", ignoreCase = true)) "af-ZA" else "en-US"
            ) { hypothesis ->
                _uiState.update { current ->
                    current.copy(transcriptionText = hypothesis)
                }
            }
        } else {
            pttAudioEngine?.stopRecording()
        }
    }

    private fun handleSetAirMouseAiming(isAiming: Boolean) {
        _uiState.update { it.copy(isAirMouseAiming = isAiming) }
        if (isAiming) {
            gyroAirMouseEngine?.startAiming()
        } else {
            gyroAirMouseEngine?.stopAiming()
        }
    }

    private fun handleAirMouseClick(buttonMask: Byte) {
        gyroAirMouseEngine?.sendClick(buttonMask)
    }

    private fun handleSetAirMouseSensitivity(sensitivity: Float) {
        _uiState.update { it.copy(airMouseSensitivity = sensitivity) }
        gyroAirMouseEngine?.sensitivity = sensitivity
    }

    override fun onCleared() {
        super.onCleared()
        pttAudioEngine?.destroy()
        gyroAirMouseEngine?.destroy()
        connectionWatchdog?.cancel()
    }
}
