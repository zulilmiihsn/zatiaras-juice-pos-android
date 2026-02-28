package com.zatiaras.pos.feature.pos.presentation.receipt

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.feature.pos.R
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI State for Receipt screen.
 */
data class ReceiptUiState(
    val transaction: Transaction? = null,
    val isPrinting: Boolean = false,
    val isPrinterConnected: Boolean = false,
    val printerName: String? = null
)

/**
 * Events from Receipt screen.
 */
sealed class ReceiptEvent {
    data class ShowToast(val message: String) : ReceiptEvent()
    data object PrintSuccess : ReceiptEvent()
    data object NavigateToPrinterSettings : ReceiptEvent()
}

/**
 * ViewModel for Receipt screen.
 * 
 * Note: PrinterService is injected via a separate optional interface
 * to avoid circular dependency between feature modules.
 */
@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val printerHelper: ReceiptPrinterHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReceiptUiState())
    val uiState: StateFlow<ReceiptUiState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<ReceiptEvent>()
    val events: SharedFlow<ReceiptEvent> = _events.asSharedFlow()
    
    init {
        checkPrinterStatus()
    }
    
    fun setTransaction(transaction: Transaction) {
        _uiState.update { it.copy(transaction = transaction) }
    }
    
    private fun checkPrinterStatus() {
        viewModelScope.launch {
            val isConnected = printerHelper.isConnected()
            val printerName = printerHelper.getConnectedPrinterName()
            
            _uiState.update {
                it.copy(
                    isPrinterConnected = isConnected,
                    printerName = printerName
                )
            }
        }
    }
    
    fun printReceipt() {
        val transaction = _uiState.value.transaction ?: return
        
        viewModelScope.launch {
            if (!printerHelper.isConnected()) {
                _events.emit(ReceiptEvent.ShowToast(context.getString(R.string.pos_receipt_printer_not_connected)))
                _events.emit(ReceiptEvent.NavigateToPrinterSettings)
                return@launch
            }
            
            _uiState.update { it.copy(isPrinting = true) }
            
            printerHelper.printReceipt(transaction).fold(
                onSuccess = {
                    Timber.d("Receipt printed successfully")
                    _events.emit(ReceiptEvent.PrintSuccess)
                    _events.emit(ReceiptEvent.ShowToast(context.getString(R.string.pos_receipt_print_success)))
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to print receipt")
                    val reason = error.message ?: context.getString(R.string.pos_receipt_print_failed_generic)
                    _events.emit(
                        ReceiptEvent.ShowToast(
                            context.getString(R.string.pos_receipt_print_failed_with_reason, reason)
                        )
                    )
                }
            )
            
            _uiState.update { it.copy(isPrinting = false) }
        }
    }
    
    fun refreshPrinterStatus() {
        checkPrinterStatus()
    }
}

/**
 * Interface for printer operations.
 * This allows the pos module to use printer without direct dependency.
 */
interface ReceiptPrinterHelper {
    fun isConnected(): Boolean
    fun getConnectedPrinterName(): String?
    suspend fun printReceipt(transaction: Transaction): Result<Unit>
}

/**
 * No-op implementation when printer module is not available.
 */
class NoOpPrinterHelper @Inject constructor() : ReceiptPrinterHelper {
    override fun isConnected(): Boolean = false
    override fun getConnectedPrinterName(): String? = null
    override suspend fun printReceipt(transaction: Transaction): Result<Unit> = 
        Result.failure(Exception("Printer service not available"))
}
