package com.headphonetracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.headphonetracker.MainActivity
import com.headphonetracker.R
import com.headphonetracker.di.AppEntryPoints
import dagger.hilt.android.EntryPointAccessors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailySummaryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            AppEntryPoints::class.java
        )
        val settingsRepository = entryPoint.getSettingsRepository()
        val dao = entryPoint.getHeadphoneUsageDao()

        if (!settingsRepository.isDailySummaryEnabled()) return Result.success()

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val totalMs = dao.getTotalUsageForDate(today) ?: 0L

        if (totalMs == 0L) return Result.success()

        val topApps = dao.getUsageByAppForDate(today)
        val topApp = topApps.maxByOrNull { it.totalDuration }

        val hours = totalMs / 3_600_000
        val minutes = (totalMs % 3_600_000) / 60_000
        val durationText = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

        val bodyText = buildString {
            append("You listened for $durationText today")
            if (topApp != null) append(" · Top app: ${topApp.appName}")
        }

        showNotification("Today's listening summary", bodyText)
        return Result.success()
    }

    private fun showNotification(title: String, body: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily listening summary at 9 PM"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "daily_summary_channel"
        private const val NOTIFICATION_ID = 10
    }
}
