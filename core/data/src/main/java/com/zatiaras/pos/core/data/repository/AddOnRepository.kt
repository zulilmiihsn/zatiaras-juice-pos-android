package com.zatiaras.pos.core.data.repository

import com.zatiaras.pos.core.data.local.dao.AddOnDao
import com.zatiaras.pos.core.data.local.entity.AddOnEntity
import com.zatiaras.pos.core.data.local.SyncPreferences
import com.zatiaras.pos.core.data.remote.AddOnRemoteDataSource
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Add-Ons/Toppings.
 * 
 * Implements offline-first pattern:
 * 1. Write to local Room database
 * 2. Sync to Supabase in background
 * 3. Pull remote changes on app start
 * 
 * Add-ons are extra items that can be added to products during POS checkout.
 */
@Singleton
class AddOnRepository @Inject constructor(
    private val addOnDao: AddOnDao,
    private val remoteDataSource: AddOnRemoteDataSource,
    private val syncPreferences: SyncPreferences
) {
    // ==================== READ ====================

    /**
     * Observe all active add-ons.
     */
    fun observeActiveAddOns(): Flow<List<AddOnEntity>> {
        return addOnDao.observeActiveAddOns()
    }

    /**
     * Observe all add-ons (including inactive).
     */
    fun observeAllAddOns(): Flow<List<AddOnEntity>> {
        return addOnDao.observeAllAddOns()
    }

    /**
     * Get all active add-ons.
     */
    suspend fun getActiveAddOns(): List<AddOnEntity> {
        return addOnDao.getActiveAddOns()
    }

    /**
     * Get add-on by ID.
     */
    suspend fun getAddOnById(id: String): AddOnEntity? {
        return addOnDao.getAddOnById(id)
    }

    /**
     * Observe add-ons by category.
     */
    fun observeAddOnsByCategory(category: String): Flow<List<AddOnEntity>> {
        return addOnDao.observeAddOnsByCategory(category)
    }

    /**
     * Get all unique add-on categories.
     */
    suspend fun getCategories(): List<String> {
        return addOnDao.getCategories()
    }

    /**
     * Search add-ons by name.
     */
    suspend fun searchAddOns(query: String): List<AddOnEntity> {
        return addOnDao.searchAddOns(query)
    }

    // ==================== WRITE ====================

    /**
     * Create a new add-on.
     */
    suspend fun createAddOn(
        name: String,
        price: Long,
        category: String? = null,
        icon: String? = null
    ): Result<AddOnEntity> {
        return try {
            val addOn = AddOnEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                price = price,
                category = category,
                icon = icon,
                sortOrder = 0,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            
            addOnDao.insertAddOn(addOn)
            Timber.d("Created add-on: ${addOn.name}")
            
            // Try to sync immediately
            syncAddOnToRemote(addOn)
            
            Result.success(addOn)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create add-on")
            Result.failure(e)
        }
    }

    /**
     * Update an existing add-on.
     */
    suspend fun updateAddOn(addOn: AddOnEntity): Result<Unit> {
        return try {
            val updated = addOn.copy(
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            addOnDao.updateAddOn(updated)
            Timber.d("Updated add-on: ${addOn.name}")
            
            syncAddOnToRemote(updated)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update add-on")
            Result.failure(e)
        }
    }

    /**
     * Delete an add-on (soft delete).
     */
    suspend fun deleteAddOn(id: String): Result<Unit> {
        return try {
            addOnDao.softDeleteAddOn(id)
            Timber.d("Deleted add-on: $id")
            
            // Get the updated entity and sync
            val deleted = addOnDao.getAddOnById(id)
            if (deleted != null) {
                syncAddOnToRemote(deleted)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete add-on")
            Result.failure(e)
        }
    }

    /**
     * Toggle add-on active status.
     */
    suspend fun toggleActive(id: String): Result<Unit> {
        return try {
            addOnDao.toggleActive(id)
            
            val updated = addOnDao.getAddOnById(id)
            if (updated != null) {
                syncAddOnToRemote(updated)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle add-on active status")
            Result.failure(e)
        }
    }

    // ==================== SYNC ====================

    /**
     * Sync add-ons from remote to local.
     * Uses delta sync based on last sync timestamp.
     */
    suspend fun syncFromRemote(): Result<Int> {
        return try {
            val lastSync = syncPreferences.getLastAddOnsSyncTimestamp()
            val remoteAddOns = remoteDataSource.fetchAddOns(lastSync).getOrThrow()
            
            if (remoteAddOns.isNotEmpty()) {
                addOnDao.insertAddOns(remoteAddOns)
                Timber.d("Synced ${remoteAddOns.size} add-ons from remote")
            }
            
            syncPreferences.updateLastAddOnsSyncTimestamp()
            
            // Also push any local changes
            pushUnsyncedToRemote()
            
            Result.success(remoteAddOns.size)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync add-ons from remote")
            Result.failure(e)
        }
    }

    /**
     * Sync a single add-on to remote.
     */
    private suspend fun syncAddOnToRemote(addOn: AddOnEntity) {
        try {
            remoteDataSource.uploadAddOn(addOn).onSuccess {
                addOnDao.markAsSynced(addOn.id)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync add-on to remote: ${addOn.id}")
        }
    }

    /**
     * Push all unsynced add-ons to remote.
     */
    suspend fun pushUnsyncedToRemote(): Result<Int> {
        return try {
            val unsynced = addOnDao.getUnsyncedAddOns()
            if (unsynced.isEmpty()) {
                return Result.success(0)
            }
            
            val result = remoteDataSource.uploadAddOns(unsynced)
            result.onSuccess { count ->
                val ids = unsynced.map { it.id }
                addOnDao.markMultipleAsSynced(ids)
                Timber.d("Pushed $count add-ons to remote")
            }
            
            result
        } catch (e: Exception) {
            Timber.e(e, "Failed to push add-ons to remote")
            Result.failure(e)
        }
    }

    /**
     * Full sync - pull all from remote and push all local changes.
     */
    suspend fun fullSync(): Result<Unit> {
        return try {
            // Pull from remote (full, not delta)
            val remoteAddOns = remoteDataSource.fetchAllAddOns().getOrThrow()
            addOnDao.insertAddOns(remoteAddOns)
            
            // Push local changes
            pushUnsyncedToRemote()
            
            syncPreferences.updateLastAddOnsSyncTimestamp()
            
            Timber.d("Full add-ons sync completed")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to perform full add-ons sync")
            Result.failure(e)
        }
    }

    /**
     * Get count of unsynced add-ons.
     */
    suspend fun getUnsyncedCount(): Int {
        return addOnDao.getUnsyncedCount()
    }

    /**
     * Cleanup deleted add-ons that have been synced.
     */
    suspend fun cleanupDeletedAddOns() {
        addOnDao.cleanupDeletedAddOns()
    }
}
