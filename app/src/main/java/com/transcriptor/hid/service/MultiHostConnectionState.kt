package com.transcriptor.hid.service

import com.transcriptor.hid.data.db.PairedHostEntity

/**
 * Finite State Machine model for Multi-Host Bluetooth HID connection and atomic switching.
 */
sealed class MultiHostConnectionState {
    data object Disconnected : MultiHostConnectionState()
    data class Connecting(val targetHost: PairedHostEntity) : MultiHostConnectionState()
    data class Connected(val activeHost: PairedHostEntity) : MultiHostConnectionState()
    data class Disconnecting(val disconnectingHost: PairedHostEntity) : MultiHostConnectionState()
    data class SwitchingHost(val fromHost: PairedHostEntity, val toHost: PairedHostEntity) : MultiHostConnectionState()
    data class Error(val message: String, val failedHost: PairedHostEntity?, val canRetry: Boolean = true) : MultiHostConnectionState()
}
