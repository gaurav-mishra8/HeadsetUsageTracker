package com.headphonetracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.headphonetracker.MainActivity
import com.headphonetracker.R
import com.headphonetracker.data.HeadphoneUsageDao
import com.headphonetracker.data.SettingsRepository

object MilestoneChecker {

    private const val CHANNEL_ID = "milestones_channel"
    private const val NOTIFICATION_ID = 20

    private data class Milestone(
        val key: String,
        val title: String,
        val message: String
    )

    private val hourMilestones = listOf(
        Milestone("hours_1", "First hour!", "You've listened for 1 hour total. Keep it up!"),
        Milestone("hours_10", "10 hours listened!", "You've clocked 10 hours of headphone use."),
        Milestone("hours_50", "50 hours!", "That's 50 hours of audio tracked. Impressive!"),
        Milestone("hours_100", "100 hours milestone!", "A century of hours — you're a dedicated listener."),
        Milestone("hours_500", "500 hours!", "500 hours tracked. Legendary listening.")
    )
    private val hourThresholds = listOf(1L, 10L, 50L, 100L, 500L)

    private val dayMilestones = listOf(
        Milestone("days_1", "First day tracked!", "You've started tracking your headphone habits."),
        Milestone("days_7", "7 days tracked!", "A full week of listening data collected."),
        Milestone("days_30", "30 days tracked!", "One month of headphone usage data."),
        Milestone("days_100", "100 days tracked!", "100 days of audio insights. You're a power user!")
    )
    private val dayThresholds = listOf(1, 7, 30, 100)

    suspend fun check(context: Context, dao: HeadphoneUsageDao, settingsRepository: SettingsRepository) {
        if (!settingsRepository.isMilestonesEnabled()) return

        val awarded = settingsRepository.getAwardedMilestones()

        // Check total hours milestones
        val totalMs = dao.getTotalUsageAllTime() ?: 0L
        val totalHours = totalMs / 3_600_000L
        hourThresholds.forEachIndexed { i, threshold ->
            val milestone = hourMilestones[i]
            if (totalHours >= threshold && !awarded.contains(milestone.key)) {
                settingsRepository.addAwardedMilestone(milestone.key)
                showNotification(context, milestone.title, milestone.message)
                return // show one milestone at a time
            }
        }

        // Check total days milestones
        val totalDays = dao.getTotalDaysWithUsage()
        dayThresholds.forEachIndexed { i, threshold ->
            val milestone = dayMilestones[i]
            if (totalDays >= threshold && !awarded.contains(milestone.key)) {
                settingsRepository.addAwardedMilestone(milestone.key)
                showNotification(context, milestone.title, milestone.message)
                return
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Milestones",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Milestone achievement notifications"
                setShowBadge(true)
            }
            nm.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }
}
