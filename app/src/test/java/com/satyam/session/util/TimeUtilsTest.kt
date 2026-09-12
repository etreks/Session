package com.satyam.session.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class TimeUtilsTest {

    @Test
    fun testFormatDuration_zero() {
        assertEquals("00:00:00", TimeUtils.formatDuration(0))
    }

    @Test
    fun testFormatDuration_seconds() {
        assertEquals("00:00:45", TimeUtils.formatDuration(45_000))
    }

    @Test
    fun testFormatDuration_minutesAndSeconds() {
        assertEquals("00:05:30", TimeUtils.formatDuration(330_000))
    }

    @Test
    fun testFormatDuration_hoursMinutesSeconds() {
        assertEquals("02:15:20", TimeUtils.formatDuration(8_120_000))
    }

    @Test
    fun testFormatDurationShort_lessThanOneMinute() {
        assertEquals("<1m", TimeUtils.formatDurationShort(30_000))
    }

    @Test
    fun testFormatDurationShort_minutesOnly() {
        assertEquals("45m", TimeUtils.formatDurationShort(45 * 60_000L))
    }

    @Test
    fun testFormatDurationShort_hoursOnly() {
        assertEquals("2h", TimeUtils.formatDurationShort(2 * 3600_000L))
    }

    @Test
    fun testFormatDurationShort_hoursAndMinutes() {
        assertEquals("3h 25m", TimeUtils.formatDurationShort((3 * 3600 + 25 * 60) * 1000L))
    }

    @Test
    fun testFormatDate_todayYesterdayTomorrow() {
        val today = LocalDate.now()
        assertEquals("Today", TimeUtils.formatDate(today))
        assertEquals("Yesterday", TimeUtils.formatDate(today.minusDays(1)))
        assertEquals("Tomorrow", TimeUtils.formatDate(today.plusDays(1)))
    }

    @Test
    fun testDayBoundaries() {
        val today = LocalDate.now()
        val start = TimeUtils.getStartOfDay(today)
        val end = TimeUtils.getEndOfDay(today)
        assertTrue(end > start)
        assertEquals(24 * 3600 * 1000L, end - start)
    }

    @Test
    fun testWeekBoundaries() {
        val today = LocalDate.now()
        val start = TimeUtils.getStartOfWeek(today)
        val end = TimeUtils.getEndOfWeek(today)
        assertTrue(end > start)
        assertEquals(7 * 24 * 3600 * 1000L, end - start)
    }

    @Test
    fun testGetHourOfDay() {
        val dateTime = LocalDateTime.of(2026, 9, 12, 14, 30)
        val epochMs = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val hour = TimeUtils.getHourOfDay(epochMs)
        assertEquals(14.5f, hour, 0.01f)
    }
}
