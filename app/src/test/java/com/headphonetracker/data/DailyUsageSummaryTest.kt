package com.headphonetracker.data

import org.junit.Assert.*
import org.junit.Test

class DailyUsageSummaryTest {
    
    @Test
    fun `test DailyUsageSummary creation`() {
        val summary = DailyUsageSummary(
            date = "2024-01-01",
            totalDuration = 7200000L // 2 hours in milliseconds
        )
        
        assertEquals("2024-01-01", summary.date)
        assertEquals(7200000L, summary.totalDuration)
    }
    
    @Test
    fun `test DailyUsageSummary with zero duration`() {
        val summary = DailyUsageSummary(
            date = "2024-01-01",
            totalDuration = 0L
        )
        
        assertEquals("2024-01-01", summary.date)
        assertEquals(0L, summary.totalDuration)
    }
    
    @Test
    fun `test DailyUsageSummary date format`() {
        val dates = listOf(
            "2024-01-01",
            "2024-12-31",
            "2023-06-15"
        )
        
        dates.forEach { date ->
            val summary = DailyUsageSummary(
                date = date,
                totalDuration = 1000L
            )
            assertEquals(date, summary.date)
        }
    }
}

