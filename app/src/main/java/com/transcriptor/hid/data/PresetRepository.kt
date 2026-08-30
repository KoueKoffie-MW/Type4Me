package com.transcriptor.hid.data

import com.transcriptor.hid.ai.PromptPreset
import com.transcriptor.hid.data.db.AppDatabase
import com.transcriptor.hid.data.db.PresetDao
import com.transcriptor.hid.data.db.PresetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository interface for managing AI prompt presets (both built-in and user-defined).
 */
interface PresetRepository {
    /**
     * Observes all prompt presets.
     */
    fun getAllPresets(): Flow<List<PromptPreset>>

    /**
     * Retrieves a single preset by ID.
     */
    suspend fun getPresetById(id: Long): PromptPreset?

    /**
     * Inserts a new custom preset into persistent storage.
     * @return The newly assigned ID.
     */
    suspend fun insertPreset(preset: PromptPreset): Long

    /**
     * Updates an existing custom preset.
     */
    suspend fun updatePreset(preset: PromptPreset)

    /**
     * Deletes a custom preset. Built-in presets cannot be deleted.
     */
    suspend fun deletePreset(preset: PromptPreset)

    /**
     * Ensures built-in presets are seeded in the database.
     */
    suspend fun ensureBuiltInPresetsSeeded()
}

/**
 * Default implementation of [PresetRepository] backed by Room [PresetDao].
 */
class DefaultPresetRepository(
    private val presetDao: PresetDao
) : PresetRepository {

    override fun getAllPresets(): Flow<List<PromptPreset>> {
        return presetDao.getAllPresets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPresetById(id: Long): PromptPreset? {
        return presetDao.getPresetById(id)?.toDomain()
    }

    override suspend fun insertPreset(preset: PromptPreset): Long {
        val entity = PresetEntity.fromDomain(preset)
        return presetDao.insertPreset(entity)
    }

    override suspend fun updatePreset(preset: PromptPreset) {
        require(!preset.isBuiltIn) { "Built-in preset '${preset.title}' cannot be modified." }
        val entity = PresetEntity.fromDomain(preset)
        presetDao.updatePreset(entity)
    }

    override suspend fun deletePreset(preset: PromptPreset) {
        require(!preset.isBuiltIn) { "Cannot delete built-in preset '${preset.title}'." }
        presetDao.deleteCustomPresetById(preset.id)
    }

    override suspend fun ensureBuiltInPresetsSeeded() {
        for ((index, preset) in PromptPreset.BUILTIN_PRESETS.withIndex()) {
            val entity = PresetEntity.fromDomain(preset, orderIndex = index)
            presetDao.insertPreset(entity)
        }
    }
}
