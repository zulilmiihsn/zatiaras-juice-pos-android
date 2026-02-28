package com.zatiaras.pos.feature.reports.domain.usecase

import com.zatiaras.pos.core.domain.util.LocaleUtils
import com.zatiaras.pos.feature.reports.domain.model.DailyRevenue
import com.zatiaras.pos.feature.reports.domain.model.DashboardStats
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

data class DashboardAnalyticsResult(
    val averageTransactionsPerDay: Int,
    val peakHours: String,
    val averageOrderValue: Long,
    val averageItemsPerTransaction: Double,
    val busiestDay: String
)

class CalculateDashboardAnalyticsUseCase @Inject constructor() {
    operator fun invoke(
        stats: DashboardStats,
        weeklyRevenue: List<DailyRevenue>
    ): DashboardAnalyticsResult {
        val avgTrans = calculateAverageTransactions(weeklyRevenue)
        val pkHours = calculatePeakHours()
        val avgOrder = calculateAverageOrderValue(stats)
        val avgItems = calculateAverageItemsPerTransaction(stats)
        val busyDay = calculateBusiestDay(weeklyRevenue)
        
        return DashboardAnalyticsResult(
            averageTransactionsPerDay = avgTrans,
            peakHours = pkHours,
            averageOrderValue = avgOrder,
            averageItemsPerTransaction = avgItems,
            busiestDay = busyDay
        )
    }

    private fun calculateAverageTransactions(weeklyRevenue: List<DailyRevenue>): Int {
        if (weeklyRevenue.isEmpty()) return 0
        val totalTransactions = weeklyRevenue.sumOf { it.transactionCount }
        return totalTransactions / weeklyRevenue.size
    }

    private fun calculatePeakHours(): String {
        return "-"
    }

    private fun calculateAverageOrderValue(stats: DashboardStats): Long {
        if (stats.todayTransactions == 0) return 0L
        return stats.todayRevenue / stats.todayTransactions
    }

    private fun calculateAverageItemsPerTransaction(stats: DashboardStats): Double {
        if (stats.todayTransactions == 0) return 0.0
        return stats.todayItemsSold.toDouble() / stats.todayTransactions
    }

    private fun calculateBusiestDay(weeklyRevenue: List<DailyRevenue>): String {
        if (weeklyRevenue.isEmpty() || weeklyRevenue.all { it.transactionCount == 0 }) return "-"
        val busiestDay = weeklyRevenue.maxByOrNull { it.transactionCount }
        return busiestDay?.let {
            SimpleDateFormat("EEEE", LocaleUtils.LOCALE_ID).format(Date(it.date))
        } ?: "-"
    }
}
