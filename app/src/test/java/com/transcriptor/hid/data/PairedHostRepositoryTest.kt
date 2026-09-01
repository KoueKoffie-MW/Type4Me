package com.transcriptor.hid.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.transcriptor.hid.data.db.AppDatabase
import com.transcriptor.hid.data.db.HostOsType
import com.transcriptor.hid.data.db.PairedHostEntity
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
class PairedHostRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: PairedHostRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DefaultPairedHostRepository(database.pairedHostDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testPairedHostCrudAndQueries() = runBlocking {
        assertEquals(0, repository.getHostCount())

        val host = PairedHostEntity(
            address = "11:22:33:44:55:66",
            hostName = "MacBook Pro",
            customAlias = "Work Mac",
            hostOs = HostOsType.MACOS,
            preferredLayout = KeyLayout.US_QWERTY,
            typingDelayMs = 6L,
            isFavorite = true,
            lastConnectedTimestamp = 12345L,
            autoReconnect = true
        )
        repository.saveHost(host)

        assertEquals(1, repository.getHostCount())

        val retrieved = repository.getHostByAddressDirect("11:22:33:44:55:66")
        assertNotNull(retrieved)
        assertEquals("MacBook Pro", retrieved!!.hostName)
        assertEquals("Work Mac", retrieved.customAlias)
        assertEquals(HostOsType.MACOS, retrieved.hostOs)

        // Update attributes
        repository.updateCustomAlias("11:22:33:44:55:66", "Personal Mac M3")
        repository.updatePreferredLayout("11:22:33:44:55:66", KeyLayout.GERMAN_QWERTZ)
        repository.updateTypingDelay("11:22:33:44:55:66", 10L)
        repository.updateLastConnected("11:22:33:44:55:66", 67890L)

        val updated = repository.getHostByAddressDirect("11:22:33:44:55:66")!!
        assertEquals("Personal Mac M3", updated.customAlias)
        assertEquals(KeyLayout.GERMAN_QWERTZ, updated.preferredLayout)
        assertEquals(10L, updated.typingDelayMs)
        assertEquals(67890L, updated.lastConnectedTimestamp)

        // Flow query
        val allHosts = repository.getAllPairedHosts().first()
        assertEquals(1, allHosts.size)

        // Delete
        val deleted = repository.deleteHostByAddress("11:22:33:44:55:66")
        assertEquals(1, deleted)
        assertEquals(0, repository.getHostCount())
        assertNull(repository.getHostByAddressDirect("11:22:33:44:55:66"))
    }
}
