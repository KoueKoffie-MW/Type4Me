package com.transcriptor.hid.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.transcriptor.hid.ai.PromptPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main Room database for Transcriptor HID, managing persistent presets, categories,
 * snippets, macros, and paired host configurations (Version 2).
 */
@Database(
    entities = [
        PresetEntity::class,
        CategoryEntity::class,
        SnippetEntity::class,
        MacroEntity::class,
        PairedHostEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun presetDao(): PresetDao
    abstract fun categoryDao(): CategoryDao
    abstract fun snippetDao(): SnippetDao
    abstract fun macroDao(): MacroDao
    abstract fun pairedHostDao(): PairedHostDao

    companion object {
        const val DATABASE_NAME = "transcriptor_hid.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Room Migration from Version 1 (presets only) to Version 2 (categories, snippets, macros, paired_hosts).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create categories table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `icon_name` TEXT NOT NULL,
                        `color_hex` TEXT NOT NULL,
                        `display_order` INTEGER NOT NULL,
                        `is_default` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL
                    )
                """.trimIndent())

                // 2. Create snippets table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `snippets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `category_id` INTEGER NOT NULL,
                        `order_index` INTEGER NOT NULL,
                        `tags` TEXT NOT NULL,
                        `syntax_type` TEXT NOT NULL,
                        `is_favorite` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                """.trimIndent())

                // 3. Create composite indices for snippets
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_snippets_category_order_id` ON `snippets` (`category_id`, `order_index`, `id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_snippets_is_favorite` ON `snippets` (`is_favorite`)")

                // 4. Create macros table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `macros` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `icon_name` TEXT NOT NULL,
                        `category_id` INTEGER NOT NULL,
                        `order_index` INTEGER NOT NULL,
                        `steps_json` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                """.trimIndent())

                // 5. Create indices for macros
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_macros_category_id` ON `macros` (`category_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_macros_order_index` ON `macros` (`order_index`)")

                // 6. Create paired_hosts table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `paired_hosts` (
                        `address` TEXT PRIMARY KEY NOT NULL,
                        `hostName` TEXT NOT NULL,
                        `customAlias` TEXT NOT NULL,
                        `hostOs` TEXT NOT NULL,
                        `preferredLayout` TEXT NOT NULL,
                        `typingDelayMs` INTEGER NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        `lastConnectedTimestamp` INTEGER NOT NULL,
                        `autoReconnect` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_paired_hosts_is_favorite` ON `paired_hosts` (`isFavorite`)")
            }
        }

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
                    .addMigrations(MIGRATION_1_2)
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
                            DefaultToolPackProvider.seedDefaultDatabase(instance)
                        } catch (_: Throwable) {
                            // Fallback seeding is handled by Repository layers
                        }
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                val instance = INSTANCE ?: return
                scope.launch {
                    try {
                        if (instance.categoryDao().getCategoryCount() == 0) {
                            DefaultToolPackProvider.seedDefaultDatabase(instance)
                        }
                    } catch (_: Throwable) {
                        // Fallback seeding is handled by Repository layers
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
