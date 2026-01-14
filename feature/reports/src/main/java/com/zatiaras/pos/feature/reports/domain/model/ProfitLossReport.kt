package com.zatiaras.pos.feature.reports.domain.model

/**
 * Profit & Loss report data.
 */
data class ProfitLossReport(
    val periodStart: Long,
    val periodEnd: Long,
    
    // Income Breakdown
    val operatingRevenue: Long,  // POS Sales + Manual Operating Income
    val otherRevenue: Long,      // Manual Other Income
    val grossRevenue: Long,      // Total Revenue (Operating + Other)
    
    // Expense Breakdown
    val operatingExpenses: Long, // COGS (if any) + Manual Operating Expenses
    val otherExpenses: Long,     // Manual Other Expenses
    val totalExpenses: Long,     // Total Expenses
    
    // Calculations
    val grossProfit: Long,       // Gross Revenue - Total Expenses
    val tax: Long,               // 0.5% turnover tax (if applicable)
    val netProfit: Long,         // Gross Profit - Tax
    
    // Metadata
    val transactionCount: Int
)

