package com.zatiaras.pos.core.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class CurrencyFormatterTest {

    @Test
    fun formatCurrency_withSymbolAndSpace_formatsCorrectly() {
        val amount = 16000L
        val formatted = CurrencyFormatter.formatCurrency(amount, includeSymbol = true, addSpace = true)
        assertEquals("Rp 16.000", formatted)
    }

    @Test
    fun formatCurrency_withSymbolNoSpace_formatsCorrectly() {
        val amount = 16000L
        val formatted = CurrencyFormatter.formatCurrency(amount, includeSymbol = true, addSpace = false)
        assertEquals("Rp16.000", formatted)
    }

    @Test
    fun formatCurrency_noSymbol_formatsCorrectly() {
        val amount = 16000L
        val formatted = CurrencyFormatter.formatCurrency(amount, includeSymbol = false)
        assertEquals("16.000", formatted)
    }

    @Test
    fun formatIdr_aliasMethod_formatsCorrectly() {
        val amount = 16000L
        val formatted = CurrencyFormatter.formatIdr(amount)
        assertEquals("Rp 16.000", formatted)
    }

    @Test
    fun formatCurrency_doubleExtension_formatsCorrectly() {
        val amount = 16000.50
        val formatted = amount.toRupiah()
        // Our formatter chops off fraction digits
        assertEquals("Rp 16.000", formatted)
    }

    @Test
    fun formatNumber_noCurrencySymbol_formatsCorrectly() {
        val amount = 2500000L
        val formatted = CurrencyFormatter.formatNumber(amount)
        assertEquals("2.500.000", formatted)
    }

    @Test
    fun formatInput_givenRawString_formatsWithSeparatorAndNoSymbol() {
        val rawInput = "Rp 150000.x"
        val formatted = CurrencyFormatter.formatInput(rawInput)
        assertEquals("150.000", formatted)
    }

    @Test
    fun formatInput_givenEmptyOrInvalidString_returnsEmptyString() {
        val rawInput = "abc"
        val formatted = CurrencyFormatter.formatInput(rawInput)
        assertEquals("", formatted)
    }

    @Test
    fun parseToLong_givenFormattedString_returnsCorrectLong() {
        val formattedValue = "Rp 25.000.000"
        val parsed = CurrencyFormatter.parseToLong(formattedValue)
        assertEquals(25000000L, parsed)
    }

    @Test
    fun parseToLong_givenInvalidString_returnsNull() {
        val invalidValue = "Rp xxx"
        val parsed = CurrencyFormatter.parseToLong(invalidValue)
        assertNull(parsed)
    }

    @Test
    fun extensions_Long_toFormattedNumber_returnsCorrectString() {
        val amount = 12500L
        assertEquals("12.500", amount.toFormattedNumber())
    }

    @Test
    fun extensions_Int_toRupiah_returnsCorrectString() {
        val amount = 50000
        assertEquals("Rp 50.000", amount.toRupiah())
    }
}
