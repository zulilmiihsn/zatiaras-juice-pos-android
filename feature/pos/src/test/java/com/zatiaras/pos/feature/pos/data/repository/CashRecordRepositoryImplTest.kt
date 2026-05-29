package com.zatiaras.pos.feature.pos.data.repository

import com.zatiaras.pos.core.data.local.dao.CashRecordDao
import com.zatiaras.pos.core.data.sync.CashRecordSyncer
import com.zatiaras.pos.core.data.sync.SyncResult
import com.zatiaras.pos.core.data.sync.SyncType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CashRecordRepositoryImplTest {

    private lateinit var cashRecordDao: CashRecordDao
    private lateinit var cashRecordSyncer: CashRecordSyncer
    private lateinit var repository: CashRecordRepositoryImpl

    @Before
    fun setup() {
        cashRecordDao = mockk(relaxed = true)
        cashRecordSyncer = mockk(relaxed = true)
        repository = CashRecordRepositoryImpl(cashRecordDao, cashRecordSyncer)
    }

    @Test
    fun `syncToRemote delegates to CashRecordSyncer`() = runTest {
        coEvery { cashRecordSyncer.sync() } returns SyncResult(
            type = SyncType.CASH_RECORDS,
            uploaded = 2,
            downloaded = 1,
            failed = 0,
        )

        val result = repository.syncToRemote()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { cashRecordSyncer.sync() }
    }

    @Test
    fun `syncToRemote returns failure when CashRecordSyncer reports failures`() = runTest {
        coEvery { cashRecordSyncer.sync() } returns SyncResult(
            type = SyncType.CASH_RECORDS,
            uploaded = 0,
            downloaded = 0,
            failed = 1,
        )

        val result = repository.syncToRemote()

        assertTrue(result.isFailure)
        coVerify(exactly = 1) { cashRecordSyncer.sync() }
    }
}
