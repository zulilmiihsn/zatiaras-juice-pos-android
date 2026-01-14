package com.zatiaras.pos.core.domain.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Utility object for common date/time calculations.
 * 
 * Centralizes date logic to avoid duplication across repositories.
 * All timestamps are in milliseconds (Unix epoch).
 */
object DateUtils {

    /**
     * Get the start of day (00:00:00.000) for a given timestamp.
     * 
     * @param timestamp Timestamp in milliseconds (defaults to now)
     * @return Timestamp at start of that day
     */
    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Get the end of day (23:59:59.999) for a given timestamp.
     * 
     * @param timestamp Timestamp in milliseconds (defaults to now)
     * @return Timestamp at end of that day
     */
    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    /**
     * Get today's date range as a pair of (startOfDay, endOfDay).
     * 
     * Note: endOfDay here is start of NEXT day (exclusive range for queries).
     * This matches the original behavior in TransactionRepositoryImpl.
     * 
     * @return Pair of (startOfDay, startOfNextDay)
     */
    fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        
        return startOfDay to endOfDay
    }

    /**
     * Get this week's date range (Monday to now).
     * 
     * @return Pair of (startOfWeek, now)
     */
    fun getThisWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        // Go to Monday
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val weekStart = getStartOfDay(calendar.timeInMillis)
        
        return weekStart to getEndOfDay(now)
    }

    /**
     * Get this month's date range (1st to now).
     * 
     * @return Pair of (startOfMonth, now)
     */
    fun getThisMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        // Go to 1st of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = getStartOfDay(calendar.timeInMillis)
        
        return monthStart to getEndOfDay(now)
    }

    /**
     * Get date range for N days ago until now.
     * 
     * @param days Number of days to look back
     * @return Pair of (startDate, endDate)
     */
    fun getLastNDaysRange(days: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = getEndOfDay(calendar.timeInMillis)
        
        calendar.add(Calendar.DAY_OF_YEAR, -(days - 1))
        val startDate = getStartOfDay(calendar.timeInMillis)
        
        return startDate to endDate
    }

    /**
     * Get previous week's date range (for comparison).
     * 
     * @return Pair of (startDate, endDate) for 7 days ago
     */
    fun getPreviousWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        
        val start = getStartOfDay(calendar.timeInMillis)
        val end = getEndOfDay(calendar.timeInMillis)
        
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
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return dateFormat.format(timestamp)
    }

    /**
     * Format a timestamp to readable date (dd MMM yyyy).
     * 
     * @param timestamp Timestamp in milliseconds
     * @return Formatted date string (e.g., "14 Jan 2026")
     */
    fun formatDateReadable(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        return dateFormat.format(timestamp)
    }

    /**
     * Format a timestamp to readable date and time.
     * 
     * @param timestamp Timestamp in milliseconds
     * @return Formatted string (e.g., "14 Jan 2026 03:10")
     */
    fun formatDateTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))
        return dateFormat.format(timestamp)
    }
}
