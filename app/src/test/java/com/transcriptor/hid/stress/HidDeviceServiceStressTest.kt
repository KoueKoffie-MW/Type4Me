package com.transcriptor.hid.stress

import com.transcriptor.hid.service.HidConnectionState
import com.transcriptor.hid.service.HidDeviceController
import com.transcriptor.hid.service.HidTransport
import com.transcriptor.hid.service.HostLedState
import com.transcriptor.hid.service.WakeLockController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class HidDeviceServiceStressTest {

    private class StressWakeLockController : WakeLockController {
        private val _held = AtomicBoolean(false)
        val acquireCount = AtomicInteger(0)
        val releaseCount = AtomicInteger(0)

        override fun acquire(timeoutMs: Long) {
            _held.set(true)
            acquireCount.incrementAndGet()
        }

        override fun release() {
            _held.set(false)
            releaseCount.incrementAndGet()
        }

        override val isHeld: Boolean
            get() = _held.get()
    }

    private class StressMockTransport : HidTransport {
        private val _connectionState = MutableStateFlow(HidConnectionState.CONNECTED)
        override val connectionState: StateFlow<HidConnectionState> = _connectionState.asStateFlow()

        private val _connectedDeviceName = MutableStateFlow<String?>("Stress PC")
        override val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

        private val _hostLedState = MutableStateFlow(HostLedState.ALL_OFF)
        override val hostLedState: StateFlow<HostLedState> = _hostLedState.asStateFlow()

        val sentReports = Collections.synchronizedList(mutableListOf<ByteArray>())
        @Volatile var isReleased = false
        @Volatile var simulateDelayMs = 0L
        @Volatile var throwOnReport = false

        override suspend fun initialize(): Boolean = true

        override suspend fun sendKeyboardReport(report: ByteArray): Boolean {
            if (simulateDelayMs > 0) delay(simulateDelayMs)
            if (throwOnReport) throw IllegalStateException("Simulated hardware bus fault")
            sentReports.add(report.copyOf())
            return true
        }

        override suspend fun sendMouseReport(buttons: Int, dx: Int, dy: Int, wheel: Int): Boolean = true
        override suspend fun disconnect() {
            _connectionState.value = HidConnectionState.DISCONNECTED
        }

        override fun release() {
            isReleased = true
            _connectionState.value = HidConnectionState.DISCONNECTED
        }
    }

    @Test
    fun testRapidServiceStartAndDestroyCycles() {
        repeat(100) { cycle ->
            val wakeLock = StressWakeLockController()
            val transport = StressMockTransport()
            var lastNotification = ""

            val controller = HidDeviceController(
                transport = transport,
                wakeLockController = wakeLock,
                notificationUpdater = { lastNotification = it }
            )

            controller.startService(autoInit = false)
            controller.acquireWakeLock()
            controller.observeTransportState()

            assertTrue("WakeLock must be held after acquire", wakeLock.isHeld)

            // Destroy immediately
            controller.destroy()

            assertFalse("WakeLock must be released in cycle $cycle", wakeLock.isHeld)
            assertTrue("Transport must be released in cycle $cycle", transport.isReleased)

            // Verify emergency release report was captured synchronously
            assertTrue(
                "Emergency release report must be sent in cycle $cycle",
                transport.sentReports.any { it.size == 8 && it.all { b -> b == 0.toByte() } }
            )
        }
    }

    @Test
    fun testOnTaskRemovedDispatchesEmergencyReleaseReport() {
        val wakeLock = StressWakeLockController()
        val transport = StressMockTransport()
        val controller = HidDeviceController(
            transport = transport,
            wakeLockController = wakeLock,
            notificationUpdater = {}
        )

        controller.onTaskRemoved()

        assertTrue(
            "onTaskRemoved must dispatch emergency release report",
            transport.sentReports.any { it.size == 8 && it.all { b -> b == 0.toByte() } }
        )
    }

    @Test
    fun testDestroyResilientToTransportFaultsAndExceptions() {
        val wakeLock = StressWakeLockController()
        val transport = StressMockTransport().apply {
            throwOnReport = true // Simulate exception in sendKeyboardReport
        }

        val controller = HidDeviceController(
            transport = transport,
            wakeLockController = wakeLock,
            notificationUpdater = {}
        )

        controller.acquireWakeLock()
        assertTrue(wakeLock.isHeld)

        // Must not throw or crash when hardware throws
        controller.destroy()

        assertFalse("WakeLock must be released even when transport throws", wakeLock.isHeld)
        assertTrue("Transport release must be invoked even when report throws", transport.isReleased)
    }

    @Test
    fun testDestroyWithNullTransport() {
        val wakeLock = StressWakeLockController()
        val controller = HidDeviceController(
            transport = null,
            wakeLockController = wakeLock,
            notificationUpdater = {}
        )

        controller.acquireWakeLock()
        assertTrue(wakeLock.isHeld)

        // Must not throw NullPointerException
        controller.destroy()

        assertFalse(wakeLock.isHeld)
    }
}
