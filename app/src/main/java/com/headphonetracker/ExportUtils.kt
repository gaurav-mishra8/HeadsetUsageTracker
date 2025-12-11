package com.headphonetracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.headphonetracker.data.DetailedUsageSummary
import com.headphonetracker.data.DailyUsageSummary
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object ExportUtils {
    
    /**
     * Export weekly usage data to CSV file and trigger share
     */
    fun exportAndShareWeeklyData(
        context: Context,
        dailySummary: List<DailyUsageSummary>,
        detailedUsage: List<DetailedUsageSummary>
    ) {
        try {
            val csvFile = generateCsvFile(context, dailySummary, detailedUsage)
            shareFile(context, csvFile)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun generateCsvFile(
        context: Context,
        dailySummary: List<DailyUsageSummary>,
        detailedUsage: List<DetailedUsageSummary>
    ): File {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val fileName = "headphone_usage_${dateFormat.format(Date())}.csv"
        
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        
        val file = File(exportDir, fileName)
        
        FileWriter(file).use { writer ->
            // Write header
            writer.append("HEADPHONE USAGE REPORT\n")
            writer.append("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            writer.append("\n")
            
            // Daily Summary Section
            writer.append("=== DAILY SUMMARY ===\n")
            writer.append("Date,Total Duration,Hours,Minutes,Seconds\n")
            
            dailySummary.sortedBy { it.date }.forEach { daily ->
                val duration = formatDurationForCsv(daily.totalDuration)
                writer.append("${daily.date},${daily.totalDuration},${duration.hours},${duration.minutes},${duration.seconds}\n")
            }
            
            // Calculate total
            val totalMs = dailySummary.sumOf { it.totalDuration }
            val totalDuration = formatDurationForCsv(totalMs)
            writer.append("TOTAL,${totalMs},${totalDuration.hours},${totalDuration.minutes},${totalDuration.seconds}\n")
            writer.append("\n")
            
            // Detailed App Usage Section
            writer.append("=== DETAILED APP USAGE ===\n")
            writer.append("Date,App Name,Package Name,Duration (ms),Hours,Minutes,Seconds\n")
            
            detailedUsage.forEach { usage ->
                val duration = formatDurationForCsv(usage.totalDuration)
                // Escape commas in app name
                val escapedAppName = "\"${usage.appName.replace("\"", "\"\"")}\""
                writer.append("${usage.date},$escapedAppName,${usage.packageName},${usage.totalDuration},${duration.hours},${duration.minutes},${duration.seconds}\n")
            }
            
            writer.append("\n")
            
            // App Summary Section (aggregated)
            writer.append("=== APP SUMMARY (WEEKLY TOTAL) ===\n")
            writer.append("App Name,Package Name,Total Duration (ms),Hours,Minutes,Seconds,Percentage\n")
            
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
                writer.append("$escapedAppName,$packageName,$total,${duration.hours},${duration.minutes},${duration.seconds},${String.format("%.1f%%", percentage)}\n")
            }
        }
        
        return file
    }
    
    private fun shareFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Headphone Usage Report")
            putExtra(Intent.EXTRA_TEXT, "Here's my headphone usage report for the past week.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share Usage Report"))
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

