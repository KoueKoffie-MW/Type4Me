package com.transcriptor.hid.ui

import com.transcriptor.hid.ai.PromptPreset
import com.transcriptor.hid.engine.KeyLayout

/**
 * User intents and asynchronous UI events processed by [MainViewModel].
 */
sealed interface MainUiIntent {
    /**
     * Fired when text canvas content changes (via Gboard IME dictation or keyboard editing).
     */
    data class TextChanged(val newText: String) : MainUiIntent

    /**
     * Fired when the user selects a different host keymap layout (DE QWERTZ vs US QWERTY).
     */
    data class LayoutSelected(val layout: KeyLayout) : MainUiIntent

    /**
     * Fired when the transmission mode is toggled between Burst and Live Diff.
     */
    data class LiveDiffToggled(val enabled: Boolean) : MainUiIntent

    /**
     * Fired when the user adjusts the inter-character typing delay slider.
     */
    data class DelayChanged(val delayMs: Long) : MainUiIntent

    /**
     * Fired when an AI prompt preset chip is selected.
     */
    data class PresetSelected(val preset: PromptPreset) : MainUiIntent

    /**
     * Fired when the "✨ Rewrite with AI" action button is tapped.
     */
    object TriggerAiRewrite : MainUiIntent

    /**
     * Fired when the "Send to PC" keystrokes button / FAB is tapped.
     */
    object SendBufferedKeystrokes : MainUiIntent

    /**
     * Fired when the "Clear" canvas button is tapped.
     */
    object ClearText : MainUiIntent

    /**
     * Fired when the "Undo" action is tapped to revert the last edit or AI rewrite.
     */
    object UndoText : MainUiIntent

    /**
     * Fired when creating or saving a custom AI prompt preset.
     */
    data class SaveCustomPreset(
        val title: String,
        val prompt: String,
        val description: String = ""
    ) : MainUiIntent

    /**
     * Fired when deleting a user-created custom prompt preset.
     */
    data class DeleteCustomPreset(val preset: PromptPreset) : MainUiIntent

    /**
     * Opens the preset management / editor dialog.
     */
    data class OpenPresetDialog(val preset: PromptPreset? = null) : MainUiIntent

    /**
     * Closes the preset management dialog.
     */
    object ClosePresetDialog : MainUiIntent

    /**
     * Dismisses the active error message banner or snackbar.
     */
    object DismissError : MainUiIntent

    /**
     * Opens the Gemini API and configuration settings dialog.
     */
    object OpenSettings : MainUiIntent

    /**
     * Closes the settings dialog without saving unsaved edits.
     */
    object CloseSettings : MainUiIntent

    /**
     * Fired when the Gemini API key input field changes.
     */
    data class UpdateApiKey(val apiKey: String) : MainUiIntent

    /**
     * Toggles visibility (masking/unmasking) of the Gemini API key.
     */
    object ToggleApiKeyVisibility : MainUiIntent

    /**
     * Fired when a Gemini model is selected (e.g. gemini-3.7-flash).
     */
    data class SelectModel(val model: String) : MainUiIntent

    /**
     * Fired when the speaker accent selection or input changes (e.g. "Afrikaans", "German").
     */
    data class UpdateSpeakerAccent(val accent: String) : MainUiIntent

    /**
     * Fired when the spoken language selection or input changes (e.g. "English", "German").
     */
    data class UpdateSpokenLanguage(val language: String) : MainUiIntent

    /**
     * Persists the current settings (API key, model, accent, language) to DataStore.
     */
    object SaveSettings : MainUiIntent

    /**
     * Tests the current Gemini API key by making a lightweight test call.
     */
    object TestApiKey : MainUiIntent

    /**
     * Opens the Bluetooth host connection & paired devices dialog.
     */
    object OpenHostConnectDialog : MainUiIntent

    /**
     * Closes the Bluetooth host connection dialog.
     */
    object CloseHostConnectDialog : MainUiIntent

    /**
     * Connects to a specific paired Bluetooth host device by MAC address.
     */
    data class ConnectToHost(val address: String) : MainUiIntent

    /**
     * Disconnects the currently active Bluetooth HID host connection.
     */
    object DisconnectActiveHost : MainUiIntent

    /**
     * Switches between Voice Keyboard mode and Touchpad Mouse mode.
     */
    data class SwitchMode(val mode: AppMode) : MainUiIntent

    /**
     * Transmits a relative mouse movement displacement (dX, dY).
     */
    data class SendMouseMove(val dx: Int, val dy: Int) : MainUiIntent

    /**
     * Transmits a mouse left-click event (press and immediate release).
     */
    object SendMouseLeftClick : MainUiIntent

    /**
     * Transmits a mouse right-click event (press and immediate release).
     */
    object SendMouseRightClick : MainUiIntent

    /**
     * Transmits a mouse middle-click event.
     */
    object SendMouseMiddleClick : MainUiIntent

    /**
     * Transmits a vertical mouse scroll wheel delta.
     */
    data class SendMouseScroll(val wheel: Int) : MainUiIntent
}
