package com.transcriptor.hid.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.transcriptor.hid.engine.KeyLayout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PairedHostDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var pairedHostDao: PairedHostDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pairedHostDao = database.pairedHostDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndGetHostByAddress() = runBlocking {
        val host = PairedHostEntity(
            address = "AA:BB:CC:DD:EE:01",
            hostName = "DESKTOP-DEV",
            customAlias = "Work ThinkPad",
            hostOs = HostOsType.WINDOWS,
            preferredLayout = KeyLayout.GERMAN_QWERTZ,
            typingDelayMs = 12L,
            isFavorite = true,
            lastConnectedTimestamp = 1000L,
            autoReconnect = true
        )
        pairedHostDao.insertHost(host)

        val direct = pairedHostDao.getHostByAddressDirect("AA:BB:CC:DD:EE:01")
        assertNotNull(direct)
        assertEquals("DESKTOP-DEV", direct!!.hostName)
        assertEquals("Work ThinkPad", direct.customAlias)
        assertEquals(HostOsType.WINDOWS, direct.hostOs)
        assertEquals(KeyLayout.GERMAN_QWERTZ, direct.preferredLayout)
        assertEquals(12L, direct.typingDelayMs)
        assertTrue(direct.isFavorite)

        val flowHost = pairedHostDao.getHostByAddress("AA:BB:CC:DD:EE:01").first()
        assertEquals(direct, flowHost)
    }

    @Test
    fun testFavoriteHostsFilter() = runBlocking {
        pairedHostDao.insertHost(
            PairedHostEntity(
                address = "AA:01",
                hostName = "Host 1",
                isFavorite = false,
                lastConnectedTimestamp = 100L
            )
        )
        pairedHostDao.insertHost(
            PairedHostEntity(
                address = "AA:02",
                hostName = "Host 2",
                isFavorite = true,
                lastConnectedTimestamp = 200L
            )
        )

        val favorites = pairedHostDao.getFavoriteHosts().first()
        assertEquals(1, favorites.size)
        assertEquals("AA:02", favorites[0].address)

        // Set AA:01 as favorite
        pairedHostDao.setFavorite("AA:01", true)
        val updatedFavorites = pairedHostDao.getFavoriteHosts().first()
        assertEquals(2, updatedFavorites.size)
    }

    @Test
    fun testUpdateHostAttributes() = runBlocking {
        pairedHostDao.insertHost(
            PairedHostEntity(
                address = "AA:03",
                hostName = "Linux Server",
                customAlias = "Old Alias",
                preferredLayout = KeyLayout.US_QWERTY,
                typingDelayMs = 8L
            )
        )

        pairedHostDao.updateCustomAlias("AA:03", "New Proxmox Node")
        pairedHostDao.updatePreferredLayout("AA:03", KeyLayout.GERMAN_QWERTZ)
        pairedHostDao.updateTypingDelay("AA:03", 16L)
        pairedHostDao.updateLastConnected("AA:03", 5000L)

        val updated = pairedHostDao.getHostByAddressDirect("AA:03")!!
        assertEquals("New Proxmox Node", updated.customAlias)
        assertEquals(KeyLayout.GERMAN_QWERTZ, updated.preferredLayout)
        assertEquals(16L, updated.typingDelayMs)
        assertEquals(5000L, updated.lastConnectedTimestamp)
    }

    @Test
    fun testDeleteHostByAddress() = runBlocking {
        pairedHostDao.insertHost(
            PairedHostEntity(address = "AA:04", hostName = "Temp Host")
        )
        assertEquals(1, pairedHostDao.getHostCount())

        val deleted = pairedHostDao.deleteHostByAddress("AA:04")
        assertEquals(1, deleted)
        assertEquals(0, pairedHostDao.getHostCount())
        assertNull(pairedHostDao.getHostByAddressDirect("AA:04"))
    }
}
