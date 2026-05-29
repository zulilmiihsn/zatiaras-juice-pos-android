package com.zatiaras.pos.feature.pos.presentation.cashrecord

import com.zatiaras.pos.feature.pos.domain.model.CashRecord
import com.zatiaras.pos.feature.pos.domain.model.CashRecordType
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.domain.repository.CashSummary

/**
 * Unified row model for POS sales and manual cash-book entries.
 *
 * POS transactions are read-only income rows; manual records can represent
 * income or expense and can be deleted from the cash-book screen.
 */
sealed class CashFlowItem(
    open val id: String,
    open val amount: Long,
    open val description: String,
    open val createdAt: Long,
    open val isIncome: Boolean,
) {
    /**
     * POS transaction income from completed sales.
     */
    data class FromTransaction(
        val transaction: Transaction,
    ) : CashFlowItem(
        id = "trx_${transaction.id}",
        amount = transaction.grandTotal,
        description = "Penjualan ${transaction.transactionNumber}",
        createdAt = transaction.createdAt,
        isIncome = true,
    ) {
        val itemCount: Int get() = transaction.items.sumOf { it.quantity }
    }

    /**
     * Manual cash-book record entered by an operator.
     */
    data class FromCashRecord(
        val record: CashRecord,
    ) : CashFlowItem(
        id = "cash_${record.id}",
        amount = record.amount,
        description = record.description,
        createdAt = record.createdAt,
        isIncome = record.type == CashRecordType.INCOME,
    ) {
        val category: String? get() = record.category
        val canDelete: Boolean get() = true
        val originalId: String get() = record.id
    }
}

/**
 * Render state for the cash-book screen.
 */
data class CashRecordUiState(
    val items: List<CashFlowItem> = emptyList(),
    val summary: CashSummary = CashSummary(0, 0, 0),
    val posTransactionCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
)

/**
 * Draft state for the add/edit cash record sheet.
 */
data class CashRecordFormState(
    val type: CashRecordType = CashRecordType.INCOME,
    val amount: String = "",
    val description: String = "",
    val category: String = "",
    val notes: String = "",
    val date: Long? = null,
    val isSubmitting: Boolean = false,
    val amountError: String? = null,
    val descriptionError: String? = null,
) {
    val isValid: Boolean
        get() = amountError == null &&
            descriptionError == null &&
            amount.isNotBlank() &&
            description.isNotBlank()
}

/**
 * User intents from cash-book list and form screens.
 */
sealed interface CashRecordEvent {
    // Date filtering.
    data class SetDateFilter(
        val period: com.zatiaras.pos.core.domain.model.DatePeriod,
        val customStartDate: Long? = null,
        val customEndDate: Long? = null,
    ) : CashRecordEvent

    // Form updates.
    data class SetType(val type: CashRecordType) : CashRecordEvent
    data class SetAmount(val amount: String) : CashRecordEvent
    data class SetDescription(val description: String) : CashRecordEvent
    data class SetCategory(val category: String) : CashRecordEvent
    data class SetNotes(val notes: String) : CashRecordEvent
    data class SetDate(val date: Long?) : CashRecordEvent
    data object SaveRecord : CashRecordEvent

    // List mutations and transient UI events.
    data class DeleteRecord(val id: String) : CashRecordEvent
    data object DismissError : CashRecordEvent
}
