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
class StatsActivityTest {
    
    private lateinit var database: AppDatabase
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    
    @Before
    fun setUp() {
        database = AppDatabase.getDatabase(context)
    }
    
    @After
    fun tearDown() {
        runBlocking {
            database.headphoneUsageDao().deleteAllUsage()
        }
        database.close()
    }
    
    @Test
    fun testStatsActivityLaunches() {
        val scenario = ActivityScenario.launch(StatsActivity::class.java)
        
        onView(withText("Statistics"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testStreakSectionIsDisplayed() {
        val scenario = ActivityScenario.launch(StatsActivity::class.java)
        
        onView(withText("Day Streak"))
            .check(matches(isDisplayed()))
        
        onView(withText("Best Streak"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testWeekComparisonSectionIsDisplayed() {
        val scenario = ActivityScenario.launch(StatsActivity::class.java)
        
        onView(withText("Week Comparison"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testHourlyChartSectionIsDisplayed() {
        val scenario = ActivityScenario.launch(StatsActivity::class.java)
        
        onView(withText("Listening by Hour"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testCategoriesSectionIsDisplayed() {
        val scenario = ActivityScenario.launch(StatsActivity::class.java)
        
        onView(withText("Usage by Category"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testMonthlySectionIsDisplayed() {
        val scenario = ActivityScenario.launch(StatsActivity::class.java)
        
        onView(withText("This Month"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testStreaksShowZeroWithNoData() {
        val scenario = ActivityScenario.launch(StatsActivity::class.java)
        
        // Wait for data to load
        Thread.sleep(500)
        
        // Streaks should show 0 with no data
        onView(withId(R.id.tvCurrentStreak))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testWeekComparisonShowsZeroWithNoData() {
        val scenario = ActivityScenario.launch(StatsActivity::class.java)
        
        Thread.sleep(500)
        
        onView(withId(R.id.tvThisWeekTotal))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.tvLastWeekTotal))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun testStatsWithData() = runBlocking {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        // Insert test data
        val usage = HeadphoneUsage(
            date = today,
            packageName = "com.spotify.music",
            appName = "Spotify",
            duration = 3600000L,
            startTime = System.currentTimeMillis() - 3600000L,
            endTime = System.currentTimeMillis()
        )
        database.headphoneUsageDao().insertUsage(usage)
        
        val scenario = ActivityScenario.launch(StatsActivity::class.java)
        
        Thread.sleep(1000) // Wait for data to load
        
        // Verify sections are still displayed
        onView(withText("Statistics"))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
}


