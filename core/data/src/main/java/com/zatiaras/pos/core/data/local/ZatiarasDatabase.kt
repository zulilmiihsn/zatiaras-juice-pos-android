package com.zatiaras.pos.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zatiaras.pos.core.data.local.dao.AddOnDao
import com.zatiaras.pos.core.data.local.dao.AppSettingsDao
import com.zatiaras.pos.core.data.local.dao.CashRecordDao
import com.zatiaras.pos.core.data.local.dao.CategoryDao
import com.zatiaras.pos.core.data.local.dao.ProductDao
import com.zatiaras.pos.core.data.local.dao.StoreSessionDao
import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.core.data.local.dao.UserDao
import com.zatiaras.pos.core.data.local.entity.AddOnEntity
import com.zatiaras.pos.core.data.local.entity.AppSettingsEntity
import com.zatiaras.pos.core.data.local.entity.CashRecordEntity
import com.zatiaras.pos.core.data.local.entity.CategoryEntity
import com.zatiaras.pos.core.data.local.entity.LockedRoutesConverter
import com.zatiaras.pos.core.data.local.entity.ProductEntity
import com.zatiaras.pos.core.data.local.entity.ProductFtsEntity
import com.zatiaras.pos.core.data.local.entity.StoreSessionEntity
import com.zatiaras.pos.core.data.local.entity.TransactionEntity
import com.zatiaras.pos.core.data.local.entity.TransactionItemEntity
import com.zatiaras.pos.core.data.local.entity.UserEntity

/**
 * Main Room Database for ZatiarasPOS.
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
 * - v4: Added Users (Offline Auth)
 * - v5: Added AppSettings and AddOns (Settings Sync + Toppings)
 * - v7: Updated Product schema (Type + AddOns)
 */
@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        ProductFtsEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        CashRecordEntity::class,
        UserEntity::class,
        AppSettingsEntity::class,
        AddOnEntity::class,
        StoreSessionEntity::class
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(LockedRoutesConverter::class)
abstract class ZatiarasDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun cashRecordDao(): CashRecordDao
    abstract fun userDao(): UserDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun addOnDao(): AddOnDao
    abstract fun storeSessionDao(): StoreSessionDao

    companion object {
        const val DATABASE_NAME = "zatiaras_pos.db"
    }
}
