package com.transcriptor.hid.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.transcriptor.hid.data.db.PairedHostEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.Executor

/**
 * Universal 154-byte Composite HID Report Descriptor for Keyboard (Report ID 1), Mouse (Report ID 2),
 * and Consumer Media Control (Report ID 3).
 * Conforms strictly to USB HID 1.11 and Bluetooth HID Profile 1.0/1.1 specifications.
 * Defines:
 * - Report ID 1: Standard 8-byte Keyboard input report + 1-byte LED output report (Usage Max 0x73 covers F1-F24).
 * - Report ID 2: 4-byte Relative Mouse input report ([Buttons, dX, dY, Wheel]).
 * - Report ID 3: 2-byte Consumer Media Control report.
 */
val ENHANCED_154_BYTE_REPORT_DESCRIPTOR: ByteArray = byteArrayOf(
    // ------------------------------------------------------------------------
    // KEYBOARD (Report ID 1: 65 Bytes)
    // ------------------------------------------------------------------------
    0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop Controls: 0x01)
    0x09.toByte(), 0x06.toByte(), // USAGE (Keyboard: 0x06)
    0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application: 0x01)
    0x85.toByte(), 0x01.toByte(), //   REPORT_ID (1)
    0x05.toByte(), 0x07.toByte(), //   USAGE_PAGE (Keyboard/Keypad: 0x07)
    0x19.toByte(), 0xE0.toByte(), //   USAGE_MINIMUM (Keyboard LeftControl: 0xE0)
    0x29.toByte(), 0xE7.toByte(), //   USAGE_MAXIMUM (Keyboard Right GUI: 0xE7)
    0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
    0x25.toByte(), 0x01.toByte(), //   LOGICAL_MAXIMUM (1)
    0x75.toByte(), 0x01.toByte(), //   REPORT_SIZE (1 bit)
    0x95.toByte(), 0x08.toByte(), //   REPORT_COUNT (8 fields -> 8 bits = 1 byte modifier field)
    0x81.toByte(), 0x02.toByte(), //   INPUT (Data, Variable, Absolute) -> [Byte 0: Modifier Bitmask]
    0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1 field)
    0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8 bits = 1 byte)
    0x81.toByte(), 0x01.toByte(), //   INPUT (Constant, Array, Absolute) -> [Byte 1: Reserved OEM Byte]
    0x95.toByte(), 0x05.toByte(), //   REPORT_COUNT (5 fields)
    0x75.toByte(), 0x01.toByte(), //   REPORT_SIZE (1 bit)
    0x05.toByte(), 0x08.toByte(), //   USAGE_PAGE (LEDs: 0x08)
    0x19.toByte(), 0x01.toByte(), //   USAGE_MINIMUM (Num Lock: 0x01)
    0x29.toByte(), 0x05.toByte(), //   USAGE_MAXIMUM (Kana: 0x05)
    0x91.toByte(), 0x02.toByte(), //   OUTPUT (Data, Variable, Absolute) -> [Output Byte 0, Bits 0-4: LEDs]
    0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1 field)
    0x75.toByte(), 0x03.toByte(), //   REPORT_SIZE (3 bits)
    0x91.toByte(), 0x01.toByte(), //   OUTPUT (Constant, Array, Absolute) -> [Output Byte 0, Bits 5-7: Padding]
    0x95.toByte(), 0x06.toByte(), //   REPORT_COUNT (6 fields -> 6 simultaneous key slots)
    0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8 bits = 1 byte per key slot)
    0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
    0x25.toByte(), 0x73.toByte(), //   LOGICAL_MAXIMUM (115 keys: covers up to F24: 0x73)
    0x05.toByte(), 0x07.toByte(), //   USAGE_PAGE (Keyboard/Keypad: 0x07)
    0x19.toByte(), 0x00.toByte(), //   USAGE_MINIMUM (0x00 - No event)
    0x29.toByte(), 0x73.toByte(), //   USAGE_MAXIMUM (0x73 - F24)
    0x81.toByte(), 0x00.toByte(), //   INPUT (Data, Array, Absolute) -> [Bytes 2..7: 6-Key Rollover Array]
    0xC0.toByte(),                 // END_COLLECTION

    // ------------------------------------------------------------------------
    // MOUSE (Report ID 2: 64 Bytes)
    // ------------------------------------------------------------------------
    0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop Controls: 0x01)
    0x09.toByte(), 0x02.toByte(), // USAGE (Mouse: 0x02)
    0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application: 0x01)
    0x85.toByte(), 0x02.toByte(), //   REPORT_ID (2)
    0x09.toByte(), 0x01.toByte(), //   USAGE (Pointer: 0x01)
    0xA1.toByte(), 0x00.toByte(), //   COLLECTION (Physical: 0x00)
    0x05.toByte(), 0x09.toByte(), //     USAGE_PAGE (Button: 0x09)
    0x19.toByte(), 0x01.toByte(), //     USAGE_MINIMUM (Button 1: Left: 0x01)
    0x29.toByte(), 0x03.toByte(), //     USAGE_MAXIMUM (Button 3: Middle: 0x03)
    0x15.toByte(), 0x00.toByte(), //     LOGICAL_MINIMUM (0)
    0x25.toByte(), 0x01.toByte(), //     LOGICAL_MAXIMUM (1)
    0x75.toByte(), 0x01.toByte(), //     REPORT_SIZE (1 bit)
    0x95.toByte(), 0x03.toByte(), //     REPORT_COUNT (3 fields -> 3 buttons)
    0x81.toByte(), 0x02.toByte(), //     INPUT (Data, Variable, Absolute) -> [Bits 0-2: Left/Right/Middle]
    0x75.toByte(), 0x05.toByte(), //     REPORT_SIZE (5 bits)
    0x95.toByte(), 0x01.toByte(), //     REPORT_COUNT (1 field)
    0x81.toByte(), 0x01.toByte(), //     INPUT (Constant, Array, Absolute) -> [Bits 3-7: Padding]
    0x05.toByte(), 0x01.toByte(), //     USAGE_PAGE (Generic Desktop Controls: 0x01)
    0x09.toByte(), 0x30.toByte(), //     USAGE (X: 0x30)
    0x09.toByte(), 0x31.toByte(), //     USAGE (Y: 0x31)
    0x15.toByte(), 0x81.toByte(), //     LOGICAL_MINIMUM (-127)
    0x25.toByte(), 0x7F.toByte(), //     LOGICAL_MAXIMUM (127)
    0x75.toByte(), 0x08.toByte(), //     REPORT_SIZE (8 bits)
    0x95.toByte(), 0x02.toByte(), //     REPORT_COUNT (2 fields -> X and Y relative deltas)
    0x81.toByte(), 0x06.toByte(), //     INPUT (Data, Variable, Relative) -> [dX, dY]
    0x09.toByte(), 0x38.toByte(), //     USAGE (Wheel: 0x38)
    0x15.toByte(), 0x81.toByte(), //     LOGICAL_MINIMUM (-127)
    0x25.toByte(), 0x7F.toByte(), //     LOGICAL_MAXIMUM (127)
    0x75.toByte(), 0x08.toByte(), //     REPORT_SIZE (8 bits)
    0x95.toByte(), 0x01.toByte(), //     REPORT_COUNT (1 field -> Wheel delta)
    0x81.toByte(), 0x06.toByte(), //     INPUT (Data, Variable, Relative) -> [Wheel]
    0xC0.toByte(),                 //   END_COLLECTION
    0xC0.toByte(),                 // END_COLLECTION

    // ------------------------------------------------------------------------
    // CONSUMER MEDIA CONTROL (Report ID 3: 25 Bytes)
    // ------------------------------------------------------------------------
    0x05.toByte(), 0x0C.toByte(), // USAGE_PAGE (Consumer Devices: 0x0C)
    0x09.toByte(), 0x01.toByte(), // USAGE (Consumer Control: 0x01)
    0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application: 0x01)
    0x85.toByte(), 0x03.toByte(), //   REPORT_ID (3)
    0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
    0x26.toByte(), 0xFF.toByte(), 0x03.toByte(), // LOGICAL_MAXIMUM (1023: 0x03FF)
    0x19.toByte(), 0x00.toByte(), //   USAGE_MINIMUM (0)
    0x2A.toByte(), 0xFF.toByte(), 0x03.toByte(), // USAGE_MAXIMUM (1023)
    0x75.toByte(), 0x10.toByte(), //   REPORT_SIZE (16 bits = 2 bytes)
    0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1 field)
    0x81.toByte(), 0x00.toByte(), //   INPUT (Data, Array, Absolute -> 2-byte Consumer Usage ID)
    0xC0.toByte()                  // END_COLLECTION
)

