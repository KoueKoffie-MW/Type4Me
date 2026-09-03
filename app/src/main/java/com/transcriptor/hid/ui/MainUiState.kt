package com.transcriptor.hid.ui

import com.transcriptor.hid.ai.PromptPreset
import com.transcriptor.hid.data.db.CategoryEntity
import com.transcriptor.hid.data.db.MacroEntity
import com.transcriptor.hid.data.db.PairedHostEntity
import com.transcriptor.hid.data.db.SnippetEntity
import com.transcriptor.hid.engine.KeyLayout
import com.transcriptor.hid.engine.VariableDescriptor
import com.transcriptor.hid.service.HidConnectionState

/**
 * Primary operating mode for the application.
 */
enum class AppMode {
    /**
     * Speech-to-text dictation canvas with AI prompt rewriting.
     */
    KEYBOARD,

    /**
     * Fast-dispatch developer code snippets pad and macro runner.
     */
    SNIPPETS,

    /**
     * Full-screen tactile precision trackpad and mouse.
     */
    TOUCHPAD
}

/**
 * Immutable screen state for the single-screen Transcriptor HID UI.
 */
data class MainUiState(
    val transcriptionText: String = "",
    val activeLayout: KeyLayout = KeyLayout.GERMAN_QWERTZ,
    val connectionState: HidConnectionState = HidConnectionState.DISCONNECTED,
    val connectedDeviceName: String? = null,
    val liveDiffEnabled: Boolean = false,
    val typingDelayMs: Long = 8L,
    val presets: List<PromptPreset> = emptyList(),
    val selectedPreset: PromptPreset? = null,
    val isAiRewriting: Boolean = false,
    val isTransmitting: Boolean = false,
    val errorMessage: String? = null,
    val undoHistory: List<String> = emptyList(),
    val isPresetDialogOpen: Boolean = false,
    val editingPreset: PromptPreset? = null,
    val isSettingsOpen: Boolean = false,
    val apiKeyInput: String = "",
    val isApiKeyVisible: Boolean = false,
    val selectedModel: String = "gemini-3.5-flash-lite",
    val isTestingApiKey: Boolean = false,
    val settingsFeedbackMessage: String? = null,
    val isApiKeyValid: Boolean? = null,
    val isHostConnectDialogOpen: Boolean = false,
    val pairedDevices: List<PairedDeviceUi> = emptyList(),
    val activeMode: AppMode = AppMode.KEYBOARD,
    val speakerAccent: String = "None",
    val spokenLanguage: String = "English",
    val speakerAccentInput: String = "None",
    val spokenLanguageInput: String = "English",

    // M3: Snippets Pad, AST & Macros
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: Long? = null,
    val snippets: List<SnippetEntity> = emptyList(),
    val favorites: List<SnippetEntity> = emptyList(),
    val macros: List<MacroEntity> = emptyList(),
    val snippetsSearchQuery: String = "",
    val activePromptSnippet: SnippetEntity? = null,
    val activePromptMacro: MacroEntity? = null,
    val activePrompts: List<VariableDescriptor.Prompt> = emptyList(),

    // M4: Multi-Host Management
    val pairedHosts: List<PairedHostEntity> = emptyList(),
    val activeHost: PairedHostEntity? = null,

    // Next-Gen Innovations (R2-R5)
    val isScreenLensOpen: Boolean = false,
    val capturedScreenContext: String? = null,
    val isPttRecording: Boolean = false,
    val audioLevel: Float = 0f,
    val isWatchdogReconnecting: Boolean = false,
    val watchdogAttempts: Int = 0,
    val isAirMouseAiming: Boolean = false,
    val isAirMouseAvailable: Boolean = true,
    val airMouseSensitivity: Float = 1.2f
) {
    /**
     * Calculated word count for dictation telemetry.
     */
    val wordCount: Int
        get() = if (transcriptionText.isBlank()) 0 else transcriptionText.trim().split(Regex("\\s+")).size

    /**
     * Calculated character count.
     */
    val charCount: Int
        get() = transcriptionText.length

    /**
     * True if text is non-empty and device is connected to host PC.
     */
    val canSend: Boolean
        get() = transcriptionText.isNotEmpty() && connectionState == HidConnectionState.CONNECTED && !isTransmitting

    /**
     * True if there are previous text states in the undo stack.
     */
    val canUndo: Boolean
        get() = undoHistory.isNotEmpty()

    /**
     * True if there is text in the canvas.
     */
    val canClear: Boolean
        get() = transcriptionText.isNotEmpty()

    /**
     * True if AI rewriting can be triggered.
     */
    val canRewrite: Boolean
        get() = transcriptionText.isNotBlank() && selectedPreset != null && !isAiRewriting
}

/**
 * UI representation of a paired Bluetooth host device.
 */
data class PairedDeviceUi(
    val name: String,
    val address: String,
    val isConnected: Boolean = false
)
