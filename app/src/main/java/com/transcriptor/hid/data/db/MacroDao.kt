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
 * Data Access Object for [MacroEntity] operations.
 */
@Dao
interface MacroDao {

    @Query("SELECT * FROM macros ORDER BY order_index ASC, id DESC")
    fun getAllMacros(): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macros WHERE category_id = :categoryId ORDER BY order_index ASC, id DESC")
    fun getMacrosByCategory(categoryId: Long): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macros WHERE id = :id")
    suspend fun getMacroById(id: Long): MacroEntity?

    @Query("SELECT * FROM macros WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY order_index ASC, id DESC")
    fun searchMacros(query: String): Flow<List<MacroEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: MacroEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(macros: List<MacroEntity>): List<Long>

    @Update
    suspend fun updateMacro(macro: MacroEntity)

    @Delete
    suspend fun deleteMacro(macro: MacroEntity)

    @Query("DELETE FROM macros WHERE id = :id")
    suspend fun deleteMacroById(id: Long): Int

    @Query("UPDATE macros SET order_index = :orderIndex WHERE id = :id")
    suspend fun updateOrderIndex(id: Long, orderIndex: Int)

    @Transaction
    suspend fun updateMacroOrders(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            updateOrderIndex(id, index)
        }
    }

    @Query("SELECT COUNT(*) FROM macros")
    suspend fun getMacroCount(): Int
}
