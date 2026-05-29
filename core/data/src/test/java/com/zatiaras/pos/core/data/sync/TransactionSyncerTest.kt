package com.zatiaras.pos.core.data.sync

import com.zatiaras.pos.core.data.local.SyncPreferences
import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.core.data.local.entity.TransactionEntity
import com.zatiaras.pos.core.data.local.entity.TransactionItemEntity
import com.zatiaras.pos.core.data.remote.TransactionRemoteDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionSyncerTest {

    private lateinit var transactionDao: TransactionDao
    private lateinit var transactionRemoteDataSource: TransactionRemoteDataSource
    private lateinit var syncPreferences: SyncPreferences
    private lateinit var syncer: TransactionSyncer

    @Before
    fun setup() {
        transactionDao = mockk(relaxed = true)
        transactionRemoteDataSource = mockk(relaxed = true)
        syncPreferences = mockk(relaxed = true)
        syncer = TransactionSyncer(transactionDao, transactionRemoteDataSource, syncPreferences)
    }

    @Test
    fun `sync pulls remote transactions when no local transactions are pending`() = runTest {
        val lastSyncTimestamp = 1_700_000_000_000L
        val remoteTransaction = transaction(id = "remote-1", updatedAt = lastSyncTimestamp + 1_000L)
        val remoteItems = listOf(transactionItem(transactionId = remoteTransaction.id))

        coEvery { transactionDao.getUnsyncedTransactions() } returns emptyList()
        coEvery { syncPreferences.getLastTransactionsSyncTimestamp() } returns lastSyncTimestamp
        coEvery {
            transactionRemoteDataSource.fetchTransactionsExtended(
                lastSyncTimestamp = lastSyncTimestamp,
                page = 0,
                pageSize = 50,
            )
        } returns Result.success(listOf(remoteTransaction to remoteItems))
        coEvery { transactionDao.getTransactionById(remoteTransaction.id) } returns null

        val result = syncer.sync()

        assertTrue(result.isSuccess)
        assertEquals(0, result.uploaded)
        assertEquals(1, result.downloaded)
        coVerify(exactly = 1) {
            transactionRemoteDataSource.fetchTransactionsExtended(
                lastSyncTimestamp = lastSyncTimestamp,
                page = 0,
                pageSize = 50,
            )
        }
        coVerify(exactly = 1) {
            transactionDao.insertTransactionWithItems(remoteTransaction, remoteItems)
        }
        coVerify(exactly = 1) { syncPreferences.updateLastTransactionsSyncTimestamp() }
    }

    private fun transaction(
        id: String,
        updatedAt: Long,
    ): TransactionEntity = TransactionEntity(
        id = id,
        transactionNumber = "TRX-20260529-0001",
        subtotal = 10_000L,
        grandTotal = 10_000L,
        paymentMethod = "CASH",
        amountPaid = 10_000L,
        changeAmount = 0L,
        createdAt = updatedAt,
        updatedAt = updatedAt,
        isSynced = true,
    )

    private fun transactionItem(transactionId: String): TransactionItemEntity = TransactionItemEntity(
        id = "item-1",
        transactionId = transactionId,
        productId = "product-1",
        productName = "Kopi",
        productPrice = 10_000L,
        quantity = 1,
        subtotal = 10_000L,
    )
}
