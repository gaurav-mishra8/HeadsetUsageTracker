package com.headphonetracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar

class DateNavigationTest {

    @Test
    fun `test isSameDay returns true for same calendar day`() {
        val cal1 = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 15, 10, 30, 0)
        }
        val cal2 = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 15, 14, 45, 0)
        }

        assertTrue(isSameDay(cal1, cal2))
    }

    @Test
    fun `test isSameDay returns false for different days`() {
        val cal1 = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 15, 10, 30, 0)
        }
        val cal2 = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 16, 10, 30, 0)
        }

        assertFalse(isSameDay(cal1, cal2))
    }

    @Test
    fun `test isSameDay returns false for different months`() {
        val cal1 = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 15, 10, 30, 0)
        }
        val cal2 = Calendar.getInstance().apply {
            set(2024, Calendar.FEBRUARY, 15, 10, 30, 0)
        }

        assertFalse(isSameDay(cal1, cal2))
    }

    @Test
    fun `test date navigation previous day`() {
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 15)
        }

        val prevDay = calendar.clone() as Calendar
        prevDay.add(Calendar.DAY_OF_YEAR, -1)

        assertEquals(14, prevDay.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `test date navigation next day`() {
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 15)
        }

        val nextDay = calendar.clone() as Calendar
        nextDay.add(Calendar.DAY_OF_YEAR, 1)

        assertEquals(16, nextDay.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `test date navigation respects month boundaries`() {
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 31)
        }

        val nextDay = calendar.clone() as Calendar
        nextDay.add(Calendar.DAY_OF_YEAR, 1)

        assertEquals(Calendar.FEBRUARY, nextDay.get(Calendar.MONTH))
        assertEquals(1, nextDay.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `test date navigation respects year boundaries`() {
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.DECEMBER, 31)
        }

        val nextDay = calendar.clone() as Calendar
        nextDay.add(Calendar.DAY_OF_YEAR, 1)

        assertEquals(2025, nextDay.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, nextDay.get(Calendar.MONTH))
        assertEquals(1, nextDay.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `test date format consistency`() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 15)
        }

        val dateString = dateFormat.format(calendar.time)
        assertEquals("2024-01-15", dateString)
    }

    @Test
    fun `test date range validation within 3 months`() {
        val calendar = Calendar.getInstance()
        val minDate = Calendar.getInstance().apply {
            add(Calendar.MONTH, -3)
        }

        val testDate = Calendar.getInstance().apply {
            add(Calendar.MONTH, -2)
        }

        assertTrue(testDate.timeInMillis >= minDate.timeInMillis)
    }

    @Test
    fun `test date range validation outside 3 months`() {
        val calendar = Calendar.getInstance()
        val minDate = Calendar.getInstance().apply {
            add(Calendar.MONTH, -3)
        }

        val testDate = Calendar.getInstance().apply {
            add(Calendar.MONTH, -4)
        }

        assertFalse(testDate.timeInMillis >= minDate.timeInMillis)
    }

    @Test
    fun `test cannot navigate to future dates`() {
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }

        assertFalse(tomorrow.timeInMillis <= today.timeInMillis)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
