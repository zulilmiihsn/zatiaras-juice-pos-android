package com.zatiaras.pos.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zatiaras.pos.core.data.local.entity.AddOnEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Add-Ons operations.
 * 
 * Add-ons are extra items that can be added to products during POS checkout.
 */
@Dao
interface AddOnDao {

    // ==================== READ ====================

    /**
     * Get all active add-ons as Flow.
     */
    @Query("SELECT * FROM add_ons WHERE isActive = 1 AND isDeleted = 0 ORDER BY sortOrder, name")
    fun observeActiveAddOns(): Flow<List<AddOnEntity>>

    /**
     * Get all add-ons (including inactive) as Flow.
     */
    @Query("SELECT * FROM add_ons WHERE isDeleted = 0 ORDER BY sortOrder, name")
    fun observeAllAddOns(): Flow<List<AddOnEntity>>

    /**
     * Get all active add-ons (suspend).
     */
    @Query("SELECT * FROM add_ons WHERE isActive = 1 AND isDeleted = 0 ORDER BY sortOrder, name")
    suspend fun getActiveAddOns(): List<AddOnEntity>

    /**
     * Get add-ons by category.
     */
    @Query("SELECT * FROM add_ons WHERE category = :category AND isActive = 1 AND isDeleted = 0 ORDER BY sortOrder, name")
    fun observeAddOnsByCategory(category: String): Flow<List<AddOnEntity>>

    /**
     * Get add-on by ID.
     */
    @Query("SELECT * FROM add_ons WHERE id = :id LIMIT 1")
    suspend fun getAddOnById(id: String): AddOnEntity?

    /**
     * Find an add-on by exact name (case-insensitive), including soft-deleted ones.
     * Used to restore a previously deleted add-on instead of creating a duplicate.
     */
    @Query("SELECT * FROM add_ons WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getByName(name: String): AddOnEntity?

    /**
     * Get add-ons by list of IDs.
     * Used to get available add-ons for a product based on ekstra_ids.
     */
    @Query("SELECT * FROM add_ons WHERE id IN (:ids) AND isActive = 1 AND isDeleted = 0 ORDER BY sortOrder, name")
    suspend fun getAddOnsByIds(ids: List<String>): List<AddOnEntity>

    /**
     * Observe add-ons by list of IDs (Flow).
     */
    @Query("SELECT * FROM add_ons WHERE id IN (:ids) AND isActive = 1 AND isDeleted = 0 ORDER BY sortOrder, name")
    fun observeAddOnsByIds(ids: List<String>): Flow<List<AddOnEntity>>

    /**
     * Get all unique categories.
     */
    @Query("SELECT DISTINCT category FROM add_ons WHERE category IS NOT NULL AND isActive = 1 AND isDeleted = 0")
    suspend fun getCategories(): List<String>

    /**
     * Search add-ons by name.
     */
    @Query("SELECT * FROM add_ons WHERE name LIKE '%' || :query || '%' AND isActive = 1 AND isDeleted = 0 ORDER BY name")
    suspend fun searchAddOns(query: String): List<AddOnEntity>

    // ==================== WRITE ====================

    /**
     * Insert a single add-on.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddOn(addOn: AddOnEntity)

    /**
     * Insert multiple add-ons.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddOns(addOns: List<AddOnEntity>)

    /**
     * Update an add-on.
     */
    @Update
    suspend fun updateAddOn(addOn: AddOnEntity)

    /**
     * Soft delete an add-on.
     */
    @Query("UPDATE add_ons SET isDeleted = 1, updatedAt = :timestamp, isSynced = 0 WHERE id = :id")
    suspend fun softDeleteAddOn(id: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Toggle add-on active status.
     */
    @Query("UPDATE add_ons SET isActive = NOT isActive, updatedAt = :timestamp, isSynced = 0 WHERE id = :id")
    suspend fun toggleActive(id: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Update active status explicitly.
     */
    @Query("UPDATE add_ons SET isActive = :isActive, updatedAt = :timestamp, isSynced = 0 WHERE id = :id")
    suspend fun updateStatus(id: String, isActive: Boolean, timestamp: Long = System.currentTimeMillis())

    /**
     * Hard delete all soft-deleted add-ons (cleanup).
     */
    @Query("DELETE FROM add_ons WHERE isDeleted = 1 AND isSynced = 1")
    suspend fun cleanupDeletedAddOns()

    // ==================== SYNC ====================

    /**
     * Get unsynced add-ons.
     */
    @Query("SELECT * FROM add_ons WHERE isSynced = 0")
    suspend fun getUnsyncedAddOns(): List<AddOnEntity>

    /**
     * Mark add-on as synced.
     */
    @Query("UPDATE add_ons SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    /**
     * Mark multiple add-ons as synced.
     */
    @Query("UPDATE add_ons SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markMultipleAsSynced(ids: List<String>)

    /**
     * Get add-ons updated after timestamp (for delta sync).
     */
    @Query("SELECT * FROM add_ons WHERE updatedAt > :timestamp")
    suspend fun getAddOnsUpdatedAfter(timestamp: Long): List<AddOnEntity>

    /**
     * Get count of unsynced add-ons.
     */
    @Query("SELECT COUNT(*) FROM add_ons WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int
}
