package com.transcriptor.hid.data

import com.transcriptor.hid.data.db.PairedHostDao
import com.transcriptor.hid.data.db.PairedHostEntity
import com.transcriptor.hid.engine.KeyLayout
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing bonded host workstations in the multi-host pairing registry.
 */
interface PairedHostRepository {
    fun getAllPairedHosts(): Flow<List<PairedHostEntity>>
    fun getFavoriteHosts(): Flow<List<PairedHostEntity>>
    fun getHostByAddress(address: String): Flow<PairedHostEntity?>
    suspend fun getHostByAddressDirect(address: String): PairedHostEntity?
    suspend fun saveHost(host: PairedHostEntity)
    suspend fun insertHosts(hosts: List<PairedHostEntity>)
    suspend fun updateHost(host: PairedHostEntity)
    suspend fun deleteHost(host: PairedHostEntity)
    suspend fun deleteHostByAddress(address: String): Int
    suspend fun updateLastConnected(address: String, timestamp: Long = System.currentTimeMillis())
    suspend fun setFavorite(address: String, isFavorite: Boolean)
    suspend fun updateCustomAlias(address: String, alias: String)
    suspend fun updatePreferredLayout(address: String, layout: KeyLayout)
    suspend fun updateTypingDelay(address: String, delayMs: Long)
    suspend fun getHostCount(): Int
}

/**
 * Default implementation of [PairedHostRepository] backed by Room [PairedHostDao].
 */
class DefaultPairedHostRepository(
    private val pairedHostDao: PairedHostDao
) : PairedHostRepository {

    override fun getAllPairedHosts(): Flow<List<PairedHostEntity>> =
        pairedHostDao.getAllPairedHosts()

    override fun getFavoriteHosts(): Flow<List<PairedHostEntity>> =
        pairedHostDao.getFavoriteHosts()

    override fun getHostByAddress(address: String): Flow<PairedHostEntity?> =
        pairedHostDao.getHostByAddress(address)

    override suspend fun getHostByAddressDirect(address: String): PairedHostEntity? =
        pairedHostDao.getHostByAddressDirect(address)

    override suspend fun saveHost(host: PairedHostEntity) =
        pairedHostDao.insertHost(host)

    override suspend fun insertHosts(hosts: List<PairedHostEntity>) =
        pairedHostDao.insertAll(hosts)

    override suspend fun updateHost(host: PairedHostEntity) =
        pairedHostDao.updateHost(host)

    override suspend fun deleteHost(host: PairedHostEntity) =
        pairedHostDao.deleteHost(host)

    override suspend fun deleteHostByAddress(address: String): Int =
        pairedHostDao.deleteHostByAddress(address)

    override suspend fun updateLastConnected(address: String, timestamp: Long) =
        pairedHostDao.updateLastConnected(address, timestamp)

    override suspend fun setFavorite(address: String, isFavorite: Boolean) =
        pairedHostDao.setFavorite(address, isFavorite)

    override suspend fun updateCustomAlias(address: String, alias: String) =
        pairedHostDao.updateCustomAlias(address, alias)

    override suspend fun updatePreferredLayout(address: String, layout: KeyLayout) =
        pairedHostDao.updatePreferredLayout(address, layout)

    override suspend fun updateTypingDelay(address: String, delayMs: Long) =
        pairedHostDao.updateTypingDelay(address, delayMs)

    override suspend fun getHostCount(): Int =
        pairedHostDao.getHostCount()
}
