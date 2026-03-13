package com.headphonetracker

/**
 * Shared duration formatting utilities used across the app.
 */
object DurationUtils {

    /**
     * Format milliseconds into a human-readable duration string.
     * Examples: "2h 15m", "45m 30s", "12s"
     */
    fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${secs}s"
            else -> "${secs}s"
        }
    }

    /**
     * Format milliseconds into a short duration (no seconds when hours present).
     * Examples: "2h 15m", "45m"
     */
    fun formatDurationShort(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}
