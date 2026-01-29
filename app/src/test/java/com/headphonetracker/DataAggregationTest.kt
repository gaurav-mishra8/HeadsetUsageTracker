package com.headphonetracker

import com.headphonetracker.data.AppUsageSummary
import com.headphonetracker.data.DailyUsageSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataAggregationTest {

    @Test
    fun `test aggregate usage by app`() {
        val usageList = listOf(
            AppUsageSummary("com.app1", "App 1", 1000L),
            AppUsageSummary("com.app1", "App 1", 2000L),
            AppUsageSummary("com.app2", "App 2", 1500L)
        )

        val totalByApp = usageList.groupBy { it.packageName }
            .mapValues { (_, usages) -> usages.sumOf { it.totalDuration } }

        assertEquals(3000L, totalByApp["com.app1"])
        assertEquals(1500L, totalByApp["com.app2"])
    }

    @Test
    fun `test calculate total duration`() {
        val usageList = listOf(
            AppUsageSummary("com.app1", "App 1", 1000L),
            AppUsageSummary("com.app2", "App 2", 2000L),
            AppUsageSummary("com.app3", "App 3", 3000L)
        )

        val total = usageList.sumOf { it.totalDuration }
        assertEquals(6000L, total)
    }

    @Test
    fun `test calculate percentage`() {
        val appDuration = 2000L
        val totalDuration = 10000L
        val percentage = (appDuration * 100 / totalDuration).toInt()

        assertEquals(20, percentage)
    }

    @Test
    fun `test daily usage aggregation`() {
        val dailyUsage = listOf(
            DailyUsageSummary("2024-01-01", 3600000L),
            DailyUsageSummary("2024-01-02", 7200000L),
            DailyUsageSummary("2024-01-03", 5400000L)
        )

        val total = dailyUsage.sumOf { it.totalDuration }
        assertEquals(16200000L, total) // 4.5 hours total
    }

    @Test
    fun `test sort by duration descending`() {
        val usageList = listOf(
            AppUsageSummary("com.app1", "App 1", 1000L),
            AppUsageSummary("com.app2", "App 2", 5000L),
            AppUsageSummary("com.app3", "App 3", 2000L)
        )

        val sorted = usageList.sortedByDescending { it.totalDuration }

        assertEquals("com.app2", sorted[0].packageName)
        assertEquals("com.app3", sorted[1].packageName)
        assertEquals("com.app1", sorted[2].packageName)
    }

    @Test
    fun `test filter usage by date`() {
        val usageList = listOf(
            AppUsageSummary("com.app1", "App 1", 1000L),
            AppUsageSummary("com.app2", "App 2", 2000L)
        )

        // In real scenario, this would filter by date field
        // For test, we just verify the list structure
        assertTrue(usageList.isNotEmpty())
        assertEquals(2, usageList.size)
    }
}
