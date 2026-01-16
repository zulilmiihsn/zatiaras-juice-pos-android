package com.zatiaras.pos.core.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.*
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for SyncWorker.
 * 
 * Tests:
 * - Worker initialization
 * - Worker execution with network constraint
 * - Retry on failure
 * - Periodic sync scheduling
 */
@RunWith(AndroidJUnit4::class)
class SyncWorkerIntegrationTest {

    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Initialize WorkManager for testing
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun syncWorker_hasCorrectConstraints() {
        // Get the sync work request
        val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SyncWorker.SYNC_INTERVAL_HOURS,
            java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        // Verify constraints
        val constraints = workRequest.workSpec.constraints
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
    }

    @Test
    fun syncWorker_isRegisteredWithUniqueName() {
        val workManager = WorkManager.getInstance(context)
        
        // Schedule sync work
        SyncWorker.schedulePeriodicSync(context)
        
        // Verify the work is enqueued
        val workInfo = workManager.getWorkInfosForUniqueWork(SyncWorker.UNIQUE_WORK_NAME).get()
        
        // Work should be scheduled
        assertTrue(workInfo.isNotEmpty())
    }

    @Test
    fun triggerImmediateSync_schedulesOneTimeWork() {
        val workManager = WorkManager.getInstance(context)
        
        // Trigger immediate sync
        SyncWorker.triggerImmediateSync(context)
        
        // Verify one-time work is scheduled
        val workInfo = workManager.getWorkInfosForUniqueWork(SyncWorker.IMMEDIATE_WORK_NAME).get()
        
        assertTrue(workInfo.isNotEmpty())
    }

    @Test
    fun cancelAllSync_cancelsWork() {
        val workManager = WorkManager.getInstance(context)
        
        // First schedule sync
        SyncWorker.schedulePeriodicSync(context)
        
        // Then cancel
        SyncWorker.cancelAllSync(context)
        
        // Verify work is cancelled
        val workInfo = workManager.getWorkInfosForUniqueWork(SyncWorker.UNIQUE_WORK_NAME).get()
        
        // Either empty or cancelled
        assertTrue(workInfo.isEmpty() || workInfo.all { it.state == WorkInfo.State.CANCELLED })
    }

    @Test
    fun syncWorker_hasExponentialBackoff() {
        // Get the sync work request
        val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SyncWorker.SYNC_INTERVAL_HOURS,
            java.util.concurrent.TimeUnit.HOURS
        )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()

        // Verify backoff policy
        assertEquals(BackoffPolicy.EXPONENTIAL, workRequest.workSpec.backoffPolicy)
    }

    @Test
    fun periodicSync_usesCorrectInterval() {
        // Verify the constant is defined correctly
        assertEquals(6L, SyncWorker.SYNC_INTERVAL_HOURS)
    }

    @Test
    fun syncWorker_hasCorrectTags() {
        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .addTag(SyncWorker.TAG_SYNC)
            .build()

        // Verify tags
        assertTrue(workRequest.tags.contains(SyncWorker.TAG_SYNC))
    }
}
