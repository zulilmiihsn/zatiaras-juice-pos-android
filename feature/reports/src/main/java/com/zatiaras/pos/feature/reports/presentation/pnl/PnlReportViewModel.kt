package com.zatiaras.pos.feature.reports.presentation.pnl

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.feature.reports.domain.model.ReportPeriod
import com.zatiaras.pos.feature.reports.domain.repository.ReportRepository
import com.zatiaras.pos.feature.reports.export.CsvExportService
import com.zatiaras.pos.feature.reports.export.PdfExportService
import com.zatiaras.pos.feature.reports.presentation.components.toDisplayName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PnlReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val pdfExportService: PdfExportService,
    private val csvExportService: CsvExportService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PnlReportUiState())
    val uiState: StateFlow<PnlReportUiState> = _uiState.asStateFlow()

    // One-time events for export
    private val _exportEvent = MutableSharedFlow<ExportEvent>()
    val exportEvent: SharedFlow<ExportEvent> = _exportEvent.asSharedFlow()

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

    init {
        loadReport()
    }

    fun selectPeriod(period: ReportPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        if (period != ReportPeriod.CUSTOM) {
            loadReport()
        }
    }

    fun showDatePicker(isStartDate: Boolean) {
        _uiState.update { 
            it.copy(
                showDatePicker = true,
                isSelectingStartDate = isStartDate
            )
        }
    }

    fun hideDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    fun setCustomDate(timestamp: Long) {
        _uiState.update { state ->
            if (state.isSelectingStartDate) {
                state.copy(
                    customStartDate = timestamp,
                    showDatePicker = false
                )
            } else {
                state.copy(
                    customEndDate = timestamp,
                    showDatePicker = false
                )
            }
        }
        
        // Load report if both dates are set
        val state = _uiState.value
        if (state.customStartDate != null && state.customEndDate != null) {
            loadReport()
        }
    }

    fun loadReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val (startDate, endDate) = calculateDateRange()
                
                val report = reportRepository.getProfitLossReport(startDate, endDate)
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        report = report
                    )
                }
                
                Timber.d("P&L Report loaded: ${report.transactionCount} transactions")
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
                    pdfExportService.exportToPdf(context, report, periodName)
                }
                
                result.fold(
                    onSuccess = { uri ->
                        // Get the file name from the URI
                        val fileName = "Laporan_Laba_Rugi_${System.currentTimeMillis()}.pdf"
                        _exportEvent.emit(ExportEvent.SavedToDownloads(fileName, uri.toString()))
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
                        _exportEvent.emit(ExportEvent.SavedToDownloads(fileName, uri.toString()))
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
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        return when (_uiState.value.selectedPeriod) {
            ReportPeriod.TODAY -> {
                val start = getStartOfDay(now)
                val end = getEndOfDay(now)
                start to end
            }
            ReportPeriod.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val start = getStartOfDay(calendar.timeInMillis)
                val end = getEndOfDay(now)
                start to end
            }
            ReportPeriod.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = getStartOfDay(calendar.timeInMillis)
                val end = getEndOfDay(now)
                start to end
            }
            ReportPeriod.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                val start = getStartOfDay(calendar.timeInMillis)
                val end = getEndOfDay(now)
                start to end
            }
            ReportPeriod.LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -29)
                val start = getStartOfDay(calendar.timeInMillis)
                val end = getEndOfDay(now)
                start to end
            }
            ReportPeriod.CUSTOM -> {
                val start = _uiState.value.customStartDate ?: getStartOfDay(now)
                val end = _uiState.value.customEndDate ?: getEndOfDay(now)
                getStartOfDay(start) to getEndOfDay(end)
            }
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getEndOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
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
