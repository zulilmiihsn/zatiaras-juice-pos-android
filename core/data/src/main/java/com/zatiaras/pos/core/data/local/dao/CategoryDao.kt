package com.zatiaras.pos.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zatiaras.pos.core.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Category operations.
 * Provides reactive Flow for observing category changes.
 */
@Dao
interface CategoryDao {

    /**
     * Get all categories ordered by sort order.
     * Returns Flow for reactive updates.
     */
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    /**
     * Get a single category by ID.
     */
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    /**
     * Insert or replace categories (for sync).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    /**
     * Insert or replace a single category.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    /**
     * Delete all categories (for full refresh).
     */
    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    /**
     * Get unsynced categories for upload.
     */
    @Query("SELECT * FROM categories WHERE isSynced = 0")
    suspend fun getUnsynced(): List<CategoryEntity>

    /**
     * Mark category as synced.
     */
    @Query("UPDATE categories SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
