package com.transcriptor.hid.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.transcriptor.hid.engine.KeyLayout

/**
 * Room entity representing a bonded host workstation in the multi-host pairing registry.
 */
@Entity(
    tableName = "paired_hosts",
    indices = [
        Index(value = ["isFavorite"], name = "index_paired_hosts_is_favorite")
    ]
)
data class PairedHostEntity(
    @PrimaryKey
    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "hostName")
    val hostName: String,

    @ColumnInfo(name = "customAlias")
    val customAlias: String = "",

    @ColumnInfo(name = "hostOs")
    val hostOs: HostOsType = HostOsType.WINDOWS,

    @ColumnInfo(name = "preferredLayout")
    val preferredLayout: KeyLayout = KeyLayout.US_QWERTY,

    @ColumnInfo(name = "typingDelayMs")
    val typingDelayMs: Long = 8L,

    @ColumnInfo(name = "isFavorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "lastConnectedTimestamp")
    val lastConnectedTimestamp: Long = 0L,

    @ColumnInfo(name = "autoReconnect")
    val autoReconnect: Boolean = true
)
