package com.zatiaras.pos.core.data.sync

import com.zatiaras.pos.core.data.local.SyncPreferences
import com.zatiaras.pos.core.data.local.dao.CategoryDao
import com.zatiaras.pos.core.data.local.entity.CategoryEntity
import com.zatiaras.pos.core.data.remote.InventoryRemoteDataSource
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Syncer for Categories.
 *
 * Push: Upload all unsynced categories (both active and inactive).
 *   - Active categories → upsert with is_active=true
 *   - Inactive (deleted) categories → upsert with is_active=false (SOFT DELETE)
 *   - No more hard delete! This avoids FK constraint violations entirely.
 *
 * Pull: Fetch all remote categories, apply Last-Write-Wins conflict resolution.
 *   - Remote active → insert/update locally as active
 *   - Remote inactive → insert/update locally as inactive
 */
@Singleton
class CategorySyncer @Inject constructor(
    private val categoryDao: CategoryDao,
    private val remoteDataSource: InventoryRemoteDataSource,
    private val syncPreferences: SyncPreferences
) : EntitySyncer {

    override val syncType: SyncType = SyncType.CATEGORIES

    override suspend fun sync(): SyncResult {
        var uploaded = 0
        var downloaded = 0
        var failed = 0

        // ──────────────────────────────────────────────
        // 1. PUSH: Upload ALL unsynced categories
        //    Both active and inactive use upsert (soft delete).
        //    No more hard delete = no more FK constraint issues!
        // ──────────────────────────────────────────────
        val unsyncedCategories = categoryDao.getUnsynced()

        if (unsyncedCategories.isNotEmpty()) {
            Timber.d("CategorySyncer: Found ${unsyncedCategories.size} unsynced categories")

            // Try batch upsert first (most efficient)
            remoteDataSource.upsertCategories(unsyncedCategories).fold(
                onSuccess = {
                    categoryDao.markAsSynced(unsyncedCategories.map { it.id })
                    uploaded += unsyncedCategories.size
                    val active = unsyncedCategories.count { it.isActive }
                    val inactive = unsyncedCategories.size - active
                    Timber.d("CategorySyncer: Batch synced $active active, $inactive inactive categories")
                },
                onFailure = { batchError ->
                    Timber.w(batchError, "CategorySyncer: Batch sync failed, falling back to individual sync")

                    // Fallback: sync one by one to isolate errors
                    for (category in unsyncedCategories) {
                        remoteDataSource.upsertCategory(category).fold(
                            onSuccess = {
                                categoryDao.markAsSynced(category.id)
                                uploaded++
                                Timber.d("CategorySyncer: Synced category ${category.id} (active=${category.isActive})")
                            },
                            onFailure = { error ->
                                failed++
                                Timber.e(error, "CategorySyncer: Failed to sync category ${category.id}")
                            }
                        )
                    }
                }
            )
        }

        // ──────────────────────────────────────────────
        // 2. PULL: Fetch remote categories
        //    Apply Last-Write-Wins conflict resolution.
        //    Respect is_active from remote.
        // ──────────────────────────────────────────────
        remoteDataSource.fetchCategories().fold(
            onSuccess = { remoteCategories ->
                if (remoteCategories.isNotEmpty()) {
                    Timber.d("CategorySyncer: Fetched ${remoteCategories.size} categories from remote")

                    val categoriesToInsert = mutableListOf<CategoryEntity>()

                    for (remoteCategory in remoteCategories) {
                        val localCategory = categoryDao.getById(remoteCategory.id)

                        if (localCategory == null) {
                            // New from remote → Insert as-is (respect remote is_active)
                            categoriesToInsert.add(remoteCategory.copy(isSynced = true))
                        } else {
                            // Conflict resolution: Last Write Wins
                            if (remoteCategory.updatedAt > localCategory.updatedAt) {
                                // Remote is newer → Overwrite local, keep remote's is_active
                                categoriesToInsert.add(remoteCategory.copy(isSynced = true))
                            } else {
                                // Local is newer → Keep local state
                                Timber.d("CategorySyncer: Keeping local version for ${remoteCategory.id} (local is newer)")
                            }
                        }
                    }

                    if (categoriesToInsert.isNotEmpty()) {
                        categoryDao.insertAll(categoriesToInsert)
                        downloaded = categoriesToInsert.size
                        Timber.d("CategorySyncer: Applied $downloaded category updates to local DB")
                    }

                    syncPreferences.updateLastCategoriesSyncTimestamp()
                }
            },
            onFailure = { error ->
                failed++
                Timber.e(error, "CategorySyncer: Failed to pull categories")
            }
        )

        Timber.d("CategorySyncer: Completed — uploaded=$uploaded, downloaded=$downloaded, failed=$failed")

        return SyncResult(
            type = SyncType.CATEGORIES,
            uploaded = uploaded,
            downloaded = downloaded,
            failed = failed
        )
    }

    override suspend fun getPendingCount(): Int {
        return categoryDao.getUnsyncedCount()
    }
}
