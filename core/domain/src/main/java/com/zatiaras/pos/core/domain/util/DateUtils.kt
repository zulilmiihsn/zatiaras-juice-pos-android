package com.zatiaras.pos.core.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utility object for common date/time calculations.
 *
 * Centralizes date logic to avoid duplication across repositories.
 * All timestamps are in milliseconds (Unix epoch).
 *
 * Migrated to java.time API (Desugaring supported).
 * Ranges are generally [Inclusive, Exclusive) to match SQL '>= AND <' queries.
 */
object DateUtils {

    private val ZONE_ID = ZoneId.systemDefault()
    private val LOCALE_ID = LocaleUtils.LOCALE_ID

    /**
     * Get the start of day (00:00:00.000) for a given timestamp.
     *
     * @param timestamp Timestamp in milliseconds (defaults to now)
     * @return Timestamp at start of that day
     */
    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZONE_ID)
            .toLocalDate()
            .atStartOfDay(ZONE_ID)
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Get the end of day (23:59:59.999) for a given timestamp.
     * Use this for UI display or inclusive ranges, but prefer Exclusive End for queries.
     *
     * @param timestamp Timestamp in milliseconds (defaults to now)
     * @return Timestamp at end of that day
     */
    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZONE_ID)
            .toLocalDate()
            .atTime(LocalTime.MAX)
            .atZone(ZONE_ID)
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Get today's date range as a pair of (startOfDay, startOfNextDay).
     *
     * @return Pair of (startOfDay, startOfNextDay) - Exclusive End
     */
    fun getTodayRange(): Pair<Long, Long> {
        val today = LocalDate.now(ZONE_ID)
        val start = today.atStartOfDay(ZONE_ID).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(ZONE_ID).toInstant().toEpochMilli()
        return start to end
    }

    /**
     * Get yesterday's date range.
     *
     * @return Pair of (startOfYesterday, startOfToday) - Exclusive End
     */
    fun getYesterdayRange(): Pair<Long, Long> {
        val today = LocalDate.now(ZONE_ID)
        val yesterday = today.minusDays(1)
        val start = yesterday.atStartOfDay(ZONE_ID).toInstant().toEpochMilli()
        val end = today.atStartOfDay(ZONE_ID).toInstant().toEpochMilli()
        return start to end
    }

    /**
     * Get this week's date range (Monday to End of Today).
     *
     * @return Pair of (startOfWeek, startOfTomorrow) - Exclusive End
     */
    fun getThisWeekRange(): Pair<Long, Long> {
        val today = LocalDate.now(ZONE_ID)
        // Go to Monday
        val monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        
        val start = monday.atStartOfDay(ZONE_ID).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(ZONE_ID).toInstant().toEpochMilli()
        
        return start to end
    }

    /**
     * Get this month's date range (1st to End of Today).
     *
     * @return Pair of (startOfMonth, startOfTomorrow) - Exclusive End
     */
    fun getThisMonthRange(): Pair<Long, Long> {
        val today = LocalDate.now(ZONE_ID)
        val firstOfMonth = today.withDayOfMonth(1)
        
        val start = firstOfMonth.atStartOfDay(ZONE_ID).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(ZONE_ID).toInstant().toEpochMilli()
        
        return start to end
    }

    /**
     * Get date range for N days ago until End of Today.
     *
     * @param days Number of days to look back
     * @return Pair of (startDate, startOfTomorrow)
     */
    fun getLastNDaysRange(days: Int): Pair<Long, Long> {
        val today = LocalDate.now(ZONE_ID)
        val startDate = today.minusDays((days - 1).toLong())
        
        val start = startDate.atStartOfDay(ZONE_ID).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(ZONE_ID).toInstant().toEpochMilli()
        
        return start to end
    }

    /**
     * Get previous week's date range (Monday to Sunday of last week).
     * Used for comparing this week vs last week.
     *
     * @return Pair of (startOfPreviousMonday, startOfThisMonday) - Exclusive End
     */
    fun getPreviousWeekRange(): Pair<Long, Long> {
        val today = LocalDate.now(ZONE_ID)
        val thisMonday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val previousMonday = thisMonday.minusWeeks(1)
        
        val start = previousMonday.atStartOfDay(ZONE_ID).toInstant().toEpochMilli()
        val end = thisMonday.atStartOfDay(ZONE_ID).toInstant().toEpochMilli()
        
        return start to end
    }

    /**
     * Format a timestamp to date string (yyyyMMdd).
     * Used for transaction numbers.
     *
     * @param timestamp Timestamp in milliseconds
     * @return Formatted date string
     */
    fun formatDateCompact(timestamp: Long = System.currentTimeMillis()): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.getDefault())
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZONE_ID)
            .format(formatter)
    }

    /**
     * Format a timestamp to readable date (dd MMM yyyy).
     *
     * @param timestamp Timestamp in milliseconds
     * @return Formatted date string (e.g., "14 Jan 2026")
     */
    fun formatDateReadable(timestamp: Long): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", LOCALE_ID)
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZONE_ID)
            .format(formatter)
    }

    /**
     * Format a timestamp to readable date and time.
     *
     * @param timestamp Timestamp in milliseconds
     * @return Formatted string (e.g., "14 Jan 2026 03:10")
     */
    fun formatDateTime(timestamp: Long): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", LOCALE_ID)
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZONE_ID)
            .format(formatter)
    }
}
