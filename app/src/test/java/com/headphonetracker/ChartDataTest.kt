package com.headphonetracker

import com.headphonetracker.data.AppUsageSummary
import com.headphonetracker.data.DailyUsageSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartDataTest {

    @Test
    fun `test pie chart data calculation`() {
        val usageList = listOf(
            AppUsageSummary("com.app1", "App 1", 3600000L), // 1 hour
            AppUsageSummary("com.app2", "App 2", 1800000L), // 30 minutes
            AppUsageSummary("com.app3", "App 3", 1800000L) // 30 minutes
        )
        val totalDuration = 7200000L // 2 hours

        // Calculate percentages
        val percentages = usageList.map { (it.totalDuration * 100 / totalDuration).toInt() }

        assertEquals(50, percentages[0]) // 50%
        assertEquals(25, percentages[1]) // 25%
        assertEquals(25, percentages[2]) // 25%
        assertEquals(100, percentages.sum())
    }

    @Test
    fun `test bar chart data calculation`() {
        val dailyUsage = listOf(
            DailyUsageSummary("2024-01-01", 3600000L), // 1 hour
            DailyUsageSummary("2024-01-02", 7200000L), // 2 hours
            DailyUsageSummary("2024-01-03", 5400000L) // 1.5 hours
        )

        val hours = dailyUsage.map { it.totalDuration / (1000 * 60 * 60.0) }

        assertEquals(1.0, hours[0], 0.01)
        assertEquals(2.0, hours[1], 0.01)
        assertEquals(1.5, hours[2], 0.01)
    }

    @Test
    fun `test empty usage list`() {
        val usageList = emptyList<AppUsageSummary>()
        val totalDuration = 0L

        assertTrue(usageList.isEmpty())
        assertEquals(0L, totalDuration)
    }

    @Test
    fun `test single app usage`() {
        val usageList = listOf(
            AppUsageSummary("com.app1", "App 1", 3600000L)
        )
        val totalDuration = 3600000L

        val percentage = (usageList[0].totalDuration * 100 / totalDuration).toInt()
        assertEquals(100, percentage)
    }

    @Test
    fun `test usage sorting by duration`() {
        val usageList = listOf(
            AppUsageSummary("com.app1", "App 1", 1000L),
            AppUsageSummary("com.app2", "App 2", 3000L),
            AppUsageSummary("com.app3", "App 3", 2000L)
        )

        val sorted = usageList.sortedByDescending { it.totalDuration }

        assertEquals(3000L, sorted[0].totalDuration)
        assertEquals(2000L, sorted[1].totalDuration)
        assertEquals(1000L, sorted[2].totalDuration)
    }
}
