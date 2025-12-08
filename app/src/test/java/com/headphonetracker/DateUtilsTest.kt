package com.headphonetracker

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class DateUtilsTest {
    
    @Test
    fun `test date format yyyy-MM-dd`() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = Date()
        val formatted = dateFormat.format(date)
        
        assertTrue(formatted.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }
    
    @Test
    fun `test date format parsing`() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = "2024-01-01"
        val date = dateFormat.parse(dateString)
        
        assertNotNull(date)
        
        val calendar = Calendar.getInstance()
        calendar.time = date!!
        assertEquals(2024, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, calendar.get(Calendar.MONTH))
        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH))
    }
    
    @Test
    fun `test date format for today`() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        assertNotNull(today)
        assertEquals(10, today.length) // yyyy-MM-dd format is 10 characters
    }
    
    @Test
    fun `test date format consistency`() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dates = listOf(
            "2024-01-01",
            "2024-12-31",
            "2023-06-15"
        )
        
        dates.forEach { dateString ->
            val date = dateFormat.parse(dateString)
            assertNotNull(date)
            val reformatted = dateFormat.format(date!!)
            assertEquals(dateString, reformatted)
        }
    }
    
    @Test
    fun `test date display format MM-dd`() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        val dateString = "2024-01-15"
        val date = dateFormat.parse(dateString)
        
        assertNotNull(date)
        val display = displayFormat.format(date!!)
        assertEquals("01/15", display)
    }
}

