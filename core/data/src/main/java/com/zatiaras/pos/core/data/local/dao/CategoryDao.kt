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
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    /**
     * Get a single category by ID.
     */
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    /**
     * Find a category by exact name (case-insensitive), including soft-deleted ones.
     * Used to restore a previously deleted category instead of creating a duplicate.
     */
    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getByName(name: String): CategoryEntity?

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
     * Delete a category by ID.
     */
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun hardDelete(id: String)

    /**
     * Soft delete: set isActive = false.
     */
    @Query("UPDATE categories SET isActive = 0, updatedAt = :timestamp, isSynced = 0 WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

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
     * Get count of unsynced categories (efficient COUNT instead of loading all).
     */
    @Query("SELECT COUNT(*) FROM categories WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int

    /**
     * Mark category as synced.
     */
    @Query("UPDATE categories SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    /**
     * Mark multiple categories as synced after successful bulk upload.
     */
    @Query("UPDATE categories SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}
