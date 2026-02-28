package com.zatiaras.pos.feature.reports.domain.usecase

import com.zatiaras.pos.core.domain.model.StoreSession
import com.zatiaras.pos.core.domain.repository.StoreSessionRepository
import com.zatiaras.pos.core.domain.util.DateUtils
import com.zatiaras.pos.feature.reports.domain.model.AiChatMessage
import com.zatiaras.pos.feature.reports.domain.model.DailyRevenue
import com.zatiaras.pos.feature.reports.domain.model.DashboardStats
import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport
import com.zatiaras.pos.feature.reports.domain.model.RawProfitLossData
import com.zatiaras.pos.feature.reports.domain.model.TopProduct
import com.zatiaras.pos.feature.reports.domain.model.TransactionSummaryItem
import com.zatiaras.pos.feature.reports.domain.repository.AiChatRepository
import com.zatiaras.pos.feature.reports.domain.repository.ReportRepository
import com.zatiaras.pos.feature.reports.presentation.chat.ChatMessage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnalyzeReportUseCaseTest {

    private lateinit var reportRepository: ReportRepository
    private lateinit var storeSessionRepository: StoreSessionRepository
    private lateinit var aiChatRepository: AiChatRepository

    private lateinit var generateProfitLossReportUseCase: GenerateProfitLossReportUseCase
    private lateinit var useCase: AnalyzeReportUseCase

    @Before
    fun setup() {
        reportRepository = mockk()
        storeSessionRepository = mockk()
        aiChatRepository = mockk()

        generateProfitLossReportUseCase = GenerateProfitLossReportUseCase(reportRepository)
        useCase = AnalyzeReportUseCase(
            generateProfitLossReportUseCase = generateProfitLossReportUseCase,
            reportRepository = reportRepository,
            storeSessionRepository = storeSessionRepository,
            aiChatRepository = aiChatRepository
        )
    }

    @Test
    fun `detectDateRange for hari ini uses today range`() {
        val actual = useCase.detectDateRange("tolong analisa hari ini")
        val expected = DateUtils.getTodayRange()

        assertEquals(expected.first, actual.first)
        assertEquals(expected.second, actual.second)
    }

    @Test
    fun `detectDateRange defaults to this month when phrase unknown`() {
        val actual = useCase.detectDateRange("analisa kondisi usaha dong")
        val expected = DateUtils.getThisMonthRange()

        assertEquals(expected.first, actual.first)
        assertEquals(expected.second, actual.second)
    }

    @Test
    fun `invoke should include system history and image in ai messages`() = runTest {
        val capturedMessages = mutableListOf<AiChatMessage>()

        coEvery { reportRepository.getRawProfitLossData(any(), any()) } returns Result.success(
            RawProfitLossData(
                posGrossRevenue = 1_000_000,
                posTotalDiscount = 0,
                posTransactions = 3,
                productSales = emptyList(),
                manualRecords = emptyList()
            )
        )
        coEvery { reportRepository.getTransactionsForAnalysis(any(), any()) } returns Result.success(
            listOf(TransactionSummaryItem(createdAt = System.currentTimeMillis(), paymentMethod = "CASH", grandTotal = 150_000))
        )
        coEvery { reportRepository.getCashRecordsForAnalysis(any(), any()) } returns Result.success(emptyList())
        coEvery { reportRepository.getDashboardStats() } returns Result.success(
            DashboardStats(
                todayRevenue = 500_000,
                todayTransactions = 5,
                todayItemsSold = 12,
                weeklyRevenue = 3_000_000,
                monthlyRevenue = 12_000_000,
                revenueGrowthPercent = 10.0
            )
        )
        coEvery { reportRepository.getDailyRevenueHistory(any()) } returns Result.success(
            listOf(DailyRevenue(date = System.currentTimeMillis(), revenue = 500_000, transactionCount = 5))
        )
        coEvery { reportRepository.getTopSellingProducts(any(), any(), any()) } returns Result.success(
            listOf(TopProduct(productId = "p1", productName = "Nasi Goreng", quantitySold = 5, totalRevenue = 250_000))
        )
        coEvery { storeSessionRepository.getActiveSessionOneShot() } returns StoreSession(
            id = "s1",
            openingCash = 100_000,
            openingTime = System.currentTimeMillis() - 3_600_000,
            closingTime = null,
            isActive = true,
            branchId = "b1"
        )
        coEvery { storeSessionRepository.getLastSessions(any()) } returns flowOf(emptyList())
        coEvery { aiChatRepository.sendChatMessage(any(), any(), any()) } answers {
            capturedMessages.clear()
            capturedMessages.addAll(firstArg())
            Result.success("Analisis berhasil")
        }

        val history = listOf(
            ChatMessage(content = "Halo", isUser = true),
            ChatMessage(content = "Hai, ada yang bisa dibantu", isUser = false)
        )

        val result = useCase(
            query = "analisa laba bulan ini",
            history = history,
            imageBase64 = "base64-image"
        )

        assertTrue(result.isSuccess)
        assertEquals("system", capturedMessages.first().role)
        assertEquals(4, capturedMessages.size)
        assertEquals("user", capturedMessages.last().role)
        assertEquals("analisa laba bulan ini", capturedMessages.last().content)
        assertEquals("base64-image", capturedMessages.last().imageBase64)

        coVerify(exactly = 1) { aiChatRepository.sendChatMessage(any(), any(), any()) }
    }

    @Test
    fun `invoke should fail when report generation fails`() = runTest {
        coEvery { reportRepository.getRawProfitLossData(any(), any()) } returns Result.failure(
            IllegalStateException("Gagal ambil data mentah")
        )

        val result = useCase(
            query = "cek profit",
            history = emptyList(),
            imageBase64 = null
        )

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { aiChatRepository.sendChatMessage(any(), any(), any()) }
    }
}
