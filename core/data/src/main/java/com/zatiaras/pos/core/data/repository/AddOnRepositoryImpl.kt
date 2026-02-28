package com.zatiaras.pos.core.data.repository

import com.zatiaras.pos.core.data.local.dao.AddOnDao
import com.zatiaras.pos.core.data.local.entity.AddOnEntity
import com.zatiaras.pos.core.data.local.SyncPreferences
import com.zatiaras.pos.core.data.remote.AddOnRemoteDataSource
import com.zatiaras.pos.core.domain.model.AddOn
import com.zatiaras.pos.core.domain.repository.AddOnRepository as IAddOnRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
class AddOnRepositoryImpl @Inject constructor(
    private val addOnDao: AddOnDao,
    private val remoteDataSource: AddOnRemoteDataSource,
    private val syncPreferences: SyncPreferences
) : IAddOnRepository {
    
    // ==================== DOMAIN INTERFACE IMPLEMENTATION ====================
    
    override fun observeActiveAddOns(): Flow<List<AddOn>> {
        return addOnDao.observeActiveAddOns().map { entities -> 
            entities.map { it.toDomain() } 
        }
    }
    
    override suspend fun getActiveAddOns(): List<AddOn> {
        return addOnDao.getActiveAddOns().map { it.toDomain() }
    }
    
    override suspend fun getAddOnsByIds(ids: List<String>): List<AddOn> {
        if (ids.isEmpty()) return emptyList()
        return addOnDao.getAddOnsByIds(ids).map { it.toDomain() }
    }
    
    override fun observeAddOnsByIds(ids: List<String>): Flow<List<AddOn>> {
        if (ids.isEmpty()) {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }
        return addOnDao.observeAddOnsByIds(ids).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getAddOnById(id: String): AddOn? {
        return addOnDao.getAddOnById(id)?.toDomain()
    }
    
    override suspend fun syncFromRemote() {
        try {
            val lastSync = syncPreferences.getLastAddOnsSyncTimestamp()
            val remoteAddOns = remoteDataSource.fetchAddOns(lastSync).getOrThrow()
            
            if (remoteAddOns.isNotEmpty()) {
                addOnDao.insertAddOns(remoteAddOns)
                Timber.d("Synced ${remoteAddOns.size} add-ons from remote")
            }
            
            syncPreferences.updateLastAddOnsSyncTimestamp()
            
            // Also push any local changes
            pushUnsyncedToRemote()
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync add-ons from remote")
            throw e
        }
    }
    
    // ==================== ENTITY CONVERSION ====================
    
    private fun AddOnEntity.toDomain(): AddOn {
        return AddOn(
            id = id,
            name = name,
            price = price,
            isActive = isActive
        )
    }
    
    private fun AddOn.toEntity(isSynced: Boolean = false): AddOnEntity {
        return AddOnEntity(
            id = id,
            name = name,
            price = price,
            isActive = isActive,
            isSynced = isSynced
        )
    }

    // ==================== LEGACY READ (for backward compatibility) ====================

    /**
     * Observe all add-ons (including inactive).
     */
    fun observeAllAddOns(): Flow<List<AddOnEntity>> {
        return addOnDao.observeAllAddOns()
    }

    /**
     * Get all active add-ons as Entity.
     */
    suspend fun getActiveAddOnsEntity(): List<AddOnEntity> {
        return addOnDao.getActiveAddOns()
    }

    /**
     * Get add-on entity by ID.
     */
    suspend fun getAddOnEntityById(id: String): AddOnEntity? {
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
     * Create a new add-on, or restore a previously deleted one with the same name.
     */
    override suspend fun createAddOn(
        name: String,
        price: Long,
        category: String?,
        icon: String?
    ): Result<AddOn> {
        return try {
            // Check if an add-on with this name already exists (including soft-deleted)
            val existing = addOnDao.getByName(name.trim())

            // If already exists AND is active → notify caller
            if (existing != null && existing.isActive && !existing.isDeleted) {
                Timber.d("Add-on '${name.trim()}' already exists and is active, skipping")
                return Result.failure(Exception("DUPLICATE_ACTIVE:${name.trim()}"))
            }

            val addOn = if (existing != null) {
                // Restore: reactivate and update price
                existing.copy(
                    name = name.trim(),
                    price = price,
                    category = category ?: existing.category,
                    icon = icon ?: existing.icon,
                    isActive = true,
                    isDeleted = false,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
            } else {
                // Create new
                AddOnEntity(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    price = price,
                    category = category,
                    icon = icon,
                    sortOrder = 0,
                    isActive = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
            }
            
            addOnDao.insertAddOn(addOn)

            if (existing != null) {
                Timber.d("Restored add-on: ${addOn.name} (id=${addOn.id})")
            } else {
                Timber.d("Created add-on: ${addOn.name} (id=${addOn.id})")
            }
            
            // Try to sync immediately
            syncAddOnToRemote(addOn)
            
            Result.success(addOn.toDomain())
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
    override suspend fun deleteAddOn(id: String): Result<Unit> {
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
     * Update an existing add-on.
     */
    override suspend fun updateAddOn(id: String, name: String, price: Long): Result<AddOn> {
        return try {
            val existingEntity = addOnDao.getAddOnById(id)
            if (existingEntity != null) {
                val updatedEntity = existingEntity.copy(
                    name = name,
                    price = price,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
                addOnDao.updateAddOn(updatedEntity)
                Timber.d("Add-on updated locally: ${updatedEntity.name}")
                
                // Sync to remote
                syncAddOnToRemote(updatedEntity)
                
                Result.success(updatedEntity.toDomain())
            } else {
                Result.failure(Exception("Add-on not found: $id"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update add-on: $id")
            Result.failure(e)
        }
    }

    /**
     * Update add-on status (active/inactive).
     */
    override suspend fun updateAddOnStatus(id: String, isActive: Boolean): Result<Unit> {
        return try {
            addOnDao.updateStatus(id, isActive)
            Timber.d("Updated add-on status: $id -> $isActive")
            
            val updated = addOnDao.getAddOnById(id)
            if (updated != null) {
                syncAddOnToRemote(updated)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update add-on status")
            Result.failure(e)
        }
    }

    /**
     * Toggle add-on active status (legacy).
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
    suspend fun syncFromRemoteWithResult(): Result<Int> {
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
            val remoteAddOns = remoteDataSource.fetchAddOns().getOrThrow()
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
