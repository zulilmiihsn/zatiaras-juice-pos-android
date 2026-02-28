package com.zatiaras.pos.core.data.sync

import com.zatiaras.pos.core.data.local.SyncPreferences
import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.core.data.remote.TransactionRemoteDataSource
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Syncer implementation for Transaction entities.
 * 
 * Handles:
 * - Fetching unsynced transactions from local DB
 * - Uploading to Supabase
 * - Marking as synced on success
 * - Updating sync timestamps
 */
@Singleton
class TransactionSyncer @Inject constructor(
    private val transactionDao: TransactionDao,
    private val transactionRemoteDataSource: TransactionRemoteDataSource,
    private val syncPreferences: SyncPreferences
) : EntitySyncer {

    override val syncType: SyncType = SyncType.TRANSACTIONS

    override suspend fun sync(): SyncResult {
        val unsyncedTransactions = transactionDao.getUnsyncedTransactions()
        
        if (unsyncedTransactions.isEmpty()) {
            Timber.d("TransactionSyncer: No unsynced transactions")
            return SyncResult(type = SyncType.TRANSACTIONS)
        }

        Timber.d("TransactionSyncer: Found ${unsyncedTransactions.size} unsynced transactions")
        
        var uploaded = 0
        var failed = 0

        // 1. Prepare batch data using single batch query (avoids N+1)
        val allTransactionIds = unsyncedTransactions.map { it.id }
        val allItems = transactionDao.getTransactionItemsByTransactionIds(allTransactionIds)
        val itemsByTransactionId = allItems.groupBy { it.transactionId }
        
        val batchData = unsyncedTransactions.map { transaction ->
            Pair(transaction, itemsByTransactionId[transaction.id] ?: emptyList())
        }

        // 2. Perform batch upload
        transactionRemoteDataSource.uploadTransactions(batchData).fold(
            onSuccess = { count ->
                // 3. Mark all as synced individually (or batch if DAO supported it)
                unsyncedTransactions.forEach { transaction ->
                    transactionDao.markAsSynced(transaction.id)
                }
                uploaded = count
                Timber.d("TransactionSyncer: Batch synced $uploaded transactions")
            },
            onFailure = { error ->
                failed = unsyncedTransactions.size
                Timber.e(error, "TransactionSyncer: Batch sync failed")
            }
        )

        if (uploaded > 0) {
            syncPreferences.updateLastTransactionsSyncTimestamp()
        }

        // 2. PULL: Fetch remote updates
        val lastSyncTimestamp = syncPreferences.getLastTransactionsSyncTimestamp()
        var page = 0
        val pageSize = 50
        var hasMore = true
        var downloaded = 0

        while (hasMore) {
            val pullResult = transactionRemoteDataSource.fetchTransactionsExtended(
                lastSyncTimestamp = lastSyncTimestamp,
                page = page,
                pageSize = pageSize
            )

            pullResult.fold(
                onSuccess = { remoteData ->
                    if (remoteData.isEmpty()) {
                        hasMore = false
                    } else {
                        // SMART MERGE: Insert or Update local
                        for ((remoteTransaction, remoteItems) in remoteData) {
                            val localTransaction = transactionDao.getTransactionById(remoteTransaction.id)
                            
                            if (localTransaction == null) {
                                // New transaction -> Insert with items
                                transactionDao.insertTransactionWithItems(remoteTransaction, remoteItems)
                                downloaded++
                            } else if (remoteTransaction.updatedAt > localTransaction.updatedAt) {
                                // Remote is newer -> Overwrite
                                transactionDao.insertTransactionWithItems(remoteTransaction, remoteItems)
                                downloaded++
                            }
                        }
                        
                        if (remoteData.size < pageSize) {
                            hasMore = false
                        } else {
                            page++
                        }
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "TransactionSyncer: Failed to pull page $page")
                    failed += pageSize // Approximation
                    hasMore = false
                }
            )
        }

        // Always update sync timestamp on successful completion
        syncPreferences.updateLastTransactionsSyncTimestamp()
        
        if (downloaded > 0) {
            Timber.d("TransactionSyncer: Downloaded $downloaded new/updated transactions")
        }

        Timber.d("TransactionSyncer: Completed - uploaded=$uploaded, downloaded=$downloaded, failed=$failed")
        
        return SyncResult(
            type = SyncType.TRANSACTIONS,
            uploaded = uploaded,
            downloaded = downloaded,
            failed = failed
        )
    }

    override suspend fun getPendingCount(): Int {
        return transactionDao.getUnsyncedCount()
    }
}
