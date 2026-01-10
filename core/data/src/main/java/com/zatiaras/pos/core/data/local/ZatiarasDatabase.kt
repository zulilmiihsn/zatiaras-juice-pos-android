package com.zatiaras.pos.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zatiaras.pos.core.data.local.dao.CashRecordDao
import com.zatiaras.pos.core.data.local.dao.CategoryDao
import com.zatiaras.pos.core.data.local.dao.ProductDao
import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.core.data.local.entity.CashRecordEntity
import com.zatiaras.pos.core.data.local.entity.CategoryEntity
import com.zatiaras.pos.core.data.local.entity.ProductEntity
import com.zatiaras.pos.core.data.local.entity.ProductFtsEntity
import com.zatiaras.pos.core.data.local.entity.TransactionEntity
import com.zatiaras.pos.core.data.local.entity.TransactionItemEntity

/**
 * Main Room Database for Zatiaras POS.
 * 
 * Design Decisions:
 * - Single database for all entities (simpler transactions)
 * - FTS4 virtual table for fast product search
 * - Version starts at 1, increment on schema changes
 * 
 * IMPORTANT: When adding new entities:
 * 1. Add to entities array
 * 2. Add abstract DAO getter
 * 3. Increment version
 * 4. Add migration or use fallbackToDestructiveMigration (dev only)
 * 
 * Version History:
 * - v1: Initial (Categories, Products, ProductFts)
 * - v2: Added Transactions and TransactionItems
 * - v3: Added CashRecords (Buku Kas)
 */
@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        ProductFtsEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        CashRecordEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class ZatiarasDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun cashRecordDao(): CashRecordDao

    companion object {
        const val DATABASE_NAME = "zatiaras_pos.db"
    }
}
