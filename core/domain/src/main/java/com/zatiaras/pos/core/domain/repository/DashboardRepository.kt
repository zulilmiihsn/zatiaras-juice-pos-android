package com.zatiaras.pos.core.domain.repository

/**
 * Repository interface for Dashboard metrics.
 * 
 * Provides today's summary statistics for the home dashboard.
 * Implementation is in core/data module.
 */
interface DashboardRepository {
    
    /**
     * Get today's total revenue.
     */
    suspend fun getTodayRevenue(): Result<Long>
    
    /**
     * Get today's transaction count.
     */
    suspend fun getTodayTransactionCount(): Result<Int>
    
    /**
     * Get today's items sold count.
     */
    suspend fun getTodayItemsSold(): Result<Int>
}
