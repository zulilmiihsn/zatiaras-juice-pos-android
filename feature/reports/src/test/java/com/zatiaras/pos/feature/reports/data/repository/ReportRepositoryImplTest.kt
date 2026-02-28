package com.zatiaras.pos.feature.reports.data.repository

import com.zatiaras.pos.core.data.local.dao.CashRecordDao
import com.zatiaras.pos.core.data.local.dao.RevenueSummaryEntity
import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.core.data.local.entity.CashRecordEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReportRepositoryImplTest {

    private lateinit var transactionDao: TransactionDao
    private lateinit var cashRecordDao: CashRecordDao
    private lateinit var reportRepository: ReportRepositoryImpl

    @Before
    fun setup() {
        transactionDao = mockk(relaxed = true)
        cashRecordDao = mockk(relaxed = true)
        reportRepository = ReportRepositoryImpl(transactionDao, cashRecordDao)
    }

    @Test
    fun `getDashboardStats should return correct stats when data is available`() = runTest {
        val todayRevenue = 1500000L
        val todayTransactions = 25
        val todayItems = 75
        val weeklyRevenue = 10000000L
        val monthlyRevenue = 45000000L
        val prevWeekRevenue = 8000000L

        coEvery { transactionDao.getTotalRevenueForDay(any(), any()) } returns todayRevenue
        coEvery { transactionDao.getTransactionCountForDay(any(), any()) } returns todayTransactions
        coEvery { transactionDao.getTotalItemsSoldForDay(any(), any()) } returns todayItems

        coEvery { transactionDao.getRevenueSummary(any(), any()) } returnsMany listOf(
            RevenueSummaryEntity(totalRevenue = weeklyRevenue, grossRevenue = weeklyRevenue, totalDiscount = 0, totalTax = 0),
            RevenueSummaryEntity(totalRevenue = monthlyRevenue, grossRevenue = monthlyRevenue, totalDiscount = 0, totalTax = 0),
            RevenueSummaryEntity(totalRevenue = prevWeekRevenue, grossRevenue = prevWeekRevenue, totalDiscount = 0, totalTax = 0)
        )

        val result = reportRepository.getDashboardStats()
        assertTrue(result.isSuccess)
        val stats = result.getOrThrow()

        assertEquals(todayRevenue, stats.todayRevenue)
        assertEquals(todayTransactions, stats.todayTransactions)
        assertEquals(todayItems, stats.todayItemsSold)
        assertEquals(weeklyRevenue, stats.weeklyRevenue)
        assertEquals(monthlyRevenue, stats.monthlyRevenue)
        assertEquals(25.0, stats.revenueGrowthPercent, 0.01)
    }

    @Test
    fun `getDashboardStats should return failure when dao throws`() = runTest {
        coEvery { transactionDao.getTotalRevenueForDay(any(), any()) } throws RuntimeException("DB Error")

        val result = reportRepository.getDashboardStats()
        assertTrue(result.isFailure)
    }

    @Test
    fun `getCashRecordsForAnalysis should map dao entities`() = runTest {
        val startDate = 0L
        val endDate = 100L

        val manualRecords = listOf(
            CashRecordEntity(
                id = "1", type = "INCOME", amount = 100000,
                category = "Operasional", description = "Jualan",
                createdAt = 10, updatedAt = 10, isSynced = true
            ),
            CashRecordEntity(
                id = "2", type = "EXPENSE", amount = 20000,
                category = "Bahan", description = "Belanja",
                createdAt = 20, updatedAt = 20, isSynced = true
            )
        )

        coEvery { cashRecordDao.getRecordsListByDateRange(any(), any()) } returns manualRecords

        val result = reportRepository.getCashRecordsForAnalysis(startDate, endDate)

        assertTrue(result.isSuccess)
        val mapped = result.getOrThrow()
        assertEquals(2, mapped.size)
        assertEquals("INCOME", mapped[0].type)
        assertEquals(100000L, mapped[0].amount)
        assertEquals("Belanja", mapped[1].description)
    }
}
