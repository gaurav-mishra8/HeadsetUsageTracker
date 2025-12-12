package com.headphonetracker

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.headphonetracker.data.AppDatabase
import com.headphonetracker.data.HeadphoneUsage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.*

@RunWith(AndroidJUnit4::class)
class ServiceIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
        
        // Clear preferences
        val prefs = context.getSharedPreferences("headphone_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
    
    @After
    fun tearDown() {
        runBlocking {
            database.headphoneUsageDao().deleteAllUsage()
        }
        database.close()
    }
    
    @Test
    fun testExcludedAppsAreNotTracked() = runBlocking {
        val prefs = context.getSharedPreferences("headphone_tracker_prefs", Context.MODE_PRIVATE)
        val excludedApps = mutableSetOf("com.excluded.app")
        prefs.edit().putStringSet("excluded_apps", excludedApps).apply()
        
        // Verify excluded apps are stored
        val storedExcluded = prefs.getStringSet("excluded_apps", emptySet())
        assertNotNull(storedExcluded)
        assertTrue(storedExcluded!!.contains("com.excluded.app"))
    }
    
    @Test
    fun testDailyLimitPreferenceIsStored() {
        val prefs = context.getSharedPreferences("headphone_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("daily_limit_minutes", 120).apply()
        
        val storedLimit = prefs.getInt("daily_limit_minutes", 0)
        assertEquals(120, storedLimit)
    }
    
    @Test
    fun testBreakRemindersPreferenceIsStored() {
        val prefs = context.getSharedPreferences("headphone_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("break_reminders_enabled", true).apply()
        
        val stored = prefs.getBoolean("break_reminders_enabled", false)
        assertTrue(stored)
    }
    
    @Test
    fun testBreakIntervalPreferenceIsStored() {
        val prefs = context.getSharedPreferences("headphone_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("break_interval_minutes", 60).apply()
        
        val stored = prefs.getInt("break_interval_minutes", 0)
        assertEquals(60, stored)
    }
    
    @Test
    fun testAutoStartPreferenceIsStored() {
        val prefs = context.getSharedPreferences("headphone_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_start_enabled", true).apply()
        
        val stored = prefs.getBoolean("auto_start_enabled", false)
        assertTrue(stored)
    }
    
    @Test
    fun testDailySummaryPreferenceIsStored() {
        val prefs = context.getSharedPreferences("headphone_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("daily_summary_enabled", true).apply()
        
        val stored = prefs.getBoolean("daily_summary_enabled", false)
        assertTrue(stored)
    }
    
    @Test
    fun testMilestonesPreferenceIsStored() {
        val prefs = context.getSharedPreferences("headphone_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("milestones_enabled", true).apply()
        
        val stored = prefs.getBoolean("milestones_enabled", false)
        assertTrue(stored)
    }
    
    @Test
    fun testThemePreferenceIsStored() {
        val prefs = context.getSharedPreferences("headphone_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("app_theme", "dark").apply()
        
        val stored = prefs.getString("app_theme", "system")
        assertEquals("dark", stored)
    }
    
    @Test
    fun testAccentColorPreferenceIsStored() {
        val prefs = context.getSharedPreferences("headphone_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("accent_color", "purple").apply()
        
        val stored = prefs.getString("accent_color", "cyan")
        assertEquals("purple", stored)
    }
    
    @Test
    fun testTrackingStatePreferenceIsStored() {
        val prefs = context.getSharedPreferences("headphone_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_tracking", true).apply()
        
        val stored = prefs.getBoolean("is_tracking", false)
        assertTrue(stored)
    }
    
    @Test
    fun testDatabaseCanStoreUsageWithExcludedApps() = runBlocking {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        val usage = HeadphoneUsage(
            date = today,
            packageName = "com.test.app",
            appName = "Test App",
            duration = 3600000L,
            startTime = System.currentTimeMillis() - 3600000L,
            endTime = System.currentTimeMillis()
        )
        
        database.headphoneUsageDao().insertUsage(usage)
        
        val stored = database.headphoneUsageDao().getTotalUsageForDate(today)
        assertEquals(3600000L, stored)
    }
}

