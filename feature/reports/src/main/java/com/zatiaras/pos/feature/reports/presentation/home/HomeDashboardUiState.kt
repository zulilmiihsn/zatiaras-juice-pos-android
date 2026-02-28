package com.zatiaras.pos.feature.reports.presentation.home

import com.zatiaras.pos.feature.reports.domain.model.DashboardStats
import com.zatiaras.pos.feature.reports.domain.model.DailyRevenue
import com.zatiaras.pos.feature.reports.domain.model.TopProduct
import androidx.compose.runtime.Immutable

/**
 * UI State for Home Dashboard Screen.
 * Contains all the data needed to display the home dashboard.
 */
@Immutable
data class HomeDashboardUiState(
    val isLoading: Boolean = true,
    val isStoreOpen: Boolean = false,
    val isOwner: Boolean = false,
    val stats: DashboardStats = DashboardStats(
        todayRevenue = 0L,
        todayTransactions = 0,
        todayItemsSold = 0,
        weeklyRevenue = 0L,
        monthlyRevenue = 0L,
        revenueGrowthPercent = 0.0
    ),
    val openingBalance: Long = 0L,
    val todayExpenses: Long = 0L,
    val weeklyRevenue: List<DailyRevenue> = emptyList(),
    val topProducts: List<TopProduct> = emptyList(),
    val averageTransactionsPerDay: Int = 0,
    val peakHours: String = "-",
    val averageOrderValue: Long = 0L,
    val averageItemsPerTransaction: Double = 0.0,
    val growthPercent: Double? = null,
    val busiestDay: String = "-",
    val error: com.zatiaras.pos.core.ui.util.UiText? = null
)
