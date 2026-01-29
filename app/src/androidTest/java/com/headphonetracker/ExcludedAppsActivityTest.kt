package com.headphonetracker

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.headphonetracker.data.AppDatabase
import com.headphonetracker.data.HeadphoneUsage
import com.headphonetracker.data.SettingsRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExcludedAppsActivityTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        hiltRule.inject()

        // Clear excluded apps
        settingsRepository.setExcludedApps(emptySet())
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

        // Should show empty state (card view)
        onView(withId(R.id.cardEmptyState))
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
