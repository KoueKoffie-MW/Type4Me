package com.transcriptor.hid.service

/**
 * Represents the lifecycle and connection state of an HID peripheral transport.
 */
enum class HidConnectionState {
    /**
     * Transport is idle, disconnected, or uninitialized.
     */
    DISCONNECTED,

    /**
     * Transport is negotiating L2CAP/USB channels or awaiting host connection.
     */
    CONNECTING,

    /**
     * Transport is fully connected to a host and ready to transmit HID reports.
     */
    CONNECTED,

    /**
     * Transport encountered a registration or protocol error.
     */
    ERROR
}
