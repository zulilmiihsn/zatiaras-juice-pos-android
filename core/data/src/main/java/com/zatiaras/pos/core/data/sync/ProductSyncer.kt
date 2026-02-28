package com.zatiaras.pos.core.data.sync

import com.zatiaras.pos.core.data.local.SyncPreferences
import com.zatiaras.pos.core.data.local.dao.CategoryDao
import com.zatiaras.pos.core.data.local.dao.ProductDao
import com.zatiaras.pos.core.data.local.entity.ProductEntity
import com.zatiaras.pos.core.data.remote.InventoryRemoteDataSource
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Syncer implementation for Product entities (Menu Items).
 *
 * Handles:
 * - Two-way sync (Push unsynced local changes, Pull remote updates)
 * - Conflict resolution: Last Write Wins based on updatedAt timestamp
 * - Smart Self-Healing for Foreign Key integrity
 */
@Singleton
class ProductSyncer @Inject constructor(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val remoteDataSource: InventoryRemoteDataSource,
    private val syncPreferences: SyncPreferences
) : EntitySyncer {

    override val syncType: SyncType = SyncType.PRODUCTS

    override suspend fun sync(): SyncResult {
        var uploaded = 0
        var downloaded = 0
        var failed = 0

        // 1. PUSH: Upload unsynced local changes first
        val unsyncedProducts = productDao.getUnsynced()
        if (unsyncedProducts.isNotEmpty()) {
            Timber.d("ProductSyncer: Found ${unsyncedProducts.size} unsynced products")
            val pushResult = pushUnsyncedProducts(unsyncedProducts)
            uploaded += pushResult.first
            failed += pushResult.second
        }

        // 2. PULL: Fetch remote updates
        val pullResult = pullRemoteUpdates()
        downloaded += pullResult.first
        failed += pullResult.second

        return SyncResult(
            type = SyncType.PRODUCTS,
            uploaded = uploaded,
            downloaded = downloaded,
            failed = failed
        )
    }

    /**
     * Push unsynced products to remote. Attempts batch first, then falls back to individual.
     * @return Pair(uploaded, failed)
     */
    private suspend fun pushUnsyncedProducts(products: List<ProductEntity>): Pair<Int, Int> {
        var uploaded = 0
        var failed = 0

        // BATCH UPLOAD: Try sending all at once
        remoteDataSource.upsertProducts(products).fold(
            onSuccess = {
                val ids = products.map { it.id }
                productDao.markAsSynced(ids)
                uploaded += products.size
                Timber.d("ProductSyncer: Batch synced ${products.size} products successfully")
            },
            onFailure = { error ->
                Timber.w(error, "ProductSyncer: Batch sync failed, falling back to individual sync")
                // FALLBACK: Try one by one to isolate the error
                for (product in products) {
                    val result = syncSingleProduct(product)
                    if (result) uploaded++ else failed++
                }
            }
        )

        return uploaded to failed
    }

    /**
     * Sync a single product with self-healing for FK integrity issues.
     * @return true if synced successfully, false if all attempts failed
     */
    private suspend fun syncSingleProduct(product: ProductEntity): Boolean {
        // Direct attempt
        remoteDataSource.upsertProduct(product).onSuccess {
            productDao.markAsSynced(product.id)
            Timber.d("ProductSyncer: Synced product ${product.id}")
            return true
        }

        // Direct failed — try self-healing if product has a category
        Timber.w("ProductSyncer: Direct sync failed for ${product.id}. Attempting self-healing...")
        if (product.categoryId != null && trySelfHealProduct(product)) {
            return true
        }

        // LAST RESORT: Sync without category to ensure product reaches remote
        Timber.w("ProductSyncer: Self-healing failed for ${product.id}. Removing category as last resort.")
        val productNoCat = product.copy(categoryId = null)
        remoteDataSource.upsertProduct(productNoCat).fold(
            onSuccess = {
                productDao.update(productNoCat.copy(isSynced = true))
                Timber.d("ProductSyncer: Product ${product.id} synced without category")
                return true
            },
            onFailure = { retryError ->
                Timber.e(retryError, "ProductSyncer: Final sync attempt failed for ${product.id}")
                return false
            }
        )
    }

    /**
     * Smart Self-Healing: Sync the missing category first, then retry the product.
     * @return true if healed and product synced, false otherwise
     */
    private suspend fun trySelfHealProduct(product: ProductEntity): Boolean {
        val categoryId = product.categoryId ?: return false
        val localCategory = categoryDao.getById(categoryId) ?: return false

        Timber.d("ProductSyncer: Syncing missing category $categoryId for product ${product.id}")
        remoteDataSource.upsertCategory(localCategory).onFailure { catError ->
            Timber.e(catError, "ProductSyncer: Failed to sync dependency category $categoryId")
            return false
        }

        // Category synced — retry product
        remoteDataSource.upsertProduct(product).fold(
            onSuccess = {
                productDao.markAsSynced(product.id)
                categoryDao.markAsSynced(categoryId)
                Timber.d("ProductSyncer: Self-healed product ${product.id} by syncing its category first")
                return true
            },
            onFailure = { retryError ->
                Timber.e(retryError, "ProductSyncer: Retry failed even after category sync for ${product.id}")
                return false
            }
        )
    }

    /**
     * Pull remote product updates and apply to local DB using Last-Write-Wins conflict resolution.
     * @return Pair(downloaded, failed)
     */
    private suspend fun pullRemoteUpdates(): Pair<Int, Int> {
        var downloaded = 0
        var failed = 0

        val lastSyncTimestamp = syncPreferences.getLastProductsSyncTimestamp()

        remoteDataSource.fetchProducts(lastSyncTimestamp).fold(
            onSuccess = { remoteProducts ->
                if (remoteProducts.isNotEmpty()) {
                    Timber.d("ProductSyncer: Fetched ${remoteProducts.size} products from remote")

                    val productsToInsert = mutableListOf<ProductEntity>()

                    for (remoteProduct in remoteProducts) {
                        val localProduct = productDao.getById(remoteProduct.id)

                        if (localProduct == null) {
                            productsToInsert.add(remoteProduct.copy(isSynced = true))
                        } else if (remoteProduct.updatedAt > localProduct.updatedAt) {
                            productsToInsert.add(remoteProduct.copy(isSynced = true))
                        } else {
                            Timber.d("ProductSyncer: Ignoring remote update for ${remoteProduct.id} (Local is newer)")
                        }
                    }

                    if (productsToInsert.isNotEmpty()) {
                        productDao.insertAll(productsToInsert)
                        downloaded = productsToInsert.size
                        Timber.d("ProductSyncer: Applied ${productsToInsert.size} updates to local DB")
                    }

                    syncPreferences.updateLastProductsSyncTimestamp()
                }
            },
            onFailure = { error ->
                failed++
                Timber.e(error, "ProductSyncer: Failed to fetch remote products")
            }
        )

        return downloaded to failed
    }

    override suspend fun getPendingCount(): Int {
        return productDao.getUnsyncedCount()
    }
}
