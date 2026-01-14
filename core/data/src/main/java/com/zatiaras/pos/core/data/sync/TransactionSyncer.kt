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

        for (transaction in unsyncedTransactions) {
            val items = transactionDao.getTransactionItems(transaction.id)
            
            transactionRemoteDataSource.uploadTransaction(transaction, items).fold(
                onSuccess = {
                    transactionDao.markAsSynced(transaction.id)
                    uploaded++
                    Timber.d("TransactionSyncer: Synced ${transaction.transactionNumber}")
                },
                onFailure = { error ->
                    failed++
                    Timber.e(error, "TransactionSyncer: Failed to sync ${transaction.id}")
                }
            )
        }

        if (uploaded > 0) {
            syncPreferences.updateLastTransactionsSyncTimestamp()
        }

        Timber.d("TransactionSyncer: Completed - uploaded=$uploaded, failed=$failed")
        
        return SyncResult(
            type = SyncType.TRANSACTIONS,
            uploaded = uploaded,
            failed = failed
        )
    }

    override suspend fun getPendingCount(): Int {
        return transactionDao.getUnsyncedTransactions().size
    }
}
