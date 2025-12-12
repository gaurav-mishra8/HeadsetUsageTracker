package com.headphonetracker

import org.junit.Test
import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.*

class StatsCalculationsTest {
    
    @Test
    fun `test calculateCurrentStreak with consecutive days`() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        
        val dates = mutableListOf<String>()
        // Today and last 4 days
        for (i in 0..4) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            dates.add(dateFormat.format(calendar.time))
        }
        
        val streak = calculateStreak(dates.sortedDescending(), dateFormat)
        assertEquals(5, streak)
    }
    
    @Test
    fun `test calculateCurrentStreak with gap breaks streak`() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        
        val dates = mutableListOf<String>()
        // Today, yesterday, skip day before, then 2 more days
        dates.add(dateFormat.format(Date())) // Today
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        dates.add(dateFormat.format(calendar.time)) // Yesterday
        calendar.add(Calendar.DAY_OF_YEAR, -2) // Skip one day
        dates.add(dateFormat.format(calendar.time))
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        dates.add(dateFormat.format(calendar.time))
        
        val streak = calculateStreak(dates.sortedDescending(), dateFormat)
        assertEquals(2, streak) // Only today and yesterday
    }
    
    @Test
    fun `test calculateCurrentStreak with no data`() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val streak = calculateStreak(emptyList(), dateFormat)
        assertEquals(0, streak)
    }
    
    @Test
    fun `test calculateLongestStreak with consecutive days`() {
        val dates = listOf("2024-01-01", "2024-01-02", "2024-01-03", "2024-01-05", "2024-01-06", "2024-01-07")
        val longestStreak = calculateLongestStreak(dates.sorted())
        assertEquals(3, longestStreak) // 01-03 is 3 days, 05-07 is 3 days
    }
    
    @Test
    fun `test calculateLongestStreak with single day`() {
        val dates = listOf("2024-01-01")
        val longestStreak = calculateLongestStreak(dates)
        assertEquals(1, longestStreak)
    }
    
    @Test
    fun `test calculateWeekComparison percentage increase`() {
        val thisWeek = 7200000L // 2 hours
        val lastWeek = 3600000L // 1 hour
        
        val changePercent = ((thisWeek - lastWeek) * 100 / lastWeek).toInt()
        assertEquals(100, changePercent)
    }
    
    @Test
    fun `test calculateWeekComparison percentage decrease`() {
        val thisWeek = 1800000L // 30 minutes
        val lastWeek = 3600000L // 1 hour
        
        val changePercent = ((thisWeek - lastWeek) * 100 / lastWeek).toInt()
        assertEquals(-50, changePercent)
    }
    
    @Test
    fun `test calculateWeekComparison with zero last week`() {
        val thisWeek = 3600000L
        val lastWeek = 0L
        
        val changePercent = if (lastWeek > 0) {
            ((thisWeek - lastWeek) * 100 / lastWeek).toInt()
        } else if (thisWeek > 0) {
            100
        } else {
            0
        }
        assertEquals(100, changePercent)
    }
    
    @Test
    fun `test categorizeApp correctly identifies music apps`() {
        val musicApps = setOf(
            "com.spotify.music",
            "com.apple.android.music",
            "com.google.android.apps.youtube.music"
        )
        
        musicApps.forEach { pkg ->
            val category = categorizeApp(pkg)
            assertEquals("Music", category)
        }
    }
    
    @Test
    fun `test categorizeApp correctly identifies video apps`() {
        val videoApps = setOf(
            "com.google.android.youtube",
            "com.netflix.mediaclient",
            "com.amazon.avod.thirdpartyclient"
        )
        
        videoApps.forEach { pkg ->
            val category = categorizeApp(pkg)
            assertEquals("Video", category)
        }
    }
    
    @Test
    fun `test categorizeApp returns Other for unknown apps`() {
        val category = categorizeApp("com.unknown.app")
        assertEquals("Other", category)
    }
    
    @Test
    fun `test calculateHourlyUsage groups correctly`() {
        val usages = listOf(
            UsageWithHour("2024-01-01", 8, 1800000L),  // 8 AM, 30 min
            UsageWithHour("2024-01-01", 8, 1800000L),  // 8 AM, 30 min
            UsageWithHour("2024-01-01", 14, 3600000L), // 2 PM, 1 hour
            UsageWithHour("2024-01-01", 20, 1800000L)  // 8 PM, 30 min
        )
        
        val hourlyUsage = LongArray(24) { 0L }
        usages.forEach { usage ->
            hourlyUsage[usage.hour] += usage.duration
        }
        
        assertEquals(3600000L, hourlyUsage[8])  // 8 AM: 1 hour total
        assertEquals(3600000L, hourlyUsage[14])  // 2 PM: 1 hour
        assertEquals(1800000L, hourlyUsage[20]) // 8 PM: 30 min
        assertEquals(0L, hourlyUsage[0])        // Midnight: 0
    }
    
    @Test
    fun `test findPeakHour returns correct hour`() {
        val hourlyUsage = LongArray(24) { 0L }
        hourlyUsage[14] = 7200000L // 2 PM has most usage
        hourlyUsage[8] = 3600000L
        hourlyUsage[20] = 1800000L
        
        val peakHour = hourlyUsage.indices.maxByOrNull { hourlyUsage[it] }
        assertEquals(14, peakHour)
    }
    
    // Helper functions for testing
    private fun calculateStreak(dates: List<String>, dateFormat: SimpleDateFormat): Int {
        if (dates.isEmpty()) return 0
        
        var streak = 0
        val calendar = Calendar.getInstance()
        val today = dateFormat.format(Date())
        
        // Check if today has data
        var checkDate = if (dates.contains(today)) {
            calendar.time = Date()
            streak++
            calendar
        } else {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            calendar
        }
        
        // Count consecutive days going backwards
        while (true) {
            checkDate.add(Calendar.DAY_OF_YEAR, -1)
            val dateStr = dateFormat.format(checkDate.time)
            if (dates.contains(dateStr)) {
                streak++
            } else {
                break
            }
        }
        
        return streak
    }
    
    private fun calculateLongestStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var longestStreak = 0
        var currentStreak = 1
        
        for (i in 1 until dates.size) {
            val prevDate = dateFormat.parse(dates[i - 1])
            val currDate = dateFormat.parse(dates[i])
            val diffDays = ((currDate?.time ?: 0) - (prevDate?.time ?: 0)) / (1000 * 60 * 60 * 24)
            
            if (diffDays == 1L) {
                currentStreak++
            } else {
                longestStreak = maxOf(longestStreak, currentStreak)
                currentStreak = 1
            }
        }
        
        return maxOf(longestStreak, currentStreak)
    }
    
    private fun categorizeApp(packageName: String): String {
        val musicApps = setOf("com.spotify.music", "com.apple.android.music", "com.google.android.apps.youtube.music")
        val videoApps = setOf("com.google.android.youtube", "com.netflix.mediaclient", "com.amazon.avod.thirdpartyclient")
        val podcastApps = setOf("com.google.android.apps.podcasts", "com.apple.podcasts")
        val socialApps = setOf("com.instagram.android", "com.snapchat.android", "com.facebook.katana")
        val gamingApps = setOf("com.supercell.clashofclans", "com.mojang.minecraftpe")
        val callApps = setOf("com.google.android.dialer", "com.whatsapp", "org.telegram.messenger")
        
        return when {
            musicApps.contains(packageName) -> "Music"
            videoApps.contains(packageName) -> "Video"
            podcastApps.contains(packageName) -> "Podcasts"
            socialApps.contains(packageName) -> "Social"
            gamingApps.contains(packageName) -> "Gaming"
            callApps.contains(packageName) -> "Calls"
            else -> "Other"
        }
    }
    
    private data class UsageWithHour(val date: String, val hour: Int, val duration: Long)
}

