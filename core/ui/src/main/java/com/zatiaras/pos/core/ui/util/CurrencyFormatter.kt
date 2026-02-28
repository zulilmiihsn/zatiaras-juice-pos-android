package com.zatiaras.pos.core.ui.util

import com.zatiaras.pos.core.domain.util.LocaleUtils
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/**
 * Utility object for formatting currency values in Indonesian Rupiah format.
 * 
 * Indonesian format uses:
 * - Period (.) as thousands separator
 * - Comma (,) as decimal separator
 * - "Rp" as currency symbol
 * 
 * Example: 16000 -> "Rp 16.000"
 */
object CurrencyFormatter {
    
    private val indonesiaLocale = LocaleUtils.LOCALE_ID
    
    private val currencyFormat: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(indonesiaLocale).apply {
            maximumFractionDigits = 0
        }
    }
    
    private val numberFormat: NumberFormat by lazy {
        NumberFormat.getNumberInstance(indonesiaLocale)
    }
    
    private val decimalSymbols: DecimalFormatSymbols by lazy {
        DecimalFormatSymbols(indonesiaLocale).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
    }
    
    private val inputDecimalFormat: DecimalFormat by lazy {
        DecimalFormat("#,###", decimalSymbols)
    }
    
    /**
     * Format a number as Indonesian Rupiah currency.
     * 
     * @param amount The amount to format
     * @param includeSymbol Whether to include "Rp" prefix
     * @param addSpace Whether to add space after "Rp" (default: true)
     * @return Formatted currency string
     * 
     * Example:
     * - formatCurrency(16000) -> "Rp 16.000"
     * - formatCurrency(16000, includeSymbol = false) -> "16.000"
     */
    fun formatCurrency(amount: Long, includeSymbol: Boolean = true, addSpace: Boolean = true): String {
        val formatted = numberFormat.format(amount)
        return if (includeSymbol) {
            if (addSpace) "Rp $formatted" else "Rp$formatted"
        } else {
            formatted
        }
    }

    /**
     * Alias for formatCurrency to match usage in the project.
     */
    fun formatIdr(amount: Long): String = formatCurrency(amount)
    
    /**
     * Format a number as Indonesian Rupiah currency (Double version).
     */
    fun formatCurrency(amount: Double, includeSymbol: Boolean = true, addSpace: Boolean = true): String {
        return formatCurrency(amount.toLong(), includeSymbol, addSpace)
    }

    /**
     * Alias for formatCurrency to match usage in the project.
     */
    fun formatIdr(amount: Double): String = formatCurrency(amount.toLong())
    
    /**
     * Format a number as Indonesian Rupiah currency (Int version).
     */
    fun formatCurrency(amount: Int, includeSymbol: Boolean = true, addSpace: Boolean = true): String {
        return formatCurrency(amount.toLong(), includeSymbol, addSpace)
    }

    /**
     * Alias for formatCurrency to match usage in the project.
     */
    fun formatIdr(amount: Int): String = formatCurrency(amount.toLong())
    
    /**
     * Format a number with thousands separator (without currency symbol).
     * Useful for input field display.
     * 
     * @param amount The amount to format
     * @return Formatted number string with period as thousands separator
     * 
     * Example: formatNumber(16000) -> "16.000"
     */
    fun formatNumber(amount: Long): String {
        return numberFormat.format(amount)
    }
    
    /**
     * Format input text with thousands separator as user types.
     * Only keeps digits and formats them.
     * 
     * @param input The raw input string (may contain non-digit characters)
     * @return Formatted number string
     * 
     * Example: formatInput("16000") -> "16.000"
     */
    fun formatInput(input: String): String {
        val digits = input.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        
        val number = digits.toLongOrNull() ?: return ""
        return inputDecimalFormat.format(number)
    }
    
    /**
     * Parse a formatted currency/number string back to Long.
     * Removes all non-digit characters.
     * 
     * @param formattedValue The formatted string (e.g., "Rp 16.000" or "16.000")
     * @return The numeric value, or null if parsing fails
     * 
     * Example: parseToLong("16.000") -> 16000
     */
    fun parseToLong(formattedValue: String): Long? {
        val digits = formattedValue.filter { it.isDigit() }
        return digits.toLongOrNull()
    }
    
    /**
     * Get a NumberFormat instance configured for Indonesian Rupiah.
     * Use this when you need more control over formatting.
     */
    fun getCurrencyFormatter(): NumberFormat {
        return NumberFormat.getCurrencyInstance(indonesiaLocale).apply {
            maximumFractionDigits = 0
        }
    }
    
    /**
     * Get a NumberFormat instance for number formatting (no currency symbol).
     */
    fun getNumberFormatter(): NumberFormat {
        return NumberFormat.getNumberInstance(indonesiaLocale)
    }
}

/**
 * Extension function to format Long as Rupiah currency.
 */
fun Long.toRupiah(includeSymbol: Boolean = true): String {
    return CurrencyFormatter.formatCurrency(this, includeSymbol)
}

/**
 * Extension function to format Int as Rupiah currency.
 */
fun Int.toRupiah(includeSymbol: Boolean = true): String {
    return CurrencyFormatter.formatCurrency(this.toLong(), includeSymbol)
}

/**
 * Extension function to format Double as Rupiah currency.
 */
fun Double.toRupiah(includeSymbol: Boolean = true): String {
    return CurrencyFormatter.formatCurrency(this.toLong(), includeSymbol)
}

/**
 * Extension function to format Long with thousands separator (no currency symbol).
 */
fun Long.toFormattedNumber(): String {
    return CurrencyFormatter.formatNumber(this)
}
