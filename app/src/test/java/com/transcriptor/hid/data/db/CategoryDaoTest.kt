package com.transcriptor.hid.data.db

import androidx.room.Room
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
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CategoryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var snippetDao: SnippetDao
    private lateinit var macroDao: MacroDao

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryDao = database.categoryDao()
        snippetDao = database.snippetDao()
        macroDao = database.macroDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndGetCategory() = runBlocking {
        val category = CategoryEntity(
            name = "DevOps",
            iconName = "Cloud",
            colorHex = "#00BCD4",
            displayOrder = 1,
            isDefault = false
        )
        val id = categoryDao.insertCategory(category)
        assertTrue(id > 0)

        val fetched = categoryDao.getCategoryById(id)
        assertNotNull(fetched)
        assertEquals("DevOps", fetched!!.name)
        assertEquals("Cloud", fetched.iconName)
        assertEquals("#00BCD4", fetched.colorHex)
        assertEquals(1, fetched.displayOrder)
        assertEquals(false, fetched.isDefault)
    }

    @Test
    fun testGetAllCategoriesOrdered() = runBlocking {
        categoryDao.insertCategory(CategoryEntity(name = "Zeta", displayOrder = 2))
        categoryDao.insertCategory(CategoryEntity(name = "Alpha", displayOrder = 1))
        categoryDao.insertCategory(CategoryEntity(name = "Beta", displayOrder = 1))

        val all = categoryDao.getAllCategories().first()
        assertEquals(3, all.size)
        assertEquals("Alpha", all[0].name)
        assertEquals("Beta", all[1].name)
        assertEquals("Zeta", all[2].name)
    }

    @Test
    fun testDeleteCategoryCascadesToSnippetsAndMacros() = runBlocking {
        val catId = categoryDao.insertCategory(CategoryEntity(name = "Temporary", isDefault = false))

        val snippet = SnippetEntity(
            title = "Test Snippet",
            content = "echo test",
            categoryId = catId
        )
        val snipId = snippetDao.insertSnippet(snippet)

        val macro = MacroEntity(
            title = "Test Macro",
            stepsJson = "[]",
            categoryId = catId
        )
        val macId = macroDao.insertMacro(macro)

        // Verify relations exist
        assertNotNull(snippetDao.getSnippetById(snipId))
        assertNotNull(macroDao.getMacroById(macId))

        // Delete Category
        categoryDao.deleteCategory(categoryDao.getCategoryById(catId)!!)

        // Verify cascade deletion
        assertNull(categoryDao.getCategoryById(catId))
        assertNull(snippetDao.getSnippetById(snipId))
        assertNull(macroDao.getMacroById(macId))
    }

    @Test
    fun testDeleteCustomCategoryByIdProtectsDefaultCategories() = runBlocking {
        val defaultId = categoryDao.insertCategory(CategoryEntity(name = "Default Cat", isDefault = true))
        val customId = categoryDao.insertCategory(CategoryEntity(name = "Custom Cat", isDefault = false))

        val deletedDefault = categoryDao.deleteCustomCategoryById(defaultId)
        assertEquals(0, deletedDefault)
        assertNotNull(categoryDao.getCategoryById(defaultId))

        val deletedCustom = categoryDao.deleteCustomCategoryById(customId)
        assertEquals(1, deletedCustom)
        assertNull(categoryDao.getCategoryById(customId))
    }

    @Test
    fun testGetCategoriesWithSnippetsAndMacrosRelations() = runBlocking {
        val catId = categoryDao.insertCategory(CategoryEntity(name = "Combined", displayOrder = 0))
        snippetDao.insertSnippet(SnippetEntity(title = "S1", content = "c1", categoryId = catId))
        snippetDao.insertSnippet(SnippetEntity(title = "S2", content = "c2", categoryId = catId))
        macroDao.insertMacro(MacroEntity(title = "M1", stepsJson = "[]", categoryId = catId))

        val withSnippets = categoryDao.getCategoriesWithSnippets().first()
        assertEquals(1, withSnippets.size)
        assertEquals(2, withSnippets[0].snippets.size)

        val withMacros = categoryDao.getCategoriesWithMacros().first()
        assertEquals(1, withMacros.size)
        assertEquals(1, withMacros[0].macros.size)
    }
}
