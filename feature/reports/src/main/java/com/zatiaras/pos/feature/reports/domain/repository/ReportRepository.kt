package com.zatiaras.pos.feature.reports.domain.repository

import com.zatiaras.pos.feature.reports.domain.model.CashFlowItem
import com.zatiaras.pos.feature.reports.domain.model.DailyRevenue
import com.zatiaras.pos.feature.reports.domain.model.DashboardStats
import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport
import com.zatiaras.pos.feature.reports.domain.model.RawProfitLossData
import com.zatiaras.pos.feature.reports.domain.model.TopProduct
import com.zatiaras.pos.feature.reports.domain.model.TransactionSummaryItem

/**
 * Repository interface for report data operations.
 */
interface ReportRepository {
    
    /**
     * Get dashboard summary statistics.
     */
    suspend fun getDashboardStats(): Result<DashboardStats>
    
    /**
     * Get daily revenue for the last N days.
     * @param days Number of days to fetch (default 7)
     */
    suspend fun getDailyRevenueHistory(days: Int = 7): Result<List<DailyRevenue>>
    
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
    ): Result<List<TopProduct>>
    
    /**
     * Get raw profit & loss data for a period before calculation.
     */
    suspend fun getRawProfitLossData(
        startDate: Long,
        endDate: Long
    ): Result<RawProfitLossData>

    /**
     * Get transaction summaries for AI analysis.
     * Returns domain-level models instead of data-layer entities.
     */
    suspend fun getTransactionsForAnalysis(
        startDate: Long,
        endDate: Long
    ): Result<List<TransactionSummaryItem>>

    /**
     * Get cash flow records for AI analysis.
     * Returns domain-level models instead of data-layer entities.
     */
    suspend fun getCashRecordsForAnalysis(
        startDate: Long,
        endDate: Long
    ): Result<List<CashFlowItem>>
}
