package com.zatiaras.pos.feature.printer.data.escpos

import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.printer.domain.model.PaperWidth
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Formats transaction data into ESC/POS byte commands for thermal printing.
 */
@Singleton
class ReceiptFormatter @Inject constructor() {
    
    private val currencyFormat: NumberFormat = CurrencyFormatter.getCurrencyFormatter()
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
    
    /**
     * Format a Transaction into ESC/POS bytes for printing.
     * 
     * @param transaction The transaction to format
     * @param storeName Store name to print on header
     * @param storeAddress Optional store address
     * @param paperWidth Paper width (58mm or 80mm)
     */
    fun formatReceipt(
        transaction: Transaction,
        storeName: String = "ZATIARAS",
        storeAddress: String? = null,
        paperWidth: PaperWidth = PaperWidth.MM_58
    ): ByteArray {
        val charPerLine = paperWidth.charPerLine
        val builder = ByteArrayBuilder()
        
        builder.apply {
            // Initialize printer
            append(EscPosCommands.INIT)
            
            // ===== HEADER =====
            append(EscPosCommands.ALIGN_CENTER)
            append(EscPosCommands.DOUBLE_SIZE_ON)
            appendText(storeName)
            appendNewLine()
            append(EscPosCommands.NORMAL_SIZE)
            
            // Address if provided
            storeAddress?.let {
                appendText(it)
                appendNewLine()
            }
            
            // Date and transaction number
            appendText(dateFormat.format(Date(transaction.createdAt)))
            appendNewLine()
            appendText(transaction.transactionNumber)
            appendNewLine()
            
            // Separator
            append(EscPosCommands.ALIGN_LEFT)
            appendSeparator(charPerLine)
            appendNewLine()
            
            // ===== ITEMS =====
            for (item in transaction.items) {
                // Product name
                appendText(truncate(item.productName, charPerLine - 2))
                appendNewLine()
                
                // Quantity x Price = Subtotal (right aligned)
                val qtyPrice = "  ${item.quantity}x ${formatCurrency(item.productPrice)}"
                val subtotal = formatCurrency(item.subtotal)
                val spacing = charPerLine - qtyPrice.length - subtotal.length
                
                appendText(qtyPrice)
                if (spacing > 0) {
                    appendText(" ".repeat(spacing))
                }
                appendText(subtotal)
                appendNewLine()
            }
            
            // Separator
            appendSeparator(charPerLine)
            
            // ===== TOTALS =====
            
            // Subtotal
            appendTwoColumn("Subtotal", formatCurrency(transaction.subtotal), charPerLine)
            
            // Discount if any
            if (transaction.discountAmount > 0) {
                appendTwoColumn(
                    "Diskon (${transaction.discountPercent.toInt()}%)",
                    "-${formatCurrency(transaction.discountAmount)}",
                    charPerLine
                )
            }
            
            // Tax if any
            if (transaction.taxAmount > 0) {
                appendTwoColumn(
                    "PPN (${transaction.taxPercent.toInt()}%)",
                    formatCurrency(transaction.taxAmount),
                    charPerLine
                )
            }
            
            // Grand Total
            appendSeparator(charPerLine)
            append(EscPosCommands.BOLD_ON)
            append(EscPosCommands.DOUBLE_HEIGHT_ON)
            appendTwoColumn("TOTAL", formatCurrency(transaction.grandTotal), charPerLine)
            append(EscPosCommands.NORMAL_SIZE)
            append(EscPosCommands.BOLD_OFF)
            
            // Payment info
            appendSeparator(charPerLine)
            appendTwoColumn("Bayar (${transaction.paymentMethod.displayName})", 
                formatCurrency(transaction.amountPaid), charPerLine)
            
            if (transaction.changeAmount > 0) {
                appendTwoColumn("Kembalian", formatCurrency(transaction.changeAmount), charPerLine)
            }
            
            // Notes if any
            if (!transaction.notes.isNullOrBlank()) {
                appendNewLine()
                appendText("Catatan: ${transaction.notes}")
                appendNewLine()
            }
            
            // ===== FOOTER =====
            appendNewLine()
            append(EscPosCommands.ALIGN_CENTER)
            appendText("Terima Kasih")
            appendNewLine()
            appendText("Atas Kunjungan Anda!")
            appendNewLine()
            appendNewLine()
            
            // Feed and cut
            append(EscPosCommands.FEED_3_LINES)
            append(EscPosCommands.CUT_PARTIAL)
        }
        
        return builder.toByteArray()
    }
    
