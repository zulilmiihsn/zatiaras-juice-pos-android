package com.zatiaras.pos.feature.reports.domain.repository

import com.zatiaras.pos.feature.reports.domain.model.DailyRevenue
import com.zatiaras.pos.feature.reports.domain.model.DashboardStats
import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport
import com.zatiaras.pos.feature.reports.domain.model.TopProduct

/**
 * Repository interface for report data operations.
 */
interface ReportRepository {
    
    /**
     * Get dashboard summary statistics.
     */
    suspend fun getDashboardStats(): DashboardStats
    
    /**
     * Get daily revenue for the last N days.
     * @param days Number of days to fetch (default 7)
     */
    suspend fun getDailyRevenueHistory(days: Int = 7): List<DailyRevenue>
    
    /**
     * Get top selling products for a period.
     * @param startDate Start timestamp
     * @param endDate End timestamp
     * @param limit Max number of products to return
     */
    suspend fun getTopSellingProducts(
        startDate: Long,
        endDate: Long,
        limit: Int = 10
    ): List<TopProduct>
    
    /**
     * Get profit & loss report for a period.
     */
    suspend fun getProfitLossReport(
        startDate: Long,
        endDate: Long
    ): ProfitLossReport
}
