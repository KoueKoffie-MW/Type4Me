package com.transcriptor.hid.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultToolPackProviderTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSeedDefaultDatabasePopulatesCategoriesSnippetsAndMacros() = runBlocking {
        assertEquals(0, database.categoryDao().getCategoryCount())
        assertEquals(0, database.snippetDao().getSnippetCount())
        assertEquals(0, database.macroDao().getMacroCount())

        DefaultToolPackProvider.seedDefaultDatabase(database)

        // 1. Verify 5 Default Categories
        val categories = database.categoryDao().getAllCategories().first()
        assertEquals(5, categories.size)
        assertTrue(categories.all { it.isDefault })

        val categoryNames = categories.map { it.name }
        assertTrue(categoryNames.contains("Git & VCS"))
        assertTrue(categoryNames.contains("Containers & Cloud"))
        assertTrue(categoryNames.contains("Languages & Runtimes"))
        assertTrue(categoryNames.contains("Terminal & Navigation"))
        assertTrue(categoryNames.contains("AI Prompting"))

        // 2. Verify 20+ Production Snippets
        val snippets = database.snippetDao().getAllSnippets().first()
        assertTrue("Expected at least 20 snippets, but found ${snippets.size}", snippets.size >= 20)

        // Verify key snippets exist
        val titles = snippets.map { it.title }
        assertTrue(titles.contains("Git Status"))
        assertTrue(titles.contains("Git Commit with Message"))
        assertTrue(titles.contains("Docker Compose Up"))
        assertTrue(titles.contains("K8s Get All Pods"))
        assertTrue(titles.contains("Cargo Release Build"))
        assertTrue(titles.contains("Pytest Verbose Short"))
        assertTrue(titles.contains("Tmux New Named Session"))
        assertTrue(titles.contains("AI Prompt: Fix Compiler Errors"))

        // 3. Verify Snippets are correctly linked to categories
        val gitCat = categories.first { it.name == "Git & VCS" }
        val gitSnippets = database.snippetDao().getSnippetsByCategory(gitCat.id).first()
        assertTrue(gitSnippets.isNotEmpty())
        assertTrue(gitSnippets.all { it.categoryId == gitCat.id })

        // 4. Verify Default Macros
        val macros = database.macroDao().getAllMacros().first()
        assertTrue("Expected at least 1 default macro, but found ${macros.size}", macros.isNotEmpty())
        assertTrue(macros.any { it.title.contains("VS Code") })
    }
}
