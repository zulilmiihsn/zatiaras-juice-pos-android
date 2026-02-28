package com.zatiaras.pos.core.domain.repository

import androidx.paging.PagingData
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Product operations.
 * 
 * IMPORTANT: This interface lives in core/domain to be accessible from any feature module
 * without creating cross-feature dependencies.
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

    // ==================== PAGINATED PRODUCTS ====================

    /**
     * Get all active products with pagination.
     * Efficiently loads products in chunks for large catalogs.
     */
    fun getProductsPaged(): Flow<PagingData<Product>>

    /**
     * Get products filtered by category with pagination.
     */
    fun getProductsByCategoryPaged(categoryId: String): Flow<PagingData<Product>>

    /**
     * Search products with pagination.
     */
    fun searchProductsPaged(query: String): Flow<PagingData<Product>>

    /**
     * Get total count of active products.
     */
    fun getProductCount(): Flow<Int>

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

    /**
     * Create a new category.
     */
    suspend fun createCategory(name: String, icon: String? = null): Result<Category>

    /**
     * Update an existing category.
     */
    suspend fun updateCategory(id: String, name: String, icon: String? = null): Result<Category>

    /**
     * Assign products to a category.
     * Clears old assignments and sets the given products to this category.
     */
    suspend fun assignProductsToCategory(categoryId: String, productIds: List<String>): Result<Unit>

    /**
     * Delete a category.
     */
    suspend fun deleteCategory(id: String): Result<Unit>

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
