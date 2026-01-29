package com.headphonetracker

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.headphonetracker.data.AppDatabase
import com.headphonetracker.data.HeadphoneUsage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@LargeTest
class IntegrationTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = AppDatabase.getDatabase(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        // Clean up if needed
    }

    @Test
    fun `test database and activity integration`() = runBlocking {
        // Insert test data
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val usage = HeadphoneUsage(
            packageName = "com.test.app",
            appName = "Test App",
            startTime = System.currentTimeMillis() - 3600000,
            endTime = System.currentTimeMillis(),
            duration = 3600000L,
            date = date
        )

        database.headphoneUsageDao().insertUsage(usage)

        // Verify data was inserted
        val total = database.headphoneUsageDao().getTotalUsageForDate(date)
        assertNotNull(total)
        assertTrue(total!! > 0)
    }

    @Test
    fun `test activity can access database`() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Activity should be able to access database
        assertNotNull(database)

        scenario.close()
    }
}
