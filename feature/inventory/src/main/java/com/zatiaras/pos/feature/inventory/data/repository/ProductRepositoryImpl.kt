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
    private val productSyncer: com.zatiaras.pos.core.data.sync.ProductSyncer,
    private val categorySyncer: com.zatiaras.pos.core.data.sync.CategorySyncer,
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
            // We use ProductSyncer now to ensure consistent logic
            applicationScope.launch {
                productSyncer.sync()
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
                productSyncer.sync()
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
            // Sync deletion in background
            applicationScope.launch {
                productSyncer.sync()
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

    override suspend fun createCategory(name: String, icon: String?): Result<Category> {
        return try {
            // Check if a category with this name already exists (including soft-deleted)
            val existing = categoryDao.getByName(name.trim())

            // If already exists AND is active → do nothing, just notify caller
            if (existing != null && existing.isActive) {
                Timber.d("Category '${name.trim()}' already exists and is active, skipping")
                return Result.failure(Exception("DUPLICATE_ACTIVE:${name.trim()}"))
            }

            val entity = if (existing != null) {
                // Restore: reactivate the existing category instead of creating a duplicate
                existing.copy(
                    name = name.trim(),
                    icon = icon ?: existing.icon,
                    isActive = true,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
            } else {
                // Create new category
                com.zatiaras.pos.core.data.local.entity.CategoryEntity(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    icon = icon,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
            }

            categoryDao.insert(entity)
            
            if (existing != null) {
                Timber.d("Category restored: ${entity.name} (id=${entity.id})")
            } else {
                Timber.d("Category created: ${entity.name} (id=${entity.id})")
            }
            
            // Sync to remote
            applicationScope.launch {
                categorySyncer.sync()
            }
            
            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Timber.e(e, "Failed to create category")
            Result.failure(e)
        }
    }

    override suspend fun updateCategory(id: String, name: String, icon: String?): Result<Category> {
        return try {
            val existingEntity = categoryDao.getById(id)
            if (existingEntity != null) {
                val updatedEntity = existingEntity.copy(
                    name = name,
                    icon = icon ?: existingEntity.icon,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
                categoryDao.insert(updatedEntity) // upsert
                Timber.d("Category updated locally: ${updatedEntity.name}")
                
                applicationScope.launch {
                    categorySyncer.sync()
                }
                
                Result.success(updatedEntity.toDomain())
            } else {
                Result.failure(Exception("Category not found: $id"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update category: $id")
            Result.failure(e)
        }
    }

    override suspend fun assignProductsToCategory(categoryId: String, productIds: List<String>): Result<Unit> {
        return try {
            // 1. Clear category from products that had this category but aren't in the new list
            if (productIds.isNotEmpty()) {
                productDao.clearCategoryExcept(categoryId, productIds)
            }
            
            // 2. Set this category for the specified products
            if (productIds.isNotEmpty()) {
                productDao.setCategoryForProducts(categoryId, productIds)
            }
            
            Timber.d("Assigned ${productIds.size} products to category: $categoryId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to assign products to category: $categoryId")
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(id: String): Result<Unit> {
        return try {
            // 1. Clear category reference from products locally
            productDao.clearCategoryFromProducts(id)
            Timber.d("Cleared category $id from all products")

            // 2. Soft delete the category locally
            categoryDao.softDelete(id)
            Timber.d("Category soft-deleted locally: $id")

            // 3. Sync in background
            //    Since CategorySyncer now uses SOFT DELETE (upsert with is_active=false),
            //    there's no FK constraint issue. We don't need to sync products first.
            applicationScope.launch {
                try {
                    categorySyncer.sync()
                    // Also sync products so the cleared categoryId reaches Supabase
                    productSyncer.sync()
                } catch (e: Exception) {
                    Timber.e(e, "Background sync after category delete failed")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete category: $id")
            Result.failure(e)
        }
    }

    // ==================== SYNC ====================

    /**
     * Sync products and categories from Supabase to Room.
     * Uses delta sync - only fetches items updated since last sync.
     */
    override suspend fun syncFromRemote(): Result<Unit> {
        return try {
            // 1. Sync Categories First
            val catResult = categorySyncer.sync()
            
            // 2. Sync Products
            val prodResult = productSyncer.sync()
            
            val totalFailed = catResult.failed + prodResult.failed

            if (totalFailed > 0) {
                Result.failure(Exception("Sync completed with $totalFailed errors"))
            } else {
                Result.success(Unit)
            }
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
            val catResult = categorySyncer.sync()
            val prodResult = productSyncer.sync()
            
            val totalFailed = catResult.failed + prodResult.failed
            
            if (totalFailed > 0) {
                Result.failure(Exception("Sync with errors"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Sync to remote failed")
            Result.failure(e)
        }
    }

    /**
     * Helper to sync single product to remote.
     */
    // syncProductToRemote is removed as we use ProductSyncer now
}
