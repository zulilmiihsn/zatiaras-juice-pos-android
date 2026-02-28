package com.zatiaras.pos.core.data.sync

import android.content.Context
import com.zatiaras.pos.core.data.local.SyncPreferences
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SyncManager.
 *
 * Tests:
 * - Initialization schedules periodic sync
 * - Manual sync triggers worker
 * - Sync status updates
 * - getPendingCount aggregates from all syncers
 * - forceFullSync resets timestamps
 * - cancelSync cancels work
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncManagerTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var syncPreferences: SyncPreferences
    private lateinit var cashRecordSyncer: CashRecordSyncer
    private lateinit var productSyncer: ProductSyncer
    private lateinit var categorySyncer: CategorySyncer
    private lateinit var transactionSyncer: TransactionSyncer
    private lateinit var syncManager: SyncManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true)
        syncPreferences = mockk(relaxed = true)
        cashRecordSyncer = mockk(relaxed = true)
        productSyncer = mockk(relaxed = true)
        categorySyncer = mockk(relaxed = true)
        transactionSyncer = mockk(relaxed = true)

        // Default mock behavior
        coEvery { syncPreferences.isSyncInProgress() } returns flowOf(false)
        coEvery { syncPreferences.getLastFullSyncTimestamp() } returns 0L
        coEvery { categorySyncer.syncType } returns SyncType.CATEGORIES
        coEvery { productSyncer.syncType } returns SyncType.PRODUCTS
        coEvery { transactionSyncer.syncType } returns SyncType.TRANSACTIONS
        coEvery { cashRecordSyncer.syncType } returns SyncType.CASH_RECORDS
        coEvery { categorySyncer.getPendingCount() } returns 2
        coEvery { productSyncer.getPendingCount() } returns 4
        coEvery { transactionSyncer.getPendingCount() } returns 5
        coEvery { cashRecordSyncer.getPendingCount() } returns 3
        coEvery { categorySyncer.sync() } returns SyncResult(
            type = SyncType.CATEGORIES, uploaded = 1, downloaded = 1, failed = 0
        )
        coEvery { productSyncer.sync() } returns SyncResult(
            type = SyncType.PRODUCTS, uploaded = 2, downloaded = 2, failed = 0
        )
        coEvery { transactionSyncer.sync() } returns SyncResult(
            type = SyncType.TRANSACTIONS, uploaded = 3, downloaded = 2, failed = 0
        )
        coEvery { cashRecordSyncer.sync() } returns SyncResult(
            type = SyncType.CASH_RECORDS, uploaded = 2, downloaded = 1, failed = 0
        )

        syncManager = SyncManager(
            context, syncPreferences,
            cashRecordSyncer, productSyncer, categorySyncer, transactionSyncer
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Initialization Tests ====================

    @Test
    fun `initialize schedules periodic sync`() {
        mockkObject(SyncWorker.Companion)
        every { SyncWorker.schedulePeriodicSync(any()) } just runs

        syncManager.initialize()

        verify { SyncWorker.schedulePeriodicSync(context) }

        unmockkObject(SyncWorker.Companion)
    }

    // ==================== Trigger Sync Tests ====================

    @Test
    fun `triggerSync triggers immediate sync worker`() {
        mockkObject(SyncWorker.Companion)
        every { SyncWorker.triggerImmediateSync(any()) } just runs

        syncManager.triggerSync()

        verify { SyncWorker.triggerImmediateSync(context) }

        unmockkObject(SyncWorker.Companion)
    }

    // ==================== getPendingCount Tests ====================

    @Test
    fun `getPendingCount returns sum of all syncers`() = runTest {
        val count = syncManager.getPendingCount()

        assertEquals(14, count) // 2 + 4 + 5 + 3
    }

    @Test
    fun `getPendingCount returns 0 when no pending items`() = runTest {
        coEvery { categorySyncer.getPendingCount() } returns 0
        coEvery { productSyncer.getPendingCount() } returns 0
        coEvery { transactionSyncer.getPendingCount() } returns 0
        coEvery { cashRecordSyncer.getPendingCount() } returns 0

        val count = syncManager.getPendingCount()

        assertEquals(0, count)
    }

    // ==================== syncNow Tests ====================

    @Test
    fun `syncNow sets sync in progress`() = runTest {
        syncManager.syncNow()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { syncPreferences.setSyncInProgress(true) }
        coVerify { syncPreferences.setSyncInProgress(false) }
    }

    @Test
    fun `syncNow calls all syncers`() = runTest {
        syncManager.syncNow()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { categorySyncer.sync() }
        coVerify { productSyncer.sync() }
        coVerify { transactionSyncer.sync() }
        coVerify { cashRecordSyncer.sync() }
    }

    @Test
    fun `syncNow updates last sync timestamp on success`() = runTest {
        syncManager.syncNow()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { syncPreferences.updateLastFullSyncTimestamp() }
    }

    @Test
    fun `syncNow returns results from all syncers`() = runTest {
        val results = syncManager.syncNow()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, results.size)
        // Order: category, product, transaction, cashRecord (as defined in SyncManager)
        assertEquals(2, results[0].totalSynced) // cat: 1+1
        assertEquals(4, results[1].totalSynced) // prod: 2+2
        assertEquals(5, results[2].totalSynced) // txn: 3+2
        assertEquals(3, results[3].totalSynced) // cash: 2+1
    }

    @Test
    fun `syncNow updates status to Success when no errors`() = runTest {
        syncManager.syncNow()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(syncManager.syncStatus.value is SyncStatus.Success)
    }

    @Test
    fun `syncNow updates status to Error when syncer has failures`() = runTest {
        coEvery { transactionSyncer.sync() } returns SyncResult(
            type = SyncType.TRANSACTIONS, uploaded = 1, downloaded = 2, failed = 2
        )

        syncManager.syncNow()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(syncManager.syncStatus.value is SyncStatus.Error)
    }

    // ==================== isSyncing Tests ====================

    @Test
    fun `isSyncing returns flow from preferences`() = runTest {
        coEvery { syncPreferences.isSyncInProgress() } returns flowOf(true)

        val isSyncing = syncManager.isSyncing().first()
        assertTrue(isSyncing)
    }

    // ==================== getLastSyncTimestamp Tests ====================

    @Test
    fun `getLastSyncTimestamp returns timestamp from preferences`() = runTest {
        val expectedTimestamp = 1234567890L
        coEvery { syncPreferences.getLastFullSyncTimestamp() } returns expectedTimestamp

        val timestamp = syncManager.getLastSyncTimestamp()

        assertEquals(expectedTimestamp, timestamp)
    }

    // ==================== forceFullSync Tests ====================

    @Test
    fun `forceFullSync resets timestamps before sync`() = runTest {
        syncManager.forceFullSync()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { syncPreferences.resetSyncTimestamps() }
        coVerify { categorySyncer.sync() }
        coVerify { productSyncer.sync() }
        coVerify { transactionSyncer.sync() }
        coVerify { cashRecordSyncer.sync() }
    }

    // ==================== cancelSync Tests ====================

    @Test
    fun `cancelSync cancels all sync work`() {
        mockkObject(SyncWorker.Companion)
        every { SyncWorker.cancelAllSync(any()) } just runs

        syncManager.cancelSync()

        verify { SyncWorker.cancelAllSync(context) }
        assertEquals(SyncStatus.Idle, syncManager.syncStatus.value)

        unmockkObject(SyncWorker.Companion)
    }

    // ==================== getSyncInfo Tests ====================

    @Test
    fun `getSyncInfo combines all sync info`() = runTest {
        coEvery { syncPreferences.isSyncInProgress() } returns flowOf(false)
        coEvery { syncPreferences.getLastFullSyncTimestamp() } returns 1234567890L

        val syncInfo = syncManager.getSyncInfo().first()
        assertFalse(syncInfo.isInProgress)
        assertEquals(1234567890L, syncInfo.lastSyncTimestamp)
        assertNotNull(syncInfo.status)
    }
}
