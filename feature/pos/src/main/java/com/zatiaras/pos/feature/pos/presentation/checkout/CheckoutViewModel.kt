package com.zatiaras.pos.feature.pos.presentation.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.feature.pos.domain.model.Cart
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import com.zatiaras.pos.feature.pos.domain.repository.TransactionRepository
import com.zatiaras.pos.feature.pos.presentation.CheckoutEvent
import dagger.hilt.android.lifecycle.HiltViewModel
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
    savedStateHandle: SavedStateHandle
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
        
        val subtotal = cart.subtotal
        val discountAmount = (subtotal * discountPercent / 100).toLong()
        val afterDiscount = subtotal - discountAmount
        val taxAmount = (afterDiscount * taxPercent / 100).toLong()
        val grandTotal = afterDiscount + taxAmount
        
        val paid = amountPaid.toLongOrNull() ?: 0
        val changeAmount = if (paymentMethod == PaymentMethod.CASH && paid > grandTotal) {
            paid - grandTotal
        } else 0
        
        _uiState.value = CheckoutUiState.Ready(
            cart = cart,
            subtotal = subtotal,
            discountPercent = discountPercent,
            discountAmount = discountAmount,
            taxPercent = taxPercent,
            taxAmount = taxAmount,
            grandTotal = grandTotal,
            selectedPaymentMethod = paymentMethod,
            amountPaid = amountPaid,
            changeAmount = changeAmount,
            notes = notes
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
            
            is CheckoutEvent.ConfirmPayment -> {
                confirmPayment(currentState)
            }
            
            is CheckoutEvent.CancelCheckout -> {
                // Handled by navigation
            }
        }
    }

    private fun confirmPayment(state: CheckoutUiState.Ready) {
        if (!state.canComplete) {
            _uiState.value = state.copy(
                paymentError = "Jumlah bayar kurang dari total"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = state.copy(isProcessing = true, paymentError = null)
            
            val amountPaid = when (state.selectedPaymentMethod) {
                PaymentMethod.CASH -> state.amountPaidValue
                else -> state.grandTotal // Exact amount for non-cash
            }
            
            transactionRepository.createTransaction(
                cart = cart,
                paymentMethod = state.selectedPaymentMethod,
                amountPaid = amountPaid,
                discountPercent = state.discountPercent,
                taxPercent = state.taxPercent,
                notes = state.notes.ifBlank { null }
            ).onSuccess { transaction ->
                Timber.d("Transaction completed: ${transaction.transactionNumber}")
                _uiState.value = CheckoutUiState.Success(transaction)
            }.onFailure { error ->
                Timber.e(error, "Failed to complete transaction")
                _uiState.value = state.copy(
                    isProcessing = false,
                    paymentError = error.message ?: "Gagal menyimpan transaksi"
                )
            }
        }
    }
    
    /**
     * Quick amount buttons for common denominations.
     */
    fun setQuickAmount(amount: Long) {
        val currentState = _uiState.value
        if (currentState !is CheckoutUiState.Ready) return
        
        _uiState.value = currentState.copy(amountPaid = amount.toString())
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
