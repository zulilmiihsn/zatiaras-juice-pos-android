package com.zatiaras.pos.feature.pos.presentation.receipt

import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.domain.model.TransactionItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptMapperTest {

    @Test
    fun `toReceipt maps transaction fields for printer boundary`() {
        val transaction = Transaction(
            id = "txn-1",
            transactionNumber = "TRX-001",
            items = listOf(
                TransactionItem(
                    id = "item-1",
                    productId = "product-1",
                    productName = "Juice",
                    productPrice = 12_000,
                    quantity = 2,
                    subtotal = 24_000,
                    notes = "less ice",
                ),
            ),
            subtotal = 24_000,
            discountAmount = 1_000,
            discountPercent = 5.0,
            taxAmount = 500,
            taxPercent = 2.0,
            grandTotal = 23_500,
            paymentMethod = PaymentMethod.CASH,
            amountPaid = 25_000,
            changeAmount = 1_500,
            notes = "take away",
            customerName = "Ayu",
            createdAt = 1_700_000_000_000,
            isSynced = false,
        )

        val receipt = transaction.toReceipt()

        assertEquals("txn-1", receipt.id)
        assertEquals("TRX-001", receipt.number)
        assertEquals("Tunai", receipt.paymentMethodName)
        assertEquals(1, receipt.items.size)
        assertEquals("Juice", receipt.items.first().productName)
        assertEquals(2, receipt.totalQuantity)
        assertEquals(23_500, receipt.grandTotal)
    }
}
