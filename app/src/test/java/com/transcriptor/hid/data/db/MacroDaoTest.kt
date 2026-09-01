package com.transcriptor.hid.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
class MacroDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var macroDao: MacroDao
    private var testCategoryId: Long = 0L

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryDao = database.categoryDao()
        macroDao = database.macroDao()

        testCategoryId = categoryDao.insertCategory(
            CategoryEntity(name = "Dev Tools", isDefault = true)
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndGetMacroById() = runBlocking {
        val macro = MacroEntity(
            title = "VS Code Save & Test",
            description = "Save all and run pytest",
            iconName = "PlayArrow",
            categoryId = testCategoryId,
            orderIndex = 0,
            stepsJson = """[{"type":"key_combo","modifiers":1,"usageId":22}]"""
        )
        val id = macroDao.insertMacro(macro)
        assertTrue(id > 0)

        val retrieved = macroDao.getMacroById(id)
        assertNotNull(retrieved)
        assertEquals("VS Code Save & Test", retrieved!!.title)
        assertEquals("Save all and run pytest", retrieved.description)
        assertEquals("PlayArrow", retrieved.iconName)
        assertEquals(testCategoryId, retrieved.categoryId)
        assertEquals(0, retrieved.orderIndex)
        assertTrue(retrieved.stepsJson.contains("key_combo"))
    }

    @Test
    fun testSearchMacros() = runBlocking {
        macroDao.insertMacro(
            MacroEntity(
                title = "Terminal Clear",
                description = "Clears the active screen",
                stepsJson = "[]",
                categoryId = testCategoryId
            )
        )
        macroDao.insertMacro(
            MacroEntity(
                title = "Git Commit All",
                description = "Stages and commits",
                stepsJson = "[]",
                categoryId = testCategoryId
            )
        )

        val searchTitle = macroDao.searchMacros("Terminal").first()
        assertEquals(1, searchTitle.size)
        assertEquals("Terminal Clear", searchTitle[0].title)

        val searchDesc = macroDao.searchMacros("commits").first()
        assertEquals(1, searchDesc.size)
        assertEquals("Git Commit All", searchDesc[0].title)
    }

    @Test
    fun testReorderingMacros() = runBlocking {
        val id1 = macroDao.insertMacro(MacroEntity(title = "M1", stepsJson = "[]", categoryId = testCategoryId, orderIndex = 0))
        val id2 = macroDao.insertMacro(MacroEntity(title = "M2", stepsJson = "[]", categoryId = testCategoryId, orderIndex = 1))
        val id3 = macroDao.insertMacro(MacroEntity(title = "M3", stepsJson = "[]", categoryId = testCategoryId, orderIndex = 2))

        macroDao.updateMacroOrders(listOf(id3, id2, id1))

        val ordered = macroDao.getMacrosByCategory(testCategoryId).first()
        assertEquals(id3, ordered[0].id)
        assertEquals(0, ordered[0].orderIndex)
        assertEquals(id2, ordered[1].id)
        assertEquals(1, ordered[1].orderIndex)
        assertEquals(id1, ordered[2].id)
        assertEquals(2, ordered[2].orderIndex)
    }

    @Test
    fun testDeleteMacroById() = runBlocking {
        val id = macroDao.insertMacro(MacroEntity(title = "Delete Me", stepsJson = "[]", categoryId = testCategoryId))
        assertNotNull(macroDao.getMacroById(id))

        val deleted = macroDao.deleteMacroById(id)
        assertEquals(1, deleted)
        assertNull(macroDao.getMacroById(id))
    }
}
