package com.headphonetracker.utils

import org.junit.Assert.*
import org.junit.Test

object TimeFormatter {
    fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m ${secs}s"
            minutes > 0 -> "${minutes}m ${secs}s"
            else -> "${secs}s"
        }
    }
    
    fun formatDurationShort(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
}

class TimeFormatterTest {
    
    @Test
    fun `test formatDuration with hours minutes and seconds`() {
        val millis = 3665000L // 1 hour, 1 minute, 5 seconds
        val result = TimeFormatter.formatDuration(millis)
        assertEquals("1h 1m 5s", result)
    }
    
    @Test
    fun `test formatDuration with only hours`() {
        val millis = 3600000L // 1 hour
        val result = TimeFormatter.formatDuration(millis)
        assertEquals("1h 0m 0s", result)
    }
    
    @Test
    fun `test formatDuration with only minutes`() {
        val millis = 120000L // 2 minutes
        val result = TimeFormatter.formatDuration(millis)
        assertEquals("0h 2m 0s", result)
    }
    
    @Test
    fun `test formatDuration with only seconds`() {
        val millis = 5000L // 5 seconds
        val result = TimeFormatter.formatDuration(millis)
        assertEquals("0h 0m 5s", result)
    }
    
    @Test
    fun `test formatDuration with zero`() {
        val millis = 0L
        val result = TimeFormatter.formatDuration(millis)
        assertEquals("0h 0m 0s", result)
    }
    
    @Test
    fun `test formatDurationShort with hours and minutes`() {
        val millis = 3660000L // 1 hour, 1 minute
        val result = TimeFormatter.formatDurationShort(millis)
        assertEquals("1h 1m", result)
    }
    
    @Test
    fun `test formatDurationShort with only minutes`() {
        val millis = 120000L // 2 minutes
        val result = TimeFormatter.formatDurationShort(millis)
        assertEquals("2m", result)
    }
    
    @Test
    fun `test formatDurationShort with only seconds`() {
        val millis = 5000L // 5 seconds
        val result = TimeFormatter.formatDurationShort(millis)
        assertEquals("5s", result)
    }
    
    @Test
    fun `test formatDuration with large duration`() {
        val millis = 86400000L // 24 hours
        val result = TimeFormatter.formatDuration(millis)
        assertEquals("24h 0m 0s", result)
    }
    
    @Test
    fun `test formatDuration with partial hours`() {
        val millis = 5400000L // 1.5 hours = 1h 30m
        val result = TimeFormatter.formatDuration(millis)
        assertEquals("1h 30m 0s", result)
    }
}

