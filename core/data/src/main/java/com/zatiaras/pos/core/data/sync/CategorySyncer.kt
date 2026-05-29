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
 * Push:
 * - Active categories are upserted with is_active=true.
 * - Inactive/deleted categories are upserted with is_active=false.
 * - Soft delete avoids product foreign-key violations during sync.
 *
 * Pull:
 * - Remote categories are applied with Last-Write-Wins by updatedAt.
 * - Remote is_active is respected so deletes propagate locally.
 */
@Singleton
class CategorySyncer @Inject constructor(
    private val categoryDao: CategoryDao,
    private val remoteDataSource: InventoryRemoteDataSource,
    private val syncPreferences: SyncPreferences,
) : EntitySyncer {

    override val syncType: SyncType = SyncType.CATEGORIES

    override suspend fun sync(): SyncResult {
        var uploaded = 0
        var downloaded = 0
        var failed = 0

        // Push all unsynced categories. Both active and inactive rows use
        // upsert so the server sees soft deletes without hard-delete FK issues.
        val unsyncedCategories = categoryDao.getUnsynced()

        if (unsyncedCategories.isNotEmpty()) {
            Timber.d("CategorySyncer: Found ${unsyncedCategories.size} unsynced categories")

            // Try batch upsert first, then isolate failures with individual upserts.
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
                            },
                        )
                    }
                },
            )
        }

        // Pull remote categories and apply Last-Write-Wins while preserving the
        // remote active/deleted state.
        remoteDataSource.fetchCategories().fold(
            onSuccess = { remoteCategories ->
                if (remoteCategories.isNotEmpty()) {
                    Timber.d("CategorySyncer: Fetched ${remoteCategories.size} categories from remote")

                    val categoriesToInsert = mutableListOf<CategoryEntity>()

                    for (remoteCategory in remoteCategories) {
                        val localCategory = categoryDao.getById(remoteCategory.id)

                        if (localCategory == null) {
                            // New remote category; insert as-is.
                            categoriesToInsert.add(remoteCategory.copy(isSynced = true))
                        } else if (remoteCategory.updatedAt > localCategory.updatedAt) {
                            // Remote changed later; overwrite local copy.
                            categoriesToInsert.add(remoteCategory.copy(isSynced = true))
                        } else {
                            // Local changed later; keep local state.
                            Timber.d("CategorySyncer: Keeping local version for ${remoteCategory.id} (local is newer)")
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
            },
        )

        Timber.d("CategorySyncer: Completed - uploaded=$uploaded, downloaded=$downloaded, failed=$failed")

        return SyncResult(
            type = SyncType.CATEGORIES,
            uploaded = uploaded,
            downloaded = downloaded,
            failed = failed,
        )
    }

    override suspend fun getPendingCount(): Int = categoryDao.getUnsyncedCount()
}
