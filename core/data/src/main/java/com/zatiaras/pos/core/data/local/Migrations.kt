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
     * Get all migrations in order.
     * Add new migrations to this list.
     */
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3
    )
}
