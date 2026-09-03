package com.transcriptor.hid.stress

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothProfile
import com.transcriptor.hid.data.db.HostOsType
import com.transcriptor.hid.data.db.PairedHostEntity
import com.transcriptor.hid.service.BluetoothHidCallback
import com.transcriptor.hid.service.BluetoothHidDeviceAdapter
import com.transcriptor.hid.service.BluetoothHidTransport
import com.transcriptor.hid.service.HidConnectionState
import com.transcriptor.hid.service.HidQosConfiguration
import com.transcriptor.hid.service.HidSdpConfiguration
import com.transcriptor.hid.service.MultiHostConnectionState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Collections
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

class BluetoothHidTransportStressTest {

    private lateinit var fakeAdapter: StressFakeBluetoothHidDeviceAdapter
    private lateinit var mockBtAdapter: BluetoothAdapter
    private lateinit var transport: BluetoothHidTransport

    private val deviceMap = mutableMapOf<String, BluetoothDevice>()

    private class StressFakeBluetoothHidDeviceAdapter : BluetoothHidDeviceAdapter {
        var callback: BluetoothHidCallback? = null
        var isRegistered = false
        val sentReports = Collections.synchronizedList(mutableListOf<ByteArray>())
        val connectInvocations = AtomicInteger(0)
        val disconnectInvocations = AtomicInteger(0)
        @Volatile var autoAcceptConnection = true

        override fun registerApp(
            sdpConfig: HidSdpConfiguration,
            inQos: HidQosConfiguration?,
            outQos: HidQosConfiguration?,
            executor: Executor,
            callback: BluetoothHidCallback
        ): Boolean {
            this.callback = callback
            isRegistered = true
            return true
        }

        override fun unregisterApp(): Boolean {
            isRegistered = false
            return true
        }

        override fun sendReport(device: BluetoothDevice, id: Int, data: ByteArray): Boolean {
            sentReports.add(data.copyOf())
            return true
        }

        override fun replyReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray): Boolean = true
        override fun reportError(device: BluetoothDevice, error: Byte): Boolean = true

        override fun connect(device: BluetoothDevice): Boolean {
            connectInvocations.incrementAndGet()
            if (autoAcceptConnection) {
                // Simulate asynchronous callback
                Thread {
                    Thread.sleep(10)
                    callback?.onConnectionStateChanged(device, BluetoothProfile.STATE_CONNECTED)
                }.start()
            }
            return true
        }

        override fun disconnect(device: BluetoothDevice): Boolean {
            disconnectInvocations.incrementAndGet()
            Thread {
                Thread.sleep(10)
                callback?.onConnectionStateChanged(device, BluetoothProfile.STATE_DISCONNECTED)
            }.start()
            return true
        }

