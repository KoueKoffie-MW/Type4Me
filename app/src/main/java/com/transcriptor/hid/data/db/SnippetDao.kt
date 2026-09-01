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
 * Data Access Object for [SnippetEntity] operations.
 */
@Dao
interface SnippetDao {

    @Query("SELECT * FROM snippets ORDER BY order_index ASC, id DESC")
    fun getAllSnippets(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets WHERE category_id = :categoryId ORDER BY order_index ASC, id DESC")
    fun getSnippetsByCategory(categoryId: Long): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets WHERE is_favorite = 1 ORDER BY updated_at DESC")
    fun getFavoriteSnippets(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets WHERE id = :id")
    suspend fun getSnippetById(id: Long): SnippetEntity?

    @Query("""
        SELECT * FROM snippets 
        WHERE title LIKE '%' || :query || '%' 
           OR content LIKE '%' || :query || '%' 
           OR tags LIKE '%' || :query || '%'
        ORDER BY is_favorite DESC, order_index ASC, id DESC
    """)
    fun searchSnippets(query: String): Flow<List<SnippetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: SnippetEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(snippets: List<SnippetEntity>): List<Long>

    @Update
    suspend fun updateSnippet(snippet: SnippetEntity)

    @Delete
    suspend fun deleteSnippet(snippet: SnippetEntity)

    @Query("DELETE FROM snippets WHERE id = :id")
    suspend fun deleteSnippetById(id: Long): Int

    @Query("UPDATE snippets SET is_favorite = :isFavorite, updated_at = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE snippets SET order_index = :orderIndex WHERE id = :id")
    suspend fun updateOrderIndex(id: Long, orderIndex: Int)

    @Transaction
    suspend fun updateSnippetOrders(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            updateOrderIndex(id, index)
        }
    }

    @Query("SELECT COUNT(*) FROM snippets")
    suspend fun getSnippetCount(): Int
}
