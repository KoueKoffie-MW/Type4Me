package com.transcriptor.hid.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.transcriptor.hid.data.db.AppDatabase
import com.transcriptor.hid.data.db.CategoryEntity
import com.transcriptor.hid.data.db.SnippetEntity
import com.transcriptor.hid.data.db.SyntaxType
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
class SnippetRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: SnippetRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DefaultSnippetRepository(
            categoryDao = database.categoryDao(),
            snippetDao = database.snippetDao(),
            appDatabase = database
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testEnsureDefaultToolPackSeeded() = runBlocking {
        assertEquals(0, repository.getCategoryCount())

        repository.ensureDefaultToolPackSeeded(database)

        val categories = repository.getAllCategories().first()
        assertEquals(5, categories.size)

        val snippets = repository.getAllSnippets().first()
        assertTrue(snippets.size >= 20)

        val favorites = repository.getFavoriteSnippets().first()
        assertTrue(favorites.isNotEmpty())
    }

    @Test
    fun testCategoryOperations() = runBlocking {
        val catId = repository.insertCategory(
            CategoryEntity(name = "Custom Cat", iconName = "Code", colorHex = "#123456", displayOrder = 10)
        )
        assertTrue(catId > 0)

        val fetched = repository.getCategoryById(catId)
        assertNotNull(fetched)
        assertEquals("Custom Cat", fetched!!.name)

        repository.updateCategory(fetched.copy(name = "Updated Cat"))
        val updated = repository.getCategoryById(catId)
        assertEquals("Updated Cat", updated!!.name)

        val deleted = repository.deleteCustomCategoryById(catId)
        assertEquals(1, deleted)
        assertNull(repository.getCategoryById(catId))
    }

    @Test
    fun testSnippetOperationsAndSearch() = runBlocking {
        val catId = repository.insertCategory(CategoryEntity(name = "Dev"))
        val snippet = SnippetEntity(
            title = "Special Command",
            content = "special --flag\n",
            categoryId = catId,
            tags = listOf("special", "flag"),
            syntaxType = SyntaxType.SHELL
        )
        val id = repository.insertSnippet(snippet)
        assertTrue(id > 0)

        val retrieved = repository.getSnippetById(id)
        assertNotNull(retrieved)
        assertEquals("Special Command", retrieved!!.title)

        // Search
        val search = repository.searchSnippets("Special").first()
        assertEquals(1, search.size)
        assertEquals("Special Command", search[0].title)

        // Set favorite
        repository.setFavorite(id, true)
        val favs = repository.getFavoriteSnippets().first()
        assertEquals(1, favs.size)

        // Delete
        repository.deleteSnippetById(id)
        assertNull(repository.getSnippetById(id))
    }
}
