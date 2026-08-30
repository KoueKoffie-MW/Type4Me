package com.transcriptor.hid.service

import kotlinx.coroutines.flow.StateFlow

/**
 * Represents the status of the host computer's keyboard lock LEDs and indicator flags.
 *
 * @property numLock Active state of NumLock LED (Bit 0).
 * @property capsLock Active state of CapsLock LED (Bit 1).
 * @property scrollLock Active state of ScrollLock LED (Bit 2).
 * @property compose Active state of Compose LED (Bit 3).
 * @property kana Active state of Kana LED (Bit 4).
 */
data class HostLedState(
    val numLock: Boolean = false,
    val capsLock: Boolean = false,
    val scrollLock: Boolean = false,
    val compose: Boolean = false,
    val kana: Boolean = false
) {
    companion object {
        val ALL_OFF = HostLedState()

        /**
         * Decodes a 1-byte HID Keyboard Output Report into a [HostLedState].
         */
        fun fromByte(ledByte: Byte): HostLedState {
            val v = ledByte.toInt()
            return HostLedState(
                numLock = (v and 0x01) != 0,
                capsLock = (v and 0x02) != 0,
                scrollLock = (v and 0x04) != 0,
                compose = (v and 0x08) != 0,
                kana = (v and 0x10) != 0
            )
        }
    }
}

/**
 * Universal interface abstraction for HID keyboard peripheral transports.
 *
 * Implementations provide physical or virtual communication channels (Bluetooth HID Device profile,
 * wired USB Android Open Accessory 2.0, reverse socket bridging, or Linux USB Gadget) for transmitting
 * standard 8-byte HID keyboard reports to a host computer.
 */
interface HidTransport {
    /**
     * Observable flow of the current HID connection state.
     */
    val connectionState: StateFlow<HidConnectionState>

    /**
     * Observable flow of the connected host device name or address (null if disconnected).
     */
    val connectedDeviceName: StateFlow<String?>

    /**
     * Observable flow of the host keyboard LED status.
     */
    val hostLedState: StateFlow<HostLedState>

    /**
     * Initializes the transport, registering SDP records or opening physical/virtual communication endpoints.
     *
     * @return True if initialization or proxy acquisition succeeded, false otherwise.
     */
    suspend fun initialize(): Boolean

    /**
     * Transmits an 8-byte standard HID keyboard input report to the connected host.
     *
     * @param report 8-byte input report (Byte 0: modifier, Byte 1: reserved, Bytes 2-7: key codes).
     * @return True if the report was successfully queued or transmitted, false otherwise.
     */
    suspend fun sendKeyboardReport(report: ByteArray): Boolean

    /**
     * Disconnects the active session with the host device.
     */
    suspend fun disconnect()

    /**
     * Releases all resources, unregisters callbacks, and unbinds profile proxies.
     */
    fun release()
}
