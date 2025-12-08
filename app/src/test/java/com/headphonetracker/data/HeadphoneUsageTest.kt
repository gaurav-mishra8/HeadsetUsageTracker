package com.headphonetracker.data

import org.junit.Assert.*
import org.junit.Test

class HeadphoneUsageTest {
    
    @Test
    fun `test HeadphoneUsage data class creation`() {
        val usage = HeadphoneUsage(
            id = 1L,
            packageName = "com.example.app",
            appName = "Example App",
            startTime = 1000L,
            endTime = 5000L,
            duration = 4000L,
            date = "2024-01-01"
        )
        
        assertEquals(1L, usage.id)
        assertEquals("com.example.app", usage.packageName)
        assertEquals("Example App", usage.appName)
        assertEquals(1000L, usage.startTime)
        assertEquals(5000L, usage.endTime)
        assertEquals(4000L, usage.duration)
        assertEquals("2024-01-01", usage.date)
    }
    
    @Test
    fun `test HeadphoneUsage with default id`() {
        val usage = HeadphoneUsage(
            packageName = "com.example.app",
            appName = "Example App",
            startTime = 1000L,
            endTime = 5000L,
            duration = 4000L,
            date = "2024-01-01"
        )
        
        assertEquals(0L, usage.id)
    }
    
    @Test
    fun `test HeadphoneUsage duration calculation`() {
        val startTime = 1000L
        val endTime = 5000L
        val expectedDuration = 4000L
        
        val usage = HeadphoneUsage(
            packageName = "com.example.app",
            appName = "Example App",
            startTime = startTime,
            endTime = endTime,
            duration = expectedDuration,
            date = "2024-01-01"
        )
        
        assertEquals(expectedDuration, usage.duration)
        assertEquals(endTime - startTime, usage.duration)
    }
}

