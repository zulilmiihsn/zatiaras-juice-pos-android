package com.zatiaras.pos.core.domain.model

/**
 * Centralized category constants for Cash Records.
 * Used in CashRecordScreen and ReportRepositoryImpl to ensure DRY compliance.
 */
object CashCategories {
    
    // === INCOME TYPES ===
    const val OPERATING_INCOME = "Pendapatan Usaha"
    const val OTHER_INCOME = "Pemasukan Lainnya"
    
    // === EXPENSE TYPES ===
    const val EXPENSE_SALARY = "Beban Gaji"
    const val EXPENSE_RENT = "Beban Sewa"
    const val EXPENSE_UTILITIES = "Beban Listrik & Air"
    const val EXPENSE_SUPPLIES = "Beban Perlengkapan"
    const val EXPENSE_OPERATING = "Beban Usaha"
    const val OTHER_EXPENSE = "Beban Lainnya"
    
    /**
     * List of operating expense categories.
     * Used for P&L classification.
     */
    val OPERATING_EXPENSES = listOf(
        EXPENSE_SALARY,
        EXPENSE_RENT,
        EXPENSE_UTILITIES,
        EXPENSE_SUPPLIES,
        EXPENSE_OPERATING
    )
    
    // === UI DROPDOWN LISTS ===
    val INCOME_CATEGORIES = listOf(OPERATING_INCOME, OTHER_INCOME)
    val EXPENSE_CATEGORIES = OPERATING_EXPENSES + OTHER_EXPENSE
}
