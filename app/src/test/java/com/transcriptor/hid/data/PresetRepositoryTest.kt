package com.transcriptor.hid.data

import com.transcriptor.hid.ai.PromptPreset
import com.transcriptor.hid.data.db.PresetDao
import com.transcriptor.hid.data.db.PresetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * In-memory fake DAO for testing PresetRepository without Android Room SQLite dependencies.
 */
class FakePresetDao : PresetDao {
    private val entities = mutableMapOf<Long, PresetEntity>()
    private val flow = MutableStateFlow<List<PresetEntity>>(emptyList())
    private var nextId = 100L

    private fun updateFlow() {
        val sorted = entities.values.sortedWith(
            compareByDescending<PresetEntity> { it.isBuiltIn }
                .thenBy { it.orderIndex }
                .thenBy { it.id }
        )
        flow.value = sorted
    }

    override fun getAllPresets(): Flow<List<PresetEntity>> = flow

    override suspend fun getPresetById(id: Long): PresetEntity? = entities[id]

    override suspend fun insertPreset(preset: PresetEntity): Long {
        val assignedId = if (preset.id <= 0) nextId++ else preset.id
        val stored = preset.copy(id = assignedId)
        entities[assignedId] = stored
        updateFlow()
        return assignedId
    }

    override suspend fun insertAll(presets: List<PresetEntity>): List<Long> {
        val ids = presets.map { preset ->
            val assignedId = if (preset.id <= 0) nextId++ else preset.id
            entities[assignedId] = preset.copy(id = assignedId)
            assignedId
        }
        updateFlow()
        return ids
    }

    override suspend fun updatePreset(preset: PresetEntity) {
        if (entities.containsKey(preset.id)) {
            entities[preset.id] = preset
            updateFlow()
        }
    }

    override suspend fun deletePreset(preset: PresetEntity) {
        entities.remove(preset.id)
        updateFlow()
    }

    override suspend fun deleteCustomPresetById(id: Long): Int {
        val existing = entities[id]
        return if (existing != null && !existing.isBuiltIn) {
            entities.remove(id)
            updateFlow()
            1
        } else {
            0
        }
    }

    override suspend fun getPresetCount(): Int = entities.size

    override suspend fun deleteAll() {
        entities.clear()
        updateFlow()
    }
}

class PresetRepositoryTest {

    private lateinit var fakeDao: FakePresetDao
    private lateinit var repository: PresetRepository

    @Before
    fun setUp() {
        fakeDao = FakePresetDao()
        repository = DefaultPresetRepository(fakeDao)
    }

    @Test
    fun testInitialSeedingOfBuiltInPresets() = runBlocking {
        assertEquals(0, fakeDao.getPresetCount())

        repository.ensureBuiltInPresetsSeeded()

        val presets = repository.getAllPresets().first()
        assertEquals(4, presets.size)

        assertTrue(presets.any { it.title == "Clean & Polish" && it.isBuiltIn })
        assertTrue(presets.any { it.title == "Agentic Prompt" && it.isBuiltIn })
        assertTrue(presets.any { it.title == "Business German" && it.isBuiltIn })
        assertTrue(presets.any { it.title == "Technical Code Comments" && it.isBuiltIn })
    }

    @Test
    fun testInsertAndGetCustomPreset() = runBlocking {
        repository.ensureBuiltInPresetsSeeded()

        val custom = PromptPreset(
            id = 0L,
            title = "Doctor Note",
            description = "Medical transcription summary",
            systemPrompt = "You are a medical scribe.",
            isBuiltIn = false,
            temperature = 0.1f
        )

        val newId = repository.insertPreset(custom)
        assertTrue(newId > 0)

        val retrieved = repository.getPresetById(newId)
        assertNotNull(retrieved)
        assertEquals("Doctor Note", retrieved!!.title)
        assertEquals("You are a medical scribe.", retrieved.systemPrompt)
        assertEquals(0.1f, retrieved.temperature, 0.001f)

        val allPresets = repository.getAllPresets().first()
        assertEquals(5, allPresets.size)
    }

    @Test
    fun testUpdateCustomPreset() = runBlocking {
        val custom = PromptPreset(
            id = 0L,
            title = "Original Title",
            description = "Original Desc",
            systemPrompt = "Original Prompt",
            isBuiltIn = false
        )
        val id = repository.insertPreset(custom)

        val updated = custom.copy(id = id, title = "Updated Title", description = "Updated Desc")
        repository.updatePreset(updated)

        val retrieved = repository.getPresetById(id)
        assertNotNull(retrieved)
        assertEquals("Updated Title", retrieved!!.title)
        assertEquals("Updated Desc", retrieved.description)
    }

    @Test
    fun testUpdateBuiltInPresetThrowsException() = runBlocking {
        repository.ensureBuiltInPresetsSeeded()

        val builtIn = PromptPreset.CLEAN_AND_POLISH
        try {
            repository.updatePreset(builtIn.copy(title = "Hacked Title"))
            fail("Expected IllegalArgumentException when modifying built-in preset")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("cannot be modified"))
        }
    }

    @Test
    fun testDeleteCustomPreset() = runBlocking {
        val custom = PromptPreset(
            id = 0L,
            title = "Temporary Preset",
            description = "To be deleted",
            systemPrompt = "Temp prompt",
            isBuiltIn = false
        )
        val id = repository.insertPreset(custom)
        val saved = repository.getPresetById(id)!!

        repository.deletePreset(saved)
        assertNull(repository.getPresetById(id))
    }

    @Test
    fun testDeleteBuiltInPresetThrowsExceptionAndPreservesPreset() = runBlocking {
        repository.ensureBuiltInPresetsSeeded()

        val cleanPolish = repository.getPresetById(1L)
        assertNotNull(cleanPolish)

        try {
            repository.deletePreset(cleanPolish!!)
            fail("Expected IllegalArgumentException when deleting built-in preset")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Cannot delete built-in preset"))
        }

        // Verify still in database
        val afterAttempt = repository.getPresetById(1L)
        assertNotNull(afterAttempt)
    }
}
