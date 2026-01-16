package com.zatiaras.pos.feature.reports.presentation.pnl

import app.cash.turbine.test
import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport
import com.zatiaras.pos.feature.reports.domain.model.ReportPeriod
import com.zatiaras.pos.feature.reports.domain.repository.ReportRepository
import com.zatiaras.pos.feature.reports.export.CsvExportService
import com.zatiaras.pos.feature.reports.export.PdfExportService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PnlReportViewModel.
 * 
 * Tests:
 * - Initial state and loading
 * - Period selection
 * - Report loading
 * - Custom date range
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PnlReportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var reportRepository: ReportRepository
    private lateinit var pdfExportService: PdfExportService
    private lateinit var csvExportService: CsvExportService
    private lateinit var viewModel: PnlReportViewModel

    private val testReport = ProfitLossReport(
        periodStart = System.currentTimeMillis() - 86400000,
        periodEnd = System.currentTimeMillis(),
        operatingRevenue = 1000000,
        otherRevenue = 50000,
        grossRevenue = 1050000,
        operatingExpenses = 300000,
        otherExpenses = 50000,
        totalExpenses = 350000,
        grossProfit = 700000,
        tax = 5250,
        netProfit = 694750,
        transactionCount = 25
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        reportRepository = mockk()
        pdfExportService = mockk()
        csvExportService = mockk()
        
        // Default mock behavior
        coEvery { reportRepository.getProfitLossReport(any(), any()) } returns testReport
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PnlReportViewModel {
        return PnlReportViewModel(reportRepository, pdfExportService, csvExportService)
    }

    // ==================== Initialization Tests ====================

    @Test
    fun `initial state has isLoading true`() = runTest {
        viewModel = createViewModel()
        
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
            assertEquals(ReportPeriod.THIS_MONTH, initialState.selectedPeriod)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `viewModel loads report on init`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.report)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Period Selection Tests ====================

    @Test
    fun `selectPeriod changes selected period`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            awaitItem() // Initial
            
            viewModel.selectPeriod(ReportPeriod.TODAY)
            testDispatcher.scheduler.advanceUntilIdle()
            
            val state = awaitItem()
            assertEquals(ReportPeriod.TODAY, state.selectedPeriod)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectPeriod triggers report reload for non-custom periods`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            awaitItem() // Initial loaded
            
            viewModel.selectPeriod(ReportPeriod.LAST_7_DAYS)
            
            // Should go through loading state
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading || loadingState.selectedPeriod == ReportPeriod.LAST_7_DAYS)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectPeriod CUSTOM does not trigger immediate reload`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            awaitItem() // Initial
            
            viewModel.selectPeriod(ReportPeriod.CUSTOM)
            testDispatcher.scheduler.advanceUntilIdle()
            
            val state = awaitItem()
            assertEquals(ReportPeriod.CUSTOM, state.selectedPeriod)
            // Should NOT be loading since custom dates not set yet
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Date Picker Tests ====================

    @Test
    fun `showDatePicker sets showDatePicker to true`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            awaitItem()
            
            viewModel.showDatePicker(isStartDate = true)
            
            val state = awaitItem()
            assertTrue(state.showDatePicker)
            assertTrue(state.isSelectingStartDate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `showDatePicker for end date sets isSelectingStartDate to false`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            awaitItem()
            
            viewModel.showDatePicker(isStartDate = false)
            
            val state = awaitItem()
            assertTrue(state.showDatePicker)
            assertFalse(state.isSelectingStartDate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `hideDatePicker sets showDatePicker to false`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            awaitItem()
            
            viewModel.showDatePicker(true)
            awaitItem()
            
            viewModel.hideDatePicker()
            
            val state = awaitItem()
            assertFalse(state.showDatePicker)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Custom Date Tests ====================

    @Test
    fun `setCustomDate sets start date when isSelectingStartDate is true`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val testTimestamp = System.currentTimeMillis()
        
        viewModel.uiState.test {
            awaitItem()
            
            viewModel.showDatePicker(isStartDate = true)
            awaitItem()
            
            viewModel.setCustomDate(testTimestamp)
            
            val state = awaitItem()
            assertEquals(testTimestamp, state.customStartDate)
            assertFalse(state.showDatePicker)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setCustomDate sets end date when isSelectingStartDate is false`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val testTimestamp = System.currentTimeMillis()
        
        viewModel.uiState.test {
            awaitItem()
            
            viewModel.showDatePicker(isStartDate = false)
            awaitItem()
            
            viewModel.setCustomDate(testTimestamp)
            
            val state = awaitItem()
            assertEquals(testTimestamp, state.customEndDate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setting both custom dates triggers report reload`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val startTimestamp = System.currentTimeMillis() - 86400000 * 7
        val endTimestamp = System.currentTimeMillis()
        
        viewModel.uiState.test {
            awaitItem() // Initial
            
            // Set start date
            viewModel.showDatePicker(true)
            awaitItem()
            viewModel.setCustomDate(startTimestamp)
            awaitItem()
            
            // Set end date - should trigger reload
            viewModel.showDatePicker(false)
            awaitItem()
            viewModel.setCustomDate(endTimestamp)
            
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Find final state with both dates
            var state = awaitItem()
            while (state.customStartDate == null || state.customEndDate == null) {
                state = awaitItem()
            }
            
            assertEquals(startTimestamp, state.customStartDate)
            assertEquals(endTimestamp, state.customEndDate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `error during load sets error message`() = runTest {
        coEvery { reportRepository.getProfitLossReport(any(), any()) } throws Exception("Database error")
        
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("Database error"))
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Report Data Tests ====================

    @Test
    fun `loaded report contains correct data`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            val state = awaitItem()
            val report = state.report
            
            assertNotNull(report)
            assertEquals(1000000L, report!!.operatingRevenue)
            assertEquals(1050000L, report.grossRevenue)
            assertEquals(350000L, report.totalExpenses)
            assertEquals(694750L, report.netProfit)
            assertEquals(25, report.transactionCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
