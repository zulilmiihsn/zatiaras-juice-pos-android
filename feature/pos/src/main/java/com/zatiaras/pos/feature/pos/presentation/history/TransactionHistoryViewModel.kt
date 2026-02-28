package com.zatiaras.pos.feature.pos.presentation.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.data.access.AccessControlManager
import com.zatiaras.pos.feature.pos.R
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accessControlManager: AccessControlManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionHistoryUiState())
    val uiState: StateFlow<TransactionHistoryUiState> = _uiState.asStateFlow()

    init {
        observeUserRole()
        observeTransactions()
    }

    private fun observeUserRole() {
        viewModelScope.launch {
            accessControlManager.isOwner().collect { isOwner ->
                _uiState.update { it.copy(isOwner = isOwner) }
            }
        }
    }

    private fun observeTransactions() {
        _uiState.update { it.copy(isLoading = true) }
        transactionRepository.getTodayTransactions()
            .onEach { transactions ->
                _uiState.update { 
                    it.copy(
                        allTransactions = transactions,
                        isLoading = false
                    ) 
                }
            }
            .catch { e ->
                Timber.e(e, "Error loading today's transactions")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = context.getString(
                            R.string.pos_history_error_load_with_reason,
                            e.message ?: context.getString(R.string.pos_error_generic)
                        )
                    ) 
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onPaymentFilterChanged(filter: PaymentFilter) {
        _uiState.update { it.copy(paymentFilter = filter) }
    }

    fun showDetail(transaction: Transaction) {
        _uiState.update { 
            it.copy(
                selectedTransaction = transaction,
                showDetailDialog = true
            ) 
        }
    }

    fun hideDetail() {
        _uiState.update { 
            it.copy(
                selectedTransaction = null,
                showDetailDialog = false
            ) 
        }
    }

    fun showDeleteConfirm(transaction: Transaction) {
        _uiState.update { 
            it.copy(
                selectedTransaction = transaction,
                showDeleteConfirmDialog = true
            ) 
        }
    }

    fun hideDeleteConfirm() {
        _uiState.update { 
            it.copy(
                selectedTransaction = null,
                showDeleteConfirmDialog = false
            ) 
        }
    }

    fun deleteTransaction() {
        val transaction = _uiState.value.selectedTransaction ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = transactionRepository.deleteTransaction(transaction.id)
            if (result is com.zatiaras.pos.core.domain.Result.Success) {
                _uiState.update { 
                    it.copy(
                        showDeleteConfirmDialog = false,
                        selectedTransaction = null,
                        isLoading = false,
                        error = null
                    ) 
                }
            } else if (result is com.zatiaras.pos.core.domain.Result.Error) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = context.getString(R.string.pos_history_error_delete)
                    ) 
                }
            }
        }
    }

    fun updatePaymentMethod(method: PaymentMethod) {
        val transaction = _uiState.value.selectedTransaction ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = transactionRepository.updatePaymentMethod(transaction.id, method)
            if (result is com.zatiaras.pos.core.domain.Result.Success) {
                // Keep dialog open, update the transaction details from the flow automatically
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = null,
                        // Close detail dialog or just let it update? 
                        // The flow will emit a new list with updated transaction
                    ) 
                }
            } else if (result is com.zatiaras.pos.core.domain.Result.Error) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = context.getString(R.string.pos_history_error_payment_update)
                    ) 
                }
            }
        }
    }

    suspend fun verifyOwnerPin(pin: String): Boolean {
        return accessControlManager.verifyOwnerPin(pin)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
