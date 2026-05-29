package com.zatiaras.pos.feature.pos.presentation.history

import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import com.zatiaras.pos.feature.pos.domain.model.Transaction

/**
 * Render state for transaction history.
 *
 * Filtering is derived so the ViewModel can retain the full transaction list
 * while the screen changes search/payment filters.
 */
data class TransactionHistoryUiState(
    val isLoading: Boolean = true,
    val isOwner: Boolean = false,
    val allTransactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val paymentFilter: PaymentFilter = PaymentFilter.ALL,
    val showDeleteConfirmDialog: Boolean = false,
    val selectedTransaction: Transaction? = null,
    val showDetailDialog: Boolean = false,
    val error: String? = null,
) {
    val displayedTransactions: List<Transaction>
        get() {
            var result = allTransactions

            // Search transaction notes and numbers; customer names are displayed
            // but may be blank for walk-in transactions.
            if (searchQuery.isNotBlank()) {
                val query = searchQuery.lowercase()
                result = result.filter {
                    it.notes?.lowercase()?.contains(query) == true ||
                        it.transactionNumber.lowercase().contains(query)
                }
            }

            // Payment filters use cashier-facing labels but compare against the
            // domain PaymentMethod enum.
            if (paymentFilter != PaymentFilter.ALL) {
                result = result.filter {
                    when (paymentFilter) {
                        PaymentFilter.QRIS -> it.paymentMethod == PaymentMethod.QRIS
                        PaymentFilter.TUNAI -> it.paymentMethod == PaymentMethod.CASH
                        else -> true
                    }
                }
            }

            return result
        }
}

enum class PaymentFilter(val label: String) {
    ALL("Semua"),
    QRIS("QRIS"),
    TUNAI("Tunai"),
}
