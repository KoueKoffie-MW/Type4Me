package com.transcriptor.hid.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.transcriptor.hid.data.db.AppDatabase
import com.transcriptor.hid.data.db.CategoryEntity
import com.transcriptor.hid.data.db.MacroEntity
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
class MacroRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: MacroRepository
    private var testCatId: Long = 0L

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DefaultMacroRepository(
            macroDao = database.macroDao(),
            categoryDao = database.categoryDao()
        )
        testCatId = database.categoryDao().insertCategory(
            CategoryEntity(name = "Automation", isDefault = true)
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testMacroCrudOperations() = runBlocking {
        val macro = MacroEntity(
            title = "Build & Deploy",
            description = "Runs build and triggers deploy",
            iconName = "RocketLaunch",
            categoryId = testCatId,
            orderIndex = 0,
            stepsJson = """[{"type":"delay","durationMs":50}]"""
        )
        val id = repository.insertMacro(macro)
        assertTrue(id > 0)

        val retrieved = repository.getMacroById(id)
        assertNotNull(retrieved)
        assertEquals("Build & Deploy", retrieved!!.title)
        assertEquals("RocketLaunch", retrieved.iconName)

        // Update
        repository.updateMacro(retrieved.copy(description = "Updated description"))
        val updated = repository.getMacroById(id)
        assertEquals("Updated description", updated!!.description)

        // Search
        val search = repository.searchMacros("Build").first()
        assertEquals(1, search.size)

        // Delete
        val deleted = repository.deleteMacroById(id)
        assertEquals(1, deleted)
        assertNull(repository.getMacroById(id))
    }

    @Test
    fun testGetCategoriesWithMacros() = runBlocking {
        repository.insertMacro(
            MacroEntity(title = "M1", stepsJson = "[]", categoryId = testCatId)
        )
        val categoriesWithMacros = repository.getCategoriesWithMacros().first()
        assertEquals(1, categoriesWithMacros.size)
        assertEquals(1, categoriesWithMacros[0].macros.size)
    }
}
