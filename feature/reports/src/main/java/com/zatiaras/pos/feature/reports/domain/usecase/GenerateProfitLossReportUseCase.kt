package com.zatiaras.pos.feature.reports.domain.usecase

import com.zatiaras.pos.core.domain.model.CashCategories
import com.zatiaras.pos.feature.reports.domain.model.ExpenseCategoryItem
import com.zatiaras.pos.feature.reports.domain.model.ExpenseDetailItem
import com.zatiaras.pos.feature.reports.domain.model.IncomeDetailItem
import com.zatiaras.pos.feature.reports.domain.model.ManualCashRecord
import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport
import com.zatiaras.pos.feature.reports.domain.repository.ReportRepository
import javax.inject.Inject

/**
 * UseCase to generate Profit & Loss Report.
 * Encapsulates the complex accounting logic (taxes, separating operating vs non-operating income)
 * away from the Repository and ViewModel to adhere to Clean Architecture.
 */
class GenerateProfitLossReportUseCase @Inject constructor(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(startDate: Long, endDate: Long, taxPercentage: Double = 0.5): Result<ProfitLossReport> {
        return try {
            val rawDataResult = reportRepository.getRawProfitLossData(startDate, endDate)
            if (rawDataResult.isFailure) {
                return Result.failure(rawDataResult.exceptionOrNull() ?: Exception("Gagal mengambil data mentah laporan"))
            }
            
            val rawData = rawDataResult.getOrThrow()
            
            // 1. Process POS Revenue
            val posNetRevenue = rawData.posGrossRevenue - rawData.posTotalDiscount
            
            // 2. Aggregate Manual Records
            val manualSummary = aggregateManualRecords(rawData.manualRecords)
            val expensesByCategory = aggregateExpensesByCategory(rawData.manualRecords)
            val (manualIncomeItems, otherIncomeItems) = aggregateManualIncomeItems(rawData.manualRecords)
            
            // 3. Combine Data
            val operatingRevenue = posNetRevenue + manualSummary.operatingIncome
            val otherRevenue = manualSummary.otherIncome
            val totalRevenue = operatingRevenue + otherRevenue
            
            val totalExpenses = manualSummary.operatingExpense + manualSummary.otherExpense
            
            // 4. Calculate Profit & Tax (based on taxPercentage on Gross Profit)
            val grossProfit = totalRevenue - totalExpenses
            val taxRate = taxPercentage / 100.0
            val tax = if (grossProfit > 0) (grossProfit * taxRate).toLong() else 0L
            val netProfit = grossProfit - tax
            
            val report = ProfitLossReport(
                periodStart = startDate,
                periodEnd = endDate,
                operatingRevenue = operatingRevenue,
                otherRevenue = otherRevenue,
                grossRevenue = totalRevenue,
                operatingExpenses = manualSummary.operatingExpense,
                otherExpenses = manualSummary.otherExpense,
                totalExpenses = totalExpenses,
                grossProfit = grossProfit,
                tax = tax,
                netProfit = netProfit,
                transactionCount = rawData.posTransactions + rawData.manualRecords.count { it.type == "INCOME" },
                // Detailed breakdown
                productSales = rawData.productSales,
                posNetRevenue = posNetRevenue,
                manualOperatingIncome = manualSummary.operatingIncome,
                manualIncomeItems = manualIncomeItems,
                otherIncomeItems = otherIncomeItems,
                expensesByCategory = expensesByCategory,
                taxPercentage = taxPercentage
            )
            
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Helper Functions mapped from old Repo ---

    private data class ManualRecordSummary(
        val operatingIncome: Long,
        val otherIncome: Long,
        val operatingExpense: Long,
        val otherExpense: Long
    )
    
    private fun aggregateManualRecords(records: List<ManualCashRecord>): ManualRecordSummary {
        var operatingIncome = 0L
        var otherIncome = 0L
        var operatingExpense = 0L
        var otherExpense = 0L
        
        records.forEach { record ->
            when {
                record.type == "INCOME" && record.category == CashCategories.OPERATING_INCOME -> 
                    operatingIncome += record.amount
                record.type == "INCOME" -> 
                    otherIncome += record.amount
                record.type == "EXPENSE" && CashCategories.OPERATING_EXPENSES.contains(record.category) -> 
                    operatingExpense += record.amount
                record.type == "EXPENSE" -> 
                    otherExpense += record.amount
            }
        }
        
        return ManualRecordSummary(operatingIncome, otherIncome, operatingExpense, otherExpense)
    }
    
    private fun aggregateExpensesByCategory(records: List<ManualCashRecord>): List<ExpenseCategoryItem> {
        val expenseRecords = records.filter { it.type == "EXPENSE" && !it.isDeleted }
        
        return expenseRecords
            .groupBy { it.category ?: "Lainnya" }
            .map { (category, items) ->
                ExpenseCategoryItem(
                    category = category,
                    amount = items.sumOf { it.amount },
                    items = items.map { record ->
                        ExpenseDetailItem(
                            description = record.description,
                            amount = record.amount
                        )
                    }
                )
            }
            .sortedByDescending { it.amount }
    }
    
    private fun aggregateManualIncomeItems(records: List<ManualCashRecord>): Pair<List<IncomeDetailItem>, List<IncomeDetailItem>> {
        val incomeRecords = records.filter { it.type == "INCOME" && !it.isDeleted }
        
        val operatingIncomeItems = incomeRecords
            .filter { it.category == CashCategories.OPERATING_INCOME }
            .map { record ->
                IncomeDetailItem(
                    description = record.description,
                    amount = record.amount,
                    category = record.category
                )
            }
            .sortedByDescending { it.amount }
        
        val otherIncomeItems = incomeRecords
            .filter { it.category != CashCategories.OPERATING_INCOME }
            .map { record ->
                IncomeDetailItem(
                    description = record.description,
                    amount = record.amount,
                    category = record.category
                )
            }
            .sortedByDescending { it.amount }
        
        return Pair(operatingIncomeItems, otherIncomeItems)
    }
}
