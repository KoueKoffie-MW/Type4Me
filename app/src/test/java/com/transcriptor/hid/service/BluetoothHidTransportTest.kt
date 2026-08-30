package com.transcriptor.hid.service

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import io.mockk.mockk
import java.util.concurrent.Executor

/**
 * Unit tests for [BluetoothHidTransport], Report Descriptor, SDP settings,
 * connection lifecycle, report transmissions, and LED synchronization.
 */
class BluetoothHidTransportTest {

    private lateinit var fakeAdapter: FakeBluetoothHidDeviceAdapter
    private lateinit var transport: BluetoothHidTransport

    private class FakeBluetoothHidDeviceAdapter : BluetoothHidDeviceAdapter {
        var registeredSdp: HidSdpConfiguration? = null
        var registeredCallback: BluetoothHidCallback? = null
        var isRegistered = false
        val sentReports = mutableListOf<ByteArray>()
        var lastRepliedReport: ByteArray? = null
        var lastErrorCode: Byte? = null
        var connectedDevice: BluetoothDevice? = null
        var connectionState: Int = BluetoothProfile.STATE_DISCONNECTED

        override fun registerApp(
            sdpConfig: HidSdpConfiguration,
            inQos: HidQosConfiguration?,
            outQos: HidQosConfiguration?,
            executor: Executor,
            callback: BluetoothHidCallback
        ): Boolean {
            registeredSdp = sdpConfig
            registeredCallback = callback
            isRegistered = true
            return true
        }

        override fun unregisterApp(): Boolean {
            isRegistered = false
            registeredCallback = null
            return true
        }

        override fun sendReport(device: BluetoothDevice, id: Int, data: ByteArray): Boolean {
            sentReports.add(data.copyOf())
            return true
        }

        override fun replyReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray): Boolean {
            lastRepliedReport = data.copyOf()
            return true
        }

        override fun reportError(device: BluetoothDevice, error: Byte): Boolean {
            lastErrorCode = error
            return true
        }

        override fun connect(device: BluetoothDevice): Boolean {
            connectedDevice = device
            connectionState = BluetoothProfile.STATE_CONNECTING
            return true
        }

        override fun disconnect(device: BluetoothDevice): Boolean {
            connectionState = BluetoothProfile.STATE_DISCONNECTED
            connectedDevice = null
            return true
        }

        override fun getConnectedDevices(): List<BluetoothDevice> =
            connectedDevice?.let { listOf(it) } ?: emptyList()

