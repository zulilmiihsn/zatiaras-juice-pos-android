package com.zatiaras.pos.core.domain.util

import java.util.Locale

/**
 * Centralized locale configuration for the application.
 * 
 * All formatters, date/time displays, and number formatting should use
 * this constant to ensure consistency across the app.
 */
object LocaleUtils {
    /**
     * Indonesian locale (Bahasa Indonesia).
     */
    val LOCALE_ID: Locale = Locale("id", "ID")
}