        override fun getConnectedDevices(): List<BluetoothDevice> = emptyList()
        override fun getConnectionState(device: BluetoothDevice): Int = BluetoothProfile.STATE_DISCONNECTED
    }

    private fun getOrCreateDevice(address: String, name: String): BluetoothDevice {
        return deviceMap.getOrPut(address) {
            val dev = mockk<BluetoothDevice>(relaxed = true)
            every { dev.address } returns address
            every { dev.name } returns name
            dev
        }
    }

    @Before
    fun setUp() {
        fakeAdapter = StressFakeBluetoothHidDeviceAdapter()
        mockBtAdapter = mockk(relaxed = true)
        every { mockBtAdapter.isEnabled } returns true
        every { mockBtAdapter.getRemoteDevice(any<String>()) } answers {
            val addr = firstArg<String>()
            getOrCreateDevice(addr, "Host-$addr")
        }

        transport = BluetoothHidTransport(
            context = null,
            bluetoothAdapter = mockBtAdapter,
            hidAdapterProvider = { listener ->
                val fakeProxy = mockk<BluetoothHidDevice>(relaxed = true)
                listener.onServiceConnected(BluetoothProfile.HID_DEVICE, fakeProxy)
                true
            },
            hidAdapterFactory = { fakeAdapter }
        )
    }

    @Test
    fun testRapidSequentialHostSwitching() = runBlocking {
        assertTrue(transport.initialize())

        val hosts = listOf(
            PairedHostEntity(address = "AA:BB:CC:DD:EE:01", hostName = "Workstation", customAlias = "Work", hostOs = HostOsType.WINDOWS),
            PairedHostEntity(address = "AA:BB:CC:DD:EE:02", hostName = "MacBook", customAlias = "Mac", hostOs = HostOsType.MACOS),
            PairedHostEntity(address = "AA:BB:CC:DD:EE:03", hostName = "LinuxBox", customAlias = "Linux", hostOs = HostOsType.LINUX)
        )

        withTimeout(30000L) {
            // Rapidly cycle between hosts 6 times
            for (i in 0 until 6) {
                val targetHost = hosts[i % hosts.size]
                val success = transport.switchHost(targetHost)
                assertTrue("Switch to ${targetHost.hostName} must succeed", success)
                assertEquals(HidConnectionState.CONNECTED, transport.connectionState.value)
                assertEquals(targetHost.address, transport.currentPairedHost?.address)
                assertTrue(transport.multiHostState.value is MultiHostConnectionState.Connected)
            }
        }
    }

    @Test
    fun testConcurrentHostSwitchContentionSerializedByMutex() = runBlocking {
        assertTrue(transport.initialize())

        val host1 = PairedHostEntity(address = "11:22:33:44:55:01", hostName = "Host1", hostOs = HostOsType.WINDOWS)
        val host2 = PairedHostEntity(address = "11:22:33:44:55:02", hostName = "Host2", hostOs = HostOsType.MACOS)
        val host3 = PairedHostEntity(address = "11:22:33:44:55:03", hostName = "Host3", hostOs = HostOsType.LINUX)

        // Launch 10 concurrent switchHost calls across 3 targets
        val targets = listOf(host1, host2, host3)
        val jobs = (0 until 9).map { idx ->
            async(Dispatchers.Default) {
                transport.switchHost(targets[idx % 3])
            }
        }

        val results = withTimeout(40000L) {
            jobs.awaitAll()
        }

        // All calls should have safely completed without throwing or corrupting transport state
        assertTrue(results.isNotEmpty())
        assertEquals(HidConnectionState.CONNECTED, transport.connectionState.value)
        assertNotNull(transport.currentPairedHost)
    }

    @Test
    fun testSwitchHostTimeoutRecoveryWhenTargetUnreachable() = runBlocking {
        assertTrue(transport.initialize())

        // Set adapter to NOT auto-accept connections (simulating host out of range / sleeping)
        fakeAdapter.autoAcceptConnection = false

        val unreachableHost = PairedHostEntity(
            address = "FF:FF:FF:FF:FF:99",
            hostName = "OfflineWorkstation",
            hostOs = HostOsType.WINDOWS
        )

        // Phase 5 timeout is 5000ms. It must time out, abort, and return false cleanly
        val startTime = System.currentTimeMillis()
        val success = transport.switchHost(unreachableHost)
        val elapsed = System.currentTimeMillis() - startTime

        assertFalse("Unreachable host switch must fail", success)
        assertTrue("Must take at least 4500ms due to timeout guard", elapsed >= 4500L)
        assertEquals(HidConnectionState.DISCONNECTED, transport.connectionState.value)
        assertTrue(transport.multiHostState.value is MultiHostConnectionState.Error)
        assertNull(transport.currentPairedHost)

        // Now verify transport recovers immediately and can connect to an active host
        fakeAdapter.autoAcceptConnection = true
        val reachableHost = PairedHostEntity(
            address = "11:22:33:44:55:01",
            hostName = "ActiveWorkstation",
            hostOs = HostOsType.WINDOWS
        )
        val recoverySuccess = transport.switchHost(reachableHost)
        assertTrue("Switch must succeed after timeout recovery", recoverySuccess)
        assertEquals(HidConnectionState.CONNECTED, transport.connectionState.value)
        assertEquals(reachableHost.address, transport.currentPairedHost?.address)
    }

    @Test
    fun testConcurrentSwitchHostAndDisconnectOrRelease() = runBlocking {
        assertTrue(transport.initialize())

        val target = PairedHostEntity(
            address = "11:22:33:44:55:01",
            hostName = "Workstation",
            hostOs = HostOsType.WINDOWS
        )

        // Launch switchHost and concurrent disconnect
        val switchJob = async(Dispatchers.Default) {
            transport.switchHost(target)
        }

        val disconnectJob = launch(Dispatchers.Default) {
            delay(50L)
            transport.disconnect()
        }

        disconnectJob.join()
        switchJob.await()

        // Transport should finish in a consistent state without native binder crash
        assertTrue(
            transport.connectionState.value == HidConnectionState.CONNECTED ||
            transport.connectionState.value == HidConnectionState.DISCONNECTED
        )

        // Verify release can be invoked safely afterwards
        transport.release()
        assertFalse(fakeAdapter.isRegistered)
        assertEquals(HidConnectionState.DISCONNECTED, transport.connectionState.value)
    }
}
