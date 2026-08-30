package com.transcriptor.hid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transcriptor.hid.ai.PromptPreset
import com.transcriptor.hid.ai.TextRewriter
import com.transcriptor.hid.data.PresetRepository
import com.transcriptor.hid.data.SettingsRepository
import com.transcriptor.hid.engine.DefaultKeystrokeDispatcher
import com.transcriptor.hid.engine.KeyLayout
import com.transcriptor.hid.engine.KeymapTranslator
import com.transcriptor.hid.engine.KeystrokeDispatcher
import com.transcriptor.hid.service.BluetoothHidTransport
import com.transcriptor.hid.service.HidConnectionState
import com.transcriptor.hid.service.HidTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI ViewModel orchestrating single-screen UI state, user intents, AI rewriting,
 * HID transmission, and repository persistence.
 */
open class MainViewModel(
    val settingsRepository: SettingsRepository,
    val presetRepository: PresetRepository,
    val textRewriter: TextRewriter,
    val keystrokeDispatcher: KeystrokeDispatcher,
    val hidTransport: HidTransport,
    externalScope: CoroutineScope? = null
) : ViewModel() {

    // Use injected scope or default to ViewModel's viewModelScope
    private val scope: CoroutineScope = externalScope ?: viewModelScope

    private var liveDiffJob: Job? = null

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
            is MainUiIntent.SaveSettings -> handleSaveSettings()
            is MainUiIntent.TestApiKey -> handleTestApiKey()
            is MainUiIntent.OpenHostConnectDialog -> handleOpenHostConnectDialog()
            is MainUiIntent.CloseHostConnectDialog -> _uiState.update { it.copy(isHostConnectDialogOpen = false) }
            is MainUiIntent.ConnectToHost -> handleConnectToHost(intent.address)
            is MainUiIntent.DisconnectActiveHost -> handleDisconnectActiveHost()
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
        // Preceding live-diff job is cancelled to prevent out-of-order execution during rapid voice typing
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
                settingsFeedbackMessage = null,
                isApiKeyValid = null
            )
        }
    }

    private fun handleCloseSettings() {
        _uiState.update {
            it.copy(
                isSettingsOpen = false,
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

    private fun handleSaveSettings() {
        val key = _uiState.value.apiKeyInput.trim()
        val model = _uiState.value.selectedModel
        scope.launch {
            settingsRepository.setApiKey(key)
            settingsRepository.setSelectedModel(model)
            _uiState.update {
                it.copy(
                    isSettingsOpen = false,
                    settingsFeedbackMessage = null,
                    isApiKeyValid = null
                )
            }
        }
    }

    private fun handleTestApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
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
            val testPreset = PromptPreset(
                id = 9999L,
                title = "Test",
                description = "Test Gemini API key validity",
                systemPrompt = "Respond with 'OK'",
                temperature = 0.0f
            )
            val result = textRewriter.rewrite("Test connection", testPreset)
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
}
