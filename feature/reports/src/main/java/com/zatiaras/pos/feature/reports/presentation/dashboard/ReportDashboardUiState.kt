package com.zatiaras.pos.feature.reports.presentation.dashboard

import com.zatiaras.pos.feature.reports.domain.model.DailyRevenue
import com.zatiaras.pos.feature.reports.domain.model.DashboardStats
import com.zatiaras.pos.feature.reports.domain.model.TopProduct

/**
 * UI State for the Reports Dashboard.
 */
data class ReportDashboardUiState(
    val isLoading: Boolean = true,
    val stats: DashboardStats = DashboardStats(
        todayRevenue = 0,
        todayTransactions = 0,
        todayItemsSold = 0,
        weeklyRevenue = 0,
        monthlyRevenue = 0,
        revenueGrowthPercent = 0.0
    ),
    val weeklyRevenue: List<DailyRevenue> = emptyList(),
    val topProducts: List<TopProduct> = emptyList(),
    val error: String? = null
)
