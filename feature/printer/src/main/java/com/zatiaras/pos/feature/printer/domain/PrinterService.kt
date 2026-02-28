package com.zatiaras.pos.feature.printer.domain

import android.content.Context
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.printer.R
import com.zatiaras.pos.feature.printer.data.bluetooth.BluetoothPrinterManager
import com.zatiaras.pos.feature.printer.data.escpos.ReceiptFormatter
import com.zatiaras.pos.feature.printer.data.preferences.PrinterPreferences
import com.zatiaras.pos.feature.printer.domain.model.PrinterStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for printing receipts from anywhere in the app.
 * 
 * This is a thin wrapper around BluetoothPrinterManager and ReceiptFormatter
 * that provides a simple API for printing transactions.
 */
@Singleton
class PrinterService @Inject constructor(
    private val printerManager: BluetoothPrinterManager,
    private val receiptFormatter: ReceiptFormatter,
    private val printerPreferences: PrinterPreferences,
    @ApplicationContext private val context: Context
) {
    /**
     * Observe printer status.
     */
    val status: StateFlow<PrinterStatus> = printerManager.status
    
    /**
     * Check if a printer is currently connected.
     */
    fun isConnected(): Boolean = printerManager.isConnected()
    
    /**
     * Check if Bluetooth is available and enabled.
     */
    fun isBluetoothReady(): Boolean = 
        printerManager.isBluetoothAvailable() && printerManager.isBluetoothEnabled()
    
    /**
     * Get the name of connected printer, or null if not connected.
     */
    fun getConnectedPrinterName(): String? = 
        printerManager.getConnectedDevice()?.displayName
    
    /**
     * Print a transaction receipt.
     * 
     * @param transaction The transaction to print
     * @return Result with Unit on success, error on failure
     */
    suspend fun printReceipt(transaction: Transaction): Result<Unit> {
        if (!printerManager.isConnected()) {
            return Result.failure(Exception(context.getString(R.string.printer_not_connected)))
        }
        
        return try {
            val paperWidth = printerPreferences.getPaperWidth()
            val storeName = printerPreferences.getStoreName()
            val storeAddress = printerPreferences.getStoreAddress()
            
            val receiptBytes = receiptFormatter.formatReceipt(
                transaction = transaction,
                storeName = storeName,
                storeAddress = storeAddress,
                paperWidth = paperWidth
            )
            
            Timber.d("Printing receipt for transaction ${transaction.transactionNumber}")
            printerManager.print(receiptBytes)
        } catch (e: Exception) {
            Timber.e(e, "Failed to print receipt")
            Result.failure(e)
        }
    }
    
    /**
     * Try to auto-connect to last used printer.
     */
    suspend fun autoConnect(): Result<Unit> {
        if (printerManager.isConnected()) {
            return Result.success(Unit)
        }
        
        val lastPrinter = printerPreferences.getLastPrinter() ?: return Result.failure(
            Exception(context.getString(R.string.printer_no_saved_device))
        )
        
        if (!printerPreferences.isAutoConnectEnabled()) {
            return Result.failure(Exception(context.getString(R.string.printer_auto_connect_disabled)))
        }
        
        Timber.d("Auto-connecting to ${lastPrinter.displayName}")
        return printerManager.connect(lastPrinter)
    }
}
