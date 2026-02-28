package com.zatiaras.pos.core.data.sync

import android.content.Context
import com.zatiaras.pos.core.data.local.SyncPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade for all sync operations.
 * 
 * Provides a unified API for:
 * - Manual sync trigger
 * - Sync status observation
 * - Pending changes count
 * - Last sync info
 * 
 * This is the main entry point for UI layer to interact with sync functionality.
 * 
 * Follows Single Responsibility Principle by delegating actual sync logic
 * to dedicated EntitySyncer implementations.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncPreferences: SyncPreferences,
    private val cashRecordSyncer: CashRecordSyncer,
    private val productSyncer: ProductSyncer,
    private val categorySyncer: CategorySyncer,
    private val transactionSyncer: TransactionSyncer
) {
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    // List of all syncers for iteration
    private val syncers: List<EntitySyncer> = listOf(
        categorySyncer,
        productSyncer,
        transactionSyncer,
        cashRecordSyncer
    )

    /**
     * Initialize sync - schedule periodic background sync.
     * Call this in Application.onCreate()
     */
    fun initialize() {
        SyncWorker.schedulePeriodicSync(context)
        Timber.d("SyncManager initialized with periodic sync")
    }

    /**
     * Trigger immediate sync.
     * Use for manual "Sync Now" button.
     */
    fun triggerSync() {
        SyncWorker.triggerImmediateSync(context)
        Timber.d("Manual sync triggered")
    }

    /**
     * Perform sync synchronously (for use in coroutine context).
     * Returns detailed results.
     */
    suspend fun syncNow(): List<SyncResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SyncResult>()
        
        try {
            syncPreferences.setSyncInProgress(true)
            
            val totalSyncers = syncers.size
            
            syncers.forEachIndexed { index, syncer ->
                val progress = ((index + 1) * 100) / totalSyncers
                _syncStatus.value = SyncStatus.Syncing(
                    progress = progress,
                    message = "Syncing ${syncer.syncType.name.lowercase().replace('_', ' ')}..."
                )
                
                val result = syncer.sync()
                results.add(result)
            }
            
            // Update timestamps
            syncPreferences.updateLastFullSyncTimestamp()
            
            val totalSynced = results.sumOf { it.totalSynced }
            val totalFailed = results.sumOf { it.failed }
            
            _syncStatus.value = if (totalFailed > 0) {
                SyncStatus.Error(
                    message = "Sync completed with $totalFailed errors",
                    isRetryable = true
                )
            } else {
                SyncStatus.Success(itemsSynced = totalSynced)
            }
            
            Timber.d("Sync completed: $totalSynced items synced, $totalFailed failed")
        } catch (e: Exception) {
            Timber.e(e, "Sync failed")
            _syncStatus.value = SyncStatus.Error(
                error = e,
                message = e.message ?: "Unknown error",
                isRetryable = true
            )
        } finally {
            syncPreferences.setSyncInProgress(false)
        }
        
        results
    }

    /**
     * Get count of pending (unsynced) items.
     */
    suspend fun getPendingCount(): Int = withContext(Dispatchers.IO) {
        syncers.sumOf { it.getPendingCount() }
    }

    /**
     * Check if sync is currently running.
     */
    fun isSyncing(): Flow<Boolean> = syncPreferences.isSyncInProgress()

    /**
     * Get last sync timestamp.
     */
    suspend fun getLastSyncTimestamp(): Long = syncPreferences.getLastFullSyncTimestamp()

    /**
     * Get sync info as a Flow for UI observation.
     */
    fun getSyncInfo(): Flow<SyncInfo> {
        return combine(
            syncPreferences.isSyncInProgress(),
            _syncStatus
        ) { inProgress, status ->
            SyncInfo(
                isInProgress = inProgress,
                status = status,
                lastSyncTimestamp = syncPreferences.getLastFullSyncTimestamp()
            )
        }
    }

    /**
     * Force full sync by resetting timestamps.
     */
    suspend fun forceFullSync() {
        syncPreferences.resetSyncTimestamps()
        syncNow()
    }

    /**
     * Cancel any pending sync work.
     */
    fun cancelSync() {
        SyncWorker.cancelAllSync(context)
        _syncStatus.value = SyncStatus.Idle
    }
}

/**
 * Sync info for UI display.
 */
data class SyncInfo(
    val isInProgress: Boolean,
    val status: SyncStatus,
    val lastSyncTimestamp: Long
) {
    val lastSyncFormatted: String
        get() {
            if (lastSyncTimestamp == 0L) return "Never synced"
            val diff = System.currentTimeMillis() - lastSyncTimestamp
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000} minutes ago"
                diff < 86400_000 -> "${diff / 3600_000} hours ago"
                else -> "${diff / 86400_000} days ago"
            }
        }
}
