package com.transcriptor.hid.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for interacting with persisted AI prompt presets.
 */
@Dao
interface PresetDao {
    /**
     * Observes all prompt presets ordered by built-in status and custom ordering.
     */
    @Query("SELECT * FROM presets ORDER BY is_built_in DESC, order_index ASC, id ASC")
    fun getAllPresets(): Flow<List<PresetEntity>>

    /**
     * Retrieves a single preset by its unique ID.
     */
    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getPresetById(id: Long): PresetEntity?

    /**
     * Inserts or replaces a preset in the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity): Long

    /**
     * Inserts multiple presets, ignoring duplicates (useful for initial seeding).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(presets: List<PresetEntity>): List<Long>

    /**
     * Updates an existing preset.
     */
    @Update
    suspend fun updatePreset(preset: PresetEntity)

    /**
     * Deletes a preset entity from the database.
     */
    @Delete
    suspend fun deletePreset(preset: PresetEntity)

    /**
     * Deletes a custom preset by ID, protecting built-in presets from deletion.
     * @return The number of rows deleted.
     */
    @Query("DELETE FROM presets WHERE id = :id AND is_built_in = 0")
    suspend fun deleteCustomPresetById(id: Long): Int

    /**
     * Returns the total count of presets currently stored.
     */
    @Query("SELECT COUNT(*) FROM presets")
    suspend fun getPresetCount(): Int

    /**
     * Deletes all presets in the table.
     */
    @Query("DELETE FROM presets")
    suspend fun deleteAll()
}
