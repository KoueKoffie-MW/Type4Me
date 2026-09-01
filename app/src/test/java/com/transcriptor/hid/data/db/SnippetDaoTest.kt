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
class SnippetDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var snippetDao: SnippetDao
    private var testCategoryId: Long = 0L

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryDao = database.categoryDao()
        snippetDao = database.snippetDao()

        testCategoryId = categoryDao.insertCategory(
            CategoryEntity(name = "Test Category", isDefault = true)
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndGetSnippetById() = runBlocking {
        val snippet = SnippetEntity(
            title = "Git Status",
            content = "git status\n",
            categoryId = testCategoryId,
            orderIndex = 0,
            tags = listOf("git", "status"),
            syntaxType = SyntaxType.GIT,
            isFavorite = true
        )
        val id = snippetDao.insertSnippet(snippet)
        assertTrue(id > 0)

        val retrieved = snippetDao.getSnippetById(id)
        assertNotNull(retrieved)
        assertEquals("Git Status", retrieved!!.title)
        assertEquals("git status\n", retrieved.content)
        assertEquals(testCategoryId, retrieved.categoryId)
        assertEquals(listOf("git", "status"), retrieved.tags)
        assertEquals(SyntaxType.GIT, retrieved.syntaxType)
        assertTrue(retrieved.isFavorite)
    }

    @Test
    fun testSearchSnippetsByTitleContentAndTags() = runBlocking {
        snippetDao.insertSnippet(
            SnippetEntity(
                title = "Docker Build",
                content = "docker build -t app .\n",
                categoryId = testCategoryId,
                tags = listOf("container", "docker")
            )
        )
        snippetDao.insertSnippet(
            SnippetEntity(
                title = "Pytest Runner",
                content = "pytest -v\n",
                categoryId = testCategoryId,
                tags = listOf("python", "test")
            )
        )
        snippetDao.insertSnippet(
            SnippetEntity(
                title = "Cargo Check",
                content = "cargo check\n",
                categoryId = testCategoryId,
                tags = listOf("rust", "cargo")
            )
        )

        // Search title
        val dockerSearch = snippetDao.searchSnippets("Docker").first()
        assertEquals(1, dockerSearch.size)
        assertEquals("Docker Build", dockerSearch[0].title)

        // Search content
        val pytestSearch = snippetDao.searchSnippets("pytest").first()
        assertEquals(1, pytestSearch.size)
        assertEquals("Pytest Runner", pytestSearch[0].title)

        // Search tags
        val rustSearch = snippetDao.searchSnippets("rust").first()
        assertEquals(1, rustSearch.size)
        assertEquals("Cargo Check", rustSearch[0].title)
    }

    @Test
    fun testFavoriteToggleAndFilter() = runBlocking {
        val id1 = snippetDao.insertSnippet(
            SnippetEntity(title = "Fav 1", content = "c1", categoryId = testCategoryId, isFavorite = false)
        )
        val id2 = snippetDao.insertSnippet(
            SnippetEntity(title = "Fav 2", content = "c2", categoryId = testCategoryId, isFavorite = true)
        )

        val favsInitial = snippetDao.getFavoriteSnippets().first()
        assertEquals(1, favsInitial.size)
        assertEquals(id2, favsInitial[0].id)

        // Toggle id1 to favorite
        snippetDao.setFavorite(id1, true)
        val favsUpdated = snippetDao.getFavoriteSnippets().first()
        assertEquals(2, favsUpdated.size)

        // Toggle id2 to non-favorite
        snippetDao.setFavorite(id2, false)
        val favsFinal = snippetDao.getFavoriteSnippets().first()
        assertEquals(1, favsFinal.size)
        assertEquals(id1, favsFinal[0].id)
    }

    @Test
    fun testReorderingSnippets() = runBlocking {
        val id1 = snippetDao.insertSnippet(SnippetEntity(title = "Item 1", content = "1", categoryId = testCategoryId, orderIndex = 0))
        val id2 = snippetDao.insertSnippet(SnippetEntity(title = "Item 2", content = "2", categoryId = testCategoryId, orderIndex = 1))
        val id3 = snippetDao.insertSnippet(SnippetEntity(title = "Item 3", content = "3", categoryId = testCategoryId, orderIndex = 2))

        // Reorder to 3, 1, 2
        snippetDao.updateSnippetOrders(listOf(id3, id1, id2))

        val snippets = snippetDao.getSnippetsByCategory(testCategoryId).first()
        assertEquals(id3, snippets[0].id)
        assertEquals(0, snippets[0].orderIndex)
        assertEquals(id1, snippets[1].id)
        assertEquals(1, snippets[1].orderIndex)
        assertEquals(id2, snippets[2].id)
        assertEquals(2, snippets[2].orderIndex)
    }

    @Test
    fun testDeleteSnippetById() = runBlocking {
        val id = snippetDao.insertSnippet(SnippetEntity(title = "To Delete", content = "del", categoryId = testCategoryId))
        assertNotNull(snippetDao.getSnippetById(id))

        val deletedCount = snippetDao.deleteSnippetById(id)
        assertEquals(1, deletedCount)
        assertNull(snippetDao.getSnippetById(id))
    }
}
