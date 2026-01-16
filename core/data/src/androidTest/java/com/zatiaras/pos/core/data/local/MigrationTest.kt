package com.zatiaras.pos.core.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Integration tests for Room database migrations.
 * 
 * Tests:
 * - Migration from v1 to v2
 * - Migration from v2 to v3
 * - Migration from v3 to v4
 * - Migration from v4 to v5
 * - Migration from v5 to v6
 * - Full migration path from v1 to v6
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ZatiarasDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        // Create database with version 1
        helper.createDatabase(TEST_DB, 1).apply {
            // Insert sample category and product
            execSQL("""
                INSERT INTO categories (id, name, sortOrder, createdAt, updatedAt, isSynced, isDeleted)
                VALUES ('cat-1', 'Minuman', 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 0)
            """)
            execSQL("""
                INSERT INTO products (id, name, price, categoryId, unit, imageUrl, description, isActive, sortOrder, createdAt, updatedAt, isSynced, isDeleted)
                VALUES ('prod-1', 'Es Teh', 5000, 'cat-1', 'pcs', null, 'Es teh manis', 1, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 0)
            """)
            close()
        }

        // Run migration 1 to 2
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, Migrations.MIGRATION_1_2)

        // Verify new tables exist
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='transactions'")
        assertTrue("transactions table should exist", cursor.moveToFirst())
        cursor.close()

        val cursorItems = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='transaction_items'")
        assertTrue("transaction_items table should exist", cursorItems.moveToFirst())
        cursorItems.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        helper.createDatabase(TEST_DB, 2).apply { close() }

        // Run migration 2 to 3
        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, Migrations.MIGRATION_2_3)

        // Verify cash_records table exists
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='cash_records'")
        assertTrue("cash_records table should exist", cursor.moveToFirst())
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        helper.createDatabase(TEST_DB, 3).apply { close() }

        // Run migration 3 to 4
        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, Migrations.MIGRATION_3_4)

        // Verify users table exists
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='users'")
        assertTrue("users table should exist", cursor.moveToFirst())
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        helper.createDatabase(TEST_DB, 4).apply { close() }

        // Run migration 4 to 5
        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, Migrations.MIGRATION_4_5)

        // Verify app_settings table exists
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='app_settings'")
        assertTrue("app_settings table should exist", cursor.moveToFirst())
        cursor.close()

        // Verify add_ons table exists
        val cursorAddOns = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='add_ons'")
        assertTrue("add_ons table should exist", cursorAddOns.moveToFirst())
        cursorAddOns.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate5To6() {
        helper.createDatabase(TEST_DB, 5).apply { close() }

        // Run migration 5 to 6
        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, Migrations.MIGRATION_5_6)

        // Verify store_sessions table exists
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='store_sessions'")
        assertTrue("store_sessions table should exist", cursor.moveToFirst())
        cursor.close()

        // Verify sessionId column was added to transactions
        val cursorTx = db.query("PRAGMA table_info(transactions)")
        var hasSessionId = false
        while (cursorTx.moveToNext()) {
            val columnName = cursorTx.getString(cursorTx.getColumnIndexOrThrow("name"))
            if (columnName == "sessionId") {
                hasSessionId = true
                break
            }
        }
        cursorTx.close()
        assertTrue("sessionId column should exist in transactions", hasSessionId)
    }

    @Test
    @Throws(IOException::class)
    fun migrateAllVersions() {
        // Create database with version 1
        helper.createDatabase(TEST_DB, 1).apply { close() }

        // Run all migrations
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 
            6, 
            true, 
            *Migrations.ALL_MIGRATIONS
        )

        // Verify all tables exist
        val expectedTables = listOf(
            "categories",
            "products",
            "products_fts",
            "transactions",
            "transaction_items",
            "cash_records",
            "users",
            "app_settings",
            "add_ons",
            "store_sessions"
        )

        for (tableName in expectedTables) {
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'")
            assertTrue("$tableName table should exist", cursor.moveToFirst())
            cursor.close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun dataPreservedAfterMigration() {
        // Create database with version 1 and insert data
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("""
                INSERT INTO categories (id, name, sortOrder, createdAt, updatedAt, isSynced, isDeleted)
                VALUES ('cat-test', 'Test Category', 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 0)
            """)
            execSQL("""
                INSERT INTO products (id, name, price, categoryId, unit, imageUrl, description, isActive, sortOrder, createdAt, updatedAt, isSynced, isDeleted)
                VALUES ('prod-test', 'Test Product', 10000, 'cat-test', 'pcs', null, 'Test description', 1, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0, 0)
            """)
            close()
        }

        // Run all migrations
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 
            6, 
            true, 
            *Migrations.ALL_MIGRATIONS
        )

        // Verify data is preserved
        val cursor = db.query("SELECT * FROM products WHERE id = 'prod-test'")
        assertTrue("Product data should be preserved", cursor.moveToFirst())
        assertEquals("Test Product", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        assertEquals(10000, cursor.getLong(cursor.getColumnIndexOrThrow("price")))
        cursor.close()

        val cursorCat = db.query("SELECT * FROM categories WHERE id = 'cat-test'")
        assertTrue("Category data should be preserved", cursorCat.moveToFirst())
        assertEquals("Test Category", cursorCat.getString(cursorCat.getColumnIndexOrThrow("name")))
        cursorCat.close()
    }
}
