package com.zatiaras.pos.feature.inventory.domain.repository

import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Inventory operations.
 * 
 * Design: Offline-first approach.
 * - All reads are from Room (Single Source of Truth)
 * - Writes save to Room first, then queue for Supabase sync
 */
interface ProductRepository {

    // ==================== PRODUCTS ====================

    /**
     * Get all active products as a Flow.
     * UI observes this for reactive updates.
     */
    fun getProducts(): Flow<List<Product>>

    /**
     * Get products filtered by category.
     */
    fun getProductsByCategory(categoryId: String): Flow<List<Product>>

    /**
     * Get a single product by ID.
     */
    suspend fun getProductById(id: String): Product?

    /**
     * Search products by name/description.
     * Uses FTS4 for fast typo-tolerant search.
     */
    fun searchProducts(query: String): Flow<List<Product>>

    /**
     * Create a new product.
     * Saves to Room immediately, syncs to Supabase when online.
     */
    suspend fun createProduct(product: Product): Result<Product>

    /**
     * Update an existing product.
     */
    suspend fun updateProduct(product: Product): Result<Product>

    /**
     * Delete a product (soft delete).
     */
    suspend fun deleteProduct(id: String): Result<Unit>

    // ==================== CATEGORIES ====================

    /**
     * Get all categories as a Flow.
     */
    fun getCategories(): Flow<List<Category>>

    // ==================== SYNC ====================

    /**
     * Sync products from Supabase to Room.
     * Uses delta sync (only fetch changed items).
     */
    suspend fun syncFromRemote(): Result<Unit>

    /**
     * Upload pending changes to Supabase.
     */
    suspend fun syncToRemote(): Result<Unit>
}
