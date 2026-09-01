package com.transcriptor.hid.data

import com.transcriptor.hid.data.db.CategoryDao
import com.transcriptor.hid.data.db.CategoryWithMacros
import com.transcriptor.hid.data.db.MacroDao
import com.transcriptor.hid.data.db.MacroEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing action macros and their execution steps.
 */
interface MacroRepository {
    fun getAllMacros(): Flow<List<MacroEntity>>
    fun getMacrosByCategory(categoryId: Long): Flow<List<MacroEntity>>
    fun getCategoriesWithMacros(): Flow<List<CategoryWithMacros>>
    suspend fun getMacroById(id: Long): MacroEntity?
    fun searchMacros(query: String): Flow<List<MacroEntity>>
    suspend fun insertMacro(macro: MacroEntity): Long
    suspend fun insertMacros(macros: List<MacroEntity>): List<Long>
    suspend fun updateMacro(macro: MacroEntity)
    suspend fun deleteMacro(macro: MacroEntity)
    suspend fun deleteMacroById(id: Long): Int
    suspend fun updateMacroOrders(orderedIds: List<Long>)
    suspend fun getMacroCount(): Int
}

/**
 * Default implementation of [MacroRepository] backed by Room DAOs.
 */
class DefaultMacroRepository(
    private val macroDao: MacroDao,
    private val categoryDao: CategoryDao
) : MacroRepository {

    override fun getAllMacros(): Flow<List<MacroEntity>> =
        macroDao.getAllMacros()

    override fun getMacrosByCategory(categoryId: Long): Flow<List<MacroEntity>> =
        macroDao.getMacrosByCategory(categoryId)

    override fun getCategoriesWithMacros(): Flow<List<CategoryWithMacros>> =
        categoryDao.getCategoriesWithMacros()

    override suspend fun getMacroById(id: Long): MacroEntity? =
        macroDao.getMacroById(id)

    override fun searchMacros(query: String): Flow<List<MacroEntity>> =
        macroDao.searchMacros(query)

    override suspend fun insertMacro(macro: MacroEntity): Long =
        macroDao.insertMacro(macro)

    override suspend fun insertMacros(macros: List<MacroEntity>): List<Long> =
        macroDao.insertAll(macros)

    override suspend fun updateMacro(macro: MacroEntity) =
        macroDao.updateMacro(macro)

    override suspend fun deleteMacro(macro: MacroEntity) =
        macroDao.deleteMacro(macro)

    override suspend fun deleteMacroById(id: Long): Int =
        macroDao.deleteMacroById(id)

    override suspend fun updateMacroOrders(orderedIds: List<Long>) =
        macroDao.updateMacroOrders(orderedIds)

    override suspend fun getMacroCount(): Int =
        macroDao.getMacroCount()
}