val HID_COMBO_REPORT_DESCRIPTOR: ByteArray get() = ENHANCED_154_BYTE_REPORT_DESCRIPTOR
val HID_KEYBOARD_REPORT_DESCRIPTOR: ByteArray get() = ENHANCED_154_BYTE_REPORT_DESCRIPTOR

/**
 * Service Discovery Protocol (SDP) configuration data model.
 */
data class HidSdpConfiguration(
    val name: String = "Type4Me Keyboard",
    val description: String = "Voice-to-HID Speech Input & Touchpad Companion",
    val provider: String = "Type4Me",
    val subclass: Byte = 0xC0.toByte(), // Combo Keyboard + Mouse
    val descriptors: ByteArray = HID_KEYBOARD_REPORT_DESCRIPTOR
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as HidSdpConfiguration
        return name == other.name &&
                description == other.description &&
                provider == other.provider &&
                subclass == other.subclass &&
                descriptors.contentEquals(other.descriptors)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + provider.hashCode()
        result = 31 * result + subclass
        result = 31 * result + descriptors.contentHashCode()
        return result
    }
}

/**
 * Quality of Service (QoS) parameters data model.
 */
data class HidQosConfiguration(
    val serviceType: Int = 1, // SERVICE_BEST_EFFORT
    val tokenRate: Int = 800,
    val tokenBucketSize: Int = 9,
    val peakBandwidth: Int = 800,
    val latency: Int = 10000,
    val delayVariation: Int = -1
)

