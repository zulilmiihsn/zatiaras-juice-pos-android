package com.zatiaras.pos.core.domain.repository

import com.zatiaras.pos.core.domain.model.AddOn
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Add-Ons data access.
 * 
 * Provides access to add-ons (ekstra/topping) that can be applied to products.
 * Follows Repository pattern for clean architecture.
 */
interface AddOnRepository {
    
    /**
     * Observe all active add-ons as a Flow.
     */
    fun observeActiveAddOns(): Flow<List<AddOn>>
    
    /**
     * Get all active add-ons (one-shot).
     */
    suspend fun getActiveAddOns(): List<AddOn>
    
    /**
     * Get add-ons by their IDs.
     * Used to fetch specific add-ons for a product based on ekstra_ids.
     */
    suspend fun getAddOnsByIds(ids: List<String>): List<AddOn>
    
    /**
     * Observe add-ons filtered by IDs.
     */
    fun observeAddOnsByIds(ids: List<String>): Flow<List<AddOn>>
    
    /**
     * Get a single add-on by ID.
     */
    suspend fun getAddOnById(id: String): AddOn?
    
    /**
     * Sync add-ons from remote server.
     */
    suspend fun syncFromRemote()

    /**
     * Create a new add-on.
     */
    suspend fun createAddOn(
        name: String,
        price: Long,
        category: String? = null,
        icon: String? = null
    ): Result<AddOn>

    /**
     * Delete an add-on (soft delete).
     */
    suspend fun deleteAddOn(id: String): Result<Unit>

    /**
     * Update an existing add-on.
     */
    suspend fun updateAddOn(id: String, name: String, price: Long): Result<AddOn>

    /**
     * Update add-on status (active/inactive).
     */
    suspend fun updateAddOnStatus(id: String, isActive: Boolean): Result<Unit>
}