    /**
     * Format a test page for printer testing.
     */
    fun formatTestPage(paperWidth: PaperWidth = PaperWidth.MM_58): ByteArray {
        val charPerLine = paperWidth.charPerLine
        val builder = ByteArrayBuilder()
        
        builder.apply {
            append(EscPosCommands.INIT)
            
            append(EscPosCommands.ALIGN_CENTER)
            append(EscPosCommands.DOUBLE_SIZE_ON)
            appendText("TEST PRINT")
            appendNewLine()
            append(EscPosCommands.NORMAL_SIZE)
            
            appendText("ZatiarasPOS")
            appendNewLine()
            appendText(dateFormat.format(Date()))
            appendNewLine()
            
            append(EscPosCommands.ALIGN_LEFT)
            appendDoubleSeparator(charPerLine)
            
            appendText("Normal Text")
            appendNewLine()
            
            append(EscPosCommands.BOLD_ON)
            appendText("Bold Text")
            appendNewLine()
            append(EscPosCommands.BOLD_OFF)
            
            append(EscPosCommands.DOUBLE_HEIGHT_ON)
            appendText("Double Height")
            appendNewLine()
            append(EscPosCommands.NORMAL_SIZE)
            
            append(EscPosCommands.DOUBLE_WIDTH_ON)
            appendText("Double Width")
            appendNewLine()
            append(EscPosCommands.NORMAL_SIZE)
            
            appendDoubleSeparator(charPerLine)
            
            append(EscPosCommands.ALIGN_LEFT)
            appendText("Left Align")
            appendNewLine()
            
            append(EscPosCommands.ALIGN_CENTER)
            appendText("Center Align")
            appendNewLine()
            
            append(EscPosCommands.ALIGN_RIGHT)
            appendText("Right Align")
            appendNewLine()
            
            append(EscPosCommands.ALIGN_CENTER)
            appendDoubleSeparator(charPerLine)
            appendText("Printer OK!")
            appendNewLine()
            appendNewLine()
            
            append(EscPosCommands.FEED_3_LINES)
            append(EscPosCommands.CUT_PARTIAL)
        }
        
        return builder.toByteArray()
    }
    
    // ==================== HELPERS ====================
    
    private fun formatCurrency(amount: Long): String {
        return currencyFormat.format(amount).replace("Rp", "Rp ")
    }
    
    private fun truncate(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) text
        else text.take(maxLength - 2) + ".."
    }
    
    /**
     * Helper class to build byte arrays.
     */
    private class ByteArrayBuilder {
        private val buffer = mutableListOf<Byte>()
        
        fun append(bytes: ByteArray) {
            buffer.addAll(bytes.toList())
        }
        
        fun appendText(text: String) {
            append(text.toByteArray(Charsets.UTF_8))
        }
        
        fun appendNewLine() {
            append(EscPosCommands.LINE_FEED)
        }
        
        fun appendSeparator(charCount: Int) {
            appendText("-".repeat(charCount))
            appendNewLine()
        }
        
        fun appendDoubleSeparator(charCount: Int) {
            appendText("=".repeat(charCount))
            appendNewLine()
        }
        
        fun appendTwoColumn(left: String, right: String, totalWidth: Int) {
            val spacing = (totalWidth - left.length - right.length).coerceAtLeast(1)
            appendText(left)
            appendText(" ".repeat(spacing))
            appendText(right)
            appendNewLine()
        }
        
        fun toByteArray(): ByteArray = buffer.toByteArray()
    }
}
