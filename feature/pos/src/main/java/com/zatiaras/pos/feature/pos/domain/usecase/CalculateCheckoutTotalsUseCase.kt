package com.zatiaras.pos.feature.pos.domain.usecase

import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import javax.inject.Inject

data class CheckoutCalculationResult(
    val subtotal: Long,
    val discountAmount: Long,
    val taxAmount: Long,
    val grandTotal: Long,
    val changeAmount: Long,
    val amountPaidParsed: Long
)

class CalculateCheckoutTotalsUseCase @Inject constructor() {
    operator fun invoke(
        subtotal: Long,
        discountPercent: Double,
        taxPercent: Double,
        amountPaidStr: String,
        paymentMethod: PaymentMethod
    ): CheckoutCalculationResult {
        val discountAmount = (subtotal * discountPercent / 100).toLong()
        val afterDiscount = subtotal - discountAmount
        val taxAmount = (afterDiscount * taxPercent / 100).toLong()
        val grandTotal = afterDiscount + taxAmount
        
        val paid = amountPaidStr.filter { it.isDigit() }.toLongOrNull() ?: 0
        val changeAmount = if (paymentMethod == PaymentMethod.CASH && paid > grandTotal) {
            paid - grandTotal
        } else 0
        
        return CheckoutCalculationResult(
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            grandTotal = grandTotal,
            changeAmount = changeAmount,
            amountPaidParsed = paid
        )
    }
}
