package com.zatiaras.pos.core.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations for Zatiaras POS.
 * 
 * IMPORTANT: Always add new migrations here instead of using fallbackToDestructiveMigration().
 * Destructive migration will delete all user data on schema changes.
 * 
 * Migration strategy:
 * - Version 1: Initial schema with products, categories, FTS
 * - Version 2: Added transactions and transaction_items tables
 * - Version 3: Added cash_records table for Buku Kas
 * - Version 4: Added users table for offline authentication
 * - Version 5: Added app_settings and add_ons tables
 * - Version 6: Added store_sessions table, sessionId to transactions
 */
object Migrations {

    /**
     * Migration from version 1 to 2.
     * Adds transactions and transaction_items tables.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create transactions table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `transactions` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `transactionNumber` TEXT NOT NULL,
                    `subtotal` INTEGER NOT NULL,
                    `discountAmount` INTEGER NOT NULL DEFAULT 0,
                    `discountPercent` REAL NOT NULL DEFAULT 0.0,
                    `taxAmount` INTEGER NOT NULL DEFAULT 0,
                    `taxPercent` REAL NOT NULL DEFAULT 0.0,
                    `grandTotal` INTEGER NOT NULL,
                    `paymentMethod` TEXT NOT NULL,
                    `amountPaid` INTEGER NOT NULL,
                    `changeAmount` INTEGER NOT NULL,
                    `notes` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `isSynced` INTEGER NOT NULL DEFAULT 0,
                    `isDeleted` INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            // Create indexes for transactions
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_createdAt` ON `transactions` (`createdAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_isSynced` ON `transactions` (`isSynced`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_transactionNumber` ON `transactions` (`transactionNumber`)")

            // Create transaction_items table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `transaction_items` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `transactionId` TEXT NOT NULL,
                    `productId` TEXT NOT NULL,
                    `productName` TEXT NOT NULL,
                    `productPrice` INTEGER NOT NULL,
                    `quantity` INTEGER NOT NULL,
                    `subtotal` INTEGER NOT NULL,
                    `notes` TEXT,
                    FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON DELETE CASCADE
                )
            """.trimIndent())

            // Create index for transaction_items
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_items_transactionId` ON `transaction_items` (`transactionId`)")
        }
    }

    /**
     * Migration from version 2 to 3.
     * Adds cash_records table for Buku Kas feature.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create cash_records table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `cash_records` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `type` TEXT NOT NULL,
                    `amount` INTEGER NOT NULL,
                    `description` TEXT NOT NULL,
                    `category` TEXT,
                    `notes` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `isSynced` INTEGER NOT NULL DEFAULT 0,
                    `isDeleted` INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            // Create indexes for cash_records
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_records_createdAt` ON `cash_records` (`createdAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_records_isSynced` ON `cash_records` (`isSynced`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_records_type` ON `cash_records` (`type`)")
        }
    }

    /**
     * Migration from version 3 to 4.
     * Adds users table for offline authentication.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create users table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `users` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `username` TEXT NOT NULL,
                    `passwordHash` TEXT NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `role` TEXT NOT NULL DEFAULT 'kasir',
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `isActive` INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent())

            // Create unique index for username
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")
        }
    }

    /**
     * Migration from version 4 to 5.
     * Adds app_settings table for settings sync and add_ons table for toppings.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create app_settings table (singleton - one row)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `app_settings` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `ownerPinHash` TEXT,
                    `lockedRoutes` TEXT NOT NULL DEFAULT '',
                    `storeName` TEXT NOT NULL DEFAULT 'ZATIARAS',
                    `storeAddress` TEXT,
                    `storePhone` TEXT,
                    `defaultPaperWidth` INTEGER NOT NULL DEFAULT 58,
                    `receiptFooter` TEXT DEFAULT 'Terima kasih atas kunjungan Anda!',
                    `showLogoOnReceipt` INTEGER NOT NULL DEFAULT 1,
                    `updatedAt` INTEGER NOT NULL,
                    `isSynced` INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            
            // Insert default settings row
            db.execSQL("""
                INSERT OR IGNORE INTO `app_settings` (id, storeName, defaultPaperWidth, updatedAt, isSynced)
                VALUES ('default', 'ZATIARAS', 58, ${System.currentTimeMillis()}, 0)
            """.trimIndent())

            // Create add_ons table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `add_ons` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `name` TEXT NOT NULL,
                    `price` INTEGER NOT NULL,
                    `category` TEXT,
                    `sortOrder` INTEGER NOT NULL DEFAULT 0,
                    `icon` TEXT,
                    `isActive` INTEGER NOT NULL DEFAULT 1,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `isSynced` INTEGER NOT NULL DEFAULT 0,
                    `isDeleted` INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            // Create indexes for add_ons
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_add_ons_category` ON `add_ons` (`category`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_add_ons_isActive` ON `add_ons` (`isActive`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_add_ons_isSynced` ON `add_ons` (`isSynced`)")
        }
    }

    /**
     * Migration from version 5 to 6.
     * Adds store_sessions table for Buka/Tutup Toko feature.
     * Adds sessionId column to transactions table.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create store_sessions table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `store_sessions` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `openingCash` INTEGER NOT NULL,
                    `openingTime` INTEGER NOT NULL,
                    `closingTime` INTEGER,
                    `isActive` INTEGER NOT NULL DEFAULT 0,
                    `branchId` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `isSynced` INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            // Create indexes for store_sessions
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_store_sessions_isActive` ON `store_sessions` (`isActive`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_store_sessions_openingTime` ON `store_sessions` (`openingTime`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_store_sessions_isSynced` ON `store_sessions` (`isSynced`)")

            // Add sessionId column to transactions table
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `sessionId` TEXT")
            
            // Create index for sessionId on transactions
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_sessionId` ON `transactions` (`sessionId`)")
        }
    }

    /**
     * Get all migrations in order.
     * Add new migrations to this list.
     */
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6
    )
}
