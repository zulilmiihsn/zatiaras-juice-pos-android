package com.zatiaras.pos.core.domain.model

/**
 * Universal time period filter for both reports and cash records.
 * 
 * This enum is shared across modules to maintain consistency
 * and follow DRY principle.
 */
enum class DatePeriod {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH,
    LAST_7_DAYS,
    LAST_30_DAYS,
    CUSTOM;
    
    /**
     * Get display name in Indonesian.
     */
    fun toDisplayName(): String = when (this) {
        TODAY -> "Hari Ini"
        YESTERDAY -> "Kemarin"
        THIS_WEEK -> "Minggu Ini"
        THIS_MONTH -> "Bulan Ini"
        LAST_7_DAYS -> "7 Hari Terakhir"
        LAST_30_DAYS -> "30 Hari Terakhir"
        CUSTOM -> "Kustom"
    }
}
