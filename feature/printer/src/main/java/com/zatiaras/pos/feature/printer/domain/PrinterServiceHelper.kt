package com.zatiaras.pos.feature.printer.domain

import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.presentation.receipt.ReceiptPrinterHelper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ReceiptPrinterHelper using PrinterService.
 * 
 * This bridges the pos module with the printer module.
 */
@Singleton
class PrinterServiceHelper @Inject constructor(
    private val printerService: PrinterService
) : ReceiptPrinterHelper {
    
    override fun isConnected(): Boolean = printerService.isConnected()
    
    override fun getConnectedPrinterName(): String? = printerService.getConnectedPrinterName()
    
    override suspend fun printReceipt(transaction: Transaction): Result<Unit> = 
        printerService.printReceipt(transaction)
}
