package com.zatiaras.pos.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker for background sync operations.
 * 
 * Delegates all sync logic to [SyncManager] to maintain a single source of truth
 * for sync orchestration (DRY principle).
 * 
 * Uses "Last Write Wins" conflict resolution based on updatedAt timestamp.
 * Runs with network constraint and exponential backoff on failure.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME_PERIODIC = "sync_periodic"
        const val WORK_NAME_ONE_TIME = "sync_one_time"
        private const val SYNC_INTERVAL_MINUTES = 15L
        private const val TAG = "SyncWorker"

        /**
         * Schedule periodic sync with network constraint.
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

            Timber.d("Scheduled periodic sync every $SYNC_INTERVAL_MINUTES minutes")
        }

        /**
         * Trigger immediate one-time sync.
         */
        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONE_TIME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )

            Timber.d("Triggered immediate sync")
        }

        /**
         * Cancel all sync work.
         */
        fun cancelAllSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_ONE_TIME)
            Timber.d("Cancelled all sync work")
        }
    }

    override suspend fun doWork(): Result {
        Timber.d("SyncWorker started")
        
        return try {
            // Delegate to SyncManager (single source of truth for sync logic)
            val results = syncManager.syncNow()
            
            val totalUploaded = results.sumOf { it.totalSynced }
            val hasErrors = results.any { it.failed > 0 }

            if (hasErrors) {
                Timber.w("Sync completed with errors. Uploaded: $totalUploaded")
                Result.retry()
            } else {
                Timber.d("Sync completed successfully. Uploaded: $totalUploaded")
                Result.success()
            }
        } catch (e: Exception) {
            Timber.e(e, "Sync failed")
            Result.retry()
        }
    }
}
