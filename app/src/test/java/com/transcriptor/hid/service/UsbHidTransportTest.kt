package com.transcriptor.hid.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [UsbHidTransport], fallback modes, byte stream channel writing,
 * and state transitions.
 */
class UsbHidTransportTest {

    private lateinit var inMemoryChannel: InMemoryUsbChannel
    private lateinit var transport: UsbHidTransport

    @Before
    fun setUp() {
        inMemoryChannel = InMemoryUsbChannel(channelName = "Virtual ADB Bridge")
        transport = UsbHidTransport(
            mode = UsbTransportMode.ADB_SOCKET_BRIDGE,
            channelProvider = { inMemoryChannel }
        )
    }

    @Test
    fun testInitialState() {
        assertEquals(HidConnectionState.DISCONNECTED, transport.connectionState.value)
        assertNull(transport.connectedDeviceName.value)
        assertEquals(UsbTransportMode.ADB_SOCKET_BRIDGE, transport.mode)
    }

    @Test
    fun testInitializationSuccess() = runBlocking {
        val success = transport.initialize()
        assertTrue("Transport initialization should succeed", success)
        assertEquals(HidConnectionState.CONNECTED, transport.connectionState.value)
        assertEquals("Virtual ADB Bridge", transport.connectedDeviceName.value)
        assertTrue(inMemoryChannel.isConnected)
    }

    @Test
    fun testInitializationFailure() = runBlocking {
        inMemoryChannel.shouldSucceedOpen = false
        val success = transport.initialize()
        assertFalse("Transport initialization should fail if channel open fails", success)
        assertEquals(HidConnectionState.ERROR, transport.connectionState.value)
        assertNull(transport.connectedDeviceName.value)
    }

    @Test
    fun testSendKeyboardReportWhenConnected() = runBlocking {
        transport.initialize()

        // 8-byte report: Mod Shift (0x02), Key 'Z' (0x1D in US / 0x1C in DE)
        val report = byteArrayOf(0x02, 0x00, 0x1D, 0x00, 0x00, 0x00, 0x00, 0x00)
        val sent = transport.sendKeyboardReport(report)
        assertTrue("Report transmission should succeed", sent)
        assertEquals(1, inMemoryChannel.writtenReports.size)
        assertArrayEquals(report, inMemoryChannel.writtenReports[0])
    }

    @Test
    fun testSendKeyboardReportRejectsInvalidSize() = runBlocking {
        transport.initialize()

        val invalidReport = byteArrayOf(0x02, 0x00, 0x1D)
        val sent = transport.sendKeyboardReport(invalidReport)
        assertFalse("Report with invalid size must be rejected", sent)
        assertEquals(0, inMemoryChannel.writtenReports.size)
    }

    @Test
    fun testSendKeyboardReportFailsWhenDisconnected() = runBlocking {
        val report = byteArrayOf(0x02, 0x00, 0x1D, 0x00, 0x00, 0x00, 0x00, 0x00)
        val sent = transport.sendKeyboardReport(report)
        assertFalse("Report transmission must fail when disconnected", sent)
        assertEquals(0, inMemoryChannel.writtenReports.size)
    }

    @Test
    fun testHostLedStateUpdate() {
        assertEquals(HostLedState.ALL_OFF, transport.hostLedState.value)

        transport.updateHostLedState(0x02) // CapsLock
        assertTrue(transport.hostLedState.value.capsLock)
        assertFalse(transport.hostLedState.value.numLock)

        transport.updateHostLedState(0x01) // NumLock
        assertTrue(transport.hostLedState.value.numLock)
        assertFalse(transport.hostLedState.value.capsLock)
    }

    @Test
    fun testDisconnectSendsReleaseReportAndCloses() = runBlocking {
        transport.initialize()
        assertEquals(HidConnectionState.CONNECTED, transport.connectionState.value)

        transport.disconnect()
        assertEquals(HidConnectionState.DISCONNECTED, transport.connectionState.value)
        assertNull(transport.connectedDeviceName.value)
        assertFalse(inMemoryChannel.isConnected)

        // Should have written release report (all 0s) before closing
        assertEquals(1, inMemoryChannel.writtenReports.size)
        assertArrayEquals(ByteArray(8), inMemoryChannel.writtenReports[0])
    }

    @Test
    fun testReleaseClosesChannel() = runBlocking {
        transport.initialize()
        transport.release()
        assertEquals(HidConnectionState.DISCONNECTED, transport.connectionState.value)
        assertFalse(inMemoryChannel.isConnected)
    }

    @Test
    fun testUsbTransportModes() {
        val aoaTransport = UsbHidTransport(mode = UsbTransportMode.AOA_2_0)
        assertEquals(UsbTransportMode.AOA_2_0, aoaTransport.mode)

        val gadgetTransport = UsbHidTransport(mode = UsbTransportMode.USB_GADGET)
        assertEquals(UsbTransportMode.USB_GADGET, gadgetTransport.mode)
    }
}
