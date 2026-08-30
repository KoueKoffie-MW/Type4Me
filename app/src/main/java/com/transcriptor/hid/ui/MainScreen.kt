package com.transcriptor.hid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.transcriptor.hid.ui.components.ConnectionHeader
import com.transcriptor.hid.ui.components.ControlBar
import com.transcriptor.hid.ui.components.ErrorBanner
import com.transcriptor.hid.ui.components.HostConnectDialog
import com.transcriptor.hid.ui.components.PresetDialog
import com.transcriptor.hid.ui.components.PresetSelector
import com.transcriptor.hid.ui.components.SettingsDialog
import com.transcriptor.hid.ui.components.TranscriptionCanvas

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestDiscoverability: () -> Unit = {},
    onOpenBluetoothSettings: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    MainScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onRequestDiscoverability = onRequestDiscoverability,
        onOpenBluetoothSettings = onOpenBluetoothSettings
    )
}

@Composable
fun MainScreenContent(
    state: MainUiState,
    onIntent: (MainUiIntent) -> Unit,
    onRequestDiscoverability: () -> Unit = {},
    onOpenBluetoothSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ConnectionHeader(
                connectionState = state.connectionState,
                connectedDeviceName = state.connectedDeviceName,
                onPairHostClick = { onIntent(MainUiIntent.OpenHostConnectDialog) },
                onSettingsClick = { onIntent(MainUiIntent.OpenSettings) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Prominent Actionable Error Banner (when an error is present)
            if (state.errorMessage != null) {
                ErrorBanner(
                    errorMessage = state.errorMessage,
                    onDismiss = { onIntent(MainUiIntent.DismissError) },
                    onOpenSettings = { onIntent(MainUiIntent.OpenSettings) },
                    onPairHost = { onIntent(MainUiIntent.OpenHostConnectDialog) }
                )
            }

            // Primary Mode Selector: Voice Keyboard vs Touchpad Mouse
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.activeMode == AppMode.KEYBOARD,
                    onClick = { onIntent(MainUiIntent.SwitchMode(AppMode.KEYBOARD)) },
                    label = {
                        Text(
                            text = "🎙️ Voice Keyboard",
                            fontWeight = if (state.activeMode == AppMode.KEYBOARD) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = state.activeMode == AppMode.TOUCHPAD,
                    onClick = { onIntent(MainUiIntent.SwitchMode(AppMode.TOUCHPAD)) },
                    label = {
                        Text(
                            text = "🖱️ Touchpad Mouse",
                            fontWeight = if (state.activeMode == AppMode.TOUCHPAD) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            if (state.activeMode == AppMode.KEYBOARD) {
                // Control Bar (Keymap, Mode, Typing Delay)
                ControlBar(
                    activeLayout = state.activeLayout,
                    liveDiffEnabled = state.liveDiffEnabled,
                    typingDelayMs = state.typingDelayMs,
                    onLayoutChange = { onIntent(MainUiIntent.LayoutSelected(it)) },
                    onLiveDiffToggle = { onIntent(MainUiIntent.LiveDiffToggled(it)) },
                    onDelayChange = { onIntent(MainUiIntent.DelayChanged(it)) }
                )

                // AI Preset Selector Chips Bar
                PresetSelector(
                    presets = state.presets,
                    selectedPreset = state.selectedPreset,
                    onPresetSelect = { onIntent(MainUiIntent.PresetSelected(it)) },
                    onAddPresetClick = { onIntent(MainUiIntent.OpenPresetDialog()) }
                )

                // Transcription & Voice Typing Editor Canvas
                TranscriptionCanvas(
                    state = state,
                    onTextChange = { onIntent(MainUiIntent.TextChanged(it)) },
                    onRewriteClick = { onIntent(MainUiIntent.TriggerAiRewrite) },
                    onSendClick = { onIntent(MainUiIntent.SendBufferedKeystrokes) },
                    onClearClick = { onIntent(MainUiIntent.ClearText) },
                    onUndoClick = { onIntent(MainUiIntent.UndoText) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Touchpad & Precision Mouse Canvas
                com.transcriptor.hid.ui.components.TouchpadCanvas(
                    isConnected = state.connectionState == com.transcriptor.hid.service.HidConnectionState.CONNECTED,
                    onMouseMove = { dx, dy -> onIntent(MainUiIntent.SendMouseMove(dx, dy)) },
                    onLeftClick = { onIntent(MainUiIntent.SendMouseLeftClick) },
                    onRightClick = { onIntent(MainUiIntent.SendMouseRightClick) },
                    onMiddleClick = { onIntent(MainUiIntent.SendMouseMiddleClick) },
                    onMouseScroll = { wheel -> onIntent(MainUiIntent.SendMouseScroll(wheel)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(540.dp)
                )
            }
        }
    }

    // Preset Editor / Creation Dialog
    if (state.isPresetDialogOpen) {
        PresetDialog(
            preset = state.editingPreset,
            onDismiss = { onIntent(MainUiIntent.ClosePresetDialog) },
            onSave = { title, prompt, description ->
                onIntent(MainUiIntent.SaveCustomPreset(title, prompt, description))
            },
            onDelete = { preset ->
                onIntent(MainUiIntent.DeleteCustomPreset(preset))
            }
        )
    }

    // Settings & AI Configuration Dialog
    if (state.isSettingsOpen) {
        SettingsDialog(
            apiKey = state.apiKeyInput,
            isApiKeyVisible = state.isApiKeyVisible,
            selectedModel = state.selectedModel,
            isTestingApiKey = state.isTestingApiKey,
            feedbackMessage = state.settingsFeedbackMessage,
            isApiKeyValid = state.isApiKeyValid,
            onApiKeyChange = { onIntent(MainUiIntent.UpdateApiKey(it)) },
            onToggleApiKeyVisibility = { onIntent(MainUiIntent.ToggleApiKeyVisibility) },
            onModelSelect = { onIntent(MainUiIntent.SelectModel(it)) },
            onTestApiKey = { onIntent(MainUiIntent.TestApiKey) },
            onSave = { onIntent(MainUiIntent.SaveSettings) },
            onDismiss = { onIntent(MainUiIntent.CloseSettings) }
        )
    }

    // Bluetooth Host Connection & Paired Devices Dialog
    if (state.isHostConnectDialogOpen) {
        HostConnectDialog(
            connectionState = state.connectionState,
            connectedDeviceName = state.connectedDeviceName,
            pairedDevices = state.pairedDevices,
            onConnectToDevice = { onIntent(MainUiIntent.ConnectToHost(it)) },
            onDisconnect = { onIntent(MainUiIntent.DisconnectActiveHost) },
            onRequestDiscoverability = onRequestDiscoverability,
            onOpenBluetoothSettings = onOpenBluetoothSettings,
            onRefreshPairedDevices = { onIntent(MainUiIntent.OpenHostConnectDialog) },
            onDismiss = { onIntent(MainUiIntent.CloseHostConnectDialog) }
        )
    }
}
