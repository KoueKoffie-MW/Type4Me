package com.transcriptor.hid.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.transcriptor.hid.engine.KeyLayout
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [PairedHostEntity] operations in the multi-host pairing registry.
 */
@Dao
interface PairedHostDao {

    @Query("SELECT * FROM paired_hosts ORDER BY isFavorite DESC, lastConnectedTimestamp DESC")
    fun getAllPairedHosts(): Flow<List<PairedHostEntity>>

    @Query("SELECT * FROM paired_hosts WHERE isFavorite = 1 ORDER BY lastConnectedTimestamp DESC")
    fun getFavoriteHosts(): Flow<List<PairedHostEntity>>

    @Query("SELECT * FROM paired_hosts WHERE address = :address")
    fun getHostByAddress(address: String): Flow<PairedHostEntity?>

    @Query("SELECT * FROM paired_hosts WHERE address = :address")
    suspend fun getHostByAddressDirect(address: String): PairedHostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHost(host: PairedHostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hosts: List<PairedHostEntity>)

    @Update
    suspend fun updateHost(host: PairedHostEntity)

    @Delete
    suspend fun deleteHost(host: PairedHostEntity)

    @Query("DELETE FROM paired_hosts WHERE address = :address")
    suspend fun deleteHostByAddress(address: String): Int

    @Query("UPDATE paired_hosts SET lastConnectedTimestamp = :timestamp WHERE address = :address")
    suspend fun updateLastConnected(address: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE paired_hosts SET isFavorite = :isFavorite WHERE address = :address")
    suspend fun setFavorite(address: String, isFavorite: Boolean)

    @Query("UPDATE paired_hosts SET customAlias = :alias WHERE address = :address")
    suspend fun updateCustomAlias(address: String, alias: String)

    @Query("UPDATE paired_hosts SET preferredLayout = :layout WHERE address = :address")
    suspend fun updatePreferredLayout(address: String, layout: KeyLayout)

    @Query("UPDATE paired_hosts SET typingDelayMs = :delayMs WHERE address = :address")
    suspend fun updateTypingDelay(address: String, delayMs: Long)

    @Query("SELECT COUNT(*) FROM paired_hosts")
    suspend fun getHostCount(): Int
}
