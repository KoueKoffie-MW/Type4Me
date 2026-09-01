package com.transcriptor.hid.ui

import com.transcriptor.hid.ai.PromptPreset
import com.transcriptor.hid.data.db.MacroEntity
import com.transcriptor.hid.data.db.PairedHostEntity
import com.transcriptor.hid.data.db.SnippetEntity
import com.transcriptor.hid.engine.HidKeyStroke
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
    data object TriggerAiRewrite : MainUiIntent

    /**
     * Fired when the "Send to PC" keystrokes button / FAB is tapped.
     */
    data object SendBufferedKeystrokes : MainUiIntent

    /**
     * Fired when the "Clear" canvas button is tapped.
     */
    data object ClearText : MainUiIntent

    /**
     * Fired when the "Undo" action is tapped to revert the last edit or AI rewrite.
     */
    data object UndoText : MainUiIntent

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
    data object ClosePresetDialog : MainUiIntent

    /**
     * Dismisses the active error message banner or snackbar.
     */
    data object DismissError : MainUiIntent

    /**
     * Opens the Gemini API and configuration settings dialog.
     */
    data object OpenSettings : MainUiIntent

    /**
     * Closes the settings dialog without saving unsaved edits.
     */
    data object CloseSettings : MainUiIntent

    /**
     * Fired when the Gemini API key input field changes.
     */
    data class UpdateApiKey(val apiKey: String) : MainUiIntent

    /**
     * Toggles visibility (masking/unmasking) of the Gemini API key.
     */
    data object ToggleApiKeyVisibility : MainUiIntent

    /**
     * Fired when a Gemini model is selected (e.g. gemini-3.5-flash-lite).
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
    data object SaveSettings : MainUiIntent

    /**
     * Tests the current Gemini API key by making a lightweight test call.
     */
    data object TestApiKey : MainUiIntent

    /**
     * Opens the Bluetooth host connection & paired devices dialog.
     */
    data object OpenHostConnectDialog : MainUiIntent

    /**
     * Closes the Bluetooth host connection dialog.
     */
    data object CloseHostConnectDialog : MainUiIntent

    /**
     * Connects to a specific paired Bluetooth host device by MAC address.
     */
    data class ConnectToHost(val address: String) : MainUiIntent

    /**
     * Disconnects the currently active Bluetooth HID host connection.
     */
    data object DisconnectActiveHost : MainUiIntent

    /**
     * Switches operating mode (Voice Keyboard, Snippets Pad, Touchpad Mouse).
     */
    data class SwitchMode(val mode: AppMode) : MainUiIntent

    /**
     * Transmits a relative mouse movement displacement (dX, dY).
     */
    data class SendMouseMove(val dx: Int, val dy: Int) : MainUiIntent

    /**
     * Transmits a mouse left-click event (press and immediate release).
     */
    data object SendMouseLeftClick : MainUiIntent

    /**
     * Transmits a mouse right-click event (press and immediate release).
     */
    data object SendMouseRightClick : MainUiIntent

    /**
     * Transmits a mouse middle-click event.
     */
    data object SendMouseMiddleClick : MainUiIntent

    /**
     * Transmits a vertical mouse scroll wheel delta.
     */
    data class SendMouseScroll(val wheel: Int) : MainUiIntent

    // --- Milestone 3: Snippets & Macros Intents ---

    /**
     * Selects a category filter in Snippets Pad (null selects "All").
     */
    data class SelectSnippetCategory(val categoryId: Long?) : MainUiIntent

    /**
     * Updates the full-text search query in Snippets Pad.
     */
    data class UpdateSnippetSearchQuery(val query: String) : MainUiIntent

    /**
     * Triggers typing dispatch for a snippet (evaluates variables / opens prompt modal if needed).
     */
    data class TriggerSnippet(val snippet: SnippetEntity) : MainUiIntent

    /**
     * Submits entered parameter answers from the VariablePromptBottomSheet.
     */
    data class SubmitPromptAnswers(val answers: Map<String, String>) : MainUiIntent

    /**
     * Dismisses the variable prompt modal bottom sheet.
     */
    data object DismissPromptDialog : MainUiIntent

    /**
     * Toggles snippet favorite status.
     */
    data class ToggleSnippetFavorite(val snippet: SnippetEntity) : MainUiIntent

    /**
     * Deletes a snippet.
     */
    data class DeleteSnippet(val snippet: SnippetEntity) : MainUiIntent

    /**
     * Saves or updates a snippet.
     */
    data class SaveSnippet(val snippet: SnippetEntity) : MainUiIntent

    /**
     * Triggers execution of a multi-step macro.
     */
    data class TriggerMacro(val macro: MacroEntity) : MainUiIntent

    /**
     * Sends a raw sequence of HID keystrokes (e.g. from HotkeyDockBar).
     */
    data class SendRawHotkey(val strokes: List<HidKeyStroke>) : MainUiIntent

    /**
     * Streams Android clipboard text to host workstation.
     */
    data class StreamClipboardToHost(val clipText: String, val bracketedPaste: Boolean = false) : MainUiIntent

    // --- Milestone 4: Multi-Host Switching Intents ---

    /**
     * Triggers 6-Phase Atomic host switching to a target paired host.
     */
    data class SwitchHost(val target: PairedHostEntity) : MainUiIntent
}
