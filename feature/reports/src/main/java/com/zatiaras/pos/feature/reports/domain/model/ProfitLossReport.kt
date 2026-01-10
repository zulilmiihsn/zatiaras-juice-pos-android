package com.zatiaras.pos.feature.reports.domain.model

/**
 * Profit & Loss report data.
 */
data class ProfitLossReport(
    val periodStart: Long,
    val periodEnd: Long,
    val grossRevenue: Long,      // Before discount
    val totalDiscount: Long,
    val netRevenue: Long,        // After discount, before tax
    val totalTax: Long,
    val grandTotal: Long,        // Final revenue collected
    val estimatedCost: Long,     // If cost data available
    val grossProfit: Long,       // revenue - cost
    val transactionCount: Int
)
