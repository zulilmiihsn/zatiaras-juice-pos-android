package com.zatiaras.pos.feature.reports.domain.model

/**
 * Summary statistics for the dashboard.
 */
data class DashboardStats(
    val todayRevenue: Long,
    val todayTransactions: Int,
    val todayItemsSold: Int,
    val weeklyRevenue: Long,
    val monthlyRevenue: Long,
    val revenueGrowthPercent: Double  // Compared to previous period
)
