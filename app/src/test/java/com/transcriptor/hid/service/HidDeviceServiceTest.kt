package com.transcriptor.hid.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [HidDeviceService] constants, [HidDeviceController] lifecycle,
 * wake lock controls, notification formatting, and emergency key release reports.
 */
class HidDeviceServiceTest {

    private lateinit var controller: HidDeviceController
    private lateinit var mockTransport: MockHidTransport
    private lateinit var fakeWakeLock: FakeWakeLockController
    private var lastNotificationText: String? = null

    private class FakeWakeLockController : WakeLockController {
        private var _held = false
        var acquireCount = 0
        var releaseCount = 0

        override fun acquire(timeoutMs: Long) {
            _held = true
            acquireCount++
        }

        override fun release() {
            _held = false
            releaseCount++
        }

        override val isHeld: Boolean
            get() = _held
    }

    private class MockHidTransport : HidTransport {
        private val _connectionState = MutableStateFlow(HidConnectionState.DISCONNECTED)
        override val connectionState: StateFlow<HidConnectionState> = _connectionState.asStateFlow()

        private val _connectedDeviceName = MutableStateFlow<String?>("Test Host PC")
        override val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

        private val _hostLedState = MutableStateFlow(HostLedState.ALL_OFF)
        override val hostLedState: StateFlow<HostLedState> = _hostLedState.asStateFlow()

        val sentReports = mutableListOf<ByteArray>()
        var isReleased = false

        fun setConnectionState(state: HidConnectionState) {
            _connectionState.value = state
        }

        fun setDeviceName(name: String?) {
            _connectedDeviceName.value = name
        }

        override suspend fun initialize(): Boolean {
            _connectionState.value = HidConnectionState.CONNECTED
            return true
        }

        override suspend fun sendKeyboardReport(report: ByteArray): Boolean {
            sentReports.add(report.copyOf())
            return true
        }

        override suspend fun disconnect() {
            _connectionState.value = HidConnectionState.DISCONNECTED
        }

        override fun release() {
            isReleased = true
            _connectionState.value = HidConnectionState.DISCONNECTED
        }
    }

    @Before
    fun setUp() {
        mockTransport = MockHidTransport()
        fakeWakeLock = FakeWakeLockController()
        lastNotificationText = null
        controller = HidDeviceController(
            transport = mockTransport,
            wakeLockController = fakeWakeLock,
            notificationUpdater = { text -> lastNotificationText = text }
        )
    }

    @Test
    fun testServiceConstants() {
        assertEquals("transcriptor_hid_service_channel", HidDeviceService.CHANNEL_ID)
        assertEquals("Transcriptor HID Keyboard", HidDeviceService.CHANNEL_NAME)
        assertEquals(1001, HidDeviceService.NOTIFICATION_ID)
        assertEquals("com.transcriptor.hid.action.START_SERVICE", HidDeviceService.ACTION_START)
        assertEquals("com.transcriptor.hid.action.STOP_SERVICE", HidDeviceService.ACTION_STOP)
        assertEquals("com.transcriptor.hid.action.CONNECT", HidDeviceService.ACTION_CONNECT)
        assertEquals("com.transcriptor.hid.action.DISCONNECT", HidDeviceService.ACTION_DISCONNECT)
        assertEquals("com.transcriptor.hid.extra.DEVICE_ADDRESS", HidDeviceService.EXTRA_DEVICE_ADDRESS)
        assertEquals("transcriptor:hid_wake_lock", HidDeviceService.WAKE_LOCK_TAG)
    }

    @Test
    fun testStartAndStopService() {
        assertFalse(controller.isForegroundActive)
        controller.startService(autoInit = false)
        assertTrue(controller.isForegroundActive)

        controller.stopService()
        assertFalse(controller.isForegroundActive)
    }

    @Test
    fun testWakeLockAcquisitionAndRelease() {
        assertFalse(fakeWakeLock.isHeld)
        controller.acquireWakeLock(5000L)
        assertTrue(fakeWakeLock.isHeld)
        assertEquals(1, fakeWakeLock.acquireCount)

        controller.releaseWakeLock()
        assertFalse(fakeWakeLock.isHeld)
        assertEquals(1, fakeWakeLock.releaseCount)
    }

    @Test
    fun testNotificationContentUpdatesOnStateChange() = runBlocking {
        controller.startService(autoInit = false)

        mockTransport.setDeviceName("Workstation PC")
        mockTransport.setConnectionState(HidConnectionState.CONNECTED)
        Thread.sleep(50)
        assertEquals("Connected to Workstation PC", lastNotificationText)

        mockTransport.setConnectionState(HidConnectionState.CONNECTING)
        Thread.sleep(50)
        assertEquals("Connecting to Workstation PC...", lastNotificationText)

        mockTransport.setConnectionState(HidConnectionState.DISCONNECTED)
        Thread.sleep(50)
        assertEquals("Bluetooth HID Keyboard idle", lastNotificationText)

        mockTransport.setConnectionState(HidConnectionState.ERROR)
        Thread.sleep(50)
        assertEquals("Bluetooth HID Error — check settings", lastNotificationText)
    }

    @Test
    fun testEmergencyReleaseReportSentOnDestroy() {
        controller.startService(autoInit = false)
        fakeWakeLock.acquire(1000L)
        assertTrue(fakeWakeLock.isHeld)

        controller.destroy()
        Thread.sleep(50)

        // Verifies wake lock was released
        assertFalse(fakeWakeLock.isHeld)
        // Verifies transport release was invoked
        assertTrue(mockTransport.isReleased)
        // Verifies emergency 8-byte all-zero release report was transmitted (mitigating Edge Case E4)
        assertTrue("Emergency release report (8 zero bytes) must be sent on destroy to prevent stuck modifier keys",
            mockTransport.sentReports.any { it.size == 8 && it.all { b -> b == 0.toByte() } }
        )
    }

    @Test
    fun testEmergencyReleaseReportSentOnTaskRemoved() {
        controller.onTaskRemoved()
        Thread.sleep(50)
        assertTrue("Emergency release report must be sent when task is removed",
            mockTransport.sentReports.any { it.size == 8 && it.all { b -> b == 0.toByte() } }
        )
    }

    @Test
    fun testActionDisconnectCommand() {
        mockTransport.setConnectionState(HidConnectionState.CONNECTED)
        controller.handleIntent(HidDeviceService.ACTION_DISCONNECT)
        Thread.sleep(50)
        assertEquals(HidConnectionState.DISCONNECTED, mockTransport.connectionState.value)
    }
}
