package com.zatiaras.pos.feature.inventory.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.zatiaras.pos.core.data.di.ApplicationScope
import com.zatiaras.pos.core.data.local.SyncPreferences
import com.zatiaras.pos.core.data.local.dao.CategoryDao
import com.zatiaras.pos.core.data.local.dao.ProductDao
import com.zatiaras.pos.core.data.remote.InventoryRemoteDataSource
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.feature.inventory.data.mapper.toDomain
import com.zatiaras.pos.feature.inventory.data.mapper.toDomainList
import com.zatiaras.pos.feature.inventory.data.mapper.toEntity
import com.zatiaras.pos.core.domain.repository.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ProductRepository.
 * 
 * Offline-First Design:
 * 1. All reads ALWAYS come from Room (Single Source of Truth)
 * 2. All writes save to Room first, then attempt Supabase sync in background
 * 3. Sync failures are queued for retry (isSynced = false)
 */
@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val remoteDataSource: InventoryRemoteDataSource,
    private val syncPreferences: SyncPreferences,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ProductRepository {

    // ==================== PRODUCTS ====================

    override fun getProducts(): Flow<List<Product>> {
        return combine(
            productDao.getAllActive(),
            categoryDao.getAll()
        ) { products, categories ->
            val categoryMap = categories.associate { it.id to it.toDomain() }
            products.toDomainList(categoryMap)
        }
    }

    override fun getProductsByCategory(categoryId: String): Flow<List<Product>> {
        return combine(
            productDao.getByCategory(categoryId),
            categoryDao.getAll()
        ) { products, categories ->
            val categoryMap = categories.associate { it.id to it.toDomain() }
            products.toDomainList(categoryMap)
        }
    }

    override suspend fun getProductById(id: String): Product? {
        val entity = productDao.getById(id) ?: return null
        val category = entity.categoryId?.let { categoryDao.getById(it)?.toDomain() }
        return entity.toDomain(category)
    }

    override fun searchProducts(query: String): Flow<List<Product>> {
        // Use simple LIKE search for now
        // FTS4 search is available but needs proper index population
        return combine(
            productDao.searchSimple(query),
            categoryDao.getAll()
        ) { products, categories ->
            val categoryMap = categories.associate { it.id to it.toDomain() }
            products.toDomainList(categoryMap)
        }
    }

    // ==================== PAGINATED PRODUCTS ====================

    companion object {
        private const val PAGE_SIZE = 20
        private const val PREFETCH_DISTANCE = 5
    }

    private val pagingConfig = PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = PREFETCH_DISTANCE,
        enablePlaceholders = false
    )

    override fun getProductsPaged(): Flow<PagingData<Product>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { productDao.getAllActivePaged() }
        ).flow.map { pagingData ->
            val categories = categoryDao.getAll().first()
            val categoryMap = categories.associate { it.id to it.toDomain() }
            pagingData.map { entity ->
                entity.toDomain(categoryMap[entity.categoryId])
            }
        }
    }

    override fun getProductsByCategoryPaged(categoryId: String): Flow<PagingData<Product>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { productDao.getByCategoryPaged(categoryId) }
        ).flow.map { pagingData ->
            val categories = categoryDao.getAll().first()
            val categoryMap = categories.associate { it.id to it.toDomain() }
            pagingData.map { entity ->
                entity.toDomain(categoryMap[entity.categoryId])
            }
        }
    }

    override fun searchProductsPaged(query: String): Flow<PagingData<Product>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { productDao.searchPaged(query) }
        ).flow.map { pagingData ->
            val categories = categoryDao.getAll().first()
            val categoryMap = categories.associate { it.id to it.toDomain() }
            pagingData.map { entity ->
                entity.toDomain(categoryMap[entity.categoryId])
            }
        }
    }

    override fun getProductCount(): Flow<Int> {
        return productDao.getActiveProductCount()
    }

    override suspend fun createProduct(product: Product): Result<Product> {
        return try {
            val newProduct = product.copy(
                id = if (product.id.isBlank()) UUID.randomUUID().toString() else product.id,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Save to Room first (offline-first)
            productDao.insert(newProduct.toEntity(isSynced = false))
            Timber.d("Product created locally: ${newProduct.id}")
            
            // Attempt sync in background (non-blocking)
            applicationScope.launch {
                syncProductToRemote(newProduct.toEntity(isSynced = false))
            }
            
            Result.success(newProduct)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create product")
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(product: Product): Result<Product> {
        return try {
            val updatedProduct = product.copy(
                updatedAt = System.currentTimeMillis()
            )
            
            productDao.update(updatedProduct.toEntity(isSynced = false))
            Timber.d("Product updated locally: ${product.id}")
            
            // Attempt sync in background
            applicationScope.launch {
                syncProductToRemote(updatedProduct.toEntity(isSynced = false))
            }
            
            Result.success(updatedProduct)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update product: ${product.id}")
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(id: String): Result<Unit> {
        return try {
            productDao.softDelete(id)
            Timber.d("Product soft-deleted: $id")
            
            // Sync deletion in background
            applicationScope.launch {
                remoteDataSource.deleteProduct(id)
                    .onSuccess { Timber.d("Product deletion synced: $id") }
                    .onFailure { Timber.w("Failed to sync deletion, will retry: $id") }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete product: $id")
            Result.failure(e)
        }
    }

    // ==================== CATEGORIES ====================

    override fun getCategories(): Flow<List<Category>> {
        return categoryDao.getAll().map { entities ->
            entities.toDomainList()
        }
    }

    // ==================== SYNC ====================

    /**
     * Sync products and categories from Supabase to Room.
     * Uses delta sync - only fetches items updated since last sync.
     */
    override suspend fun syncFromRemote(): Result<Unit> {
        return try {
            // 1. Sync categories (full sync, they rarely change)
            remoteDataSource.fetchCategories()
                .onSuccess { categories ->
                    if (categories.isNotEmpty()) {
                        categoryDao.insertAll(categories)
                        syncPreferences.updateLastCategoriesSyncTimestamp()
                        Timber.d("Synced ${categories.size} categories from remote")
                    }
                }
                .onFailure { Timber.w(it, "Failed to sync categories") }

            // 2. Sync products (delta sync)
            val lastSync = syncPreferences.getLastProductsSyncTimestamp()
            remoteDataSource.fetchProducts(lastSync)
                .onSuccess { products ->
                    if (products.isNotEmpty()) {
                        productDao.insertAll(products)
                        syncPreferences.updateLastProductsSyncTimestamp()
                        Timber.d("Synced ${products.size} products from remote (since $lastSync)")
                    }
                }
                .onFailure { Timber.w(it, "Failed to sync products") }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Sync from remote failed")
            Result.failure(e)
        }
    }

    /**
     * Push unsynced local changes to Supabase.
     */
    override suspend fun syncToRemote(): Result<Unit> {
        return try {
            // Get all unsynced products
            val unsyncedProducts = productDao.getUnsynced()
            Timber.d("Found ${unsyncedProducts.size} unsynced products")

            var successCount = 0
            unsyncedProducts.forEach { product ->
                remoteDataSource.upsertProduct(product)
                    .onSuccess {
                        productDao.markAsSynced(product.id)
                        successCount++
                    }
                    .onFailure { Timber.w(it, "Failed to sync product: ${product.id}") }
            }

            Timber.d("Synced $successCount/${unsyncedProducts.size} products to remote")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Sync to remote failed")
            Result.failure(e)
        }
    }

    /**
     * Helper to sync single product to remote.
     */
    private suspend fun syncProductToRemote(product: com.zatiaras.pos.core.data.local.entity.ProductEntity) {
        remoteDataSource.upsertProduct(product)
            .onSuccess {
                productDao.markAsSynced(product.id)
                Timber.d("Product synced to remote: ${product.id}")
            }
            .onFailure {
                Timber.w(it, "Failed to sync product ${product.id}, marked for retry")
            }
    }
}

