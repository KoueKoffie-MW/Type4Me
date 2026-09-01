package com.transcriptor.hid.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [CategoryEntity] operations.
 */
@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY display_order ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Transaction
    @Query("SELECT * FROM categories ORDER BY display_order ASC")
    fun getCategoriesWithSnippets(): Flow<List<CategoryWithSnippets>>

    @Transaction
    @Query("SELECT * FROM categories ORDER BY display_order ASC")
    fun getCategoriesWithMacros(): Flow<List<CategoryWithMacros>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id AND is_default = 0")
    suspend fun deleteCustomCategoryById(id: Long): Int

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}
