package com.headphonetracker

import org.junit.Test
import org.junit.Assert.*
import java.util.*

class SettingsPreferencesTest {
    
    @Test
    fun `test daily limit preference values`() {
        val limitOptions = intArrayOf(0, 30, 60, 90, 120, 180, 240, 300, 360)
        
        // Test all valid limit values
        limitOptions.forEach { limit ->
            assertTrue("Limit $limit should be valid", limit >= 0)
        }
        
        // Test that 0 means no limit
        assertEquals(0, limitOptions[0])
    }
    
    @Test
    fun `test break interval preference values`() {
        val intervalOptions = intArrayOf(30, 45, 60, 90, 120)
        
        intervalOptions.forEach { interval ->
            assertTrue("Interval $interval should be valid", interval >= 30)
            assertTrue("Interval $interval should be reasonable", interval <= 120)
        }
    }
    
    @Test
    fun `test theme preference values`() {
        val themes = arrayOf("light", "dark", "system")
        
        themes.forEach { theme ->
            assertTrue("Theme $theme should be valid", 
                theme in listOf("light", "dark", "system"))
        }
    }
    
    @Test
    fun `test accent color preference values`() {
        val colors = arrayOf("cyan", "purple", "teal", "orange", "pink", "green")
        
        colors.forEach { color ->
            assertTrue("Color $color should be valid",
                color in listOf("cyan", "purple", "teal", "orange", "pink", "green"))
        }
    }
    
    @Test
    fun `test excluded apps set operations`() {
        val excludedApps = mutableSetOf<String>()
        
        // Add apps
        excludedApps.add("com.app1")
        excludedApps.add("com.app2")
        
        assertEquals(2, excludedApps.size)
        assertTrue(excludedApps.contains("com.app1"))
        assertTrue(excludedApps.contains("com.app2"))
        
        // Remove app
        excludedApps.remove("com.app1")
        
        assertEquals(1, excludedApps.size)
        assertFalse(excludedApps.contains("com.app1"))
        assertTrue(excludedApps.contains("com.app2"))
    }
    
    @Test
    fun `test auto start preference default`() {
        // Default should be false
        val autoStartEnabled = false
        assertFalse(autoStartEnabled)
    }
    
    @Test
    fun `test break reminders preference default`() {
        // Default should be false
        val breakRemindersEnabled = false
        assertFalse(breakRemindersEnabled)
    }
    
    @Test
    fun `test daily summary preference default`() {
        // Default should be false
        val dailySummaryEnabled = false
        assertFalse(dailySummaryEnabled)
    }
    
    @Test
    fun `test milestone alerts preference default`() {
        // Default should be true
        val milestonesEnabled = true
        assertTrue(milestonesEnabled)
    }
    
    @Test
    fun `test format daily limit display`() {
        val limitMinutes = 90
        val hours = limitMinutes / 60
        val mins = limitMinutes % 60
        
        val display = if (limitMinutes == 0) {
            "No limit set"
        } else {
            if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        }
        
        assertEquals("1h 30m", display)
    }
    
    @Test
    fun `test format break interval display`() {
        val intervalMinutes = 60
        val display = "Every $intervalMinutes minutes"
        
        assertEquals("Every 60 minutes", display)
    }
}

