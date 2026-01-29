package com.headphonetracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppUsageSummaryTest {

    @Test
    fun `test AppUsageSummary creation`() {
        val summary = AppUsageSummary(
            packageName = "com.example.app",
            appName = "Example App",
            totalDuration = 3600000L // 1 hour in milliseconds
        )

        assertEquals("com.example.app", summary.packageName)
        assertEquals("Example App", summary.appName)
        assertEquals(3600000L, summary.totalDuration)
    }

    @Test
    fun `test AppUsageSummary with zero duration`() {
        val summary = AppUsageSummary(
            packageName = "com.example.app",
            appName = "Example App",
            totalDuration = 0L
        )

        assertEquals(0L, summary.totalDuration)
    }

    @Test
    fun `test AppUsageSummary with large duration`() {
        val largeDuration = 86400000L // 24 hours in milliseconds
        val summary = AppUsageSummary(
            packageName = "com.example.app",
            appName = "Example App",
            totalDuration = largeDuration
        )

        assertEquals(largeDuration, summary.totalDuration)
    }
}
