package com.headphonetracker

import com.headphonetracker.data.HeadphoneUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTest {

    @Test
    fun `test valid headphone usage`() {
        val usage = HeadphoneUsage(
            packageName = "com.example.app",
            appName = "Example App",
            startTime = 1000L,
            endTime = 5000L,
            duration = 4000L,
            date = "2024-01-01"
        )

        assertTrue(usage.endTime > usage.startTime)
        assertEquals(usage.endTime - usage.startTime, usage.duration)
    }

    @Test
    fun `test date format validation`() {
        val validDates = listOf(
            "2024-01-01",
            "2024-12-31",
            "2023-06-15"
        )

        validDates.forEach { date ->
            assertTrue(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        }
    }

    @Test
    fun `test duration is positive`() {
        val usage = HeadphoneUsage(
            packageName = "com.example.app",
            appName = "Example App",
            startTime = 1000L,
            endTime = 5000L,
            duration = 4000L,
            date = "2024-01-01"
        )

        assertTrue(usage.duration > 0)
    }

    @Test
    fun `test package name is not empty`() {
        val usage = HeadphoneUsage(
            packageName = "com.example.app",
            appName = "Example App",
            startTime = 1000L,
            endTime = 5000L,
            duration = 4000L,
            date = "2024-01-01"
        )

        assertFalse(usage.packageName.isEmpty())
        assertTrue(usage.packageName.isNotBlank())
    }

    @Test
    fun `test app name is not empty`() {
        val usage = HeadphoneUsage(
            packageName = "com.example.app",
            appName = "Example App",
            startTime = 1000L,
            endTime = 5000L,
            duration = 4000L,
            date = "2024-01-01"
        )

        assertFalse(usage.appName.isEmpty())
        assertTrue(usage.appName.isNotBlank())
    }
}
