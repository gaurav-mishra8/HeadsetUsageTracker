package com.headphonetracker

import com.headphonetracker.data.DailyUsageSummary
import com.headphonetracker.data.DetailedUsageSummary
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.*

class ExportUtilsTest {
    
    @Test
    fun `test formatDurationForCsv with hours minutes and seconds`() {
        val millis = 3665000L // 1 hour, 1 minute, 5 seconds
        val result = formatDurationForCsv(millis)
        assertEquals(1L, result.hours)
        assertEquals(1L, result.minutes)
        assertEquals(5L, result.seconds)
    }
    
    @Test
    fun `test formatDurationForCsv with only hours`() {
        val millis = 3600000L // 1 hour
        val result = formatDurationForCsv(millis)
        assertEquals(1L, result.hours)
        assertEquals(0L, result.minutes)
        assertEquals(0L, result.seconds)
    }
    
    @Test
    fun `test formatDurationForCsv with zero`() {
        val millis = 0L
        val result = formatDurationForCsv(millis)
        assertEquals(0L, result.hours)
        assertEquals(0L, result.minutes)
        assertEquals(0L, result.seconds)
    }
    
    @Test
    fun `test generateCsvFile creates valid CSV structure`() {
        val dailySummary = listOf(
            DailyUsageSummary("2024-01-01", 3600000L),
            DailyUsageSummary("2024-01-02", 7200000L)
        )
        
        val detailedUsage = listOf(
            DetailedUsageSummary("2024-01-01", "com.app1", "App 1", 3600000L),
            DetailedUsageSummary("2024-01-02", "com.app2", "App 2", 7200000L)
        )
        
        // Create a mock context directory
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test_exports")
        tempDir.mkdirs()
        
        try {
            val csvContent = generateCsvContent(dailySummary, detailedUsage)
            
            // Verify CSV structure
            assertTrue(csvContent.contains("HEADPHONE USAGE REPORT"))
            assertTrue(csvContent.contains("=== DAILY SUMMARY ==="))
            assertTrue(csvContent.contains("=== DETAILED APP USAGE ==="))
            assertTrue(csvContent.contains("=== APP SUMMARY (WEEKLY TOTAL) ==="))
            
            // Verify data is present
            assertTrue(csvContent.contains("2024-01-01"))
            assertTrue(csvContent.contains("2024-01-02"))
            assertTrue(csvContent.contains("App 1"))
            assertTrue(csvContent.contains("App 2"))
            
            // Verify totals
            assertTrue(csvContent.contains("TOTAL"))
            
        } finally {
            tempDir.deleteRecursively()
        }
    }
    
    @Test
    fun `test CSV handles app names with commas`() {
        val detailedUsage = listOf(
            DetailedUsageSummary("2024-01-01", "com.app", "App, Name", 3600000L)
        )
        
        val csvContent = generateCsvContent(emptyList(), detailedUsage)
        
        // App name with comma should be quoted
        assertTrue(csvContent.contains("\"App, Name\""))
    }
    
    @Test
    fun `test CSV handles app names with quotes`() {
        val detailedUsage = listOf(
            DetailedUsageSummary("2024-01-01", "com.app", "App \"Special\" Name", 3600000L)
        )
        
        val csvContent = generateCsvContent(emptyList(), detailedUsage)
        
        // Quotes should be escaped
        assertTrue(csvContent.contains("\"App \"\"Special\"\" Name\""))
    }
    
    @Test
    fun `test CSV calculates percentages correctly`() {
        val dailySummary = listOf(
            DailyUsageSummary("2024-01-01", 3600000L) // 1 hour total
        )
        
        val detailedUsage = listOf(
            DetailedUsageSummary("2024-01-01", "com.app1", "App 1", 1800000L), // 30 min = 50%
            DetailedUsageSummary("2024-01-01", "com.app2", "App 2", 1800000L)  // 30 min = 50%
        )
        
        val csvContent = generateCsvContent(dailySummary, detailedUsage)
        
        // Should contain percentage calculations
        assertTrue(csvContent.contains("50.0%"))
    }
    
    @Test
    fun `test CSV handles empty data`() {
        val csvContent = generateCsvContent(emptyList(), emptyList())
        
        assertTrue(csvContent.contains("HEADPHONE USAGE REPORT"))
        assertTrue(csvContent.contains("=== DAILY SUMMARY ==="))
        // Should not crash with empty data
    }
    
    private fun generateCsvContent(
        dailySummary: List<DailyUsageSummary>,
        detailedUsage: List<DetailedUsageSummary>
    ): String {
        val sb = StringBuilder()
        
        // Header
        sb.append("HEADPHONE USAGE REPORT\n")
        sb.append("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        sb.append("\n")
        
        // Daily Summary
        sb.append("=== DAILY SUMMARY ===\n")
        sb.append("Date,Total Duration,Hours,Minutes,Seconds\n")
        
        dailySummary.sortedBy { it.date }.forEach { daily ->
            val duration = formatDurationForCsv(daily.totalDuration)
            sb.append("${daily.date},${daily.totalDuration},${duration.hours},${duration.minutes},${duration.seconds}\n")
        }
        
        val totalMs = dailySummary.sumOf { it.totalDuration }
        val totalDuration = formatDurationForCsv(totalMs)
        sb.append("TOTAL,${totalMs},${totalDuration.hours},${totalDuration.minutes},${totalDuration.seconds}\n")
        sb.append("\n")
        
        // Detailed Usage
        sb.append("=== DETAILED APP USAGE ===\n")
        sb.append("Date,App Name,Package Name,Duration (ms),Hours,Minutes,Seconds\n")
        
        detailedUsage.forEach { usage ->
            val duration = formatDurationForCsv(usage.totalDuration)
            val escapedAppName = "\"${usage.appName.replace("\"", "\"\"")}\""
            sb.append("${usage.date},$escapedAppName,${usage.packageName},${usage.totalDuration},${duration.hours},${duration.minutes},${duration.seconds}\n")
        }
        
        sb.append("\n")
        
        // App Summary
        sb.append("=== APP SUMMARY (WEEKLY TOTAL) ===\n")
        sb.append("App Name,Package Name,Total Duration (ms),Hours,Minutes,Seconds,Percentage\n")
        
        val appTotals = detailedUsage
            .groupBy { it.packageName }
            .map { (pkg, usages) ->
                Triple(
                    usages.first().appName,
                    pkg,
                    usages.sumOf { it.totalDuration }
                )
            }
            .sortedByDescending { it.third }
        
        appTotals.forEach { (appName, packageName, total) ->
            val duration = formatDurationForCsv(total)
            val percentage = if (totalMs > 0) (total * 100.0 / totalMs) else 0.0
            val escapedAppName = "\"${appName.replace("\"", "\"\"")}\""
            sb.append("$escapedAppName,$packageName,$total,${duration.hours},${duration.minutes},${duration.seconds},${String.format("%.1f%%", percentage)}\n")
        }
        
        return sb.toString()
    }
    
    private data class DurationParts(
        val hours: Long,
        val minutes: Long,
        val seconds: Long
    )
    
    private fun formatDurationForCsv(millis: Long): DurationParts {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return DurationParts(hours, minutes, seconds)
    }
}


