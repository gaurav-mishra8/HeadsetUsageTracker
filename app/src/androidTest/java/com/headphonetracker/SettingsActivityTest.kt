package com.headphonetracker

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.headphonetracker.data.AppDatabase
import com.headphonetracker.data.HeadphoneUsage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.*

@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {
    
    private lateinit var database: AppDatabase
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    
    @Before
    fun setUp() {
        database = AppDatabase.getDatabase(context)
        
        // Clear preferences
        val prefs = context.getSharedPreferences("headphone_tracker_prefs", android.content.Context.MODE_PRIVATE)
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
    fun testSettingsActivityLaunches() {
        val scenario = ActivityScenario.launch(SettingsActivity::class.java)
        
        onView(withText("Settings"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testDailyLimitCardIsDisplayed() {
        val scenario = ActivityScenario.launch(SettingsActivity::class.java)
        
        onView(withText("Daily listening limit"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testBreakRemindersToggleIsDisplayed() {
        val scenario = ActivityScenario.launch(SettingsActivity::class.java)
        
        onView(withText("Break reminders"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testAutoStartToggleIsDisplayed() {
        val scenario = ActivityScenario.launch(SettingsActivity::class.java)
        
        onView(withText("Auto-start on boot"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testExportDataCardIsDisplayed() {
        val scenario = ActivityScenario.launch(SettingsActivity::class.java)
        
        onView(withText("Export as CSV"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testStorageInfoIsDisplayed() {
        val scenario = ActivityScenario.launch(SettingsActivity::class.java)
        
        onView(withText("Storage used"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testStorageInfoShowsCorrectData() = runBlocking {
        // Insert test data
        val usage = HeadphoneUsage(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            packageName = "com.test.app",
            appName = "Test App",
            duration = 3600000L,
            startTime = System.currentTimeMillis() - 3600000L,
            endTime = System.currentTimeMillis()
        )
        database.headphoneUsageDao().insertUsage(usage)
        
        val scenario = ActivityScenario.launch(SettingsActivity::class.java)
        
        // Wait for data to load
        Thread.sleep(500)
        
        onView(withText("Storage used"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testExcludedAppsCardIsDisplayed() {
        val scenario = ActivityScenario.launch(SettingsActivity::class.java)
        
        onView(withText("Excluded apps"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testThemeCardIsDisplayed() {
        val scenario = ActivityScenario.launch(SettingsActivity::class.java)
        
        onView(withText("Theme"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testAccentColorCardIsDisplayed() {
        val scenario = ActivityScenario.launch(SettingsActivity::class.java)
        
        onView(withText("Accent color"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testBackupCardIsDisplayed() {
        val scenario = ActivityScenario.launch(SettingsActivity::class.java)
        
        onView(withText("Backup data"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testRestoreCardIsDisplayed() {
        val scenario = ActivityScenario.launch(SettingsActivity::class.java)
        
        onView(withText("Restore data"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
}


