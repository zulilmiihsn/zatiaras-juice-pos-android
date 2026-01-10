package com.zatiaras.pos.core.data.sync

import android.content.Context
import com.zatiaras.pos.core.data.local.SyncPreferences
import com.zatiaras.pos.core.data.local.dao.CashRecordDao
import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.core.data.remote.CashRecordRemoteDataSource
import com.zatiaras.pos.core.data.remote.TransactionRemoteDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncPreferences: SyncPreferences,
    private val transactionDao: TransactionDao,
    private val cashRecordDao: CashRecordDao,
    private val transactionRemoteDataSource: TransactionRemoteDataSource,
    private val cashRecordRemoteDataSource: CashRecordRemoteDataSource
) {
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

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
        
        _syncStatus.value = SyncStatus.Syncing(message = "Syncing transactions...")
        
        try {
            syncPreferences.setSyncInProgress(true)
            
            // 1. Sync Transactions
            val transactionResult = syncTransactions()
            results.add(transactionResult)
            
            _syncStatus.value = SyncStatus.Syncing(
                progress = 50,
                message = "Syncing cash records..."
            )
            
            // 2. Sync Cash Records
            val cashRecordResult = syncCashRecords()
            results.add(cashRecordResult)
            
            // 3. Update timestamps
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
        val unsyncedTransactions = transactionDao.getUnsyncedTransactions().size
        val unsyncedCashRecords = cashRecordDao.getUnsynced().size
        unsyncedTransactions + unsyncedCashRecords
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

    // ==================== Private Sync Methods ====================

    private suspend fun syncTransactions(): SyncResult {
        val unsyncedTransactions = transactionDao.getUnsyncedTransactions()
        
        if (unsyncedTransactions.isEmpty()) {
            return SyncResult(type = SyncType.TRANSACTIONS)
        }

        var uploaded = 0
        var failed = 0

        for (transaction in unsyncedTransactions) {
            val items = transactionDao.getTransactionItems(transaction.id)
            
            transactionRemoteDataSource.uploadTransaction(transaction, items).fold(
                onSuccess = {
                    transactionDao.markAsSynced(transaction.id)
                    uploaded++
                },
                onFailure = { failed++ }
            )
        }

        if (uploaded > 0) {
            syncPreferences.updateLastTransactionsSyncTimestamp()
        }

        return SyncResult(
            type = SyncType.TRANSACTIONS,
            uploaded = uploaded,
            failed = failed
        )
    }

    private suspend fun syncCashRecords(): SyncResult {
        val unsyncedRecords = cashRecordDao.getUnsynced()
        
        if (unsyncedRecords.isEmpty()) {
            return SyncResult(type = SyncType.CASH_RECORDS)
        }

        return cashRecordRemoteDataSource.uploadCashRecords(unsyncedRecords).fold(
            onSuccess = { uploadedCount ->
                unsyncedRecords.forEach { record ->
                    cashRecordDao.markAsSynced(record.id)
                }
                syncPreferences.updateLastCashRecordsSyncTimestamp()
                SyncResult(
                    type = SyncType.CASH_RECORDS,
                    uploaded = uploadedCount
                )
            },
            onFailure = {
                SyncResult(
                    type = SyncType.CASH_RECORDS,
                    failed = unsyncedRecords.size
                )
            }
        )
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
