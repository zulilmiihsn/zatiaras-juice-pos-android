package com.zatiaras.pos.feature.reports.presentation.pnl

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.feature.reports.domain.model.ReportPeriod
import com.zatiaras.pos.core.domain.util.LocaleUtils
import com.zatiaras.pos.feature.reports.domain.usecase.GenerateProfitLossReportUseCase
import com.zatiaras.pos.core.data.repository.AppSettingsRepository
import com.zatiaras.pos.feature.printer.data.preferences.PrinterPreferences
import com.zatiaras.pos.feature.reports.export.CsvExportService
import com.zatiaras.pos.feature.reports.export.PdfExportService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import com.zatiaras.pos.core.domain.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PnlReportViewModel @Inject constructor(
    private val generateProfitLossReportUseCase: GenerateProfitLossReportUseCase,
    private val pdfExportService: PdfExportService,
    private val csvExportService: CsvExportService,
    private val appSettingsRepository: AppSettingsRepository,
    private val printerPreferences: PrinterPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(PnlReportUiState())
    val uiState: StateFlow<PnlReportUiState> = _uiState.asStateFlow()

    // One-time events for export
    private val _exportEvent = MutableSharedFlow<ExportEvent>()
    val exportEvent: SharedFlow<ExportEvent> = _exportEvent.asSharedFlow()

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", LocaleUtils.LOCALE_ID)

    init {
        // Initialize dates for default period
        initializeDatesForDefaultPeriod()
        loadTaxPercentageAndReport()
    }

    /**
     * Observe settings from repository and load the report.
     */
    private fun loadTaxPercentageAndReport() {
        viewModelScope.launch {
            appSettingsRepository.observeSettings().collectLatest { settings ->
                val taxPercentage = settings?.defaultTaxPercentage ?: 0.5
                _uiState.update { it.copy(taxPercentage = taxPercentage) }
                loadReport()
            }
        }
    }
    
    /**
     * Initialize custom date fields based on the default period (THIS_MONTH).
     */
    private fun initializeDatesForDefaultPeriod() {
        val (startDate, endDate) = calculateDateRangeForPeriod(_uiState.value.selectedPeriod)
        _uiState.update {
            it.copy(
                customStartDate = startDate,
                customEndDate = endDate
            )
        }
    }

    fun selectPeriod(period: ReportPeriod) {
        // Calculate and populate dates for the selected period
        val (startDate, endDate) = calculateDateRangeForPeriod(period)
        
        _uiState.update { 
            it.copy(
                selectedPeriod = period,
                customStartDate = startDate,
                customEndDate = endDate
            ) 
        }
        
        if (period != ReportPeriod.CUSTOM) {
            loadReport()
        }
    }
    
    /**
     * Calculate date range for a given period.
     * Used to populate the date fields when a quick period is selected.
     */
    private fun calculateDateRangeForPeriod(period: ReportPeriod): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        
        return when (period) {
            ReportPeriod.TODAY -> {
                DateUtils.getStartOfDay(now) to DateUtils.getEndOfDay(now)
            }
            ReportPeriod.YESTERDAY -> {
                DateUtils.getYesterdayRange()
            }
            ReportPeriod.THIS_WEEK -> {
                DateUtils.getThisWeekRange()
            }
            ReportPeriod.THIS_MONTH -> {
                DateUtils.getThisMonthRange()
            }
            ReportPeriod.LAST_7_DAYS -> {
                DateUtils.getLastNDaysRange(7)
            }
            ReportPeriod.LAST_30_DAYS -> {
                DateUtils.getLastNDaysRange(30)
            }
            ReportPeriod.CUSTOM -> {
                val start = _uiState.value.customStartDate ?: DateUtils.getStartOfDay(now)
                val end = _uiState.value.customEndDate ?: DateUtils.getEndOfDay(now)
                start to end
            }
        }
    }

    fun showDatePicker() {
        _uiState.update { 
            it.copy(showDatePicker = true)
        }
    }

    fun hideDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    fun setCustomRange(start: Long, end: Long) {
        _uiState.update { 
            it.copy(
                customStartDate = start,
                customEndDate = end,
                showDatePicker = false,
                selectedPeriod = ReportPeriod.CUSTOM
            )
        }
        loadReport()
    }


    fun loadReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val (startDate, endDate) = calculateDateRange()
                val currentTaxPercentage = _uiState.value.taxPercentage
                
                val reportResult = generateProfitLossReportUseCase(startDate, endDate, currentTaxPercentage)
                val report = reportResult.getOrNull()
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        report = report
                    )
                }
                
                Timber.d("P&L Report loaded: ${report?.transactionCount ?: 0} transactions")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load P&L report")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Gagal memuat laporan"
                    )
                }
            }
        }
    }

    /**
     * Export report to PDF format.
     * File is saved to Downloads folder.
     */
    fun exportToPdf(context: Context) {
        val report = _uiState.value.report ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            
            try {
                val periodName = getPeriodDisplayName()
                
                val result = withContext(Dispatchers.IO) {
                    val storeLogoUri = printerPreferences.getStoreLogo()
                    pdfExportService.exportToPdf(context, report, periodName, storeLogoUri)
                }
                
                result.fold(
                    onSuccess = { uri ->
                        // Get the file name from the URI
                        val fileName = "Laporan_Laba_Rugi_${System.currentTimeMillis()}.pdf"
                        val intent = pdfExportService.createShareIntent(uri)
                        _exportEvent.emit(ExportEvent.ShareFile(intent, fileName))
                    },
                    onFailure = { error ->
                        _exportEvent.emit(ExportEvent.Error("Gagal export PDF: ${error.message}"))
                    }
                )
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }

    /**
     * Export report to CSV/Excel format.
     * File is saved to Downloads folder.
     */
    fun exportToCsv(context: Context) {
        val report = _uiState.value.report ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            
            try {
                val periodName = getPeriodDisplayName()
                
                val result = withContext(Dispatchers.IO) {
                    csvExportService.exportPnlToCsv(context, report, periodName)
                }
                
                result.fold(
                    onSuccess = { uri ->
                        val fileName = "Laporan_Laba_Rugi_${System.currentTimeMillis()}.csv"
                        val intent = csvExportService.createShareIntent(uri)
                        _exportEvent.emit(ExportEvent.ShareFile(intent, fileName))
                    },
                    onFailure = { error ->
                        _exportEvent.emit(ExportEvent.Error("Gagal export CSV: ${error.message}"))
                    }
                )
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }

    private fun getPeriodDisplayName(): String {
        val state = _uiState.value
        return when (state.selectedPeriod) {
            ReportPeriod.CUSTOM -> {
                val start = state.customStartDate?.let { dateFormat.format(Date(it)) } ?: "-"
                val end = state.customEndDate?.let { dateFormat.format(Date(it)) } ?: "-"
                "$start - $end"
            }
            else -> state.selectedPeriod.toDisplayName()
        }
    }

    private fun calculateDateRange(): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        
        return when (_uiState.value.selectedPeriod) {
            ReportPeriod.TODAY -> {
                DateUtils.getStartOfDay(now) to DateUtils.getEndOfDay(now)
            }
            ReportPeriod.YESTERDAY -> {
                DateUtils.getYesterdayRange()
            }
            ReportPeriod.THIS_WEEK -> {
                DateUtils.getThisWeekRange()
            }
            ReportPeriod.THIS_MONTH -> {
                DateUtils.getThisMonthRange()
            }
            ReportPeriod.LAST_7_DAYS -> {
                DateUtils.getLastNDaysRange(7)
            }
            ReportPeriod.LAST_30_DAYS -> {
                DateUtils.getLastNDaysRange(30)
            }
            ReportPeriod.CUSTOM -> {
                val start = _uiState.value.customStartDate ?: DateUtils.getStartOfDay(now)
                val end = _uiState.value.customEndDate ?: DateUtils.getEndOfDay(now)
                DateUtils.getStartOfDay(start) to DateUtils.getEndOfDay(end)
            }
        }
    }
}

/**
 * One-time events for export operations.
 */
sealed class ExportEvent {
    data class SavedToDownloads(val fileName: String, val filePath: String) : ExportEvent()
    data class ShareFile(val intent: Intent, val fileName: String) : ExportEvent()
    data class Error(val message: String) : ExportEvent()
}
