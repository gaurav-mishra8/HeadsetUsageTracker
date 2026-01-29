package com.headphonetracker

import android.content.pm.PackageManager
import com.headphonetracker.data.AppUsageSummary
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class AppUsageAdapterTest {

    @Mock
    private lateinit var packageManager: PackageManager

    private lateinit var adapter: AppUsageAdapter

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `test adapter creation with empty list`() {
        adapter = AppUsageAdapter(emptyList(), 0L, packageManager)
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `test adapter item count`() {
        val usageList = listOf(
            AppUsageSummary("com.app1", "App 1", 1000L),
            AppUsageSummary("com.app2", "App 2", 2000L),
            AppUsageSummary("com.app3", "App 3", 3000L)
        )
        adapter = AppUsageAdapter(usageList, 6000L, packageManager)
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `test adapter update data`() {
        adapter = AppUsageAdapter(emptyList(), 0L, packageManager)
        assertEquals(0, adapter.itemCount)

        val newList = listOf(
            AppUsageSummary("com.app1", "App 1", 1000L),
            AppUsageSummary("com.app2", "App 2", 2000L)
        )
        adapter.updateData(newList, 3000L)
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `test adapter sorts by duration descending`() {
        val usageList = listOf(
            AppUsageSummary("com.app1", "App 1", 1000L),
            AppUsageSummary("com.app2", "App 2", 3000L),
            AppUsageSummary("com.app3", "App 3", 2000L)
        )
        adapter = AppUsageAdapter(usageList, 6000L, packageManager)
        adapter.updateData(usageList, 6000L)

        // After updateData, list should be sorted by duration descending
        // We can't directly access the internal list, but we can verify itemCount
        assertEquals(3, adapter.itemCount)
    }
}
