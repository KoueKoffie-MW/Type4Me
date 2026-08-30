package com.transcriptor.hid.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.transcriptor.hid.ai.PromptPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main Room database for Transcriptor HID, managing persistent presets and configuration.
 */
@Database(
    entities = [PresetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun presetDao(): PresetDao

    companion object {
        const val DATABASE_NAME = "transcriptor_hid.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(
            context: Context,
            coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback(coroutineScope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val instance = INSTANCE
                if (instance != null) {
                    scope.launch {
                        try {
                            populateInitialPresets(instance.presetDao())
                        } catch (_: Throwable) {
                            // Fallback seeding is handled by PresetRepository.ensureBuiltInPresetsSeeded()
                        }
                    }
                }
            }
        }

        /**
         * Seeds built-in prompt presets into the database.
         */
        suspend fun populateInitialPresets(presetDao: PresetDao) {
            val entities = PromptPreset.BUILTIN_PRESETS.mapIndexed { index, preset ->
                PresetEntity.fromDomain(preset, orderIndex = index)
            }
            presetDao.insertAll(entities)
        }
    }
}
