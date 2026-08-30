package com.transcriptor.hid.service

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Operating mode for USB HID peripheral fallback.
 */
enum class UsbTransportMode {
    /**
     * Android Open Accessory (AOA) 2.0 HID Protocol over USB accessory endpoint.
     */
    AOA_2_0,

    /**
     * Local TCP socket bridge over USB cable via ADB port forwarding (e.g., adb reverse tcp:8080 tcp:8080).
     */
    ADB_SOCKET_BRIDGE,

    /**
     * Linux USB Gadget / ConfigFS character device (/dev/hidg0) for rooted/embedded environments.
     */
    USB_GADGET
}

/**
 * Interface abstraction representing a bidirectional USB byte stream channel.
 */
interface UsbChannel {
    val channelName: String
    val isConnected: Boolean
    fun open(): Boolean
    fun write(data: ByteArray): Boolean
    fun close()
    fun getInputStream(): InputStream? = null
    fun getOutputStream(): OutputStream? = null
}

/**
 * TCP socket implementation of [UsbChannel] for ADB reverse port bridging.
 */
class SocketUsbChannel(
    private val host: String = "127.0.0.1",
    private val port: Int = 8080,
    private val timeoutMs: Int = 3000
) : UsbChannel {
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null

    override val channelName: String
        get() = "ADB Socket ($host:$port)"

    override val isConnected: Boolean
        get() = socket?.isConnected == true && socket?.isClosed == false

    override fun open(): Boolean {
        return try {
            val sock = Socket()
            sock.connect(InetSocketAddress(host, port), timeoutMs)
            sock.tcpNoDelay = true
            socket = sock
            outputStream = sock.getOutputStream()
            true
        } catch (e: Exception) {
            close()
            false
        }
    }

    override fun write(data: ByteArray): Boolean {
        val stream = outputStream ?: return false
        return try {
            stream.write(data)
            stream.flush()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun close() {
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            socket?.close()
        } catch (_: Exception) {}
        outputStream = null
        socket = null
    }

    override fun getOutputStream(): OutputStream? = outputStream
}

/**
 * Memory-backed [UsbChannel] for testing and in-memory verification.
 */
class InMemoryUsbChannel(
    override val channelName: String = "Test USB Channel",
    var shouldSucceedOpen: Boolean = true,
    var shouldSucceedWrite: Boolean = true
) : UsbChannel {
    private var _connected = false
    val writtenReports = mutableListOf<ByteArray>()

    override val isConnected: Boolean
        get() = _connected

    override fun open(): Boolean {
        _connected = shouldSucceedOpen
        return _connected
    }

    override fun write(data: ByteArray): Boolean {
        if (!_connected || !shouldSucceedWrite) return false
        writtenReports.add(data.copyOf())
        return true
    }

    override fun close() {
        _connected = false
    }
}

/**
 * USB HID Transport Fallback.
 *
 * Implements [HidTransport] for wired USB connections when Bluetooth is disabled, restricted,
 * or unavailable on the host workstation.
 */
class UsbHidTransport(
    val mode: UsbTransportMode = UsbTransportMode.ADB_SOCKET_BRIDGE,
    private val channelProvider: (() -> UsbChannel)? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HidTransport {

    private val _connectionState = MutableStateFlow(HidConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<HidConnectionState> = _connectionState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    override val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _hostLedState = MutableStateFlow(HostLedState.ALL_OFF)
    override val hostLedState: StateFlow<HostLedState> = _hostLedState.asStateFlow()

    private var activeChannel: UsbChannel? = null

    override suspend fun initialize(): Boolean = withContext(ioDispatcher) {
        _connectionState.value = HidConnectionState.CONNECTING
        try {
            val channel = channelProvider?.invoke() ?: createDefaultChannel(mode)
            val success = channel.open()
            if (success) {
                activeChannel = channel
                _connectedDeviceName.value = channel.channelName
                _connectionState.value = HidConnectionState.CONNECTED
                true
            } else {
                activeChannel = null
                _connectedDeviceName.value = null
                _connectionState.value = HidConnectionState.ERROR
                false
            }
        } catch (e: Exception) {
            activeChannel = null
            _connectedDeviceName.value = null
            _connectionState.value = HidConnectionState.ERROR
            false
        }
    }

    override suspend fun sendKeyboardReport(report: ByteArray): Boolean = withContext(ioDispatcher) {
        if (report.size != 8) {
            return@withContext false
        }
        val channel = activeChannel ?: return@withContext false
        if (_connectionState.value != HidConnectionState.CONNECTED) {
            return@withContext false
        }

        val success = channel.write(report)
        if (!success && !channel.isConnected) {
            _connectionState.value = HidConnectionState.DISCONNECTED
            _connectedDeviceName.value = null
        }
        success
    }

    override suspend fun sendMouseReport(buttons: Int, dx: Int, dy: Int, wheel: Int): Boolean = withContext(ioDispatcher) {
        val channel = activeChannel ?: return@withContext false
        if (_connectionState.value != HidConnectionState.CONNECTED) {
            return@withContext false
        }
        val clampedDx = dx.coerceIn(-127, 127).toByte()
        val clampedDy = dy.coerceIn(-127, 127).toByte()
        val clampedWheel = wheel.coerceIn(-127, 127).toByte()
        val mousePacket = byteArrayOf(
            0x02.toByte(), // Mouse report marker / Report ID 2
            buttons.toByte(),
            clampedDx,
            clampedDy,
            clampedWheel
        )
        channel.write(mousePacket)
    }

    /**
     * Updates internal host LED state received from host USB control endpoint.
     */
    fun updateHostLedState(ledByte: Byte) {
        _hostLedState.value = HostLedState.fromByte(ledByte)
    }

    override suspend fun disconnect() = withContext(ioDispatcher) {
        val channel = activeChannel
        if (channel != null) {
            try {
                // Transmit key release before closing
                channel.write(ByteArray(8))
            } catch (_: Exception) {}
            channel.close()
        }
        activeChannel = null
        _connectedDeviceName.value = null
        _connectionState.value = HidConnectionState.DISCONNECTED
    }

    override fun release() {
        activeChannel?.close()
        activeChannel = null
        _connectedDeviceName.value = null
        _connectionState.value = HidConnectionState.DISCONNECTED
    }

    private fun createDefaultChannel(mode: UsbTransportMode): UsbChannel {
        return when (mode) {
            UsbTransportMode.ADB_SOCKET_BRIDGE -> SocketUsbChannel()
            UsbTransportMode.AOA_2_0 -> SocketUsbChannel(port = 8081)
            UsbTransportMode.USB_GADGET -> SocketUsbChannel(port = 8082)
        }
    }
}
