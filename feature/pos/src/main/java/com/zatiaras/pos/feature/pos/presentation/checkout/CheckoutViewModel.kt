package com.zatiaras.pos.feature.pos.presentation.checkout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.domain.Result
import com.zatiaras.pos.core.domain.onFailure
import com.zatiaras.pos.core.domain.onSuccess
import com.zatiaras.pos.feature.pos.R
import com.zatiaras.pos.feature.pos.domain.model.Cart
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import com.zatiaras.pos.feature.pos.domain.repository.TransactionRepository
import com.zatiaras.pos.feature.pos.presentation.CheckoutEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Checkout screen.
 * 
 * Handles:
 * - Payment method selection
 * - Amount paid input (for cash)
 * - Discount and tax calculations
 * - Transaction completion
 */
@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val calculateCheckoutTotalsUseCase: com.zatiaras.pos.feature.pos.domain.usecase.CalculateCheckoutTotalsUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Loading)
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()
    
    // Cart is passed from PosViewModel via navigation argument or shared holder
    private var cart: Cart = Cart()
    
    /**
     * Initialize checkout with cart data.
     * Call this from the UI when the screen is displayed.
     */
    fun initializeWithCart(cart: Cart) {
        this.cart = cart
        calculateAndUpdateState()
    }
    
    private fun calculateAndUpdateState() {
        val currentState = _uiState.value
        val discountPercent = if (currentState is CheckoutUiState.Ready) {
            currentState.discountPercent
        } else 0.0
        val taxPercent = if (currentState is CheckoutUiState.Ready) {
            currentState.taxPercent
        } else 11.0
        val paymentMethod = if (currentState is CheckoutUiState.Ready) {
            currentState.selectedPaymentMethod
        } else PaymentMethod.CASH
        val amountPaid = if (currentState is CheckoutUiState.Ready) {
            currentState.amountPaid
        } else ""
        val notes = if (currentState is CheckoutUiState.Ready) {
            currentState.notes
        } else ""
        val customerName = if (currentState is CheckoutUiState.Ready) {
            currentState.customerName
        } else ""
        
        val result = calculateCheckoutTotalsUseCase(
            subtotal = cart.subtotal,
            discountPercent = discountPercent,
            taxPercent = taxPercent,
            amountPaidStr = amountPaid,
            paymentMethod = paymentMethod
        )
        
        _uiState.value = CheckoutUiState.Ready(
            cart = cart,
            subtotal = result.subtotal,
            discountPercent = discountPercent,
            discountAmount = result.discountAmount,
            taxPercent = taxPercent,
            taxAmount = result.taxAmount,
            grandTotal = result.grandTotal,
            selectedPaymentMethod = paymentMethod,
            amountPaid = amountPaid,
            changeAmount = result.changeAmount,
            notes = notes,
            customerName = customerName
        )
    }

    fun onEvent(event: CheckoutEvent) {
        val currentState = _uiState.value
        if (currentState !is CheckoutUiState.Ready) return

        when (event) {
            is CheckoutEvent.SetPaymentMethod -> {
                _uiState.value = currentState.copy(
                    selectedPaymentMethod = event.method,
                    // Reset amount paid when switching payment method
                    amountPaid = if (event.method != PaymentMethod.CASH) "" else currentState.amountPaid,
                    changeAmount = 0
                )
                calculateAndUpdateState()
            }
            
            is CheckoutEvent.SetAmountPaid -> {
                // Only allow digits
                val cleanAmount = event.amount.filter { it.isDigit() }
                _uiState.value = currentState.copy(amountPaid = cleanAmount)
                calculateAndUpdateState()
            }
            
            is CheckoutEvent.SetDiscountPercent -> {
                val percent = event.percent.toDoubleOrNull() ?: 0.0
                val validPercent = percent.coerceIn(0.0, 100.0)
                _uiState.value = currentState.copy(discountPercent = validPercent)
                calculateAndUpdateState()
            }
            
            is CheckoutEvent.SetNotes -> {
                _uiState.value = currentState.copy(notes = event.notes)
            }

            is CheckoutEvent.SetCustomerName -> {
                _uiState.value = currentState.copy(customerName = event.name)
            }
            
            is CheckoutEvent.ConfirmPayment -> {
                confirmPayment(currentState)
            }
            
            is CheckoutEvent.DismissError -> {
                _uiState.value = currentState.copy(paymentError = null)
            }
            
            is CheckoutEvent.CancelCheckout -> {
                // Handled by navigation
            }
        }
    }

    private fun confirmPayment(state: CheckoutUiState.Ready) {
        if (!state.canComplete) {
            _uiState.value = state.copy(
                paymentError = context.getString(R.string.checkout_error_insufficient_amount)
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = state.copy(isProcessing = true, paymentError = null)
            
            val amountPaidValue = when (state.selectedPaymentMethod) {
                PaymentMethod.CASH -> state.amountPaidValue
                else -> state.grandTotal // Exact amount for non-cash
            }
            
            transactionRepository.createTransaction(
                cart = cart,
                paymentMethod = state.selectedPaymentMethod,
                amountPaid = amountPaidValue,
                discountPercent = state.discountPercent,
                taxPercent = state.taxPercent,
                notes = state.notes.ifBlank { null },
                customerName = state.customerName.ifBlank { null }
            ).onSuccess { transaction ->
                Timber.d("Transaction completed: ${transaction.transactionNumber}")
                _uiState.value = CheckoutUiState.Success(transaction)
            }.onFailure { error ->
                Timber.e(error, "Failed to complete transaction")
                _uiState.value = state.copy(
                    isProcessing = false,
                    paymentError = error?.message ?: context.getString(R.string.checkout_error_save_failed)
                )
            }
        }
    }
    
    /**
     * Quick amount buttons for common denominations (Additive).
     * e.g. pressing 20,000 when current is 50,000 makes it 70,000.
     */
    fun setQuickAmount(amount: Long) {
        val currentState = _uiState.value
        if (currentState !is CheckoutUiState.Ready) return
        
        val currentAmount = currentState.amountPaid.filter { it.isDigit() }.toLongOrNull() ?: 0L
        val newAmount = currentAmount + amount
        
        _uiState.value = currentState.copy(amountPaid = newAmount.toString())
        calculateAndUpdateState()
    }
    
    /**
     * Resets the amount paid to empty.
     */
    fun clearAmount() {
        val currentState = _uiState.value
        if (currentState !is CheckoutUiState.Ready) return
        
        _uiState.value = currentState.copy(amountPaid = "")
        calculateAndUpdateState()
    }
    
    /**
     * Set exact amount (pay with exact change).
     */
    fun setExactAmount() {
        val currentState = _uiState.value
        if (currentState !is CheckoutUiState.Ready) return
        
        _uiState.value = currentState.copy(amountPaid = currentState.grandTotal.toString())
        calculateAndUpdateState()
    }
}
