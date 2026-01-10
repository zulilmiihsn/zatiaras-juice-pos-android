package com.zatiaras.pos.feature.reports.domain.model

/**
 * Represents daily revenue data for charts.
 */
data class DailyRevenue(
    val date: Long,           // Epoch timestamp (start of day)
    val revenue: Long,        // Total revenue in cents/smallest unit
    val transactionCount: Int // Number of transactions
)
