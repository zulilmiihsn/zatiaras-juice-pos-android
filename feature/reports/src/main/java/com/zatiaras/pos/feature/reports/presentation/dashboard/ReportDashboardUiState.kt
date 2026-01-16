package com.zatiaras.pos.feature.reports.presentation.dashboard

import com.zatiaras.pos.feature.reports.domain.model.DailyRevenue
import com.zatiaras.pos.feature.reports.domain.model.DashboardStats
import com.zatiaras.pos.feature.reports.domain.model.TopProduct

/**
 * UI State for the Reports Dashboard.
 */
data class ReportDashboardUiState(
    val isLoading: Boolean = true,
    val isStoreOpen: Boolean = false,
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
    // Statistics metrics
    val averageTransactionsPerDay: Int = 0,
    val peakHours: String = "-",
    val averageOrderValue: Long = 0,           // Rata-rata nilai transaksi
    val averageItemsPerTransaction: Double = 0.0, // Rata-rata item per transaksi
    val growthPercent: Double? = null,         // Pertumbuhan vs kemarin (null = belum ada data)
    val busiestDay: String = "-",              // Hari paling ramai
    val error: String? = null

)

