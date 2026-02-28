package com.zatiaras.pos.feature.reports.presentation.pnl

import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport
import com.zatiaras.pos.feature.reports.domain.model.ReportPeriod

/**
 * UI State for Profit & Loss Report screen.
 */
data class PnlReportUiState(
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val selectedPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val report: ProfitLossReport? = null,
    val error: String? = null,
    val showDatePicker: Boolean = false,
    val taxPercentage: Double = 0.5
)
