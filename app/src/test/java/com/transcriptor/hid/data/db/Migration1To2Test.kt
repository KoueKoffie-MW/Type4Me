package com.transcriptor.hid.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration1To2Test {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("test_migration.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create V1 schema (presets only)
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `presets` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `system_prompt` TEXT NOT NULL,
                            `is_built_in` INTEGER NOT NULL,
                            `order_index` INTEGER NOT NULL,
                            `temperature` REAL NOT NULL,
                            `created_at` INTEGER NOT NULL,
                            `updated_at` INTEGER NOT NULL
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // No-op during initial setup
                }
            })
            .build()

        helper = FrameworkSQLiteOpenHelperFactory().create(config)
        db = helper.writableDatabase
    }

    @After
    fun tearDown() {
        db.close()
        helper.close()
    }

    @Test
    fun testMigration1To2PreservesPresetsAndCreatesNewTables() {
        // 1. Insert V1 preset
        db.execSQL("""
            INSERT INTO `presets` (`id`, `title`, `description`, `system_prompt`, `is_built_in`, `order_index`, `temperature`, `created_at`, `updated_at`)
            VALUES (1, 'V1 Preset', 'Desc', 'Prompt', 1, 0, 0.7, 1000, 1000)
        """.trimIndent())

        // 2. Execute Migration 1 -> 2
        AppDatabase.MIGRATION_1_2.migrate(db)

        // 3. Verify V1 data is preserved
        val presetCursor = db.query("SELECT * FROM presets WHERE id = 1")
        assertTrue(presetCursor.moveToFirst())
        val titleIndex = presetCursor.getColumnIndexOrThrow("title")
        assertEquals("V1 Preset", presetCursor.getString(titleIndex))
        presetCursor.close()

        // 4. Verify categories table exists and accepts records
        db.execSQL("""
            INSERT INTO `categories` (`name`, `icon_name`, `color_hex`, `display_order`, `is_default`, `created_at`)
            VALUES ('Git', 'Commit', '#F44336', 0, 1, 2000)
        """.trimIndent())
        val catCursor = db.query("SELECT * FROM categories WHERE name = 'Git'")
        assertTrue(catCursor.moveToFirst())
        assertEquals("Git", catCursor.getString(catCursor.getColumnIndexOrThrow("name")))
        val catId = catCursor.getLong(catCursor.getColumnIndexOrThrow("id"))
        catCursor.close()

        // 5. Verify snippets table exists and foreign key works
        db.execSQL("""
            INSERT INTO `snippets` (`title`, `content`, `category_id`, `order_index`, `tags`, `syntax_type`, `is_favorite`, `created_at`, `updated_at`)
            VALUES ('Git Push', 'git push', $catId, 0, '["git"]', 'GIT', 1, 2000, 2000)
        """.trimIndent())
        val snipCursor = db.query("SELECT * FROM snippets WHERE title = 'Git Push'")
        assertTrue(snipCursor.moveToFirst())
        assertEquals("git push", snipCursor.getString(snipCursor.getColumnIndexOrThrow("content")))
        snipCursor.close()

        // 6. Verify macros table exists
        db.execSQL("""
            INSERT INTO `macros` (`title`, `description`, `icon_name`, `category_id`, `order_index`, `steps_json`, `created_at`, `updated_at`)
            VALUES ('Macro 1', 'Desc', 'Bolt', $catId, 0, '[]', 2000, 2000)
        """.trimIndent())
        val macroCursor = db.query("SELECT * FROM macros WHERE title = 'Macro 1'")
        assertTrue(macroCursor.moveToFirst())
        assertEquals("Bolt", macroCursor.getString(macroCursor.getColumnIndexOrThrow("icon_name")))
        macroCursor.close()

        // 7. Verify paired_hosts table exists
        db.execSQL("""
            INSERT INTO `paired_hosts` (`address`, `hostName`, `customAlias`, `hostOs`, `preferredLayout`, `typingDelayMs`, `isFavorite`, `lastConnectedTimestamp`, `autoReconnect`)
            VALUES ('00:11:22:33:44:55', 'Host1', 'Work', 'WINDOWS', 'US_QWERTY', 8, 1, 3000, 1)
        """.trimIndent())
        val hostCursor = db.query("SELECT * FROM paired_hosts WHERE address = '00:11:22:33:44:55'")
        assertTrue(hostCursor.moveToFirst())
        assertEquals("Work", hostCursor.getString(hostCursor.getColumnIndexOrThrow("customAlias")))
        hostCursor.close()
    }
}
