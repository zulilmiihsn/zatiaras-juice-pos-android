package com.zatiaras.pos.core.data.sync

import com.zatiaras.pos.core.data.local.SyncPreferences
import com.zatiaras.pos.core.data.local.dao.CashRecordDao
import com.zatiaras.pos.core.data.remote.CashRecordRemoteDataSource
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Syncer implementation for CashRecord entities.
 * 
 * Handles:
 * - Fetching unsynced cash records from local DB
 * - Uploading to Supabase in batch
 * - Marking as synced on success
 * - Updating sync timestamps
 */
@Singleton
class CashRecordSyncer @Inject constructor(
    private val cashRecordDao: CashRecordDao,
    private val cashRecordRemoteDataSource: CashRecordRemoteDataSource,
    private val syncPreferences: SyncPreferences
) : EntitySyncer {

    override val syncType: SyncType = SyncType.CASH_RECORDS

    override suspend fun sync(): SyncResult {
        val unsyncedRecords = cashRecordDao.getUnsynced()
        
        if (unsyncedRecords.isEmpty()) {
            Timber.d("CashRecordSyncer: No unsynced cash records")
            return SyncResult(type = SyncType.CASH_RECORDS)
        }

        Timber.d("CashRecordSyncer: Found ${unsyncedRecords.size} unsynced cash records")

        return cashRecordRemoteDataSource.uploadCashRecords(unsyncedRecords).fold(
            onSuccess = { uploadedCount ->
                unsyncedRecords.forEach { record ->
                    cashRecordDao.markAsSynced(record.id)
                }
                syncPreferences.updateLastCashRecordsSyncTimestamp()
                
                Timber.d("CashRecordSyncer: Synced $uploadedCount cash records")
                
                SyncResult(
                    type = SyncType.CASH_RECORDS,
                    uploaded = uploadedCount
                )
            },
            onFailure = { error ->
                Timber.e(error, "CashRecordSyncer: Failed to sync cash records")
                
                SyncResult(
                    type = SyncType.CASH_RECORDS,
                    failed = unsyncedRecords.size
                )
            }
        )
    }

    override suspend fun getPendingCount(): Int {
        return cashRecordDao.getUnsynced().size
    }
}
