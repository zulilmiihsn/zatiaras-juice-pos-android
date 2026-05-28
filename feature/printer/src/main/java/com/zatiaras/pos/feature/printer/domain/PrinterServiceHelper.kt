package com.zatiaras.pos.feature.printer.domain

import com.zatiaras.pos.core.domain.printing.Receipt
import com.zatiaras.pos.core.domain.printing.ReceiptPrinter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ReceiptPrinter using PrinterService.
 *
 * This bridges the pos module with the printer module.
 */
@Singleton
class PrinterServiceHelper @Inject constructor(
    private val printerService: PrinterService,
) : ReceiptPrinter {

    override fun isConnected(): Boolean = printerService.isConnected()

    override fun getConnectedPrinterName(): String? = printerService.getConnectedPrinterName()

    override suspend fun printReceipt(receipt: Receipt): Result<Unit> = printerService.printReceipt(receipt)
}
