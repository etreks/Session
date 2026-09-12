package com.satyam.session.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Formatting and date math helpers used across screens. */
object TimeUtils {

    /** "01:23:45" — for the stopwatch display. */
    fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    /** "3h 42m" or "42m" — compact form for stats and session blocks. */
    fun formatDurationShort(ms: Long): String {
        val totalMinutes = ms / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    /** "2:30 PM" — for displaying session start/end times. */
    fun formatTime(epochMs: Long): String {
        val time = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(epochMs),
            ZoneId.systemDefault()
        ).toLocalTime()
        return time.format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    fun getStartOfDay(date: LocalDate = LocalDate.now()): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun getEndOfDay(date: LocalDate = LocalDate.now()): Long =
        date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun getStartOfWeek(date: LocalDate = LocalDate.now()): Long {
        val monday = date.with(DayOfWeek.MONDAY)
        return monday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getEndOfWeek(date: LocalDate = LocalDate.now()): Long {
        val sunday = date.with(DayOfWeek.SUNDAY)
        return sunday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /** "Today", "Yesterday", or "Sat, Sep 6". */
    fun formatDate(date: LocalDate): String {
        val today = LocalDate.now()
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            today.plusDays(1) -> "Tomorrow"
            else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        }
    }

    /** "Saturday, September 6, 2026". */
    fun formatDateFull(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))

    /**
     * Returns the fractional hour-of-day for an epoch timestamp.
     * e.g. 2:30 PM → 14.5f   Used to position session blocks on the timeline.
     */
    fun getHourOfDay(epochMs: Long): Float {
        val time = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(epochMs),
            ZoneId.systemDefault()
        ).toLocalTime()
        return time.hour + time.minute / 60f
    }
}
