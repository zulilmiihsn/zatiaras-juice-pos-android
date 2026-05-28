package com.zatiaras.pos.core.domain.printing

data class Receipt(
    val id: String,
    val number: String,
    val items: List<ReceiptItem>,
    val subtotal: Long,
    val discountAmount: Long,
    val discountPercent: Double,
    val taxAmount: Long,
    val taxPercent: Double,
    val grandTotal: Long,
    val paymentMethodName: String,
    val amountPaid: Long,
    val changeAmount: Long,
    val notes: String?,
    val customerName: String?,
    val createdAt: Long,
) {
    val totalQuantity: Int get() = items.sumOf { it.quantity }
}

data class ReceiptItem(
    val id: String,
    val productId: String,
    val productName: String,
    val productPrice: Long,
    val quantity: Int,
    val subtotal: Long,
    val notes: String?,
)

interface ReceiptPrinter {
    fun isConnected(): Boolean
    fun getConnectedPrinterName(): String?
    suspend fun printReceipt(receipt: Receipt): Result<Unit>
}
