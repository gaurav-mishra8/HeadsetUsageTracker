package com.headphonetracker

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
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
class ExcludedAppsActivityTest {
    
    private lateinit var database: AppDatabase
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    
    @Before
    fun setUp() {
        database = AppDatabase.getDatabase(context)
        
        // Clear excluded apps
        val prefs = context.getSharedPreferences("headphone_tracker_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putStringSet("excluded_apps", emptySet()).apply()
    }
    
    @After
    fun tearDown() {
        runBlocking {
            database.headphoneUsageDao().deleteAllUsage()
        }
        database.close()
    }
    
    @Test
    fun testExcludedAppsActivityLaunches() {
        val scenario = ActivityScenario.launch(ExcludedAppsActivity::class.java)
        
        onView(withText("Excluded Apps"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testEmptyStateIsDisplayedWithNoData() {
        val scenario = ActivityScenario.launch(ExcludedAppsActivity::class.java)
        
        Thread.sleep(500)
        
        // Should show empty state
        onView(withId(R.id.tvEmptyState))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testAppsListIsDisplayedWithData() = runBlocking {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        // Insert test data
        val usage1 = HeadphoneUsage(
            date = today,
            packageName = "com.spotify.music",
            appName = "Spotify",
            duration = 3600000L,
            startTime = System.currentTimeMillis() - 3600000L,
            endTime = System.currentTimeMillis()
        )
        
        val usage2 = HeadphoneUsage(
            date = today,
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            duration = 1800000L,
            startTime = System.currentTimeMillis() - 1800000L,
            endTime = System.currentTimeMillis()
        )
        
        database.headphoneUsageDao().insertUsage(usage1)
        database.headphoneUsageDao().insertUsage(usage2)
        
        val scenario = ActivityScenario.launch(ExcludedAppsActivity::class.java)
        
        Thread.sleep(1000) // Wait for data to load
        
        // Should show apps list
        onView(withId(R.id.rvApps))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
}


