package com.zatiaras.pos.core.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class DateUtilsTest {

    private val testZoneId = ZoneId.systemDefault()

    @Test
    fun getStartOfDay_givenTimestamp_returnsMidnightOfThatDay() {
        // Arrange
        val dateString = "2026-02-21T14:30:15Z" // Example 2:30 PM UTC
        val instant = Instant.parse(dateString)
        val timestamp = instant.toEpochMilli()

        // Act
        val startOfDay = DateUtils.getStartOfDay(timestamp)

        // Assert
        val resultZonedDateTime = Instant.ofEpochMilli(startOfDay).atZone(testZoneId)
        assertEquals(0, resultZonedDateTime.hour)
        assertEquals(0, resultZonedDateTime.minute)
        assertEquals(0, resultZonedDateTime.second)
        assertEquals(0, resultZonedDateTime.nano)
        // Ensure same day (depends on local timezone where test is run, so we match the date from the timestamp's local equivalent)
        val expectedDate = instant.atZone(testZoneId).toLocalDate()
        assertEquals(expectedDate, resultZonedDateTime.toLocalDate())
    }

    @Test
    fun getEndOfDay_givenTimestamp_returns235959ofThatDay() {
        // Arrange
        val dateString = "2026-02-21T14:30:15Z"
        val instant = Instant.parse(dateString)
        val timestamp = instant.toEpochMilli()

        // Act
        val endOfDay = DateUtils.getEndOfDay(timestamp)

        // Assert
        val resultZonedDateTime = Instant.ofEpochMilli(endOfDay).atZone(testZoneId)
        assertEquals(23, resultZonedDateTime.hour)
        assertEquals(59, resultZonedDateTime.minute)
        assertEquals(59, resultZonedDateTime.second)
        // Ensure same day
        val expectedDate = instant.atZone(testZoneId).toLocalDate()
        assertEquals(expectedDate, resultZonedDateTime.toLocalDate())
    }

    @Test
    fun getTodayRange_returnsValid24HourRange() {
        // Act
        val (start, end) = DateUtils.getTodayRange()

        // Assert
        assertTrue("Start should be strictly less than End", start < end)
        
        // Exact 24 hours difference? Or 23/25 depending on DST, but typically 24 in ms
        val expectedDiffMs = 24 * 60 * 60 * 1000L
        val actualDiffMs = end - start
        
        // At minimum we can assert the time difference is somewhat exactly 1 day duration
        assertTrue("Difference should be around 24 hours", actualDiffMs == expectedDiffMs || actualDiffMs == 23 * 3600 * 1000L || actualDiffMs == 25 * 3600 * 1000L)
        
        val startZDT = Instant.ofEpochMilli(start).atZone(testZoneId)
        val endZDT = Instant.ofEpochMilli(end).atZone(testZoneId)

        assertEquals("Start time should be midnight", 0, startZDT.hour)
        assertEquals("End time should be midnight of the next day", 0, endZDT.hour)
        assertEquals("End date should be exactly one day after start date", startZDT.toLocalDate().plusDays(1), endZDT.toLocalDate())
    }

    @Test
    fun getYesterdayRange_returnsValid24HourRangeBeforeToday() {
        // Act
        val (start, end) = DateUtils.getYesterdayRange()

        // Assert
        assertTrue("Start should be less than End", start < end)
        
        val startZDT = Instant.ofEpochMilli(start).atZone(testZoneId)
        val endZDT = Instant.ofEpochMilli(end).atZone(testZoneId)

        assertEquals(0, startZDT.hour)
        assertEquals(0, endZDT.hour)
        assertEquals(startZDT.toLocalDate().plusDays(1), endZDT.toLocalDate())
    }

    @Test
    fun formatDateCompact_givenTimestamp_returnsCorrectFormat() {
        // Arrange
        // We need a specific time in the local timezone to ensure format doesn't shift
        val zonedDateTime = ZonedDateTime.of(2026, 2, 21, 14, 30, 0, 0, testZoneId)
        val timestamp = zonedDateTime.toInstant().toEpochMilli()

        // Act
        val formattedDate = DateUtils.formatDateCompact(timestamp)

        // Assert
        assertEquals("formatDateCompact should output yyyyMMdd", "20260221", formattedDate)
    }

    @Test
    fun formatDateReadable_givenTimestamp_returnsCorrectFormat() {
        // Arrange
        val zonedDateTime = ZonedDateTime.of(2026, 2, 21, 14, 30, 0, 0, testZoneId)
        val timestamp = zonedDateTime.toInstant().toEpochMilli()

        // Act
        val formattedDate = DateUtils.formatDateReadable(timestamp)

        // Assert
        // Given thatLOCALE_ID is likely Indonesian, "Feb" might be "Feb" or "Peb" depending on device, 
        // but typically "Feb" is standard. Let's just check length and components to avoid strict locale issues 
        // without mocking.
        assertTrue(formattedDate.contains("21"))
        assertTrue(formattedDate.contains("2026"))
        assertEquals("Expected 11 chars (e.g., '21 Feb 2026')", 11, formattedDate.length)
    }

    @Test
    fun formatDateTime_givenTimestamp_returnsCorrectFormat() {
        // Arrange
        val zonedDateTime = ZonedDateTime.of(2026, 2, 21, 14, 30, 0, 0, testZoneId)
        val timestamp = zonedDateTime.toInstant().toEpochMilli()

        // Act
        val formattedDateTime = DateUtils.formatDateTime(timestamp)

        // Assert
        assertTrue(formattedDateTime.contains("21"))
        assertTrue(formattedDateTime.contains("2026"))
        assertTrue(formattedDateTime.contains("14:30"))
        assertEquals("Expected length like '21 Feb 2026 14:30'", 17, formattedDateTime.length)
    }
}
