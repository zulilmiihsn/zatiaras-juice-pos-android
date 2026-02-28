package com.zatiaras.pos.feature.reports.domain.model

/**
 * Profit & Loss report data with detailed breakdown.
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
    val transactionCount: Int,
    
    // === DETAILED BREAKDOWN ===
    // POS Sales per product
    val productSales: List<ProductSaleItem> = emptyList(),
    
    // Operating Revenue Details (POS net + Manual operating income)  
    val posNetRevenue: Long = 0,
    val manualOperatingIncome: Long = 0,
    
    // Manual income items breakdown (like "Sangu Ilham", "Titipan dari X")
    val manualIncomeItems: List<IncomeDetailItem> = emptyList(),
    
    // Other income items breakdown
    val otherIncomeItems: List<IncomeDetailItem> = emptyList(),
    
    // Expense breakdown by category
    val expensesByCategory: List<ExpenseCategoryItem> = emptyList(),
    
    // Tax percentage applied
    val taxPercentage: Double = 0.5
)

/**
 * Detail penjualan per produk dari POS.
 */
data class ProductSaleItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val revenue: Long
)

/**
 * Detail pemasukan manual individual (e.g., "Sangu Ilham", "Titipan dari X").
 */
data class IncomeDetailItem(
    val description: String,
    val amount: Long,
    val category: String? = null
)

/**
 * Detail pengeluaran per kategori.
 */
data class ExpenseCategoryItem(
    val category: String,
    val amount: Long,
    val items: List<ExpenseDetailItem> = emptyList()
)

/**
 * Detail item pengeluaran individual.
 */
data class ExpenseDetailItem(
    val description: String,
    val amount: Long
)