/**
 * Callback interface for Bluetooth HID events.
 */
interface BluetoothHidCallback {
    fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean)
    fun onConnectionStateChanged(device: BluetoothDevice, state: Int)
    fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int)
    fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray)
    fun onSetProtocol(device: BluetoothDevice, protocol: Byte)
    fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray)
    fun onVirtualCableUnplug(device: BluetoothDevice)
}

/**
 * Abstraction layer over [android.bluetooth.BluetoothHidDevice] for testability.
 */
interface BluetoothHidDeviceAdapter {
    fun registerApp(
        sdpConfig: HidSdpConfiguration,
        inQos: HidQosConfiguration?,
        outQos: HidQosConfiguration?,
        executor: Executor,
        callback: BluetoothHidCallback
    ): Boolean

    fun unregisterApp(): Boolean
    fun sendReport(device: BluetoothDevice, id: Int, data: ByteArray): Boolean
    fun replyReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray): Boolean
    fun reportError(device: BluetoothDevice, error: Byte): Boolean
    fun connect(device: BluetoothDevice): Boolean
    fun disconnect(device: BluetoothDevice): Boolean
    fun getConnectedDevices(): List<BluetoothDevice>
    fun getConnectionState(device: BluetoothDevice): Int
}

/**
 * Default implementation of [BluetoothHidDeviceAdapter] delegating to Android SDK [BluetoothHidDevice].
 */
