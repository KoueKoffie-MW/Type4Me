package com.transcriptor.hid.data

import com.transcriptor.hid.data.db.AppDatabase
import com.transcriptor.hid.data.db.CategoryDao
import com.transcriptor.hid.data.db.CategoryEntity
import com.transcriptor.hid.data.db.CategoryWithSnippets
import com.transcriptor.hid.data.db.DefaultToolPackProvider
import com.transcriptor.hid.data.db.SnippetDao
import com.transcriptor.hid.data.db.SnippetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing categories and snippets.
 */
interface SnippetRepository {
    // Categories
    fun getAllCategories(): Flow<List<CategoryEntity>>
    suspend fun getCategoryById(id: Long): CategoryEntity?
    fun getCategoriesWithSnippets(): Flow<List<CategoryWithSnippets>>
    suspend fun insertCategory(category: CategoryEntity): Long
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun deleteCategory(category: CategoryEntity)
    suspend fun deleteCustomCategoryById(id: Long): Int
    suspend fun getCategoryCount(): Int

    // Snippets
    fun getAllSnippets(): Flow<List<SnippetEntity>>
    fun getSnippetsByCategory(categoryId: Long): Flow<List<SnippetEntity>>
    fun getFavoriteSnippets(): Flow<List<SnippetEntity>>
    suspend fun getSnippetById(id: Long): SnippetEntity?
    fun searchSnippets(query: String): Flow<List<SnippetEntity>>
    suspend fun insertSnippet(snippet: SnippetEntity): Long
    suspend fun insertSnippets(snippets: List<SnippetEntity>): List<Long>
    suspend fun updateSnippet(snippet: SnippetEntity)
    suspend fun deleteSnippet(snippet: SnippetEntity)
    suspend fun deleteSnippetById(id: Long): Int
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
    suspend fun updateSnippetOrders(orderedIds: List<Long>)

    // Seeding
    suspend fun ensureDefaultToolPackSeeded(db: AppDatabase? = null)
}

/**
 * Default implementation of [SnippetRepository] backed by Room DAOs.
 */
class DefaultSnippetRepository(
    private val categoryDao: CategoryDao,
    private val snippetDao: SnippetDao,
    private val appDatabase: AppDatabase? = null
) : SnippetRepository {

    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    override suspend fun getCategoryById(id: Long): CategoryEntity? =
        categoryDao.getCategoryById(id)

    override fun getCategoriesWithSnippets(): Flow<List<CategoryWithSnippets>> =
        categoryDao.getCategoriesWithSnippets()

    override suspend fun insertCategory(category: CategoryEntity): Long =
        categoryDao.insertCategory(category)

    override suspend fun updateCategory(category: CategoryEntity) =
        categoryDao.updateCategory(category)

    override suspend fun deleteCategory(category: CategoryEntity) =
        categoryDao.deleteCategory(category)

    override suspend fun deleteCustomCategoryById(id: Long): Int =
        categoryDao.deleteCustomCategoryById(id)

    override suspend fun getCategoryCount(): Int =
        categoryDao.getCategoryCount()

    override fun getAllSnippets(): Flow<List<SnippetEntity>> =
        snippetDao.getAllSnippets()

    override fun getSnippetsByCategory(categoryId: Long): Flow<List<SnippetEntity>> =
        snippetDao.getSnippetsByCategory(categoryId)

    override fun getFavoriteSnippets(): Flow<List<SnippetEntity>> =
        snippetDao.getFavoriteSnippets()

    override suspend fun getSnippetById(id: Long): SnippetEntity? =
        snippetDao.getSnippetById(id)

    override fun searchSnippets(query: String): Flow<List<SnippetEntity>> =
        snippetDao.searchSnippets(query)

    override suspend fun insertSnippet(snippet: SnippetEntity): Long =
        snippetDao.insertSnippet(snippet)

    override suspend fun insertSnippets(snippets: List<SnippetEntity>): List<Long> =
        snippetDao.insertAll(snippets)

    override suspend fun updateSnippet(snippet: SnippetEntity) =
        snippetDao.updateSnippet(snippet)

    override suspend fun deleteSnippet(snippet: SnippetEntity) =
        snippetDao.deleteSnippet(snippet)

    override suspend fun deleteSnippetById(id: Long): Int =
        snippetDao.deleteSnippetById(id)

    override suspend fun setFavorite(id: Long, isFavorite: Boolean) =
        snippetDao.setFavorite(id, isFavorite)

    override suspend fun updateSnippetOrders(orderedIds: List<Long>) =
        snippetDao.updateSnippetOrders(orderedIds)

    override suspend fun ensureDefaultToolPackSeeded(db: AppDatabase?) {
        val targetDb = db ?: appDatabase
        if (targetDb != null) {
            if (categoryDao.getCategoryCount() == 0) {
                DefaultToolPackProvider.seedDefaultDatabase(targetDb)
            }
        }
    }
}
