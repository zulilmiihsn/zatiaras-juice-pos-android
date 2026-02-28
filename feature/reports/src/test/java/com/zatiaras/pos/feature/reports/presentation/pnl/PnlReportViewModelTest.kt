package com.zatiaras.pos.feature.reports.presentation.pnl

import com.zatiaras.pos.core.data.local.entity.AppSettingsEntity
import com.zatiaras.pos.core.data.repository.AppSettingsRepository
import com.zatiaras.pos.feature.printer.data.preferences.PrinterPreferences
import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport
import com.zatiaras.pos.feature.reports.domain.model.ReportPeriod
import com.zatiaras.pos.feature.reports.domain.usecase.GenerateProfitLossReportUseCase
import com.zatiaras.pos.feature.reports.export.CsvExportService
import com.zatiaras.pos.feature.reports.export.PdfExportService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PnlReportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var generateProfitLossReportUseCase: GenerateProfitLossReportUseCase
    private lateinit var pdfExportService: PdfExportService
    private lateinit var csvExportService: CsvExportService
    private lateinit var appSettingsRepository: AppSettingsRepository
    private lateinit var printerPreferences: PrinterPreferences

    private val testReport = ProfitLossReport(
        periodStart = System.currentTimeMillis() - 86_400_000,
        periodEnd = System.currentTimeMillis(),
        operatingRevenue = 1_000_000,
        otherRevenue = 50_000,
        grossRevenue = 1_050_000,
        operatingExpenses = 300_000,
        otherExpenses = 50_000,
        totalExpenses = 350_000,
        grossProfit = 700_000,
        tax = 3_500,
        netProfit = 696_500,
        transactionCount = 25
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        generateProfitLossReportUseCase = mockk()
        pdfExportService = mockk(relaxed = true)
        csvExportService = mockk(relaxed = true)
        appSettingsRepository = mockk()
        printerPreferences = mockk(relaxed = true)

        coEvery { appSettingsRepository.observeSettings() } returns flowOf(
            AppSettingsEntity(defaultTaxPercentage = 0.5)
        )
        coEvery { generateProfitLossReportUseCase.invoke(any(), any(), any()) } returns Result.success(testReport)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PnlReportViewModel {
        return PnlReportViewModel(
            generateProfitLossReportUseCase = generateProfitLossReportUseCase,
            pdfExportService = pdfExportService,
            csvExportService = csvExportService,
            appSettingsRepository = appSettingsRepository,
            printerPreferences = printerPreferences
        )
    }

    @Test
    fun `init should load report and stop loading`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.report)
        assertEquals(ReportPeriod.THIS_MONTH, state.selectedPeriod)
    }

    @Test
    fun `selectPeriod TODAY updates selectedPeriod`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectPeriod(ReportPeriod.TODAY)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ReportPeriod.TODAY, state.selectedPeriod)
        assertFalse(state.isLoading)
    }

    @Test
    fun `showDatePicker and hideDatePicker update flag`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showDatePicker()
        assertTrue(viewModel.uiState.value.showDatePicker)

        viewModel.hideDatePicker()
        assertFalse(viewModel.uiState.value.showDatePicker)
    }
}
