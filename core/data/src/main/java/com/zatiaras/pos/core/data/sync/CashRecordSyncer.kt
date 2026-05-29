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
 * Handles two-way sync:
 * - PUSH: Upload unsynced local cash records to Supabase
 * - PULL: Fetch remote cash records and apply Last-Write-Wins
 */
@Singleton
class CashRecordSyncer @Inject constructor(
    private val cashRecordDao: CashRecordDao,
    private val cashRecordRemoteDataSource: CashRecordRemoteDataSource,
    private val syncPreferences: SyncPreferences,
) : EntitySyncer {

    override val syncType: SyncType = SyncType.CASH_RECORDS

    override suspend fun sync(): SyncResult {
        var uploaded = 0
        var downloaded = 0
        var failed = 0

        // Push local changes first so operator-entered cash records are not
        // overwritten by the following remote pull.
        val unsyncedRecords = cashRecordDao.getUnsynced()

        if (unsyncedRecords.isNotEmpty()) {
            Timber.d("CashRecordSyncer: Found ${unsyncedRecords.size} unsynced cash records")

            cashRecordRemoteDataSource.uploadCashRecords(unsyncedRecords).fold(
                onSuccess = { uploadedCount ->
                    unsyncedRecords.forEach { record ->
                        cashRecordDao.markAsSynced(record.id)
                    }
                    uploaded = uploadedCount
                    Timber.d("CashRecordSyncer: Pushed $uploaded cash records")
                },
                onFailure = { error ->
                    failed = unsyncedRecords.size
                    Timber.e(error, "CashRecordSyncer: Failed to push cash records")
                },
            )
        }

        // Pull remote records after push and apply Last-Write-Wins by updatedAt.
        val lastSyncTimestamp = syncPreferences.getLastCashRecordsSyncTimestamp()
        cashRecordRemoteDataSource.fetchCashRecords(lastSyncTimestamp).fold(
            onSuccess = { remoteRecords ->
                if (remoteRecords.isNotEmpty()) {
                    Timber.d("CashRecordSyncer: Fetched ${remoteRecords.size} cash records from remote")

                    val recordsToInsert = mutableListOf<com.zatiaras.pos.core.data.local.entity.CashRecordEntity>()

                    for (remoteRecord in remoteRecords) {
                        val localRecord = cashRecordDao.getById(remoteRecord.id)

                        if (localRecord == null) {
                            // New remote record; insert locally.
                            recordsToInsert.add(remoteRecord.copy(isSynced = true))
                        } else if (remoteRecord.updatedAt > localRecord.updatedAt) {
                            // Remote changed later; overwrite local.
                            recordsToInsert.add(remoteRecord.copy(isSynced = true))
                        } else {
                            // Local changed later; keep local.
                            Timber.d("CashRecordSyncer: Keeping local version for ${remoteRecord.id}")
                        }
                    }

                    if (recordsToInsert.isNotEmpty()) {
                        cashRecordDao.insertAll(recordsToInsert)
                        downloaded = recordsToInsert.size
                        Timber.d("CashRecordSyncer: Applied $downloaded updates from remote")
                    }

                    syncPreferences.updateLastCashRecordsSyncTimestamp()
                }
            },
            onFailure = { error ->
                failed++
                Timber.e(error, "CashRecordSyncer: Failed to pull cash records")
            },
        )

        Timber.d("CashRecordSyncer: Completed - uploaded=$uploaded, downloaded=$downloaded, failed=$failed")

        return SyncResult(
            type = SyncType.CASH_RECORDS,
            uploaded = uploaded,
            downloaded = downloaded,
            failed = failed,
        )
    }

    override suspend fun getPendingCount(): Int = cashRecordDao.getUnsyncedCount()
}
