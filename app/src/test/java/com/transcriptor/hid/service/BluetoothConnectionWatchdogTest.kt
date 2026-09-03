package com.transcriptor.hid.service

import android.bluetooth.BluetoothDevice
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BluetoothConnectionWatchdogTest {

    @Test
    fun testWatchdog_reconnectsOnConnectionLost() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val connectionStateFlow = MutableStateFlow(HidConnectionState.DISCONNECTED)
        val transport = mockk<BluetoothHidTransport>(relaxed = true) {
            every { connectionState } returns connectionStateFlow
            every { connectHost(any()) } returns true
        }

        val watchdog = BluetoothConnectionWatchdog(transport, testScope)
        val mockDevice = mockk<BluetoothDevice>(relaxed = true)

        watchdog.onConnectionLost(mockDevice)

        // Advancing time past first probe (+300ms)
        testScope.advanceTimeBy(350L)

        verify(atLeast = 1) { transport.connectHost(mockDevice) }
    }

    @Test
    fun testWatchdog_disabledDoesNotReconnect() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val connectionStateFlow = MutableStateFlow(HidConnectionState.DISCONNECTED)
        val transport = mockk<BluetoothHidTransport>(relaxed = true) {
            every { connectionState } returns connectionStateFlow
            every { connectHost(any()) } returns true
        }

        val watchdog = BluetoothConnectionWatchdog(transport, testScope)
        watchdog.isEnabled = false
        val mockDevice = mockk<BluetoothDevice>(relaxed = true)

        watchdog.onConnectionLost(mockDevice)
        testScope.advanceTimeBy(2000L)

        assertThat(watchdog.isReconnecting.value).isFalse()
        verify(exactly = 0) { transport.connectHost(any()) }
    }
}
