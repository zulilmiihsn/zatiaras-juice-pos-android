package com.zatiaras.pos.core.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zatiaras.pos.core.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Product operations.
 * Supports offline-first CRUD with sync tracking.
 * Includes FTS4 full-text search.
 */
@Dao
interface ProductDao {

    // ==================== READ ====================

    /**
     * Get all active products, ordered by creation date (newest first).
     * Returns Flow for reactive UI updates.
     */
    @Query("""
        SELECT * FROM products 
        WHERE isActive = 1 
        ORDER BY createdAt DESC
    """)
    fun getAllActive(): Flow<List<ProductEntity>>

    /**
     * Get products filtered by category.
     */
    @Query("""
        SELECT * FROM products 
        WHERE categoryId = :categoryId AND isActive = 1
        ORDER BY createdAt DESC
    """)
    fun getByCategory(categoryId: String): Flow<List<ProductEntity>>

    /**
     * Get a single product by ID.
     */
    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: String): ProductEntity?

    /**
     * Get products updated after a timestamp (for delta sync).
     */
    @Query("SELECT * FROM products WHERE updatedAt > :timestamp")
    suspend fun getUpdatedAfter(timestamp: Long): List<ProductEntity>

    // ==================== WRITE ====================

    /**
     * Insert or replace a single product.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity)

    /**
     * Insert or replace multiple products (for sync).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    /**
     * Update an existing product.
     */
    @Update
    suspend fun update(product: ProductEntity)

    /**
     * Soft delete: set isActive = false.
     * Also updates the timestamp for sync.
     */
    @Query("""
        UPDATE products 
        SET isActive = 0, updatedAt = :timestamp, isSynced = 0 
        WHERE id = :id
    """)
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Hard delete (use with caution).
     */
    @Query("DELETE FROM products WHERE id = :id")
    suspend fun delete(id: String)

    /**
     * Delete all products (for full refresh).
     */
    @Query("DELETE FROM products")
    suspend fun deleteAll()

    /**
     * Set category for multiple products.
     */
    @Query("""
        UPDATE products 
        SET categoryId = :categoryId, updatedAt = :timestamp, isSynced = 0 
        WHERE id IN (:productIds)
    """)
    suspend fun setCategoryForProducts(
        categoryId: String, 
        productIds: List<String>, 
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Clear category from products that have this category but are not in the list.
     */
    @Query("""
        UPDATE products 
        SET categoryId = NULL, updatedAt = :timestamp, isSynced = 0 
        WHERE categoryId = :categoryId AND id NOT IN (:keepProductIds)
    """)
    suspend fun clearCategoryExcept(
        categoryId: String, 
        keepProductIds: List<String>,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Clear category from products that have this category (e.g. when category is deleted).
     */
    @Query("""
        UPDATE products 
        SET categoryId = NULL, updatedAt = :timestamp, isSynced = 0 
        WHERE categoryId = :categoryId
    """)
    suspend fun clearCategoryFromProducts(
        categoryId: String, 
        timestamp: Long = System.currentTimeMillis()
    )

    // ==================== SYNC ====================

    /**
     * Get all unsynced products for upload.
     */
    @Query("SELECT * FROM products WHERE isSynced = 0")
    suspend fun getUnsynced(): List<ProductEntity>

    /**
     * Get count of unsynced products (efficient COUNT instead of loading all).
     */
    @Query("SELECT COUNT(*) FROM products WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int

    /**
     * Mark product as synced after successful upload.
     */
    @Query("UPDATE products SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    /**
     * Mark multiple products as synced after successful bulk upload.
     */
    @Query("UPDATE products SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    // ==================== SEARCH (FTS4) ====================

    /**
     * Full-text search on product name and description.
     * Uses FTS4 for fast, typo-tolerant search.
     * 
     * Query format: "kopi*" for prefix match.
     */
    @Query("""
        SELECT products.* FROM products
        JOIN products_fts ON products.rowid = products_fts.rowid
        WHERE products_fts MATCH :query AND products.isActive = 1
        ORDER BY products.createdAt DESC
    """)
    fun search(query: String): Flow<List<ProductEntity>>

    /**
     * Simple LIKE search for basic queries.
     * Fallback when FTS is not suitable.
     */
    @Query("""
        SELECT * FROM products 
        WHERE isActive = 1 AND (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchSimple(query: String): Flow<List<ProductEntity>>

    // ==================== PAGINATION ====================

    /**
     * Get all active products with pagination support.
     * Room automatically generates PagingSource implementation.
     */
    @Query("""
        SELECT * FROM products 
        WHERE isActive = 1 
        ORDER BY createdAt DESC
    """)
    fun getAllActivePaged(): PagingSource<Int, ProductEntity>

    /**
     * Get products filtered by category with pagination.
     */
    @Query("""
        SELECT * FROM products 
        WHERE categoryId = :categoryId AND isActive = 1
        ORDER BY createdAt DESC
    """)
    fun getByCategoryPaged(categoryId: String): PagingSource<Int, ProductEntity>

    /**
     * Search products with pagination.
     * Uses simple LIKE for better compatibility with paging.
     */
    @Query("""
        SELECT * FROM products 
        WHERE isActive = 1 AND (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchPaged(query: String): PagingSource<Int, ProductEntity>

    /**
     * Get total count of active products (for UI display).
     */
    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1")
    fun getActiveProductCount(): Flow<Int>
}