class DefaultBluetoothHidDeviceAdapter(
    private val hidDevice: BluetoothHidDevice
) : BluetoothHidDeviceAdapter {
    override fun registerApp(
        sdpConfig: HidSdpConfiguration,
        inQos: HidQosConfiguration?,
        outQos: HidQosConfiguration?,
        executor: Executor,
        callback: BluetoothHidCallback
    ): Boolean {
        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            sdpConfig.name,
            sdpConfig.description,
            sdpConfig.provider,
            sdpConfig.subclass,
            sdpConfig.descriptors
        )
        val inQosSettings = inQos?.let {
            BluetoothHidDeviceAppQosSettings(
                it.serviceType,
                it.tokenRate,
                it.tokenBucketSize,
                it.peakBandwidth,
                it.latency,
                it.delayVariation
            )
        }
        val outQosSettings = outQos?.let {
            BluetoothHidDeviceAppQosSettings(
                it.serviceType,
                it.tokenRate,
                it.tokenBucketSize,
                it.peakBandwidth,
                it.latency,
                it.delayVariation
            )
        }

        val nativeCallback = object : BluetoothHidDevice.Callback() {
            override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                callback.onAppStatusChanged(pluggedDevice, registered)
            }

            override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
                callback.onConnectionStateChanged(device, state)
            }

            override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
                callback.onGetReport(device, type, id, bufferSize)
            }

            override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
                callback.onSetReport(device, type, id, data)
            }

            override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
                callback.onSetProtocol(device, protocol)
            }

            override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
                callback.onInterruptData(device, reportId, data)
            }

            override fun onVirtualCableUnplug(device: BluetoothDevice) {
                callback.onVirtualCableUnplug(device)
            }
        }

        return hidDevice.registerApp(sdpSettings, inQosSettings, outQosSettings, executor, nativeCallback)
    }

    override fun unregisterApp(): Boolean = hidDevice.unregisterApp()
    override fun sendReport(device: BluetoothDevice, id: Int, data: ByteArray): Boolean = hidDevice.sendReport(device, id, data)
    override fun replyReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray): Boolean = hidDevice.replyReport(device, type, id, data)
    override fun reportError(device: BluetoothDevice, error: Byte): Boolean = hidDevice.reportError(device, error)
    override fun connect(device: BluetoothDevice): Boolean = hidDevice.connect(device)
    override fun disconnect(device: BluetoothDevice): Boolean = hidDevice.disconnect(device)
    override fun getConnectedDevices(): List<BluetoothDevice> = hidDevice.connectedDevices
    override fun getConnectionState(device: BluetoothDevice): Int = hidDevice.getConnectionState(device)
}

/**
 * Bluetooth HID Keyboard & Composite Peripheral Transport.
 *
 * Interacts with Android's [BluetoothHidDevice] stack to emulate a plug-and-play hardware keyboard,
 * mouse, and consumer control peripheral with 6-Phase Atomic Multi-Host Quick-Switching.
 */
