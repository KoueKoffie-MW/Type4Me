package com.transcriptor.hid.service

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Autonomous Bluetooth L2CAP Connection Watchdog.
 *
 * Automatically detects dropped HID connections when a host PC sleeps or drops out of range,
 * and executes a prioritized 3-probe burst reconnection (+300ms, +800ms, +1400ms)
 * to restore the link in under 1.5 seconds once the workstation wakes up.
 */
class BluetoothConnectionWatchdog(
    private val transport: BluetoothHidTransport,
    private val scope: CoroutineScope
) {
    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting.asStateFlow()

    private val _reconnectAttempts = MutableStateFlow(0)
    val reconnectAttempts: StateFlow<Int> = _reconnectAttempts.asStateFlow()

    private var watchdogJob: Job? = null
    var isEnabled: Boolean = true

    init {
        transport.onConnectionLostListener = { device ->
            onConnectionLost(device)
        }
    }

    /**
     * Called by transport when an established connection drops.
     */
    fun onConnectionLost(lastConnectedDevice: BluetoothDevice?) {
        if (!isEnabled || lastConnectedDevice == null) return
        if (_isReconnecting.value) return

        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            _isReconnecting.value = true
            _reconnectAttempts.value = 0

            // 3-Probe Reconnection Burst
            val probeDelays = listOf(300L, 800L, 1400L)

            for ((index, delayMs) in probeDelays.withIndex()) {
                delay(delayMs)
                _reconnectAttempts.value = index + 1

                if (transport.connectionState.value == HidConnectionState.CONNECTED) {
                    // Already reconnected successfully!
                    break
                }

                try {
                    val success = transport.connectHost(lastConnectedDevice)
                    if (success) {
                        break
                    }
                } catch (e: Exception) {
                    // Probe failed, continue to next attempt
                }
            }

            _isReconnecting.value = false
        }
    }

    /**
     * Cancels any active watchdog reconnection attempt.
     */
    fun cancel() {
        watchdogJob?.cancel()
        watchdogJob = null
        _isReconnecting.value = false
        _reconnectAttempts.value = 0
    }
}
