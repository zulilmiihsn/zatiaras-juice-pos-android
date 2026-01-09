package com.zatiaras.pos.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zatiaras.pos.core.data.local.dao.CategoryDao
import com.zatiaras.pos.core.data.local.dao.ProductDao
import com.zatiaras.pos.core.data.local.entity.CategoryEntity
import com.zatiaras.pos.core.data.local.entity.ProductEntity
import com.zatiaras.pos.core.data.local.entity.ProductFtsEntity

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
 */
@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        ProductFtsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ZatiarasDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao

    companion object {
        const val DATABASE_NAME = "zatiaras_pos.db"
    }
}