class BluetoothHidTransport(
    private val context: Context? = null,
    private val bluetoothAdapter: BluetoothAdapter? = null,
    private val hidAdapterProvider: ((BluetoothProfile.ServiceListener) -> Boolean)? = null,
    private val hidAdapterFactory: ((Any) -> BluetoothHidDeviceAdapter)? = null
) : HidTransport {

    companion object {
        const val DEVICE_NAME = "Type4Me Keyboard"
        const val DEVICE_DESCRIPTION = "Voice-to-HID Speech Input & Touchpad Companion"
        const val DEVICE_PROVIDER = "Type4Me"
        const val REPORT_ID_KEYBOARD = 1
        const val REPORT_ID_MOUSE = 2
        const val REPORT_ID_CONSUMER = 3
        const val SDP_SUBCLASS_KEYBOARD: Byte = 0x40.toByte()
        const val SDP_SUBCLASS_COMBO: Byte = 0xC0.toByte()

        const val MOUSE_BUTTON_NONE = 0x00
        const val MOUSE_BUTTON_LEFT = 0x01
        const val MOUSE_BUTTON_RIGHT = 0x02
        const val MOUSE_BUTTON_MIDDLE = 0x04
    }

    private val _connectionState = MutableStateFlow(HidConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<HidConnectionState> = _connectionState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    override val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _hostLedState = MutableStateFlow(HostLedState.ALL_OFF)
    override val hostLedState: StateFlow<HostLedState> = _hostLedState.asStateFlow()

    private val _multiHostState = MutableStateFlow<MultiHostConnectionState>(MultiHostConnectionState.Disconnected)
    val multiHostState: StateFlow<MultiHostConnectionState> = _multiHostState.asStateFlow()

    private val switchingMutex = Mutex()
    var currentPairedHost: PairedHostEntity? = null
        private set

    private val _isAppRegistered = MutableStateFlow(false)
    val isAppRegistered: StateFlow<Boolean> = _isAppRegistered.asStateFlow()

    var activeDevice: BluetoothDevice? = null
        private set

    private var hidAdapter: BluetoothHidDeviceAdapter? = null
    private var rawHidDeviceProxy: BluetoothProfile? = null
    private var currentInputReport = ByteArray(8)

    /**
     * The Composite 154-byte Report Descriptor (Keyboard + Mouse + Consumer Media Control).
     */
    val reportDescriptor: ByteArray
        get() = ENHANCED_154_BYTE_REPORT_DESCRIPTOR

    /**
     * SDP Settings configuration with Subclass 0xC0 (Combo Keyboard + Mouse) and 154B Composite descriptor.
     */
    val sdpConfig: HidSdpConfiguration = HidSdpConfiguration(
        name = DEVICE_NAME,
        description = DEVICE_DESCRIPTION,
        provider = DEVICE_PROVIDER,
        subclass = SDP_SUBCLASS_COMBO,
        descriptors = ENHANCED_154_BYTE_REPORT_DESCRIPTOR
    )

    /**
     * Best-effort QoS settings for low-latency keystroke transmission.
     */
    val qosConfig: HidQosConfiguration = HidQosConfiguration(
        serviceType = 1,
        tokenRate = 800,
        tokenBucketSize = 9,
        peakBandwidth = 800,
        latency = 10000,
        delayVariation = -1
    )

    /**
     * BluetoothProfile.ServiceListener for acquiring and releasing the HID profile proxy.
     */
    val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                rawHidDeviceProxy = proxy
                val adapter = hidAdapterFactory?.invoke(proxy)
                    ?: DefaultBluetoothHidDeviceAdapter(proxy as BluetoothHidDevice)
                hidAdapter = adapter
                registerAppInternal(adapter)
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidAdapter = null
                rawHidDeviceProxy = null
                _isAppRegistered.value = false
                activeDevice = null
                currentPairedHost = null
                _connectedDeviceName.value = null
                _connectionState.value = HidConnectionState.DISCONNECTED
                _multiHostState.value = MultiHostConnectionState.Disconnected
            }
        }
    }

    /**
     * Callback implementation handling host pairing, connection state,
     * GET_REPORT / SET_REPORT handshakes, and host LED updates.
     */
    val hidCallback: BluetoothHidCallback = object : BluetoothHidCallback {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            _isAppRegistered.value = registered
            if (!registered) {
                _connectionState.value = HidConnectionState.DISCONNECTED
                activeDevice = null
                currentPairedHost = null
                _connectedDeviceName.value = null
                _multiHostState.value = MultiHostConnectionState.Disconnected
            } else if (pluggedDevice != null) {
                activeDevice = pluggedDevice
                _connectedDeviceName.value = getDeviceDisplayName(pluggedDevice)
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    activeDevice = device
                    _connectedDeviceName.value = getDeviceDisplayName(device)
                    _connectionState.value = HidConnectionState.CONNECTED
                    val host = currentPairedHost
                    if (host != null && (host.address.equals(device.address, ignoreCase = true))) {
                        _multiHostState.value = MultiHostConnectionState.Connected(host)
                    }
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    activeDevice = device
                    _connectedDeviceName.value = getDeviceDisplayName(device)
                    _connectionState.value = HidConnectionState.CONNECTING
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    _connectionState.value = HidConnectionState.DISCONNECTED
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (isSameDevice(activeDevice, device)) {
                        activeDevice = null
                        currentPairedHost = null
                        _connectedDeviceName.value = null
                        _connectionState.value = HidConnectionState.DISCONNECTED
                        _multiHostState.value = MultiHostConnectionState.Disconnected
                    }
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            if (type == 1.toByte() && id == 0.toByte()) { // REPORT_TYPE_INPUT = 1
                hidAdapter?.replyReport(device, type, id, currentInputReport.copyOf())
            } else {
                hidAdapter?.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
            }
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            if (type == 2.toByte() && data.isNotEmpty()) { // REPORT_TYPE_OUTPUT = 2
                _hostLedState.value = HostLedState.fromByte(data[0])
                hidAdapter?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
            } else {
                hidAdapter?.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
            }
        }

        override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
            hidAdapter?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            if (data.isNotEmpty()) {
                _hostLedState.value = HostLedState.fromByte(data[0])
            }
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            if (isSameDevice(activeDevice, device)) {
                activeDevice = null
                currentPairedHost = null
                _connectedDeviceName.value = null
                _connectionState.value = HidConnectionState.DISCONNECTED
                _multiHostState.value = MultiHostConnectionState.Disconnected
            }
        }
    }

    override suspend fun initialize(): Boolean {
        if (hidAdapterProvider != null) {
            return hidAdapterProvider.invoke(serviceListener)
        }

        val adapter = bluetoothAdapter
            ?: (context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = HidConnectionState.ERROR
            return false
        }

        return try {
            val ctx = context ?: return false
            val proxyRequested = adapter.getProfileProxy(
                ctx,
                serviceListener,
                BluetoothProfile.HID_DEVICE
            )
            if (!proxyRequested) {
                _connectionState.value = HidConnectionState.ERROR
            }
            proxyRequested
        } catch (e: Exception) {
            _connectionState.value = HidConnectionState.ERROR
            false
        }
    }

    private fun registerAppInternal(adapter: BluetoothHidDeviceAdapter): Boolean {
        val executor = createDirectExecutor()
        return try {
            adapter.registerApp(
                sdpConfig,
                qosConfig,
                null,
                executor,
                hidCallback
            )
        } catch (e: Exception) {
            _connectionState.value = HidConnectionState.ERROR
            false
        }
    }

    /**
     * Connects to a specific paired [BluetoothDevice] host.
     */
    fun connectHost(device: BluetoothDevice): Boolean {
        val adapter = hidAdapter ?: return false
        _connectionState.value = HidConnectionState.CONNECTING
        activeDevice = device
        _connectedDeviceName.value = getDeviceDisplayName(device)
        return try {
            adapter.connect(device)
        } catch (e: Exception) {
            _connectionState.value = HidConnectionState.ERROR
            false
        }
    }

    /**
     * 6-Phase Atomic Multi-Host Quick-Switching Protocol.
     * Serialized with `switchingMutex` to protect the Android Bluetooth Fluoride/GD stack.
     *
     * Phase 1: Zero-report flush (Report IDs 1 & 2).
     * Phase 2: Teardown existing L2CAP link if active.
     * Phase 3: 150ms settling guard & dead-link watchdog (up to 1000ms).
     * Phase 4: Target L2CAP connection invocation.
     * Phase 5: Connection await with 5000ms timeout guard.
     * Phase 6: Active profile engagement.
     */
    suspend fun switchHost(target: PairedHostEntity): Boolean = switchingMutex.withLock {
        val adapter = hidAdapter ?: return@withLock false
        val btAdapter = bluetoothAdapter
            ?: (context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            ?: BluetoothAdapter.getDefaultAdapter()
            ?: return@withLock false

        val targetDevice = try {
            btAdapter.getRemoteDevice(target.address)
        } catch (_: Throwable) {
            _multiHostState.value = MultiHostConnectionState.Error("Invalid MAC address: ${target.address}", target)
            return@withLock false
        }

        val currentHost = currentPairedHost
        val currentDev = activeDevice

        // Phase 1: Flush / Zero Key & Mouse Release
        if (currentDev != null) {
            try {
                adapter.sendReport(currentDev, REPORT_ID_KEYBOARD, ByteArray(8))
                adapter.sendReport(currentDev, REPORT_ID_MOUSE, ByteArray(4))
            } catch (_: Throwable) {}
        }

        // Phase 2: Disconnect Current Host if connected
        if (currentDev != null) {
            if (currentHost != null) {
                _multiHostState.value = MultiHostConnectionState.SwitchingHost(currentHost, target)
            } else {
                _multiHostState.value = MultiHostConnectionState.Disconnecting(target)
            }
            try {
                adapter.disconnect(currentDev)
            } catch (_: Throwable) {}

            // Phase 3: Settling Guard & Dead-Link Watchdog (150ms settling, up to 1000ms timeout)
            val disconnectDeadline = System.currentTimeMillis() + 1000L
            while (activeDevice != null && System.currentTimeMillis() < disconnectDeadline) {
                delay(50L)
            }
            delay(150L) // Crucial L2CAP recycle settling delay
        }

        // Phase 4: Target Connection Invocation
        _multiHostState.value = MultiHostConnectionState.Connecting(target)
        _connectionState.value = HidConnectionState.CONNECTING
        _connectedDeviceName.value = target.customAlias.ifBlank { target.hostName }

        val connectInitiated = try {
            adapter.connect(targetDevice)
        } catch (e: Exception) {
            false
        }

        if (!connectInitiated) {
            _multiHostState.value = MultiHostConnectionState.Error("Failed to initiate connection to ${target.hostName}", target)
            _connectionState.value = HidConnectionState.ERROR
            return@withLock false
        }

        // Phase 5: Connection Await & Timeout Guard (up to 5000ms)
        val connectDeadline = System.currentTimeMillis() + 5000L
        var connected = false
        while (System.currentTimeMillis() < connectDeadline) {
            if (_connectionState.value == HidConnectionState.CONNECTED && isSameDevice(activeDevice, targetDevice)) {
                connected = true
                break
            }
            if (_connectionState.value == HidConnectionState.ERROR) {
                break
            }
            delay(50L)
        }

        if (connected) {
            // Phase 6: Active Profile Engagement
            currentPairedHost = target
            _multiHostState.value = MultiHostConnectionState.Connected(target)
            true
        } else {
            _multiHostState.value = MultiHostConnectionState.Error("Connection to ${target.hostName} timed out", target)
            _connectionState.value = HidConnectionState.DISCONNECTED
            false
        }
    }

    /**
     * Retrieves all bonded (paired) Bluetooth devices.
     */
    fun getBondedDevices(): List<BluetoothDevice> {
        val adapter = bluetoothAdapter
            ?: (context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            ?: BluetoothAdapter.getDefaultAdapter()
        return try {
            adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Connects to a host by its MAC address.
     */
    fun connectDeviceByAddress(address: String): Boolean {
        val adapter = bluetoothAdapter
            ?: (context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            ?: BluetoothAdapter.getDefaultAdapter()
            ?: return false
        val device = try {
            adapter.getRemoteDevice(address)
        } catch (_: Throwable) {
            return false
        }
        return connectHost(device)
    }

    override suspend fun sendKeyboardReport(report: ByteArray): Boolean {
        if (report.size != 8) {
            return false
        }
        val device = activeDevice ?: return false
        val adapter = hidAdapter ?: return false

        if (_connectionState.value != HidConnectionState.CONNECTED) {
            return false
        }

        currentInputReport = report.copyOf()
        return try {
            adapter.sendReport(device, REPORT_ID_KEYBOARD, report)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendMouseReport(buttons: Int, dx: Int, dy: Int, wheel: Int): Boolean {
        val device = activeDevice ?: return false
        val adapter = hidAdapter ?: return false

        if (_connectionState.value != HidConnectionState.CONNECTED) {
            return false
        }

        val clampedDx = dx.coerceIn(-127, 127).toByte()
        val clampedDy = dy.coerceIn(-127, 127).toByte()
        val clampedWheel = wheel.coerceIn(-127, 127).toByte()
        val mouseReport = byteArrayOf(
            buttons.toByte(),
            clampedDx,
            clampedDy,
            clampedWheel
        )
        return try {
            adapter.sendReport(device, REPORT_ID_MOUSE, mouseReport)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Sends a 2-byte Consumer Media Control report on Report ID 3 (Page 0x0C).
     * Transmits in Little-Endian format.
     */
    suspend fun sendConsumerReport(usageId: Int): Boolean {
        val device = activeDevice ?: return false
        val adapter = hidAdapter ?: return false

        if (_connectionState.value != HidConnectionState.CONNECTED) {
            return false
        }

        val low = (usageId and 0xFF).toByte()
        val high = ((usageId shr 8) and 0xFF).toByte()
        val report = byteArrayOf(low, high)

        return try {
            adapter.sendReport(device, REPORT_ID_CONSUMER, report)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Convenience helper to send an all-zero key release report.
     */
    suspend fun sendKeyRelease(): Boolean {
        return sendKeyboardReport(ByteArray(8))
    }

    /**
     * Convenience helper to send a mouse button release report.
     */
    suspend fun sendMouseRelease(): Boolean {
        return sendMouseReport(buttons = MOUSE_BUTTON_NONE, dx = 0, dy = 0, wheel = 0)
    }

    /**
     * Toggles/resets consumer media key release.
     */
    suspend fun sendConsumerRelease(): Boolean {
        return sendConsumerReport(0)
    }

    override suspend fun disconnect() {
        val device = activeDevice
        val adapter = hidAdapter
        if (device != null && adapter != null) {
            try {
                // Transmit Release reports for Report ID 1 and Report ID 2 before tearing down connection
                adapter.sendReport(device, REPORT_ID_KEYBOARD, ByteArray(8))
                adapter.sendReport(device, REPORT_ID_MOUSE, ByteArray(4))
                adapter.disconnect(device)
            } catch (_: Exception) {}
        }
        activeDevice = null
        currentPairedHost = null
        _connectedDeviceName.value = null
        _connectionState.value = HidConnectionState.DISCONNECTED
        _multiHostState.value = MultiHostConnectionState.Disconnected
    }

    override fun release() {
        try {
            hidAdapter?.unregisterApp()
        } catch (_: Exception) {}

        try {
            rawHidDeviceProxy?.let { proxy ->
                val adapter = bluetoothAdapter
                    ?: (context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, proxy)
            }
        } catch (_: Exception) {}

        hidAdapter = null
        rawHidDeviceProxy = null
        _isAppRegistered.value = false
        activeDevice = null
        currentPairedHost = null
        _connectedDeviceName.value = null
        _connectionState.value = HidConnectionState.DISCONNECTED
        _multiHostState.value = MultiHostConnectionState.Disconnected
    }

    private fun isSameDevice(dev1: BluetoothDevice?, dev2: BluetoothDevice?): Boolean {
        if (dev1 === dev2) return true
        if (dev1 == null || dev2 == null) return false
        return try {
            dev1.address == dev2.address
        } catch (_: Throwable) {
            dev1 == dev2
        }
    }

    private fun getDeviceDisplayName(device: BluetoothDevice?): String {
        if (device == null) return "Host PC"
        return try {
            device.name?.takeIf { it.isNotBlank() } ?: device.address
        } catch (_: Throwable) {
            "Host PC"
        }
    }

    private fun createDirectExecutor(): Executor {
        return try {
            val mainLooper = Looper.getMainLooper()
            if (mainLooper != null) {
                val handler = Handler(mainLooper)
                Executor { command -> handler.post(command) }
            } else {
                Executor { command -> command.run() }
            }
        } catch (_: Exception) {
            Executor { command -> command.run() }
        }
    }
}