        override fun getConnectionState(device: BluetoothDevice): Int = connectionState
    }

    private fun createMockDevice(): BluetoothDevice {
        return mockk<BluetoothDevice>(relaxed = true)
    }

    @Before
    fun setUp() {
        fakeAdapter = FakeBluetoothHidDeviceAdapter()
        transport = BluetoothHidTransport(
            context = null,
            hidAdapterProvider = { listener ->
                val fakeProxy = mockk<BluetoothHidDevice>(relaxed = true)
                listener.onServiceConnected(BluetoothProfile.HID_DEVICE, fakeProxy)
                true
            },
            hidAdapterFactory = { fakeAdapter }
        )
    }

    @Test
    fun testReportDescriptorSpecification() {
        val desc = HID_COMBO_REPORT_DESCRIPTOR
        assertEquals("Report descriptor must be exactly 129 bytes (Composite Keyboard + Mouse)", 129, desc.size)

        // Verify Usage Page (Generic Desktop: 0x05, 0x01)
        assertEquals(0x05.toByte(), desc[0])
        assertEquals(0x01.toByte(), desc[1])

        // Verify Usage (Keyboard: 0x09, 0x06)
        assertEquals(0x09.toByte(), desc[2])
        assertEquals(0x06.toByte(), desc[3])

        // Verify Collection (Application: 0xA1, 0x01)
        assertEquals(0xA1.toByte(), desc[4])
        assertEquals(0x01.toByte(), desc[5])

        // Verify End Collection (0xC0)
        assertEquals(0xC0.toByte(), desc[128])
    }

    @Test
    fun testSdpSettingsParameters() {
        val sdp = transport.sdpConfig
        assertNotNull(sdp)
        assertArrayEquals(HID_COMBO_REPORT_DESCRIPTOR, transport.reportDescriptor)
        assertEquals(0xC0.toByte(), BluetoothHidTransport.SDP_SUBCLASS_COMBO)
        assertEquals("Type4Me Keyboard", sdp.name)
        assertEquals("Type4Me", sdp.provider)
    }

    @Test
    fun testQosSettingsParameters() {
        val qos = transport.qosConfig
        assertNotNull(qos)
        assertEquals(800, qos.tokenRate)
        assertEquals(9, qos.tokenBucketSize)
        assertEquals(10000, qos.latency)
    }

    @Test
    fun testInitializationAndRegistration() = runBlocking {
        assertEquals(HidConnectionState.DISCONNECTED, transport.connectionState.value)
        assertFalse(transport.isAppRegistered.value)

        val initSuccess = transport.initialize()
        assertTrue("Initialization must succeed with test provider", initSuccess)
        assertTrue("Adapter must be registered", fakeAdapter.isRegistered)

        // Trigger callback onAppStatusChanged
        fakeAdapter.registeredCallback?.onAppStatusChanged(null, true)
        assertTrue(transport.isAppRegistered.value)
    }

    @Test
    fun testConnectionStateLifecycle() = runBlocking {
        transport.initialize()
        val mockDev = createMockDevice()

        // 1. Connecting
        fakeAdapter.registeredCallback?.onConnectionStateChanged(mockDev, BluetoothProfile.STATE_CONNECTING)
        assertEquals(HidConnectionState.CONNECTING, transport.connectionState.value)

        // 2. Connected
        fakeAdapter.registeredCallback?.onConnectionStateChanged(mockDev, BluetoothProfile.STATE_CONNECTED)
        assertEquals(HidConnectionState.CONNECTED, transport.connectionState.value)
        assertSame(mockDev, transport.activeDevice)

        // 3. Disconnected
        fakeAdapter.registeredCallback?.onConnectionStateChanged(mockDev, BluetoothProfile.STATE_DISCONNECTED)
        assertEquals(HidConnectionState.DISCONNECTED, transport.connectionState.value)
        assertNull(transport.activeDevice)
    }

    @Test
    fun testReportTransmissionWhenConnected() = runBlocking {
        transport.initialize()
        val mockDev = createMockDevice()
        fakeAdapter.registeredCallback?.onConnectionStateChanged(mockDev, BluetoothProfile.STATE_CONNECTED)

        // Send valid 8-byte report: Mod Shift (0x02), Key 'A' (0x04)
        val validReport = byteArrayOf(0x02, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00)
        val sent = transport.sendKeyboardReport(validReport)
        assertTrue("Report transmission should succeed when connected", sent)
        assertEquals(1, fakeAdapter.sentReports.size)
        assertArrayEquals(validReport, fakeAdapter.sentReports[0])

        // Send release report
        val releaseSent = transport.sendKeyRelease()
        assertTrue(releaseSent)
        assertEquals(2, fakeAdapter.sentReports.size)
        assertArrayEquals(ByteArray(8), fakeAdapter.sentReports[1])
    }

    @Test
    fun testReportTransmissionRejectedWhenDisconnected() = runBlocking {
        transport.initialize()
        val validReport = byteArrayOf(0x02, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00)
        val sent = transport.sendKeyboardReport(validReport)
        assertFalse("Report transmission must fail when disconnected", sent)
        assertEquals(0, fakeAdapter.sentReports.size)
    }

    @Test
    fun testReportTransmissionRejectedWithInvalidLength() = runBlocking {
        transport.initialize()
        val mockDev = createMockDevice()
        fakeAdapter.registeredCallback?.onConnectionStateChanged(mockDev, BluetoothProfile.STATE_CONNECTED)

        // 5 bytes invalid size
        val invalidReport = byteArrayOf(0x02, 0x00, 0x04, 0x00, 0x00)
        val sent = transport.sendKeyboardReport(invalidReport)
        assertFalse("Invalid report size must be rejected", sent)
        assertEquals(0, fakeAdapter.sentReports.size)
    }

    @Test
    fun testMouseReportTransmissionAndClamping() = runBlocking {
        transport.initialize()
        val mockDev = createMockDevice()
        fakeAdapter.registeredCallback?.onConnectionStateChanged(mockDev, BluetoothProfile.STATE_CONNECTED)

        // Valid mouse report: Left Click + Move (dx=25, dy=-10, wheel=0)
        val sent = transport.sendMouseReport(buttons = BluetoothHidTransport.MOUSE_BUTTON_LEFT, dx = 25, dy = -10, wheel = 0)
        assertTrue("Mouse report must be sent when connected", sent)
        assertEquals(1, fakeAdapter.sentReports.size)
        val report = fakeAdapter.sentReports[0]
        assertEquals(4, report.size)
        assertEquals(BluetoothHidTransport.MOUSE_BUTTON_LEFT.toByte(), report[0])
        assertEquals(25.toByte(), report[1])
        assertEquals((-10).toByte(), report[2])
        assertEquals(0.toByte(), report[3])

        // Clamping test: dx=300 -> 127, dy=-200 -> -127
        transport.sendMouseReport(buttons = BluetoothHidTransport.MOUSE_BUTTON_RIGHT, dx = 300, dy = -200, wheel = 5)
        assertEquals(2, fakeAdapter.sentReports.size)
        val clampedReport = fakeAdapter.sentReports[1]
        assertEquals(BluetoothHidTransport.MOUSE_BUTTON_RIGHT.toByte(), clampedReport[0])
        assertEquals(127.toByte(), clampedReport[1])
        assertEquals((-127).toByte(), clampedReport[2])
        assertEquals(5.toByte(), clampedReport[3])
    }

    @Test
    fun testHostLedStateDecoding() = runBlocking {
        // NumLock (0x01)
        val numLockOnly = HostLedState.fromByte(0x01.toByte())
        assertTrue(numLockOnly.numLock)
        assertFalse(numLockOnly.capsLock)
        assertFalse(numLockOnly.scrollLock)

        // CapsLock (0x02)
        val capsLockOnly = HostLedState.fromByte(0x02.toByte())
        assertFalse(capsLockOnly.numLock)
        assertTrue(capsLockOnly.capsLock)
        assertFalse(capsLockOnly.scrollLock)

        // NumLock + CapsLock + ScrollLock (0x07)
        val allThree = HostLedState.fromByte(0x07.toByte())
        assertTrue(allThree.numLock)
        assertTrue(allThree.capsLock)
        assertTrue(allThree.scrollLock)

        // Test SetReport callback
        transport.initialize()
        val mockDev = createMockDevice()
        fakeAdapter.registeredCallback?.onSetReport(
            mockDev,
            2.toByte(), // REPORT_TYPE_OUTPUT
            0.toByte(),
            byteArrayOf(0x02)
        )
        assertTrue(transport.hostLedState.value.capsLock)
        assertEquals(BluetoothHidDevice.ERROR_RSP_SUCCESS, fakeAdapter.lastErrorCode)

        // Test InterruptData callback
        fakeAdapter.registeredCallback?.onInterruptData(
            mockDev,
            0.toByte(),
            byteArrayOf(0x01)
        )
        assertTrue(transport.hostLedState.value.numLock)
        assertFalse(transport.hostLedState.value.capsLock)
    }

    @Test
    fun testGetReportHandshake() = runBlocking {
        transport.initialize()
        val mockDev = createMockDevice()
        fakeAdapter.registeredCallback?.onConnectionStateChanged(mockDev, BluetoothProfile.STATE_CONNECTED)

        val report = byteArrayOf(0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00)
        transport.sendKeyboardReport(report)

        // Host requests input report
        fakeAdapter.registeredCallback?.onGetReport(
            mockDev,
            1.toByte(), // REPORT_TYPE_INPUT
            0.toByte(),
            8
        )
        assertNotNull(fakeAdapter.lastRepliedReport)
        assertArrayEquals(report, fakeAdapter.lastRepliedReport)

        // Host requests unsupported report type
        fakeAdapter.registeredCallback?.onGetReport(
            mockDev,
            3.toByte(), // REPORT_TYPE_FEATURE
            1.toByte(),
            8
        )
        assertEquals(BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ, fakeAdapter.lastErrorCode)
    }

    @Test
    fun testVirtualCableUnplugResetsState() = runBlocking {
        transport.initialize()
        val mockDev = createMockDevice()
        fakeAdapter.registeredCallback?.onConnectionStateChanged(mockDev, BluetoothProfile.STATE_CONNECTED)
        assertEquals(HidConnectionState.CONNECTED, transport.connectionState.value)

        fakeAdapter.registeredCallback?.onVirtualCableUnplug(mockDev)
        assertEquals(HidConnectionState.DISCONNECTED, transport.connectionState.value)
        assertNull(transport.activeDevice)
    }

    @Test
    fun testDisconnectAndRelease() = runBlocking {
        transport.initialize()
        val mockDev = createMockDevice()
        fakeAdapter.registeredCallback?.onConnectionStateChanged(mockDev, BluetoothProfile.STATE_CONNECTED)

        transport.disconnect()
        assertEquals(HidConnectionState.DISCONNECTED, transport.connectionState.value)

        transport.release()
        assertFalse(fakeAdapter.isRegistered)
    }
}
